#include <jni.h>
#include <string>
#include <android/bitmap.h>

extern "C" JNIEXPORT jstring

JNICALL
Java_wallstudio_work_kamishiba_Jni_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}extern "C"
JNIEXPORT void JNICALL
Java_wallstudio_work_kamishiba_Jni_yuvByteArrayToBmp(JNIEnv *env, jclass type,
                                                     jobject yBuffer,
                                                     jobject uBuffer,
                                                     jobject vBuffer,
                                                     jint width, jint height,
                                                     jobject bitmap) {

    uint8_t  *ySrc = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(yBuffer));
    uint8_t  *uSrc = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(uBuffer));
    uint8_t  *vSrc = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(vBuffer));
    void *bitmapPtr;
    AndroidBitmap_lockPixels(env, bitmap, &bitmapPtr);
    uint32_t *bitmapRow = reinterpret_cast<uint32_t *>(bitmapPtr);
    for (int i = 0; i < width * height; ++i) {
        float y = ySrc[i];
        int i_uv = (i / width) / 2 * (width / 2) + (i % width) / 2;
        float u = uSrc[i_uv];
        float v = vSrc[i_uv];

        double r = y + (1.4065 * (u - 128));
        double g = y - (0.3455 * (u - 128)) - (0.7169 * (v - 128));
        double b = y + (1.7790 * (v - 128));

        if (r < 0) r = 0;
        else if (r > 255) r = 255;
        if (g < 0) g = 0;
        else if (g > 255) g = 255;
        if (b < 0) b = 0;
        else if (b > 255) b = 255;

        uint32_t pixel = ((uint32_t)0xFFU << 24) + ((uint32_t)b << 16) + ((uint32_t)g << 8) + (uint32_t)r;
        bitmapRow[i] = pixel;
    }
    AndroidBitmap_unlockPixels(env, bitmap);

}