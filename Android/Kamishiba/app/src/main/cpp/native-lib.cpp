#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>

inline void  ThrowJavaException(JNIEnv *env, std::string message);
inline uint32_t Yuv2Rgb(const float y, const float u, const float v);
inline uint32_t Y2Rgb(const float y);

extern "C"
JNIEXPORT void JNICALL
Java_wallstudio_work_kamishiba_Jni_yuvByteArrayToBmp(JNIEnv *env, jclass type,
                                                     jobject bufferY, jobject bufferU, jobject  bufferV,
                                                     jint bufferYLength, jint bufferULength, jint bufferVLength,
                                                     jobject destBitmap,
                                                     jint bitmapWidth, jint bitmapHeight,
                                                     jint imageWidth, jint  imageHeight,
                                                     jint imagePlanesCount,
                                                     jint imageRowStrideY, jint imageRowStrideU, jint imageRowStrideV,
                                                     jint imagePixelStrideY, jint imagePixelStrideU, jint imagePixelStrideV) {

    try {

        if (imagePlanesCount != 3) {
            ThrowJavaException(env, "Plane count need 3. (now " + std::to_string(imagePlanesCount) +
                                    ")");
            return;
        }
        // 入力Planeの準備
        uint8_t *planeY = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(bufferY));
        uint8_t *planeU = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(bufferU));
        uint8_t *planeV = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(bufferV));
        // ref. https://developer.android.com/reference/android/graphics/ImageFormat.html#YUV_420_888
        if (!((imagePixelStrideY == 1 || imagePixelStrideY == 2 || imagePixelStrideY == 4) &&
            (imagePixelStrideU == 1 || imagePixelStrideU == 2 || imagePixelStrideU == 4) &&
            (imagePixelStrideV == 1 || imagePixelStrideV == 2 || imagePixelStrideV == 4) &&
            imagePixelStrideU == imagePixelStrideV)) {
            ThrowJavaException(env, "Pixel strides need 1,2,4. (now " +
                                    std::to_string(imagePixelStrideY) + "-" +
                                    std::to_string(imagePixelStrideU) + "-" +
                                    std::to_string(imagePixelStrideV) + ")");
            return;
        }
        if (imageRowStrideU != imageRowStrideV){
            ThrowJavaException(env, "Row stride U,V need same (now " +
                    std::to_string(imageRowStrideU) + "-" +
                    std::to_string(imageRowStrideV) + ")");
            return;
        }

        // 出力先の準備
        AndroidBitmapInfo info;
        if (AndroidBitmap_getInfo(env, destBitmap, &info) < 0) {
            ThrowJavaException(env, std::string("Failed AndroidBitmap_getInfo()"));
            return;
        }
        if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
            ThrowJavaException(env, "Bitmap format need RGBA. (now " + std::to_string(info.format) + ")");
            return;
        }
        int bitmapStride = info.stride;
        void *bitmapRawPtr;
        if (AndroidBitmap_lockPixels(env, destBitmap, &bitmapRawPtr) < 0) {
            ThrowJavaException(env, std::string("Failed AndroidBitmap_lockPixels()"));
            return;
        }
        uint32_t *destArray = reinterpret_cast<uint32_t *>(bitmapRawPtr);

        // 走査
        int i = 0, j = 0;
        for (int c = 0; c < bufferYLength; c++) {
            i = c % imageRowStrideY;
            j = c / imageRowStrideY;
            int idxY = i + j * imageRowStrideY;
            int idxU = (i / 2 * imagePixelStrideU) + (j / 2) * imageRowStrideU;
            int idxV = (i / 2 * imagePixelStrideV) + (j / 2) * imageRowStrideU;
            float y = idxY < bufferYLength ? planeY[idxY] : 0;
            float u = idxU < bufferULength ? planeU[idxU] : 0;
            float v = idxV < bufferVLength ? planeV[idxV] : 0;
            if(i < bitmapStride / sizeof(uint32_t) && j < bitmapHeight)
                destArray[i + j * bitmapStride / sizeof(uint32_t)] = Yuv2Rgb(y, u, v);
        }
        AndroidBitmap_unlockPixels(env, destBitmap);

    }catch (std::exception e){
        ThrowJavaException(env, std::string(e.what()));
    }
}

inline uint32_t Yuv2Rgb(const float y, const float u, const float v){
    double r = y + (1.4065 * (u - 128));
    double g = y - (0.3455 * (u - 128)) - (0.7169 * (v - 128));
    double b = y + (1.7790 * (v - 128));

    if (r < 0) r = 0;
    else if (r > 255) r = 255;
    if (g < 0) g = 0;
    else if (g > 255) g = 255;
    if (b < 0) b = 0;
    else if (b > 255) b = 255;

    return ((uint32_t) 0xFFU << 24) + ((uint32_t) r << 16) + ((uint32_t) g << 8) + (uint32_t) b;
}

inline uint32_t Y2Rgb(const float y){
    uint8_t g = (uint8_t) y;
    return ((uint32_t) 0xFFU << 24) + ((uint32_t) g << 16) + ((uint32_t) g << 8) + (uint32_t) g;
}

inline void  ThrowJavaException(JNIEnv *env, std::string message){
    jclass  exception = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(exception, message.c_str());
}