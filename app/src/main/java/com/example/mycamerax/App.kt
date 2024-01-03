package com.example.mycamerax

import android.app.Application
import android.content.Context

class App : Application() {

    companion object{
        @JvmStatic
        lateinit  var CONTEXT: Context
    }

    override fun onCreate() {
        super.onCreate()
        CONTEXT = applicationContext
    }
}