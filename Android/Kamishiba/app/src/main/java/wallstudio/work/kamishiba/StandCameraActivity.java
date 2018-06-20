package wallstudio.work.kamishiba;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
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
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StandCameraActivity extends Activity {

    public static final Point DIAL_INPUT_IMAGE_SIZE = new Point(800, 480);
    // public static final Point DIAL_INPUT_IMAGE_SIZE = new Point(1280, 720);
    public static final int CAMERA_SIDE = CameraCharacteristics.LENS_FACING_BACK;
    public static final Bitmap.Config BUFFER_BITMAP_FORMAT = Bitmap.Config.ARGB_8888;


    private String mPackageId;
    private int mImageCount;
    private int mAudioIndex;
    private double[] mTrackTiming;

    private TextureView mDebugPreview;
    private TextView mDebugPrint;
    private HashMap<String, String> mDisplayDebugMessageList = new HashMap<>();

    private InputPreviewView mInputPreviewView;
    private BackgroundView mBackgroundView;
    private MatchPreviewView mMatchPreviewView;
    private ImageView mCoverView;
    private TextView mTitleView;
    private TextView mAuthorView;
    private TextView mPageLabelView;
    private PerspectiveController mController;

    private String mCameraID;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private Point mInputImageSize;
    private android.util.Size[] mSupportPreviewSize;
    private Surface mDebugPreviewSurface;
    private ImageReader mImageBufferForProcessing;

    private Bitmap mOriginalBitmap;
    private LearndImageSet mLearndImageSet;
    private int mCurrentPage;
    private MediaPlayer mMediaPlayer;

    static {System.loadLibrary("opencv_java3");}


    // 準備が整ったタイミングでカメラを起動するだけ
    private TextureView.SurfaceTextureListener mDebugPreviewListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {
                openCamera();
            } catch (CameraAccessException ca) {
                Toast.makeText(StandCameraActivity.this, "Camera access error", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return false; }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) { }
    };

    // カメラの設定 & バッファの初期化と登録
    private CameraDevice.StateCallback mDeviceSettingCallback = new CameraDevice.StateCallback() {
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
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Toast.makeText(StandCameraActivity.this,"Camera disconnected (onDisconnected)", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            Toast.makeText(StandCameraActivity.this,"Camera disconnected (onError)", Toast.LENGTH_SHORT).show();
        }
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
        public void onReady(CameraCaptureSession session){ }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
            Toast.makeText(StandCameraActivity.this,"Camera disconnected (onConfigureFailed)", Toast.LENGTH_SHORT).show();
        }
    };

    // メインループ
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        private Mat mOriginal = new Mat();
        private int mCameraOrientation = -1;

        @Override
        public void onImageAvailable(ImageReader reader) {
            if (isFinishing()) return;

            Image image = reader.acquireNextImage();
            if (image == null) return;

            // ImageReader から Mat に変換
            Jni.image2Bitmap(image, mOriginalBitmap, true);
            Utils.bitmapToMat(mOriginalBitmap, mOriginal, false);

            // 内面カメラなので D = -C なら向きが一致する
            int displayOrientation = getDisplayOrientation();
            if (mCameraOrientation == -1)
                mCameraOrientation = getCameraOrientation();
            if (Math.abs(360 - mCameraOrientation) != displayOrientation) {
                Core.flip(mOriginal, mOriginal, 0);
            } else {
                Core.flip(mOriginal, mOriginal, 1);
            }

            // 処理の呼び出し
            mBackgroundView.convert(mOriginal, mController.vanishingRatio, mController.pageEdgeY);
            mInputPreviewView.convert(mOriginal, mController.vanishingRatio, mController.pageEdgeY);
            if (mMatchPreviewView.getStatus() == AsyncTask.Status.FINISHED) {
                // AKAZE抽出は重いので，非同期
                mMatchPreviewView.convertAsync(mOriginal, mController.vanishingRatio, mController.pageEdgeY);
                // 音声
                mCurrentPage = smooth(mMatchPreviewView.page);
                action(mCurrentPage);
                // 音声止める
                if (mMediaPlayer.isPlaying()){
                    int end = (int) (mTrackTiming[mCurrentPage * 2 + 1] * 1000);
                    int now = mMediaPlayer.getCurrentPosition();
                    if(end < now){
                        mMediaPlayer.pause();
                    }
                }
            }
            mPageLabelView.setText((mCurrentPage >= 0 ? mCurrentPage + 1 : "-")  + "/" + mImageCount);

            setDisplayDebugMessage("page", mMatchPreviewView.page + "/" + mImageCount);
            setDisplayDebugMessage("similar", String.format("%.3f", mMatchPreviewView.similar));
        }

        private static final int BUFFER_SIZE = 8;
        private List<Integer> mBufferForSmooth = new ArrayList<>();

        private int smooth(int value) {
            mBufferForSmooth.add(value);
            while (mBufferForSmooth.size() > BUFFER_SIZE)
                mBufferForSmooth.remove(0);
            int ret;
            if (mBufferForSmooth.size() == BUFFER_SIZE) {

                int pre = mBufferForSmooth.get(0);
                boolean isSame = true;
                for (Integer i : mBufferForSmooth) {
                    if (pre != i) {
                        isSame = false;
                        break;
                    }
                }

                ret = isSame ? pre : -1;
            } else {
                ret = -1;
            }
            Log.d("SmoothPage", String.format("smooth=%d (%s), ", ret, mBufferForSmooth.toString()));
            return ret;
        }

        private int mPreCount = -1;

        private void action(int count) {
            if (mPreCount != count) {
                Log.d("Action", String.format("CHANGE %d -> %d", mPreCount, count));
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                }
                if (count < 0) return;
                if (count * 2 < mTrackTiming.length) {
                    mMediaPlayer.seekTo((int) (mTrackTiming[count * 2] * 1000));
                    mMediaPlayer.start();
                } else {
                    Log.e("Action", "Out of range " + count * 2 + ">" + mTrackTiming.length);
                }
            }
            mPreCount = count < 0 ? mPreCount : count;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stand_camera);
        // Fullscreen
        this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        // loadOpenCVManagerApp();

        mDebugPreview = findViewById(R.id.debugPreview);
        mDebugPrint = findViewById(R.id.debugPrint);

        mInputPreviewView = findViewById(R.id.inputPreviewView);
        mBackgroundView = findViewById(R.id.background);
        mMatchPreviewView = findViewById(R.id.matchPreviewView);
        mCoverView = findViewById(R.id.coverView);
        mTitleView = findViewById(R.id.titile_label);
        mAuthorView = findViewById(R.id.author_label);
        mPageLabelView = findViewById(R.id.page_labal);
        mController = findViewById(R.id.controller_view);

        mPackageId = getIntent().getStringExtra("package");
        mTitleView.setText(getIntent().getStringExtra("title"));
        mAuthorView.setText(getIntent().getStringExtra("author"));
        mImageCount = getIntent().getIntExtra("image_count", -1);
        mAudioIndex = getIntent().getIntExtra("audio_index", -1);
        mTrackTiming = getIntent().getDoubleArrayExtra("track_timing");

        mCoverView.setImageBitmap(LoadUtil.getBitmapFromUrlWithCache(this,
                getResources().getString(R.string.root_url) + mPackageId + "/" + LauncherActivity.COVER_PATH));

        String imagePath = LoadUtil.getPackagePath(this, mPackageId) + "set/";
        // TODO: これ重いから非同期にしたい
        mLearndImageSet = new LearndImageSet(this, imagePath, mImageCount);
        mLearndImageSet.setImageReduction(2);
        mMatchPreviewView.setSet(mLearndImageSet);

        try {
            String audioPath = LoadUtil.getPackagePath(this, mPackageId) + "audio/" + mAudioIndex + ".mp3";
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(audioPath);
            mMediaPlayer.prepare();
            mMediaPlayer.setLooping(false);
        }catch (IOException e){
            mMediaPlayer.release();
            mMediaPlayer = null;
            Toast.makeText(this, "Failed load audio source", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // OpenCamera when ready surface.
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
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if(null != mImageBufferForProcessing)
            mImageBufferForProcessing.close();
        if(null != mCaptureSession)
            mCaptureSession.close();
        if(null != mCameraDevice)
            mCameraDevice.close();
        super.onPause();
        finish();
    }

    @Override
    protected void onStop() {
        if(mLearndImageSet != null)
            mLearndImageSet.release();
        if(mMediaPlayer != null)
            mMediaPlayer.release();
        super.onStop();
    }


    public void onClickBack(View v){
        onPause();
    }

    public void oClickVolumeButton(View v){ }

    public void onClickSensitiveButton(View v){
        Log.d("Button", "tapped");
        System.gc();
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
            Toast.makeText(this, "Cannot recognaize camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadOpenCVManagerApp(){
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);
            }
        });
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
           Toast.makeText(this, "This isn't supported camera.", Toast.LENGTH_SHORT).show();
            return new Point();
        }
    }

    private void setDisplayDebugMessage(String key, String message){
        mDisplayDebugMessageList.put(key, message);
        String joindMessage = "";
        for(Map.Entry<String, String> entry : mDisplayDebugMessageList.entrySet()){
            joindMessage += entry.getKey() + ": " + entry.getValue() + "\n";
        }
        mDebugPrint.setText(joindMessage);
    }

    private String getActivityOrientation() {
        switch (getResources().getConfiguration().orientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                return "ORIENTATION_LANDSCAPE";
            case Configuration.ORIENTATION_PORTRAIT:
                return "ORIENTATION_PORTRAIT";
            case Configuration.ORIENTATION_SQUARE:
                return "ORIENTATION_SQUARE";
            case Configuration.ORIENTATION_UNDEFINED:
            default: {
                return "ORIENTATION_UNDEFINED";
            }
        }
    }

    private int getDisplayOrientation() {
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int rotation = windowManager.getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                Toast.makeText(this, "ROTATION_UNDEFINED", Toast.LENGTH_SHORT).show();
                return 90;
        }
    }

    private int getCameraOrientation() {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(mCameraID);
            Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            return sensorOrientation;
        } catch (CameraAccessException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return 90;
    }
}
