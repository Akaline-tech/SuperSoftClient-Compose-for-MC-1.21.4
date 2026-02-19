package com.xiamo.utils.render

import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.joml.Vector3f
import org.joml.Vector4f

object RenderUtils {
    fun drawBox3D(matrixStack: MatrixStack, box: Box, r: Float = 0.5f, g: Float = 1.0f, b: Float = 1.0f, a: Float = 1.0f) {}
    fun drawFillBox3D(matrixStack: MatrixStack, box: Box, r: Float = 0.5f, g: Float = 1.0f, b: Float = 1.0f, a: Float = 1.0f) {}
    fun draw2DRect(matrixStack: MatrixStack, x1: Float, y1: Float, z1: Float, x2: Float, y2: Float, z2: Float, color: Int) {}
}
