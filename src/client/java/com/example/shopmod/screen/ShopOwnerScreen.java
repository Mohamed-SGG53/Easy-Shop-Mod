package com.example.shopmod.screen;

import com.example.shopmod.client.ClientPacketHandler;
import com.example.shopmod.client.ClientPacketHandler.ShopNavigationHolder;
import com.example.shopmod.data.I18n;
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
import net.minecraft.network.chat.Style;

import java.util.List;

@Environment(EnvType.CLIENT)
public class ShopOwnerScreen extends Screen {

    private final String shopName;
    private ShopData shopData;
    private ItemStack pendingSell = ItemStack.EMPTY, pendingBuy = ItemStack.EMPTY;
    private int page = 0;
    private boolean shopMoveEnabled = false;
    private static final int PER_PAGE = 4, ROW_HEIGHT = 28, SLOT_SIZE = 28;
    private static final int W = 380, H = 270;
    private int sellSlotX, sellSlotY, buySlotX, buySlotY;

    public ShopOwnerScreen(String shopName, ShopData data) {
        super(Component.literal(I18n.get("shop.title", shopName)));
        this.shopName = shopName; this.shopData = data;
        this.shopMoveEnabled = data.isShopMoveEnabled();
    }

    public void rebuild() { clearWidgets(); init(); }
    public void refreshData(ShopData data) {
        this.shopData = data;
        this.shopMoveEnabled = data.isShopMoveEnabled();
        clearWidgets(); init();
    }
    public void setPendingBuyItem(ItemStack s) { this.pendingBuy = s; }
    public void setPendingSellItem(ItemStack s) { this.pendingSell = s; }

    private static void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color); ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color); ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        int slotCenterX = px + 8 + (242 - SLOT_SIZE) / 2;
        sellSlotX = slotCenterX; sellSlotY = py + 30;
        buySlotX = slotCenterX; buySlotY = py + 62;

        addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.storage")),
            btn -> ClientPlayNetworking.send(new ModPackets.ReqStoragePayload(shopName))).bounds(px + 260, py + 28, 90, 24).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.add_offer")), btn -> submitTrade()).bounds(px + 260, py + 60, 90, 24).build());

        // Shop Move toggle button with colored On (green) / Off (red)
        Component moveLabel = Component.literal(I18n.get("shop.move_label"))
            .append(Component.literal(shopMoveEnabled ? I18n.get("shop.move_on") : I18n.get("shop.move_off"))
                .setStyle(Style.EMPTY.withColor(shopMoveEnabled ? 0x55FF55 : 0xFF5555).withBold(true)));
        addRenderableWidget(Button.builder(moveLabel, btn -> {
            shopMoveEnabled = !shopMoveEnabled;
            ClientPlayNetworking.send(new ModPackets.ToggleShopMovePayload(shopName, shopMoveEnabled));
            clearWidgets(); init();
        }).bounds(px + 160, py + 44, 90, 24).build());

        // Delete buttons for trades
        List<ShopData.ShopTrade> trades = shopData.getTrades();
        int start = page * PER_PAGE, end = Math.min(start + PER_PAGE, trades.size());
        for (int i = start; i < end; i++) {
            final int idx = i, row = i - start;
            addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.cancel_btn")),
                btn -> ClientPlayNetworking.send(new ModPackets.RemoveTradePayload(shopName, idx))).bounds(px + 278, py + 120 + row * ROW_HEIGHT, 70, 20).build());
        }

        int total = Math.max(1, (int) Math.ceil(trades.size() / (double) PER_PAGE));
        if (page > 0) addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.prev")), btn -> { page--; clearWidgets(); init(); }).bounds(px + 10, py + H - 28, 40, 20).build());
        if (page < total - 1) addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.next")), btn -> { page++; clearWidgets(); init(); }).bounds(px + W - 50, py + H - 28, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.close")), btn -> onClose()).bounds(px + W / 2 - 75, py + H - 28, 80, 20).build());

        if (!ShopNavigationHolder.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("\u25C0"), btn -> {
                ClientPacketHandler.PendingSellHolder.clear(); ClientPacketHandler.PendingBuyHolder.clear();
                String prev = ShopNavigationHolder.getPrevShop(shopName);
                if (prev != null) ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(prev));
            }).bounds(px - 26, py + H / 2 - 12, 22, 24).build());
            addRenderableWidget(Button.builder(Component.literal("\u25B6"), btn -> {
                ClientPacketHandler.PendingSellHolder.clear(); ClientPacketHandler.PendingBuyHolder.clear();
                String next = ShopNavigationHolder.getNextShop(shopName);
                if (next != null) ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(next));
            }).bounds(px + W + 4, py + H / 2 - 12, 22, 24).build());
        }
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
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px = (width - W) / 2, py = (height - H) / 2;
        ctx.fill(px, py, px + W, py + H, 0xCC000000);
        drawBorder(ctx, px, py, W, H, 0xFF8B4513);
        ctx.fill(px, py, px + W, py + 22, 0xFF553311);
        ctx.drawCenteredString(font, title, px + W / 2, py + 7, 0xFFFFFFFF);

        ctx.fill(px + 8, py + 24, px + W - 8, py + 96, 0x88000000);
        drawBorder(ctx, px + 8, py + 24, W - 16, 72, 0xFF666666);

        ctx.drawString(font, Component.literal(I18n.get("shop.add_buyable")), px + 14, py + 38, 0xFF55FF55);
        boolean sellHov = mx >= sellSlotX && mx < sellSlotX + SLOT_SIZE && my >= sellSlotY && my < sellSlotY + SLOT_SIZE;
        ctx.fill(sellSlotX, sellSlotY, sellSlotX + SLOT_SIZE, sellSlotY + SLOT_SIZE, sellHov ? 0xFF553300 : 0xFF333333);
        drawBorder(ctx, sellSlotX, sellSlotY, SLOT_SIZE, SLOT_SIZE, pendingSell.isEmpty() ? 0xFF888888 : 0xFF00FF00);
        if (!pendingSell.isEmpty()) {
            drawItemWithCount(ctx, pendingSell, sellSlotX + 6, sellSlotY + 6);
        }

        ctx.drawString(font, Component.literal(I18n.get("shop.item_price")), px + 14, py + 70, 0xFFFFFF00);
        boolean buyHov = mx >= buySlotX && mx < buySlotX + SLOT_SIZE && my >= buySlotY && my < buySlotY + SLOT_SIZE;
        ctx.fill(buySlotX, buySlotY, buySlotX + SLOT_SIZE, buySlotY + SLOT_SIZE, buyHov ? 0xFF553300 : 0xFF333333);
        drawBorder(ctx, buySlotX, buySlotY, SLOT_SIZE, SLOT_SIZE, pendingBuy.isEmpty() ? 0xFF888888 : 0xFFFFFF00);
        if (!pendingBuy.isEmpty()) {
            drawItemWithCount(ctx, pendingBuy, buySlotX + 6, buySlotY + 6);
        }

        ctx.fill(px + 8, py + 100, px + W - 8, py + H - 36, 0x66000000);
        drawBorder(ctx, px + 8, py + 100, W - 16, H - 136, 0xFF666666);
        ctx.drawString(font, Component.literal(I18n.get("shop.item_header")), px + 14, py + 106, 0xFF55FF55);
        ctx.drawString(font, Component.literal(I18n.get("shop.price_header")), px + 140, py + 106, 0xFFFFFF00);
        ctx.drawString(font, Component.literal(I18n.get("shop.cancel_header")), px + 290, py + 106, 0xFFFF5555);
        ctx.fill(px + 8, py + 118, px + W - 8, py + 119, 0xFF666666);
        ctx.fill(px + 125, py + 100, px + 126, py + H - 36, 0xFF555555);
        ctx.fill(px + 255, py + 100, px + 256, py + H - 36, 0xFF555555);

        List<ShopData.ShopTrade> trades = shopData.getTrades();
        int start = page * PER_PAGE, end = Math.min(start + PER_PAGE, trades.size());
        if (trades.isEmpty()) ctx.drawCenteredString(font, Component.literal(I18n.get("shop.no_offers")), px + W / 2, py + 150, 0xFFFFFFFF);

        for (int i = start; i < end; i++) {
            ShopData.ShopTrade t = trades.get(i);
            int ry = py + 123 + (i - start) * ROW_HEIGHT;
            drawItemWithCount(ctx, t.sellItem, px + 14, ry + 2);
            String sn = t.sellItem.getHoverName().getString();
            if (sn.length() > 14) { ctx.drawString(font, Component.literal(sn.substring(0, 14)), px + 38, ry + 4, 0xFFFFFFFF); ctx.drawString(font, Component.literal(sn.substring(14)), px + 38, ry + 14, 0xFFFFFFFF); }
            else ctx.drawString(font, Component.literal(sn), px + 38, ry + 4, 0xFFFFFFFF);
            drawItemWithCount(ctx, t.buyItem, px + 140, ry + 2);
            String bn = t.buyItem.getHoverName().getString();
            if (bn.length() > 14) { ctx.drawString(font, Component.literal(bn.substring(0, 14)), px + 164, ry + 4, 0xFFFFFFFF); ctx.drawString(font, Component.literal(bn.substring(14)), px + 164, ry + 14, 0xFFFFFFFF); }
            else ctx.drawString(font, Component.literal(bn), px + 164, ry + 4, 0xFFFFFFFF);
        }

        int total = Math.max(1, (int) Math.ceil(trades.size() / (double) PER_PAGE));
        ctx.drawString(font, Component.literal(I18n.get("shop.page_offers", page + 1, total, trades.size())), px + W / 2 + 10, py + H - 24, 0xFFFFFFFF);
        super.render(ctx, mx, my, delta);

        if (!pendingSell.isEmpty() && sellHov) {
            ctx.setTooltipForNextFrame(font, pendingSell, mx, my);
        }
        if (!pendingBuy.isEmpty() && buyHov) {
            ctx.setTooltipForNextFrame(font, pendingBuy, mx, my);
        }

        for (int ti = start; ti < end; ti++) {
            ShopData.ShopTrade t = trades.get(ti);
            int ry2 = py + 123 + (ti - start) * ROW_HEIGHT;
            if (!t.sellItem.isEmpty() && mx >= px + 14 && mx < px + 56 && my >= ry2 + 2 && my < ry2 + 18) {
                ctx.setTooltipForNextFrame(font, t.sellItem, mx, my); break;
            }
            if (!t.buyItem.isEmpty() && mx >= px + 140 && mx < px + 156 && my >= ry2 + 2 && my < ry2 + 18) {
                ctx.setTooltipForNextFrame(font, t.buyItem, mx, my); break;
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        if (mx >= sellSlotX && mx < sellSlotX + SLOT_SIZE && my >= sellSlotY && my < sellSlotY + SLOT_SIZE) {
            if (this.minecraft != null && this.minecraft.player != null && !pendingSell.isEmpty()) {
                this.minecraft.player.getInventory().add(pendingSell.copy());
                pendingSell = ItemStack.EMPTY; ClientPacketHandler.PendingSellHolder.clear();
            }
            if (this.minecraft != null) this.minecraft.setScreen(new InventorySelectScreen(shopName, shopData));
            return true;
        }
        if (mx >= buySlotX && mx < buySlotX + SLOT_SIZE && my >= buySlotY && my < buySlotY + SLOT_SIZE) {
            ClientPlayNetworking.send(new ModPackets.ReqPickerPayload(shopName)); return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void submitTrade() {
        if (pendingSell.isEmpty()) { msg(I18n.get("msg.select_item")); return; }
        if (pendingBuy.isEmpty())  { msg(I18n.get("msg.select_price")); return; }
        ClientPlayNetworking.send(new ModPackets.AddEnchantedBookTradePayload(shopName, ShopData.itemStackToNbt(pendingSell), ShopData.itemStackToNbt(pendingBuy)));
        pendingSell = ItemStack.EMPTY; pendingBuy = ItemStack.EMPTY;
        ClientPacketHandler.PendingSellHolder.clear(); ClientPacketHandler.PendingBuyHolder.clear();
    }

    private void msg(String t) { if (this.minecraft != null && this.minecraft.player != null) this.minecraft.player.displayClientMessage(Component.literal(t), false); }

    @Override public boolean isPauseScreen() { return false; }
}