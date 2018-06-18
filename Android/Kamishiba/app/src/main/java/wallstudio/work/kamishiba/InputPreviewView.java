package wallstudio.work.kamishiba;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class InputPreviewView extends CVImageView<Void> {

    public static final int GRID_DIVISIONS = 5;
    public static final Scalar BORDER_COLOR = new Scalar(255, 100, 100);
    public static final  Scalar GRID_COLOR = new Scalar(100, 255, 100);

    public InputPreviewView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); }
    
    public InputPreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {  super(context, attrs, defStyleAttr); }

    @Override
    protected Void process(Mat frame, Point vanishingRatio, double pageEdgeY) {

        Point vanising = new Point((int) (vanishingRatio.x * frame.width()), (int) (vanishingRatio.y * frame.height()));
        float[] ptsf = calc4Points(frame, vanishingRatio, pageEdgeY);
        Point[] ptsP = floats2Points(ptsf);
        Point[] ptsPs = shurinkPoints(ptsP);

        // Perspective guid
        Imgproc.line(frame, vanising, ptsP[2], BORDER_COLOR, 3);
        Imgproc.line(frame, vanising, ptsP[3], BORDER_COLOR, 3);
        Imgproc.line(frame, ptsP[0], ptsP[1], BORDER_COLOR, 3);
        // Vertical grid
        for (int i = 0; i < GRID_DIVISIONS + 1; i++) {
            Point start = new Point(ptsPs[0].x + (ptsPs[1].x - ptsPs[0].x) / GRID_DIVISIONS * i, ptsPs[0].y + (ptsPs[1].y - ptsPs[0].y) / GRID_DIVISIONS * i);
            Point end = new Point(ptsPs[2].x + (ptsPs[3].x - ptsPs[2].x) / GRID_DIVISIONS * i, ptsPs[2].y + (ptsPs[3].y - ptsPs[2].y) / GRID_DIVISIONS * i);
            Imgproc.line(frame, start, end, GRID_COLOR, 2);
        }
        // Horizontal grid
        for (int i = 0; i < GRID_DIVISIONS + 1; i++) {
            Point start = new Point(ptsPs[0].x + (ptsPs[2].x - ptsPs[0].x) / GRID_DIVISIONS * i, ptsPs[0].y + (ptsPs[2].y - ptsPs[0].y) / GRID_DIVISIONS * i);
            Point end = new Point(ptsPs[1].x + (ptsPs[3].x - ptsPs[1].x) / GRID_DIVISIONS * i, ptsPs[1].y + (ptsPs[3].y - ptsPs[1].y) / GRID_DIVISIONS * i);
            Imgproc.line(frame, start, end, GRID_COLOR, 2);
        }

        return null;
    }
}
