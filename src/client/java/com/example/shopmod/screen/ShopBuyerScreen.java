package com.example.shopmod.screen;

import com.example.shopmod.client.ClientPacketHandler;
import com.example.shopmod.client.ClientPacketHandler.ShopNavigationHolder;
import com.example.shopmod.data.ShopData;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ShopBuyerScreen extends Screen {

    private final String shopName;
    private ShopData shopData;
    private int selectedTrade = 0;
    private int scrollOffset = 0;
    private static final int VISIBLE_TRADES = 7;
    private static final int ROW_HEIGHT = 28;
    private static final int SLOT_SIZE = 24;
    private static final int W = 360, H = 270;

    public ShopBuyerScreen(String shopName, ShopData data) {
        super(Text.literal(shopName + "'s Shop"));
        this.shopName = shopName;
        this.shopData = data;
    }
    
    /** Rebuild UI widgets (e.g. after navigation data arrives) */
    public void rebuild() { clearChildren(); init(); }

    public void refreshData(ShopData data) {
        this.shopData = data;
        // Adjust selected trade if it's now out of bounds
        if (selectedTrade >= data.getTrades().size()) {
            selectedTrade = Math.max(0, data.getTrades().size() - 1);
        }
        // Adjust scroll offset
        int maxScroll = Math.max(0, data.getTrades().size() - VISIBLE_TRADES);
        scrollOffset = Math.min(scrollOffset, maxScroll);
        clearChildren();
        init();
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawItemWithCount(DrawContext ctx, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        ctx.drawItem(stack, x, y);
        if (stack.getCount() > 1) {
            String countStr = String.valueOf(stack.getCount());
            int w = textRenderer.getWidth(countStr);
            ctx.drawTextWithShadow(textRenderer, Text.literal(countStr), x + 19 - w, y + 11, 0xFFFFFFFF);
        }
    }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        List<ShopData.ShopTrade> trades = shopData.getTrades();

        if (!trades.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Buy"), btn -> {
                if (selectedTrade >= 0 && selectedTrade < trades.size()) {
                    ClientPlayNetworking.send(new ModPackets.DoTradePayload(shopName, selectedTrade));
                    // Screen will be refreshed via OPEN_BUYER_ID packet from server
                }
            }).dimensions(px + W / 2 - 100, py + H - 32, 80, 22).build());
        }

        // Navigation arrows - always show (loop navigation between shops)
        if (!ShopNavigationHolder.isEmpty()) {
            // Always show left arrow (loops from first → last)
            addDrawableChild(ButtonWidget.builder(Text.literal("\u25C0"), btn -> {
                String prev = ShopNavigationHolder.getPrevShop(shopName);
                if (prev != null) {
                    ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(prev));
                }
            }).dimensions(px - 26, py + H / 2 - 12, 22, 24).build());

            // Always show right arrow (loops from last → first)
            addDrawableChild(ButtonWidget.builder(Text.literal("\u25B6"), btn -> {
                String next = ShopNavigationHolder.getNextShop(shopName);
                if (next != null) {
                    ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(next));
                }
            }).dimensions(px + W + 4, py + H / 2 - 12, 22, 24).build());
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px = (width - W) / 2, py = (height - H) / 2;

        // Main background - dark theme like ShopOwnerScreen
        ctx.fill(px, py, px + W, py + H, 0xCC000000);
        drawBorder(ctx, px, py, W, H, 0xFF8B4513);

        // Title bar
        ctx.fill(px, py, px + W, py + 22, 0xFF553311);
        ctx.drawCenteredTextWithShadow(textRenderer, title, px + W / 2, py + 7, 0xFFFFFFFF);

        List<ShopData.ShopTrade> trades = shopData.getTrades();

        // Trade list background
        int listX = px + 10;
        int listY = py + 28;
        int listW = W - 20;
        int listH = VISIBLE_TRADES * ROW_HEIGHT;
        ctx.fill(listX, listY, listX + listW, listY + listH, 0x88000000);
        drawBorder(ctx, listX, listY, listW, listH, 0xFF666666);

        if (trades.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("No trades available"), px + W / 2, listY + listH / 2 - 4, 0xFFFFFFFF);
        } else {
            // Draw trades
            int startIdx = scrollOffset;
            int endIdx = Math.min(startIdx + VISIBLE_TRADES, trades.size());

            for (int i = startIdx; i < endIdx; i++) {
                ShopData.ShopTrade trade = trades.get(i);
                int rowY = listY + (i - startIdx) * ROW_HEIGHT;

                // Highlight selected trade
                boolean isSelected = (i == selectedTrade);
                if (isSelected) {
                    ctx.fill(listX + 1, rowY, listX + listW - 1, rowY + ROW_HEIGHT, 0x80FFD700);
                }

                // Hover effect
                boolean hovered = mx >= listX && mx < listX + listW && my >= rowY && my < rowY + ROW_HEIGHT;
                if (hovered && !isSelected) {
                    ctx.fill(listX + 1, rowY, listX + listW - 1, rowY + ROW_HEIGHT, 0x40FFFFFF);
                }

                // Draw item slot (left side)
                int itemX = listX + 10;
                int itemY = rowY + 2;
                ctx.fill(itemX, itemY, itemX + SLOT_SIZE, itemY + SLOT_SIZE, 0xFF333333);
                drawBorder(ctx, itemX, itemY, SLOT_SIZE, SLOT_SIZE, 0xFF666666);
                drawItemWithCount(ctx, trade.sellItem, itemX + 4, itemY + 4);

                // Draw arrow between slots
                int arrowX = itemX + SLOT_SIZE + 6;
                ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("->"), arrowX + 10, rowY + ROW_HEIGHT / 2 - 4, 0xFFFFFFFF);

                // Draw price slot (right side)
                int priceX = arrowX + 24;
                ctx.fill(priceX, itemY, priceX + SLOT_SIZE, itemY + SLOT_SIZE, 0xFF333333);
                drawBorder(ctx, priceX, itemY, SLOT_SIZE, SLOT_SIZE, 0xFF666666);
                drawItemWithCount(ctx, trade.buyItem, priceX + 4, itemY + 4);

                // Draw item name -> price name
                String itemName = trade.sellItem.getName().getString();
                String priceName = trade.buyItem.getName().getString();
                String tradeText = itemName + " -> " + priceName;
                int maxTextLen = (listX + listW - 10 - (priceX + SLOT_SIZE + 6)) / 6;
                if (maxTextLen < 10) maxTextLen = 10;
                if (tradeText.length() > maxTextLen) tradeText = tradeText.substring(0, maxTextLen - 3) + "...";
                ctx.drawTextWithShadow(textRenderer, Text.literal(tradeText), priceX + SLOT_SIZE + 6, rowY + ROW_HEIGHT / 2 - 4, 0xFFFFFFFF);
            }

            // Draw scroll bar on the right side
            int scrollBarX = listX + listW - 6;
            int scrollBarH = listH;
            ctx.fill(scrollBarX, listY, scrollBarX + 4, listY + scrollBarH, 0xFF555555);
            
            // Draw scroll thumb
            int totalTrades = trades.size();
            if (totalTrades > VISIBLE_TRADES) {
                int thumbH = Math.max(20, scrollBarH * VISIBLE_TRADES / totalTrades);
                int thumbY = listY + (scrollBarH - thumbH) * scrollOffset / (totalTrades - VISIBLE_TRADES);
                ctx.fill(scrollBarX, thumbY, scrollBarX + 4, thumbY + thumbH, 0xFFAAAAAA);
            }

            // Offers count - to the right of Buy button
            ctx.drawTextWithShadow(textRenderer, Text.literal("Offers: " + trades.size()), px + W / 2 - 10, py + H - 28, 0xFFFFFFFF);
        }

        super.render(ctx, mx, my, delta);

        // Tooltips for trade list items (sell & buy columns)
        for (int ti = scrollOffset; ti < Math.min(scrollOffset + VISIBLE_TRADES, trades.size()); ti++) {
            ShopData.ShopTrade trade = trades.get(ti);
            int rowY = listY + (ti - scrollOffset) * ROW_HEIGHT;
            int itemX = listX + 10;
            int itemY = rowY + 2;
            // Hover over sell item (left)
            if (!trade.sellItem.isEmpty() && mx >= itemX && mx < itemX + SLOT_SIZE && my >= itemY && my < itemY + SLOT_SIZE) {
                ctx.drawItemTooltip(textRenderer, trade.sellItem, mx, my);
                break;
            }
            // Hover over buy item (right / price)
            int priceX = itemX + SLOT_SIZE + 36;
            if (!trade.buyItem.isEmpty() && mx >= priceX && mx < priceX + SLOT_SIZE && my >= itemY && my < itemY + SLOT_SIZE) {
                ctx.drawItemTooltip(textRenderer, trade.buyItem, mx, my);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int listX = px + 10;
        int listY = py + 28;
        int listW = W - 20;
        double mx = click.x(), my = click.y();

        List<ShopData.ShopTrade> trades = shopData.getTrades();

        // Check if clicking on trade list
        if (mx >= listX && mx < listX + listW && my >= listY && my < listY + VISIBLE_TRADES * ROW_HEIGHT) {
            int clickedRow = (int) ((my - listY) / ROW_HEIGHT);
            int clickedIdx = scrollOffset + clickedRow;

            if (clickedIdx >= 0 && clickedIdx < trades.size()) {
                selectedTrade = clickedIdx;
                return true;
            }
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<ShopData.ShopTrade> trades = shopData.getTrades();
        int maxScroll = Math.max(0, trades.size() - VISIBLE_TRADES);
        
        scrollOffset -= (int) verticalAmount;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        
        // Adjust selected trade if it's out of view
        if (selectedTrade < scrollOffset) selectedTrade = scrollOffset;
        if (selectedTrade >= scrollOffset + VISIBLE_TRADES) selectedTrade = scrollOffset + VISIBLE_TRADES - 1;
        
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
