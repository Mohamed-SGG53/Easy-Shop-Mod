package com.example.shopmod.screen;

import com.example.shopmod.client.ClientPacketHandler;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class InventorySelectScreen extends Screen {

    private final String shopName;
    private final ShopData shopData;

    // Local copy of inventory (36 slots)
    private final ItemStack[] localInv = new ItemStack[36];

    // The item being held by cursor (drag & drop)
    private ItemStack cursorStack = ItemStack.EMPTY;

    // The selected item to sell (in the special slot)
    private ItemStack selectedStack = ItemStack.EMPTY;

    // Slot dimensions
    private static final int SLOT_SIZE = 18;
    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 3;

    // GUI dimensions
    private static final int W = 250;
    private static final int H = 170;

    public InventorySelectScreen(String shopName, ShopData data) {
        super(Component.literal(I18n.get("select.title") + " - " + shopName));
        this.shopName = shopName;
        this.shopData = data;
    }

    @Override
    protected void init() {
        if (this.minecraft != null && this.minecraft.player != null) {
            Inventory inv = this.minecraft.player.getInventory();
            for (int i = 0; i < 36; i++) {
                localInv[i] = inv.getItem(i).copy();
            }
        }

        int px = (width - W) / 2, py = (height - H) / 2;
        int specialSlotX = px + INV_COLS * SLOT_SIZE + 24;
        int specialSlotY = py + 70;

        addRenderableWidget(Button.builder(Component.literal("\u2714"), btn -> {
            if (!selectedStack.isEmpty()) {
                ClientPacketHandler.PendingSellHolder.shopName = shopName;
                ClientPacketHandler.PendingSellHolder.sellItem = selectedStack.copy();
                ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName));
            }
        }).bounds(specialSlotX, specialSlotY + SLOT_SIZE + 4, SLOT_SIZE, SLOT_SIZE).build());

        addRenderableWidget(Button.builder(Component.literal("\u2716"), btn -> {
            ClientPacketHandler.PendingSellHolder.clear();
            ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName));
        }).bounds(specialSlotX + SLOT_SIZE + 4, specialSlotY + SLOT_SIZE + 4, SLOT_SIZE, SLOT_SIZE).build());
    }

    private static void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawSlot(GuiGraphics ctx, int x, int y, ItemStack stack, boolean hovered) {
        ctx.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hovered ? 0xFF5B5B5B : 0xFF3B3B3B);
        drawBorder(ctx, x, y, SLOT_SIZE, SLOT_SIZE, hovered ? 0xFFAAAAAA : 0xFF6B6B6B);
        if (!stack.isEmpty()) {
            ctx.renderItem(stack, x + 1, y + 1);
            if (stack.getCount() > 1) {
                String countStr = String.valueOf(stack.getCount());
                int w = font.width(countStr);
                ctx.drawString(font, Component.literal(countStr), x + 19 - w, y + 11, 0xFFFFFFFF);
            }
        }
    }

    private void drawCursorStack(GuiGraphics ctx, int mx, int my) {
        if (!cursorStack.isEmpty()) {
            ctx.renderItem(cursorStack, mx - 8, my - 8);
            if (cursorStack.getCount() > 1) {
                String countStr = String.valueOf(cursorStack.getCount());
                int w = font.width(countStr);
                ctx.drawString(font, Component.literal(countStr), mx - 8 + 19 - w, my - 8 + 11, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x88000000);
        int px = (width - W) / 2, py = (height - H) / 2;

        ctx.fill(px, py, px + W, py + H, 0xF0101010);
        drawBorder(ctx, px, py, W, H, 0xFF8B4513);
        ctx.fill(px, py, px + W, py + 24, 0xFF3D2810);
        ctx.drawCenteredString(font, Component.literal(I18n.get("select.title") + " - " + shopName), px + W / 2, py + 8, 0xFFFFFFFF);

        int invX = px + 12;
        int invY = py + 42;
        ctx.drawString(font, Component.literal(I18n.get("select.inventory")), invX, invY - 10, 0xFFAAAAAA);

        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int mcSlot = 9 + row * INV_COLS + col;
                int sx = invX + col * SLOT_SIZE;
                int sy = invY + row * SLOT_SIZE;
                drawSlot(ctx, sx, sy, localInv[mcSlot], isOverSlot(mx, my, sx, sy));
            }
        }

        int hotbarY = invY + INV_ROWS * SLOT_SIZE + 4;
        ctx.fill(invX, hotbarY, invX + INV_COLS * SLOT_SIZE, hotbarY + 1, 0xFF555555);
        ctx.drawString(font, Component.literal(I18n.get("select.hotbar")), invX, hotbarY + 4, 0xFFAAAAAA);
        hotbarY += 14;
        for (int col = 0; col < INV_COLS; col++) {
            int sx = invX + col * SLOT_SIZE;
            int sy = hotbarY;
            drawSlot(ctx, sx, sy, localInv[col], isOverSlot(mx, my, sx, sy));
        }

        int specialX = invX + INV_COLS * SLOT_SIZE + 20;
        int specialY = py + 70;
        ctx.drawCenteredString(font, Component.literal(I18n.get("select.sell")), specialX + SLOT_SIZE / 2, specialY - 10, 0xFF55FF55);

        boolean specialHov = isOverSlot(mx, my, specialX, specialY);
        ctx.fill(specialX, specialY, specialX + SLOT_SIZE, specialY + SLOT_SIZE, specialHov ? 0xFF4A6B4A : 0xFF3B5B3B);
        drawBorder(ctx, specialX, specialY, SLOT_SIZE, SLOT_SIZE, selectedStack.isEmpty() ? 0xFF888888 : 0xFF00FF00);
        if (!selectedStack.isEmpty()) {
            ctx.renderItem(selectedStack, specialX + 1, specialY + 1);
            if (selectedStack.getCount() > 1) {
                String countStr = String.valueOf(selectedStack.getCount());
                int w = font.width(countStr);
                ctx.drawString(font, Component.literal(countStr), specialX + 19 - w, specialY + 11, 0xFFFFFFFF);
            }
        }

        drawCursorStack(ctx, mx, my);
        ctx.drawCenteredString(font, Component.literal(I18n.get("select.hint")), px + W / 2, py + H - 14, 0xFF888888);
        super.render(ctx, mx, my, delta);
    }

    private boolean isOverSlot(int mx, int my, int sx, int sy) {
        return mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE;
    }

    private int getSlotAtPosition(int mx, int my) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int invX = px + 12, invY = py + 42;
        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int sx = invX + col * SLOT_SIZE, sy = invY + row * SLOT_SIZE;
                if (isOverSlot(mx, my, sx, sy)) return 9 + row * INV_COLS + col;
            }
        }
        int hotbarY = invY + INV_ROWS * SLOT_SIZE + 18;
        for (int col = 0; col < INV_COLS; col++) {
            if (isOverSlot(mx, my, invX + col * SLOT_SIZE, hotbarY)) return col;
        }
        return -1;
    }

    private boolean isOverSpecialSlot(int mx, int my) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int specialX = px + 12 + INV_COLS * SLOT_SIZE + 20;
        return isOverSlot(mx, my, specialX, py + 70);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (isOverSpecialSlot((int) mx, (int) my)) {
            handleSpecialSlotClick(button);
            return true;
        }
        int slot = getSlotAtPosition((int) mx, (int) my);
        if (slot >= 0) {
            handleInventorySlotClick(slot, button);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void handleInventorySlotClick(int slot, int button) {
        ItemStack stackInSlot = localInv[slot];
        if (button == 0) {
            if (!cursorStack.isEmpty()) {
                if (stackInSlot.isEmpty()) {
                    localInv[slot] = cursorStack; cursorStack = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(cursorStack, stackInSlot)) {
                    int canAdd = Math.min(cursorStack.getCount(), stackInSlot.getMaxStackSize() - stackInSlot.getCount());
                    if (canAdd > 0) { stackInSlot.grow(canAdd); cursorStack.shrink(canAdd); if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY; }
                } else { localInv[slot] = cursorStack; cursorStack = stackInSlot; }
            } else if (!stackInSlot.isEmpty()) { cursorStack = stackInSlot.copy(); localInv[slot] = ItemStack.EMPTY; }
        } else if (button == 1) {
            if (!cursorStack.isEmpty()) {
                if (stackInSlot.isEmpty()) {
                    ItemStack one = cursorStack.copy(); one.setCount(1); localInv[slot] = one; cursorStack.shrink(1); if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(cursorStack, stackInSlot) && stackInSlot.getCount() < stackInSlot.getMaxStackSize()) {
                    stackInSlot.grow(1); cursorStack.shrink(1); if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY;
                }
            } else if (!stackInSlot.isEmpty()) {
                int half = (stackInSlot.getCount() + 1) / 2; cursorStack = new ItemStack(stackInSlot.getItem(), half); stackInSlot.shrink(half); if (stackInSlot.getCount() <= 0) localInv[slot] = ItemStack.EMPTY;
            }
        }
    }

    private void handleSpecialSlotClick(int button) {
        if (button == 0) {
            if (!cursorStack.isEmpty()) {
                if (selectedStack.isEmpty()) { selectedStack = cursorStack; cursorStack = ItemStack.EMPTY; }
                else if (ItemStack.isSameItemSameComponents(cursorStack, selectedStack)) {
                    int canAdd = Math.min(cursorStack.getCount(), selectedStack.getMaxStackSize() - selectedStack.getCount());
                    if (canAdd > 0) { selectedStack.grow(canAdd); cursorStack.shrink(canAdd); if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY; }
                } else { ItemStack t = selectedStack; selectedStack = cursorStack; cursorStack = t; }
            } else if (!selectedStack.isEmpty()) { cursorStack = selectedStack; selectedStack = ItemStack.EMPTY; }
        } else if (button == 1) {
            if (!cursorStack.isEmpty()) {
                if (selectedStack.isEmpty()) { ItemStack one = cursorStack.copy(); one.setCount(1); selectedStack = one; cursorStack.shrink(1); if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY; }
                else if (ItemStack.isSameItemSameComponents(cursorStack, selectedStack) && selectedStack.getCount() < selectedStack.getMaxStackSize()) { selectedStack.grow(1); cursorStack.shrink(1); if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY; }
            } else if (!selectedStack.isEmpty()) {
                int half = selectedStack.getCount() / 2;
                if (half > 0) { cursorStack = new ItemStack(selectedStack.getItem(), half); selectedStack.shrink(half); if (selectedStack.getCount() <= 0) selectedStack = ItemStack.EMPTY; }
            }
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}