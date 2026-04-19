package com.example.shopmod.command;

import com.example.shopmod.ShopMod;
import com.example.shopmod.data.I18n;
import com.example.shopmod.data.ShopData;
import com.example.shopmod.data.ShopManager;
import com.example.shopmod.network.ModPackets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ShopCommand {

    private static final SuggestionProvider<CommandSourceStack> SHOP_SUGGESTIONS =
        (context, builder) -> {
            CommandSourceStack source = context.getSource();
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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext access,
                                Commands.CommandSelection env) {

        // /create_shop - Spawns a Shop NPC (like /summon)
        dispatcher.register(Commands.literal("create_shop")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                ServerPlayer player  = source.getPlayerOrException();
                MinecraftServer server     = source.getServer();
                String playerName          = player.getName().getString();
                ShopManager mgr            = ShopManager.get(server);

                if (mgr.hasNpc(playerName)) {
                    UUID npcId = mgr.getNpcId(playerName);
                    ServerLevel sw = source.getLevel();
                    List<Villager> found = sw.getEntitiesOfClass(
                        Villager.class,
                        new AABB(
                            player.getX() - 1000, player.getY() - 128, player.getZ() - 1000,
                            player.getX() + 1000, player.getY() + 128, player.getZ() + 1000
                        ),
                        e -> e.getUUID().equals(npcId) && e.getCustomName() != null &&
                             e.getCustomName().getString().endsWith("'s Shop"));
                    if (!found.isEmpty()) {
                        player.displayClientMessage(Component.literal(I18n.get("msg.shop_exists")), false);
                        return 0;
                    }
                }

                ServerLevel world = source.getLevel();
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
                    return 1;
                } else {
                    player.displayClientMessage(Component.literal("Failed to spawn shop NPC"), false);
                    return 0;
                }
            }));

        // /close_shop - Remove your NPC
        dispatcher.register(Commands.literal("close_shop")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                ServerPlayer player  = source.getPlayerOrException();
                MinecraftServer server     = source.getServer();
                String playerName          = player.getName().getString();
                ShopManager mgr            = ShopManager.get(server);

                if (!mgr.hasNpc(playerName)) {
                    player.displayClientMessage(Component.literal(I18n.get("msg.shop_not_found")), false);
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
                    player.displayClientMessage(Component.literal(I18n.get("msg.shop_closed")), false);
                    return 1;
                } else {
                    // NPC is already dead or gone — only remove npcId, keep shop data intact
                    mgr.removeNpc(playerName);
                    mgr.setDirty();
                    player.displayClientMessage(Component.literal(I18n.get("msg.shop_npc_not_found")), false);
                    return 0;
                }
            }));

        // /shops - Opens GUI with all shops
        dispatcher.register(Commands.literal("shops")
            .executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                ServerPlayer player  = source.getPlayerOrException();
                MinecraftServer server     = source.getServer();
                String playerName          = player.getName().getString();
                ShopManager mgr            = ShopManager.get(server);
                Set<String> owners         = mgr.getAllOwners();

                boolean playerHasShop = mgr.hasShop(playerName);

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