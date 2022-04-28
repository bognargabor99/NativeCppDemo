#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_hu_bme_aut_android_nativecppdemo_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF("hello.c_str()");
}

extern "C" JNIEXPORT jstring JNICALL
Java_hu_bme_aut_android_nativecppdemo_MainActivity_otherMethod(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF("na mi van????");
}