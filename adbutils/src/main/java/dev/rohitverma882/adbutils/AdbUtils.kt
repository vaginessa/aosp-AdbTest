package dev.rohitverma882.adbutils

import android.content.Context

import java.io.File
import java.lang.ref.WeakReference

object AdbUtils {
    private lateinit var applicationContext: WeakReference<Context>
    private lateinit var adbKey: File

    init {
        System.loadLibrary("adb_utils")
    }

    @JvmStatic
    private external fun nativeGenerateKey(file: String): Boolean

    @JvmStatic
    private external fun nativeGetPublicKey(file: String): ByteArray

    @JvmStatic
    private external fun nativeGetPrivateKey(file: String): ByteArray

    @JvmStatic
    private external fun nativeGenerateCertificate(file: String): ByteArray

    @JvmStatic
    private external fun nativeSign(file: String, maxPayload: Int, token: ByteArray): ByteArray

    @JvmStatic
    fun init(context: Context) {
        applicationContext = WeakReference(context.applicationContext)

        if (applicationContext.get() != null) {
            adbKey = File(applicationContext.get()!!.filesDir, "adbkey")
        } else {
            throw IllegalStateException("Failed to init")
        }

        if (!nativeGenerateKey(adbKey.absolutePath)) {
            throw IllegalStateException("Failed to generate adb keys")
        }
    }

    @JvmStatic
    fun getPublicKey(): ByteArray = nativeGetPublicKey(adbKey.absolutePath)

    @JvmStatic
    fun getPrivateKey(): ByteArray = nativeGetPrivateKey(adbKey.absolutePath)

    @JvmStatic
    fun generateCertificate(): ByteArray = nativeGenerateCertificate(adbKey.absolutePath)

    @JvmStatic
    fun sign(maxPayload: Int, token: ByteArray): ByteArray =
        nativeSign(adbKey.absolutePath, maxPayload, token.copyOf())

    @JvmStatic
    fun sign(token: ByteArray): ByteArray = nativeSign(adbKey.absolutePath, -1, token.copyOf())
}