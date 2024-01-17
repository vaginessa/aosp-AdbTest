package dev.rohitverma882.adbtest

import android.app.Application

import dev.rohitverma882.adbtest.adb.AdbUtils

import java.io.File

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AdbUtils.init(this)

        Thread {
            try {
                val logFile =
                    "${getExternalFilesDir(null)?.absolutePath ?: filesDir.absolutePath}/logcat.txt"
                File(logFile).delete()
                val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-f", logFile))
                proc.waitFor()
            } catch (_: Throwable) {

            }
        }.start()
    }
}
