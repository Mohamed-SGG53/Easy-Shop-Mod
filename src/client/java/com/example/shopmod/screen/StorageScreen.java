package com.example.shopmod.screen;

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
public class StorageScreen extends Screen {

    private final String shopName;
    private ShopData shopData;
    private int page = 0;
    private static final int COLS = 9, ROWS = 5, PER_PAGE = COLS * ROWS;
    private static final int W = COLS * 20 + 30, H = ROWS * 20 + 100;

    public StorageScreen(String shopName, ShopData data) {
        super(Text.literal("Storage - " + shopName));
        this.shopName = shopName;
        this.shopData = data;
    }
    
    public void refreshData(ShopData data) {
        this.shopData = data;
        clearChildren();
        init();
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    protected void init() {
        int px = (width - W) / 2, py = (height - H) / 2;
        
        int total = Math.max(1, (int) Math.ceil(shopData.getStorage().size() / (double) PER_PAGE));
        if (page > 0)
            addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn -> { page--; clearChildren(); init(); })
                .dimensions(px + 10, py + H - 28, 40, 20).build());
        if (page < total - 1)
            addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn -> { page++; clearChildren(); init(); })
                .dimensions(px + W - 50, py + H - 28, 40, 20).build());

        // Back button - returns to ShopOwnerScreen
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> {
            ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName));
        }).dimensions(px + W / 2 - 85, py + H - 28, 70, 20).build());

        // Close button
        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
            .dimensions(px + W / 2 + 15, py + H - 28, 70, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px = (width - W) / 2, py = (height - H) / 2;

        ctx.fill(px, py, px + W, py + H, 0xE0100800);
        drawBorder(ctx, px, py, W, H, 0xFF8B6914);
        ctx.fill(px, py, px + W, py + 20, 0xFF3D1F00);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Storage - " + shopName), px + W / 2, py + 6, 0xFFFFFFFF);

        List<ItemStack> storage = shopData.getStorage();
        int gridX = px + 10, gridY = py + 28;
        ItemStack hovered = ItemStack.EMPTY;
        int startIdx = page * PER_PAGE;

        for (int i = 0; i < PER_PAGE; i++) {
            int idx = startIdx + i;
            if (idx >= storage.size()) break;
            ItemStack s = storage.get(idx);
            int col = i % COLS, row = i / COLS, sx = gridX + col * 20, sy = gridY + row * 20;
            boolean hov = mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18;
            ctx.fill(sx, sy, sx + 18, sy + 18, hov ? 0xAA8B6914 : 0x88333333);
            drawBorder(ctx, sx, sy, 18, 18, 0xFF555555);
            ctx.drawItem(s, sx + 1, sy + 1);
            if (s.getCount() > 1) {
                String countStr = String.valueOf(s.getCount());
                int w = textRenderer.getWidth(countStr);
                ctx.drawTextWithShadow(textRenderer, Text.literal(countStr), sx + 19 - w, sy + 11, 0xFFFFFFFF);
            }
            if (hov) hovered = s;
        }

        int total = Math.max(1, (int) Math.ceil(storage.size() / (double) PER_PAGE));
        ctx.drawCenteredTextWithShadow(textRenderer, 
            Text.literal("Page " + (page + 1) + "/" + total + "  |  " + storage.size() + " items"), 
            px + W / 2, py + H - 42, 0xFFFFFFFF);

        super.render(ctx, mx, my, delta);
        if (!hovered.isEmpty()) ctx.drawItemTooltip(textRenderer, hovered, mx, my);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int px = (width - W) / 2, py = (height - H) / 2, gridX = px + 10, gridY = py + 28;
        int startIdx = page * PER_PAGE;
        double mx = click.x(), my = click.y();
        
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = startIdx + i;
            if (idx >= shopData.getStorage().size()) break;
            int col = i % COLS, row = i / COLS, sx = gridX + col * 20, sy = gridY + row * 20;
            if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                ClientPlayNetworking.send(new ModPackets.TakeStoragePayload(shopName, idx));
                return true;
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override public boolean shouldPause() { return false; }
}
