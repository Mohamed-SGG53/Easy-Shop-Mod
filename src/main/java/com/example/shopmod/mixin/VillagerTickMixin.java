package com.example.shopmod.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerTickMixin extends LivingEntity {

    protected VillagerTickMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        Text customName = self.getCustomName();
        
        if (customName != null && customName.getString().endsWith("'s Shop")) {
            if (!self.getEntityWorld().isClient()) {
                // Health regeneration every second
                if (this.age % 20 == 0) {
                    StatusEffectInstance health = new StatusEffectInstance(
                        StatusEffects.INSTANT_HEALTH,
                        1,
                        0,
                        false,
                        false
                    );
                    self.addStatusEffect(health);
                }
                
                // Look at nearest player every tick
                PlayerEntity nearestPlayer = self.getEntityWorld().getClosestPlayer(self, 8.0);
                if (nearestPlayer != null) {
                    // Calculate angle to player
                    double dx = nearestPlayer.getX() - self.getX();
                    double dz = nearestPlayer.getZ() - self.getZ();
                    float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f;
                    
                    // Smoothly rotate towards player
                    float currentYaw = self.getYaw();
                    float diff = yaw - currentYaw;
                    
                    // Normalize angle difference to -180 to 180
                    while (diff < -180) diff += 360;
                    while (diff > 180) diff -= 360;
                    
                    // Rotate smoothly (max 10 degrees per tick)
                    float rotationSpeed = 10f;
                    float newYaw = currentYaw + Math.max(-rotationSpeed, Math.min(rotationSpeed, diff));
                    
                    self.setYaw(newYaw);
                    self.setHeadYaw(newYaw);
                    
                    // Also look up/down at player
                    double dy = nearestPlayer.getEyeY() - self.getEyeY();
                    double horizontalDist = Math.sqrt(dx * dx + dz * dz);
                    float pitch = (float) -(Math.atan2(dy, horizontalDist) * 180.0 / Math.PI);
                    self.setPitch(pitch);
                }
            }
        }
    }
}
