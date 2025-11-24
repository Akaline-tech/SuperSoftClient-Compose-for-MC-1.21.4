package com.xiamo.gui.musicPlayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xiamo.gui.ComposeScreen
import com.xiamo.gui.clickGui.ClickGuiScreen
import com.xiamo.module.ModuleManager
import com.xiamo.module.modules.render.ClickGui.instance
import com.xiamo.utils.misc.MediaPlayer
import kotlinx.coroutines.delay
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import java.lang.reflect.Constructor

class MusicPlayerScreen(var parentScreen : Screen? = null) : ComposeScreen(Text.of("MusicPlayer")) {

    @Composable
    override fun renderCompose() {
        LaunchedEffect(Unit) {
            isVisible = true
        }
        LaunchedEffect(isVisible){
            if (!isVisible){
                delay(300)
                MinecraftClient.getInstance().setScreen(null)




            }
        }

        AnimatedVisibility(
            isVisible,
            enter = expandIn(),
            exit = shrinkOut(tween(durationMillis = 300))
        ) {
            Box(modifier = Modifier.Companion.fillMaxSize(), contentAlignment = Alignment.Companion.Center) {

                Button(onClick = {
                    println("6")
                    MediaPlayer.playerSound("1.mp3")
                }) {
                    Text("der")
                }


            }
        }



        super.renderCompose()
    }
}