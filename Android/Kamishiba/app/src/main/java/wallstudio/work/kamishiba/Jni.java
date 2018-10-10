package wallstudio.work.kamishiba;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import java.nio.ByteBuffer;

public class Jni {

    static {
        System.loadLibrary("native-lib");
    }

    public static native String stringFromJNI();

    public static native void yuvByteArrayToBmp(ByteBuffer bufferY, ByteBuffer bufferU, ByteBuffer  bufferV,
                                                int bufferYLength, int bufferULength, int bufferVLength,
                                                Bitmap destBitmap,
                                                int bitmapWidth, int bitmapHeight,
                                                int imageWidth, int  imageHeight,
                                                int imagePlanesCount,
                                                int imageRowStrideY, int imageRowStrideU, int imageRowStrideV,
                                                int imagePixelStrideY, int imagePixelStrideU, int imagePixelStrideV);

    static int s_planeYLength = -1;
    static int s_planeULength = -1;
    static int s_planeVLength = -1;
    public static void image2Bitmap(Image src, Bitmap dest, boolean isImageRelease){
        // YUV -> ARGB
        Image.Plane planeY = src.getPlanes()[0];
        Image.Plane planeU = src.getPlanes()[2];
        Image.Plane planeV = src.getPlanes()[1];

        if(s_planeYLength < 0 || s_planeULength < 0 || s_planeVLength < 0){
            s_planeYLength = planeY.getBuffer().limit();
            s_planeULength = planeU.getBuffer().limit();
            s_planeVLength = planeV.getBuffer().limit();
        }
        Log.d("YUV2RGB", getImage2BitmapInfo(src, dest));

        // Create original bitmap
        yuvByteArrayToBmp(
                planeY.getBuffer(), planeU.getBuffer(), planeV.getBuffer(),
                s_planeYLength, s_planeULength, s_planeVLength,
                dest,
                dest.getWidth(), dest.getHeight(),
                src.getWidth(), src.getHeight(),
                src.getPlanes().length,
                planeY.getRowStride(), planeU.getRowStride(), planeV.getRowStride(),
                planeY.getPixelStride(), planeU.getPixelStride(), planeV.getPixelStride());

        if(isImageRelease)
            src.close();
    }

    public static String getImage2BitmapInfo(Image src, Bitmap dest){
        Image.Plane planeY = src.getPlanes()[0];
        Image.Plane planeU = src.getPlanes()[2];
        Image.Plane planeV = src.getPlanes()[1];

        if(s_planeYLength < 0 || s_planeULength < 0 || s_planeVLength < 0){
            s_planeYLength = planeY.getBuffer().limit();
            s_planeULength = planeU.getBuffer().limit();
            s_planeVLength = planeV.getBuffer().limit();
        }
        return 
        String.format("PL:%d,%d,%d;\nSD:%d,%d;\nSS:%d,%d;\nPC:%d;\nRS:%d,%d,%d;\nPS%d,%d,%d;",
                s_planeYLength, s_planeULength, s_planeVLength,
                dest.getWidth(), dest.getHeight(),
                src.getWidth(), src.getHeight(),
                src.getPlanes().length,
                planeY.getRowStride(), planeU.getRowStride(), planeV.getRowStride(),
                planeY.getPixelStride(), planeU.getPixelStride(), planeV.getPixelStride());
    }
}
