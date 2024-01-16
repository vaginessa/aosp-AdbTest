package dev.rohitverma882.adbtest

import android.app.Application

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val logFile = "${getExternalFilesDir(null)?.absolutePath ?: filesDir.absolutePath}/logcat.txt"
            Runtime.getRuntime().exec(arrayOf("logcat", "-f", logFile))
        } catch (_: Throwable) {

        }
    }
}
