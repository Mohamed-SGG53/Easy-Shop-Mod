package com.example.shopmod.screen;

import com.example.shopmod.client.ClientPacketHandler;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class EnchantedBookAmountScreen extends Screen {

    private final String shopName;
    private final ItemStack bookTemplate;
    private static final int W=240, H=120;

    public EnchantedBookAmountScreen(String shopName, ItemStack bookTemplate) {
        super(Component.literal("Set Price - " + shopName));
        this.shopName = shopName;
        this.bookTemplate = bookTemplate.copy();
    }

    private static void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,y,x+w,y+1,color); ctx.fill(x,y+h-1,x+w,y+h,color);
        ctx.fill(x,y,x+1,y+h,color); ctx.fill(x+w-1,y,x+w,y+h,color);
    }

    @Override
    protected void init() {
        int px=(width-W)/2, py=(height-H)/2;
        addRenderableWidget(Button.builder(Component.literal("Confirm (x1)"), btn->confirm()).bounds(px+W/2-60,py+H-30,120,20).build());
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px=(width-W)/2, py=(height-H)/2;
        ctx.fill(px,py,px+W,py+H,0xE0100800);
        drawBorder(ctx,px,py,W,H,0xFF8B6914);
        ctx.fill(px,py,px+W,py+20,0xFF3D1F00);
        ctx.drawCenteredString(font, Component.literal("Set Price - " + shopName), px+W/2, py+6, 0xFFFFFFFF);

        // Draw enchanted book icon
        ctx.fill(px+W/2-12,py+28,px+W/2+12,py+52,0xFF444444);
        drawBorder(ctx,px+W/2-12,py+28,24,24,0xFFAAAAAA);
        ctx.renderItem(bookTemplate, px+W/2-8, py+32);

        // Draw book name
        ctx.drawCenteredString(font, bookTemplate.getHoverName(), px+W/2, py+58, 0xFFFFFFFF);
        ctx.drawCenteredString(font, Component.literal("Quantity: x1"), px+W/2, py+72, 0xAAAAAA);

        super.render(ctx, mx, my, delta);
    }

    private void confirm() {
        // Enchanted books and weapons always x1 as price
        ItemStack book = bookTemplate.copy();
        book.setCount(1);
        ClientPacketHandler.PendingBuyHolder.buyItem = book;
        ClientPacketHandler.PendingBuyHolder.shopName = shopName;
        ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName));
        onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
}
