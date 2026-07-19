package com.example.shopmod.screen;

import com.example.shopmod.client.ClientPacketHandler;
import com.example.shopmod.data.I18n;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class EnchantedBookAmountScreen extends Screen {

    private final String shopName;
    private final ItemStack bookTemplate;
    private EditBox amountField;
    private static final int W=240, H=160;
    private static final int MAX_AMOUNT = 64;

    public EnchantedBookAmountScreen(String shopName, ItemStack bookTemplate) {
        super(Component.literal(I18n.get("shop.set_price") + " - " + shopName));
        this.shopName = shopName;
        this.bookTemplate = bookTemplate.copy();
    }

    private static void drawBorder(GuiGraphicsExtractor ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,y,x+w,y+1,color); ctx.fill(x,y+h-1,x+w,y+h,color);
        ctx.fill(x,y,x+1,y+h,color); ctx.fill(x+w-1,y,x+w,y+h,color);
    }

    @Override
    protected void init() {
        int px=(width-W)/2, py=(height-H)/2;
        amountField=new EditBox(font, px+W/2-40, py+84, 80, 20, Component.literal("1"));
        amountField.setValue("1"); amountField.setMaxLength(4);
        addRenderableWidget(amountField);
        int[][] qp={{1,0},{16,1},{32,2},{64,3}};
        for (int[] q:qp) { final int qty=q[0], slot=q[1];
            addRenderableWidget(Button.builder(Component.literal("x"+qty), btn->amountField.setValue(""+qty)).bounds(px+14+slot*52,py+108,48,18).build()); }
        addRenderableWidget(Button.builder(Component.literal(I18n.get("amount.confirm")), btn->confirm()).bounds(px+W/2-60,py+H-30,120,20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);
        int px=(width-W)/2, py=(height-H)/2;
        ctx.fill(px,py,px+W,py+H,0xE0100800);
        drawBorder(ctx,px,py,W,H,0xFF8B6914);
        ctx.fill(px,py,px+W,py+20,0xFF3D1F00);
        ctx.centeredText(font, Component.literal(I18n.get("shop.set_price") + " - " + shopName), px+W/2, py+6, 0xFFFFFFFF);

        // Draw enchanted book icon
        ctx.fill(px+W/2-12,py+30,px+W/2+12,py+54,0xFF444444);
        drawBorder(ctx,px+W/2-12,py+30,24,24,0xFFAAAAAA);
        ctx.item(bookTemplate, px+W/2-8, py+34);

        // Draw book name
        ctx.centeredText(font, bookTemplate.getHoverName(), px+W/2, py+60, 0xFFFFFFFF);
        ctx.centeredText(font, Component.literal(I18n.get("amount.enter")), px+W/2, py+72, 0xAAAAAA);

        super.extractRenderState(ctx, mx, my, delta);
    }

    private void confirm() {
        int amount;
        try { amount=Integer.parseInt(amountField.getValue().trim()); }
        catch (NumberFormatException e) { amount=1; }
        amount=Math.max(1, Math.min(amount, MAX_AMOUNT));
        ItemStack book = bookTemplate.copy();
        book.setCount(amount);
        ClientPacketHandler.PendingBuyHolder.buyItem = book;
        ClientPacketHandler.PendingBuyHolder.shopName = shopName;
        ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName));
        onClose();
    }

    @Override public boolean isPauseScreen() { return false; }
}