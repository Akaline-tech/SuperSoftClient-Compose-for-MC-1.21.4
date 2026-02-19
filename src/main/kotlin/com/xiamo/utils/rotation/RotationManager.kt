package com.xiamo.utils.rotation

import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.MathHelper
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random


object RotationManager {

    var targetRotation: Rotation? = null
        private set


    var serverYaw: Float = 0f
        private set
    var serverPitch: Float = 0f
        private set

    private var prevServerYaw: Float = 0f
    private var prevServerPitch: Float = 0f


    var renderYaw: Float = 0f
        private set
    var renderPitch: Float = 0f
        private set

    private var prevRenderYaw: Float = 0f
    private var prevRenderPitch: Float = 0f


    var silentRotation: Boolean = true
    var renderRotation: Boolean = true


    enum class MoveFixMode {
        OFF,
        NORMAL,
        STRICT
    }

    var moveFixMode: MoveFixMode = MoveFixMode.NORMAL


    var rotationSpeed: Float = 45f
    var smoothness: Float = 0.6f


    var randomizationEnabled: Boolean = false
    var yawRandomRange: Float = 1.5f
    var pitchRandomRange: Float = 1.0f


    private var currentYawOffset: Float = 0f
    private var currentPitchOffset: Float = 0f
    private var randomUpdateCounter: Int = 0


    var isActive: Boolean = false
        private set
    private var initialized: Boolean = false
    private var currentTickDelta: Float = 0f


    private var rotationApplied: Boolean = false
    private var savedClientYaw: Float = 0f
    private var savedClientPitch: Float = 0f
    private var savedPrevYaw: Float = 0f
    private var savedPrevPitch: Float = 0f




    fun setTargetRotation(yaw: Float, pitch: Float) {
        val rotation = Rotation(
            MathHelper.wrapDegrees(yaw),
            pitch.coerceIn(-90f, 90f)
        )

        targetRotation = normalizeRotation(rotation)
        isActive = true
    }

    fun setTargetRotation(rotation: Rotation) {
        setTargetRotation(rotation.yaw, rotation.pitch)
    }


    fun clearTarget() {
        targetRotation = null
        isActive = false
        initialized = false
        currentYawOffset = 0f
        currentPitchOffset = 0f
        randomUpdateCounter = 0
    }

    fun setTickDelta(delta: Float) {
        currentTickDelta = delta
    }


    fun tick() {
        val player = MinecraftClient.getInstance().player ?: return
        val target = targetRotation ?: return


        updateRandomValues()


        if (!initialized) {
            initializeFromPlayer(player.yaw, player.pitch)
            initialized = true
        }


        prevServerYaw = serverYaw
        prevServerPitch = serverPitch
        prevRenderYaw = renderYaw
        prevRenderPitch = renderPitch


        val targetYaw = target.yaw + currentYawOffset
        val targetPitch = (target.pitch + currentPitchOffset).coerceIn(-90f, 90f)


        val yawDiff = MathHelper.wrapDegrees(targetYaw - serverYaw)
        val pitchDiff = targetPitch - serverPitch

        val clampedYawDiff = yawDiff.coerceIn(-rotationSpeed, rotationSpeed)
        val clampedPitchDiff = pitchDiff.coerceIn(-rotationSpeed * 0.8f, rotationSpeed * 0.8f)


        val newYaw = serverYaw + clampedYawDiff * smoothness
        val newPitch = (serverPitch + clampedPitchDiff * smoothness).coerceIn(-90f, 90f)


        val gcdValue = gcd
        val yawChange = MathHelper.wrapDegrees(newYaw - serverYaw)
        val pitchChange = newPitch - serverPitch

        val normalizedYawChange = (yawChange / gcdValue).roundToInt().toDouble() * gcdValue
        val normalizedPitchChange = (pitchChange / gcdValue).roundToInt().toDouble() * gcdValue

        serverYaw = MathHelper.wrapDegrees(serverYaw + normalizedYawChange.toFloat())
        serverPitch = (serverPitch + normalizedPitchChange.toFloat()).coerceIn(-90f, 90f)


        renderYaw = serverYaw
        renderPitch = serverPitch
    }

    private fun initializeFromPlayer(yaw: Float, pitch: Float) {
        prevServerYaw = yaw
        prevServerPitch = pitch
        serverYaw = yaw
        serverPitch = pitch
        renderYaw = yaw
        renderPitch = pitch
        prevRenderYaw = yaw
        prevRenderPitch = pitch
    }

    private fun updateRandomValues() {
        if (!randomizationEnabled) {
            currentYawOffset = 0f
            currentPitchOffset = 0f
            return
        }

        randomUpdateCounter++
        if (randomUpdateCounter >= 3) {
            randomUpdateCounter = 0
            currentYawOffset = (Random.nextFloat() * 2f - 1f) * yawRandomRange
            currentPitchOffset = (Random.nextFloat() * 2f - 1f) * pitchRandomRange
        }
    }


    fun applyRotationToPlayer(): Boolean {
        if (!isActive || targetRotation == null) return false
        if (moveFixMode == MoveFixMode.OFF) return false
        if (rotationApplied) return false

        val player = MinecraftClient.getInstance().player ?: return false


        savedClientYaw = player.yaw
        savedClientPitch = player.pitch
        savedPrevYaw = player.lastYaw
        savedPrevPitch = player.lastPitch


        player.yaw = serverYaw
        player.pitch = serverPitch
        player.lastYaw = prevServerYaw
        player.lastPitch = prevServerPitch

        rotationApplied = true
        return true
    }


    fun restoreClientRotation() {
        if (!rotationApplied) return

        val player = MinecraftClient.getInstance().player ?: return

        // 恢复客户端旋转
        player.yaw = savedClientYaw
        player.pitch = savedClientPitch
        player.lastYaw = savedPrevYaw
        player.lastPitch = savedPrevPitch

        rotationApplied = false
    }


    fun isRotationApplied(): Boolean = rotationApplied


    fun shouldApplyMoveFix(): Boolean {
        return isActive && moveFixMode != MoveFixMode.OFF && targetRotation != null
    }



    fun getServerYawNeeded(): Float? {
        if (!isActive || targetRotation == null) return null
        return serverYaw
    }

    fun getServerPitchNeeded(): Float? {
        if (!isActive || targetRotation == null) return null
        return serverPitch
    }

    fun getPrevServerYaw(): Float? {
        if (!isActive || targetRotation == null) return null
        return prevServerYaw
    }

    fun getPrevServerPitch(): Float? {
        if (!isActive || targetRotation == null) return null
        return prevServerPitch
    }



    fun getRenderYaw(originalYaw: Float): Float {
        if (!isActive || !renderRotation) return originalYaw
        return renderYaw
    }

    fun getLerpedRenderYaw(tickDelta: Float, originalYaw: Float): Float {
        if (!isActive || !renderRotation) return originalYaw
        return MathHelper.lerpAngleDegrees(tickDelta, prevRenderYaw, renderYaw)
    }

    fun getRenderPitch(originalPitch: Float): Float {
        if (!isActive || !renderRotation) return originalPitch
        return renderPitch
    }

    fun getLerpedRenderPitch(tickDelta: Float, originalPitch: Float): Float {
        if (!isActive || !renderRotation) return originalPitch
        return MathHelper.lerp(tickDelta, prevRenderPitch, renderPitch)
    }

    // ============== GCD Fix ==============


    val gcd: Double
        get() {
            val mc = MinecraftClient.getInstance()
            val f = mc.options.mouseSensitivity.value * 0.6 + 0.2
            return f * f * f * 8.0 * 0.15
        }

    // ============== 工具方法 ==============

    fun reset() {
        val player = MinecraftClient.getInstance().player ?: return
        initializeFromPlayer(player.yaw, player.pitch)
        initialized = true
        rotationApplied = false
    }

    fun calculateRotation(
        fromX: Double, fromY: Double, fromZ: Double,
        toX: Double, toY: Double, toZ: Double
    ): Rotation {
        val diffX = toX - fromX
        val diffY = toY - fromY
        val diffZ = toZ - fromZ
        val dist = sqrt(diffX * diffX + diffZ * diffZ)
        val yaw = (Math.toDegrees(kotlin.math.atan2(diffZ, diffX)) - 90.0).toFloat()
        val pitch = (-Math.toDegrees(kotlin.math.atan2(diffY, dist))).toFloat()
        return Rotation(MathHelper.wrapDegrees(yaw), pitch.coerceIn(-90f, 90f))
    }


    fun normalizeRotation(rotation: Rotation): Rotation {
        val currentRot = if (isActive && targetRotation != null) {
            Rotation(serverYaw, serverPitch)
        } else {
            val player = MinecraftClient.getInstance().player ?: return rotation
            Rotation(player.yaw, player.pitch)
        }

        val gcdValue = gcd


        val yawDiff = MathHelper.wrapDegrees(rotation.yaw - currentRot.yaw)
        val pitchDiff = rotation.pitch - currentRot.pitch


        val normalizedYawDiff = (yawDiff / gcdValue).roundToInt().toDouble() * gcdValue
        val normalizedPitchDiff = (pitchDiff / gcdValue).roundToInt().toDouble() * gcdValue


        val normalizedYaw = currentRot.yaw + normalizedYawDiff.toFloat()
        val normalizedPitch = (currentRot.pitch + normalizedPitchDiff.toFloat()).coerceIn(-90f, 90f)

        return Rotation(normalizedYaw, normalizedPitch)
    }

    fun isRotationReached(threshold: Float = 5f): Boolean {
        val target = targetRotation ?: return false
        val yawDiff = kotlin.math.abs(MathHelper.wrapDegrees(target.yaw - serverYaw))
        val pitchDiff = kotlin.math.abs(target.pitch - serverPitch)
        return yawDiff < threshold && pitchDiff < threshold
    }

    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== RotationManager ===")
            appendLine("Active: $isActive | MoveFix: $moveFixMode")
            appendLine("Target: ${targetRotation?.let { "Y:%.1f P:%.1f".format(it.yaw, it.pitch) } ?: "None"}")
            appendLine("Server: Y:%.1f P:%.1f".format(serverYaw, serverPitch))
            appendLine("Applied: $rotationApplied")
        }
    }
}

data class Rotation(var yaw: Float, var pitch: Float) {
    fun copy(): Rotation = Rotation(yaw, pitch)

    fun distanceTo(other: Rotation): Float {
        val yawDiff = kotlin.math.abs(MathHelper.wrapDegrees(yaw - other.yaw))
        val pitchDiff = kotlin.math.abs(pitch - other.pitch)
        return kotlin.math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff)
    }

    override fun toString(): String = "Rotation(yaw=%.2f, pitch=%.2f)".format(yaw, pitch)
}
