package com.example.shopmod.entity;

import com.example.shopmod.ShopMod;
import com.example.shopmod.data.ShopManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ShopVillagerEntity extends VillagerEntity {

    private String ownerName = null;

    public ShopVillagerEntity(World world, String ownerName) {
        super(EntityType.VILLAGER, world);
        this.ownerName = ownerName;
        System.out.println("[ShopMod] Created ShopVillagerEntity with owner: " + ownerName);
    }

    public ShopVillagerEntity(EntityType<? extends VillagerEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
    }

    public String getOwnerName() { 
        return ownerName;
    }
    
    public void setOwnerName(String n) { 
        this.ownerName = n;
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        if (ownerName != null && !ownerName.isEmpty()) {
            view.putString("ShopOwner", ownerName);
        }
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        ownerName = view.getString("ShopOwner", "");
        if (ownerName.isEmpty()) {
            ownerName = null;
        }
        System.out.println("[ShopMod] Read ShopVillagerEntity owner from NBT: " + ownerName);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        System.out.println("[ShopMod] ShopVillagerEntity.interactMob called! Owner: " + ownerName + ", CustomName: " + getCustomName());
        return handleShopInteraction(player);
    }
    
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        System.out.println("[ShopMod] ShopVillagerEntity.interact called! Owner: " + ownerName + ", CustomName: " + getCustomName());
        ActionResult result = handleShopInteraction(player);
        if (result != ActionResult.PASS) {
            return result;
        }
        return super.interact(player, hand);
    }
    
    private ActionResult handleShopInteraction(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity sp) {
            // Get owner from field or custom name
            String owner = ownerName;
            
            if ((owner == null || owner.isEmpty()) && getCustomName() != null) {
                String name = getCustomName().getString();
                if (name.endsWith("'s Shop")) {
                    owner = name.substring(0, name.length() - 7).trim();
                }
            }
            
            System.out.println("[ShopMod] Resolved owner: " + owner);
            
            if (owner != null && !owner.isEmpty()) {
                ShopManager mgr = ShopManager.get(sp.getCommandSource().getServer());
                mgr.getOrCreate(owner);
                
                if (sp.getName().getString().equals(owner)) {
                    ShopMod.sendOpenOwner(sp, owner);
                } else {
                    ShopMod.sendOpenBuyer(sp, owner);
                }
                return ActionResult.SUCCESS;
            }
        }
        return ActionResult.PASS;
    }
}
