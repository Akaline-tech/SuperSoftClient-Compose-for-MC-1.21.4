package com.xiamo.mixin;

import com.xiamo.utils.rotation.RotationManager;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端玩家 Mixin
 * 实现服务端转头
 *
 * 注意：MoveFix 已移除，因为会导致 Simulation 违规。
 * 玩家移动方向将基于自己的视角，而非服务端旋转。
 *
 * 关键时序：
 * 1. MinecraftClient.tick() HEAD → TickEvent → KillAura 设置目标
 * 2. ClientPlayerEntity.tick() HEAD → 更新 RotationManager，锁定 serverYaw
 * 3. sendMovementPackets() → 发送锁定的 serverYaw
 */
@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientEntity {

    // ===== 本tick的服务端旋转（在tick开始时锁定） =====
    @Unique
    private Float supersoft$currentTickServerYaw = null;
    @Unique
    private Float supersoft$currentTickServerPitch = null;
    @Unique
    private Float supersoft$currentTickPrevServerYaw = null;
    @Unique
    private Float supersoft$currentTickPrevServerPitch = null;

    // ===== 发送数据包时的原始值保存 =====
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

    /**
     * 在 tick 开始时更新 RotationManager 并锁定本tick的服务端旋转
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void supersoft$onTickStart(CallbackInfo ci) {
        // 更新转头管理器
        RotationManager.INSTANCE.tick();

        // 锁定本tick的服务端旋转值
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

    /**
     * 在发送移动数据包前修改旋转
     * 注意：不修改移动计算，只修改发送的旋转
     * 这意味着玩家会朝自己看的方向移动，但服务器看到的旋转不同
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void supersoft$onPreSendMovementPackets(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

        if (supersoft$currentTickServerYaw != null && supersoft$currentTickServerPitch != null) {
            // 保存原始值
            supersoft$originalYaw = player.getYaw();
            supersoft$originalPitch = player.getPitch();
            supersoft$originalPrevYaw = player.prevYaw;
            supersoft$originalPrevPitch = player.prevPitch;
            supersoft$rotationModified = true;

            // 设置服务端旋转
            player.setYaw(supersoft$currentTickServerYaw);
            player.setPitch(supersoft$currentTickServerPitch);

            // 设置 prevYaw/prevPitch
            if (supersoft$currentTickPrevServerYaw != null && supersoft$currentTickPrevServerPitch != null) {
                player.prevYaw = supersoft$currentTickPrevServerYaw;
                player.prevPitch = supersoft$currentTickPrevServerPitch;
            }
        }
    }

    /**
     * 在发送移动数据包后恢复原始旋转
     */
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
