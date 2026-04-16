package com.example.shopmod.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.*;

public class ShopManager extends PersistentState {

    private static final String KEY = "shopmod_shops";

    // ✅ Cache الـ registry عشان متلخصش لما العالم بيحمل تاني
    private static volatile RegistryWrapper.WrapperLookup cachedRegistries = null;

    public static void setCachedRegistries(RegistryWrapper.WrapperLookup reg) { cachedRegistries = reg; }
    public static RegistryWrapper.WrapperLookup getCachedRegistries() { return cachedRegistries; }

    private final Map<String, ShopData> shops  = new LinkedHashMap<>();
    private final Map<String, UUID>     npcIds = new HashMap<>();

    public ShopManager() {}

    private static final Codec<ShopManager> CODEC = new Codec<ShopManager>() {
        @Override
        public <T> DataResult<Pair<ShopManager, T>> decode(DynamicOps<T> ops, T input) {
            try {
                NbtElement nbtElement = ops.convertTo(NbtOps.INSTANCE, input);
                if (nbtElement instanceof NbtCompound nbt) {
                    // ✅ نحاول نستخرج الـ registry من الـ ops عشان ندعم enchanted books
                    RegistryWrapper.WrapperLookup registries = null;
                    // الطريقة الأولى: Reflection لاستخراج registryLookup من ops
                    try {
                        for (java.lang.reflect.Method m : ops.getClass().getMethods()) {
                            if (m.getParameterCount() == 0
                                && m.getReturnType().isAssignableFrom(RegistryWrapper.WrapperLookup.class)) {
                                Object lookup = m.invoke(ops);
                                if (lookup instanceof RegistryWrapper.WrapperLookup wl) {
                                    registries = wl;
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                    // الطريقة الثانية: من الـ cachedRegistries (wakeup fallback)
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
                NbtCompound nbt = new NbtCompound();
                input.writeData(nbt);
                T result = NbtOps.INSTANCE.convertTo(ops, nbt);
                return DataResult.success(result);
            } catch (Exception e) {
                return DataResult.success(prefix);
            }
        }
    };

    private static final PersistentStateType<ShopManager> TYPE =
        new PersistentStateType<>(KEY, ShopManager::new, CODEC, null);

    public static ShopManager get(MinecraftServer server) {
        // ✅ نحفظ الـ registry قبل ما نحمّل البيانات عشان يبقى متاح للـ Codec decode
        cachedRegistries = server.getRegistryManager();
        return server.getWorld(World.OVERWORLD)
                     .getPersistentStateManager()
                     .getOrCreate(TYPE);
    }

    public ShopData getOrCreate(String owner) { return shops.computeIfAbsent(owner, ShopData::new); }
    public ShopData  get(String owner)        { return shops.get(owner); }
    public boolean   hasShop(String owner)    { return shops.containsKey(owner); }
    public Set<String> getAllOwners()         { return shops.keySet(); }

    public void    setNpcId(String owner, UUID id) { npcIds.put(owner, id); markDirty(); }
    public UUID    getNpcId(String owner)           { return npcIds.get(owner); }
    public boolean hasNpc(String owner)             { return npcIds.containsKey(owner); }
    public void    removeNpc(String owner)          { npcIds.remove(owner); markDirty(); }

    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        // ✅ نحفظ الـ registry عشان نستخدمه في التحميل القادم
        cachedRegistries = registries;
        return writeData(nbt);
    }

    public NbtCompound writeNbt(NbtCompound nbt) {
        return writeData(nbt);
    }

    private NbtCompound writeData(NbtCompound nbt) {
        NbtCompound shopMap = new NbtCompound();
        shops.forEach((k, v) -> shopMap.put(k, v.toNbt()));
        nbt.put("shops", shopMap);

        NbtCompound npcMap = new NbtCompound();
        npcIds.forEach((k, v) -> {
            NbtCompound e = new NbtCompound();
            e.putLong("most",  v.getMostSignificantBits());
            e.putLong("least", v.getLeastSignificantBits());
            npcMap.put(k, e);
        });
        nbt.put("npcs", npcMap);
        return nbt;
    }

    private static ShopManager fromNbtInternal(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ShopManager m = new ShopManager();
        nbt.getCompound("shops").ifPresent(shopMap ->
            shopMap.getKeys().forEach(k ->
                shopMap.getCompound(k).ifPresent(c -> m.shops.put(k, ShopData.fromNbt(c, registries)))));
        nbt.getCompound("npcs").ifPresent(npcMap ->
            npcMap.getKeys().forEach(k ->
                npcMap.getCompound(k).ifPresent(e -> {
                    long most  = e.getLong("most").orElse(0L);
                    long least = e.getLong("least").orElse(0L);
                    m.npcIds.put(k, new UUID(most, least));
                })));
        return m;
    }
}
