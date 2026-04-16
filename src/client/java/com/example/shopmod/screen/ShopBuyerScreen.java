package com.example.shopmod.screen;

import com.example.shopmod.client.ClientPacketHandler;
import com.example.shopmod.client.ClientPacketHandler.ShopNavigationHolder;
import com.example.shopmod.data.ShopData;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ShopBuyerScreen extends Screen {

    private final String shopName;
    private ShopData shopData;
    private int selectedTrade = 0;
    private int scrollOffset = 0;
    private static final int VISIBLE_TRADES = 7, ROW_HEIGHT = 28, SLOT_SIZE = 24;
    private static final int W = 360, H = 270;

    public ShopBuyerScreen(String shopName, ShopData data) {
        super(Component.literal(shopName + "'s Shop"));
        this.shopName = shopName;
        this.shopData = data;
    }

    public void rebuild() { clearWidgets(); init(); }

    public void refreshData(ShopData data) {
        this.shopData = data;
        if (selectedTrade >= data.getTrades().size()) selectedTrade = Math.max(0, data.getTrades().size() - 1);
        scrollOffset = Math.min(scrollOffset, Math.max(0, data.getTrades().size() - VISIBLE_TRADES));
        clearWidgets(); init();
    }

    private static void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color); ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color); ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawItemWithCount(GuiGraphics ctx, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        ctx.renderItem(stack, x, y);
        if (stack.getCount() > 1) {
            int w = font.width(String.valueOf(stack.getCount()));
            ctx.drawString(font, Component.literal(String.valueOf(stack.getCount())), x + 19 - w, y + 11, 0xFFFFFFFF);
        }
    }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        List<ShopData.ShopTrade> trades = shopData.getTrades();
        if (!trades.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("Buy"), btn -> {
                if (selectedTrade >= 0 && selectedTrade < trades.size())
                    ClientPlayNetworking.send(new ModPackets.DoTradePayload(shopName, selectedTrade));
            }).bounds(px + W / 2 - 100, py + H - 32, 80, 22).build());
        }
        if (!ShopNavigationHolder.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("\u25C0"), btn -> {
                String prev = ShopNavigationHolder.getPrevShop(shopName);
                if (prev != null) ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(prev));
            }).bounds(px - 26, py + H / 2 - 12, 22, 24).build());
            addRenderableWidget(Button.builder(Component.literal("\u25B6"), btn -> {
                String next = ShopNavigationHolder.getNextShop(shopName);
                if (next != null) ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(next));
            }).bounds(px + W + 4, py + H / 2 - 12, 22, 24).build());
        }
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px = (width - W) / 2, py = (height - H) / 2;
        ctx.fill(px, py, px + W, py + H, 0xCC000000);
        drawBorder(ctx, px, py, W, H, 0xFF8B4513);
        ctx.fill(px, py, px + W, py + 22, 0xFF553311);
        ctx.drawCenteredString(font, title, px + W / 2, py + 7, 0xFFFFFFFF);

        List<ShopData.ShopTrade> trades = shopData.getTrades();
        int listX = px + 10, listY = py + 28, listW = W - 20, listH = VISIBLE_TRADES * ROW_HEIGHT;
        ctx.fill(listX, listY, listX + listW, listY + listH, 0x88000000);
        drawBorder(ctx, listX, listY, listW, listH, 0xFF666666);

        if (trades.isEmpty()) {
            ctx.drawCenteredString(font, Component.literal("No trades available"), px + W / 2, listY + listH / 2 - 4, 0xFFFFFFFF);
        } else {
            int endIdx = Math.min(scrollOffset + VISIBLE_TRADES, trades.size());
            for (int i = scrollOffset; i < endIdx; i++) {
                ShopData.ShopTrade trade = trades.get(i);
                int rowY = listY + (i - scrollOffset) * ROW_HEIGHT;
                if (i == selectedTrade) ctx.fill(listX + 1, rowY, listX + listW - 1, rowY + ROW_HEIGHT, 0x80FFD700);
                else if (mx >= listX && mx < listX + listW && my >= rowY && my < rowY + ROW_HEIGHT) ctx.fill(listX + 1, rowY, listX + listW - 1, rowY + ROW_HEIGHT, 0x40FFFFFF);

                int itemX = listX + 10, itemY = rowY + 2;
                ctx.fill(itemX, itemY, itemX + SLOT_SIZE, itemY + SLOT_SIZE, 0xFF333333);
                drawBorder(ctx, itemX, itemY, SLOT_SIZE, SLOT_SIZE, 0xFF666666);
                drawItemWithCount(ctx, trade.sellItem, itemX + 4, itemY + 4);

                int arrowX = itemX + SLOT_SIZE + 6;
                ctx.drawCenteredString(font, Component.literal("->"), arrowX + 10, rowY + ROW_HEIGHT / 2 - 4, 0xFFFFFFFF);

                int priceX = arrowX + 24;
                ctx.fill(priceX, itemY, priceX + SLOT_SIZE, itemY + SLOT_SIZE, 0xFF333333);
                drawBorder(ctx, priceX, itemY, SLOT_SIZE, SLOT_SIZE, 0xFF666666);
                drawItemWithCount(ctx, trade.buyItem, priceX + 4, itemY + 4);

                String tradeStr = trade.sellItem.getHoverName().getString() + " -> " + trade.buyItem.getHoverName().getString();
                int maxLen = (listX + listW - 10 - (priceX + SLOT_SIZE + 6)) / 6;
                if (maxLen < 10) maxLen = 10;
                if (tradeStr.length() > maxLen) tradeStr = tradeStr.substring(0, maxLen - 3) + "...";
                ctx.drawString(font, Component.literal(tradeStr), priceX + SLOT_SIZE + 6, rowY + ROW_HEIGHT / 2 - 4, 0xFFFFFFFF);
            }

            if (trades.size() > VISIBLE_TRADES) {
                int thumbH = Math.max(20, listH * VISIBLE_TRADES / trades.size());
                int thumbY = listY + (listH - thumbH) * scrollOffset / (trades.size() - VISIBLE_TRADES);
                ctx.fill(listX + listW - 6, listY, listX + listW - 2, listY + listH, 0xFF555555);
                ctx.fill(listX + listW - 6, thumbY, listX + listW - 2, thumbY + thumbH, 0xFFAAAAAA);
            }
            ctx.drawString(font, Component.literal("Offers: " + trades.size()), px + W / 2 - 10, py + H - 28, 0xFFFFFFFF);
        }
        super.render(ctx, mx, my, delta);

        // Proper tooltip rendering for hovered items
        for (int ti = scrollOffset; ti < Math.min(scrollOffset + VISIBLE_TRADES, trades.size()); ti++) {
            ShopData.ShopTrade trade = trades.get(ti);
            int ry = listY + (ti - scrollOffset) * ROW_HEIGHT;
            int itemX = listX + 10, itemY = ry + 2;
            if (!trade.sellItem.isEmpty() && mx >= itemX && mx < itemX + SLOT_SIZE && my >= itemY && my < itemY + SLOT_SIZE) {
                ctx.setTooltipForNextFrame(font, trade.sellItem, mx, my); break;
            }
            int priceX = itemX + SLOT_SIZE + 36;
            if (!trade.buyItem.isEmpty() && mx >= priceX && mx < priceX + SLOT_SIZE && my >= itemY && my < itemY + SLOT_SIZE) {
                ctx.setTooltipForNextFrame(font, trade.buyItem, mx, my); break;
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int listX = px + 10, listY = py + 28;
        double mx = event.x(), my = event.y();
        List<ShopData.ShopTrade> trades = shopData.getTrades();
        if (mx >= listX && mx < listX + W - 20 && my >= listY && my < listY + VISIBLE_TRADES * ROW_HEIGHT) {
            int clickedIdx = scrollOffset + (int) ((my - listY) / ROW_HEIGHT);
            if (clickedIdx >= 0 && clickedIdx < trades.size()) { selectedTrade = clickedIdx; return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, shopData.getTrades().size() - VISIBLE_TRADES);
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) verticalAmount, maxScroll));
        if (selectedTrade < scrollOffset) selectedTrade = scrollOffset;
        if (selectedTrade >= scrollOffset + VISIBLE_TRADES) selectedTrade = scrollOffset + VISIBLE_TRADES - 1;
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
