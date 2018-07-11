package wallstudio.work.kamishiba;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

public class MatchPreviewView extends CVImageView<Void> {

    public static final int DEFAULT_CORRECTED_IMAGE_WIDTH = 512;
    public static final int DEFAULT_CORRECTED_IMAGE_HEIGHT = 512;

    private int mCorrectedImageWidth;
    private int mCorrectedImageHeight;

    public TrainingDataList mTrainingDataList;

    public int page;
    public double similarity;

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

    @Override
    protected Void process(Mat frame, Point vanishingRatio, double pageEdgeY, boolean isPerspective) {
        if(frame == null || frame.width() == 0 || frame.height() == 0) return null;

        if(isPerspective) {
            float[] pts1 = calc4Points(frame, vanishingRatio, pageEdgeY);
            float[] pts2 = new float[]{0, 0, mCorrectedImageWidth, 0, 0, mCorrectedImageHeight, mCorrectedImageWidth, mCorrectedImageHeight};
            Mat pts1m = new Mat(4, 2, CvType.CV_32F);
            Mat pts2m = new Mat(4, 2, CvType.CV_32F);
            pts1m.put(0, 0, pts1);
            pts2m.put(0, 0, pts2);
            Mat persMatrix = Imgproc.getPerspectiveTransform(pts1m, pts2m);
            Imgproc.warpPerspective(frame, frame, persMatrix, new Size(mCorrectedImageWidth, mCorrectedImageHeight));
            Core.flip(frame, frame, 0);
            persMatrix.release();
        }else{
            Rect trimArea = new Rect(
                    (int)(frame.width() * TRIM_RATIO), (int)(frame.height() * TRIM_RATIO),
                    (int)(frame.width() - frame.width() * TRIM_RATIO), (int)(frame.height() - frame.height() * TRIM_RATIO));
            Mat trim = new Mat(frame, trimArea);
            Imgproc.resize(trim, frame, new Size(mCorrectedImageWidth, mCorrectedImageHeight));
        }

        FeaturedImage input = new FeaturedImage(frame);
        page = mTrainingDataList.indexOf(input);
        mTrainingDataList.drawMatches(input, frame, page);
        similarity = mTrainingDataList.getSimilarity(input);
        input.release();
        return null;
    }
}
