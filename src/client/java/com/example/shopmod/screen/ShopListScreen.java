package com.example.shopmod.screen;

import com.example.shopmod.client.SkinHelper;
import com.example.shopmod.data.I18n;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class ShopListScreen extends Screen {

    private final List<ShopEntry> shops = new ArrayList<>();
    private boolean playerHasShop;
    private final String playerName;
    private final UUID playerUuid;
    private int selectedShop = -1;
    private int scrollOffset = 0;
    private long lastClickTime = 0;

    private static final int ROW_HEIGHT = 24, VISIBLE_SHOPS = 7, FACE_SIZE = 18;
    private static final int W = 300, H = 230;

    private static class ShopEntry {
        final String ownerName;
        final UUID uuid;
        final int offerCount;
        final boolean isOwnShop;

        ShopEntry(String ownerName, UUID uuid, int offerCount, boolean isOwnShop) {
            this.ownerName = ownerName;
            this.uuid = uuid;
            this.offerCount = offerCount;
            this.isOwnShop = isOwnShop;
        }
    }

    public ShopListScreen(ModPackets.OpenShopsListPayload payload) {
        super(Component.literal(I18n.get("shoplist.title")));
        this.playerHasShop = payload.playerHasShop();
        this.playerName = payload.playerName();
        this.playerUuid = new UUID(payload.playerUuidMost(), payload.playerUuidLeast());
        for (ModPackets.ShopEntryInfo info : payload.shops()) {
            boolean isOwn = info.ownerName().equals(playerName);
            UUID uuid = (info.uuidMost() != 0 || info.uuidLeast() != 0) ? new UUID(info.uuidMost(), info.uuidLeast()) : null;
            shops.add(new ShopEntry(info.ownerName(), uuid, info.offerCount(), isOwn));
        }
        selectedShop = shops.isEmpty() ? -1 : 0;
    }

    private static void drawBorder(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawPlayerAvatar(GuiGraphicsExtractor ctx, UUID uuid, String name, int x, int y) {
        SkinHelper.drawPlayerFace(ctx, uuid, x, y, FACE_SIZE);
    }

    // ==================== Screen Methods ====================

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        if (selectedShop >= 0 && selectedShop < getTotalEntries())
            addRenderableWidget(Button.builder(Component.literal(I18n.get("shoplist.open")), btn -> openSelected())
                .bounds(px + W / 2 - 85, py + H - 25, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.get("shoplist.close")), btn -> onClose())
            .bounds(px + W / 2 + 15, py + H - 25, 70, 20).build());
    }

    private int getTotalEntries() { return playerHasShop ? shops.size() : shops.size() + 1; }
    private boolean isCreateEntry(int index) { return !playerHasShop && index == 0; }
    private ShopEntry getShopEntry(int displayIndex) {
        int idx = playerHasShop ? displayIndex : displayIndex - 1;
        return (idx < 0 || idx >= shops.size()) ? null : shops.get(idx);
    }

    private void openSelected() {
        if (selectedShop < 0 || selectedShop >= getTotalEntries()) return;
        if (isCreateEntry(selectedShop)) ClientPlayNetworking.send(new ModPackets.CreateShopFromListPayload());
        else { ShopEntry e = getShopEntry(selectedShop); if (e != null) ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(e.ownerName, 0)); }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);
        int px = (width - W) / 2, py = (height - H) / 2;
        ctx.fill(px, py, px + W, py + H, 0xCC000000);
        drawBorder(ctx, px, py, W, H, 0xFF8B4513);
        ctx.fill(px, py, px + W, py + 24, 0xFF553311);
        ctx.centeredText(font, Component.literal(I18n.get("shoplist.title")), px + W / 2, py + 7, 0xFFFFFFFF);

        int listX = px + 8, listY = py + 30, listW = W - 16, listH = VISIBLE_SHOPS * ROW_HEIGHT;
        ctx.fill(listX, listY, listX + listW, listY + listH, 0x88000000);
        drawBorder(ctx, listX, listY, listW, listH, 0xFF666666);

        int totalEntries = getTotalEntries();
        if (totalEntries == 0 && !playerHasShop) {
            ctx.centeredText(font, Component.literal(I18n.get("shoplist.empty")), px + W / 2, listY + listH / 2 - 4, 0xFFFFFFFF);
        } else {
            int endIdx = Math.min(scrollOffset + VISIBLE_SHOPS, totalEntries);
            for (int i = scrollOffset; i < endIdx; i++) {
                int rowY = listY + (i - scrollOffset) * ROW_HEIGHT, rowX = listX + 2, rowW = listW - 4;
                boolean isSelected = (i == selectedShop);
                boolean hovered = mx >= listX && mx < listX + listW && my >= rowY && my < rowY + ROW_HEIGHT;
                if (isSelected) ctx.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, 0x80FFD700);
                else if (hovered) ctx.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, 0x40FFFFFF);

                int avatarY = rowY + (ROW_HEIGHT - FACE_SIZE) / 2;
                int textX = rowX + FACE_SIZE + 5;
                int textY = rowY + 7;

                if (isCreateEntry(i)) {
                    drawPlayerAvatar(ctx, playerUuid, playerName, rowX + 2, avatarY);
                    ctx.text(font, Component.literal(I18n.get("shoplist.create")), textX, textY, 0xFF55FF55);
                    ctx.text(font, Component.literal("+"), rowX + rowW - 16, textY, 0xFF55FF55);
                } else {
                    ShopEntry entry = getShopEntry(i);
                    if (entry != null) {
                        drawPlayerAvatar(ctx, entry.uuid, entry.ownerName, rowX + 2, avatarY);
                        ctx.text(font, Component.literal(I18n.get("shoplist.shop_of", entry.ownerName)), textX, textY, entry.isOwnShop ? 0xFF55FFFF : 0xFFFFFFFF);
                        String offerComp = entry.offerCount == 1
                            ? I18n.get("shoplist.offer_single", entry.offerCount)
                            : I18n.get("shoplist.offer_plural", entry.offerCount);
                        int offerWidth = font.width(offerComp);
                        ctx.text(font, Component.literal(offerComp), rowX + rowW - offerWidth - 6, rowY + 4, 0xFFAAAAAA);
                    }
                }
            }
            if (totalEntries > VISIBLE_SHOPS) {
                int maxScroll = totalEntries - VISIBLE_SHOPS;
                int thumbH = Math.max(20, listH * VISIBLE_SHOPS / totalEntries);
                int thumbY = listY + (listH - thumbH) * scrollOffset / maxScroll;
                ctx.fill(listX + listW - 5, listY, listX + listW - 2, listY + listH, 0xFF555555);
                ctx.fill(listX + listW - 5, thumbY, listX + listW - 2, thumbY + thumbH, 0xFFAAAAAA);
            }
        }
        ctx.centeredText(font, Component.literal(I18n.get("shoplist.count", shops.size())), px + W / 2, py + H - 48, 0xFFAAAAAA);
        super.extractRenderState(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int listX = px + 8, listY = py + 30;
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (mx >= listX && mx < listX + W - 16 && my >= listY && my < listY + VISIBLE_SHOPS * ROW_HEIGHT) {
            int clickedIdx = scrollOffset + (int) ((my - listY) / ROW_HEIGHT);
            if (clickedIdx >= 0 && clickedIdx < getTotalEntries()) {
                long now = System.currentTimeMillis();
                if (clickedIdx == selectedShop && (now - lastClickTime) < 400 && button == 0) { openSelected(); lastClickTime = 0; }
                else { selectedShop = clickedIdx; lastClickTime = now; }
                this.children().clear(); init(); return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, getTotalEntries() - VISIBLE_SHOPS);
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int) verticalAmount, maxScroll));
        if (selectedShop < scrollOffset) selectedShop = scrollOffset;
        if (selectedShop >= scrollOffset + VISIBLE_SHOPS) selectedShop = scrollOffset + VISIBLE_SHOPS - 1;
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
