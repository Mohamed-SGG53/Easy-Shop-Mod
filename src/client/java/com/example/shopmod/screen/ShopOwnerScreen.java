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
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ShopOwnerScreen extends Screen {

    private final String shopName;
    private ShopData shopData;
    private ItemStack pendingSell = ItemStack.EMPTY;
    private ItemStack pendingBuy  = ItemStack.EMPTY;
    private int page = 0;
    private static final int PER_PAGE = 4;
    private static final int ROW_HEIGHT = 28;
    private static final int W = 360, H = 270; // Reduced from 280 by 10px (5 from top + 5 from bottom)
    private static final int SLOT_SIZE = 28;
    private int sellSlotX, sellSlotY;
    private int buySlotX, buySlotY;

    public ShopOwnerScreen(String shopName, ShopData data) {
        super(Text.literal(shopName + " - Shop Manager"));
        this.shopName = shopName;
        this.shopData = data;
    }

    /** Rebuild UI widgets (e.g. after navigation data arrives) */
    public void rebuild() { clearChildren(); init(); }

    public void refreshData(ShopData data) { this.shopData = data; clearChildren(); init(); }
    public void setPendingBuyItem(ItemStack s) { this.pendingBuy = s; }
    public void setPendingSellItem(ItemStack s) { this.pendingSell = s; }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        
        // Calculate centered slot positions
        // Available space from px + 8 to px + 250 (before Storage button) = 242px
        // Center the slot in this space
        int slotCenterX = px + 8 + (242 - SLOT_SIZE) / 2;
        sellSlotX = slotCenterX; sellSlotY = py + 30;
        buySlotX = slotCenterX; buySlotY = py + 62;

        // Storage button - far right on row 1
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Storage"),
            btn -> ClientPlayNetworking.send(new ModPackets.ReqStoragePayload(shopName))
        ).dimensions(px + 250, py + 28, 90, 24).build());

        // Add Offer button - far right on row 2
        addDrawableChild(ButtonWidget.builder(
            Text.literal("Add Offer"),
            btn -> submitTrade()
        ).dimensions(px + 250, py + 60, 90, 24).build());

        // Delete buttons for trades - on the right side of each row (after Price column)
        List<ShopData.ShopTrade> trades = shopData.getTrades();
        int start = page * PER_PAGE, end = Math.min(start + PER_PAGE, trades.size());
        for (int i = start; i < end; i++) {
            final int idx = i;
            final int row = i - start;
            // Button is on the right side of the row, NOT in the Price column
            addDrawableChild(ButtonWidget.builder(Text.literal("X"),
                btn -> ClientPlayNetworking.send(new ModPackets.RemoveTradePayload(shopName, idx)))
                .dimensions(px + 275, py + 120 + row * ROW_HEIGHT, 26, 20).build());
        }

        int total = Math.max(1, (int) Math.ceil(trades.size() / (double) PER_PAGE));
        if (page > 0)
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> { page--; clearChildren(); init(); })
                .dimensions(px + 10, py + H - 28, 40, 20).build());
        if (page < total - 1)
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> { page++; clearChildren(); init(); })
                .dimensions(px + W - 50, py + H - 28, 40, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
            .dimensions(px + W / 2 - 75, py + H - 28, 80, 20).build());

        // Navigation arrows - always show (loop navigation between shops)
        if (!ShopNavigationHolder.isEmpty()) {
            // Always show left arrow (loops from first → last)
            addDrawableChild(ButtonWidget.builder(Text.literal("\u25C0"), btn -> {
                ClientPacketHandler.PendingSellHolder.clear();
                ClientPacketHandler.PendingBuyHolder.clear();
                String prev = ShopNavigationHolder.getPrevShop(shopName);
                if (prev != null) {
                    ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(prev));
                }
            }).dimensions(px - 26, py + H / 2 - 12, 22, 24).build());

            // Always show right arrow (loops from last → first)
            addDrawableChild(ButtonWidget.builder(Text.literal("\u25B6"), btn -> {
                ClientPacketHandler.PendingSellHolder.clear();
                ClientPacketHandler.PendingBuyHolder.clear();
                String next = ShopNavigationHolder.getNextShop(shopName);
                if (next != null) {
                    ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(next));
                }
            }).dimensions(px + W + 4, py + H / 2 - 12, 22, 24).build());
        }
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
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px = (width - W) / 2, py = (height - H) / 2;

        // Main background
        ctx.fill(px, py, px + W, py + H, 0xCC000000);
        drawBorder(ctx, px, py, W, H, 0xFF8B4513);
        
        // Title bar
        ctx.fill(px, py, px + W, py + 22, 0xFF553311);
        ctx.drawCenteredTextWithShadow(textRenderer, title, px + W / 2, py + 7, 0xFFFFFFFF);

        // Top section background (expanded for bigger slots)
        ctx.fill(px + 8, py + 24, px + W - 8, py + 96, 0x88000000);
        drawBorder(ctx, px + 8, py + 24, W - 16, 72, 0xFF666666);

        // Row 1: "Add a Buyable Item:" label (GREEN) and slot
        ctx.drawTextWithShadow(textRenderer, "Add a Buyable Item:", px + 14, py + 38, 0xFF55FF55);
        boolean sellHov = mx >= sellSlotX && mx < sellSlotX + SLOT_SIZE && my >= sellSlotY && my < sellSlotY + SLOT_SIZE;
        ctx.fill(sellSlotX, sellSlotY, sellSlotX + SLOT_SIZE, sellSlotY + SLOT_SIZE, sellHov ? 0xFF553300 : 0xFF333333);
        drawBorder(ctx, sellSlotX, sellSlotY, SLOT_SIZE, SLOT_SIZE, pendingSell.isEmpty() ? 0xFF888888 : 0xFF00FF00);
        if (!pendingSell.isEmpty()) {
            drawItemWithCount(ctx, pendingSell, sellSlotX + 6, sellSlotY + 6);
            // Show count next to slot
            ctx.drawTextWithShadow(textRenderer, "x" + pendingSell.getCount(), sellSlotX + SLOT_SIZE + 3, sellSlotY + 10, 0xFF55FF55);
        }

        // Row 2: "Item Price:" label (YELLOW) and slot
        ctx.drawTextWithShadow(textRenderer, "Item Price:", px + 14, py + 70, 0xFFFFFF00);
        boolean buyHov = mx >= buySlotX && mx < buySlotX + SLOT_SIZE && my >= buySlotY && my < buySlotY + SLOT_SIZE;
        ctx.fill(buySlotX, buySlotY, buySlotX + SLOT_SIZE, buySlotY + SLOT_SIZE, buyHov ? 0xFF553300 : 0xFF333333);
        drawBorder(ctx, buySlotX, buySlotY, SLOT_SIZE, SLOT_SIZE, pendingBuy.isEmpty() ? 0xFF888888 : 0xFFFFFF00);
        if (!pendingBuy.isEmpty()) {
            drawItemWithCount(ctx, pendingBuy, buySlotX + 6, buySlotY + 6);
            // Show price count next to slot
            ctx.drawTextWithShadow(textRenderer, "x" + pendingBuy.getCount(), buySlotX + SLOT_SIZE + 3, buySlotY + 10, 0xFFFFFF00);
        }

        // Trades section background
        ctx.fill(px + 8, py + 100, px + W - 8, py + H - 36, 0x66000000);
        drawBorder(ctx, px + 8, py + 100, W - 16, H - 136, 0xFF666666);

        // Header: Item | Price | Cancel
        ctx.drawTextWithShadow(textRenderer, "Item", px + 40, py + 106, 0xFF55FF55);
        ctx.drawTextWithShadow(textRenderer, "Price", px + 140, py + 106, 0xFFFFFF00);
        ctx.drawTextWithShadow(textRenderer, "Cancel", px + 275, py + 106, 0xFFFF5555);
        ctx.fill(px + 8, py + 118, px + W - 8, py + 119, 0xFF666666);

        // Trades list
        List<ShopData.ShopTrade> trades = shopData.getTrades();
        int start = page * PER_PAGE, end = Math.min(start + PER_PAGE, trades.size());
        if (trades.isEmpty())
            ctx.drawCenteredTextWithShadow(textRenderer, "No offers yet", px + W / 2, py + 150, 0xFFFFFFFF);

        for (int i = start; i < end; i++) {
            ShopData.ShopTrade t = trades.get(i);
            int ry = py + 123 + (i - start) * ROW_HEIGHT;
            
            // Item column (left) - item count shown on the item itself
            drawItemWithCount(ctx, t.sellItem, px + 40, ry + 2);
            String sellName = t.sellItem.getName().getString();
            if (sellName.length() > 12) {
                ctx.drawTextWithShadow(textRenderer, sellName.substring(0, 12), px + 72, ry + 4, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, sellName.substring(12), px + 72, ry + 14, 0xFFFFFFFF);
            } else {
                ctx.drawTextWithShadow(textRenderer, sellName, px + 72, ry + 4, 0xFFFFFFFF);
            }
            
            // Price column (center) - item count shown on the item itself
            drawItemWithCount(ctx, t.buyItem, px + 140, ry + 2);
            String buyName = t.buyItem.getName().getString();
            if (buyName.length() > 12) {
                ctx.drawTextWithShadow(textRenderer, buyName.substring(0, 12), px + 172, ry + 4, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, buyName.substring(12), px + 172, ry + 14, 0xFFFFFFFF);
            } else {
                ctx.drawTextWithShadow(textRenderer, buyName, px + 172, ry + 4, 0xFFFFFFFF);
            }
            // Cancel button (X) is rendered by ButtonWidget at px + 275
        }

        // Page info and offers count - to the right of Close button
        int total = Math.max(1, (int) Math.ceil(trades.size() / (double) PER_PAGE));
        String pageInfo = "Page " + (page+1) + "/" + total + " | Offers: " + trades.size();
        ctx.drawTextWithShadow(textRenderer, pageInfo, px + W / 2 + 10, py + H - 24, 0xFFFFFFFF);

        super.render(ctx, mx, my, delta);
        
        if (!pendingSell.isEmpty() && sellHov) ctx.drawItemTooltip(textRenderer, pendingSell, mx, my);
        if (!pendingBuy.isEmpty() && buyHov) ctx.drawItemTooltip(textRenderer, pendingBuy, mx, my);

        // Tooltips for trade list items (sell & buy columns)
        for (int ti = start; ti < end; ti++) {
            ShopData.ShopTrade t = trades.get(ti);
            int ry2 = py + 123 + (ti - start) * ROW_HEIGHT;
            // Hover over sell item (Item column)
            if (!t.sellItem.isEmpty() && mx >= px + 40 && mx < px + 56 && my >= ry2 + 2 && my < ry2 + 18) {
                ctx.drawItemTooltip(textRenderer, t.sellItem, mx, my);
                break;
            }
            // Hover over buy item (Price column)
            if (!t.buyItem.isEmpty() && mx >= px + 140 && mx < px + 156 && my >= ry2 + 2 && my < ry2 + 18) {
                ctx.drawItemTooltip(textRenderer, t.buyItem, mx, my);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mx = click.x(), my = click.y();
        
        if (mx >= sellSlotX && mx < sellSlotX + SLOT_SIZE && my >= sellSlotY && my < sellSlotY + SLOT_SIZE) {
            // Return pending item to inventory first, then open selection screen
            if (client != null && client.player != null && !pendingSell.isEmpty()) {
                client.player.getInventory().offerOrDrop(pendingSell.copy());
                pendingSell = ItemStack.EMPTY;
                ClientPacketHandler.PendingSellHolder.clear();
            }
            if (client != null) {
                client.setScreen(new InventorySelectScreen(shopName, shopData));
            }
            return true;
        }
        
        if (mx >= buySlotX && mx < buySlotX + SLOT_SIZE && my >= buySlotY && my < buySlotY + SLOT_SIZE) {
            ClientPlayNetworking.send(new ModPackets.ReqPickerPayload(shopName));
            return true;
        }
        
        return super.mouseClicked(click, bl);
    }

    private void submitTrade() {
        if (pendingSell.isEmpty()) { msg("Select item to sell first!"); return; }
        if (pendingBuy.isEmpty())  { msg("Set the price first!"); return; }

        // ✅ دايماً نستخدم الـ NBT packet عشان نحافظ على التطويرات والـ components
        net.minecraft.nbt.NbtCompound sellNbt = ShopData.itemStackToNbt(pendingSell);
        net.minecraft.nbt.NbtCompound buyNbt  = ShopData.itemStackToNbt(pendingBuy);
        ClientPlayNetworking.send(new ModPackets.AddEnchantedBookTradePayload(shopName, sellNbt, buyNbt));

        pendingSell = ItemStack.EMPTY;
        pendingBuy  = ItemStack.EMPTY;
        // Clear the pending holders since trade was submitted
        ClientPacketHandler.PendingSellHolder.clear();
        ClientPacketHandler.PendingBuyHolder.clear();
    }

    private void msg(String t) { if (client != null && client.player != null) client.player.sendMessage(Text.literal(t), false); }

    @Override public boolean shouldPause() { return false; }
}
