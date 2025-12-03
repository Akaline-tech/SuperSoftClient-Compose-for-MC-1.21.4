package com.xiamo.gui.clickGui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateSizeAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xiamo.gui.ComposeScreen
import com.xiamo.module.Category
import com.xiamo.module.ModuleManager
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import java.util.concurrent.CopyOnWriteArrayList

class ClickGuiScreen(val parentScreen: Screen? = null) : ComposeScreen(Text.of("ClickGui")) {
    val categories = CopyOnWriteArrayList<ClickGuiWindow>()

    override fun removed() {
        ModuleManager.modules.find { it.name == "ClickGui" }?.enabled = false
        super.removed()
    }

    @Composable
    override fun renderCompose() {
        val width = 100f
        val height = 20f
        val start = 20f

        LaunchedEffect(Unit) {
            isVisible = true
        }

        val scale by animateSizeAsState(
            if (isVisible) Size(1f, 1f) else Size(0f, 0f),
            tween(durationMillis = 300),
            finishedListener = {
                if (!isVisible) {
                    MinecraftClient.getInstance().setScreen(parentScreen)
                }
            }
        )

        val blurAlpha by animateFloatAsState(
            targetValue = if (isVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 400)
        )

        if (categories.count() == 0) {
            var x = start
            val y = 200
            Category.entries.forEach { category ->
                categories.add(ClickGuiWindow(x.toInt(), y, category, width, height))
                x += width * MinecraftClient.getInstance().window.scaleFactor.toInt() + 20 * MinecraftClient.getInstance().window.scaleFactor.toInt()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(blurAlpha * 0.6f)
                    .blur(20.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0, 0, 0, 180),
                                Color(0, 0, 0, 220)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(blurAlpha * 0.4f)
                    .background(Color(0, 0, 0, 100))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(animateFloatAsState(if (isVisible) 1f else 0f).value)
                .safeContentPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .animateContentSize()
                    .scale(scale.width, scale.height)
            ) {
                categories.forEach { it.renderCompose() }
            }
        }
    }

    override fun shouldPause(): Boolean {
        return false
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        categories.forEach { it.onDragged(mouseX.toInt(), mouseY.toInt()) }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (categories.any { it.isHover }) {
            categories.last { it.isHover }.onClicked(mouseX.toInt(), mouseY.toInt())
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        categories.forEach { it.onMouseReleased() }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun close() {
        ModuleManager.modules.find { it.name == "ClickGui" }?.enabled = false
        super.close()
    }
}
