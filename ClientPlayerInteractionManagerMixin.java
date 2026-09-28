package com.bvents.bv_anchor_helper.mixin.client;

import com.bvents.bv_anchor_helper.client.Bv_anchor_helperClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Unique
    private ItemStack bvAnchorHelper$lastUsedStack = ItemStack.EMPTY;

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void bvAnchorHelper$beforeInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                                    CallbackInfoReturnable<ActionResult> cir) {
        this.bvAnchorHelper$lastUsedStack = player.getStackInHand(hand).copy();
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void bvAnchorHelper$afterInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult,
                                                   CallbackInfoReturnable<ActionResult> cir) {
        if (!cir.getReturnValue().isAccepted()) {
            this.bvAnchorHelper$lastUsedStack = ItemStack.EMPTY;
            return;
        }
        Bv_anchor_helperClient.onSuccessfulBlockUse(this.bvAnchorHelper$lastUsedStack, hand);
        this.bvAnchorHelper$lastUsedStack = ItemStack.EMPTY;
    }
}
