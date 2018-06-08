package wallstudio.work.kamishiba;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
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

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2RGBA;
import static org.opencv.imgproc.Imgproc.findContours;


public class StandCameraActivity extends Activity {


    public static final Point DIAL_INPUT_IMAGE_SIZE = new Point(1280, 720);
    public static final int CAMERA_SIDE = CameraCharacteristics.LENS_FACING_BACK;
    public static final Bitmap.Config BUFFER_BITMAP_FORMAT = Bitmap.Config.ARGB_8888;
    public static final int BACKGROUND_BITMAP_REDUCTION = 16;

    private Point mInputImageSize;

    private TextureView mDebugPreview;
    private TextView mDebugPrint;

    private ImageView mInputPreviewView;
    private ImageView mBackground;
    private ImageView mMatchPreviewView;
    private ImageView mCoverView;
    private TextView mPageLabelView;

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

    public Surface mDebugPreviewSurface;
    public ImageReader mPreviewBuffer;

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
            Imgproc.resize(original, background, new Size(original.width() / BACKGROUND_BITMAP_REDUCTION, original.height() / BACKGROUND_BITMAP_REDUCTION));
            Imgproc.cvtColor(background, background, COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(background, background, new Size(21, 21), 0);
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
            createMatch(original, match);
            Utils.matToBitmap(match, mMatchBitmap, false);
            mMatchPreviewView.setImageBitmap(mMatchBitmap);
            match.release();

            original.release();
            image.close();
        }

        public void createPreview(Mat in, Mat out){
            Imgproc.resize(in, out, in.size());
        }

        public void createMatch(Mat in, Mat out){
            Imgproc.cvtColor(in,out, COLOR_BGR2GRAY);
            Imgproc.resize(in, out, new Size(mMatchBitmap.getWidth(), mMatchBitmap.getHeight()));
        }
    };

    TextureView.SurfaceTextureListener mDebugPreviewListener = new TextureView.SurfaceTextureListener() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        beginFullScreen();
        bindViews();

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
            }
        });
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
    }

    @Override
    protected void onPause(){
        releaseCamera();
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
        setContentView(R.layout.activity_stand_camera);
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
