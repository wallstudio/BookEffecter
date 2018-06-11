package wallstudio.work.kamishiba;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MatchPreviewView extends CVImageView<Void> {

    public static final int DEFAULT_CORRECTED_IMAGE_WIDTH = 360;
    public static final int DEFAULT_CORRECTED_IMAGE_HEIGHT = 240;

    private int mCorrectedImageWidth;
    private int mCorrectedImageHeight;

    private int mBitmapSize;
    private LearndImageSet mSet;

    public MatchPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MatchPreviewView);
        mCorrectedImageWidth  = typedArray.getDimensionPixelSize(R.styleable.MatchPreviewView_corrected_image_width, DEFAULT_CORRECTED_IMAGE_WIDTH);
        mCorrectedImageHeight  = typedArray.getDimensionPixelSize(R.styleable.MatchPreviewView_corrected_image_height, DEFAULT_CORRECTED_IMAGE_HEIGHT);
    }
    public MatchPreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MatchPreviewView);
        mCorrectedImageWidth  = typedArray.getDimensionPixelSize(R.styleable.MatchPreviewView_corrected_image_width, DEFAULT_CORRECTED_IMAGE_WIDTH);
        mCorrectedImageHeight  = typedArray.getDimensionPixelSize(R.styleable.MatchPreviewView_corrected_image_height, DEFAULT_CORRECTED_IMAGE_HEIGHT);
    }

    public void setSet(LearndImageSet set){
        mSet = set;
    }

    @Override
    protected Void process(Mat frame, Point vanishingRatio, double pageEdgeY) {

        float[] pts1 = calc4Points(frame, vanishingRatio, pageEdgeY);
        float[] pts2 = new float[]{0, 0, mCorrectedImageWidth, 0, 0, mCorrectedImageHeight, mCorrectedImageWidth, mCorrectedImageHeight};
        Mat pts1m = new Mat(4,2, CvType.CV_32F);
        Mat pts2m = new Mat(4,2, CvType.CV_32F);
        pts1m.put(0,0, pts1);
        pts2m.put(0,0, pts2);
        Mat persMatrix = Imgproc.getPerspectiveTransform(pts1m, pts2m);
        Imgproc.warpPerspective(frame, frame, persMatrix, new Size(mCorrectedImageWidth, mCorrectedImageHeight));
        Core.flip(frame, frame, -1);
        persMatrix.release();

        mSet.search(frame);
        mSet.inputImageReduction(2);
        mSet.drawResult();
        frame.release();
        mSet.resultImage.copyTo(frame);
        return null;
    }
}
