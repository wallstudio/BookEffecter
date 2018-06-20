package wallstudio.work.kamishiba;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class BackgroundView extends CVImageView<Void> {

    public static final int BACKGROUND_BITMAP_BLUR_KERNAL = 5;

    public BackgroundView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); }
    
    public BackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); }

    @Override
    protected Void process(Mat frame, Point vanishingRatio, double pageEdgeY, boolean isPerspective) {
        Imgproc.resize(frame, frame, new Size(bufferWidth, bufferHeight));
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(frame, frame, new Size(BACKGROUND_BITMAP_BLUR_KERNAL, BACKGROUND_BITMAP_BLUR_KERNAL), 0);
        return null;
    }
}
