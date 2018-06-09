package wallstudio.work.kamishiba;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class CorrectedImage {

    public final static int GRID_DIVISIONS = 5;
    public final static  Scalar BORDER_COLOR = new Scalar(255, 100, 100);
    public final static  Scalar GRID_COLOR = new Scalar(100, 255, 100);

    public static void PerspectiveTransform(Mat src, Mat dest, Point vanisingRate, double pageAreaRatio, int size){

        float[] pts1 = calc4Points(src, vanisingRate, pageAreaRatio);
        float[] pts2 = new float[]{0, 0, size, 0, 0, size, size, size};
        Mat pts1m = new Mat(4,2, CvType.CV_32F);
        Mat pts2m = new Mat(4,2, CvType.CV_32F);
        pts1m.put(0,0, pts1);
        pts2m.put(0,0, pts2);
        Mat persMatrix = Imgproc.getPerspectiveTransform(pts1m, pts2m);
        Imgproc.warpPerspective(src, dest, persMatrix, new Size(size,size));
        Core.flip(dest, dest, -1);
        persMatrix.release();
//        Point[] ptsP = floats2Points(pts2);
//        // Vertical grid
//        for (int i = 0; i < GRID_DIVISIONS; i++) {
//            Point start = new Point(ptsP[0].x + (ptsP[1].x - ptsP[0].x) / GRID_DIVISIONS * i, ptsP[0].y + (ptsP[1].y - ptsP[0].y) / GRID_DIVISIONS * i);
//            Point end = new Point(ptsP[2].x + (ptsP[3].x - ptsP[2].x) / GRID_DIVISIONS * i, ptsP[2].y + (ptsP[3].y - ptsP[2].y) / GRID_DIVISIONS * i);
//            Imgproc.line(dest, start, end, GRID_COLOR, 2);
//        }
//        // Horizontal grid
//        for (int i = 0; i < GRID_DIVISIONS; i++) {
//            Point start = new Point(ptsP[0].x + (ptsP[2].x - ptsP[0].x) / GRID_DIVISIONS * i, ptsP[0].y + (ptsP[2].y - ptsP[0].y) / GRID_DIVISIONS * i);
//            Point end = new Point(ptsP[1].x + (ptsP[3].x - ptsP[1].x) / GRID_DIVISIONS * i, ptsP[1].y + (ptsP[3].y - ptsP[1].y) / GRID_DIVISIONS * i);
//            Imgproc.line(dest, start, end, GRID_COLOR, 2);
//        }
    }

    public static void DrawPerspectiveGuidLine(Mat srcAndDest, Point vanisingRate, double pageAreaRatio) {

        Point vanising = new Point((int) (vanisingRate.x * srcAndDest.width()), (int) (vanisingRate.y * srcAndDest.height()));
        float[] ptsf = calc4Points(srcAndDest, vanisingRate, pageAreaRatio);
        Point[] ptsP = floats2Points(ptsf);
        Point[] ptsPs = shurinkPoints(ptsP);

        // Perspective guid
        Imgproc.line(srcAndDest, vanising, ptsP[2], BORDER_COLOR, 3);
        Imgproc.line(srcAndDest, vanising, ptsP[3], BORDER_COLOR, 3);
        Imgproc.line(srcAndDest, ptsP[0], ptsP[1], BORDER_COLOR, 3);
        // Vertical grid
        for (int i = 0; i < GRID_DIVISIONS + 1; i++) {
            Point start = new Point(ptsPs[0].x + (ptsPs[1].x - ptsPs[0].x) / GRID_DIVISIONS * i, ptsPs[0].y + (ptsPs[1].y - ptsPs[0].y) / GRID_DIVISIONS * i);
            Point end = new Point(ptsPs[2].x + (ptsPs[3].x - ptsPs[2].x) / GRID_DIVISIONS * i, ptsPs[2].y + (ptsPs[3].y - ptsPs[2].y) / GRID_DIVISIONS * i);
            Imgproc.line(srcAndDest, start, end, GRID_COLOR, 2);
        }
        // Horizontal grid
        for (int i = 0; i < GRID_DIVISIONS + 1; i++) {
            Point start = new Point(ptsPs[0].x + (ptsPs[2].x - ptsPs[0].x) / GRID_DIVISIONS * i, ptsPs[0].y + (ptsPs[2].y - ptsPs[0].y) / GRID_DIVISIONS * i);
            Point end = new Point(ptsPs[1].x + (ptsPs[3].x - ptsPs[1].x) / GRID_DIVISIONS * i, ptsPs[1].y + (ptsPs[3].y - ptsPs[1].y) / GRID_DIVISIONS * i);
            Imgproc.line(srcAndDest, start, end, GRID_COLOR, 2);
        }
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

    private  static Point[] floats2Points(float[] p){
        return new Point[]{new Point(p[0], p[1]), new Point(p[2], p[3]), new Point(p[4], p[5]), new Point(p[6], p[7]) };
    }

    private  static Point[] shurinkPoints(Point[] p){
        int padding = 10;
        int buttomCorrent = padding / 2;

        return new Point[]{
                new Point(p[0].x + padding, p[0].y + padding),
                new Point(p[1].x - padding, p[1].y + padding),
                new Point(p[2].x + padding + buttomCorrent * 2, p[2].y - padding + buttomCorrent),
                new Point(p[3].x - padding - buttomCorrent * 2, p[3].y - padding + buttomCorrent)};
    }

    private static double distance(Point a, Point b){
        double x = a.x - b.x;
        double y = a.y - b.y;
        return Math.sqrt(x * x + y * y);
    }

    private static double area(Point[] polygon){
        double dot = (polygon[0].x - polygon[2].x) * (polygon[1].x - polygon[3].x)
                + (polygon[0].y - polygon[2].y) * (polygon[1].y - polygon[3].y);
        double cos = dot / (distance(polygon[0], polygon[2]) * distance(polygon[1], polygon[3]));
        double sin = Math.sin(Math.acos(cos));
        return  distance(polygon[0], polygon[2]) * distance(polygon[1], polygon[3]) * sin / 2;

    }
}
