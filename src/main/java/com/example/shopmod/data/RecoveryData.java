package com.example.shopmod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecoveryData {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShopMod");

    public static Path getRecoveryDir() {
        Path dir = Paths.get("config", "Easy Shop Mod", "Recovery");
        try { Files.createDirectories(dir); } catch (IOException e) {
            LOGGER.warn("[ShopMod] Failed to create recovery directory: {}", e.getMessage());
        }
        return dir;
    }

    public static Path getRecoveryFile(UUID uuid) {
        return getRecoveryDir().resolve(uuid.toString() + ".dat");
    }

    public static boolean hasRecoveryData(UUID uuid) {
        return Files.exists(getRecoveryFile(uuid));
    }

    /**
     * Save items to recovery file as NBT.
     */
    public static void save(UUID uuid, List<ItemStackWithSource> items, HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        root.putLong("uuid_most", uuid.getMostSignificantBits());
        root.putLong("uuid_least", uuid.getLeastSignificantBits());
        ListTag itemsList = new ListTag();
        for (ItemStackWithSource item : items) {
            CompoundTag entry = new CompoundTag();
            entry.put("item", ShopData.itemStackToNbt(item.stack));
            entry.putString("source", item.source);
            itemsList.add(entry);
        }
        root.put("items", itemsList);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.write(root, new DataOutputStream(baos));
            Files.write(getRecoveryFile(uuid), baos.toByteArray(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("[ShopMod] Failed to save recovery data for {}: {}", uuid, e.getMessage());
        }
    }

    /**
     * Load all recovery items from file. Returns null if file doesn't exist.
     */
    public static List<ItemStackWithSource> load(UUID uuid, HolderLookup.Provider registries) {
        Path file = getRecoveryFile(uuid);
        if (!Files.exists(file)) return null;
        try {
            byte[] data = Files.readAllBytes(file);
            Tag tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(data)));
            if (!(tag instanceof CompoundTag root)) return null;

            List<ItemStackWithSource> items = new ArrayList<>();
            if (root.contains("items")) {
                ListTag itemsList = root.getList("items").orElseGet(ListTag::new);
                for (int i = 0; i < itemsList.size(); i++) {
                    CompoundTag entry = itemsList.getCompound(i).orElseGet(CompoundTag::new);
                    CompoundTag itemTag = entry.getCompound("item").orElseGet(CompoundTag::new);
                    ItemStack stack = ShopData.itemStackFromNbt(itemTag, registries);
                    String source = entry.getString("source").orElse("offer");
                    if (!stack.isEmpty()) {
                        items.add(new ItemStackWithSource(stack, source));
                    }
                }
            }
            return items;
        } catch (IOException e) {
            LOGGER.error("[ShopMod] Failed to load recovery data for {}: {}", uuid, e.getMessage());
            return null;
        }
    }

    /**
     * Remove a specific item by index and save.
     */
    public static void removeItem(UUID uuid, int index, HolderLookup.Provider registries) {
        List<ItemStackWithSource> items = load(uuid, registries);
        if (items == null || index < 0 || index >= items.size()) return;
        items.remove(index);
        if (items.isEmpty()) {
            deleteFile(uuid);
        } else {
            save(uuid, items, registries);
        }
    }

    public static boolean isEmpty(UUID uuid) {
        return !hasRecoveryData(uuid);
    }

    public static void deleteFile(UUID uuid) {
        try { Files.deleteIfExists(getRecoveryFile(uuid)); } catch (IOException ignored) {}
    }

    /**
     * Serialize a list of recovery items into a CompoundTag for network transmission.
     */
    public static CompoundTag serializeForNetwork(List<ItemStackWithSource> items, HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemStackWithSource item : items) {
            CompoundTag entry = new CompoundTag();
            entry.put("item", ShopData.itemStackToNbt(item.stack));
            entry.putString("source", item.source);
            list.add(entry);
        }
        root.put("items", list);
        return root;
    }

    /**
     * Deserialize recovery items from a network CompoundTag.
     */
    public static List<ItemStackWithSource> deserializeFromNetwork(CompoundTag root, HolderLookup.Provider registries) {
        List<ItemStackWithSource> items = new ArrayList<>();
        if (root.contains("items")) {
            ListTag list = root.getList("items").orElseGet(ListTag::new);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i).orElseGet(CompoundTag::new);
                CompoundTag itemTag = entry.getCompound("item").orElseGet(CompoundTag::new);
                ItemStack stack = ShopData.itemStackFromNbt(itemTag, registries);
                String source = entry.getString("source").orElse("offer");
                if (!stack.isEmpty()) {
                    items.add(new ItemStackWithSource(stack, source));
                }
            }
        }
        return items;
    }

    public record ItemStackWithSource(ItemStack stack, String source) {}
}