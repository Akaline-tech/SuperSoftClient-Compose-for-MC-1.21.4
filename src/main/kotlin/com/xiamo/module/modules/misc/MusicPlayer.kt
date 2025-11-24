package com.xiamo.module.modules.misc

import com.xiamo.gui.ComposeScreen
import com.xiamo.gui.musicPlayer.MusicPlayerScreen
import com.xiamo.module.Category
import com.xiamo.module.Module
import net.minecraft.client.MinecraftClient

object MusicPlayer : Module("MusicPlayer","音乐播放器", Category.Misc) {

    var instance : ComposeScreen? = null



    override fun disable() {
        instance?.isVisible = false
        super.disable()
    }

    override fun enable() {
        val currentScreen = MinecraftClient.getInstance().currentScreen
        if (currentScreen is ComposeScreen){
            currentScreen.isVisible = false
        }
        instance = MusicPlayerScreen(currentScreen)
        MinecraftClient.getInstance().setScreen(instance)
        super.enable()
    }
}