package com.xiamo.utils.misc

import com.xiamo.utils.config.ConfigManager.mainDir
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import net.fabricmc.loader.impl.game.minecraft.applet.AppletMain
import net.minecraft.client.MinecraftClient

import java.io.File

object MediaPlayer {
    val cachePath = File(mainDir.path+"/songs")





    init {
        if (!cachePath.exists()) cachePath.mkdir()

    }

    fun playerSound(file : File){


    }






}