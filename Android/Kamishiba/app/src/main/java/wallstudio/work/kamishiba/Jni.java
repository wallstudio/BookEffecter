package wallstudio.work.kamishiba;

import android.graphics.Bitmap;

import java.nio.ByteBuffer;

public class Jni {

    static {
        System.loadLibrary("native-lib");
    }

    public static native String stringFromJNI();

    public static native void yuvByteArrayToBmp(ByteBuffer yBuffer, ByteBuffer uBuffer, ByteBuffer vBuffer, int width, int height, Bitmap bitmap);
}
