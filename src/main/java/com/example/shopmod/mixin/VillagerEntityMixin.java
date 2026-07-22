package com.example.shopmod.mixin;

import com.example.shopmod.ShopMod;
import com.example.shopmod.data.ShopManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public class VillagerEntityMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void shopmod$onInteractMob(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Villager self = (Villager) (Object) this;

        // Only process on server side
        if (self.level().isClientSide()) return;

        if (player instanceof ServerPlayer sp) {
            Component customName = self.getCustomName();
            if (customName != null) {
                String name = customName.getString();
                if (name.endsWith("'s Shop")) {
                    String ownerName = name.substring(0, name.length() - 7).trim();

                    System.out.println("[ShopMod] Right-click on shop NPC: " + ownerName + " by " + sp.getName().getString());

                    // In MC 1.21.11, ServerPlayer has no getServer() - use level().getServer()
                    ServerLevel serverLevel = (ServerLevel) sp.level();
                    ShopManager mgr = ShopManager.get(serverLevel.getServer());
                    mgr.getOrCreate(ownerName);

                    if (sp.getName().getString().equals(ownerName)) {
                        ShopMod.sendOpenOwner(sp, ownerName);
                    } else {
                        ShopMod.sendOpenBuyer(sp, ownerName);
                    }
                    cir.setReturnValue(InteractionResult.SUCCESS);
                    cir.cancel();
                }
            }
        }
    }
}