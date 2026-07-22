package com.example.shopmod.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerTickMixin extends LivingEntity {

    protected VillagerTickMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        Component customName = self.getCustomName();

        if (customName != null && customName.getString().endsWith("'s Shop")) {
            if (!self.level().isClientSide()) {
                // Health regeneration every second - use heal() directly instead of MobEffects.HEAL
                if (this.tickCount % 20 == 0) {
                    self.heal(1.0f);
                }

                // Look at nearest player every tick
                // In MC 1.21.11, use getNearestPlayer(AABB, Entity) or getClosestPlayer overload
                Player nearestPlayer = null;
                AABB searchArea = self.getBoundingBox().inflate(8.0);
                List<Player> nearbyPlayers = self.level().getEntitiesOfClass(
                    Player.class, searchArea,
                    p -> p.distanceTo(self) <= 8.0
                );
                if (!nearbyPlayers.isEmpty()) {
                    nearestPlayer = nearbyPlayers.get(0);
                }

                if (nearestPlayer != null) {
                    double dx = nearestPlayer.getX() - self.getX();
                    double dz = nearestPlayer.getZ() - self.getZ();
                    float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;

                    float currentYaw = self.getYRot();
                    float diff = yaw - currentYaw;

                    while (diff < -180) diff += 360;
                    while (diff > 180) diff -= 360;

                    float rotationSpeed = 10f;
                    float newYaw = currentYaw + Math.max(-rotationSpeed, Math.min(rotationSpeed, diff));

                    self.setYRot(newYaw);
                    self.setYHeadRot(newYaw);

                    double dy = nearestPlayer.getEyeY() - self.getEyeY();
                    double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                    float pitch = (float) -(Math.atan2(dy, horizontalDist) * 180.0 / Math.PI);
                    self.setXRot(pitch);
                }
            }
        }
    }
}