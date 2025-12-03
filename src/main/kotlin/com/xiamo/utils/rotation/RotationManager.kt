package com.xiamo.utils.rotation

import androidx.compose.runtime.mutableStateOf
import com.xiamo.event.EventTarget
import com.xiamo.event.PlayerMovementTickPacketSendPre
import net.minecraft.client.MinecraftClient
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.MathHelper

object RotationManager {
    var targetRotation : Rotation? = null
    var prevYaw = 0f
    var prevPitch = 0f
    var isActive = mutableStateOf(true)
    var tickDelta : Float = 0f

    var serverPrevYaw : Float = 0f
    var serverPrevPitch : Float = 0f

    var serverYaw : Float = 0f
    var serverPitch : Float = 0f


    fun soomthRotationToPacketRotation(targetRotation : Rotation,maxSpeed : Float = 180F){
        val yawDiff = MathHelper.wrapDegrees(targetRotation.yaw - serverPrevYaw)
        val pitchDiff = MathHelper.wrapDegrees(targetRotation.pitch - serverPrevPitch)

        val clampedYaw = yawDiff.coerceIn(-maxSpeed, maxSpeed)
        val clampedPitch = pitchDiff.coerceIn(-maxSpeed, maxSpeed)

        val newYaw = serverPrevYaw + clampedYaw
        val newPitch = serverPrevPitch + clampedPitch

        serverYaw = applyGCDFix(newYaw, serverPrevYaw)
        serverPitch = applyGCDFix(newPitch, serverPrevPitch).coerceIn(-90f, 90f)

    }
    fun soomthRotationToRenderRotation(targetRotation : Rotation) : Rotation{
        val player = MinecraftClient.getInstance().player
        val speed = 0.5f
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


    private fun applyGCDFix(current: Float, previous: Float): Float {
        val sensitivity = MinecraftClient.getInstance().options.mouseSensitivity.value.toFloat()
        val f = sensitivity * 0.6f + 0.2f
        val gcd = f * f * f * 1.2f
        var delta = current - previous
        delta -= delta % gcd
        return previous + delta
    }



    @EventTarget
    fun onSendMovementPacket(event: PlayerMovementTickPacketSendPre){

    }







}



data class Rotation(var yaw:Float, var pitch:Float)