package wallstudio.work.kamishiba;

import android.support.annotation.Nullable;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;

public class CorrectedImage {

    public static void PerspectiveTransform(Mat src, Mat dest, Point vanisingRate, double pageAreaRatio, int size){

        float[] pts1 = calc4Points(src, vanisingRate, pageAreaRatio);
        float[] pts2 = new float[]{0, 0, size, 0, 0, size, size, size};
        Mat pts1m = new Mat(4,2, CvType.CV_32F);
        Mat pts2m = new Mat(4,2, CvType.CV_32F);
        pts1m.put(0,0, pts1);
        pts2m.put(0,0, pts2);
        Mat persMatrix = Imgproc.getPerspectiveTransform(pts1m, pts2m);
//        Imgproc.cvtColor(src, dest, COLOR_BGR2GRAY);
        Imgproc.warpPerspective(src, dest, persMatrix, new Size(size,size));
        Core.flip(dest, dest, -1);
    }

    public static void DrawPerspectiveGuidLine(Mat srcAndDest, Point vanisingRate, double pageAreaRatio){

        Point vanising = new Point((int)(vanisingRate.x*srcAndDest.width()), (int)(vanisingRate.y*srcAndDest.height()));
        float[] pts1 = calc4Points(srcAndDest, vanisingRate, pageAreaRatio);

        Scalar color = new Scalar(255, 100, 0, 100);
        Imgproc.line(srcAndDest, vanising, new Point(0, srcAndDest.height()), color,3);
        Imgproc.line(srcAndDest, vanising, new Point(srcAndDest.width(), srcAndDest.height()), color,3);
        Imgproc.line(srcAndDest, new Point(pts1[0], pts1[1]), new Point(pts1[2], pts1[3]), color,3);
    }

    public static float[] calc4Points(Mat src, Point vanisingRate, double pageAreaRatio){
        int h = src.height();
        int w = src.width();
        Point vanising = new Point((int)(vanisingRate.x*w), (int)(vanisingRate.y*h));
        int pageAreaY = (int)(pageAreaRatio*h);
        // ref. https://imgur.com/a/mNAz9Mm
        double a = pageAreaY - vanising.y;
        double b = h - pageAreaY;
        double ab = a + b;
        Point cross0 = new Point ((b / ab) * vanising.x, pageAreaY);
        Point cross1 = new Point ((a / ab) * (w - vanising.x) + vanising.x, pageAreaY);

        return new float[]{(int)cross0.x, (int)cross0.y, (int)cross1.x, (int)cross1.y, 0, h, w, h};
    }
}
