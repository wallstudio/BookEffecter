package wallstudio.work.kamishiba;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class StandCameraActivity extends Activity {

    public static final Point DIAL_INPUT_IMAGE_SIZE = new Point(800, 480);
//    public static final Point DIAL_INPUT_IMAGE_SIZE = new Point(1280, 720);
    public static final int CAMERA_SIDE = CameraCharacteristics.LENS_FACING_BACK;
    public static final Bitmap.Config BUFFER_BITMAP_FORMAT = Bitmap.Config.ARGB_8888;
    public static final int BACKGROUND_BITMAP_REDUCTION = 16;
    public static final int BACKGROUND_BITMAP_BLUR_KERNAL = 15;

    private Point mInputImageSize;

    private TextureView mDebugPreview;
    private TextView mDebugPrint;

    private ImageView mInputPreviewView;
    private ImageView mBackground;
    private ImageView mMatchPreviewView;
    private ImageView mCoverView;
    private TextView mPageLabelView;
    private ImageView mVanisingCursor;
    private View mVanisingCursorArea;
    private View mPageYCursor;
    private View mPageYCursorArea;
    private View mInputPreviewWrapper;

    private HashMap<String, String> mDisplayDebugMessageList = new HashMap<>();

    private Bitmap mOriginalBitmap;
    private Bitmap mPreviewBitmap;
    private Bitmap mMatchBitmap;
    private Bitmap mBackgroundBitmap;

    private String mCameraID;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private android.util.Size[] mSupportPreviewSize;

    private Surface mDebugPreviewSurface;
    private ImageReader mPreviewBuffer;

    // 汚いけど，レイアウトが完全に計算され切ったときのイベントが無いので
    private boolean mIsInitedCursors = false;
    private org.opencv.core.Point mVanisingRatio = new org.opencv.core.Point(0.5, 0.2);
    private double mPageAearRatio = 0.65;
    private LearndImageSet mLearndImageSet;


    // Device setting (Contains Capture and Processing callback)
    CameraDevice.StateCallback mDeviceSettingCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            try {
                // Get supported size
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraID);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mSupportPreviewSize = map.getOutputSizes(ImageFormat.YUV_420_888);
                mInputImageSize = selectSupportedPreviewSize(DIAL_INPUT_IMAGE_SIZE, mSupportPreviewSize);
                setDisplayDebugMessage("SelectedSupportSize", mInputImageSize.x + "x" + mInputImageSize.y);
                // DebugPreview
                SurfaceTexture texture = mDebugPreview.getSurfaceTexture();
                texture.setDefaultBufferSize(mDebugPreview.getWidth(), mDebugPreview.getHeight());
                mDebugPreviewSurface = new Surface(texture);
                // InputBuffer
                mOriginalBitmap = Bitmap.createBitmap(mInputImageSize.x, mInputImageSize.y, BUFFER_BITMAP_FORMAT);
                mPreviewBitmap = Bitmap.createBitmap(mInputImageSize.x, mInputImageSize.y, BUFFER_BITMAP_FORMAT);
                mMatchBitmap = Bitmap.createBitmap(mMatchPreviewView.getWidth(), mMatchPreviewView.getHeight(), BUFFER_BITMAP_FORMAT);
                mBackgroundBitmap = Bitmap.createBitmap(mInputImageSize.x / BACKGROUND_BITMAP_REDUCTION, mInputImageSize.y/ BACKGROUND_BITMAP_REDUCTION, BUFFER_BITMAP_FORMAT);
                mPreviewBuffer = ImageReader.newInstance(mInputImageSize.x,mInputImageSize.y, ImageFormat.YUV_420_888,2);
                mPreviewBuffer.setOnImageAvailableListener(mOnImageAvailableListener, null);
                // Launch device
                mCameraDevice.createCaptureSession(Arrays.asList(mDebugPreviewSurface, mPreviewBuffer.getSurface()),
                        mCaptureSettingCallback,null);
            } catch (CameraAccessException e) {
                Log.e("ERROR",e.toString());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) { releaseCamera(); }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) { releaseCamera(); }
    };
    // Capture setting
    private CameraCaptureSession.StateCallback mCaptureSettingCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            mCaptureSession = cameraCaptureSession;
            CaptureRequest.Builder builder = null;
            try {
                builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.addTarget(mDebugPreviewSurface);
                builder.addTarget(mPreviewBuffer.getSurface());
                builder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                mCaptureSession.setRepeatingRequest(builder.build(), null,null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) { }
    };
    // Processing setting
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireNextImage();
            if (image == null) {
                return;
            }
            // YUV -> ARGB
            Image.Plane Y_plane = image.getPlanes()[0];
            int Y_rowStride = Y_plane.getRowStride();
            Image.Plane U_plane = image.getPlanes()[1];
            Image.Plane V_plane = image.getPlanes()[2];
            int imageHeight = image.getHeight();
            // Create original bitmap
            Jni.yuvByteArrayToBmp(Y_plane.getBuffer(), U_plane.getBuffer(), V_plane.getBuffer(), mOriginalBitmap.getWidth(), mOriginalBitmap.getHeight(), mOriginalBitmap);
            Mat original = new Mat();
            Utils.bitmapToMat(mOriginalBitmap, original, false);
            // Convert for Background
            Mat background = new Mat();
            createBackground(original, background);
            Utils.matToBitmap(background, mBackgroundBitmap, false);
            mBackground.setImageBitmap(mBackgroundBitmap);
            background.release();
            // Convert for Preview
            Mat preview = new Mat();
            createPreview(original, preview);
            Utils.matToBitmap(preview, mPreviewBitmap);
            mInputPreviewView.setImageBitmap(mPreviewBitmap);
            preview.release();
            // Convert for Parsing
            Mat match = new Mat();
            createMatch(original, match, 256);
            if(mMatchBitmap.getWidth() != match.width() || mMatchBitmap.getHeight() != match.height()){
                mMatchBitmap.recycle();
                mMatchBitmap = Bitmap.createBitmap(match.width(), match.height(), BUFFER_BITMAP_FORMAT);
            }
            Utils.matToBitmap(match, mMatchBitmap, false);
            mMatchPreviewView.setImageBitmap(mMatchBitmap);
            match.release();

            original.release();
            image.close();
        }

        private void createBackground(Mat in, Mat out) {
            Imgproc.resize(in, out, new Size(in.width() / BACKGROUND_BITMAP_REDUCTION, in.height() / BACKGROUND_BITMAP_REDUCTION));
            Imgproc.cvtColor(out, out, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(out, out, new Size(BACKGROUND_BITMAP_BLUR_KERNAL, BACKGROUND_BITMAP_BLUR_KERNAL), 0);
        }

        private void createPreview(Mat in, Mat out){
            in.copyTo(out);
            CorrectedImage.DrawPerspectiveGuidLine(out, mVanisingRatio, mPageAearRatio);
        }

        private void createMatch(Mat in, Mat out, int size){
            CorrectedImage.PerspectiveTransform(in, out, mVanisingRatio, mPageAearRatio, size);
            mLearndImageSet.search(out);
            mLearndImageSet.drawResult();
            out.release();
            mLearndImageSet.resultImage.copyTo(out);
        }
    };

    private TextureView.SurfaceTextureListener mDebugPreviewListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {
                openCamera();
                configureTransform(mDebugPreview, width, height);
            } catch (CameraAccessException ca) {
                ca.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            configureTransform(mDebugPreview, width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return false; }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
    };

    private View.OnTouchListener mVanisingCursorDragListener = new View.OnTouchListener() {
        // ref. https://akira-watson.com/android/imageview-drag.html
        private int mPreX;
        private int mPreY;
        @Override
        public boolean onTouch(View view, MotionEvent event) {

            mIsInitedCursors = true;

            int globalX = (int) event.getRawX();
            int globalY = (int) event.getRawY();

            if(event.getAction() == MotionEvent.ACTION_MOVE){
                    int deltaX = globalX - mPreX;
                    int deltaY = globalY - mPreY;
                    int left = mVanisingCursor.getLeft() + deltaX;
                    int top = mVanisingCursor.getTop() + deltaY;
                    int minLeft = 0;
                    int minTop = 0;
                    int maxRight = mVanisingCursorArea.getWidth() - mVanisingCursor.getWidth();
                    int maxBottom = mVanisingCursorArea.getHeight() - mVanisingCursor.getHeight();
                    left = left < minLeft ? minLeft : left;
                    left = left > maxRight ? maxRight : left;
                    top = top < minTop ? minTop : top;
                    top = top > maxBottom ? maxBottom : top;
                    int right = left + mVanisingCursor.getWidth();
                    int bottom = top + mVanisingCursor.getHeight();

                    mVanisingCursor.layout(left, top, right, bottom);

                    int xOnImage = mVanisingCursorArea.getLeft() + mVanisingCursor.getLeft() + mVanisingCursor.getWidth() / 2;
                    int yOnImage = mVanisingCursorArea.getTop() + mVanisingCursor.getTop() + mVanisingCursor.getHeight() / 2;

                    mVanisingRatio.x = xOnImage / (double)mInputPreviewWrapper.getWidth();
                    mVanisingRatio.y = yOnImage / (double)mInputPreviewWrapper.getHeight();

                    if(mVanisingRatio.y > mPageAearRatio - 0.05){
                        mPageAearRatio = mVanisingRatio.y + 0.05;
                        top = (int) (mInputPreviewWrapper.getHeight() * mPageAearRatio - mPageYCursorArea.getTop()) - mPageYCursor.getHeight() / 2;
                        mPageYCursor.layout(mPageYCursor.getLeft(), top, mPageYCursor.getRight(), top + mPageYCursor.getHeight());
                    }
            }
            mPreX = globalX;
            mPreY = globalY;
            return true;
        }
    };

    private View.OnTouchListener mPageYCursorDragListener = new View.OnTouchListener() {
        private int mPreY;
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            mIsInitedCursors = true;

            int globalY = (int) event.getRawY();

            if(event.getAction() == MotionEvent.ACTION_MOVE){
                int deltaY = globalY - mPreY;
                int left = mPageYCursor.getLeft();
                int top = mPageYCursor.getTop() + deltaY;
                int minLeft = 0;
                int minTop = 0;
                int maxRight = mPageYCursorArea.getWidth() - mPageYCursor.getWidth();
                int maxBottom = mPageYCursorArea.getHeight() - mPageYCursor.getHeight();
                left = left < minLeft ? minLeft : left;
                left = left > maxRight ? maxRight : left;
                top = top < minTop ? minTop : top;
                top = top > maxBottom ? maxBottom : top;
                int right = left + mPageYCursor.getWidth();
                int bottom = top + mPageYCursor.getHeight();

                mPageYCursor.layout(left, top, right, bottom);

                int yOnImage = mPageYCursorArea.getTop() + mPageYCursor.getTop() + mPageYCursor.getHeight() / 2;

                mPageAearRatio =  yOnImage / (double)mInputPreviewWrapper.getHeight();

                if(mVanisingRatio.y > mPageAearRatio - 0.05){
                    mVanisingRatio.y = mPageAearRatio - 0.05;
                    left = (int) (mInputPreviewWrapper.getWidth() * mVanisingRatio.x - mVanisingCursorArea.getLeft()) - mVanisingCursor.getWidth() / 2;
                    top = (int) (mInputPreviewWrapper.getHeight() * mVanisingRatio.y - mVanisingCursorArea.getTop()) - mVanisingCursor.getHeight() / 2;
                    mVanisingCursor.layout(left, top, left + mVanisingCursor.getWidth(), top + mVanisingCursor.getHeight());
                }
            }
            mPreY = globalY;
            return true;
        }
    };

    private View.OnLayoutChangeListener mInitSetVanisingCursorListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if(!mIsInitedCursors) {
                left = (int) (mInputPreviewWrapper.getWidth() * mVanisingRatio.x - mVanisingCursorArea.getLeft()) - mVanisingCursor.getWidth() / 2;
                top = (int) (mInputPreviewWrapper.getHeight() * mVanisingRatio.y - mVanisingCursorArea.getTop()) - mVanisingCursor.getHeight() / 2;
                mVanisingCursor.layout(left, top, left + mVanisingCursor.getWidth(), top + mVanisingCursor.getHeight());
            }
        }
    };

    private View.OnLayoutChangeListener mInitSetPageYCursorListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if(!mIsInitedCursors) {
                top = (int) (mInputPreviewWrapper.getHeight() * mPageAearRatio - mPageYCursorArea.getTop()) - mPageYCursor.getHeight() / 2;
                mPageYCursor.layout(mPageYCursor.getLeft(), top, mPageYCursor.getRight(), top + mPageYCursor.getHeight());
            }
        }
    };

    static {System.loadLibrary("opencv_java3");}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stand_camera);
        beginFullScreen();
        bindViews();

//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, new BaseLoaderCallback(this) {
//            @Override
//            public void onManagerConnected(int status) {
//                super.onManagerConnected(status);
//            }
//        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mDebugPreview.isAvailable()){
            mDebugPreviewListener.onSurfaceTextureAvailable(
                    mDebugPreview.getSurfaceTexture(),
                    mDebugPreview.getWidth(),
                    mDebugPreview.getHeight());
        }else {
            mDebugPreview.setSurfaceTextureListener(mDebugPreviewListener);
        }
        mIsInitedCursors = false;
        mVanisingCursor.addOnLayoutChangeListener(mInitSetVanisingCursorListener);
        mVanisingCursor.setOnTouchListener(mVanisingCursorDragListener);
        mPageYCursor.addOnLayoutChangeListener(mInitSetPageYCursorListener);
        mPageYCursor.setOnTouchListener(mPageYCursorDragListener);
        mLearndImageSet = new LearndImageSet(getResources());
    }

    @Override
    protected void onPause(){
        releaseCamera();
        mLearndImageSet.release();
        super.onPause();
    }

    @SuppressLint("MissingPermission")
    private void openCamera() throws CameraAccessException {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        mCameraID = mCameraManager.getCameraIdList()[CAMERA_SIDE];
        StreamConfigurationMap map = mCameraManager.getCameraCharacteristics(mCameraID).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        mSupportPreviewSize = map.getOutputSizes(SurfaceTexture.class);
        // Detaile config (buffers and displays)
        mCameraManager.openCamera(mCameraID, mDeviceSettingCallback, null);
    }

    private void releaseCamera(){
        mCaptureSession.close();
        mCameraDevice.close();
    }

    private void configureTransform(TextureView textureView, int previewWidth, int previewHeight) {
        int rotation = StandCameraActivity.this.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
//        RectF viewRect = new RectF(0, 0, textureView.getWidth(), textureView.getHeight());
//        RectF bufferRect = new RectF(0, 0, previewHeight, previewWidth);
//        PointF center = new PointF(viewRect.centerX(), viewRect.centerY());
//        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
//            bufferRect.offset(center.x - bufferRect.centerX(), center.y - bufferRect.centerY());
//            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
//            float scale = Math.max(
//                    (float) textureView.getHeight() / previewHeight,
//                    (float) textureView.getWidth() / previewHeight);
//            matrix.postScale(scale, scale, center.x, center.y);
//            matrix.postRotate(90 * (rotation - 2), center.x, center.y);
//        } else if (Surface.ROTATION_180 == rotation) {
//            matrix.postRotate(180, center.x, center.y);
//        }
//        matrix.postScale(0.5f, 0.5f);
//        matrix.postRotate(0);
//        textureView.setTransform(matrix);
    }

    private void beginFullScreen(){
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void bindViews(){
        mDebugPreview = findViewById(R.id.debugPreview);
        mDebugPrint = findViewById(R.id.debugPrint);

        mInputPreviewView = findViewById(R.id.inputPreviewView);
        mBackground = findViewById(R.id.background);
        mMatchPreviewView = findViewById(R.id.matchPreviewView);
        mCoverView = findViewById(R.id.coverView);
        mPageLabelView = findViewById(R.id.pageLabelView);
        mVanisingCursor = findViewById(R.id.vanising_cursor);
        mVanisingCursorArea = findViewById(R.id.vanising_cursor_area);
        mPageYCursor = findViewById(R.id.page_y_cursor);
        mPageYCursorArea = findViewById(R.id.page_y_cursor_area);
        mInputPreviewWrapper = findViewById(R.id.input_preview_wrapper);
    }

    private Point selectSupportedPreviewSize(Point ideal, android.util.Size[] supports){
        if(supports.length < 1)
            alert("ERROR", "This isn't supported camera.");
        // Search val minimam distance
        double minDistance = Double.MAX_VALUE;
        android.util.Size betterSupport = supports[0];
        for(android.util.Size support: supports){
            double distance = Math.abs(ideal.x - support.getWidth()) * Math.abs(ideal.y - support.getHeight());
            if(distance < minDistance){
                minDistance = distance;
                betterSupport = support;
            }
        }
        return  new Point(Math.round(betterSupport.getWidth()), Math.round(betterSupport.getHeight()));
    }

    private void alert(String title, String message){
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void setDisplayDebugMessage(String key, String message){
        mDisplayDebugMessageList.put(key, message);
        String joindMessage = "";
        for(Map.Entry<String, String> entry : mDisplayDebugMessageList.entrySet()){
            joindMessage += entry.getKey() + ": " + entry.getValue() + "\n";
        }
        mDebugPrint.setText(joindMessage);
    }
}
