package wallstudio.work.kamishiba;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.preference.PreferenceManager;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StandCameraActivity extends Activity {

    public static final Point DIAL_INPUT_IMAGE_SIZE = new Point(800, 480);
    // public static final Point DIAL_INPUT_IMAGE_SIZE = new Point(1280, 720);
    public static final Bitmap.Config BUFFER_BITMAP_FORMAT = Bitmap.Config.ARGB_8888;
    private static final int SMOOTH_BUFFER_SIZE = 4;


    private String mPackageId;
    public String getmPackageId(){ return mPackageId; }
    private int mImageCount;
    private int mAudioPosition;
    private double[][] mTrackTiming;

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
    private TrainingDataList mTrainingDataList;
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
    protected boolean mIsLandscape = true;
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        private Mat mOriginal = new Mat();
        private int mCameraOrientation = -1;
        private long counter = 0;

        @Override
        public void onImageAvailable(ImageReader reader) {
            if (isFinishing()) return;

            Image image = reader.acquireNextImage();
            if (image == null) return;

            if(counter++ == 10) {
                Jni.imageDump(StandCameraActivity.this, image, mOriginalBitmap);
                Toast.makeText(StandCameraActivity.this,
                        "ログを作成しました。メールで/Document/kamishiba_dev/内のファイルをyukawallstudio@gmail.comまで送信してください",
                        Toast.LENGTH_LONG).show();
            }
            // ImageReader から Mat に変換
            Jni.image2Bitmap(image, mOriginalBitmap, true);
            Utils.bitmapToMat(mOriginalBitmap, mOriginal, false);

            // 画像の向きを補正
            int displayOrientation = getDisplayOrientation();
            if (mCameraOrientation == -1)
                mCameraOrientation = getCameraOrientation();
            if (mIsLandscape) {
                // 内面カメラなので D = -C なら向きが一致する
                if (Math.abs(360 - mCameraOrientation) != displayOrientation) {
                    Core.flip(mOriginal, mOriginal, 0);
                } else {
                    Core.flip(mOriginal, mOriginal, 1);
                }
            } else {
                if (mCameraOrientation == 90){
                    Core.flip(mOriginal, mOriginal, 0);
                } else {
                    Core.flip(mOriginal, mOriginal, 1);
                }
                Core.transpose(mOriginal, mOriginal);
            }

            // 画像処理の呼び出し
            mBackgroundView.convert(mOriginal, mController.vanishingRatio, mController.pageEdgeY, mIsLandscape);
            mInputPreviewView.convert(mOriginal, mController.vanishingRatio, mController.pageEdgeY, mIsLandscape);
            // AKAZE抽出は重いので，非同期
            boolean isExcused = mMatchPreviewView.convertAsync(mOriginal, mController.vanishingRatio, mController.pageEdgeY, mIsLandscape);
            if(isExcused){
                // 音声
                try {
                    // チラつきの平滑化
                    mCurrentPage = smooth(mMatchPreviewView.page);
                    // 再生
                    playCatch(mCurrentPage);
                    // 音声止める
                    stopCatch();
                }catch (Exception e){
                    Toast.makeText(StandCameraActivity.this, "音声の再生エラー " + mCurrentPage, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
            mPageLabelView.setText((mCurrentPage >= 0 ? mCurrentPage + 1 : "-")  + "/" + mImageCount);
        }

        private List<Integer> mBufferForSmooth = new ArrayList<>();

        private int smooth(int value) {
            // リングバッファ
            mBufferForSmooth.add(value);
            while (mBufferForSmooth.size() > SMOOTH_BUFFER_SIZE)
                mBufferForSmooth.remove(0);
            int ret = -1;
            if (mBufferForSmooth.size() == SMOOTH_BUFFER_SIZE) {

                // 3個以上のを吐き出す
                for(int i = 0; i < mTrackTiming.length; i++) {
                    int count = 0;
                    for(Integer page: mBufferForSmooth){
                        if(i == page) count++;
                    }
                    if(count >= 3){
                        ret = i;
                        break;
                    }
                }
            }
            Log.d("SmoothPage", String.format("smooth=%d (%s), ", ret, mBufferForSmooth.toString()));
            setDisplayDebugMessage("SmoothPage", String.format("smooth=%d (%s), ", ret, mBufferForSmooth.toString()));
            setDisplayDebugMessage("Similarity", String.format("[%.2f, %.2f]", mMatchPreviewView.similarity[0],  mMatchPreviewView.similarity[1]));
            return ret;
        }

        private int mPreCount = -1;
        private void playCatch(int count) {
            if (mPreCount != count && 0 <= count && count < mTrackTiming.length) {
                // 再生中のを止める
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                // シークして再生
                mMediaPlayer.seekTo((int) (mTrackTiming[count][0] * 1000));
                mMediaPlayer.start();
            }
            mPreCount = count;
        }

        private void stopCatch(){
            if (mMediaPlayer.isPlaying() && mCurrentPage >= 0) {
                int end = (int) (mTrackTiming[mCurrentPage][1] * 1000);
                int now = mMediaPlayer.getCurrentPosition();
                if (end < now) {
                    mMediaPlayer.pause();
                }
            }
        }
    };


    protected int mLayout = R.layout.activity_stand_camera;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(mLayout);
        // Fullscreen
        if(mIsLandscape) {
            this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }else{

        }
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
        mAudioPosition = getIntent().getIntExtra("audio", -1);
        try {
            Map packageData = (Map) LoadUtil.getYamlFromPath(getFilesDir() + "/" + mPackageId + "/" + LoadUtil.LOCAL_PACKAGE_FILENAME);
            mImageCount = (int)packageData.get("page_count");
            mTitleView.setText((String) packageData.get("title"));
            mAuthorView.setText((String)packageData.get("author"));
            mCoverView.setImageBitmap(LoadUtil.getBitmapFromPath(getFilesDir() + "/" + mPackageId + "/000.jpg"));
            Map audioData = ((List<Map>)packageData.get("audio")).get(mAudioPosition);
            List<Number> trackTimingSQ = (List<Number>) audioData.get("track_timing");
            mTrackTiming = perseTiming(trackTimingSQ);

            // TODO: これ重いから非同期にしたい
            mTrainingDataList = new TrainingDataList(getFilesDir() + "/" + mPackageId);
            mMatchPreviewView.mTrainingDataList = mTrainingDataList;

            // 音声
            try {
                String audioPath = getFilesDir() + "/" + mPackageId + "/" + audioData.get("id") + ".mp3";
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDataSource(audioPath);
                mMediaPlayer.prepare();
                mMediaPlayer.setLooping(false);
            }catch (IOException e){
                mMediaPlayer.release();
                mMediaPlayer = null;
                Toast.makeText(this, "Failed load audio source", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) { }
    }

    protected double[][] perseTiming(List<Number> trackTimingSQ){
        double[][] trackTiming = new double[trackTimingSQ.size() / 2][];
        for (int i = 0; i < trackTimingSQ.size() / 2; i++)
            trackTiming[i] = new double[]{trackTimingSQ.get(i * 2).doubleValue(), trackTimingSQ.get(i * 2 + 1).doubleValue()};

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isJoin = sharedPreferences.getBoolean("join2page", false);
        if(isJoin) {
            for(int i = 0; i < trackTiming.length/2; i++){
                double[] even = trackTiming[i*2];
                double[] odd = trackTiming[i*2 + 1];
                even[1] = odd[1];
                odd[0] = even[0];
            }
        }
        return trackTiming;
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

        if(null != mCaptureSession)
            mCaptureSession.close();
        if(null != mCameraDevice)
            mCameraDevice.close();
        if(null != mImageBufferForProcessing)
            mImageBufferForProcessing.close();
        super.onPause();
        finish();
    }

    @Override
    protected void onStop() {
        if(mTrainingDataList != null)
            mTrainingDataList.release();
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

    protected int mCameraSide = CameraCharacteristics.LENS_FACING_BACK;
    @SuppressLint("MissingPermission")
    private void openCamera() throws CameraAccessException {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if(mCameraManager != null && mCameraManager.getCameraIdList().length > 0) {
            mCameraID = mCameraManager.getCameraIdList()[mCameraSide];
            StreamConfigurationMap map = mCameraManager.getCameraCharacteristics(mCameraID).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                mSupportPreviewSize = map.getOutputSizes(SurfaceTexture.class);
                // Detaile config (buffers and displays)
                mCameraManager.openCamera(mCameraID, mDeviceSettingCallback, null);
                return;
            }
        }
        Toast.makeText(this, "Cannot recognaize camera", Toast.LENGTH_SHORT).show();
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
