package com.xiamo.utils

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.xiamo.SuperSoft
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit


object CoilInitializer {

    private var initialized = false

    fun init() {
        if (initialized) return

        try {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val cacheDir = File(System.getProperty("user.home"), ".supersoft/image_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val imageLoader = ImageLoader.Builder(coil3.PlatformContext.INSTANCE)
                .components {
                    add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
                }
                .memoryCache {
                    MemoryCache.Builder()
                        .maxSizeBytes(50 * 1024 * 1024)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir)
                        .maxSizeBytes(100 * 1024 * 1024)
                        .build()
                }
                .crossfade(true)
                .build()

            SingletonImageLoader.setSafe { imageLoader }
            initialized = true
            SuperSoft.logger.info("Coil初始化成功")
        } catch (e: Exception) {
            SuperSoft.logger.error("Coil初始化失败", e)
        }
    }
}
