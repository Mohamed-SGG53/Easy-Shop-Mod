package com.example.shopmod.data;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

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

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.put("sell", ShopData.itemStackToNbt(sellItem));
            nbt.put("buy",  ShopData.itemStackToNbt(buyItem));
            return nbt;
        }

        public static ShopTrade fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            ItemStack sell = ShopData.itemStackFromNbt(nbt.getCompound("sell").orElseGet(NbtCompound::new), registries);
            ItemStack buy  = ShopData.itemStackFromNbt(nbt.getCompound("buy").orElseGet(NbtCompound::new), registries);
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

    public NbtCompound toNbt() {
        NbtCompound nbt  = new NbtCompound();
        nbt.putString("owner", ownerName);
        if (ownerUuid != null) {
            nbt.putLong("owner_uuid_most", ownerUuid.getMostSignificantBits());
            nbt.putLong("owner_uuid_least", ownerUuid.getLeastSignificantBits());
        }
        
        NbtList tradeList = new NbtList();
        for (ShopTrade t : trades) tradeList.add(t.toNbt());
        nbt.put("trades", tradeList);
        
        NbtList storageList = new NbtList();
        for (ItemStack s : storage) {
            storageList.add(itemStackToNbt(s));
        }
        nbt.put("storage", storageList);
        
        return nbt;
    }

    public static ShopData fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        String owner = nbt.getString("owner").orElse("unknown");
        ShopData data = new ShopData(owner);

        long uuidMost = nbt.getLong("owner_uuid_most", 0L);
        long uuidLeast = nbt.getLong("owner_uuid_least", 0L);
        if (uuidMost != 0 || uuidLeast != 0) {
            data.ownerUuid = new UUID(uuidMost, uuidLeast);
        }
        
        nbt.getList("trades").ifPresent(list -> {
            for (int i = 0; i < list.size(); i++) {
                list.getCompound(i).ifPresent(c -> data.trades.add(ShopTrade.fromNbt(c, registries)));
            }
        });
        
        nbt.getList("storage").ifPresent(list -> {
            for (int i = 0; i < list.size(); i++) {
                list.getCompound(i).ifPresent(c -> {
                    ItemStack stack = itemStackFromNbt(c, registries);
                    if (!stack.isEmpty()) {
                        data.storage.add(stack);
                    }
                });
            }
        });
        
        return data;
    }

    // ========================================================================
    // itemStackToNbt / itemStackFromNbt - تدعم كل الآيتمات المطورة (كتب + أسلحة + دروع)
    // ========================================================================

    /**
     * تسلسل التعاويذ يدوياً إلى NbtList
     */
    private static void serializeEnchantmentsToNbt(ItemEnchantmentsComponent enchants, NbtCompound nbt, String tagName) {
        if (enchants == null || enchants.isEmpty()) return;
        try {
            NbtList list = new NbtList();
            for (var entry : enchants.getEnchantments()) {
                try {
                    Optional<? extends RegistryKey<Enchantment>> keyOpt = entry.getKey();
                    if (keyOpt.isPresent()) {
                        String enchId = keyOpt.get().getValue().toString();
                        int level = enchants.getLevel(entry);
                        NbtCompound ec = new NbtCompound();
                        ec.putString("id", enchId);
                        ec.putInt("lvl", level);
                        list.add(ec);
                    }
                } catch (Exception ignored) {}
            }
            if (!list.isEmpty()) nbt.put(tagName, list);
        } catch (Exception ignored) {}
    }

    /**
     * فك تسلسل التعاويذ من NbtList وإضافتها للـ stack
     */
    private static void deserializeEnchantmentsFromNbt(NbtCompound nbt, String tagName, ItemStack stack, net.minecraft.component.ComponentType<ItemEnchantmentsComponent> componentType, RegistryWrapper.WrapperLookup registries) {
        try {
            Optional<NbtList> opt = nbt.getList(tagName);
            if (!opt.isPresent()) return;
            NbtList list = opt.get();
            if (list.isEmpty()) return;

            ItemEnchantmentsComponent.Builder builder =
                new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);

            for (int i = 0; i < list.size(); i++) {
                Optional<NbtCompound> ecOpt = list.getCompound(i);
                if (ecOpt.isEmpty()) continue;
                NbtCompound ec = ecOpt.get();
                String enchId = ec.getString("id").orElse("");
                int level = ec.getInt("lvl", 1);
                if (enchId.isEmpty()) continue;

                try {
                    Identifier enchIdentifier = Identifier.of(enchId);
                    boolean added = false;

                    // الطريقة الأولى: من خلال registries (WrapperLookup)
                    if (registries != null) {
                        try {
                            RegistryKey<Enchantment> enchKey =
                                RegistryKey.of(RegistryKeys.ENCHANTMENT, enchIdentifier);
                            Optional<? extends RegistryWrapper.Impl<Enchantment>> enchReg =
                                registries.getOptional(RegistryKeys.ENCHANTMENT);
                            if (enchReg.isPresent()) {
                                Optional<? extends RegistryEntry<Enchantment>> entryOpt =
                                    enchReg.get().getOptional(enchKey);
                                if (entryOpt.isPresent()) {
                                    builder.add(entryOpt.get(), level);
                                    added = true;
                                }
                            }
                        } catch (Exception ignored) {}
                    }

                    // الطريقة الثانية: من cachedRegistries (fallback)
                    if (!added && ShopManager.getCachedRegistries() != null) {
                        try {
                            RegistryWrapper.WrapperLookup fallbackReg = ShopManager.getCachedRegistries();
                            RegistryKey<Enchantment> enchKey2 =
                                RegistryKey.of(RegistryKeys.ENCHANTMENT, enchIdentifier);
                            Optional<? extends RegistryWrapper.Impl<Enchantment>> enchReg2 =
                                fallbackReg.getOptional(RegistryKeys.ENCHANTMENT);
                            if (enchReg2.isPresent()) {
                                Optional<? extends RegistryEntry<Enchantment>> entryOpt2 =
                                    enchReg2.get().getOptional(enchKey2);
                                if (entryOpt2.isPresent()) {
                                    builder.add(entryOpt2.get(), level);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }

            ItemEnchantmentsComponent built = builder.build();
            if (!built.isEmpty()) {
                stack.set(componentType, built);
            }
        } catch (Exception ignored) {}
    }

    /**
     * تحويل ItemStack إلى NBT.
     * لأي آيتم فيه تطويرات (كتب مطورة + أسلحة + دروع): تسلسل يدوي كامل.
     * للأصناف العادية: Codec الأصلي.
     */
    public static NbtCompound itemStackToNbt(ItemStack stack) {
        if (stack.isEmpty()) return new NbtCompound();

        boolean hasStored = stack.contains(DataComponentTypes.STORED_ENCHANTMENTS);
        boolean hasEnchants = stack.contains(DataComponentTypes.ENCHANTMENTS);

        // ✅ لأي آيتم فيه تطويرات: تسلسل يدوي كامل
        if (hasStored || hasEnchants) {
            NbtCompound nbt = new NbtCompound();
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            nbt.putString("id", itemId.toString());
            nbt.putInt("count", stack.getCount());

            // تعاويذ الكتب المطورة (stored)
            if (hasStored) {
                serializeEnchantmentsToNbt(stack.get(DataComponentTypes.STORED_ENCHANTMENTS), nbt, "stored_enchantments");
            }
            // تعاويذ الأسلحة والدروع (enchantments)
            if (hasEnchants) {
                serializeEnchantmentsToNbt(stack.get(DataComponentTypes.ENCHANTMENTS), nbt, "enchantments");
            }

            return nbt;
        }

        // للأصناف العادية: Codec الأصلي
        var result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack);
        return result.result()
            .filter(e -> e instanceof NbtCompound)
            .map(e -> (NbtCompound) e)
            .orElseGet(NbtCompound::new);
    }

    /**
     * استرجاع ItemStack من NBT مع دعم كامل لكل الآيتمات المطورة.
     */
    public static ItemStack itemStackFromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        if (nbt == null || nbt.isEmpty()) return ItemStack.EMPTY;

        // ✅ للتنسيق اليدوي: أي آيتم فيه تطويرات (كتب + أسلحة + دروع)
        if (nbt.contains("id") && (nbt.contains("stored_enchantments") || nbt.contains("enchantments"))) {
            String idStr = nbt.getString("id").orElse("");
            int count = nbt.getInt("count", 1);

            if (idStr.isEmpty()) return ItemStack.EMPTY;
            Identifier itemId = Identifier.of(idStr);
            if (!Registries.ITEM.containsId(itemId)) return ItemStack.EMPTY;
            var item = Registries.ITEM.get(itemId);
            if (item == Items.AIR) return ItemStack.EMPTY;

            ItemStack stack = new ItemStack(item, count);

            // استرجاع تعاويذ الكتب المطورة (stored)
            if (nbt.contains("stored_enchantments")) {
                deserializeEnchantmentsFromNbt(nbt, "stored_enchantments", stack, DataComponentTypes.STORED_ENCHANTMENTS, registries);
            }
            // استرجاع تعاويذ الأسلحة والدروع
            if (nbt.contains("enchantments")) {
                deserializeEnchantmentsFromNbt(nbt, "enchantments", stack, DataComponentTypes.ENCHANTMENTS, registries);
            }

            return stack;
        }

        // للتنسيق العادي (Codec): فك بالـ Codec الأصلي
        return ItemStack.CODEC.parse(NbtOps.INSTANCE, nbt)
            .result()
            .orElse(ItemStack.EMPTY);
    }
}
