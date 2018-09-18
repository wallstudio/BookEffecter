package wallstudio.work.kamishiba;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.Calendar;

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

    public static void imageDump(Context context, Image src, Bitmap dest) {
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("ImageDump\n\n");
        headerBuilder.append("BMP_W=" + dest.getWidth() + "\n");
        headerBuilder.append("BMP_H=" + dest.getHeight() + "\n");
        headerBuilder.append("IMG_W" + src.getWidth() + "\n");
        headerBuilder.append("IMG_H" + src.getHeight() + "\n");
        headerBuilder.append("IMG_PLANES_COUNT=" + src.getPlanes().length + "\n");
        headerBuilder.append("IMG_ROW_STRIDE_Y=" + src.getPlanes()[0].getRowStride() + "\n");
        headerBuilder.append("IMG_ROW_STRIDE_V=" + src.getPlanes()[1].getRowStride() + "\n");
        headerBuilder.append("IMG_ROW_STRIDE_U=" + src.getPlanes()[2].getRowStride() + "\n");
        headerBuilder.append("IMG_PX_STRIDE_Y=" + src.getPlanes()[0].getPixelStride() + "\n");
        headerBuilder.append("IMG_PX_STRIDE_V=" + src.getPlanes()[1].getPixelStride() + "\n");
        headerBuilder.append("IMG_PX_STRIDE_U=" + src.getPlanes()[2].getPixelStride() + "\n");
        try {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                Toast.makeText(context, "内臓ストレージがありません", Toast.LENGTH_LONG).show();
                return;
            }

            String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath() + "/kamishiba_dev/";
            if(!new File(dir).exists())
                new File(dir).mkdirs();

            String timeStamp = String.valueOf(System.currentTimeMillis());
            LoadUtil.saveString(headerBuilder.toString(), dir + "image_dump_header_" + timeStamp + ".txt");
            for(int i = 0; i < 3; i++) {
                byte[] byteArrayY = new byte[src.getPlanes()[i].getBuffer().remaining()];
                src.getPlanes()[i].getBuffer().get(byteArrayY);
                LoadUtil.saveArray(byteArrayY, dir + "image_dump_" + i + "_" + timeStamp + ".plane");
            }
        } catch (IOException e) {
            Toast.makeText(context, "ダンプが生成できません " + e.getCause(), Toast.LENGTH_LONG).show();
        }
    }
}
