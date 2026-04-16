package com.example.shopmod.screen;

import com.example.shopmod.client.ClientPacketHandler;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class AmountInputScreen extends Screen {

    private final String shopName;
    private final Item item;
    private final ItemStack displayStack;
    private TextFieldWidget amountField;
    private static final int W=240, H=160;

    public AmountInputScreen(String shopName, String itemId) {
        super(Text.literal("Set Quantity - " + shopName));
        this.shopName = shopName;
        this.item     = Registries.ITEM.get(Identifier.of(itemId));
        this.displayStack = new ItemStack(item);
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,y,x+w,y+1,color); ctx.fill(x,y+h-1,x+w,y+h,color);
        ctx.fill(x,y,x+1,y+h,color); ctx.fill(x+w-1,y,x+w,y+h,color);
    }

    @Override
    protected void init() {
        int px=(width-W)/2, py=(height-H)/2;
        amountField=new TextFieldWidget(textRenderer, px+W/2-40, py+84, 80, 20, Text.literal("1"));
        amountField.setText("1"); amountField.setMaxLength(4);
        addDrawableChild(amountField);
        int[][] qp={{1,0},{16,1},{32,2},{64,3}};
        for (int[] q:qp) { final int qty=q[0], slot=q[1];
            addDrawableChild(ButtonWidget.builder(Text.literal("x"+qty), btn->amountField.setText(""+qty)).dimensions(px+14+slot*52,py+108,48,18).build()); }
        addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), btn->confirm()).dimensions(px+W/2-60,py+H-30,120,20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px=(width-W)/2, py=(height-H)/2;
        ctx.fill(px,py,px+W,py+H,0xE0100800);
        drawBorder(ctx,px,py,W,H,0xFF8B6914);
        ctx.fill(px,py,px+W,py+20,0xFF3D1F00);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Set Quantity - " + shopName), px+W/2, py+6, 0xFFFFFFFF);
        ctx.fill(px+W/2-12,py+30,px+W/2+12,py+54,0xFF444444);
        drawBorder(ctx,px+W/2-12,py+30,24,24,0xFFAAAAAA);
        ctx.drawItem(displayStack, px+W/2-8, py+34);
        ctx.drawCenteredTextWithShadow(textRenderer, displayStack.getName(), px+W/2, py+60, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Enter quantity:"), px+W/2, py+72, 0xAAAAAA);
        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        if (keyCode == 257 || keyCode == 335) { confirm(); return true; }
        return super.keyPressed(keyInput);
    }

    private void confirm() {
        int amount;
        try { amount=Integer.parseInt(amountField.getText().trim()); }
        catch (NumberFormatException e) { amount=1; }
        amount=Math.max(1,Math.min(amount, item.getDefaultStack().getMaxCount()));
        ClientPacketHandler.PendingBuyHolder.buyItem  = new ItemStack(item, amount);
        ClientPacketHandler.PendingBuyHolder.shopName = shopName;
        ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName));
        close();
    }

    @Override public boolean shouldPause() { return false; }
}
