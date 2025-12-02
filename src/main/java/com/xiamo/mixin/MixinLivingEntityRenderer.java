package com.xiamo.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.xiamo.utils.rotation.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer {

    @Unique
    private LivingEntity renderEntity;

    @Inject(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("HEAD")
    )
    private void onUpdateRenderState(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        this.renderEntity = entity;
        RotationManager.INSTANCE.setTickDelta(tickDelta);
    }

    @ModifyExpressionValue(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"
            )
    )
    private float hookPitch(float original) {
        if (renderEntity != MinecraftClient.getInstance().player) return original;
        if (!RotationManager.INSTANCE.isActive().getValue()) return original;
        if (RotationManager.INSTANCE.getTargetRotation() != null) {
            return RotationManager.INSTANCE.soomthRotationToRenderRotation(RotationManager.INSTANCE.getTargetRotation()).getPitch();
        }
        return original;
    }


    @ModifyExpressionValue(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F"
            )
    )
    private float hookBodyYaw(float original) {
        if (renderEntity != MinecraftClient.getInstance().player) return original;
        if (!RotationManager.INSTANCE.isActive().getValue()) return original;
        if (RotationManager.INSTANCE.getTargetRotation() != null) {
            return RotationManager.INSTANCE.soomthRotationToRenderRotation(RotationManager.INSTANCE.getTargetRotation()).getYaw();
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F",
                    ordinal = 0
            )
    )
    private float hookHeadYaw(float original) {
        if (renderEntity != MinecraftClient.getInstance().player) return original;
        if (!RotationManager.INSTANCE.isActive().getValue()) return original;
        if (RotationManager.INSTANCE.getTargetRotation() != null) {
            return  RotationManager.INSTANCE.soomthRotationToRenderRotation(RotationManager.INSTANCE.getTargetRotation()).getYaw();
        }
        return original;
    }
}
