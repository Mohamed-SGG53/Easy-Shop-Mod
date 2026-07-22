package com.example.shopmod.screen;

import com.example.shopmod.data.I18n;
import com.example.shopmod.data.RecoveryData;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class RecoveryScreen extends Screen {

    private final List<ItemStack> items = new ArrayList<>();
    private int page = 0;
    private static final int COLS = 9, ROWS = 5, PER_PAGE = COLS * ROWS;
    private static final int W = COLS * 20 + 30, H = ROWS * 20 + 100;

    public RecoveryScreen() {
        super(Component.literal(I18n.get("recovery.title")));
    }

    public void setItems(List<RecoveryData.ItemStackWithSource> recoveryItems) {
        items.clear();
        for (RecoveryData.ItemStackWithSource item : recoveryItems) {
            if (item != null && !item.stack().isEmpty()) {
                items.add(item.stack());
            }
        }
        page = 0;
        clearWidgets();
        init();
    }

    private static void drawBorder(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color); ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color); ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    protected void init() {
        super.init(); // Let parent add vanilla widgets (close button etc.)
        clearWidgets(); // Remove all vanilla widgets
        int px = (width - W) / 2, py = (height - H) / 2;
        int total = Math.max(1, (int) Math.ceil(items.size() / (double) PER_PAGE));
        if (page > 0) addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.prev")), btn -> { page--; clearWidgets(); init(); }).bounds(px + 10, py + H - 28, 40, 20).build());
        if (page < total - 1) addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.next")), btn -> { page++; clearWidgets(); init(); }).bounds(px + W - 50, py + H - 28, 40, 20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.close")), btn -> onClose()).bounds(px + W / 2 - 35, py + H - 28, 70, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);
        int px = (width - W) / 2, py = (height - H) / 2;
        ctx.fill(px, py, px + W, py + H, 0xE0100800);
        drawBorder(ctx, px, py, W, H, 0xFF8B6914);
        ctx.fill(px, py, px + W, py + 20, 0xFF3D1F00);
        ctx.centeredText(font, Component.literal(I18n.get("recovery.title")), px + W / 2, py + 6, 0xFFFFFFFF);

        List<ItemStack> storage = items;
        int gridX = px + 10, gridY = py + 28;
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
        }
        int total = Math.max(1, (int) Math.ceil(storage.size() / (double) PER_PAGE));
        ctx.centeredText(font, Component.literal(I18n.get("storage.page", page + 1, total, storage.size())), px + W / 2, py + H - 42, 0xFFFFFFFF);

        super.extractRenderState(ctx, mx, my, delta);

        // Proper tooltip rendering
        if (!storage.isEmpty()) {
            int si = page * PER_PAGE;
            for (int i = 0; i < PER_PAGE; i++) {
                int idx = si + i; if (idx >= storage.size()) break;
                int col = i % COLS, row = i / COLS, sx = gridX + col * 20, sy = gridY + row * 20;
                if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                    ctx.setTooltipForNextFrame(font, storage.get(idx), mx, my);
                    break;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int px = (width - W) / 2, py = (height - H) / 2, gridX = px + 10, gridY = py + 28;
        double mx = event.x(), my = event.y();
        int startIdx = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = startIdx + i; if (idx >= items.size()) break;
            int col = i % COLS, row = i / COLS, sx = gridX + col * 20, sy = gridY + row * 20;
            if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                ClientPlayNetworking.send(new ModPackets.ClaimRecoveryItemPayload(idx));
                items.remove(idx);
                int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) PER_PAGE));
                if (page >= totalPages) page = totalPages - 1;
                if (items.isEmpty()) {
                    onClose();
                } else {
                    clearWidgets();
                    init();
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean isPauseScreen() { return false; }
}