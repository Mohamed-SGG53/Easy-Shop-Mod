package com.example.shopmod.mixin;

import com.example.shopmod.ShopMod;
import com.example.shopmod.data.ShopManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin {

    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void shopmod$onInteractMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        
        // Only process on server side
        if (self.getEntityWorld().isClient()) return;
        
        if (player instanceof ServerPlayerEntity sp) {
            Text customName = self.getCustomName();
            if (customName != null) {
                String name = customName.getString();
                if (name.endsWith("'s Shop")) {
                    String ownerName = name.substring(0, name.length() - 7).trim();
                    
                    // Log for debugging
                    System.out.println("[ShopMod] Right-click on shop NPC: " + ownerName + " by " + sp.getName().getString());
                    
                    ShopManager mgr = ShopManager.get(sp.getCommandSource().getServer());
                    mgr.getOrCreate(ownerName);
                    
                    if (sp.getName().getString().equals(ownerName)) {
                        ShopMod.sendOpenOwner(sp, ownerName);
                    } else {
                        ShopMod.sendOpenBuyer(sp, ownerName);
                    }
                    cir.setReturnValue(ActionResult.SUCCESS);
                    cir.cancel();
                }
            }
        }
    }
}
