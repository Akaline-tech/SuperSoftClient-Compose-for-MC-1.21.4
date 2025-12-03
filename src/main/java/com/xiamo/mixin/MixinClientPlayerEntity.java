package com.xiamo.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 客户端玩家 Mixin
 * 注意：第一人称视角不做任何修改，让用户保持完全控制
 * 转头只影响：
 * 1. 发送给服务器的数据包（MixinClientEntity）
 * 2. 第三人称模型渲染（MixinLivingEntityRenderer）
 */
@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    // 不修改 getYaw/getPitch，让用户保持第一人称视角的完全控制
}