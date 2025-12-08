package com.xiamo.module.modules.combat

import androidx.compose.runtime.mutableStateOf
import com.xiamo.module.Category
import com.xiamo.module.Module
import com.xiamo.setting.AbstractSetting
import com.xiamo.utils.rotation.RotationManager
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.lwjgl.glfw.GLFW
import java.util.concurrent.CopyOnWriteArrayList

/**
 * KillAura - 自动攻击模块
 */
object KillAura : Module("KillAura", "自动攻击附近的实体", Category.Combat) {
    var isAttacking = mutableStateOf(false)
    var targetObject = mutableStateOf<LivingEntity?>(null)

    // ============== 基础设置 ==============
    private val rangeSetting = numberSetting(
        "Range", "攻击范围",
        3.5, 2.0, 6.0, 0.1
    )

    // ============== 旋转设置 ==============
    private val rotationSpeedSetting = numberSetting(
        "RotSpeed", "转头速度",
        60.0, 10.0, 180.0, 5.0
    )

    private val smoothnessSetting = numberSetting(
        "Smooth", "平滑度",
        0.7, 0.1, 1.0, 0.05
    )

    private val silentRotationSetting = booleanSetting(
        "Silent", "静默转头",
        true
    )

    // ============== 移动修复设置 ==============
    private val moveFixSetting = modeSetting(
        "MoveFix", "移动修复模式",
        "Normal", "Off", "Normal", "Strict"
    )

    // ============== 攻击设置 ==============
    private val autoAttackSetting = booleanSetting(
        "AutoAttack", "自动攻击",
        true
    )

    private val hitChanceSetting = numberSetting(
        "HitChance", "命中率%",
        100.0, 50.0, 100.0, 5.0
    )

    // ============== 挥手动画设置 ==============
    private val swingModeSetting = modeSetting(
        "SwingMode", "挥手动画模式",
        "Normal", "Normal", "Client", "Packet", "None"
    )

    // ============== 预测设置 ==============
    private val predictSetting = booleanSetting(
        "Predict", "目标预测",
        true
    )

    private val predictFactorSetting = numberSetting(
        "PredictFactor", "预测强度",
        2.0, 0.5, 5.0, 0.5
    ).apply {
        dependency = { predictSetting.value }
    }

    // ============== 目标信息 ==============
    val targetBarSetting = booleanSetting(
        "TargetBar", "目标信息",
        true
    )

    private val randomizeSetting = booleanSetting(
        "Randomize", "随机化转头",
        false
    )

    // ============== 内部状态 ==============
    private var currentTarget: LivingEntity? = null
    private var lastTargetPos: Vec3d? = null

    init {
        this.key = GLFW.GLFW_KEY_G
    }

    override fun onSettingChanged(setting: AbstractSetting<*>) {
        super.onSettingChanged(setting)
        updateRotationConfig()
    }

    private fun updateRotationConfig() {
        // 静默模式时不渲染旋转（第三人称不显示）
        RotationManager.renderRotation = !silentRotationSetting.value

        // 移动修复模式
        RotationManager.moveFixMode = when (moveFixSetting.value) {
            "Off" -> RotationManager.MoveFixMode.OFF
            "Normal" -> RotationManager.MoveFixMode.NORMAL
            "Strict" -> RotationManager.MoveFixMode.STRICT
            else -> RotationManager.MoveFixMode.NORMAL
        }

        // 旋转参数
        RotationManager.rotationSpeed = rotationSpeedSetting.floatValue
        RotationManager.smoothness = smoothnessSetting.floatValue
        RotationManager.randomizationEnabled = randomizeSetting.value
    }

    override fun toggle() {
        RotationManager.reset()
        super.toggle()
    }

    override fun enable() {
        updateRotationConfig()
        currentTarget = null
        lastTargetPos = null
        super.enable()
    }

    override fun onTick() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return
        val world = mc.world ?: return

        val attackRange = rangeSetting.value

        // 寻找目标
        val targets = CopyOnWriteArrayList<LivingEntity>()
        world.entities.forEach { entity ->
            if (entity is LivingEntity
                && entity.isAlive
                && entity.isAttackable
                && entity != player
                && player.squaredDistanceTo(entity) < attackRange * attackRange
            ) {
                targets.add(entity)
            }
        }

        if (targets.isEmpty()) {
            isAttacking.value = false
            currentTarget = null
            lastTargetPos = null
            targetObject.value = null
            RotationManager.clearTarget()
            return
        }

        // 选择最近的目标
        val target = targets.minByOrNull { it.distanceTo(player) } ?: return
        isAttacking.value = true
        targetObject.value = target

        // 计算目标位置
        val targetPos = getTargetPosition(target)

        // 更新目标追踪
        if (currentTarget != target) {
            lastTargetPos = null
        }
        currentTarget = target
        lastTargetPos = target.pos

        // 计算旋转
        val rotation = RotationManager.calculateRotation(
            player.eyePos.x, player.eyePos.y, player.eyePos.z,
            targetPos.x, targetPos.y, targetPos.z
        )

        if (silentRotationSetting.value) {
            // 静默旋转模式
            RotationManager.setTargetRotation(rotation)

            if (autoAttackSetting.value && canAttack(target)) {
                attackTarget(target)
            }
        } else {
            // 非静默模式 - 直接旋转玩家视角
            rotatePlayer(rotation.yaw, rotation.pitch)

            if (autoAttackSetting.value && canAttackNonSilent(target)) {
                attackTarget(target)
            }
        }

        super.onTick()
    }

    /**
     * 获取目标位置（带预测）
     */
    private fun getTargetPosition(target: LivingEntity): Vec3d {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return target.eyePos

        // 瞄准身体中上部
        var targetY = target.y + target.height * 0.7

        if (!predictSetting.value || lastTargetPos == null) {
            return Vec3d(target.x, targetY, target.z)
        }

        // 计算速度向量
        val velocity = Vec3d(
            target.x - target.prevX,
            target.y - target.prevY,
            target.z - target.prevZ
        )

        // 根据距离调整预测
        val distance = player.distanceTo(target)
        val factor = predictFactorSetting.value * (distance / 3.0).coerceIn(0.5, 2.0)

        // 预测位置
        val predictedX = target.x + velocity.x * factor
        val predictedY = targetY + velocity.y * factor
        val predictedZ = target.z + velocity.z * factor

        return Vec3d(predictedX, predictedY, predictedZ)
    }

    /**
     * 直接旋转玩家（非静默模式）
     */
    private fun rotatePlayer(targetYaw: Float, targetPitch: Float) {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        val speed = rotationSpeedSetting.floatValue
        val smooth = smoothnessSetting.floatValue

        val yawDiff = MathHelper.wrapDegrees(targetYaw - player.yaw)
        val pitchDiff = targetPitch - player.pitch

        val yawStep = calculateStep(yawDiff, speed, smooth)
        val pitchStep = calculateStep(pitchDiff, speed * 0.7f, smooth)

        player.yaw = player.yaw + yawStep
        player.pitch = (player.pitch + pitchStep).coerceIn(-90f, 90f)
    }

    private fun calculateStep(diff: Float, maxSpeed: Float, smooth: Float): Float {
        val absDiff = kotlin.math.abs(diff)
        if (absDiff < 0.5f) return diff

        val exponential = diff * (1f - smooth) * 0.8f
        val linear = kotlin.math.sign(diff) * kotlin.math.min(absDiff, maxSpeed) * smooth * 0.5f
        val step = exponential + linear

        return step.coerceIn(-maxSpeed, maxSpeed)
    }

    /**
     * 检查是否可以攻击（静默模式）
     */
    private fun canAttack(target: LivingEntity): Boolean {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return false

        // 检查攻击冷却
        if (player.getAttackCooldownProgress(0.5f) < 1.0f) return false

        // 检查距离
        if (player.squaredDistanceTo(target) > rangeSetting.value * rangeSetting.value) return false

        // 命中率检查
        if (hitChanceSetting.value < 100.0 && Math.random() * 100 > hitChanceSetting.value) return false

        // 检查旋转是否到位
        val targetRot = RotationManager.targetRotation ?: return false
        val serverYaw = RotationManager.serverYaw
        val serverPitch = RotationManager.serverPitch

        val yawDiff = kotlin.math.abs(MathHelper.wrapDegrees(targetRot.yaw - serverYaw))
        val pitchDiff = kotlin.math.abs(targetRot.pitch - serverPitch)

        // 允许一定角度误差
        return yawDiff < 30f && pitchDiff < 30f
    }

    /**
     * 检查是否可以攻击（非静默模式）
     */
    private fun canAttackNonSilent(target: LivingEntity): Boolean {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return false

        if (player.getAttackCooldownProgress(0.5f) < 1.0f) return false
        if (player.squaredDistanceTo(target) > rangeSetting.value * rangeSetting.value) return false
        if (hitChanceSetting.value < 100.0 && Math.random() * 100 > hitChanceSetting.value) return false

        val targetPos = getTargetPosition(target)
        val rotation = RotationManager.calculateRotation(
            player.eyePos.x, player.eyePos.y, player.eyePos.z,
            targetPos.x, targetPos.y, targetPos.z
        )

        val yawDiff = kotlin.math.abs(MathHelper.wrapDegrees(rotation.yaw - player.yaw))
        val pitchDiff = kotlin.math.abs(rotation.pitch - player.pitch)

        return yawDiff < 35f && pitchDiff < 35f
    }

    /**
     * 攻击目标
     */
    private fun attackTarget(target: LivingEntity) {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return
        val interactionManager = mc.interactionManager ?: return

        // 执行攻击
        interactionManager.attackEntity(player, target)

        // 根据挥手模式处理动画
        when (swingModeSetting.value) {
            "Normal" -> {
                // 正常挥手 - 服务器和客户端都能看到
                player.swingHand(Hand.MAIN_HAND)
            }
            "Client" -> {
                // 仅客户端挥手 - 只有自己能看到
                player.swingHand(Hand.MAIN_HAND, false)
            }
            "Packet" -> {
                // 仅发包 - 服务器认为你挥手了，但本地不播放动画
                mc.networkHandler?.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
            }
            "None" -> {
                // 不挥手
            }
        }
    }

    override fun disable() {
        currentTarget = null
        lastTargetPos = null
        targetObject.value = null
        RotationManager.clearTarget()
        isAttacking.value = false
        super.disable()
    }
}
