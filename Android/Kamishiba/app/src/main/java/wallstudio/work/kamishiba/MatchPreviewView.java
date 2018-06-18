package wallstudio.work.kamishiba;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MatchPreviewView extends CVImageView<Void> {

    public static final int DEFAULT_CORRECTED_IMAGE_WIDTH = 512;
    public static final int DEFAULT_CORRECTED_IMAGE_HEIGHT = 512;

    private int mCorrectedImageWidth;
    private int mCorrectedImageHeight;

    private int mBitmapSize;
    public LearndImageSet mSet;
    private ConvertTask mTask;

    public int page;
    public double similar;

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

    public AsyncTask.Status getStatus(){
        if(mTask != null)
            return  mTask.getStatus();
        else
            return AsyncTask.Status.FINISHED;
    }

    public void setSet(LearndImageSet set){
        mSet = set;
    }

    public final void convertAsync(final Mat frame, final Point vanishingRatio, final double pageEdgeY){
        mTask = new ConvertTask(this, this, mBitmapBuffer);
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                new ConvertTask.MatAndPram(frame.clone(), vanishingRatio, pageEdgeY));
    }

    @Override
    protected Void process(Mat frame, Point vanishingRatio, double pageEdgeY) {
        if(frame == null || frame.width() == 0 || frame.height() == 0) return null;

        float[] pts1 = calc4Points(frame, vanishingRatio, pageEdgeY);
        float[] pts2 = new float[]{0, 0, mCorrectedImageWidth, 0, 0, mCorrectedImageHeight, mCorrectedImageWidth, mCorrectedImageHeight};
        Mat pts1m = new Mat(4,2, CvType.CV_32F);
        Mat pts2m = new Mat(4,2, CvType.CV_32F);
        pts1m.put(0,0, pts1);
        pts2m.put(0,0, pts2);
        Mat persMatrix = Imgproc.getPerspectiveTransform(pts1m, pts2m);
        Imgproc.warpPerspective(frame, frame, persMatrix, new Size(mCorrectedImageWidth, mCorrectedImageHeight));
        Core.flip(frame, frame, 0);
        persMatrix.release();

        mSet.search(frame);
        mSet.inputImageReduction(2);
        mSet.drawResult();
        frame.release();
        mSet.resultImage.copyTo(frame);
        similar = mSet.bestScore;
        page = mSet.pageIndex;
//        mSet.release();
        return null;
    }

    private static class ConvertTask extends AsyncTask<ConvertTask.MatAndPram, Void, Mat> {

        public static class MatAndPram{
            public Mat mat;
            public Point vanising;
            public double pageY;

            public MatAndPram(Mat mat, Point vanising, double pageY){
                this.mat = mat;
                this.vanising = vanising;
                this.pageY = pageY;
            }
        }

        private CVImageView mContext;
        private ImageView mImageView;
        private Bitmap mBitmapBuffer;

        public ConvertTask(CVImageView context, ImageView imageView, Bitmap bitmap){
            mContext = context;
            mImageView = imageView;
            mBitmapBuffer = bitmap;
        }

        @Override
        protected Mat doInBackground(MatAndPram... matAndPrams) {
            Mat frame = matAndPrams[0].mat;
            Point vanising = matAndPrams[0].vanising;
            double pageY = matAndPrams[0].pageY;

            mContext.process(frame, vanising, pageY);
            return frame;
        }

        @Override
        protected void onPostExecute(Mat mat){
            if(mat == null || mat.width() <= 0 && mat.height() <= 0) return;
            if(mBitmapBuffer.getWidth() != mat.width() || mBitmapBuffer.getHeight() != mat.height())
                Imgproc.resize(mat, mat, new Size(mBitmapBuffer.getWidth(), mBitmapBuffer.getHeight()));
            Utils.matToBitmap(mat, mBitmapBuffer, false);
            mContext.setImageBitmap(mBitmapBuffer);
        }
    }
}
