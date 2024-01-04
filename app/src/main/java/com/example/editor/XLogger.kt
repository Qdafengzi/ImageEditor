package com.example.editor

import android.content.Context
import android.util.Log
import android.widget.Toast

object XLogger {
    var className: String? = null
    var methodName: String? = null
    var lineNumber = 0
    val isDebuggable: Boolean
        get() = true

    private fun createLog(log: String): String {
        return "${com.example.editor.XLogger.methodName}(${com.example.editor.XLogger.className}:${com.example.editor.XLogger.lineNumber})$log"
    }

    private fun getMethodNames(sElements: Array<StackTraceElement>) {
        com.example.editor.XLogger.className = sElements[1].fileName
        com.example.editor.XLogger.methodName = sElements[1].methodName
        com.example.editor.XLogger.lineNumber = sElements[1].lineNumber
    }

    fun e(message: String) {
        if (!com.example.editor.XLogger.isDebuggable) return

        // Throwable instance must be created before any methods
        com.example.editor.XLogger.getMethodNames(Throwable().stackTrace)
        Log.e(com.example.editor.XLogger.className, com.example.editor.XLogger.createLog(message))
    }

    fun i(message: String) {
        if (!com.example.editor.XLogger.isDebuggable) return
        com.example.editor.XLogger.getMethodNames(Throwable().stackTrace)
        Log.i(com.example.editor.XLogger.className, com.example.editor.XLogger.createLog(message))
    }

    @JvmStatic
    fun d(message: String) {
        if (!com.example.editor.XLogger.isDebuggable) return
        com.example.editor.XLogger.getMethodNames(Throwable().stackTrace)
        Log.d(com.example.editor.XLogger.className, com.example.editor.XLogger.createLog(message))
    }

    fun v(message: String) {
        if (!com.example.editor.XLogger.isDebuggable) return
        com.example.editor.XLogger.getMethodNames(Throwable().stackTrace)
        Log.v(com.example.editor.XLogger.className, com.example.editor.XLogger.createLog(message))
    }

    fun w(message: String) {
        if (!com.example.editor.XLogger.isDebuggable) return
        com.example.editor.XLogger.getMethodNames(Throwable().stackTrace)
        Log.w(com.example.editor.XLogger.className, com.example.editor.XLogger.createLog(message))
    }

    fun log(tag: String, content: String) {
        if (com.example.editor.XLogger.isDebuggable) {
            println("$tag:$content")
        }
    }

    fun toast(context: Context?, content: String?) {
        if (com.example.editor.XLogger.isDebuggable) {
            Toast.makeText(context, content, Toast.LENGTH_SHORT).show()
        }
    }

    fun longD(msg: String) {
        if (com.example.editor.XLogger.isDebuggable) {
            val maxLogSize = 3000
            for (i in 0..msg.length / maxLogSize) {
                val start = i * maxLogSize
                var end = (i + 1) * maxLogSize
                end = if (end > msg.length) msg.length else end
                Log.d(com.example.editor.XLogger.className, msg.substring(start, end))
            }
        }
    }

    fun longE(msg: String) {
        if (com.example.editor.XLogger.isDebuggable) {
            val maxLogSize = 3000
            for (i in 0..msg.length / maxLogSize) {
                val start = i * maxLogSize
                var end = (i + 1) * maxLogSize
                end = if (end > msg.length) msg.length else end
                Log.e(com.example.editor.XLogger.className, msg.substring(start, end))
            }
        }
    }
}