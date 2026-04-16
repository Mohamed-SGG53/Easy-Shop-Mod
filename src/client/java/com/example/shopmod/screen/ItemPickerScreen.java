package com.example.shopmod.screen;

import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Environment(EnvType.CLIENT)
public class ItemPickerScreen extends Screen {

    private final String shopName;
    private List<ItemStack> allItems;
    private List<ItemStack> shown;
    private TextFieldWidget searchBox;
    private int page = 0;
    private static final int COLS=9, ROWS=7, PER_PAGE=COLS*ROWS; // 7 rows
    private static final int SLOT_SIZE=22; // Increased from 18 by 2px on each side
    private static final int SLOT_GAP=3; // Horizontal gap between slots
    private static final int W=COLS*(SLOT_SIZE+SLOT_GAP)+30, H=250; // Fixed height, added row without increasing window size

    // Items to exclude
    private static final Set<String> EXCLUDED_ITEMS = Set.of(
        "minecraft:command_block",
        "minecraft:chain_command_block", 
        "minecraft:repeating_command_block",
        "minecraft:command_block_minecart",
        "minecraft:structure_block",
        "minecraft:structure_void",
        "minecraft:barrier",
        "minecraft:light",
        "minecraft:jigsaw",
        "minecraft:debug_stick",
        "minecraft:test_block",
        "minecraft:test_instance_block",
        "minecraft:end_portal_frame",
        "minecraft:bedrock"
    );

    public ItemPickerScreen(String shopName) {
        super(Text.literal("Select Price - " + shopName));
        this.shopName = shopName;
        allItems = new ArrayList<>();
        
        // Add all regular items (excluding enchanted_book - we add those separately with enchantments)
        for (Item item : Registries.ITEM) {
            String itemId = Registries.ITEM.getId(item).toString();
            
            // Skip spawn eggs
            if (itemId.contains("spawn_egg")) continue;
            
            // Skip excluded items
            if (EXCLUDED_ITEMS.contains(itemId)) continue;
            
            // Skip hidden/technical items
            if (isHiddenItem(itemId)) continue;
            
            // Skip enchanted_book - we'll add enchanted books with enchantments
            if (itemId.equals("minecraft:enchanted_book")) continue;
            
            allItems.add(new ItemStack(item));
        }
        
        // Add enchanted books from game registry (all enchantments with all levels)
        // Note: We can only do this if we have client world access
        // For now, we'll add them when the screen is first shown
        
        shown = new ArrayList<>(allItems);
    }
    
    /**
     * Called to add enchanted books when client world is available
     */
    private void addEnchantedBooksFromRegistry() {
        if (client == null || client.world == null) return;
        
        try {
            var enchantmentRegistry = client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            
            for (RegistryEntry<Enchantment> entry : enchantmentRegistry.streamEntries().toList()) {
                try {
                    // Get enchantment info
                    var key = entry.getKey();
                    if (key.isEmpty()) continue;
                    
                    Identifier enchantId = key.get().getValue();
                    Enchantment enchantment = entry.value();
                    
                    // Get max level for this enchantment
                    int maxLevel = enchantment.getMaxLevel();
                    
                    // Create enchanted books for each level
                    for (int level = 1; level <= maxLevel; level++) {
                        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK, 1);
                        
                        // Apply enchantment to book
                        ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
                        builder.add(entry, level);
                        book.set(DataComponentTypes.STORED_ENCHANTMENTS, builder.build());
                        
                        // Set custom name to show enchantment and level
                        Text enchantName = Text.translatable(enchantId.toTranslationKey("enchantment"));
                        String levelStr = getRomanNumeral(level);
                        book.set(DataComponentTypes.CUSTOM_NAME, Text.literal(enchantName.getString() + " " + levelStr));
                        
                        allItems.add(book);
                    }
                } catch (Exception e) {
                    // Skip this enchantment if there's an error
                }
            }
        } catch (Exception e) {
            // Registry not available
        }
    }
    
    private String getRomanNumeral(int num) {
        String[] numerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (num >= 1 && num <= numerals.length) {
            return numerals[num - 1];
        }
        return String.valueOf(num);
    }
    
    private boolean isHiddenItem(String itemId) {
        return itemId.contains("command") || 
               itemId.contains("barrier") || 
               itemId.contains("structure") ||
               itemId.contains("jigsaw") ||
               itemId.contains("light") ||
               itemId.contains("test_block") ||
               itemId.contains("test_instance") ||
               itemId.endsWith("_spawn_egg");
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,y,x+w,y+1,color); ctx.fill(x,y+h-1,x+w,y+h,color);
        ctx.fill(x,y,x+1,y+h,color); ctx.fill(x+w-1,y,x+w,y+h,color);
    }

    private void filter(String query) {
        // Re-add enchanted books when filtering (in case they weren't added yet)
        if (allItems.stream().noneMatch(i -> i.getItem() == Items.ENCHANTED_BOOK)) {
            addEnchantedBooksFromRegistry();
        }
        
        if (query==null||query.isBlank()) { shown=new ArrayList<>(allItems); }
        else {
            String q=query.toLowerCase().trim(); shown=new ArrayList<>();
            for (ItemStack s:allItems) {
                String name = s.getName().getString().toLowerCase();
                String id = Registries.ITEM.getId(s.getItem()).toString();
                if (name.contains(q) || id.contains(q)) {
                    shown.add(s);
                }
            }
        }
        page=0;
    }

    @Override
    protected void init() {
        // Add enchanted books when screen initializes
        addEnchantedBooksFromRegistry();
        
        int px=(width-W)/2, py=(height-H)/2;
        searchBox=new TextFieldWidget(textRenderer, px+5, py+26, W-10, 16, Text.literal("Search"));
        searchBox.setPlaceholder(Text.literal("Search..."));
        searchBox.setChangedListener(this::filter);
        addDrawableChild(searchBox);
        
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), btn->{if(page>0)page--;}).dimensions(px+4,py+H-24,30,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), btn->{int max=(shown.size()-1)/PER_PAGE;if(page<max)page++;}).dimensions(px+W-34,py+H-24,30,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn->
            ClientPlayNetworking.send(new ModPackets.ReqOwnerScreenPayload(shopName))
        ).dimensions(px+W/2-30,py+H-24,60,20).build());
        
        // Refresh shown list after adding enchanted books
        filter("");
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        int px=(width-W)/2, py=(height-H)/2;
        ctx.fill(px,py,px+W,py+H,0xE0100800);
        drawBorder(ctx,px,py,W,H,0xFF8B6914);
        ctx.fill(px,py,px+W,py+22,0xFF3D1F00);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Select Price - " + shopName), px+W/2, py+7, 0xFFFFFFFF);

        // Center the grid horizontally
        int gridWidth = COLS * (SLOT_SIZE + SLOT_GAP);
        int gridX = px + (W - gridWidth) / 2;
        int gridY = py + 46;
        ItemStack hovered=ItemStack.EMPTY;
        int startIdx=page*PER_PAGE;
        for (int i=0; i<PER_PAGE; i++) {
            int idx=startIdx+i; if(idx>=shown.size()) break;
            ItemStack s=shown.get(idx);
            int col=i%COLS, row=i/COLS, sx=gridX+col*(SLOT_SIZE+SLOT_GAP), sy=gridY+row*(SLOT_SIZE+SLOT_GAP);
            boolean hov=mx>=sx&&mx<sx+SLOT_SIZE&&my>=sy&&my<sy+SLOT_SIZE;
            ctx.fill(sx,sy,sx+SLOT_SIZE,sy+SLOT_SIZE, hov?0xAA8B6914:0x88333333);
            drawBorder(ctx,sx,sy,SLOT_SIZE,SLOT_SIZE,0xFF555555);
            ctx.drawItem(s,sx+3,sy+3); // Center item in larger slot
            if(hov) hovered=s;
        }
        int total=Math.max(1,(shown.size()+PER_PAGE-1)/PER_PAGE);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Page "+(page+1)+"/"+total+"  |  "+shown.size()+" items"), px+W/2, py+H-38, 0xAAAAAA);
        super.render(ctx, mx, my, delta);
        if(!hovered.isEmpty()) ctx.drawItemTooltip(textRenderer, hovered, mx, my);
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        int px=(width-W)/2, py=(height-H)/2;
        // Center the grid horizontally (same as render)
        int gridWidth = COLS * (SLOT_SIZE + SLOT_GAP);
        int gridX = px + (W - gridWidth) / 2;
        int gridY = py + 46;
        int startIdx=page*PER_PAGE;
        double mx=click.x(), my=click.y();
        for (int i=0; i<PER_PAGE; i++) {
            int idx=startIdx+i; if(idx>=shown.size()) break;
            int col=i%COLS, row=i/COLS, sx=gridX+col*(SLOT_SIZE+SLOT_GAP), sy=gridY+row*(SLOT_SIZE+SLOT_GAP);
            if(mx>=sx&&mx<sx+SLOT_SIZE&&my>=sy&&my<sy+SLOT_SIZE) {
                ItemStack selected = shown.get(idx);
                
                // Check if it's an enchanted book with stored enchantments
                if (selected.getItem() == Items.ENCHANTED_BOOK && selected.contains(DataComponentTypes.STORED_ENCHANTMENTS)) {
                    // For enchanted books, we need to set quantity
                    if (client != null) {
                        client.setScreen(new EnchantedBookAmountScreen(shopName, selected.copy()));
                    }
                    return true;
                }
                
                // Regular item - open amount screen
                ClientPlayNetworking.send(new ModPackets.ReqAmountPayload(shopName, Registries.ITEM.getId(selected.getItem()).toString()));
                return true;
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override public boolean shouldPause() { return false; }
}
