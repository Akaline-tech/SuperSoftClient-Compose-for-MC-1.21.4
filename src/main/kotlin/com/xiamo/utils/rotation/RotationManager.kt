package com.xiamo.utils.rotation

import androidx.compose.runtime.mutableStateOf
import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.MathHelper

object RotationManager {
    var targetRotation : Rotation? = null
    var prevYaw = 0f
    var prevPitch = 0f
    var isActive = mutableStateOf(true)
    var tickDelta : Float = 0f

    var packetRotation : Rotation? = null

    fun soomthRotationToPacketRotation(targetRotation : Rotation,maxSpeed : Float = 180F){
        val yawDiff = MathHelper.wrapDegrees(targetRotation.yaw - prevYaw)
        val pitchDiff = MathHelper.wrapDegrees(targetRotation.pitch - prevPitch)

        val clampedYaw = yawDiff.coerceIn(-maxSpeed, maxSpeed)
        val clampedPitch = pitchDiff.coerceIn(-maxSpeed, maxSpeed)

        packetRotation?.yaw = prevYaw + clampedYaw
        packetRotation?.pitch = prevPitch + clampedPitch

    }
    fun soomthRotationToRenderRotation(targetRotation : Rotation) : Rotation{
        val player = MinecraftClient.getInstance().player
        val speed = 0.2f
        if (player != null){
            if (prevYaw == 0f || prevPitch == 0f){
                prevYaw = player.prevYaw
                prevPitch = player.prevPitch
            }

            val yaw =  MathHelper.lerpAngleDegrees(
                tickDelta * speed,
                prevYaw,
                targetRotation.yaw
            )
            val pitch = MathHelper.lerpAngleDegrees(
                tickDelta * speed,
                prevPitch,
                targetRotation.pitch
            )

            prevYaw = yaw
            prevPitch = pitch
            return Rotation(yaw,pitch)

        }

        return Rotation(0f,0f)

    }











}



data class Rotation(var yaw:Float, var pitch:Float)