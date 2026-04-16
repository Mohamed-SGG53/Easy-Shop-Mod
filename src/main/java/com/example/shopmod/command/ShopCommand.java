package com.example.shopmod.command;

import com.example.shopmod.ShopMod;
import com.example.shopmod.data.I18n;
import com.example.shopmod.data.ShopData;
import com.example.shopmod.data.ShopManager;
import com.example.shopmod.network.ModPackets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ShopCommand {

    private static final SuggestionProvider<ServerCommandSource> SHOP_SUGGESTIONS =
        (context, builder) -> {
            ServerCommandSource source = context.getSource();
            ShopManager mgr = ShopManager.get(source.getServer());
            Set<String> owners = mgr.getAllOwners();
            String input = builder.getRemaining().toLowerCase();

            for (String owner : owners) {
                String shopName = owner + "'s Shop";
                if (shopName.toLowerCase().startsWith(input) || owner.toLowerCase().startsWith(input)) {
                    builder.suggest(shopName);
                }
            }
            return builder.buildFuture();
        };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess access,
                                CommandManager.RegistrationEnvironment env) {

        // /create_shop - Spawns a Shop NPC (like /summon)
        dispatcher.register(CommandManager.literal("create_shop")
            .executes(ctx -> {
                ServerCommandSource source = ctx.getSource();
                ServerPlayerEntity player  = source.getPlayerOrThrow();
                MinecraftServer server     = source.getServer();
                String playerName          = player.getName().getString();
                ShopManager mgr            = ShopManager.get(server);

                // Check if player already has a shop
                if (mgr.hasNpc(playerName)) {
                    UUID npcId = mgr.getNpcId(playerName);
                    ServerWorld sw = source.getWorld();
                    List<VillagerEntity> found = sw.getEntitiesByClass(
                        VillagerEntity.class,
                        new Box(
                            player.getX() - 1000, player.getY() - 128, player.getZ() - 1000,
                            player.getX() + 1000, player.getY() + 128, player.getZ() + 1000
                        ),
                        e -> e.getUuid().equals(npcId) && e.getCustomName() != null &&
                             e.getCustomName().getString().endsWith("'s Shop"));
                    if (!found.isEmpty()) {
                        player.sendMessage(Text.literal(I18n.get("msg.shop_exists")), false);
                        return 0;
                    }
                }

                // Spawn a regular VillagerEntity using EntityType.spawn()
                ServerWorld world = source.getWorld();
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

                    // Make NPC look at the player
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
                    return 1;
                } else {
                    player.sendMessage(Text.literal("Failed to spawn shop NPC"), false);
                    return 0;
                }
            }));

        // /close_shop - Remove your NPC
        dispatcher.register(CommandManager.literal("close_shop")
            .executes(ctx -> {
                ServerCommandSource source = ctx.getSource();
                ServerPlayerEntity player  = source.getPlayerOrThrow();
                MinecraftServer server     = source.getServer();
                String playerName          = player.getName().getString();
                ShopManager mgr            = ShopManager.get(server);

                if (!mgr.hasNpc(playerName)) {
                    player.sendMessage(Text.literal(I18n.get("msg.shop_not_found")), false);
                    return 0;
                }

                UUID npcId = mgr.getNpcId(playerName);
                ServerWorld world = source.getWorld();

                List<VillagerEntity> found = world.getEntitiesByClass(
                    VillagerEntity.class,
                    new Box(
                        player.getX() - 100, player.getY() - 64, player.getZ() - 100,
                        player.getX() + 100, player.getY() + 64, player.getZ() + 100
                    ),
                    e -> e.getUuid().equals(npcId));

                if (found.isEmpty()) {
                    for (ServerWorld sw : server.getWorlds()) {
                        List<VillagerEntity> allNpcs = sw.getEntitiesByClass(
                            VillagerEntity.class,
                            new Box(-30000000, -64, -30000000, 30000000, 320, 30000000),
                            e -> e.getUuid().equals(npcId));
                        if (!allNpcs.isEmpty()) {
                            found = allNpcs;
                            break;
                        }
                    }
                }

                if (!found.isEmpty()) {
                    found.get(0).discard();
                    mgr.removeNpc(playerName);
                    mgr.markDirty();
                    player.sendMessage(Text.literal(I18n.get("msg.shop_closed")), false);
                    return 1;
                } else {
                    mgr.removeNpc(playerName);
                    mgr.markDirty();
                    player.sendMessage(Text.literal(I18n.get("msg.shop_npc_not_found")), false);
                    return 0;
                }
            }));

        // /shops - Opens GUI with all shops
        dispatcher.register(CommandManager.literal("shops")
            .executes(ctx -> {
                ServerCommandSource source = ctx.getSource();
                ServerPlayerEntity player  = source.getPlayerOrThrow();
                MinecraftServer server     = source.getServer();
                String playerName          = player.getName().getString();
                ShopManager mgr            = ShopManager.get(server);
                Set<String> owners         = mgr.getAllOwners();

                boolean playerHasShop = mgr.hasShop(playerName);

                List<ModPackets.ShopEntryInfo> shopEntries = new ArrayList<>();

                // First: player's own shop (if they have one)
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

                // Then: all other shops
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

                // Send the shop list packet to client
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                    new ModPackets.OpenShopsListPayload(
                        shopEntries,
                        playerHasShop,
                        playerName,
                        player.getUuid().getMostSignificantBits(),
                        player.getUuid().getLeastSignificantBits()
                    ));

                return 1;
            }));
    }

    /**
     * Resolve the UUID of a shop owner.
     * Priority: ShopData stored UUID -> Online player -> null
     */
    private static UUID resolveOwnerUuid(MinecraftServer server, String ownerName, ShopData shopData) {
        // 1. Try from ShopData
        if (shopData != null && shopData.getOwnerUuid() != null) {
            return shopData.getOwnerUuid();
        }

        // 2. Try from online players (if server is available)
        if (server != null) {
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerName);
            if (owner != null) {
                // Update the shop data with the UUID for future use
                if (shopData != null) {
                    shopData.setOwnerUuid(owner.getUuid());
                    ShopManager.get(server).markDirty();
                }
                return owner.getUuid();
            }
        }

        return null;
    }
}
