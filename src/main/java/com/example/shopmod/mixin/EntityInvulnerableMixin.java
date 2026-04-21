package com.example.shopmod.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityInvulnerableMixin {

    @Inject(method = "isInvulnerable", at = @At("HEAD"), cancellable = true)
    private void onIsInvulnerable(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Villager) {
            Component customName = self.getCustomName();
            if (customName != null && customName.getString().endsWith("'s Shop")) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }
}
