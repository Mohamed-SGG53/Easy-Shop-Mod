package com.example.shopmod;

import com.example.shopmod.command.ShopCommand;
import com.example.shopmod.data.I18n;
import com.example.shopmod.data.ShopData;
import com.example.shopmod.data.ShopManager;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.item.ItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ShopMod implements ModInitializer {
    public static final String MOD_ID = "shopmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ShopMod initializing...");

        // Register payload types
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_OWNER_ID,   ModPackets.OpenOwnerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_BUYER_ID,   ModPackets.OpenBuyerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_PICKER_ID,  ModPackets.OpenPickerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_AMOUNT_ID,  ModPackets.OpenAmountPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.SYNC_SHOP_ID,    ModPackets.SyncShopPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_STORAGE_ID, ModPackets.OpenStoragePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_SHOPS_LIST_ID, ModPackets.OpenShopsListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.SHOP_NAV_ID, ModPackets.ShopNavPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.ADD_TRADE_ID,        ModPackets.AddTradePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.ADD_BOOK_TRADE_ID,    ModPackets.AddEnchantedBookTradePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.REMOVE_TRADE_ID,     ModPackets.RemoveTradePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.DO_TRADE_ID,         ModPackets.DoTradePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.REQ_PICKER_ID,       ModPackets.ReqPickerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.REQ_AMOUNT_ID,       ModPackets.ReqAmountPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.REQ_OWNER_SCREEN_ID, ModPackets.ReqOwnerScreenPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.REQ_STORAGE_ID,      ModPackets.ReqStoragePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.TAKE_STORAGE_ID,     ModPackets.TakeStoragePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.OPEN_SHOP_FROM_LIST_ID, ModPackets.OpenShopFromListPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.CREATE_SHOP_FROM_LIST_ID, ModPackets.CreateShopFromListPayload.CODEC);

        CommandRegistrationCallback.EVENT.register(ShopCommand::register);
        registerServerPackets();
        LOGGER.info("ShopMod ready!");
    }

    // ========================================================================
    // Helper: resolve Item from registry identifier string
    // In MC 1.21.11, BuiltInRegistries.ITEM.get() returns Optional<Reference<Item>>
    // ========================================================================

    private static Item resolveItem(String idStr) {
        if (idStr == null || idStr.isEmpty()) return Items.AIR;
        // Look up item by iterating the registry (avoids Identifier.parse dependency)
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).toString().equals(idStr)) {
                return item;
            }
        }
        return Items.AIR;
    }

    private void registerServerPackets() {

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.ADD_TRADE_ID,
            (payload, ctx) -> {
                String owner   = payload.shopName();
                String sellId  = payload.sellId();
                int    sellCnt = payload.sellCount();
                String buyId   = payload.buyId();
                int    buyCnt  = payload.buyCount();
                ctx.server().execute(() -> {
                    ServerPlayer player = ctx.player();
                    if (!player.getName().getString().equals(owner)) return;

                    Item sellItem = resolveItem(sellId);
                    if (sellItem == Items.AIR) return;
                    ItemStack sellStack = new ItemStack(sellItem, sellCnt);

                    int found = countItem(player, sellStack);
                    if (found < sellCnt) {
                        player.displayClientMessage(Component.literal(
                            I18n.get("msg.not_enough_items", sellCnt, sellStack.getHoverName().getString(), found)), false);
                        return;
                    }

                    removeItems(player, sellStack.copy(), sellCnt);

                    Item buyItem = resolveItem(buyId);
                    if (buyItem == Items.AIR) return;
                    ItemStack buyStack = new ItemStack(buyItem, buyCnt);
                    ShopManager mgr = ShopManager.get(ctx.server());
                    mgr.getOrCreate(owner).addTrade(sellStack, buyStack);
                    mgr.setDirty();
                    syncShop(player, mgr.get(owner));

                    player.displayClientMessage(Component.literal(I18n.get("msg.offer_added")), false);
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.ADD_BOOK_TRADE_ID,
            (payload, ctx) -> {
                ctx.server().execute(() -> {
                    ServerPlayer player = ctx.player();
                    if (!player.getName().getString().equals(payload.shopName())) return;

                    HolderLookup.Provider registries = ctx.server().registryAccess();
                    ShopManager.setCachedRegistries(registries);
                    ItemStack sellStack = ShopData.itemStackFromNbt(payload.sellData(), registries);
                    ItemStack buyStack  = ShopData.itemStackFromNbt(payload.buyData(), registries);

                    if (sellStack.isEmpty()) {
                        player.displayClientMessage(Component.literal("\u00a7cFailed to read sell item!"), false);
                        return;
                    }
                    if (buyStack.isEmpty()) {
                        player.displayClientMessage(Component.literal("\u00a7cFailed to read price item!"), false);
                        return;
                    }

                    int needed = sellStack.getCount();
                    int found  = countItem(player, sellStack);
                    if (found < needed) {
                        player.displayClientMessage(Component.literal(
                            I18n.get("msg.not_enough_items", needed, sellStack.getHoverName().getString(), found)), false);
                        return;
                    }

                    removeItems(player, sellStack.copy(), needed);

                    ShopManager mgr = ShopManager.get(ctx.server());
                    mgr.getOrCreate(payload.shopName()).addTrade(sellStack, buyStack);
                    mgr.setDirty();
                    syncShop(player, mgr.get(payload.shopName()));
                    player.displayClientMessage(Component.literal(I18n.get("msg.offer_added")), false);
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REMOVE_TRADE_ID,
            (payload, ctx) -> {
                String owner = payload.shopName();
                int index    = payload.index();
                ctx.server().execute(() -> {
                    ServerPlayer player = ctx.player();
                    if (!player.getName().getString().equals(owner)) return;
                    ShopManager mgr = ShopManager.get(ctx.server());
                    ShopData data = mgr.get(owner);
                    if (data == null) return;

                    ShopData.ShopTrade removed = data.removeTrade(index);
                    if (removed != null && !removed.sellItem.isEmpty()) {
                        ItemStack toReturn = removed.sellItem.copy();
                        if (!player.getInventory().add(toReturn)) {
                            data.addToStorage(removed.sellItem.copy());
                            player.displayClientMessage(Component.literal(I18n.get("msg.offer_returned_storage")), false);
                        } else {
                            player.displayClientMessage(Component.literal(I18n.get("msg.offer_returned_inventory")), false);
                        }
                    }

                    mgr.setDirty();
                    syncShop(player, data);
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.DO_TRADE_ID,
            (payload, ctx) -> {
                String owner = payload.shopName();
                int index    = payload.index();
                ctx.server().execute(() -> {
                    ServerPlayer player = ctx.player();
                    ShopManager mgr = ShopManager.get(ctx.server());
                    ShopData data = mgr.get(owner);
                    if (data == null) return;
                    List<ShopData.ShopTrade> trades = data.getTrades();
                    if (index < 0 || index >= trades.size()) return;
                    ShopData.ShopTrade trade = trades.get(index);

                    ItemStack price  = trade.buyItem.copy();
                    int needed = price.getCount();
                    int found  = countItem(player, price);

                    if (found < needed) {
                        player.displayClientMessage(Component.literal(
                            I18n.get("msg.not_enough_money", needed, price.getHoverName().getString(), found)), false);
                        return;
                    }

                    removeItems(player, price, needed);
                    data.addToStorage(price.copy());

                    ItemStack reward = trade.sellItem.copy();
                    String rewardName = reward.getHoverName().getString();
                    int rewardCount = reward.getCount();

                    // In MC 1.21.11, spawnAtLocation needs (ServerLevel, ItemStack)
                    ServerLevel serverLevel = (ServerLevel) player.level();
                    if (!player.getInventory().add(reward)) {
                        serverLevel.addFreshEntity(new ItemEntity(serverLevel,
                            player.getX(), player.getEyeY() - 0.3, player.getZ(), reward));
                    }

                    data.removeTrade(index);
                    mgr.setDirty();

                    player.displayClientMessage(Component.literal(
                        I18n.get("msg.trade_success", rewardName + " x" + rewardCount)), true);

                    ServerPlayNetworking.send(player, new ModPackets.OpenBuyerPayload(owner, data.toNbt()));
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_PICKER_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                if (!player.getName().getString().equals(payload.shopName())) return;
                ServerPlayNetworking.send(player, new ModPackets.OpenPickerPayload(payload.shopName()));
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_AMOUNT_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                if (!player.getName().getString().equals(payload.shopName())) return;
                ServerPlayNetworking.send(player,
                    new ModPackets.OpenAmountPayload(payload.shopName(), payload.itemId()));
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_OWNER_SCREEN_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                if (!player.getName().getString().equals(payload.shopName())) return;
                sendOpenOwner(player, payload.shopName());
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_STORAGE_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                if (!player.getName().getString().equals(payload.shopName())) return;
                ShopManager mgr = ShopManager.get(ctx.server());
                ShopData data = mgr.get(payload.shopName());
                if (data != null) {
                    ServerPlayNetworking.send(player, new ModPackets.OpenStoragePayload(payload.shopName(), data.toNbt()));
                }
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.TAKE_STORAGE_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                if (!player.getName().getString().equals(payload.shopName())) return;
                ShopManager mgr = ShopManager.get(ctx.server());
                ShopData data = mgr.get(payload.shopName());
                if (data == null) return;

                ItemStack taken = data.takeFromStorage(payload.index());
                if (!taken.isEmpty()) {
                    String itemName = taken.getHoverName().getString();
                    int itemCount = taken.getCount();

                    if (!player.getInventory().add(taken)) {
                        data.addToStorage(taken);
                        player.displayClientMessage(Component.literal(I18n.get("msg.inventory_full")), false);
                    } else {
                        player.displayClientMessage(Component.literal(I18n.get("msg.trade_item_taken", itemName, itemCount)), false);
                    }
                    mgr.setDirty();
                    ServerPlayNetworking.send(player, new ModPackets.OpenStoragePayload(payload.shopName(), data.toNbt()));
                }
            }));

        // Open a specific shop from the shops list GUI
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_SHOP_FROM_LIST_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                String owner = payload.ownerName();
                String playerName = player.getName().getString();

                if (playerName.equals(owner)) {
                    sendOpenOwner(player, owner);
                } else {
                    sendOpenBuyer(player, owner);
                }
            }));

        // Create a shop from the shops list GUI
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.CREATE_SHOP_FROM_LIST_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                String playerName = player.getName().getString();
                ShopManager mgr = ShopManager.get(ctx.server());

                if (mgr.hasNpc(playerName)) {
                    player.displayClientMessage(Component.literal(I18n.get("msg.shop_exists")), false);
                    return;
                }

                ServerLevel world = (ServerLevel) player.level();
                // In MC 1.21.11, use relative(Direction) instead of offset(Direction)
                Villager npc = EntityType.VILLAGER.spawn(
                    world,
                    player.blockPosition().relative(player.getDirection()),
                    EntitySpawnReason.COMMAND
                );

                if (npc != null) {
                    npc.setCustomName(Component.literal(playerName + "'s Shop"));
                    npc.setCustomNameVisible(true);
                    npc.setNoAi(true);
                    npc.setInvulnerable(true);

                    double dx = player.getX() - npc.getX();
                    double dz = player.getZ() - npc.getZ();
                    float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                    npc.setYRot(yaw);
                    npc.setYHeadRot(yaw);

                    mgr.setNpcId(playerName, npc.getUUID());
                    ShopData data = mgr.getOrCreate(playerName);
                    data.setOwnerUuid(player.getUUID());
                    mgr.setDirty();

                    player.displayClientMessage(Component.literal(I18n.get("msg.shop_created", playerName)), false);

                    // Refresh the shop list after creating
                    sendShopsList(player);
                } else {
                    player.displayClientMessage(Component.literal("Failed to spawn shop NPC"), false);
                }
            }));
    }

    public static void syncShop(ServerPlayer player, ShopData data) {
        if (data == null) return;
        ServerPlayNetworking.send(player, new ModPackets.SyncShopPayload(data.toNbt()));
    }

    public static void sendOpenOwner(ServerPlayer player, String owner) {
        MinecraftServer server = player.createCommandSourceStack().getServer();
        ShopManager mgr = ShopManager.get(server);
        ShopData data   = mgr.getOrCreate(owner);
        // Auto-save UUID if missing and owner is online
        if (data.getOwnerUuid() == null) {
            ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner);
            if (ownerPlayer != null) {
                data.setOwnerUuid(ownerPlayer.getUUID());
                mgr.setDirty();
            }
        }
        ServerPlayNetworking.send(player, new ModPackets.OpenOwnerPayload(owner, data.toNbt()));
        sendShopNav(player, server, mgr);
    }

    public static void sendOpenBuyer(ServerPlayer player, String owner) {
        MinecraftServer server = player.createCommandSourceStack().getServer();
        ShopManager mgr = ShopManager.get(server);
        ShopData data   = mgr.get(owner);
        if (data == null || data.getTrades().isEmpty()) {
            player.displayClientMessage(Component.literal(I18n.get("buyer.empty")), false);
            return;
        }
        // Auto-save UUID if missing and owner is online
        if (data.getOwnerUuid() == null) {
            ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner);
            if (ownerPlayer != null) {
                data.setOwnerUuid(ownerPlayer.getUUID());
                mgr.setDirty();
            }
        }
        ServerPlayNetworking.send(player, new ModPackets.OpenBuyerPayload(owner, data.toNbt()));
        sendShopNav(player, server, mgr);
    }

    /** Send navigation data (list of all shop owners) to the client */
    private static void sendShopNav(ServerPlayer player, MinecraftServer server, ShopManager mgr) {
        String playerName = player.getName().getString();
        Set<String> owners = mgr.getAllOwners();
        List<String> orderedOwners = new ArrayList<>();

        // Player's own shop first
        if (owners.contains(playerName)) {
            orderedOwners.add(playerName);
        }
        // Then all other shops
        for (String o : owners) {
            if (!o.equals(playerName)) {
                orderedOwners.add(o);
            }
        }

        ServerPlayNetworking.send(player, new ModPackets.ShopNavPayload(orderedOwners));
    }

    public static void sendShopsList(ServerPlayer player) {
        String playerName = player.getName().getString();
        MinecraftServer server = ((net.minecraft.server.level.ServerLevel) player.level()).getServer();
        ShopManager mgr = ShopManager.get(server);
        Set<String> owners = mgr.getAllOwners();
        boolean playerHasShop = mgr.hasShop(playerName);

        List<ModPackets.ShopEntryInfo> shopEntries = new ArrayList<>();

        // Player's own shop first
        if (playerHasShop) {
            ShopData ownData = mgr.get(playerName);
            // Auto-save UUID for old shops that don't have it
            if (ownData != null && ownData.getOwnerUuid() == null) {
                ownData.setOwnerUuid(player.getUUID());
                mgr.setDirty();
            }
            UUID ownUuid = player.getUUID();
            shopEntries.add(new ModPackets.ShopEntryInfo(
                playerName,
                ownUuid.getMostSignificantBits(),
                ownUuid.getLeastSignificantBits(),
                ownData != null ? ownData.getTrades().size() : 0
            ));
        }

        // All other shops
        for (String owner : owners) {
            if (owner.equals(playerName)) continue;
            ShopData d = mgr.get(owner);
            UUID uuid = null;
            if (d != null) {
                uuid = d.getOwnerUuid();
                // If UUID is missing, check if owner is online and save it
                if (uuid == null) {
                    ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner);
                    if (ownerPlayer != null) {
                        d.setOwnerUuid(ownerPlayer.getUUID());
                        mgr.setDirty();
                        uuid = ownerPlayer.getUUID();
                    }
                }
            }
            shopEntries.add(new ModPackets.ShopEntryInfo(
                owner,
                uuid != null ? uuid.getMostSignificantBits() : 0,
                uuid != null ? uuid.getLeastSignificantBits() : 0,
                d != null ? d.getTrades().size() : 0
            ));
        }

        ServerPlayNetworking.send(player,
            new ModPackets.OpenShopsListPayload(
                shopEntries,
                playerHasShop,
                playerName,
                player.getUUID().getMostSignificantBits(),
                player.getUUID().getLeastSignificantBits()
            ));
    }

    private static int countItem(ServerPlayer player, ItemStack target) {
        int c = 0;
        // In MC 1.21.11, use getContainerSize() instead of size()
        int invSize = player.getInventory().getContainerSize();
        for (int i = 0; i < invSize; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(s, target)) c += s.getCount();
        }
        return c;
    }

    private static void removeItems(ServerPlayer player, ItemStack target, int amount) {
        int remaining = amount;
        // In MC 1.21.11, use getContainerSize() instead of size()
        int invSize = player.getInventory().getContainerSize();
        for (int i = 0; i < invSize && remaining > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(s, target)) {
                int take = Math.min(s.getCount(), remaining);
                // In MC 1.21.11, use shrink() instead of decrement()
                s.shrink(take);
                remaining -= take;
            }
        }
        player.getInventory().setChanged();
    }
}
