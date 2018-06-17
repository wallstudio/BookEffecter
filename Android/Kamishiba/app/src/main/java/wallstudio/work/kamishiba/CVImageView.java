package wallstudio.work.kamishiba;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.ExecutionException;

public abstract class CVImageView<Return> extends android.support.v7.widget.AppCompatImageView {

    public static final Bitmap.Config BUFFER_BITMAP_FORMAT = Bitmap.Config.ARGB_8888;
    public static final Size DEFAULT_BUFFER_BITMAP_SIZE = new Size(256, 256);

    public int bufferWidth = 0;
    public int bufferHeight = 0;

    protected Bitmap mBitmapBuffer;
    protected Mat mMatBuffer;


    public CVImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initBitmap(context, attrs);
    }
    public CVImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initBitmap(context, attrs);
    }


    public void convert(final Mat frame, final Point vanishingRatio, final double pageEdgeY) {
        mMatBuffer = frame.clone();
        process(mMatBuffer, vanishingRatio, pageEdgeY);
        if(mBitmapBuffer.getWidth() != mMatBuffer.width() || mBitmapBuffer.getHeight() != mMatBuffer.height())
            Imgproc.resize(mMatBuffer, mMatBuffer, new Size(mBitmapBuffer.getWidth(), mBitmapBuffer.getHeight()));
        Utils.matToBitmap(mMatBuffer, mBitmapBuffer, false);
        setImageBitmap(mBitmapBuffer);
    }

    protected abstract Return process(final Mat frame, final Point vanishingRatio, final double pageEdgeY);

    private void initBitmap(Context context, AttributeSet attrs){
        // ref. https://qiita.com/Hoshi_7/items/57c3a79c43efe05b5368
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CVImageView);
        bufferWidth  = typedArray.getDimensionPixelSize(R.styleable.CVImageView_buffer_width, (int)DEFAULT_BUFFER_BITMAP_SIZE.width);
        bufferHeight  = typedArray.getDimensionPixelSize(R.styleable.CVImageView_buffer_height, (int)DEFAULT_BUFFER_BITMAP_SIZE.height);
        mBitmapBuffer =  Bitmap.createBitmap(bufferWidth, bufferHeight, BUFFER_BITMAP_FORMAT);
    }

    protected static float[] calc4Points(Mat src, Point vanisingRate, double pageAreaRatio){
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

    protected  static Point[] floats2Points(float[] p){
        return new Point[]{new Point(p[0], p[1]), new Point(p[2], p[3]), new Point(p[4], p[5]), new Point(p[6], p[7]) };
    }

    protected  static Point[] shurinkPoints(Point[] p){
        int padding = 10;
        int buttomCorrent = padding / 2;

        return new Point[]{
                new Point(p[0].x + padding, p[0].y + padding),
                new Point(p[1].x - padding, p[1].y + padding),
                new Point(p[2].x + padding + buttomCorrent * 2, p[2].y - padding + buttomCorrent),
                new Point(p[3].x - padding - buttomCorrent * 2, p[3].y - padding + buttomCorrent)};
    }

    protected static double distance(Point a, Point b){
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
