package wallstudio.work.cameraprocessing;

import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.AKAZE;

public class LearndImage{

    private static AKAZE akaze;

    public  String imagePath = "";
    public  Mat image;

    public MatOfKeyPoint keyPoints;
    public Mat descriptors;

    public  LearndImage(String path){
        imagePath = path;
        try{
            Utils.bitmapToMat(BitmapFactory.decodeFile(path), image, false);
        }catch (Exception e){
            Log.e("ERROR", "FILE read error");
        }
        if(akaze == null) akaze = AKAZE.create();
        keyPoints = new MatOfKeyPoint();
        descriptors = new Mat();
        akaze.detectAndCompute(image, null, keyPoints, descriptors);
    }

    public  LearndImage(Mat image){
        if(akaze == null) akaze = AKAZE.create();
        this.image = image;
        keyPoints = new MatOfKeyPoint();
        descriptors = new Mat();
        akaze.detectAndCompute(image, null, keyPoints, descriptors);
    }
}