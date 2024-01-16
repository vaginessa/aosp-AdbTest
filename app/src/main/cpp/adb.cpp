//
// Created by Rohit Verma on 15-01-2024.
//

#include "adb.h"

#include <jni.h>
#include <string>
#include <sys/stat.h>

#include "auth.h"
#include "logging.h"

using namespace adb;

namespace adb {

} // namespace adb

static jboolean Adb_generateKey(JNIEnv *env, jclass obj, jstring java_file) {
    const char *file = env->GetStringUTFChars(java_file, nullptr);

    jboolean ret = JNI_TRUE;

    struct stat buf;
    if (stat(file, &buf) == -1) {
        LOGI("Auth key '%s' does not exist...", file);
        std::string auth_file(file);
        if (!auth::GenerateKey(auth_file)) {
            LOGE("Failed to generate new key");
            ret = JNI_FALSE;
        }
    }

    env->ReleaseStringUTFChars(java_file, file);
    return ret;
}

static jbyteArray Adb_getPublicKey(JNIEnv *env, jclass obj, jstring java_file) {
    const char *file = env->GetStringUTFChars(java_file, nullptr);

    LOGI("Calling GetPublicKey()");
    std::string auth_file(file);
    std::string key = auth::GetPublicKey(auth_file);

    env->ReleaseStringUTFChars(java_file, file);

    if (key.empty()) {
        LOGE("Failed to get auth public key");
        return nullptr;
    }

    if (key.size() >= MAX_PAYLOAD_V1) {
        LOGE("Auth public key too large (%zu B)", key.size());
        return nullptr;
    }

    jbyteArray data = env->NewByteArray(key.size());
    env->SetByteArrayRegion(data, 0, key.size(),
                            reinterpret_cast<const jbyte *>(key.data()));
    return data;
}

static jbyteArray
Adb_signToken(JNIEnv *env, jclass obj, jstring java_file, jbyteArray java_token) {
    const char *file = env->GetStringUTFChars(java_file, nullptr);

    std::string auth_file(file);

    int token_size = env->GetArrayLength(java_token);
    char token[token_size];
    env->GetByteArrayRegion(java_token, 0, token_size, reinterpret_cast<jbyte *>(token));

    std::string signed_token = auth::SignToken(auth_file, token, token_size);

    env->ReleaseStringUTFChars(java_file, file);

    jbyteArray data = env->NewByteArray(signed_token.size());
    env->SetByteArrayRegion(data, 0, signed_token.size(),
                            reinterpret_cast<const jbyte *>(signed_token.data()));
    return data;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    jclass c = env->FindClass("dev/rohitverma882/adbtest/adb/Adb");
    if (c == nullptr) return JNI_ERR;

    static const JNINativeMethod methods[] = {
            {"generateKey",  "(Ljava/lang/String;)Z",    reinterpret_cast<void *>(Adb_generateKey)},
            {"getPublicKey", "(Ljava/lang/String;)[B",   reinterpret_cast<void *>(Adb_getPublicKey)},
            {"signToken",    "(Ljava/lang/String;[B)[B", reinterpret_cast<void *>(Adb_signToken)},
    };

    int rc = env->RegisterNatives(c, methods, sizeof(methods) / sizeof(JNINativeMethod));
    if (rc != JNI_OK) return rc;
    return JNI_VERSION_1_6;
}