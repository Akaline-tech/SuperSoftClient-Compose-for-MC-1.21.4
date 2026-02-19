package com.xiamo.module.modules.movement

import com.xiamo.module.Category
import com.xiamo.module.Module
import com.xiamo.setting.AbstractSetting
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.effect.StatusEffects
import org.lwjgl.glfw.GLFW
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object Speed : Module("Speed", "增加移动速度", Category.Movement) {

    private val modeSetting = modeSetting(
        "Mode", "速度模式",
        "BHop",
        "BHop", "Strafe", "LowHop", "YPort", "Legit", "OnGround", "Vanilla", "Jump"
    )

    private val speedBoostSetting = numberSetting(
        "Boost", "加速强度",
        1.2, 0.5, 2.5, 0.1
    )

    private val jumpHeightSetting = numberSetting(
        "JumpHeight", "跳跃高度",
        0.42, 0.2, 0.5, 0.02
    ).apply {
        dependency = { modeSetting.value in listOf("BHop", "LowHop", "Jump") }
    }

    private val autoJumpSetting = booleanSetting(
        "AutoJump", "自动跳跃",
        true
    )

    private val autoSprintSetting = booleanSetting(
        "AutoSprint", "自动疾跑",
        true
    )

    private val strafeBoostSetting = booleanSetting(
        "StrafeBoost", "Strafe加速",
        true
    ).apply {
        dependency = { modeSetting.value in listOf("BHop", "Strafe", "LowHop") }
    }

    private val randomizationSetting = booleanSetting(
        "Randomize", "启用随机化",
        true
    )

    private var wasOnGround: Boolean = false
    private var airTicks: Int = 0
    private var groundTicks: Int = 0
    private var moveSpeed: Double = 0.0
    private var stage: Int = 0
    private var yportStage: Int = 0
    private var lastDistance: Double = 0.0

    init {
        this.key = GLFW.GLFW_KEY_V
    }

    override fun onSettingChanged(setting: AbstractSetting<*>) {
        super.onSettingChanged(setting)
        if (setting == modeSetting) {
            resetState()
        }
    }

    override fun enable() {
        resetState()
        super.enable()
    }

    private fun resetState() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player

        airTicks = 0
        groundTicks = 0
        stage = 0
        yportStage = 0
        moveSpeed = getBaseMoveSpeed()
        lastDistance = 0.0
        wasOnGround = player?.isOnGround ?: false
    }

    override fun onTick() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (autoSprintSetting.value && isMoving() && !player.isSprinting && player.hungerManager.foodLevel > 6) {
            player.isSprinting = true
        }

        if (!isMoving()) {
            moveSpeed = getBaseMoveSpeed()
            stage = 0
            return
        }

        lastDistance = sqrt(
            (player.x - player.lastX) * (player.x - player.lastX) +
            (player.z - player.lastZ) * (player.z - player.lastZ)
        )

        if (player.isOnGround) {
            groundTicks++
            airTicks = 0
        } else {
            airTicks++
            groundTicks = 0
        }

        when (modeSetting.value) {
            "BHop" -> handleBHop()
            "Strafe" -> handleStrafe()
            "LowHop" -> handleLowHop()
            "YPort" -> handleYPort()
            "Legit" -> handleLegit()
            "OnGround" -> handleOnGround()
            "Vanilla" -> handleVanilla()
            "Jump" -> handleJump()
        }

        wasOnGround = player.isOnGround
        super.onTick()
    }

    private fun handleBHop() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (player.isOnGround) {
            stage = 2
        }

        when (stage) {
            0 -> {
                stage = 1
                moveSpeed = getBaseMoveSpeed() * 1.35 * speedBoostSetting.value
            }
            1 -> {
                if (player.isOnGround && isMoving()) {
                    stage = 2
                }
            }
            2 -> {
                if (player.isOnGround && isMoving()) {
                    val jumpHeight = jumpHeightSetting.value + getRandomOffset() * 0.02
                    player.velocity = player.velocity.add(0.0, jumpHeight, 0.0)
                    moveSpeed = getBaseMoveSpeed() * (1.8 + getRandomOffset() * 0.1) * speedBoostSetting.value
                }
                stage = 3
            }
            3 -> {
                moveSpeed = lastDistance - lastDistance / 159.0
                stage = 4
            }
            else -> {
                if (player.isOnGround) {
                    stage = 2
                }
                moveSpeed = lastDistance - lastDistance / 159.0
            }
        }

        val maxSpeed = getBaseMoveSpeed() * 1.9 * speedBoostSetting.value
        moveSpeed = moveSpeed.coerceIn(getBaseMoveSpeed(), maxSpeed)

        if (strafeBoostSetting.value) {
            applyStrafe(moveSpeed)
        }
    }

    private fun handleStrafe() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (player.isOnGround) {
            stage = 2
        }

        when (stage) {
            2 -> {
                if (player.isOnGround && isMoving()) {
                    player.jump()
                    moveSpeed = getBaseMoveSpeed() * (1.54 + getRandomOffset() * 0.05) * speedBoostSetting.value
                }
                stage = 3
            }
            3 -> {
                moveSpeed = lastDistance - lastDistance / 159.0
                stage = 4
            }
            else -> {
                if (player.isOnGround) {
                    stage = 2
                }
                moveSpeed = lastDistance - lastDistance / 159.0
            }
        }

        val maxSpeed = getBaseMoveSpeed() * 1.8 * speedBoostSetting.value
        moveSpeed = moveSpeed.coerceIn(getBaseMoveSpeed(), maxSpeed)

        if (strafeBoostSetting.value) {
            applyStrafe(moveSpeed)
        }
    }

    private fun handleLowHop() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (player.isOnGround && isMoving()) {
            val lowJump = (jumpHeightSetting.value * 0.7).coerceIn(0.2, 0.35) + getRandomOffset() * 0.01
            player.velocity = player.velocity.add(0.0, lowJump, 0.0)
            moveSpeed = getBaseMoveSpeed() * 1.4 * speedBoostSetting.value
        }

        if (!player.isOnGround && player.velocity.y < 0.1) {
            player.velocity = player.velocity.add(0.0, -0.03, 0.0)
        }

        moveSpeed = (lastDistance - lastDistance / 200.0).coerceIn(getBaseMoveSpeed(), getBaseMoveSpeed() * 1.6 * speedBoostSetting.value)

        if (strafeBoostSetting.value) {
            applyStrafe(moveSpeed)
        }
    }

    private fun handleYPort() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (player.isOnGround && isMoving()) {
            yportStage++

            when (yportStage % 4) {
                0 -> player.velocity = player.velocity.add(0.0, 0.1 + getRandomOffset() * 0.02, 0.0)
                1 -> {}
                2 -> player.velocity = player.velocity.add(0.0, 0.05 + getRandomOffset() * 0.01, 0.0)
                3 -> player.velocity = player.velocity.add(0.0, -0.1, 0.0)
            }

            applyStrafe(getBaseMoveSpeed() * 1.3 * speedBoostSetting.value)
        } else if (!player.isOnGround) {
            if (player.velocity.y < 0) {
                player.velocity = player.velocity.add(0.0, -0.05, 0.0)
            }
        }
    }

    private fun handleLegit() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (autoJumpSetting.value && player.isOnGround && isMoving() && groundTicks >= 1) {
            player.jump()
        }

        if (!player.isOnGround && airTicks > 0) {
            optimizeAirMovement()
        }
    }

    private fun handleOnGround() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (player.isOnGround && isMoving()) {
            if (groundTicks % getRandomInterval(4, 6) == 0) {
                val microJump = 0.08 + getRandomOffset() * 0.02
                player.velocity = player.velocity.add(0.0, microJump, 0.0)
            }

            val speed = getBaseMoveSpeed() * 1.15 * speedBoostSetting.value
            applyStrafe(speed)
        }
    }

    private fun handleVanilla() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (isMoving()) {
            val boost = 0.008 * speedBoostSetting.value + getRandomOffset() * 0.002

            val yaw = Math.toRadians(getMovementYaw().toDouble())
            player.velocity = player.velocity.add(
                -sin(yaw) * boost,
                0.0,
                cos(yaw) * boost
            )

            val horizontal = sqrt(player.velocity.x * player.velocity.x + player.velocity.z * player.velocity.z)
            val maxSpeed = getBaseMoveSpeed() * 1.1

            if (horizontal > maxSpeed) {
                val factor = maxSpeed / horizontal
                player.velocity = player.velocity.multiply(factor, 1.0, factor)
            }
        }
    }

    private fun handleJump() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (player.isOnGround && isMoving()) {
            val jumpHeight = jumpHeightSetting.value + getRandomOffset() * 0.02
            player.velocity = player.velocity.add(0.0, jumpHeight, 0.0)

            val yaw = Math.toRadians(getMovementYaw().toDouble())
            val boost = 0.02 * speedBoostSetting.value
            player.velocity = player.velocity.add(
                -sin(yaw) * boost,
                0.0,
                cos(yaw) * boost
            )
        }

        if (!player.isOnGround) {
            optimizeAirMovement()
        }
    }

    private fun applyStrafe(speed: Double) {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (!isMoving()) return

        val yaw = getMovementYaw()
        val yawRad = Math.toRadians(yaw.toDouble())

        val motionX = -sin(yawRad) * speed
        val motionZ = cos(yawRad) * speed

        player.velocity = player.velocity.multiply(0.0, 1.0, 0.0).add(motionX, 0.0, motionZ)
    }

    private fun optimizeAirMovement() {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return

        if (!isMoving()) return

        val moveYaw = getMovementYaw()
        val yawRad = Math.toRadians(moveYaw.toDouble())

        val boostFactor = speedBoostSetting.value * 0.01
        val acceleration = 0.02 * (1.0 + boostFactor) + getRandomOffset() * 0.001

        val correctionX = -sin(yawRad) * acceleration
        val correctionZ = cos(yawRad) * acceleration

        player.velocity = player.velocity.add(correctionX, 0.0, correctionZ)

        val horizontal = sqrt(player.velocity.x * player.velocity.x + player.velocity.z * player.velocity.z)
        val maxSpeed = getMaxAllowedSpeed()

        if (horizontal > maxSpeed) {
            val factor = maxSpeed / horizontal
            player.velocity = player.velocity.multiply(factor, 1.0, factor)
        }
    }

    private fun getBaseMoveSpeed(): Double {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return 0.2873

        var speed = 0.2873

        player.getStatusEffect(StatusEffects.SPEED)?.let { effect ->
            speed *= 1.0 + 0.2 * (effect.amplifier + 1)
        }

        player.getStatusEffect(StatusEffects.SLOWNESS)?.let { effect ->
            speed *= 1.0 - 0.15 * (effect.amplifier + 1)
        }

        return speed
    }

    private fun getMaxAllowedSpeed(): Double {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return 0.28

        var maxSpeed = 0.28

        player.getStatusEffect(StatusEffects.SPEED)?.let { effect ->
            maxSpeed *= 1.0 + 0.2 * (effect.amplifier + 1)
        }

        player.getStatusEffect(StatusEffects.SLOWNESS)?.let { effect ->
            maxSpeed *= 1.0 - 0.15 * (effect.amplifier + 1)
        }

        player.getStatusEffect(StatusEffects.DOLPHINS_GRACE)?.let {
            maxSpeed *= 1.3
        }

        return maxSpeed * 1.1
    }

    private fun getMovementYaw(): Float {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return 0f

        var yaw = player.yaw
        val forward = player.input.forwardMovement
        val strafe = player.input.sidewaysMovement

        if (forward != 0f) {
            if (strafe > 0) {
                yaw -= if (forward > 0) 45f else -45f
            } else if (strafe < 0) {
                yaw += if (forward > 0) 45f else -45f
            }

            if (forward < 0) {
                yaw += 180f
            }
        } else if (strafe != 0f) {
            yaw += if (strafe > 0) -90f else 90f
        }

        return yaw
    }

    private fun isMoving(): Boolean {
        val mc = MinecraftClient.getInstance()
        val player = mc.player ?: return false
        return player.input.forwardMovement != 0f || player.input.sidewaysMovement != 0f
    }

    private fun getRandomOffset(): Double {
        if (!randomizationSetting.value) return 0.0
        return (Random.nextDouble() * 2 - 1) * 0.01
    }

    private fun getRandomInterval(min: Int, max: Int): Int {
        if (!randomizationSetting.value) return min
        return Random.nextInt(min, max + 1)
    }

    override fun disable() {
        resetState()
        super.disable()
    }
}
