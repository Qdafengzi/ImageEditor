package com.example.editor

import android.app.Application
import android.content.Context

class App : Application() {

    companion object{
        @JvmStatic
        lateinit  var CONTEXT: Context
    }

    override fun onCreate() {
        super.onCreate()
        com.example.editor.App.Companion.CONTEXT = applicationContext
    }
}