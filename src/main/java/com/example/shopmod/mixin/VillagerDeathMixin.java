package com.example.shopmod.mixin;

import com.example.shopmod.data.I18n;
import com.example.shopmod.data.ShopManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Villager.class)
public class VillagerDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void shopmod$onShopDeath(DamageSource source, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;

        if (self.level().isClientSide()) return;

        Component customName = self.getCustomName();
        if (customName == null) return;

        String name = customName.getString();
        if (!name.endsWith("'s Shop")) return;

        String ownerName = name.substring(0, name.length() - 7).trim();

        Entity killer = source.getEntity();
        String killerName = "unknown";
        if (killer != null) {
            killerName = killer.getName().getString();
        }

        if (self.level() instanceof ServerLevel serverLevel) {
            AABB area = self.getBoundingBox().inflate(64.0);
            List<ServerPlayer> nearby = serverLevel.getEntitiesOfClass(
                ServerPlayer.class, area
            );
            Component msg = Component.literal(
                I18n.get("msg.shop_npc_killed", ownerName, killerName)
            );
            for (ServerPlayer p : nearby) {
                p.sendSystemMessage(msg);
            }

            ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerName);
            if (owner != null && !nearby.contains(owner)) {
                owner.sendSystemMessage(msg);
            }

            ShopManager mgr = ShopManager.get(serverLevel.getServer());
            if (mgr.hasNpc(ownerName)) {
                mgr.removeNpc(ownerName);
                mgr.setDirty();
            }
        }
    }
}