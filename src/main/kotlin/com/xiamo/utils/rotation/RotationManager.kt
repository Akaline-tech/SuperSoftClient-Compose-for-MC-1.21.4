package com.xiamo.utils.rotation

import net.minecraft.client.MinecraftClient
import net.minecraft.util.math.MathHelper
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 旋转管理器 - 管理客户端与服务端的旋转同步
 * 支持静默旋转、移动修复、平滑旋转等功能
 */
object RotationManager {
    // ============== 目标旋转 ==============
    var targetRotation: Rotation? = null
        private set

    // ============== 服务器旋转 (发送给服务器的值) ==============
    var serverYaw: Float = 0f
        private set
    var serverPitch: Float = 0f
        private set

    private var prevServerYaw: Float = 0f
    private var prevServerPitch: Float = 0f

    // ============== 渲染旋转 (第三人称显示) ==============
    var renderYaw: Float = 0f
        private set
    var renderPitch: Float = 0f
        private set

    private var prevRenderYaw: Float = 0f
    private var prevRenderPitch: Float = 0f

    // ============== 配置选项 ==============
    var silentRotation: Boolean = true
    var renderRotation: Boolean = true

    /**
     * 移动修复模式
     */
    enum class MoveFixMode {
        OFF,      // 不修复移动
        NORMAL,   // 普通修复 - 修正移动方向

        STRICT    // 严格修复 - 完全同步
    }

    var moveFixMode: MoveFixMode = MoveFixMode.NORMAL

    // ============== 旋转速度和平滑度 ==============
    var rotationSpeed: Float = 45f
    var smoothness: Float = 0.6f

    // ============== 随机化配置 ==============
    var randomizationEnabled: Boolean = false
    var yawRandomRange: Float = 1.5f
    var pitchRandomRange: Float = 1.0f

    // ============== 随机化内部状态 ==============
    private var currentYawOffset: Float = 0f
    private var currentPitchOffset: Float = 0f
    private var randomUpdateCounter: Int = 0

    // ============== 状态标记 ==============
    var isActive: Boolean = false
        private set
    private var initialized: Boolean = false
    private var currentTickDelta: Float = 0f

    // ============== 旋转修改状态 (用于Mixin) ==============
    private var rotationApplied: Boolean = false
    private var savedClientYaw: Float = 0f
    private var savedClientPitch: Float = 0f
    private var savedPrevYaw: Float = 0f
    private var savedPrevPitch: Float = 0f

    // ============== 公共方法 ==============

    /**
     * 设置目标旋转
     */
    fun setTargetRotation(yaw: Float, pitch: Float) {
        targetRotation = Rotation(
            MathHelper.wrapDegrees(yaw),
            pitch.coerceIn(-90f, 90f)
        )
        isActive = true
    }

    fun setTargetRotation(rotation: Rotation) {
        setTargetRotation(rotation.yaw, rotation.pitch)
    }

    /**
     * 清除目标旋转
     */
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

    /**
     * 每tick更新旋转 - 在tick开始时调用
     */
    fun tick() {
        val player = MinecraftClient.getInstance().player ?: return
        val target = targetRotation ?: return

        // 更新随机化
        updateRandomValues()

        // 初始化
        if (!initialized) {
            initializeFromPlayer(player.yaw, player.pitch)
            initialized = true
        }

        // 保存上一帧
        prevServerYaw = serverYaw
        prevServerPitch = serverPitch
        prevRenderYaw = renderYaw
        prevRenderPitch = renderPitch

        // 计算目标旋转（带随机偏移）
        val targetYaw = target.yaw + currentYawOffset
        val targetPitch = (target.pitch + currentPitchOffset).coerceIn(-90f, 90f)

        // 计算差值并限制速度
        val yawDiff = MathHelper.wrapDegrees(targetYaw - serverYaw)
        val pitchDiff = targetPitch - serverPitch

        val clampedYawDiff = yawDiff.coerceIn(-rotationSpeed, rotationSpeed)
        val clampedPitchDiff = pitchDiff.coerceIn(-rotationSpeed * 0.8f, rotationSpeed * 0.8f)

        // 应用平滑度
        serverYaw = MathHelper.wrapDegrees(serverYaw + clampedYawDiff * smoothness)
        serverPitch = (serverPitch + clampedPitchDiff * smoothness).coerceIn(-90f, 90f)

        // 同步渲染旋转
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

    // ============== 移动修复方法 ==============

    /**
     * 应用旋转到玩家 - 在tick/tickMovement开始时调用
     * 这会保存客户端旋转并设置服务器旋转
     */
    fun applyRotationToPlayer(): Boolean {
        if (!isActive || targetRotation == null) return false
        if (moveFixMode == MoveFixMode.OFF) return false
        if (rotationApplied) return false

        val player = MinecraftClient.getInstance().player ?: return false

        // 保存客户端旋转
        savedClientYaw = player.yaw
        savedClientPitch = player.pitch
        savedPrevYaw = player.prevYaw
        savedPrevPitch = player.prevPitch

        // 应用服务器旋转
        player.yaw = serverYaw
        player.pitch = serverPitch
        player.prevYaw = prevServerYaw
        player.prevPitch = prevServerPitch

        rotationApplied = true
        return true
    }

    /**
     * 恢复客户端旋转 - 在sendMovementPackets之后调用
     */
    fun restoreClientRotation() {
        if (!rotationApplied) return

        val player = MinecraftClient.getInstance().player ?: return

        // 恢复客户端旋转
        player.yaw = savedClientYaw
        player.pitch = savedClientPitch
        player.prevYaw = savedPrevYaw
        player.prevPitch = savedPrevPitch

        rotationApplied = false
    }

    /**
     * 检查旋转是否已应用
     */
    fun isRotationApplied(): Boolean = rotationApplied

    /**
     * 是否应该应用移动修复
     */
    fun shouldApplyMoveFix(): Boolean {
        return isActive && moveFixMode != MoveFixMode.OFF && targetRotation != null
    }

    // ============== 服务器旋转获取方法 ==============

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

    // ============== 渲染旋转获取方法 ==============

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
