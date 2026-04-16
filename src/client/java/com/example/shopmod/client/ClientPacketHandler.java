package com.example.shopmod.client;

import com.example.shopmod.data.ShopData;
import com.example.shopmod.network.ModPackets;
import com.example.shopmod.screen.AmountInputScreen;
import com.example.shopmod.screen.ItemPickerScreen;
import com.example.shopmod.screen.ShopBuyerScreen;
import com.example.shopmod.screen.ShopListScreen;
import com.example.shopmod.screen.ShopOwnerScreen;
import com.example.shopmod.screen.StorageScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Environment(EnvType.CLIENT)
public final class ClientPacketHandler {

    public static void register() {

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_SHOPS_LIST_ID,
            (payload, ctx) -> {
                // Populate navigation holder with shop list
                List<String> ownerNames = new ArrayList<>();
                for (ModPackets.ShopEntryInfo info : payload.shops()) {
                    ownerNames.add(info.ownerName());
                }
                ShopNavigationHolder.setShops(Collections.unmodifiableList(ownerNames));

                ctx.client().execute(() -> {
                    ctx.client().setScreen(new ShopListScreen(payload));
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_OWNER_ID,
            (payload, ctx) -> {
                HolderLookup.Provider reg = ctx.client().level != null ? ctx.client().level.registryAccess() : null;
                ShopData data = ShopData.fromNbt(payload.data(), reg);
                ctx.client().execute(() -> {
                    ShopOwnerScreen screen = new ShopOwnerScreen(payload.shopName(), data);
                    // Check for pending sell item - DON'T clear, it persists until Add Offer
                    if (PendingSellHolder.shopName != null
                            && PendingSellHolder.shopName.equals(payload.shopName())
                            && PendingSellHolder.sellItem != null) {
                        screen.setPendingSellItem(PendingSellHolder.sellItem);
                    }
                    // Check for pending buy item - DON'T clear, it persists until Add Offer
                    if (PendingBuyHolder.shopName != null
                            && PendingBuyHolder.shopName.equals(payload.shopName())
                            && PendingBuyHolder.buyItem != null) {
                        screen.setPendingBuyItem(PendingBuyHolder.buyItem);
                    }
                    ctx.client().setScreen(screen);
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_BUYER_ID,
            (payload, ctx) -> {
                HolderLookup.Provider reg = ctx.client().level != null ? ctx.client().level.registryAccess() : null;
                ShopData data = ShopData.fromNbt(payload.data(), reg);
                ctx.client().execute(() -> {
                    if (ctx.client().screen instanceof ShopBuyerScreen s) {
                        s.refreshData(data);
                    } else {
                        ctx.client().setScreen(new ShopBuyerScreen(payload.shopName(), data));
                    }
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_PICKER_ID,
            (payload, ctx) ->
                ctx.client().execute(() ->
                    ctx.client().setScreen(new ItemPickerScreen(payload.shopName()))));

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_AMOUNT_ID,
            (payload, ctx) ->
                ctx.client().execute(() ->
                    ctx.client().setScreen(new AmountInputScreen(payload.shopName(), payload.itemId()))));

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.SYNC_SHOP_ID,
            (payload, ctx) -> {
                HolderLookup.Provider reg = ctx.client().level != null ? ctx.client().level.registryAccess() : null;
                ShopData data = ShopData.fromNbt(payload.data(), reg);
                ctx.client().execute(() -> {
                    if (ctx.client().screen instanceof ShopOwnerScreen s) {
                        s.refreshData(data);
                    }
                });
            });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_STORAGE_ID,
            (payload, ctx) -> {
                HolderLookup.Provider reg = ctx.client().level != null ? ctx.client().level.registryAccess() : null;
                ShopData data = ShopData.fromNbt(payload.data(), reg);
                ctx.client().execute(() -> {
                    if (ctx.client().screen instanceof StorageScreen s) {
                        s.refreshData(data);
                    } else {
                        ctx.client().setScreen(new StorageScreen(payload.shopName(), data));
                    }
                });
            });

        // Shop navigation data - update holder and re-init current shop screen
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.SHOP_NAV_ID,
            (payload, ctx) -> {
                ShopNavigationHolder.setShops(payload.ownerNames());
                // Re-init the current shop screen so navigation arrows appear
                ctx.client().execute(() -> {
                    if (ctx.client().screen instanceof ShopBuyerScreen s) {
                        s.rebuild();
                    } else if (ctx.client().screen instanceof ShopOwnerScreen s) {
                        s.rebuild();
                    }
                });
            });
    }

    public static final class PendingBuyHolder {
        public static ItemStack buyItem  = null;
        public static String    shopName = null;
        public static void clear() { buyItem = null; shopName = null; }
        private PendingBuyHolder() {}
    }

    public static final class PendingSellHolder {
        public static ItemStack sellItem = null;
        public static String    shopName = null;
        public static void clear() { sellItem = null; shopName = null; }
        private PendingSellHolder() {}
    }

    /**
     * Holds the shop owner names list for navigation arrows
     * in ShopOwnerScreen and ShopBuyerScreen.
     */
    public static final class ShopNavigationHolder {
        private static List<String> shopOwnerNames = List.of();

        public static void setShops(List<String> owners) {
            shopOwnerNames = owners != null ? owners : List.of();
        }

        public static List<String> getShops() {
            return shopOwnerNames;
        }

        public static boolean isEmpty() {
            return shopOwnerNames.isEmpty();
        }

        public static int getCurrentIndex(String shopName) {
            return shopOwnerNames.indexOf(shopName);
        }

        public static String getPrevShop(String currentShop) {
            if (shopOwnerNames.size() <= 1) return null;
            int idx = getCurrentIndex(currentShop);
            if (idx <= 0) return shopOwnerNames.get(shopOwnerNames.size() - 1);
            return shopOwnerNames.get(idx - 1);
        }

        public static String getNextShop(String currentShop) {
            if (shopOwnerNames.size() <= 1) return null;
            int idx = getCurrentIndex(currentShop);
            if (idx < 0 || idx >= shopOwnerNames.size() - 1) return shopOwnerNames.get(0);
            return shopOwnerNames.get(idx + 1);
        }

        public static void clear() {
            shopOwnerNames = List.of();
        }

        private ShopNavigationHolder() {}
    }
}
