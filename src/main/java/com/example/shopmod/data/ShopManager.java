package com.example.shopmod.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;

import java.util.*;

public class ShopManager extends SavedData {

    private static final Identifier KEY = Identifier.fromNamespaceAndPath("shopmod", "shops");

    private static volatile HolderLookup.Provider cachedRegistries = null;

    public static void setCachedRegistries(HolderLookup.Provider reg) { cachedRegistries = reg; }
    public static HolderLookup.Provider getCachedRegistries() { return cachedRegistries; }

    private final Map<String, ShopData> shops  = new LinkedHashMap<>();
    private final Map<String, UUID>     npcIds = new HashMap<>();

    public ShopManager() {}

    private static final Codec<ShopManager> CODEC = new Codec<ShopManager>() {
        @Override
        public <T> DataResult<Pair<ShopManager, T>> decode(DynamicOps<T> ops, T input) {
            try {
                Tag nbtElement = ops.convertTo(NbtOps.INSTANCE, input);
                if (nbtElement instanceof CompoundTag nbt) {
                    HolderLookup.Provider registries = null;
                    try {
                        for (java.lang.reflect.Method m : ops.getClass().getMethods()) {
                            if (m.getParameterCount() == 0
                                && m.getReturnType().isAssignableFrom(HolderLookup.Provider.class)) {
                                Object lookup = m.invoke(ops);
                                if (lookup instanceof HolderLookup.Provider wl) {
                                    registries = wl;
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    if (registries == null) {
                        registries = cachedRegistries;
                    }
                    return DataResult.success(Pair.of(fromNbtInternal(nbt, registries), input));
                }
                return DataResult.success(Pair.of(new ShopManager(), input));
            } catch (Exception e) {
                return DataResult.success(Pair.of(new ShopManager(), input));
            }
        }

        @Override
        public <T> DataResult<T> encode(ShopManager input, DynamicOps<T> ops, T prefix) {
            try {
                CompoundTag nbt = new CompoundTag();
                input.writeData(nbt);
                T result = NbtOps.INSTANCE.convertTo(ops, nbt);
                return DataResult.success(result);
            } catch (Exception e) {
                return DataResult.success(prefix);
            }
        }
    };

    private static final SavedDataType<ShopManager> TYPE =
        new SavedDataType<>(KEY, ShopManager::new, CODEC, null);

    public static ShopManager get(MinecraftServer server) {
        cachedRegistries = server.registryAccess();
        // In MC 1.21.11, getOrCreate() was replaced by computeIfAbsent()
        return server.overworld()
                     .getDataStorage()
                     .computeIfAbsent(TYPE);
    }

    public ShopData getOrCreate(String owner) { return shops.computeIfAbsent(owner, ShopData::new); }
    public ShopData  get(String owner)        { return shops.get(owner); }
    public boolean   hasShop(String owner)    { return shops.containsKey(owner); }
    public Set<String> getAllOwners()         { return shops.keySet(); }

    public void    setNpcId(String owner, UUID id) { npcIds.put(owner, id); setDirty(); }
    public UUID    getNpcId(String owner)           { return npcIds.get(owner); }
    public boolean hasNpc(String owner)             { return npcIds.containsKey(owner); }
    public void    removeNpc(String owner)          { npcIds.remove(owner); setDirty(); }

    // NOTE: save() override methods were removed in MC 1.21.11.
    // SavedData no longer has save() methods - serialization is handled entirely by the Codec.

    private CompoundTag writeData(CompoundTag nbt) {
        CompoundTag shopMap = new CompoundTag();
        shops.forEach((k, v) -> shopMap.put(k, v.toNbt()));
        nbt.put("shops", shopMap);

        CompoundTag npcMap = new CompoundTag();
        npcIds.forEach((k, v) -> {
            CompoundTag e = new CompoundTag();
            e.putLong("most",  v.getMostSignificantBits());
            e.putLong("least", v.getLeastSignificantBits());
            npcMap.put(k, e);
        });
        nbt.put("npcs", npcMap);
        return nbt;
    }

    private static ShopManager fromNbtInternal(CompoundTag nbt, HolderLookup.Provider registries) {
        ShopManager m = new ShopManager();
        if (nbt.contains("shops")) {
            CompoundTag shopMap = nbt.getCompound("shops").orElseGet(CompoundTag::new);
            for (String k : shopMap.keySet()) {
                CompoundTag c = shopMap.getCompound(k).orElseGet(CompoundTag::new);
                m.shops.put(k, ShopData.fromNbt(c, registries));
            }
        }
        if (nbt.contains("npcs")) {
            CompoundTag npcMap = nbt.getCompound("npcs").orElseGet(CompoundTag::new);
            for (String k : npcMap.keySet()) {
                CompoundTag e = npcMap.getCompound(k).orElseGet(CompoundTag::new);
                long most  = e.getLong("most").orElse(0L);
                long least = e.getLong("least").orElse(0L);
                m.npcIds.put(k, new UUID(most, least));
            }
        }
        return m;
    }
}
