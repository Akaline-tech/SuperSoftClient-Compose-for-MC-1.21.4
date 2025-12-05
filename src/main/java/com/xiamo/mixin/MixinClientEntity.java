package com.xiamo.mixin;

import com.xiamo.utils.rotation.RotationManager;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientEntity {


    @Unique
    private Float supersoft$currentTickServerYaw = null;
    @Unique
    private Float supersoft$currentTickServerPitch = null;
    @Unique
    private Float supersoft$currentTickPrevServerYaw = null;
    @Unique
    private Float supersoft$currentTickPrevServerPitch = null;


    @Unique
    private float supersoft$originalYaw;
    @Unique
    private float supersoft$originalPitch;
    @Unique
    private float supersoft$originalPrevYaw;
    @Unique
    private float supersoft$originalPrevPitch;
    @Unique
    private boolean supersoft$rotationModified = false;


    @Inject(method = "tick", at = @At("HEAD"))
    private void supersoft$onTickStart(CallbackInfo ci) {
        RotationManager.INSTANCE.tick();

        if (RotationManager.INSTANCE.isActive() && RotationManager.INSTANCE.getTargetRotation() != null) {
            supersoft$currentTickServerYaw = RotationManager.INSTANCE.getServerYawNeeded();
            supersoft$currentTickServerPitch = RotationManager.INSTANCE.getServerPitchNeeded();
            supersoft$currentTickPrevServerYaw = RotationManager.INSTANCE.getPrevServerYaw();
            supersoft$currentTickPrevServerPitch = RotationManager.INSTANCE.getPrevServerPitch();
        } else {
            supersoft$currentTickServerYaw = null;
            supersoft$currentTickServerPitch = null;
            supersoft$currentTickPrevServerYaw = null;
            supersoft$currentTickPrevServerPitch = null;
        }
    }


    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void supersoft$onPreSendMovementPackets(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (supersoft$currentTickServerYaw != null && supersoft$currentTickServerPitch != null) {
            supersoft$originalYaw = player.getYaw();
            supersoft$originalPitch = player.getPitch();
            supersoft$originalPrevYaw = player.prevYaw;
            supersoft$originalPrevPitch = player.prevPitch;
            supersoft$rotationModified = true;
            player.setYaw(supersoft$currentTickServerYaw);
            player.setPitch(supersoft$currentTickServerPitch);

            if (supersoft$currentTickPrevServerYaw != null && supersoft$currentTickPrevServerPitch != null) {
                player.prevYaw = supersoft$currentTickPrevServerYaw;
                player.prevPitch = supersoft$currentTickPrevServerPitch;
            }
        }
    }


    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void supersoft$onPostSendMovementPackets(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (supersoft$rotationModified) {
            player.setYaw(supersoft$originalYaw);
            player.setPitch(supersoft$originalPitch);
            player.prevYaw = supersoft$originalPrevYaw;
            player.prevPitch = supersoft$originalPrevPitch;
            supersoft$rotationModified = false;
        }
    }
}
