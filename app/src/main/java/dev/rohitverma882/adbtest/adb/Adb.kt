package dev.rohitverma882.adbtest.adb

import java.io.File

class Adb private constructor(keyFile: File) {
    private var keyPath = keyFile.absolutePath

    fun getPublicKey(): ByteArray {
        return getPublicKey(keyPath)
    }

    fun signToken(token: ByteArray): ByteArray {
        return signToken(keyPath, token)
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

        @JvmStatic
        fun init(filesDir: File): Adb {
            val keyFile = File(filesDir, "adbkey")
            if (!keyFile.exists()) {
                generateKey(keyFile.absolutePath)
            }
            return Adb(keyFile)
        }
    }
}