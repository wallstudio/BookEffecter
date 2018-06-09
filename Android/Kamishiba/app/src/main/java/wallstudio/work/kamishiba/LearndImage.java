package wallstudio.work.kamishiba;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.AKAZE;
import org.opencv.imgproc.Imgproc;

public class LearndImage{

    private static AKAZE sAkaze;

    public Mat image;
    public MatOfKeyPoint keyPoints;
    public Mat descriptors;

    public  LearndImage(Mat image){
        if(sAkaze == null) sAkaze = AKAZE.create();
        this.image = image;
        Mat gray = new Mat();
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
        // ref. https://jp.mathworks.com/help/vision/ug/local-feature-detection-and-extract// ref. 
        keyPoints = new MatOfKeyPoint();
        descriptors = new Mat();
        Mat mask = Mat.ones(image.size(), CvType.CV_8U);
        sAkaze.detectAndCompute(image, mask, keyPoints, descriptors);
        gray.release();
    }

    public void release(){
        image.release();
        keyPoints.release();
        descriptors.release();
    }

    // TODO: Serializer and Desirializer
}