package com.example.shopmod.screen;

import com.example.shopmod.client.ClientPacketHandler;
import com.example.shopmod.data.ShopData;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class InventorySelectScreen extends Screen {

    private final String shopName;
    private final ShopData shopData;

    // Local copy of inventory (36 slots)
    // Minecraft inventory: slots 0-8 = hotbar, slots 9-35 = main inventory (3 rows)
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
        super(Text.literal("Select Item - " + shopName));
        this.shopName = shopName;
        this.shopData = data;
    }

    @Override
    protected void init() {
        // Copy player's real inventory to local inventory
        if (client != null && client.player != null) {
            PlayerInventory inv = client.player.getInventory();
            for (int i = 0; i < 36; i++) {
                localInv[i] = inv.getStack(i).copy();
            }
        }

        int px = (width - W) / 2, py = (height - H) / 2;
        int specialSlotX = px + INV_COLS * SLOT_SIZE + 24;
        int specialSlotY = py + 70;

        // Confirm button (checkmark) - same size as slot (18x18)
        addDrawableChild(ButtonWidget.builder(Text.literal("✔"), btn -> {
            if (!selectedStack.isEmpty()) {
                removeFromRealInventory();
                ClientPacketHandler.PendingSellHolder.shopName = shopName;
                ClientPacketHandler.PendingSellHolder.sellItem = selectedStack.copy();
                ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName));
            }
        }).dimensions(specialSlotX, specialSlotY + SLOT_SIZE + 4, SLOT_SIZE, SLOT_SIZE).build());

        // Cancel button - same size as slot (18x18)
        addDrawableChild(ButtonWidget.builder(Text.literal("✖"), btn -> {
            ClientPacketHandler.PendingSellHolder.clear();
            ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName));
        }).dimensions(specialSlotX + SLOT_SIZE + 4, specialSlotY + SLOT_SIZE + 4, SLOT_SIZE, SLOT_SIZE).build());
    }

    private void removeFromRealInventory() {
        if (client == null || client.player == null) return;
        PlayerInventory inv = client.player.getInventory();

        int toRemove = selectedStack.getCount();
        for (int i = 0; i < 36 && toRemove > 0; i++) {
            ItemStack realStack = inv.getStack(i);
            if (!realStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(realStack, selectedStack)) {
                int take = Math.min(toRemove, realStack.getCount());
                realStack.decrement(take);
                toRemove -= take;
            }
        }
        inv.markDirty();
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawSlot(DrawContext ctx, int x, int y, ItemStack stack, boolean hovered) {
        ctx.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, hovered ? 0xFF5B5B5B : 0xFF3B3B3B);
        drawBorder(ctx, x, y, SLOT_SIZE, SLOT_SIZE, hovered ? 0xFFAAAAAA : 0xFF6B6B6B);

        if (!stack.isEmpty()) {
            ctx.drawItem(stack, x + 1, y + 1);
            if (stack.getCount() > 1) {
                String countStr = String.valueOf(stack.getCount());
                int w = textRenderer.getWidth(countStr);
                ctx.drawTextWithShadow(textRenderer, Text.literal(countStr), x + 19 - w, y + 11, 0xFFFFFFFF);
            }
        }
    }

    private void drawCursorStack(DrawContext ctx, int mx, int my) {
        if (!cursorStack.isEmpty()) {
            ctx.drawItem(cursorStack, mx - 8, my - 8);
            if (cursorStack.getCount() > 1) {
                String countStr = String.valueOf(cursorStack.getCount());
                int w = textRenderer.getWidth(countStr);
                ctx.drawTextWithShadow(textRenderer, Text.literal(countStr), mx - 8 + 19 - w, my - 8 + 11, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px = (width - W) / 2, py = (height - H) / 2;

        ctx.fill(px, py, px + W, py + H, 0xF0101010);
        drawBorder(ctx, px, py, W, H, 0xFF8B4513);

        ctx.fill(px, py, px + W, py + 24, 0xFF3D2810);
        ctx.drawCenteredTextWithShadow(textRenderer, "Select Item - " + shopName, px + W / 2, py + 8, 0xFFFFFFFF);

        int invX = px + 12;
        int invY = py + 42;

        ctx.drawTextWithShadow(textRenderer, "Inventory", invX, invY - 10, 0xFFAAAAAA);

        // Draw main inventory (3 rows) - MC slots 9-35
        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int mcSlot = 9 + row * INV_COLS + col;
                int sx = invX + col * SLOT_SIZE;
                int sy = invY + row * SLOT_SIZE;
                ItemStack stack = localInv[mcSlot];
                boolean hov = isOverSlot(mx, my, sx, sy);
                drawSlot(ctx, sx, sy, stack, hov);
            }
        }

        int hotbarY = invY + INV_ROWS * SLOT_SIZE + 4;
        ctx.fill(invX, hotbarY, invX + INV_COLS * SLOT_SIZE, hotbarY + 1, 0xFF555555);
        ctx.drawTextWithShadow(textRenderer, "Hotbar", invX, hotbarY + 4, 0xFFAAAAAA);

        hotbarY += 14;
        for (int col = 0; col < INV_COLS; col++) {
            int mcSlot = col;
            int sx = invX + col * SLOT_SIZE;
            int sy = hotbarY;
            ItemStack stack = localInv[mcSlot];
            boolean hov = isOverSlot(mx, my, sx, sy);
            drawSlot(ctx, sx, sy, stack, hov);
        }

        int specialX = invX + INV_COLS * SLOT_SIZE + 20;
        int specialY = py + 70;

        ctx.drawCenteredTextWithShadow(textRenderer, "Sell", specialX + SLOT_SIZE / 2, specialY - 10, 0xFF55FF55);

        boolean specialHov = isOverSlot(mx, my, specialX, specialY);
        ctx.fill(specialX, specialY, specialX + SLOT_SIZE, specialY + SLOT_SIZE, specialHov ? 0xFF4A6B4A : 0xFF3B5B3B);
        drawBorder(ctx, specialX, specialY, SLOT_SIZE, SLOT_SIZE, selectedStack.isEmpty() ? 0xFF888888 : 0xFF00FF00);

        if (!selectedStack.isEmpty()) {
            ctx.drawItem(selectedStack, specialX + 1, specialY + 1);
            if (selectedStack.getCount() > 1) {
                String countStr = String.valueOf(selectedStack.getCount());
                int w = textRenderer.getWidth(countStr);
                ctx.drawTextWithShadow(textRenderer, Text.literal(countStr), specialX + 19 - w, specialY + 11, 0xFFFFFFFF);
            }
        }

        drawCursorStack(ctx, mx, my);

        ctx.drawCenteredTextWithShadow(textRenderer, "Left: Pick/Place all | Right: Split/Place one", px + W / 2, py + H - 14, 0xFF888888);

        super.render(ctx, mx, my, delta);
    }

    private boolean isOverSlot(int mx, int my, int sx, int sy) {
        return mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE;
    }

    private int getSlotAtPosition(int mx, int my) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int invX = px + 12;
        int invY = py + 42;

        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int sx = invX + col * SLOT_SIZE;
                int sy = invY + row * SLOT_SIZE;
                if (isOverSlot(mx, my, sx, sy)) {
                    return 9 + row * INV_COLS + col;
                }
            }
        }

        int hotbarY = invY + INV_ROWS * SLOT_SIZE + 18;
        for (int col = 0; col < INV_COLS; col++) {
            int sx = invX + col * SLOT_SIZE;
            int sy = hotbarY;
            if (isOverSlot(mx, my, sx, sy)) {
                return col;
            }
        }

        return -1;
    }

    private boolean isOverSpecialSlot(int mx, int my) {
        int px = (width - W) / 2, py = (height - H) / 2;
        int invX = px + 12;
        int specialX = invX + INV_COLS * SLOT_SIZE + 20;
        int specialY = py + 70;
        return isOverSlot(mx, my, specialX, specialY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mx = click.x(), my = click.y();
        int button = click.button();

        if (isOverSpecialSlot((int) mx, (int) my)) {
            handleSpecialSlotClick(button);
            return true;
        }

        int slot = getSlotAtPosition((int) mx, (int) my);
        if (slot >= 0) {
            handleInventorySlotClick(slot, button);
            return true;
        }

        return super.mouseClicked(click, bl);
    }

    private void handleInventorySlotClick(int slot, int button) {
        ItemStack stackInSlot = localInv[slot];

        if (button == 0) { // Left click
            if (!cursorStack.isEmpty()) {
                if (stackInSlot.isEmpty()) {
                    localInv[slot] = cursorStack;
                    cursorStack = ItemStack.EMPTY;
                } else if (ItemStack.areItemsAndComponentsEqual(cursorStack, stackInSlot)) {
                    int max = stackInSlot.getMaxCount();
                    int canAdd = Math.min(cursorStack.getCount(), max - stackInSlot.getCount());
                    if (canAdd > 0) {
                        stackInSlot.increment(canAdd);
                        cursorStack.decrement(canAdd);
                        if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY;
                    }
                } else {
                    localInv[slot] = cursorStack;
                    cursorStack = stackInSlot;
                }
            } else if (!stackInSlot.isEmpty()) {
                cursorStack = stackInSlot.copy();
                localInv[slot] = ItemStack.EMPTY;
            }
        } else if (button == 1) { // Right click
            if (!cursorStack.isEmpty()) {
                if (stackInSlot.isEmpty()) {
                    ItemStack oneItem = cursorStack.copy();
                    oneItem.setCount(1);
                    localInv[slot] = oneItem;
                    cursorStack.decrement(1);
                    if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY;
                } else if (ItemStack.areItemsAndComponentsEqual(cursorStack, stackInSlot) && stackInSlot.getCount() < stackInSlot.getMaxCount()) {
                    stackInSlot.increment(1);
                    cursorStack.decrement(1);
                    if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY;
                }
            } else if (!stackInSlot.isEmpty()) {
                int half = (stackInSlot.getCount() + 1) / 2;
                cursorStack = new ItemStack(stackInSlot.getItem(), half);
                stackInSlot.decrement(half);
                if (stackInSlot.getCount() <= 0) localInv[slot] = ItemStack.EMPTY;
            }
        }
    }

    private void handleSpecialSlotClick(int button) {
        if (button == 0) { // Left click
            if (!cursorStack.isEmpty()) {
                if (selectedStack.isEmpty()) {
                    selectedStack = cursorStack;
                    cursorStack = ItemStack.EMPTY;
                } else if (ItemStack.areItemsAndComponentsEqual(cursorStack, selectedStack)) {
                    int max = selectedStack.getMaxCount();
                    int canAdd = Math.min(cursorStack.getCount(), max - selectedStack.getCount());
                    if (canAdd > 0) {
                        selectedStack.increment(canAdd);
                        cursorStack.decrement(canAdd);
                        if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY;
                    }
                } else {
                    ItemStack temp = selectedStack;
                    selectedStack = cursorStack;
                    cursorStack = temp;
                }
            } else if (!selectedStack.isEmpty()) {
                cursorStack = selectedStack;
                selectedStack = ItemStack.EMPTY;
            }
        } else if (button == 1) { // Right click
            if (!cursorStack.isEmpty()) {
                if (selectedStack.isEmpty()) {
                    ItemStack oneItem = cursorStack.copy();
                    oneItem.setCount(1);
                    selectedStack = oneItem;
                    cursorStack.decrement(1);
                    if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY;
                } else if (ItemStack.areItemsAndComponentsEqual(cursorStack, selectedStack) && selectedStack.getCount() < selectedStack.getMaxCount()) {
                    selectedStack.increment(1);
                    cursorStack.decrement(1);
                    if (cursorStack.getCount() <= 0) cursorStack = ItemStack.EMPTY;
                }
            } else if (!selectedStack.isEmpty()) {
                int half = selectedStack.getCount() / 2;
                if (half > 0) {
                    cursorStack = new ItemStack(selectedStack.getItem(), half);
                    selectedStack.decrement(half);
                    if (selectedStack.getCount() <= 0) selectedStack = ItemStack.EMPTY;
                }
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
