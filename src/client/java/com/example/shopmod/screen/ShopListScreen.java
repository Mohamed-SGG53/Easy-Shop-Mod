package com.example.shopmod.screen;

import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.texture.PlayerSkinCache;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;

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

    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_SHOPS = 7;
    private static final int FACE_SIZE = 18;
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
        super(Text.literal("Shops List"));
        this.playerHasShop = payload.playerHasShop();
        this.playerName = payload.playerName();
        this.playerUuid = new UUID(payload.playerUuidMost(), payload.playerUuidLeast());

        for (ModPackets.ShopEntryInfo info : payload.shops()) {
            boolean isOwn = info.ownerName().equals(playerName);
            UUID uuid = (info.uuidMost() != 0 || info.uuidLeast() != 0)
                ? new UUID(info.uuidMost(), info.uuidLeast()) : null;
            shops.add(new ShopEntry(info.ownerName(), uuid, info.offerCount(), isOwn));
        }

        if (shops.isEmpty()) {
            selectedShop = -1;
        } else {
            selectedShop = 0;
        }
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private SkinTextures getSkinTextures(UUID uuid, String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return DefaultSkinHelper.getSteve();

        try {
            com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(uuid, name);
            net.minecraft.component.type.ProfileComponent profileComp =
                net.minecraft.component.type.ProfileComponent.ofStatic(profile);

            PlayerSkinCache skinCache = mc.getPlayerSkinCache();
            if (skinCache != null) {
                // Use synchronous get() - safe on render thread, returns from cache
                PlayerSkinCache.Entry entry = skinCache.get(profileComp);
                if (entry != null) {
                    return entry.getTextures();
                }
            }
        } catch (Exception ignored) {}

        // Fallback to default skin based on UUID
        return DefaultSkinHelper.getSkinTextures(uuid);
    }

    private void drawPlayerFace(DrawContext ctx, UUID uuid, String name, int x, int y) {
        SkinTextures textures = getSkinTextures(uuid, name);
        PlayerSkinDrawer.draw(ctx, textures, x, y, FACE_SIZE);
    }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;

        // Open button
        if (selectedShop >= 0 && selectedShop < getTotalEntries()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Open"), btn -> openSelected())
                .dimensions(px + W / 2 - 85, py + H - 25, 70, 20).build());
        }

        // Close button
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
            .dimensions(px + W / 2 + 15, py + H - 25, 70, 20).build());
    }

    private int getTotalEntries() {
        // If player has no shop, there's a "Create Your Shop" entry at index 0
        if (!playerHasShop) {
            return shops.size() + 1;
        }
        return shops.size();
    }

    private boolean isCreateEntry(int index) {
        return !playerHasShop && index == 0;
    }

    private ShopEntry getShopEntry(int displayIndex) {
        // If player has no shop, display index 0 = "Create Your Shop", real shops start at display index 1
        if (!playerHasShop) {
            int shopIndex = displayIndex - 1;
            if (shopIndex < 0 || shopIndex >= shops.size()) return null;
            return shops.get(shopIndex);
        }
        if (displayIndex < 0 || displayIndex >= shops.size()) return null;
        return shops.get(displayIndex);
    }

    private void openSelected() {
        if (selectedShop < 0 || selectedShop >= getTotalEntries()) return;

        if (isCreateEntry(selectedShop)) {
            // Send create shop packet
            ClientPlayNetworking.send(new ModPackets.CreateShopFromListPayload());
        } else {
            ShopEntry entry = getShopEntry(selectedShop);
            if (entry != null) {
                ClientPlayNetworking.send(new ModPackets.OpenShopFromListPayload(entry.ownerName));
            }
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
        ctx.fill(px, py, px + W, py + 24, 0xFF553311);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Shops List"), px + W / 2, py + 7, 0xFFFFFFFF);

        // List area background
        int listX = px + 8;
        int listY = py + 30;
        int listW = W - 16;
        int listH = VISIBLE_SHOPS * ROW_HEIGHT;
        ctx.fill(listX, listY, listX + listW, listY + listH, 0x88000000);
        drawBorder(ctx, listX, listY, listW, listH, 0xFF666666);

        int totalEntries = getTotalEntries();

        if (totalEntries == 0 && !playerHasShop) {
            // Only shows if there are really no shops at all and player has no shop
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("No shops yet"), px + W / 2, listY + listH / 2 - 4, 0xFFFFFFFF);
        } else {
            int startIdx = scrollOffset;
            int endIdx = Math.min(startIdx + VISIBLE_SHOPS, totalEntries);

            for (int i = startIdx; i < endIdx; i++) {
                int rowY = listY + (i - startIdx) * ROW_HEIGHT;
                int rowX = listX + 2;
                int rowW = listW - 4;

                boolean isSelected = (i == selectedShop);
                boolean hovered = mx >= listX && mx < listX + listW && my >= rowY && my < rowY + ROW_HEIGHT;

                // Highlight
                if (isSelected) {
                    ctx.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, 0x80FFD700);
                } else if (hovered) {
                    ctx.fill(rowX, rowY, rowX + rowW, rowY + ROW_HEIGHT, 0x40FFFFFF);
                }

                if (isCreateEntry(i)) {
                    // "Create Your Shop" entry
                    drawPlayerFace(ctx, playerUuid, playerName, rowX + 3, rowY + 3);

                    // Green text for "Create Your Shop"
                    ctx.drawTextWithShadow(textRenderer, Text.literal("Create Your Shop"),
                        rowX + FACE_SIZE + 8, rowY + 7, 0xFF55FF55);

                    // Small + icon
                    ctx.drawTextWithShadow(textRenderer, Text.literal("+"),
                        rowX + rowW - 16, rowY + 7, 0xFF55FF55);

                } else {
                    ShopEntry entry = getShopEntry(i);
                    if (entry != null) {
                        // Player face
                        drawPlayerFace(ctx, entry.uuid, entry.ownerName, rowX + 3, rowY + 3);

                        // Shop name
                        String shopText = entry.ownerName + "'s Shop";
                        int textColor = entry.isOwnShop ? 0xFF55FFFF : 0xFFFFFFFF;
                        ctx.drawTextWithShadow(textRenderer, Text.literal(shopText),
                            rowX + FACE_SIZE + 8, rowY + 4, textColor);

                        // Offers count
                        String offerText = "(" + entry.offerCount + (entry.offerCount == 1 ? " Offer)" : " Offers)");
                        int offerWidth = textRenderer.getWidth(offerText);
                        ctx.drawTextWithShadow(textRenderer, Text.literal(offerText),
                            rowX + rowW - offerWidth - 6, rowY + 4, 0xFFAAAAAA);
                    }
                }
            }

            // Scroll bar
            if (totalEntries > VISIBLE_SHOPS) {
                int scrollBarX = listX + listW - 5;
                int scrollBarH = listH;
                ctx.fill(scrollBarX, listY, scrollBarX + 3, listY + scrollBarH, 0xFF555555);

                int maxScroll = totalEntries - VISIBLE_SHOPS;
                int thumbH = Math.max(20, scrollBarH * VISIBLE_SHOPS / totalEntries);
                int thumbY = listY + (scrollBarH - thumbH) * scrollOffset / maxScroll;
                ctx.fill(scrollBarX, thumbY, scrollBarX + 3, thumbY + thumbH, 0xFFAAAAAA);
            }
        }

        // Shop count info
        int shopCount = shops.size();
        String infoText = shopCount + (shopCount == 1 ? " Shop" : " Shops");
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(infoText), px + W / 2, py + H - 48, 0xFFAAAAAA);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int listX = px + 8;
        int listY = py + 30;
        double mx = click.x(), my = click.y();
        int button = click.button();

        // Check if clicking on the list
        if (mx >= listX && mx < listX + W - 16 && my >= listY && my < listY + VISIBLE_SHOPS * ROW_HEIGHT) {
            int clickedRow = (int) ((my - listY) / ROW_HEIGHT);
            int clickedIdx = scrollOffset + clickedRow;

            if (clickedIdx >= 0 && clickedIdx < getTotalEntries()) {
                // Double-click detection
                long now = System.currentTimeMillis();
                if (clickedIdx == selectedShop && (now - lastClickTime) < 400 && button == 0) {
                    openSelected();
                    lastClickTime = 0;
                } else {
                    selectedShop = clickedIdx;
                    lastClickTime = now;
                }
                clearChildren();
                init();
                return true;
            }
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, getTotalEntries() - VISIBLE_SHOPS);
        scrollOffset -= (int) verticalAmount;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        if (selectedShop < scrollOffset) selectedShop = scrollOffset;
        if (selectedShop >= scrollOffset + VISIBLE_SHOPS) selectedShop = scrollOffset + VISIBLE_SHOPS - 1;

        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();

        // Enter to open selected shop
        if (keyCode == 257 || keyCode == 335) { // Enter / Numpad Enter
            if (selectedShop >= 0 && selectedShop < getTotalEntries()) {
                openSelected();
                return true;
            }
        }

        // Up/Down arrows
        if (keyCode == 265) { // Up
            if (selectedShop > 0) {
                selectedShop--;
                if (selectedShop < scrollOffset) scrollOffset = selectedShop;
                clearChildren(); init();
                return true;
            }
        }
        if (keyCode == 264) { // Down
            if (selectedShop < getTotalEntries() - 1) {
                selectedShop++;
                if (selectedShop >= scrollOffset + VISIBLE_SHOPS) scrollOffset++;
                clearChildren(); init();
                return true;
            }
        }

        return super.keyPressed(keyInput);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
