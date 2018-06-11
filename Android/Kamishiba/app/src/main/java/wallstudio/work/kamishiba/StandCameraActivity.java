package wallstudio.work.kamishiba;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class StandCameraActivity extends Activity {

    public static final Point DIAL_INPUT_IMAGE_SIZE = new Point(800, 480);
//    public static final Point DIAL_INPUT_IMAGE_SIZE = new Point(1280, 720);
    public static final int CAMERA_SIDE = CameraCharacteristics.LENS_FACING_BACK;
    public static final Bitmap.Config BUFFER_BITMAP_FORMAT = Bitmap.Config.ARGB_8888;

    private Point mInputImageSize;

    private TextureView mDebugPreview;
    private TextView mDebugPrint;

    private InputPreviewView mInputPreviewView;
    private BackgroundView mBackground;
    private MatchPreviewView mMatchPreviewView;
    private ImageView mCoverView;
    private TextView mPageLabelView;
    private PerspectiveController mController;

    private HashMap<String, String> mDisplayDebugMessageList = new HashMap<>();

    private Bitmap mOriginalBitmap;

    private String mCameraID;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private android.util.Size[] mSupportPreviewSize;

    private Surface mDebugPreviewSurface;
    private ImageReader mImageBufferForProcessing;

    private LearndImageSet mLearndImageSet;


    // Suface is ready -> openCamera ->
    private TextureView.SurfaceTextureListener mDebugPreviewListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {
                openCamera();
                configureTransform(mDebugPreview, width, height);
            } catch (CameraAccessException ca) {
                alert("エラー", "カメラアクセスエラー\n\n" + ca.toString(), true);
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
                mImageBufferForProcessing = ImageReader.newInstance(mInputImageSize.x,mInputImageSize.y, ImageFormat.YUV_420_888,2);
                mImageBufferForProcessing.setOnImageAvailableListener(mOnImageAvailableListener, null);
                // Launch device
                mCameraDevice.createCaptureSession(Arrays.asList(mDebugPreviewSurface, mImageBufferForProcessing.getSurface()),
                        mCaptureSettingCallback,null);
            } catch (CameraAccessException e) {
                Log.e("ERROR",e.toString());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) { alert("エラー","カメラの接続が切れました (onDisconnected)", true); }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) { alert("エラー","カメラの接続が切れました (onError)", true); }
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
                builder.addTarget(mImageBufferForProcessing.getSurface());
                builder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                mCaptureSession.setRepeatingRequest(builder.build(), null,null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onReady(CameraCaptureSession session){

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) { alert("エラー","カメラの接続が切れました (onConfigureFailed)", true); }
    };
    // Processing setting
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if(isFinishing()) return;

            Image image = reader.acquireNextImage();
            if (image == null) {
                return;
            }
            Jni.image2Bitmap(image, mOriginalBitmap, true);
            Mat original = new Mat();
            Utils.bitmapToMat(mOriginalBitmap, original, false);

            // Multi thread
            mBackground.convertAsync(original, mController.vanishingRatio, mController.pageEdgeY);
            mInputPreviewView.convertAsync(original, mController.vanishingRatio, mController.pageEdgeY);
            mMatchPreviewView.convertAsync(original, mController.vanishingRatio, mController.pageEdgeY);

            mBackground.waitAndSetBufferBitmap();
            mInputPreviewView.waitAndSetBufferBitmap();
            mMatchPreviewView.waitAndSetBufferBitmap();

            original.release();
        }

    };


    static {System.loadLibrary("opencv_java3");}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stand_camera);
        beginFullScreen();
//        loadOpenCVManagerApp();

        mDebugPreview = findViewById(R.id.debugPreview);
        mDebugPrint = findViewById(R.id.debugPrint);

        mInputPreviewView = findViewById(R.id.inputPreviewView);
        mBackground = findViewById(R.id.background);
        mMatchPreviewView = findViewById(R.id.matchPreviewView);
        mCoverView = findViewById(R.id.coverView);
        mPageLabelView = findViewById(R.id.pageLabelView);
        mController = findViewById(R.id.controller_view);

        mLearndImageSet = new LearndImageSet(getResources());
        mLearndImageSet.setImageReduction(2);
        mMatchPreviewView.setSet(mLearndImageSet);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // OpenCamera when ready surface.
        openCameraWhenReadySurface();
    }

    @Override
    protected void onPause(){

        if(null != mImageBufferForProcessing)
            mImageBufferForProcessing.close();
        if(null != mCaptureSession)
            mCaptureSession.close();
        if(null != mCameraDevice)
            mCameraDevice.close();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mLearndImageSet.release();
        super.onStop();
    }


    public void onClickBack(View v){
        finish();
    }


    private void openCameraWhenReadySurface(){
        if(mDebugPreview.isAvailable()){
            mDebugPreviewListener.onSurfaceTextureAvailable(
                    mDebugPreview.getSurfaceTexture(),
                    mDebugPreview.getWidth(),
                    mDebugPreview.getHeight());
        }else {
            mDebugPreview.setSurfaceTextureListener(mDebugPreviewListener);
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() throws CameraAccessException {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if( mCameraManager.getCameraIdList().length > 0) {
            mCameraID = mCameraManager.getCameraIdList()[CAMERA_SIDE];
            StreamConfigurationMap map = mCameraManager.getCameraCharacteristics(mCameraID).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSupportPreviewSize = map.getOutputSizes(SurfaceTexture.class);
            // Detaile config (buffers and displays)
            mCameraManager.openCamera(mCameraID, mDeviceSettingCallback, null);
        }else{
            alert("エラー", "カメラが認識できません", true);
        }
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

    private Point selectSupportedPreviewSize(Point ideal, android.util.Size[] supports){
        if(supports.length > 0) {

            // Search val minimam distance
            double minDistance = Double.MAX_VALUE;
            android.util.Size betterSupport = supports[0];
            for (android.util.Size support : supports) {
                double distance = Math.abs(ideal.x - support.getWidth()) * Math.abs(ideal.y - support.getHeight());
                if (distance < minDistance) {
                    minDistance = distance;
                    betterSupport = support;
                }
            }
            return new Point(Math.round(betterSupport.getWidth()), Math.round(betterSupport.getHeight()));
        }else {
            alert("ERROR", "This isn't supported camera.", true);
            return new Point();
        }
    }

    private void alert(String title, String message, boolean isExit){

        DialogInterface.OnClickListener listener = null;
        if(isExit){
            listener = new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            };
        }

        Log.d("Alert", "[" + title + "] " + message);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", listener)
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

    private void loadOpenCVManagerApp(){
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
            }
        });
    }
}
