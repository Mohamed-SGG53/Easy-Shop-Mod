package com.example.shopmod.data;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ShopData {

    public static class ShopTrade {
        public ItemStack sellItem;  // Item being sold
        public ItemStack buyItem;   // Item being paid

        public ShopTrade(ItemStack sellItem, ItemStack buyItem) {
            this.sellItem = sellItem.copy();
            this.buyItem  = buyItem.copy();
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.put("sell", ShopData.itemStackToNbt(sellItem));
            nbt.put("buy",  ShopData.itemStackToNbt(buyItem));
            return nbt;
        }

        public static ShopTrade fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
            CompoundTag sellTag = nbt.contains("sell") ? nbt.getCompound("sell").orElseGet(CompoundTag::new) : new CompoundTag();
            CompoundTag buyTag  = nbt.contains("buy")  ? nbt.getCompound("buy").orElseGet(CompoundTag::new)  : new CompoundTag();
            ItemStack sell = ShopData.itemStackFromNbt(sellTag, registries);
            ItemStack buy  = ShopData.itemStackFromNbt(buyTag, registries);
            return new ShopTrade(sell, buy);
        }
    }

    private final String ownerName;
    private UUID ownerUuid;
    private final List<ShopTrade> trades = new ArrayList<>();
    private final List<ItemStack> storage = new ArrayList<>();

    public ShopData(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerName()       { return ownerName; }
    public UUID getOwnerUuid()         { return ownerUuid; }
    public void setOwnerUuid(UUID uuid){ this.ownerUuid = uuid; }
    public List<ShopTrade> getTrades() { return trades; }
    public List<ItemStack> getStorage() { return storage; }

    public void addToStorage(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        storage.add(stack.copy());
    }

    public ItemStack takeFromStorage(int index) {
        if (index >= 0 && index < storage.size()) {
            return storage.remove(index);
        }
        return ItemStack.EMPTY;
    }

    public void removeStorageItem(int index) {
        if (index >= 0 && index < storage.size()) {
            storage.remove(index);
        }
    }

    public void addTrade(ItemStack sell, ItemStack buy) {
        trades.add(new ShopTrade(sell, buy));
    }

    public ShopTrade removeTrade(int index) {
        if (index >= 0 && index < trades.size()) {
            return trades.remove(index);
        }
        return null;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt  = new CompoundTag();
        nbt.putString("owner", ownerName);
        if (ownerUuid != null) {
            nbt.putLong("owner_uuid_most", ownerUuid.getMostSignificantBits());
            nbt.putLong("owner_uuid_least", ownerUuid.getLeastSignificantBits());
        }

        ListTag tradeList = new ListTag();
        for (ShopTrade t : trades) tradeList.add(t.toNbt());
        nbt.put("trades", tradeList);

        ListTag storageList = new ListTag();
        for (ItemStack s : storage) {
            storageList.add(itemStackToNbt(s));
        }
        nbt.put("storage", storageList);

        return nbt;
    }

    public static ShopData fromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        String owner = nbt.getString("owner").orElse("unknown");
        ShopData data = new ShopData(owner);

        long uuidMost = nbt.getLong("owner_uuid_most").orElse(0L);
        long uuidLeast = nbt.getLong("owner_uuid_least").orElse(0L);
        if (uuidMost != 0 || uuidLeast != 0) {
            data.ownerUuid = new UUID(uuidMost, uuidLeast);
        }

        if (nbt.contains("trades")) {
            ListTag list = nbt.getList("trades").orElseGet(ListTag::new);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i).orElseGet(CompoundTag::new);
                data.trades.add(ShopTrade.fromNbt(c, registries));
            }
        }

        if (nbt.contains("storage")) {
            ListTag list = nbt.getList("storage").orElseGet(ListTag::new);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag c = list.getCompound(i).orElseGet(CompoundTag::new);
                ItemStack stack = itemStackFromNbt(c, registries);
                if (!stack.isEmpty()) {
                    data.storage.add(stack);
                }
            }
        }

        return data;
    }

    // ========================================================================
    // Helper: Extract location string from ResourceKey.toString()
    // Handles "ResourceKey[<registry> / <location>]" format
    // ========================================================================
    private static String extractLocationString(Object resourceKey) {
        String s = resourceKey.toString();
        // Remove outer wrapper: "ResourceKey[...]" -> "..."
        if (s.startsWith("ResourceKey[") && s.endsWith("]")) {
            s = s.substring(12, s.length() - 1);
        }
        // Split by " / " and take the last part (the location)
        int idx = s.lastIndexOf(" / ");
        if (idx >= 0) {
            return s.substring(idx + 3);
        }
        return s;
    }

    // ========================================================================
    // itemStackToNbt / itemStackFromNbt - Full support for enchanted items
    // ========================================================================

    /**
     * Serialize enchantments manually to ListTag.
     * Uses Holder.Reference.key() to get ResourceKey for each enchantment.
     */
    private static void serializeEnchantmentsToNbt(ItemEnchantments enchants, CompoundTag nbt, String tagName) {
        if (enchants == null || enchants.isEmpty()) return;
        try {
            ListTag list = new ListTag();
            for (var entry : enchants.entrySet()) {
                try {
                    Holder<Enchantment> enchHolder = entry.getKey();
                    int level = entry.getIntValue();
                    String enchId = "";

                    // Get enchantment ID from Holder.Reference.key()
                    if (enchHolder instanceof Holder.Reference) {
                        try {
                            enchId = extractLocationString(((Holder.Reference<Enchantment>) enchHolder).key());
                        } catch (Exception ignored) {}
                    }

                    if (enchId.isEmpty()) continue;
                    CompoundTag ec = new CompoundTag();
                    ec.putString("id", enchId);
                    ec.putInt("lvl", level);
                    list.add(ec);
                } catch (Exception ignored) {}
            }
            if (!list.isEmpty()) nbt.put(tagName, list);
        } catch (Exception ignored) {}
    }

    /**
     * Deserialize enchantments from ListTag and apply to stack.
     * Uses registry iteration to find enchantments by matching string IDs.
     */
    private static void deserializeEnchantmentsFromNbt(CompoundTag nbt, String tagName, ItemStack stack, DataComponentType<ItemEnchantments> componentType, HolderLookup.Provider registries) {
        try {
            if (!nbt.contains(tagName)) return;
            ListTag list = nbt.getList(tagName).orElseGet(ListTag::new);
            if (list.isEmpty()) return;

            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

            // Look up each enchantment individually via ResourceKey + RegistryLookup.get()
            for (int i = 0; i < list.size(); i++) {
                CompoundTag ec = list.getCompound(i).orElseGet(CompoundTag::new);
                String enchId = ec.getString("id").orElse("");
                int level = ec.getInt("lvl").orElse(1);
                if (enchId.isEmpty()) continue;

                try {
                    int colonIdx = enchId.indexOf(':');
                    String ns = colonIdx >= 0 ? enchId.substring(0, colonIdx) : "minecraft";
                    String path = colonIdx >= 0 ? enchId.substring(colonIdx + 1) : enchId;
                    Identifier enchIdentifier = Identifier.fromNamespaceAndPath(ns, path);
                    ResourceKey<Enchantment> enchKey = ResourceKey.create(Registries.ENCHANTMENT, enchIdentifier);
                    boolean added = false;

                    // Try primary registries
                    if (registries != null) {
                        try {
                            var enchReg = registries.lookup(Registries.ENCHANTMENT);
                            if (enchReg.isPresent()) {
                                var holder = enchReg.get().get(enchKey);
                                if (holder.isPresent()) {
                                    mutable.set(holder.get(), level);
                                    added = true;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    // Fallback to cached registries
                    if (!added && ShopManager.getCachedRegistries() != null) {
                        try {
                            var enchReg2 = ShopManager.getCachedRegistries().lookup(Registries.ENCHANTMENT);
                            if (enchReg2.isPresent()) {
                                var holder2 = enchReg2.get().get(enchKey);
                                if (holder2.isPresent()) {
                                    mutable.set(holder2.get(), level);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }

            ItemEnchantments built = mutable.toImmutable();
            if (!built.isEmpty()) {
                stack.set(componentType, built);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Convert ItemStack to NBT.
     * For enchanted items (books, weapons, armor): full manual serialization.
     * For normal items: original Codec.
     */
    public static CompoundTag itemStackToNbt(ItemStack stack) {
        if (stack.isEmpty()) return new CompoundTag();

        // Check for enchantment components
        ItemEnchantments storedEnch = stack.get(DataComponents.STORED_ENCHANTMENTS);
        ItemEnchantments ench = stack.get(DataComponents.ENCHANTMENTS);
        boolean hasStored = (storedEnch != null && !storedEnch.isEmpty());
        boolean hasEnchants = (ench != null && !ench.isEmpty());

        // For any item with enchantments: full manual serialization
        if (hasStored || hasEnchants) {
            CompoundTag nbt = new CompoundTag();
            // Use var to avoid dependency on ResourceLocation class name
            var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            nbt.putString("id", itemId.toString());
            nbt.putInt("count", stack.getCount());

            // Stored enchantments (enchanted books)
            if (hasStored) {
                serializeEnchantmentsToNbt(storedEnch, nbt, "stored_enchantments");
            }
            // Regular enchantments (weapons, armor)
            if (hasEnchants) {
                serializeEnchantmentsToNbt(ench, nbt, "enchantments");
            }

            return nbt;
        }

        // For normal items: use the original Codec
        var result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack);
        return result.result()
            .filter(e -> e instanceof CompoundTag)
            .map(e -> (CompoundTag) e)
            .orElseGet(CompoundTag::new);
    }

    /**
     * Restore ItemStack from NBT with full enchantment support.
     * Supports: Official Mapping manual format, Codec format, and legacy Yarn migration.
     */
    public static ItemStack itemStackFromNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        if (nbt == null || nbt.isEmpty()) return ItemStack.EMPTY;

        // Path 1: Manual format (Official Mapping - enchanted items with top-level keys)
        if (nbt.contains("id") && (nbt.contains("stored_enchantments") || nbt.contains("enchantments"))) {
            String idStr = nbt.getString("id").orElse("");
            int count = nbt.getInt("count").orElse(1);
            if (idStr.isEmpty()) return ItemStack.EMPTY;

            Item foundItem = resolveItemFromRegistry(idStr);
            if (foundItem == null || foundItem == Items.AIR) return ItemStack.EMPTY;

            ItemStack stack = new ItemStack(foundItem, count);
            if (nbt.contains("stored_enchantments")) {
                deserializeEnchantmentsFromNbt(nbt, "stored_enchantments", stack, DataComponents.STORED_ENCHANTMENTS, registries);
            }
            if (nbt.contains("enchantments")) {
                deserializeEnchantmentsFromNbt(nbt, "enchantments", stack, DataComponents.ENCHANTMENTS, registries);
            }
            return stack;
        }

        // Path 2: Standard Codec format (handles normal items + Yarn-compatible data)
        ItemStack codecResult = ItemStack.CODEC.parse(NbtOps.INSTANCE, nbt)
            .result()
            .orElse(ItemStack.EMPTY);
        if (!codecResult.isEmpty()) return codecResult;

        // Path 3: Legacy Yarn migration - codec data that fails to parse
        // Extract item id/count and try to manually restore enchantments from components
        if (nbt.contains("id")) {
            String idStr = nbt.getString("id").orElse("");
            int count = nbt.getInt("count").orElse(1);
            if (!idStr.isEmpty()) {
                Item foundItem = resolveItemFromRegistry(idStr);
                if (foundItem != null && foundItem != Items.AIR) {
                    ItemStack stack = new ItemStack(foundItem, count);
                    CompoundTag components = nbt.getCompound("components").orElseGet(CompoundTag::new);
                    if (!components.isEmpty()) {
                        // Try to extract stored enchantments from "minecraft:stored_enchantments" levels
                        tryMigrateEnchantmentsFromComponents(components, "minecraft:stored_enchantments", stack, DataComponents.STORED_ENCHANTMENTS, registries);
                        tryMigrateEnchantmentsFromComponents(components, "minecraft:enchantments", stack, DataComponents.ENCHANTMENTS, registries);
                    }
                    return stack;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Resolve an Item from a registry identifier string (e.g., "minecraft:diamond_sword").
     */
    private static Item resolveItemFromRegistry(String idStr) {
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).toString().equals(idStr)) {
                return item;
            }
        }
        return Items.AIR;
    }

    /**
     * Legacy Yarn migration: extract enchantments from a "levels" compound
     * inside a component tag (Codec format: {"levels": {"minecraft:sharpness": 5}}).
     */
    private static void tryMigrateEnchantmentsFromComponents(CompoundTag components, String componentKey,
            ItemStack stack, DataComponentType<ItemEnchantments> componentType, HolderLookup.Provider registries) {
        try {
            if (!components.contains(componentKey)) return;
            CompoundTag enchComp = components.getCompound(componentKey).orElseGet(CompoundTag::new);
            if (enchComp.isEmpty()) return;

            if (enchComp.contains("levels")) {
                CompoundTag levels = enchComp.getCompound("levels").orElseGet(CompoundTag::new);
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                iterateCompoundKeys(levels, (enchId, level) -> {
                    applyEnchantmentById(enchId, level, mutable, registries);
                });
                ItemEnchantments built = mutable.toImmutable();
                if (!built.isEmpty()) {
                    stack.set(componentType, built);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Iterate over CompoundTag keys using multiple possible methods (for Bedrock compatibility).
     */
    @FunctionalInterface
    private interface KeyConsumer { void accept(String key, int value); }

    private static void iterateCompoundKeys(CompoundTag tag, KeyConsumer consumer) {
        for (String k : tag.keySet()) {
            consumer.accept(k, tag.getInt(k).orElse(1));
        }
    }

    /**
     * Look up an enchantment by string ID and add it to the mutable enchantments.
     */
    private static void applyEnchantmentById(String enchId, int level, ItemEnchantments.Mutable mutable, HolderLookup.Provider registries) {
        try {
            int colonIdx = enchId.indexOf(':');
            String ns = colonIdx >= 0 ? enchId.substring(0, colonIdx) : "minecraft";
            String path = colonIdx >= 0 ? enchId.substring(colonIdx + 1) : enchId;
            Identifier enchIdentifier = Identifier.fromNamespaceAndPath(ns, path);
            ResourceKey<Enchantment> enchKey = ResourceKey.create(Registries.ENCHANTMENT, enchIdentifier);

            if (registries != null) {
                try {
                    var enchReg = registries.lookup(Registries.ENCHANTMENT);
                    if (enchReg.isPresent()) {
                        var holder = enchReg.get().get(enchKey);
                        if (holder.isPresent()) { mutable.set(holder.get(), level); return; }
                    }
                } catch (Exception ignored) {}
            }
            if (ShopManager.getCachedRegistries() != null) {
                try {
                    var enchReg2 = ShopManager.getCachedRegistries().lookup(Registries.ENCHANTMENT);
                    if (enchReg2.isPresent()) {
                        var holder2 = enchReg2.get().get(enchKey);
                        if (holder2.isPresent()) { mutable.set(holder2.get(), level); }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
