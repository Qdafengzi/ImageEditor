package com.example.mycamerax

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class App : Application(), CameraXConfig.Provider {

    companion object{
        @JvmStatic
        lateinit  var CONTEXT: Context
    }

    override fun onCreate() {
        super.onCreate()
        CONTEXT = applicationContext
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.INFO).build()
    }
}