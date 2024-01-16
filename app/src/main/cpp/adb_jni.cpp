//
// Created by Rohit Verma on 16-01-2024.
//

#include <jni.h>
#include <string>
#include <sys/stat.h>

#include "adb.h"
#include "auth.h"
#include "logging.h"

using namespace adb;

static jboolean Adb_generateKey(JNIEnv *env, jclass obj, jstring java_file) {
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

static jbyteArray Adb_getPublicKey(JNIEnv *env, jclass obj, jstring java_file) {
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

static jbyteArray
Adb_signToken(JNIEnv *env, jclass obj, jstring java_file, jbyteArray java_token) {
    const char *temp_file = env->GetStringUTFChars(java_file, nullptr);
    std::string file = std::string(temp_file);

    size_t token_size = env->GetArrayLength(java_token);
    char token[token_size];
    env->GetByteArrayRegion(java_token, 0, token_size, reinterpret_cast<jbyte *>(token));

    std::string signed_token = auth::SignToken(file, token, token_size);

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