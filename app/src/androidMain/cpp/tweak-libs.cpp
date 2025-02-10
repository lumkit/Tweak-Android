#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jlong JNICALL
Java_io_github_lumkit_tweak_common_TweakNative_getKernelPropLong(JNIEnv *env, jobject thiz, jstring path) {
    if (path == nullptr) {
        jclass npeCls = env->FindClass("java/lang/NullPointerException");
        if (npeCls != nullptr) { // 确保类被找到
            env->ThrowNew(npeCls, "Path string is null");
        }
        return 0;
    }

    const char* charData = env->GetStringUTFChars(path, nullptr);
    if (charData == nullptr) {
        return 0;
    }

    FILE *kernelProp = fopen(charData, "r");
    env->ReleaseStringUTFChars(path, charData);

    if (kernelProp == nullptr) {
        char errorMsg[256];
        snprintf(errorMsg, sizeof(errorMsg), "Failed to open file '%s': %s", charData, strerror(errno));

        jclass ioExceptionCls = env->FindClass("java/io/IOException");
        if (ioExceptionCls != nullptr) {
            env->ThrowNew(ioExceptionCls, errorMsg);
        }
        return 0;
    }

    long freq;
    int scanResult = fscanf(kernelProp, "%ld", &freq);
    if (scanResult != 1) {
        fclose(kernelProp);
        jclass ioExceptionCls = env->FindClass("java/io/IOException");
        if (ioExceptionCls != nullptr) {
            env->ThrowNew(ioExceptionCls, "Failed to read long value from file");
        }
        return 0;
    }

    fclose(kernelProp);
    return static_cast<jlong>(freq);
}