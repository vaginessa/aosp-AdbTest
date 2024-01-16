package dev.rohitverma882.adbtest.adb

import dev.rohitverma882.adbtest.MyApp

import java.io.File

class Adb(app: MyApp) {
    private val context = app.applicationContext
    private val filesDir = context.filesDir

    private val authKey = File(filesDir, "adbkey")

    var isRequireSignature: Boolean = true

    fun generateKey(): Boolean {
        return generateKey(authKey.absolutePath)
    }

    fun getPublicKey(): ByteArray {
        return getPublicKey(authKey.absolutePath)
    }

    fun signToken(token: ByteArray): ByteArray {
        return signToken(authKey.absolutePath, token.copyOf())
    }

    companion object {
        init {
            System.loadLibrary("adb")
        }

        @JvmStatic
        private external fun generateKey(file: String): Boolean

        @JvmStatic
        private external fun getPublicKey(file: String): ByteArray

        @JvmStatic
        private external fun signToken(file: String, token: ByteArray): ByteArray
    }
}