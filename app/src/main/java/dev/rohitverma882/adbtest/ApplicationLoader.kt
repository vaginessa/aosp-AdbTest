package dev.rohitverma882.adbtest

import android.app.Application

import dev.rohitverma882.adbutils.AdbUtils

class ApplicationLoader : Application() {
    override fun onCreate() {
        super.onCreate()
        AdbUtils.init(this)
    }
}
