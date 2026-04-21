package com.example.shopmod.screen;

import com.example.shopmod.client.ClientPacketHandler.ShopNavigationHolder;
import com.example.shopmod.data.I18n;
import com.example.shopmod.data.ShopData;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    private static final int VISIBLE_TRADES = 7, ROW_HEIGHT = 28, SLOT_SIZE = 22;
    private static final int W = 380, H = 270;

    // Grid columns: Item | Price | Details
    private static final int COL_ITEM_W = 90;
    private static final int COL_PRICE_W = 90;
    private static final int HEADER_H = 16;

    public ShopBuyerScreen(String shopName, ShopData data) {
        super(Component.literal(I18n.get("buyer.title", shopName)));
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

    private static void drawBorder(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color); ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color); ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawItemWithCount(GuiGraphicsExtractor ctx, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        ctx.item(stack, x, y);
        if (stack.getCount() > 1) {
            int w = font.width(String.valueOf(stack.getCount()));
            ctx.text(font, Component.literal(String.valueOf(stack.getCount())), x + 17 - w, y + 10, 0xFFFFFFFF);
        }
    }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        List<ShopData.ShopTrade> trades = shopData.getTrades();
        if (!trades.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal(I18n.get("buyer.buy")), btn -> {
                if (selectedTrade >= 0 && selectedTrade < trades.size())
                    ClientPlayNetworking.send(new ModPackets.DoTradePayload(shopName, selectedTrade));
            }).bounds(px + W / 2 - 95, py + H - 26, 80, 22).build());
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
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);
        int px = (width - W) / 2, py = (height - H) / 2;
        ctx.fill(px, py, px + W, py + H, 0xCC000000);
        drawBorder(ctx, px, py, W, H, 0xFF8B4513);
        ctx.fill(px, py, px + W, py + 22, 0xFF553311);
        ctx.centeredText(font, title, px + W / 2, py + 7, 0xFFFFFFFF);

        List<ShopData.ShopTrade> trades = shopData.getTrades();
        int listX = px + 10, listY = py + 28, listW = W - 20;
        int dataH = VISIBLE_TRADES * ROW_HEIGHT;
        int totalH = HEADER_H + dataH;
        ctx.fill(listX, listY, listX + listW, listY + totalH, 0x88000000);
        drawBorder(ctx, listX, listY, listW, totalH, 0xFF666666);

        // Header row background
        int headerY = listY;
        ctx.fill(listX + 1, headerY, listX + listW - 1, headerY + HEADER_H, 0xFF2A2A2A);

        // Column header text
        int col1Center = listX + COL_ITEM_W / 2;
        int col2Center = listX + COL_ITEM_W + COL_PRICE_W / 2;
        int col3Start = listX + COL_ITEM_W + COL_PRICE_W;
        int col3Center = col3Start + (listW - COL_ITEM_W - COL_PRICE_W) / 2;

        int headerTextY = headerY + (HEADER_H - 8) / 2;

        // Item = Green, Price = Yellow, Details = White
        ctx.centeredText(font, Component.literal(I18n.get("shop.item_header")), col1Center, headerTextY, 0xFF55FF55);
        ctx.centeredText(font, Component.literal(I18n.get("shop.price_header")), col2Center, headerTextY, 0xFFFFFF55);

        // Details header
        String detailsHeader = I18n.get("buyer.details");
        int detailsW = font.width(detailsHeader);
        ctx.text(font, Component.literal(detailsHeader), col3Center - detailsW / 2, headerTextY, 0xFFFFFFFF);

        // Vertical column separators
        int sep1X = listX + COL_ITEM_W;
        int sep2X = listX + COL_ITEM_W + COL_PRICE_W;
        ctx.fill(sep1X, listY + 1, sep1X + 1, listY + totalH - 1, 0xFF555555);
        ctx.fill(sep2X, listY + 1, sep2X + 1, listY + totalH - 1, 0xFF555555);

        // Horizontal separator below header
        ctx.fill(listX + 1, headerY + HEADER_H, listX + listW - 1, headerY + HEADER_H + 1, 0xFF555555);

        if (trades.isEmpty()) {
            ctx.centeredText(font, Component.literal(I18n.get("buyer.no_trades")),
                listX + listW / 2, headerY + HEADER_H + dataH / 2 - 4, 0xFFFFFFFF);
        } else {
            int endIdx = Math.min(scrollOffset + VISIBLE_TRADES, trades.size());
            for (int i = scrollOffset; i < endIdx; i++) {
                ShopData.ShopTrade trade = trades.get(i);
                int rowY = headerY + HEADER_H + 1 + (i - scrollOffset) * ROW_HEIGHT;

                // Row highlight
                if (i == selectedTrade) {
                    ctx.fill(listX + 2, rowY, listX + listW - 2, rowY + ROW_HEIGHT, 0x80FFD700);
                } else if (mx >= listX && mx < listX + listW && my >= rowY && my < rowY + ROW_HEIGHT) {
                    ctx.fill(listX + 2, rowY, listX + listW - 2, rowY + ROW_HEIGHT, 0x40FFFFFF);
                }

                // Column 1: Item slot
                int itemSlotX = col1Center - SLOT_SIZE / 2;
                int itemSlotY = rowY + (ROW_HEIGHT - SLOT_SIZE) / 2;
                ctx.fill(itemSlotX, itemSlotY, itemSlotX + SLOT_SIZE, itemSlotY + SLOT_SIZE, 0xFF333333);
                drawBorder(ctx, itemSlotX, itemSlotY, SLOT_SIZE, SLOT_SIZE, 0xFF666666);
                drawItemWithCount(ctx, trade.sellItem, itemSlotX + 3, itemSlotY + 3);

                // Column 2: Price slot
                int priceSlotX = col2Center - SLOT_SIZE / 2;
                int priceSlotY = rowY + (ROW_HEIGHT - SLOT_SIZE) / 2;
                ctx.fill(priceSlotX, priceSlotY, priceSlotX + SLOT_SIZE, priceSlotY + SLOT_SIZE, 0xFF333333);
                drawBorder(ctx, priceSlotX, priceSlotY, SLOT_SIZE, SLOT_SIZE, 0xFF666666);
                drawItemWithCount(ctx, trade.buyItem, priceSlotX + 3, priceSlotY + 3);

                // Column 3: Details text (Item -> Price)
                String sellName = trade.sellItem.getHoverName().getString();
                String buyName = trade.buyItem.getHoverName().getString();
                String tradeStr = sellName + " \u2192 " + buyName;

                // Truncate if too long
                int maxTextW = listW - COL_ITEM_W - COL_PRICE_W - 16;
                int tradeStrW = font.width(tradeStr);
                if (tradeStrW > maxTextW) {
                    while (font.width(tradeStr + "...") > maxTextW && tradeStr.length() > 3) {
                        tradeStr = tradeStr.substring(0, tradeStr.length() - 1);
                    }
                    tradeStr = tradeStr + "...";
                }

                int detailsTextY = rowY + (ROW_HEIGHT - 8) / 2;
                int detailsTextX = col3Start + (listW - COL_ITEM_W - COL_PRICE_W - font.width(tradeStr)) / 2;
                ctx.text(font, Component.literal(tradeStr), detailsTextX, detailsTextY, 0xFFCCCCCC);
            }

            // Scrollbar
            if (trades.size() > VISIBLE_TRADES) {
                int thumbH = Math.max(20, dataH * VISIBLE_TRADES / trades.size());
                int thumbY = headerY + HEADER_H + (dataH - thumbH) * scrollOffset / (trades.size() - VISIBLE_TRADES);
                ctx.fill(listX + listW - 6, headerY + HEADER_H, listX + listW - 2, headerY + HEADER_H + dataH, 0xFF555555);
                ctx.fill(listX + listW - 6, thumbY, listX + listW - 2, thumbY + thumbH, 0xFFAAAAAA);
            }

            ctx.text(font, Component.literal(I18n.get("buyer.offers_count", trades.size())), px + W / 2 - 5, py + H - 18, 0xFFFFFFFF);
        }
        super.extractRenderState(ctx, mx, my, delta);

        // Tooltip rendering for hovered item slots
        for (int ti = scrollOffset; ti < Math.min(scrollOffset + VISIBLE_TRADES, trades.size()); ti++) {
            ShopData.ShopTrade trade = trades.get(ti);
            int ry = headerY + HEADER_H + 1 + (ti - scrollOffset) * ROW_HEIGHT;

            // Item slot tooltip
            int itemSlotX = col1Center - SLOT_SIZE / 2;
            int itemSlotY = ry + (ROW_HEIGHT - SLOT_SIZE) / 2;
            if (!trade.sellItem.isEmpty() && mx >= itemSlotX && mx < itemSlotX + SLOT_SIZE
                && my >= itemSlotY && my < itemSlotY + SLOT_SIZE) {
                ctx.setTooltipForNextFrame(font, trade.sellItem, mx, my);
                break;
            }

            // Price slot tooltip
            int priceSlotX = col2Center - SLOT_SIZE / 2;
            int priceSlotY = ry + (ROW_HEIGHT - SLOT_SIZE) / 2;
            if (!trade.buyItem.isEmpty() && mx >= priceSlotX && mx < priceSlotX + SLOT_SIZE
                && my >= priceSlotY && my < priceSlotY + SLOT_SIZE) {
                ctx.setTooltipForNextFrame(font, trade.buyItem, mx, my);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int listX = px + 10, listY = py + 28 + HEADER_H;
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