//
// Created by Rohit Verma on 16-01-2024.
//

#include <jni.h>
#include <string>
#include <sys/stat.h>

#include "logging.h"
#include "auth.h"
#include "utils.h"

using namespace adb;

static jboolean AdbUtils_GenerateKey(JNIEnv *env, jclass obj, jstring java_file) {
    const char *temp_file = env->GetStringUTFChars(java_file, nullptr);
    std::string file = std::string(temp_file);

    jboolean ret = JNI_TRUE;

    struct stat buf;
    if (stat(file.c_str(), &buf) == -1) {
        LOGI("Auth key '%s' does not exist...", file.c_str());
        if (!auth::GenerateKey(file)) {
            LOGE("Failed to generate new key");
            ret = JNI_FALSE;
        }
    }

    env->ReleaseStringUTFChars(java_file, temp_file);
    return ret;
}

static jbyteArray AdbUtils_GetPublicKey(JNIEnv *env, jclass obj, jstring java_file) {
    const char *temp_file = env->GetStringUTFChars(java_file, nullptr);
    std::string file = std::string(temp_file);
    std::string key = auth::GetPublicKey(file);
    env->ReleaseStringUTFChars(java_file, temp_file);

    jsize data_size = key.size() + 1;
    jbyteArray data = env->NewByteArray(data_size);
    env->SetByteArrayRegion(data, 0, data_size,
                            reinterpret_cast<const jbyte *>(key.c_str()));
    return data;
}

static jbyteArray AdbUtils_GetPrivateKey(JNIEnv *env, jclass obj, jstring java_file) {
    const char *temp_file = env->GetStringUTFChars(java_file, nullptr);
    std::string file = std::string(temp_file);
    auto private_key = auth::GetPrivateKey(file);
    env->ReleaseStringUTFChars(java_file, temp_file);

    std::string key = crypto::ToPEMString(private_key.get());

    jsize data_size = key.size();
    jbyteArray data = env->NewByteArray(data_size);
    env->SetByteArrayRegion(data, 0, data_size,
                            reinterpret_cast<const jbyte *>(key.data()));
    return data;
}

static jbyteArray AdbUtils_GenerateCertificate(JNIEnv *env, jclass obj, jstring java_file) {
    const char *temp_file = env->GetStringUTFChars(java_file, nullptr);
    std::string file = std::string(temp_file);
    auto private_key = auth::GetPrivateKey(file);
    env->ReleaseStringUTFChars(java_file, temp_file);

    auto x509_certificate = crypto::GenerateX509Certificate(private_key.get());

    std::string certificate;
    if (!x509_certificate) {
        LOGE("Unable to create X509 certificate");
        certificate = "";
    } else {
        certificate = crypto::X509ToPEMString(x509_certificate.get());
    }

    jsize data_size = certificate.size();
    jbyteArray data = env->NewByteArray(data_size);
    env->SetByteArrayRegion(data, 0, data_size,
                            reinterpret_cast<const jbyte *>(certificate.data()));
    return data;
}

static jbyteArray
AdbUtils_Sign(JNIEnv *env, jclass obj, jstring java_file, jint java_max_payload,
              jbyteArray java_token) {
    const char *temp_file = env->GetStringUTFChars(java_file, nullptr);
    std::string file = std::string(temp_file);

    size_t token_size = env->GetArrayLength(java_token);
    char token[token_size];
    env->GetByteArrayRegion(java_token, 0, token_size, reinterpret_cast<jbyte *>(token));

    size_t max_payload = java_max_payload > 0 ? static_cast<size_t>(java_max_payload) : MAX_PAYLOAD;
    std::string signed_token = auth::Sign(file, max_payload, token, token_size);

    env->ReleaseStringUTFChars(java_file, temp_file);

    jsize data_size = signed_token.size();
    jbyteArray data = env->NewByteArray(data_size);
    env->SetByteArrayRegion(data, 0, data_size,
                            reinterpret_cast<const jbyte *>(signed_token.data()));
    return data;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass c = env->FindClass("dev/rohitverma882/adbtest/adb/AdbUtils");
    if (c == nullptr) return JNI_ERR;

    static const JNINativeMethod methods[] = {
            {"nativeGenerateKey",         "(Ljava/lang/String;)Z",     reinterpret_cast<void *>(AdbUtils_GenerateKey)},
            {"nativeGetPublicKey",        "(Ljava/lang/String;)[B",    reinterpret_cast<void *>(AdbUtils_GetPublicKey)},
            {"nativeGetPrivateKey",       "(Ljava/lang/String;)[B",    reinterpret_cast<void *>(AdbUtils_GetPrivateKey)},
            {"nativeGenerateCertificate", "(Ljava/lang/String;)[B",    reinterpret_cast<void *>(AdbUtils_GenerateCertificate)},
            {"nativeSign",                "(Ljava/lang/String;I[B)[B", reinterpret_cast<void *>(AdbUtils_Sign)},
    };

    int rc = env->RegisterNatives(c, methods, sizeof(methods) / sizeof(JNINativeMethod));
    if (rc != JNI_OK) return rc;
    return JNI_VERSION_1_6;
}