package com.example.shopmod.screen;

import com.example.shopmod.data.I18n;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ItemPickerScreen extends Screen {

    private final String shopName;
    private List<ItemStack> allItems;
    private List<ItemStack> shown;
    private EditBox searchBox;
    private int page = 0;
    private static final int COLS=9, ROWS=7, PER_PAGE=COLS*ROWS;
    private static final int SLOT_SIZE=22, SLOT_GAP=3;
    private static final int W=COLS*(SLOT_SIZE+SLOT_GAP)+30, H=250;

    private static final Set<String> EXCLUDED_ITEMS = Set.of(
        "minecraft:command_block","minecraft:chain_command_block","minecraft:repeating_command_block",
        "minecraft:command_block_minecart","minecraft:structure_block","minecraft:structure_void",
        "minecraft:barrier","minecraft:light","minecraft:jigsaw","minecraft:debug_stick",
        "minecraft:test_block","minecraft:test_instance_block","minecraft:end_portal_frame","minecraft:bedrock",
        "minecraft:spawner","minecraft:trial_spawner","minecraft:player_head", "minecraft:tipped_arrow"
    );

    /**
     * 构造函数，创建物品选择器界面
     * @param shopName 商店名称，将显示在界面标题中
     */
    public ItemPickerScreen(String shopName) {
        super(Component.literal(I18n.get("picker.title") + " - " + shopName));
        this.shopName = shopName;
        allItems = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            var itemId = BuiltInRegistries.ITEM.getKey(item);
            String itemIdStr = itemId.toString();
            if (itemIdStr.contains("spawn_egg") || EXCLUDED_ITEMS.contains(itemIdStr) || isHiddenItem(itemIdStr) || itemIdStr.equals("minecraft:enchanted_book")) continue;
            allItems.add(new ItemStack(item));
        }
        shown = new ArrayList<>(allItems);
    }

    /**
     * Extract location string from ResourceKey.toString().
     * Handles "ResourceKey[<registry> / <location>]" format.
     */
    private static String extractLocationString(Object resourceKey) {
        String s = resourceKey.toString();
        if (s.startsWith("ResourceKey[") && s.endsWith("]")) {
            s = s.substring(12, s.length() - 1);
        }
        int idx = s.lastIndexOf(" / ");
        if (idx >= 0) {
            return s.substring(idx + 3);
        }
        return s;
    }

    private void addEnchantedBooksFromRegistry() {
        if (this.minecraft == null || this.minecraft.level == null) return;
        try {
            var enchRef = this.minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            for (var mapEntry : enchRef.entrySet()) {
                try {
                    net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchKey = mapEntry.getKey();
                    Enchantment enchantment = mapEntry.getValue();

                    String enchantIdStr = extractLocationString(enchKey);

                    var holderOpt = enchRef.get(enchKey);
                    if (holderOpt.isEmpty()) continue;

                    int maxLevel = enchantment.getMaxLevel();
                    for (int level = 1; level <= maxLevel; level++) {
                        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK, 1);

                        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        mutable.set(holderOpt.get(), level);
                        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());

                        int colonIdx = enchantIdStr.indexOf(':');
                        String enchTranslateKey;
                        if (colonIdx >= 0) {
                            enchTranslateKey = "enchantment." + enchantIdStr.substring(0, colonIdx) + "." + enchantIdStr.substring(colonIdx + 1);
                        } else {
                            enchTranslateKey = "enchantment.minecraft." + enchantIdStr;
                        }
                        Component enchantName = Component.translatable(enchTranslateKey);
                        String levelStr = getRomanNumeral(level);
                        book.set(DataComponents.CUSTOM_NAME, Component.literal(enchantName.getString() + " " + levelStr));

                        allItems.add(book);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private String getRomanNumeral(int num) {
        String[] n = {"I","II","III","IV","V","VI","VII","VIII","IX","X"};
        return (num >= 1 && num <= n.length) ? n[num - 1] : String.valueOf(num);
    }

    private boolean isHiddenItem(String id) {
        return id.contains("command") || id.contains("barrier") || id.contains("structure")
            || id.contains("jigsaw") || id.contains("light") || id.contains("test_block")
            || id.contains("test_instance") || id.endsWith("_spawn_egg");
    }

    private static void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,y,x+w,y+1,color); ctx.fill(x,y+h-1,x+w,y+h,color);
        ctx.fill(x,y,x+1,y+h,color); ctx.fill(x+w-1,y,x+w,y+h,color);
    }

    private void filter(String query) {
        if (allItems.stream().noneMatch(i -> i.getItem() == Items.ENCHANTED_BOOK)) addEnchantedBooksFromRegistry();
        if (query==null||query.isBlank()) { shown=new ArrayList<>(allItems); }
        else {
            String q=query.toLowerCase().trim(); shown=new ArrayList<>();
            for (ItemStack s:allItems) {
                String name = s.getHoverName().getString().toLowerCase();
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
                if (name.contains(q) || id.contains(q)) shown.add(s);
            }
        }
        page=0;
    }

    @Override
    protected void init() {
        addEnchantedBooksFromRegistry();
        int px=(width-W)/2, py=(height-H)/2;
        searchBox=new EditBox(font, px+5, py+26, W-10, 16, Component.literal("Search"));
        searchBox.setHint(Component.literal(I18n.get("picker.search")));
        searchBox.setResponder(this::filter);
        addRenderableWidget(searchBox);
        addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.prev")), btn->{if(page>0)page--;}).bounds(px+4,py+H-24,30,20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.get("shop.next")), btn->{int max=(shown.size()-1)/PER_PAGE;if(page<max)page++;}).bounds(px+W-34,py+H-24,30,20).build());
        addRenderableWidget(Button.builder(Component.literal(I18n.get("picker.back")), btn->
            ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName))
        ).bounds(px+W/2-30,py+H-24,60,20).build());
        filter("");
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px=(width-W)/2, py=(height-H)/2;
        ctx.fill(px,py,px+W,py+H,0xE0100800);
        drawBorder(ctx,px,py,W,H,0xFF8B6914);
        ctx.fill(px,py,px+W,py+22,0xFF3D1F00);
        ctx.drawCenteredString(font, Component.literal(I18n.get("picker.title") + " - " + shopName), px+W/2, py+7, 0xFFFFFFFF);

        int gridWidth = COLS * (SLOT_SIZE + SLOT_GAP);
        int gridX = px + (W - gridWidth) / 2, gridY = py + 46;
        ItemStack hovered=ItemStack.EMPTY;
        int startIdx=page*PER_PAGE;
        for (int i=0; i<PER_PAGE; i++) {
            int idx=startIdx+i; if(idx>=shown.size()) break;
            int col=i%COLS, row=i/COLS, sx=gridX+col*(SLOT_SIZE+SLOT_GAP), sy=gridY+row*(SLOT_SIZE+SLOT_GAP);
            boolean hov=mx>=sx&&mx<sx+SLOT_SIZE&&my>=sy&&my<sy+SLOT_SIZE;
            ctx.fill(sx,sy,sx+SLOT_SIZE,sy+SLOT_SIZE, hov?0xAA8B6914:0x88333333);
            drawBorder(ctx,sx,sy,SLOT_SIZE,SLOT_SIZE,0xFF555555);
            ctx.renderItem(shown.get(idx),sx+3,sy+3);
            if(hov) hovered=shown.get(idx);
        }
        int total=Math.max(1,(shown.size()+PER_PAGE-1)/PER_PAGE);
        ctx.drawCenteredString(font, Component.literal(I18n.get("picker.page_info", page+1, total, shown.size())), px+W/2, py+H-38, 0xAAAAAA);
        super.render(ctx, mx, my, delta);
        // Proper tooltip rendering
        if(!hovered.isEmpty()) {
            ctx.setTooltipForNextFrame(font, hovered, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int px=(width-W)/2, py=(height-H)/2;
        int gridWidth = COLS * (SLOT_SIZE + SLOT_GAP);
        int gridX = px + (W - gridWidth) / 2, gridY = py + 46;
        double mx=event.x(), my=event.y();
        int startIdx=page*PER_PAGE;
        for (int i=0; i<PER_PAGE; i++) {
            int idx=startIdx+i; if(idx>=shown.size()) break;
            int col=i%COLS, row=i/COLS, sx=gridX+col*(SLOT_SIZE+SLOT_GAP), sy=gridY+row*(SLOT_SIZE+SLOT_GAP);
            if(mx>=sx&&mx<sx+SLOT_SIZE&&my>=sy&&my<sy+SLOT_SIZE) {
                ItemStack selected = shown.get(idx);
                if (selected.getItem() == Items.ENCHANTED_BOOK && selected.get(DataComponents.STORED_ENCHANTMENTS) != null) {
                    if (this.minecraft != null) this.minecraft.setScreen(new EnchantedBookAmountScreen(shopName, selected.copy()));
                    return true;
                }
                ClientPlayNetworking.send(new ModPackets.ReqAmountPayload(shopName, BuiltInRegistries.ITEM.getKey(selected.getItem()).toString()));
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean isPauseScreen() { return false; }
}