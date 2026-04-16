package com.example.shopmod.entity;

import com.example.shopmod.ShopMod;
import com.example.shopmod.data.ShopManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public class ShopVillagerEntity extends Villager {

    private String ownerName = null;

    public ShopVillagerEntity(Level world, String ownerName) {
        super(EntityType.VILLAGER, world);
        this.ownerName = ownerName;
        System.out.println("[ShopMod] Created ShopVillagerEntity with owner: " + ownerName);
    }

    public ShopVillagerEntity(EntityType<? extends Villager> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String n) {
        this.ownerName = n;
    }

    // In MC 1.21.11, Villager uses mobInteract instead of interactMob
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        System.out.println("[ShopMod] ShopVillagerEntity.mobInteract called! Owner: " + ownerName + ", CustomName: " + getCustomName());
        return handleShopInteraction(player);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        System.out.println("[ShopMod] ShopVillagerEntity.interact called! Owner: " + ownerName + ", CustomName: " + getCustomName());
        InteractionResult result = handleShopInteraction(player);
        if (result != InteractionResult.PASS) {
            return result;
        }
        return super.interact(player, hand);
    }

    private InteractionResult handleShopInteraction(Player player) {
        if (player instanceof ServerPlayer sp) {
            String owner = ownerName;

            if ((owner == null || owner.isEmpty()) && getCustomName() != null) {
                String name = getCustomName().getString();
                if (name.endsWith("'s Shop")) {
                    owner = name.substring(0, name.length() - 7).trim();
                }
            }

            System.out.println("[ShopMod] Resolved owner: " + owner);

            if (owner != null && !owner.isEmpty()) {
                // In MC 1.21.11, ServerPlayer has no getServer() - use level().getServer()
                ServerLevel serverLevel = (ServerLevel) sp.level();
                ShopManager mgr = ShopManager.get(serverLevel.getServer());
                mgr.getOrCreate(owner);

                if (sp.getName().getString().equals(owner)) {
                    ShopMod.sendOpenOwner(sp, owner);
                } else {
                    ShopMod.sendOpenBuyer(sp, owner);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }
}
