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
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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

    private void registerServerPackets() {

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.ADD_TRADE_ID,
            (payload, ctx) -> {
                String owner   = payload.shopName();
                String sellId  = payload.sellId();
                int    sellCnt = payload.sellCount();
                String buyId   = payload.buyId();
                int    buyCnt  = payload.buyCount();
                ctx.server().execute(() -> {
                    ServerPlayerEntity player = ctx.player();
                    if (!player.getName().getString().equals(owner)) return;
                    
                    ItemStack sellStack = new ItemStack(Registries.ITEM.get(Identifier.of(sellId)), sellCnt);
                    
                    int found = countItem(player, sellStack);
                    if (found < sellCnt) {
                        player.sendMessage(Text.literal(
                            I18n.get("msg.not_enough_items", sellCnt, sellStack.getName().getString(), found)), false);
                        return;
                    }
                    
                    removeItems(player, sellStack.copy(), sellCnt);
                    
                    ItemStack buyStack  = new ItemStack(Registries.ITEM.get(Identifier.of(buyId)),  buyCnt);
                    ShopManager mgr = ShopManager.get(ctx.server());
                    mgr.getOrCreate(owner).addTrade(sellStack, buyStack);
                    mgr.markDirty();
                    syncShop(player, mgr.get(owner));
                    
                    player.sendMessage(Text.literal(I18n.get("msg.offer_added")), false);
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.ADD_BOOK_TRADE_ID,
            (payload, ctx) -> {
                ctx.server().execute(() -> {
                    ServerPlayerEntity player = ctx.player();
                    if (!player.getName().getString().equals(payload.shopName())) return;

                    RegistryWrapper.WrapperLookup registries = ctx.server().getRegistryManager();
                    ShopManager.setCachedRegistries(registries);
                    ItemStack sellStack = ShopData.itemStackFromNbt(payload.sellData(), registries);
                    ItemStack buyStack  = ShopData.itemStackFromNbt(payload.buyData(), registries);

                    if (sellStack.isEmpty()) {
                        player.sendMessage(Text.literal("\u00a7cFailed to read sell item!"), false);
                        return;
                    }
                    if (buyStack.isEmpty()) {
                        player.sendMessage(Text.literal("\u00a7cFailed to read price item!"), false);
                        return;
                    }

                    int needed = sellStack.getCount();
                    int found  = countItem(player, sellStack);
                    if (found < needed) {
                        player.sendMessage(Text.literal(
                            I18n.get("msg.not_enough_items", needed, sellStack.getName().getString(), found)), false);
                        return;
                    }

                    removeItems(player, sellStack.copy(), needed);

                    ShopManager mgr = ShopManager.get(ctx.server());
                    mgr.getOrCreate(payload.shopName()).addTrade(sellStack, buyStack);
                    mgr.markDirty();
                    syncShop(player, mgr.get(payload.shopName()));
                    player.sendMessage(Text.literal(I18n.get("msg.offer_added")), false);
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REMOVE_TRADE_ID,
            (payload, ctx) -> {
                String owner = payload.shopName();
                int index    = payload.index();
                ctx.server().execute(() -> {
                    ServerPlayerEntity player = ctx.player();
                    if (!player.getName().getString().equals(owner)) return;
                    ShopManager mgr = ShopManager.get(ctx.server());
                    ShopData data = mgr.get(owner);
                    if (data == null) return;
                    
                    ShopData.ShopTrade removed = data.removeTrade(index);
                    if (removed != null && !removed.sellItem.isEmpty()) {
                        ItemStack toReturn = removed.sellItem.copy();
                        if (!player.getInventory().insertStack(toReturn)) {
                            data.addToStorage(removed.sellItem.copy());
                            player.sendMessage(Text.literal(I18n.get("msg.offer_returned_storage")), false);
                        } else {
                            player.sendMessage(Text.literal(I18n.get("msg.offer_returned_inventory")), false);
                        }
                    }
                    
                    mgr.markDirty();
                    syncShop(player, data);
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.DO_TRADE_ID,
            (payload, ctx) -> {
                String owner = payload.shopName();
                int index    = payload.index();
                ctx.server().execute(() -> {
                    ServerPlayerEntity player = ctx.player();
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
                        player.sendMessage(Text.literal(
                            I18n.get("msg.not_enough_money", needed, price.getName().getString(), found)), false);
                        return;
                    }
                    
                    removeItems(player, price, needed);
                    data.addToStorage(price.copy());
                    
                    ItemStack reward = trade.sellItem.copy();
                    String rewardName = reward.getName().getString();
                    int rewardCount = reward.getCount();
                    
                    if (!player.getInventory().insertStack(reward)) {
                        player.dropItem(reward, false);
                    }
                    
                    data.removeTrade(index);
                    mgr.markDirty();
                    
                    player.sendMessage(Text.literal(
                        I18n.get("msg.trade_success", rewardName + " x" + rewardCount)), true);
                    
                    ServerPlayNetworking.send(player, new ModPackets.OpenBuyerPayload(owner, data.toNbt()));
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_PICKER_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayerEntity player = ctx.player();
                if (!player.getName().getString().equals(payload.shopName())) return;
                ServerPlayNetworking.send(player, new ModPackets.OpenPickerPayload(payload.shopName()));
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_AMOUNT_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayerEntity player = ctx.player();
                if (!player.getName().getString().equals(payload.shopName())) return;
                ServerPlayNetworking.send(player,
                    new ModPackets.OpenAmountPayload(payload.shopName(), payload.itemId()));
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_OWNER_SCREEN_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayerEntity player = ctx.player();
                if (!player.getName().getString().equals(payload.shopName())) return;
                sendOpenOwner(player, payload.shopName());
            }));
            
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_STORAGE_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayerEntity player = ctx.player();
                if (!player.getName().getString().equals(payload.shopName())) return;
                ShopManager mgr = ShopManager.get(ctx.server());
                ShopData data = mgr.get(payload.shopName());
                if (data != null) {
                    ServerPlayNetworking.send(player, new ModPackets.OpenStoragePayload(payload.shopName(), data.toNbt()));
                }
            }));
            
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.TAKE_STORAGE_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayerEntity player = ctx.player();
                if (!player.getName().getString().equals(payload.shopName())) return;
                ShopManager mgr = ShopManager.get(ctx.server());
                ShopData data = mgr.get(payload.shopName());
                if (data == null) return;
                
                ItemStack taken = data.takeFromStorage(payload.index());
                if (!taken.isEmpty()) {
                    String itemName = taken.getName().getString();
                    int itemCount = taken.getCount();
                    
                    if (!player.getInventory().insertStack(taken)) {
                        data.addToStorage(taken);
                        player.sendMessage(Text.literal(I18n.get("msg.inventory_full")), false);
                    } else {
                        player.sendMessage(Text.literal(I18n.get("msg.trade_item_taken", itemName, itemCount)), false);
                    }
                    mgr.markDirty();
                    ServerPlayNetworking.send(player, new ModPackets.OpenStoragePayload(payload.shopName(), data.toNbt()));
                }
            }));

        // Open a specific shop from the shops list GUI
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_SHOP_FROM_LIST_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayerEntity player = ctx.player();
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
                ServerPlayerEntity player = ctx.player();
                String playerName = player.getName().getString();
                ShopManager mgr = ShopManager.get(ctx.server());

                if (mgr.hasNpc(playerName)) {
                    player.sendMessage(Text.literal(I18n.get("msg.shop_exists")), false);
                    return;
                }

                ServerWorld world = (ServerWorld) player.getEntityWorld();
                VillagerEntity npc = EntityType.VILLAGER.spawn(
                    world,
                    player.getBlockPos().offset(player.getHorizontalFacing()),
                    SpawnReason.COMMAND
                );

                if (npc != null) {
                    npc.setCustomName(Text.literal(playerName + "'s Shop"));
                    npc.setCustomNameVisible(true);
                    npc.setAiDisabled(true);
                    npc.setInvulnerable(true);

                    double dx = player.getX() - npc.getX();
                    double dz = player.getZ() - npc.getZ();
                    float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                    npc.setYaw(yaw);
                    npc.setHeadYaw(yaw);

                    mgr.setNpcId(playerName, npc.getUuid());
                    ShopData data = mgr.getOrCreate(playerName);
                    data.setOwnerUuid(player.getUuid());
                    mgr.markDirty();

                    player.sendMessage(Text.literal(I18n.get("msg.shop_created", playerName)), false);

                    // Refresh the shop list after creating
                    sendShopsList(player);
                } else {
                    player.sendMessage(Text.literal("Failed to spawn shop NPC"), false);
                }
            }));
    }

    public static void syncShop(ServerPlayerEntity player, ShopData data) {
        if (data == null) return;
        ServerPlayNetworking.send(player, new ModPackets.SyncShopPayload(data.toNbt()));
    }

    public static void sendOpenOwner(ServerPlayerEntity player, String owner) {
        MinecraftServer server = player.getCommandSource().getServer();
        ShopManager mgr = ShopManager.get(server);
        ShopData data   = mgr.getOrCreate(owner);
        ServerPlayNetworking.send(player, new ModPackets.OpenOwnerPayload(owner, data.toNbt()));
        sendShopNav(player, server, mgr);
    }

    public static void sendOpenBuyer(ServerPlayerEntity player, String owner) {
        MinecraftServer server = player.getCommandSource().getServer();
        ShopManager mgr = ShopManager.get(server);
        ShopData data   = mgr.get(owner);
        if (data == null || data.getTrades().isEmpty()) {
            player.sendMessage(Text.literal(I18n.get("buyer.empty")), false);
            return;
        }
        ServerPlayNetworking.send(player, new ModPackets.OpenBuyerPayload(owner, data.toNbt()));
        sendShopNav(player, server, mgr);
    }

    /** Send navigation data (list of all shop owners) to the client */
    private static void sendShopNav(ServerPlayerEntity player, MinecraftServer server, ShopManager mgr) {
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

    public static void sendShopsList(ServerPlayerEntity player) {
        String playerName = player.getName().getString();
        MinecraftServer server = ((net.minecraft.server.world.ServerWorld) player.getEntityWorld()).getServer();
        ShopManager mgr = ShopManager.get(server);
        Set<String> owners = mgr.getAllOwners();
        boolean playerHasShop = mgr.hasShop(playerName);

        List<ModPackets.ShopEntryInfo> shopEntries = new ArrayList<>();

        // Player's own shop first
        if (playerHasShop) {
            ShopData ownData = mgr.get(playerName);
            UUID ownUuid = (ownData != null && ownData.getOwnerUuid() != null) ? ownData.getOwnerUuid() : player.getUuid();
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
            UUID uuid = d != null ? d.getOwnerUuid() : null;
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
                player.getUuid().getMostSignificantBits(),
                player.getUuid().getLeastSignificantBits()
            ));
    }

    private static int countItem(ServerPlayerEntity player, ItemStack target) {
        int c = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (ItemStack.areItemsAndComponentsEqual(s, target)) c += s.getCount();
        }
        return c;
    }

    private static void removeItems(ServerPlayerEntity player, ItemStack target, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (ItemStack.areItemsAndComponentsEqual(s, target)) {
                int take = Math.min(s.getCount(), remaining);
                s.decrement(take);
                remaining -= take;
            }
        }
        player.getInventory().markDirty();
    }
}
