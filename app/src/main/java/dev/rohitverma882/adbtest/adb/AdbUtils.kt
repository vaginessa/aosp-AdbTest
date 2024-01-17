package dev.rohitverma882.adbtest.adb

import android.content.Context

import java.io.File

object AdbUtils {
    private lateinit var applicationContext: Context
    private lateinit var adbKey: File

    init {
        System.loadLibrary("adb_utils")
    }

    @JvmStatic
    private external fun generateKey(file: String): Boolean

    @JvmStatic
    private external fun getPublicKey(file: String): ByteArray

    @JvmStatic
    private external fun signToken(file: String, token: ByteArray): ByteArray

    @JvmStatic
    fun init(context: Context) {
        applicationContext = context.applicationContext
        adbKey = File(applicationContext.filesDir, "adbkey")

        if (!generateKey(adbKey.absolutePath)) {
            throw RuntimeException("Failed to generate adb keys")
        }
    }

    @JvmStatic
    fun getPublicKey(): ByteArray = getPublicKey(adbKey.absolutePath)

    @JvmStatic
    fun signToken(token: ByteArray): ByteArray = signToken(adbKey.absolutePath, token.copyOf())
}