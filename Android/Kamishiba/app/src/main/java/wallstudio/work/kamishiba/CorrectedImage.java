package wallstudio.work.kamishiba;

import android.graphics.Color;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;

public class CorrectedImage {

    public Mat resultImage;
    public Point vanising;
    public int pageAreaY;
    public Point cross0;
    public Point cross1;
    public int size;
    public Mat processingImage;

    public CorrectedImage(Mat image, Point vanisingRate, double pageAreaRatio, boolean isDrawProcessing, int size){
        resultImage = image.clone();
        Imgproc.cvtColor(resultImage, resultImage, COLOR_BGR2GRAY);
        int h = resultImage.height();
        int w = resultImage.width();
        vanising = new Point((int)(vanisingRate.x*w), (int)(vanisingRate.y*h));
        pageAreaY = (int)(pageAreaRatio*h);
        cross0 = new Point ((int)((1-((pageAreaY-vanising.y)/(h-vanising.y)))*vanising.x), pageAreaY);
        cross1 = new Point (w-(int)((1-((pageAreaY-vanising.y)/(h-vanising.y)))*vanising.x), pageAreaY);

        this.size = size;
        float[] pts1 = new float[]{(int)cross0.x, (int)cross0.y, (int)cross1.x, (int)cross1.y, 0, h, w, h};
        float[] pts2 = new float[]{0, 0, size, 0, 0, size, size, size};
        Mat pts1m = new Mat(4,2, CvType.CV_32F);
        Mat pts2m = new Mat(4,2, CvType.CV_32F);
        pts1m.put(0,0,pts1);
        pts2m.put(0,0,pts2);
        Mat persMatrix = Imgproc.getPerspectiveTransform(pts1m, pts2m);
        Imgproc.warpPerspective(resultImage,resultImage,persMatrix,new Size(size,size));
        Core.flip(resultImage,resultImage, -1);

        if (isDrawProcessing) {
            Scalar color = new Scalar(255, 100, 0, 100);
            processingImage = image.clone();
            Imgproc.line(processingImage, vanising, new Point(0, h), color,3);
            Imgproc.line(processingImage, vanising, new Point(w, h), color,3);
            Imgproc.line(processingImage, cross0, cross1, color,3);
        }
    }
}
