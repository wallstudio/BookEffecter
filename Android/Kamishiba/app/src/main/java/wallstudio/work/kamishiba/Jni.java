package wallstudio.work.kamishiba;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Environment;
import android.widget.Toast;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.Calendar;

public class Jni {

    static {
        System.loadLibrary("native-lib");
    }

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
        Image.Plane planeU = src.getPlanes()[1];
        Image.Plane planeV = src.getPlanes()[2];

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

    public static String imageDump(Context context, Image src, Bitmap dest) {
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
                return "";
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

            return dir;
        } catch (IOException e) {
            new AlertDialog.Builder(context)
                    .setTitle("ダンプが生成できません")
                    .setMessage(e.getCause() + "\n" + e.getMessage() +"\n" +e.toString())
                    .setPositiveButton("OK", null)
                    .show();
            return "";
        }
    }
    
}
