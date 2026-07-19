package com.example.shopmod;

import com.example.shopmod.command.ShopCommand;
import com.example.shopmod.data.I18n;
import com.example.shopmod.data.PlayerSkinStore;
import com.example.shopmod.data.ShopData;
import com.example.shopmod.data.ShopManager;
import com.example.shopmod.data.RecoveryData;
import com.example.shopmod.network.ModPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
import net.minecraft.nbt.CompoundTag;
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
import java.nio.file.*;

public class ShopMod implements ModInitializer {
    public static final String MOD_ID = "shopmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("ShopMod initializing...");

        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_OWNER_ID,   ModPackets.OpenOwnerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_BUYER_ID,   ModPackets.OpenBuyerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_PICKER_ID,  ModPackets.OpenPickerPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_AMOUNT_ID,  ModPackets.OpenAmountPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.SYNC_SHOP_ID,    ModPackets.SyncShopPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_STORAGE_ID, ModPackets.OpenStoragePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_SHOPS_LIST_ID, ModPackets.OpenShopsListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.SHOP_NAV_ID, ModPackets.ShopNavPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.SKIN_DATA_ID, ModPackets.SkinDataPayload.CODEC);
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
        PayloadTypeRegistry.playC2S().register(ModPackets.TOGGLE_SHOP_MOVE_ID, ModPackets.ToggleShopMovePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.UPLOAD_SKIN_ID,      ModPackets.UploadSkinPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.REQUEST_SKINS_ID,    ModPackets.RequestSkinsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.OPEN_RECOVERY_ID, ModPackets.OpenRecoveryPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ModPackets.RECOVERY_DATA_ID, ModPackets.RecoveryDataPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ModPackets.CLAIM_RECOVERY_ITEM_ID, ModPackets.ClaimRecoveryItemPayload.CODEC);

        CommandRegistrationCallback.EVENT.register(ShopCommand::register);
        registerServerPackets();

        // Shop NPC Follow Owner - checks every 2 seconds (40 ticks)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 40 != 0) return;

            ShopManager mgr = ShopManager.get(server);

            for (String owner : mgr.getAllOwners()) {
                ShopData data = mgr.get(owner);
                if (data == null || !data.isShopMoveEnabled()) continue;

                UUID npcUuid = mgr.getNpcId(owner);
                if (npcUuid == null) continue;

                ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner);
                if (ownerPlayer == null || !ownerPlayer.isAlive()) continue;

                Villager villager = null;
                for (ServerLevel level : server.getAllLevels()) {
                    var e = level.getEntity(npcUuid);
                    if (e instanceof Villager v && v.isAlive()) { villager = v; break; }
                }
                if (villager == null) continue;

                if (!villager.level().equals(ownerPlayer.level())) {
                    villager.setNoAi(true);
                    continue;
                }

                double dx = ownerPlayer.getX() - villager.getX();
                double dy = ownerPlayer.getY() - villager.getY();
                double dz = ownerPlayer.getZ() - villager.getZ();
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                if (dist >= 25.0) {
                    villager.teleportTo(
                        ownerPlayer.getX() + 2, ownerPlayer.getY(), ownerPlayer.getZ() + 2
                    );
                    villager.setYRot(ownerPlayer.getYRot());
                    villager.setXRot(0);
                    villager.setYHeadRot(ownerPlayer.getYRot());
                } else if (dist > 3.0) {
                    villager.setNoAi(false);
                    villager.getNavigation().moveTo(
                        ownerPlayer.getX(), ownerPlayer.getY(), ownerPlayer.getZ(), 0.8
                    );
                } else {
                    villager.setNoAi(true);
                    float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                    villager.setYRot(yaw);
                    villager.setYHeadRot(yaw);
                }
            }
        });

        LOGGER.info("ShopMod ready!");
    }

    private static Item resolveItem(String idStr) {
        if (idStr == null || idStr.isEmpty()) return Items.AIR;
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).toString().equals(idStr)) {
                return item;
            }
        }
        return Items.AIR;
    }

    // ========================================================================
    // Add item to main inventory only (slots 0-35: hotbar + main)
    // Returns true if fully added, false if not enough space
    // ========================================================================
    private static boolean addToMainInventory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return true;

        ItemStack remaining = stack.copy();

        // First pass: stack onto existing same items (slots 0-35)
        for (int i = 0; i < 36 && !remaining.isEmpty(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                int canAdd = Math.min(remaining.getCount(), slotStack.getMaxStackSize() - slotStack.getCount());
                if (canAdd > 0) {
                    slotStack.grow(canAdd);
                    remaining.shrink(canAdd);
                }
            }
        }

        // Second pass: fill empty slots (slots 0-35)
        if (!remaining.isEmpty()) {
            for (int i = 0; i < 36 && !remaining.isEmpty(); i++) {
                if (player.getInventory().getItem(i).isEmpty()) {
                    player.getInventory().setItem(i, remaining.copy());
                    remaining = ItemStack.EMPTY;
                }
            }
        }

        player.getInventory().setChanged();
        return remaining.isEmpty();
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
                    ShopManager mgr = ShopManager.get(ctx.server());
                    if (!isOwner(player, owner, mgr)) return;

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
                    player.inventoryMenu.broadcastChanges();

                    Item buyItem = resolveItem(buyId);
                    if (buyItem == Items.AIR) return;
                    ItemStack buyStack = new ItemStack(buyItem, buyCnt);
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
                    ShopManager mgr = ShopManager.get(ctx.server());
                    if (!isOwner(player, payload.shopName(), mgr)) return;

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
                    player.inventoryMenu.broadcastChanges();

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
                    ShopManager mgr = ShopManager.get(ctx.server());
                    if (!isOwner(player, owner, mgr)) return;
                    ShopData data = mgr.get(owner);
                    if (data == null) return;

                    ShopData.ShopTrade removed = data.removeTrade(index);
                    if (removed != null && !removed.sellItem.isEmpty()) {
                        ItemStack toReturn = removed.sellItem.copy();
                        // Use addToMainInventory (slots 0-35 only, no armor/offhand)
                        if (!addToMainInventory(player, toReturn)) {
                            data.addToStorage(removed.sellItem.copy());
                            player.displayClientMessage(Component.literal(I18n.get("msg.offer_returned_storage")), false);
                        } else {
                            player.displayClientMessage(Component.literal(I18n.get("msg.offer_returned_inventory")), false);
                        }
                    }

                    player.inventoryMenu.broadcastChanges();
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

                    ServerLevel serverLevel = (ServerLevel) player.level();
                    if (!player.getInventory().add(reward)) {
                        serverLevel.addFreshEntity(new ItemEntity(serverLevel,
                            player.getX(), player.getEyeY() - 0.3, player.getZ(), reward));
                    }

                    data.removeTrade(index);
                    player.inventoryMenu.broadcastChanges();
                    mgr.setDirty();

                    player.displayClientMessage(Component.literal(
                        I18n.get("msg.trade_success", rewardName + " x" + rewardCount)), true);

                    ServerPlayNetworking.send(player, new ModPackets.OpenBuyerPayload(owner, data.toNbt()));
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_PICKER_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                ShopManager mgr = ShopManager.get(ctx.server());
                if (!isOwner(player, payload.shopName(), mgr)) return;
                ServerPlayNetworking.send(player, new ModPackets.OpenPickerPayload(payload.shopName()));
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_AMOUNT_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                ShopManager mgr = ShopManager.get(ctx.server());
                if (!isOwner(player, payload.shopName(), mgr)) return;
                ServerPlayNetworking.send(player,
                    new ModPackets.OpenAmountPayload(payload.shopName(), payload.itemId()));
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_OWNER_SCREEN_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                ShopManager mgr = ShopManager.get(ctx.server());
                if (!isOwner(player, payload.shopName(), mgr)) return;
                sendOpenOwner(player, payload.shopName());
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQ_STORAGE_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                ShopManager mgr = ShopManager.get(ctx.server());
                if (!isOwner(player, payload.shopName(), mgr)) return;
                ShopData data = mgr.get(payload.shopName());
                if (data != null) {
                    ServerPlayNetworking.send(player, new ModPackets.OpenStoragePayload(payload.shopName(), data.toNbt()));
                }
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.TAKE_STORAGE_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                ShopManager mgr = ShopManager.get(ctx.server());
                if (!isOwner(player, payload.shopName(), mgr)) return;
                ShopData data = mgr.get(payload.shopName());
                if (data == null) return;

                ItemStack taken = data.takeFromStorage(payload.index());
                if (!taken.isEmpty()) {
                    String itemName = taken.getHoverName().getString();
                    int itemCount = taken.getCount();

                    // Use addToMainInventory (slots 0-35 only, no armor/offhand)
                    if (!addToMainInventory(player, taken)) {
                        data.addToStorage(taken);
                        player.displayClientMessage(Component.literal(I18n.get("msg.inventory_full")), false);
                    } else {
                        player.displayClientMessage(Component.literal(I18n.get("msg.trade_item_taken", itemName, itemCount)), false);
                    }
                    mgr.setDirty();
                    player.inventoryMenu.broadcastChanges();
                    ServerPlayNetworking.send(player, new ModPackets.OpenStoragePayload(payload.shopName(), data.toNbt()));
                }
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_SHOP_FROM_LIST_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                String owner = payload.ownerName();
                String playerName = player.getName().getString();

                if (playerName.equals(owner)) {
                    // Own shop - open owner screen
                    sendOpenOwner(player, owner);
                } else {
                    // Other player's shop - open buyer screen (even if empty)
                    sendOpenBuyer(player, owner);
                }
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.CREATE_SHOP_FROM_LIST_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                String playerName = player.getName().getString();
                ShopManager mgr = ShopManager.get(ctx.server());

                if (mgr.playerHasShop(playerName, player.getUUID())) {
                    player.displayClientMessage(Component.literal(I18n.get("msg.shop_exists")), false);
                    return;
                }

                ServerLevel world = (ServerLevel) player.level();
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
                    sendShopsList(player);
                } else {
                    player.displayClientMessage(Component.literal("Failed to spawn shop NPC"), false);
                }
            }));

        ServerPlayNetworking.registerGlobalReceiver(ModPackets.TOGGLE_SHOP_MOVE_ID,
            (payload, ctx) -> {
                String owner = payload.shopName();
                boolean enabled = payload.enabled();
                ctx.server().execute(() -> {
                    ServerPlayer player = ctx.player();
                    ShopManager mgr = ShopManager.get(ctx.server());
                    if (!isOwner(player, owner, mgr)) return;

                    ShopData data = mgr.get(owner);
                    if (data == null) return;

                    data.setShopMoveEnabled(enabled);
                    mgr.setDirty();

                    UUID npcUuid = mgr.getNpcId(owner);
                    if (npcUuid != null) {
                        for (ServerLevel level : ctx.server().getAllLevels()) {
                            var entity = level.getEntity(npcUuid);
                            if (entity instanceof Villager villager) {
                                villager.setNoAi(!enabled);
                                break;
                            }
                        }
                    }

                    player.displayClientMessage(Component.literal(
                        enabled ? "Shop Movement: ON" : "Shop Movement: OFF"), false);

                    syncShop(player, data);
                });
            });

        // ==================== Skin System Packets ====================

        // C2S: Player uploads their skin PNG to server
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.UPLOAD_SKIN_ID,
            (payload, ctx) -> {
                UUID uuid = new UUID(payload.uuidMost(), payload.uuidLeast());
                byte[] pngData = payload.pngData();
                ctx.server().execute(() -> {
                    if (pngData != null && pngData.length > 0) {
                        PlayerSkinStore.saveSkin(uuid, pngData);
                        LOGGER.info("[ShopMod] Received skin from {} ({} bytes)", uuid, pngData.length);

                        // Broadcast the updated skin to all OTHER online players
                        for (ServerPlayer onlinePlayer : ctx.server().getPlayerList().getPlayers()) {
                            if (!onlinePlayer.getUUID().equals(uuid)) {
                                ServerPlayNetworking.send(onlinePlayer, new ModPackets.SkinDataPayload(
                                    payload.uuidMost(), payload.uuidLeast(), pngData
                                ));
                            }
                        }
                    }
                });
            });

        // C2S: Player requests all other players' skins
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.REQUEST_SKINS_ID,
            (payload, ctx) -> {
                UUID requesterUuid = new UUID(payload.uuidMost(), payload.uuidLeast());
                ctx.server().execute(() -> {
                    MinecraftServer server = ctx.server();
                    Path skinsDir = PlayerSkinStore.getSkinsDir();

                    // Send all skins from server storage (except the requester's own)
                    try {
                        if (Files.exists(skinsDir)) {
                            for (java.nio.file.Path skinFile : Files.list(skinsDir).toArray(java.nio.file.Path[]::new)) {
                                String fileName = skinFile.getFileName().toString();
                                if (!fileName.endsWith(".png")) continue;

                                // Parse UUID from filename
                                String uuidStr = fileName.substring(0, fileName.length() - 4);
                                try {
                                    UUID skinUuid = UUID.fromString(uuidStr);
                                    // Skip the requester's own skin
                                    if (skinUuid.equals(requesterUuid)) continue;

                                    byte[] data = Files.readAllBytes(skinFile);
                                    if (data != null && data.length > 0) {
                                        ServerPlayNetworking.send(ctx.player(), new ModPackets.SkinDataPayload(
                                            skinUuid.getMostSignificantBits(),
                                            skinUuid.getLeastSignificantBits(),
                                            data
                                        ));
                                    }
                                } catch (IllegalArgumentException ignored) {
                                    // Not a valid UUID file, skip
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.warn("[ShopMod] Error sending skins to {}: {}", requesterUuid, e.getMessage());
                    }

                    LOGGER.info("[ShopMod] Sent all available skins to {}", requesterUuid);
                });
            });

        // ==================== Recovery System Packets ====================

        // C2S: Player claims a recovery item
        ServerPlayNetworking.registerGlobalReceiver(ModPackets.CLAIM_RECOVERY_ITEM_ID,
            (payload, ctx) -> ctx.server().execute(() -> {
                ServerPlayer player = ctx.player();
                int index = payload.index();
                HolderLookup.Provider registries = ctx.server().registryAccess();
                List<RecoveryData.ItemStackWithSource> items = RecoveryData.load(player.getUUID(), registries);
                if (items == null || index < 0 || index >= items.size()) return;

                ItemStack stack = items.get(index).stack();
                if (addToMainInventory(player, stack)) {
                    RecoveryData.removeItem(player.getUUID(), index, registries);
                    player.inventoryMenu.broadcastChanges();
                    // Check if all items recovered
                    if (RecoveryData.isEmpty(player.getUUID())) {
                        player.displayClientMessage(Component.literal(I18n.get("msg.all_recovered")), false);
                        // Close recovery screen
                        if (player.containerMenu != player.inventoryMenu) {
                            player.closeContainer();
                        }
                    }
                } else {
                    player.displayClientMessage(Component.literal(I18n.get("msg.recovery_inventory_full")), false);
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
        ShopData data   = mgr.getOrCreate(owner);
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

    private static void sendShopNav(ServerPlayer player, MinecraftServer server, ShopManager mgr) {
        String playerName = player.getName().getString();
        Set<String> owners = mgr.getAllOwners();
        List<String> orderedOwners = new ArrayList<>();

        if (owners.contains(playerName)) {
            orderedOwners.add(playerName);
        }
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

        if (playerHasShop) {
            ShopData ownData = mgr.get(playerName);
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

        for (String owner : owners) {
            if (owner.equals(playerName)) continue;
            ShopData d = mgr.get(owner);
            UUID uuid = null;
            if (d != null) {
                uuid = d.getOwnerUuid();
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
        int invSize = player.getInventory().getContainerSize();
        for (int i = 0; i < invSize; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(s, target)) c += s.getCount();
        }
        return c;
    }

    private static void removeItems(ServerPlayer player, ItemStack target, int amount) {
        int remaining = amount;
        int invSize = player.getInventory().getContainerSize();
        for (int i = 0; i < invSize && remaining > 0; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(s, target)) {
                int take = Math.min(s.getCount(), remaining);
                s.shrink(take);
                remaining -= take;
            }
        }
        player.getInventory().setChanged();
    }

    private static boolean isOwner(ServerPlayer player, String shopOwnerName, ShopManager mgr) {
        ShopData data = mgr.get(shopOwnerName);
        if (data == null) return false;
        // If shop has UUID, compare by UUID (handles name changes)
        if (data.getOwnerUuid() != null) {
            return data.getOwnerUuid().equals(player.getUUID());
        }
        // Fallback: compare by name
        return player.getName().getString().equals(shopOwnerName);
    }
}