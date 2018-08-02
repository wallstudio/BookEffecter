package wallstudio.work.kamishiba;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.AKAZE;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import io.github.jdiemke.triangulation.Triangle2D;

public class FeaturedImage extends Mat{

    private static AKAZE sAkaze;
    public Set<Triangulator.Edge> mesh;
    public List<Triangle2D> mesh2;
    public List<KeyPoint> keyPoints2;

    {
        if(sAkaze == null) sAkaze = AKAZE.create();
    }

    public MatOfKeyPoint keyPoints = new MatOfKeyPoint();
    public Mat descriptors = new Mat();

    public FeaturedImage(Mat image){
        super();
        Imgproc.cvtColor(image, this, Imgproc.COLOR_BGR2GRAY);
        Mat mask = Mat.ones(size(), CvType.CV_8U);
        sAkaze.detectAndCompute(this, mask, keyPoints, descriptors);
    }

    public FeaturedImage(Bitmap image){
        super();
        Utils.bitmapToMat(image, this);
        Imgproc.cvtColor(this, this, Imgproc.COLOR_BGR2GRAY);
        Mat mask = Mat.ones(size(), CvType.CV_8U);
        sAkaze.detectAndCompute(this, mask, keyPoints, descriptors);
    }

    public FeaturedImage(String path) throws IOException {
        super();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap image = LoadUtil.getBitmapFromPath(path);
        Utils.bitmapToMat(image, this);
        Imgproc.cvtColor(this, this, Imgproc.COLOR_BGR2GRAY);
        Mat mask = Mat.ones(size(), CvType.CV_8U);
        sAkaze.detectAndCompute(this, mask, keyPoints, descriptors);
        image.recycle();
    }

    public void release(){
        keyPoints.release();
        descriptors.release();
        super.release();
    }
}