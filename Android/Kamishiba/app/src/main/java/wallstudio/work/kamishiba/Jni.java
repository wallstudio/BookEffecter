package wallstudio.work.kamishiba;

import android.graphics.Bitmap;
import android.media.Image;

import java.nio.ByteBuffer;

public class Jni {

    static {
        System.loadLibrary("native-lib");
    }

    public static native String stringFromJNI();

    public static native void yuvByteArrayToBmp(ByteBuffer yBuffer, ByteBuffer uBuffer, ByteBuffer vBuffer, int width, int height, Bitmap bitmap);

    public static void image2Bitmap(Image src, Bitmap dest, boolean isImageRelease){
        // YUV -> ARGB
        Image.Plane Y_plane = src.getPlanes()[0];
        int Y_rowStride = Y_plane.getRowStride();
        Image.Plane U_plane = src.getPlanes()[2];
        Image.Plane V_plane = src.getPlanes()[1];
        int imageHeight = src.getHeight();

        // Create original bitmap
        yuvByteArrayToBmp(Y_plane.getBuffer(), U_plane.getBuffer(), V_plane.getBuffer(), dest.getWidth(), dest.getHeight(), dest);

        if(isImageRelease)
            src.close();
    }
}
