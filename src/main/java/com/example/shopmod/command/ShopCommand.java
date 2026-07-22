package com.example.shopmod.command;

import com.example.shopmod.data.I18n;
import com.example.shopmod.data.RecoveryData;
import com.example.shopmod.data.ShopData;
import com.example.shopmod.data.ShopManager;
import com.example.shopmod.network.ModPackets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ShopCommand {

    // Suggests only the owner name (no "'s Shop" suffix) so word() can parse it
    private static final SuggestionProvider<CommandSourceStack> SHOP_SUGGESTIONS =
        (context, builder) -> {
            ShopManager mgr = ShopManager.get(context.getSource().getServer());
            Set<String> owners = mgr.getAllOwners();
            String input = builder.getRemaining().toLowerCase();
            for (String owner : owners) {
                if (owner.toLowerCase().startsWith(input)) {
                    builder.suggest(owner);
                }
            }
            return builder.buildFuture();
        };

    // Suggests online players who do NOT already own a shop (for transfer target)
    private static final SuggestionProvider<CommandSourceStack> TRANSFER_TARGET_SUGGESTIONS =
        (context, builder) -> {
            ShopManager mgr = ShopManager.get(context.getSource().getServer());
            String input = builder.getRemaining().toLowerCase();
            for (ServerPlayer p : context.getSource().getServer().getPlayerList().getPlayers()) {
                String name = p.getName().getString();
                if (!mgr.playerHasShop(name, p.getUUID())
                    && name.toLowerCase().startsWith(input)) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext access,
                                Commands.CommandSelection env) {

        // /shop create - Spawns a Shop NPC (re-spawns if data exists but NPC is gone)
        dispatcher.register(Commands.literal("shop")
            .then(Commands.literal("create")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    ServerPlayer player  = source.getPlayerOrException();
                    MinecraftServer server     = source.getServer();
                    String playerName          = player.getName().getString();
                    ShopManager mgr            = ShopManager.get(server);

                    // If NPC ID is registered, check if it actually exists in the world
                    if (mgr.hasNpc(playerName)) {
                        UUID npcId = mgr.getNpcId(playerName);
                        for (ServerLevel sw : server.getAllLevels()) {
                            List<Villager> found = sw.getEntitiesOfClass(
                                Villager.class,
                                new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000),
                                e -> e.getUUID().equals(npcId));
                            if (!found.isEmpty()) {
                                player.sendSystemMessage(Component.literal(I18n.get("msg.shop_exists")));
                                return 0;
                            }
                        }
                    }

                    ServerLevel world = source.getLevel();
                    @SuppressWarnings("unchecked")
                    EntityType<Villager> villagerType = (EntityType<Villager>) BuiltInRegistries.ENTITY_TYPE.get(Identifier.fromNamespaceAndPath("minecraft", "villager")).orElseThrow().value();
                    Villager npc = villagerType.spawn(
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

                        player.sendSystemMessage(Component.literal(I18n.get("msg.shop_created", playerName)));
                        return 1;
                    } else {
                        player.sendSystemMessage(Component.literal("Failed to spawn shop NPC"));
                        return 0;
                    }
                })
            )
            // /shop close - Remove NPC only (keep shop data intact)
            .then(Commands.literal("close")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    ServerPlayer player  = source.getPlayerOrException();
                    MinecraftServer server     = source.getServer();
                    String playerName          = player.getName().getString();
                    ShopManager mgr            = ShopManager.get(server);

                    if (!mgr.hasNpc(playerName)) {
                        player.sendSystemMessage(Component.literal(I18n.get("msg.shop_not_found")));
                        return 0;
                    }

                    UUID npcId = mgr.getNpcId(playerName);
                    ServerLevel world = source.getLevel();

                    List<Villager> found = world.getEntitiesOfClass(
                        Villager.class,
                        new AABB(
                            player.getX() - 100, player.getY() - 64, player.getZ() - 100,
                            player.getX() + 100, player.getY() + 64, player.getZ() + 100
                        ),
                        e -> e.getUUID().equals(npcId));

                    if (found.isEmpty()) {
                        for (ServerLevel sw : server.getAllLevels()) {
                            List<Villager> allNpcs = sw.getEntitiesOfClass(
                                Villager.class,
                                new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000),
                                e -> e.getUUID().equals(npcId));
                            if (!allNpcs.isEmpty()) {
                                found = allNpcs;
                                break;
                            }
                        }
                    }

                    if (!found.isEmpty()) {
                        found.get(0).discard();
                        mgr.removeNpc(playerName);
                        mgr.setDirty();
                        player.sendSystemMessage(Component.literal(I18n.get("msg.shop_closed")));
                        return 1;
                    } else {
                        // NPC already gone — only remove npcId, keep shop data
                        mgr.removeNpc(playerName);
                        mgr.setDirty();
                        player.sendSystemMessage(Component.literal(I18n.get("msg.shop_npc_not_found")));
                        return 0;
                    }
                })
            )
            // /shop recover - Load and send recovery items to client
            .then(Commands.literal("recover")
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    ServerPlayer player = source.getPlayerOrException();
                    HolderLookup.Provider registries = source.getServer().registryAccess();
                    List<RecoveryData.ItemStackWithSource> items = RecoveryData.load(player.getUUID(), registries);
                    if (items == null || items.isEmpty()) {
                        player.sendSystemMessage(Component.literal(I18n.get("msg.no_recovery_items")));
                        return 0;
                    }
                    net.minecraft.nbt.CompoundTag data = RecoveryData.serializeForNetwork(items, registries);
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, new ModPackets.RecoveryDataPayload(data));
                    return 1;
                })
            )
        );

        // /shops - Opens GUI with all shops
        dispatcher.register(Commands.literal("shops")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                ServerPlayer player  = source.getPlayerOrException();
                MinecraftServer server     = source.getServer();
                String playerName          = player.getName().getString();
                ShopManager mgr            = ShopManager.get(server);
                Set<String> owners         = mgr.getAllOwners();

                boolean playerHasShop = mgr.playerHasShop(playerName, player.getUUID());

                List<ModPackets.ShopEntryInfo> shopEntries = new ArrayList<>();

                if (playerHasShop) {
                    ShopData ownData = mgr.get(playerName);
                    UUID ownUuid = resolveOwnerUuid(server, playerName, ownData);
                    shopEntries.add(new ModPackets.ShopEntryInfo(
                        playerName,
                        ownUuid != null ? ownUuid.getMostSignificantBits() : 0,
                        ownUuid != null ? ownUuid.getLeastSignificantBits() : 0,
                        ownData != null ? ownData.getTrades().size() : 0
                    ));
                }

                for (String owner : owners) {
                    if (owner.equals(playerName)) continue;
                    ShopData d = mgr.get(owner);
                    UUID uuid = resolveOwnerUuid(server, owner, d);
                    shopEntries.add(new ModPackets.ShopEntryInfo(
                        owner,
                        uuid != null ? uuid.getMostSignificantBits() : 0,
                        uuid != null ? uuid.getLeastSignificantBits() : 0,
                        d != null ? d.getTrades().size() : 0
                    ));
                }

                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                    new ModPackets.OpenShopsListPayload(
                        shopEntries,
                        playerHasShop,
                        playerName,
                        player.getUUID().getMostSignificantBits(),
                        player.getUUID().getLeastSignificantBits()
                    ));

                return 1;
            }));

        // /shopadmin - Admin commands (OP only)
        dispatcher.register(Commands.literal("shopadmin")
            .requires(src -> src.getPlayer() != null && src.getServer().getPlayerList().isOp(new NameAndId(src.getPlayer().getGameProfile())))
            .then(Commands.literal("transfer")
                .then(Commands.argument("shopOwner", StringArgumentType.word())
                    .suggests(SHOP_SUGGESTIONS)
                    .then(Commands.argument("target", StringArgumentType.word())
                        .suggests(TRANSFER_TARGET_SUGGESTIONS)
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            MinecraftServer server = source.getServer();
                            String shopOwnerName = StringArgumentType.getString(ctx, "shopOwner");
                            String targetName = StringArgumentType.getString(ctx, "target");
                            ShopManager mgr = ShopManager.get(server);

                            // Find shopOwner player (can be offline - look up shop by name)
                            ShopData ownerData = mgr.get(shopOwnerName);
                            if (ownerData == null) {
                                source.sendFailure(Component.literal(I18n.get("msg.no_shop_to_transfer", shopOwnerName)));
                                return 0;
                            }

                            // Find target player: try online first, if offline use name only (UUID null)
                            ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetName);
                            UUID targetUuid = null;

                            if (targetPlayer != null) {
                                targetUuid = targetPlayer.getUUID();
                                targetName = targetPlayer.getName().getString();
                            }
                            // If offline: targetUuid stays null, shop linked by name only

                            // Check target doesn't already have a shop
                            if (mgr.playerHasShop(targetName, targetUuid)) {
                                source.sendFailure(Component.literal(I18n.get("msg.target_has_shop")));
                                return 0;
                            }

                            // Transfer: rename NPC and move data to new owner (keep NPC alive)
                            UUID oldNpcId = mgr.getNpcId(shopOwnerName);

                            // Rename the NPC in the world BEFORE deleting old shop data
                            if (oldNpcId != null) {
                                for (ServerLevel level : server.getAllLevels()) {
                                    var entity = level.getEntity(oldNpcId);
                                    if (entity instanceof Villager villager && villager.isAlive()) {
                                        villager.setCustomName(Component.literal(targetName + "'s Shop"));
                                    }
                                }
                            }

                            // Delete old shop data (removes shops[shopOwnerName] and npcIds[shopOwnerName])
                            mgr.deleteShop(shopOwnerName);

                            // Create new shop data for the target player
                            ShopData targetShop = mgr.getOrCreate(targetName);
                            targetShop.setOwnerUuid(targetUuid);
                            for (ShopData.ShopTrade trade : ownerData.getTrades()) {
                                targetShop.addTrade(trade.sellItem, trade.buyItem);
                            }
                            for (ItemStack s : ownerData.getStorage()) {
                                targetShop.addToStorage(s);
                            }
                            targetShop.setShopMoveEnabled(ownerData.isShopMoveEnabled());

                            // Re-link the same NPC to the new owner
                            if (oldNpcId != null) {
                                mgr.setNpcId(targetName, oldNpcId);
                            }

                            mgr.setDirty();
                            source.sendSuccess(() -> Component.literal(I18n.get("msg.transfer_success")), false);
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("delete")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(SHOP_SUGGESTIONS)
                    .executes(ctx -> {
                        CommandSourceStack source = ctx.getSource();
                        MinecraftServer server = source.getServer();
                        String targetName = StringArgumentType.getString(ctx, "player");
                        ShopManager mgr = ShopManager.get(server);
                        HolderLookup.Provider registries = server.registryAccess();

                        // Find the player's shop (by name or UUID)
                        ShopData shopData = mgr.get(targetName);
                        ServerPlayer targetPlayer = server.getPlayerList().getPlayer(targetName);

                        // Try UUID lookup if player is online and name doesn't match
                        if (shopData == null && targetPlayer != null) {
                            String nameByUuid = mgr.getOwnerNameByUuid(targetPlayer.getUUID());
                            if (nameByUuid != null) {
                                shopData = mgr.get(nameByUuid);
                                targetName = nameByUuid;
                            }
                        }

                        if (shopData == null) {
                            source.sendFailure(Component.literal(I18n.get("msg.shop_not_found_admin", StringArgumentType.getString(ctx, "player"))));
                            return 0;
                        }

                        // Save all items to recovery (append to existing)
                        UUID ownerUuid = shopData.getOwnerUuid();
                        if (ownerUuid != null) {
                            List<RecoveryData.ItemStackWithSource> recoveryItems = new ArrayList<>();
                            // Load existing recovery items first
                            List<RecoveryData.ItemStackWithSource> existing = RecoveryData.load(ownerUuid, registries);
                            if (existing != null) recoveryItems.addAll(existing);
                            // Add new items from deleted shop
                            for (ShopData.ShopTrade trade : shopData.getTrades()) {
                                recoveryItems.add(new RecoveryData.ItemStackWithSource(trade.sellItem.copy(), "offer"));
                            }
                            for (ItemStack s : shopData.getStorage()) {
                                recoveryItems.add(new RecoveryData.ItemStackWithSource(s.copy(), "storage"));
                            }
                            if (!recoveryItems.isEmpty()) {
                                RecoveryData.save(ownerUuid, recoveryItems, registries);
                            }

                            if (targetPlayer != null) {
                                targetPlayer.sendSystemMessage(Component.literal(I18n.get("msg.shop_deleted_notify")));
                            }
                        }

                        mgr.removeNpcEntity(targetName, server);
                        mgr.deleteShop(targetName);

                        final String deletedName = targetName;
                        source.sendSuccess(() -> Component.literal(I18n.get("msg.shop_deleted_admin", deletedName)), false);
                        return 1;
                    })
                )
            )

        );
    }

    private static UUID resolveOwnerUuid(MinecraftServer server, String ownerName, ShopData shopData) {
        if (shopData != null && shopData.getOwnerUuid() != null) {
            return shopData.getOwnerUuid();
        }
        if (server != null) {
            ServerPlayer owner = server.getPlayerList().getPlayer(ownerName);
            if (owner != null) {
                if (shopData != null) {
                    shopData.setOwnerUuid(owner.getUUID());
                    ShopManager.get(server).setDirty();
                }
                return owner.getUUID();
            }
        }
        return null;
    }
}