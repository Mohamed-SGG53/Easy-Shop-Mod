package com.example.shopmod.screen;

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
public class StorageScreen extends Screen {

    private final String shopName;
    private ShopData shopData;
    private int page = 0;
    private static final int COLS = 9, ROWS = 5, PER_PAGE = COLS * ROWS;
    private static final int W = COLS * 20 + 30, H = ROWS * 20 + 100;

    public StorageScreen(String shopName, ShopData data) {
        super(Component.literal(I18n.get("storage.title", shopName)));
        this.shopName = shopName;
        this.shopData = data;
    }

    public void refreshData(ShopData data) { this.shopData = data; clearWidgets(); init(); }

    private static void drawBorder(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color); ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color); ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        int total = Math.max(1, (int) Math.ceil(shopData.getStorage().size() / (double) PER_PAGE));
        if (page > 0) addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.prev")), btn -> { page--; clearWidgets(); init(); }).bounds(px + 10, py + H - 28, 40, 20).build());
        if (page < total - 1) addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.next")), btn -> { page++; clearWidgets(); init(); }).bounds(px + W - 50, py + H - 28, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.get("picker.back")), btn -> ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName))).bounds(px + W / 2 - 85, py + H - 28, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.close")), btn -> onClose()).bounds(px + W / 2 + 15, py + H - 28, 70, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);
        int px = (width - W) / 2, py = (height - H) / 2;
        ctx.fill(px, py, px + W, py + H, 0xE0100800);
        drawBorder(ctx, px, py, W, H, 0xFF8B6914);
        ctx.fill(px, py, px + W, py + 20, 0xFF3D1F00);
        ctx.centeredText(font, Component.literal(I18n.get("storage.title", shopName)), px + W / 2, py + 6, 0xFFFFFFFF);

        List<ItemStack> storage = shopData.getStorage();
        int gridX = px + 10, gridY = py + 28;
        ItemStack hovered = ItemStack.EMPTY;
        int startIdx = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = startIdx + i; if (idx >= storage.size()) break;
            ItemStack s = storage.get(idx);
            int col = i % COLS, row = i / COLS, sx = gridX + col * 20, sy = gridY + row * 20;
            boolean hov = mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18;
            ctx.fill(sx, sy, sx + 18, sy + 18, hov ? 0xAA8B6914 : 0x88333333);
            drawBorder(ctx, sx, sy, 18, 18, 0xFF555555);
            ctx.item(s, sx + 1, sy + 1);
            if (s.getCount() > 1) {
                int w = font.width(String.valueOf(s.getCount()));
                ctx.text(font, Component.literal(String.valueOf(s.getCount())), sx + 19 - w, sy + 11, 0xFFFFFFFF);
            }
            if (hov) hovered = s;
        }
        int total = Math.max(1, (int) Math.ceil(storage.size() / (double) PER_PAGE));
        ctx.centeredText(font, Component.literal(I18n.get("storage.page", page + 1, total, storage.size())), px + W / 2, py + H - 42, 0xFFFFFFFF);
        super.extractRenderState(ctx, mx, my, delta);
        // Proper tooltip rendering
        if (!hovered.isEmpty()) {
            ctx.setTooltipForNextFrame(font, hovered, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int px = (width - W) / 2, py = (height - H) / 2, gridX = px + 10, gridY = py + 28;
        double mx = event.x(), my = event.y();
        int startIdx = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = startIdx + i; if (idx >= shopData.getStorage().size()) break;
            int col = i % COLS, row = i / COLS, sx = gridX + col * 20, sy = gridY + row * 20;
            if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                ClientPlayNetworking.send(new ModPackets.TakeStoragePayload(shopName, idx)); return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean isPauseScreen() { return false; }
}