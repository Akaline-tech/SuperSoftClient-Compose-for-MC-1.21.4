package com.xiamo.mixin;

import com.xiamo.utils.rotation.RotationManager;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ClientPlayerEntity Mixin - 处理旋转同步和移动修复
 *
 * 关键流程:
 * 1. tick() 开始 -> 更新RotationManager -> 应用服务器旋转
 * 2. tickMovement() -> 使用服务器旋转计算移动
 * 3. sendMovementPackets() -> 发送服务器旋转
 * 4. sendMovementPackets() 结束 -> 恢复客户端旋转
 */
@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientEntity {

    @Unique
    private boolean supersoft$rotationApplied = false;

    /**
     * tick开始时:
     * 1. 更新RotationManager
     * 2. 应用服务器旋转到玩家 (这样tickMovement会使用正确的yaw)
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void supersoft$onTickStart(CallbackInfo ci) {
        // 更新旋转管理器
        RotationManager.INSTANCE.tick();

        // 应用旋转 (会保存客户端旋转并设置服务器旋转)
        supersoft$rotationApplied = RotationManager.INSTANCE.applyRotationToPlayer();
    }

    /**
     * sendMovementPackets结束后恢复客户端旋转
     */
    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void supersoft$onPostSendMovementPackets(CallbackInfo ci) {
        if (supersoft$rotationApplied) {
            RotationManager.INSTANCE.restoreClientRotation();
            supersoft$rotationApplied = false;
        }
    }
}
