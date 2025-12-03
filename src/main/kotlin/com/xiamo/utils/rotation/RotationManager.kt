package com.xiamo.utils.rotation

import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.MathHelper
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

    var silentRotation: Boolean = false
    var renderRotation: Boolean = true
    var moveFixEnabled: Boolean = true

    var renderYaw: Float = 0f
        private set
    var renderPitch: Float = 0f
        private set

    private var prevRenderYaw: Float = 0f
    private var prevRenderPitch: Float = 0f

    var rotationSpeed: Float = 30f
    var smoothness: Float = 0.5f

    var randomizationEnabled: Boolean = true
    var yawRandomRange: Float = 2.5f
    var pitchRandomRange: Float = 1.5f
    var speedRandomRange: Float = 0.15f
    var smoothRandomRange: Float = 0.1f

    private var currentYawOffset: Float = 0f
    private var currentPitchOffset: Float = 0f
    private var currentSpeedMultiplier: Float = 1f
    private var currentSmoothMultiplier: Float = 1f

    private var randomUpdateCounter: Int = 0
    private var randomUpdateInterval: Int = 3

    var isActive: Boolean = false
    private var initialized: Boolean = false
    private var currentTickDelta: Float = 0f

    fun setTargetRotation(yaw: Float, pitch: Float) {
        targetRotation = Rotation(yaw, pitch.coerceIn(-90f, 90f))
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
        currentSpeedMultiplier = 1f
        currentSmoothMultiplier = 1f
        randomUpdateCounter = 0
    }

    fun setTickDelta(delta: Float) {
        currentTickDelta = delta
    }

    private fun updateRandomValues() {
        if (!randomizationEnabled) {
            currentYawOffset = 0f
            currentPitchOffset = 0f
            currentSpeedMultiplier = 1f
            currentSmoothMultiplier = 1f
            return
        }

        randomUpdateCounter++
        if (randomUpdateCounter >= randomUpdateInterval) {
            randomUpdateCounter = 0
            currentYawOffset = (Random.nextGaussian() * yawRandomRange * 0.5f).toFloat()
                .coerceIn(-yawRandomRange, yawRandomRange)
            currentPitchOffset = (Random.nextGaussian() * pitchRandomRange * 0.5f).toFloat()
                .coerceIn(-pitchRandomRange, pitchRandomRange)
            currentSpeedMultiplier = 1f + (Random.nextFloat() * 2f - 1f) * speedRandomRange
            currentSmoothMultiplier = 1f + (Random.nextFloat() * 2f - 1f) * smoothRandomRange
            randomUpdateInterval = Random.nextInt(2, 6)
        }
    }

    private fun Random.nextGaussian(): Double {
        var u1: Double
        var u2: Double
        do {
            u1 = nextDouble()
            u2 = nextDouble()
        } while (u1 <= 1e-10)
        return kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)
    }

    fun tick() {
        val player = MinecraftClient.getInstance().player ?: return
        val target = targetRotation ?: return

        updateRandomValues()

        if (!initialized) {
            prevServerYaw = player.yaw
            prevServerPitch = player.pitch
            serverYaw = player.yaw
            serverPitch = player.pitch
            renderYaw = player.yaw
            renderPitch = player.pitch
            prevRenderYaw = player.yaw
            prevRenderPitch = player.pitch
            initialized = true
        }

        prevServerYaw = serverYaw
        prevServerPitch = serverPitch
        prevRenderYaw = renderYaw
        prevRenderPitch = renderPitch

        val randomizedTargetYaw = target.yaw + currentYawOffset
        val randomizedTargetPitch = (target.pitch + currentPitchOffset).coerceIn(-90f, 90f)

        val yawDiff = MathHelper.wrapDegrees(randomizedTargetYaw - serverYaw)
        val pitchDiff = randomizedTargetPitch - serverPitch

        val effectiveSpeed = rotationSpeed * currentSpeedMultiplier
        val clampedYawDiff = yawDiff.coerceIn(-effectiveSpeed, effectiveSpeed)
        val clampedPitchDiff = pitchDiff.coerceIn(-effectiveSpeed, effectiveSpeed)

        val effectiveSmoothness = (smoothness * currentSmoothMultiplier).coerceIn(0.1f, 1f)

        serverYaw = MathHelper.wrapDegrees(serverYaw + clampedYawDiff * effectiveSmoothness)
        serverPitch = (serverPitch + clampedPitchDiff * effectiveSmoothness).coerceIn(-90f, 90f)

        renderYaw = serverYaw
        renderPitch = serverPitch
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

    fun smoothRotationToRenderRotation(target: Rotation): Rotation {
        return Rotation(
            MathHelper.lerpAngleDegrees(currentTickDelta, prevRenderYaw, renderYaw),
            MathHelper.lerp(currentTickDelta, prevRenderPitch, renderPitch)
        )
    }

    fun reset() {
        val player = MinecraftClient.getInstance().player ?: return
        prevServerYaw = player.yaw
        prevServerPitch = player.pitch
        serverYaw = player.yaw
        serverPitch = player.pitch
        renderYaw = player.yaw
        renderPitch = player.pitch
        prevRenderYaw = player.yaw
        prevRenderPitch = player.pitch
        initialized = true
    }

    fun calculateRotation(fromX: Double, fromY: Double, fromZ: Double, toX: Double, toY: Double, toZ: Double): Rotation {
        val diffX = toX - fromX
        val diffY = toY - fromY
        val diffZ = toZ - fromZ
        val dist = sqrt(diffX * diffX + diffZ * diffZ)
        val yaw = (Math.toDegrees(kotlin.math.atan2(diffZ, diffX)) - 90.0).toFloat()
        val pitch = (-Math.toDegrees(kotlin.math.atan2(diffY, dist))).toFloat()
        return Rotation(MathHelper.wrapDegrees(yaw), pitch.coerceIn(-90f, 90f))
    }

    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== Rotation Manager ===")
            appendLine("Active: $isActive")
            appendLine("Silent: $silentRotation")
            appendLine("Render: $renderRotation")
            appendLine("MoveFix: $moveFixEnabled")
            appendLine("Randomization: $randomizationEnabled")
            appendLine("Target: ${targetRotation?.let { "Y:%.1f P:%.1f".format(it.yaw, it.pitch) } ?: "None"}")
            appendLine("Server: Y:%.1f P:%.1f".format(serverYaw, serverPitch))
            appendLine("PrevServer: Y:%.1f P:%.1f".format(prevServerYaw, prevServerPitch))
            appendLine("RandomOffset: Y:%.2f P:%.2f".format(currentYawOffset, currentPitchOffset))
            appendLine("Speed: $rotationSpeed (x%.2f), Smooth: $smoothness (x%.2f)".format(currentSpeedMultiplier, currentSmoothMultiplier))
        }
    }
}

data class Rotation(var yaw: Float, var pitch: Float) {
    fun copy(): Rotation = Rotation(yaw, pitch)
    override fun toString(): String = "Rotation(yaw=%.2f, pitch=%.2f)".format(yaw, pitch)
}
