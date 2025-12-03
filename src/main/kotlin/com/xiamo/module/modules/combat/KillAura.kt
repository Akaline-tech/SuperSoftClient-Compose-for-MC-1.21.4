package com.xiamo.module.modules.combat

import com.xiamo.module.Category
import com.xiamo.module.Module
import com.xiamo.utils.rotation.Rotation
import com.xiamo.utils.rotation.RotationManager
import com.xiamo.utils.rotation.RotationManager.isActive
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.LivingEntity
import org.lwjgl.glfw.GLFW
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.atan2
import kotlin.math.sqrt


object KillAura : Module("Kill Aura", "Kill Aura", Category.Combat) {
    init {
        this.key = GLFW.GLFW_KEY_G
    }

    override fun onTick() {
        val entities = MinecraftClient.getInstance().world?.entities
        val target = CopyOnWriteArrayList<LivingEntity>()
        if (entities != null) {
            entities.filter {
                it.isAlive && it.isAttackable && it != MinecraftClient.getInstance().player
                        && it.squaredDistanceTo(MinecraftClient.getInstance().player) < 50
            }.forEach {
                target.add(it as LivingEntity)
            }
        }

        if (target.isNotEmpty()) {
            val attack_target = target.sortedBy { it.distanceTo(MinecraftClient.getInstance().player) }[0]
            RotationManager.isActive.value = true
            rotate(attack_target)
        }else {
            RotationManager.targetRotation = null
            RotationManager.isActive.value = false
        }



        super.onTick()
    }

    override fun disable() {
        RotationManager.targetRotation = null
        RotationManager.isActive.value = false
        super.disable()
    }


    fun rotate(entity: LivingEntity) {
        val player = MinecraftClient.getInstance().player
        if (player != null){
            val diffX = entity.eyePos.x - player.eyePos.x
            val diffY = entity.eyePos.y - player.eyePos.y
            val diffZ = entity.eyePos.z - player.eyePos.z

            val dist = sqrt(diffX * diffX + diffZ * diffZ)

            val yaw = (Math.toDegrees(atan2(diffZ, diffX)) - 90.0).toFloat()
            val pitch = (-Math.toDegrees(atan2(diffY, dist))).toFloat()
            RotationManager.targetRotation = Rotation(yaw, pitch)

        }
    }



}