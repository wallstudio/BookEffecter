package wallstudio.work.cameraprocessing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextureView mTextureView;
    private Surface mPreviewSurface;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;

    private ImageView mImageView;
    private TextView mTextView;
    private MediaPlayer mMediaPlayer;

    void PlaySound(final String url, final boolean isInternet){
        if(mMediaPlayer.isPlaying())
            mMediaPlayer.stop();
        mMediaPlayer.release();
        mMediaPlayer = new MediaPlayer();
        try {
            AssetFileDescriptor afd = getAssets().openFd(url);
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            Log.d("SOUND", "Play start \""+ url + "\"");
        }catch (IOException e){
            Log.e("ERROR", "Cann't load audio file.");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.textView);
        mTextView.setText("Initialize");
        mMediaPlayer = new MediaPlayer();
    }

    static {
        // ネイティブライブラリ読み込み
        System.loadLibrary("opencv_java3");       
        
        // OpenCV Managerつかうならこっち
        //
        // OpenCVLoader.initDebug()で切り分けるのがお行儀良い
        //OpenCVLoader.initAsync(
        //        OpenCVLoader.OPENCV_VERSION_3_0_0,
        //        this, new BaseLoaderCallback(this) {
        // @Override
        // public void onManagerConnected(int status) {
        //     if (status == LoaderCallbackInterface.SUCCESS)
        //         mCameraView.enableView();
        //     else
        //         super.onManagerConnected(status);
        // });
    }

    @Override
    protected void onResume() {
        super.onResume();
        

        mTextureView = findViewById(R.id.view_texture);
        mImageView = findViewById(R.id.image_view);
        try {
            if (mTextureView.isAvailable()) {
                openCamera();
            } else {
                mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                        try {
                            openCamera();
                        } catch (CameraAccessException ca) {
                            Log.e("ERROR", ca.toString());
                        }
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                        Log.d("",mTextureView.getWidth() +"x"+mTextureView.getHeight() + " "+mTextureView.getBitmap().isMutable());
                        HoleParser holeParser = new HoleParser(mTextureView.getBitmap(), new HoleParser.HoleParserCallBack() {
                            @Override
                            public void playSound(String url) {
                                PlaySound(url, false);
                            }

                            @Override
                            public void refleshText(String message) {
                                mTextView.setText(message);
                            }
                        });
                        mImageView.setImageBitmap(holeParser.getProcessedBitmap());
                    }
                });
            }
        } catch (CameraAccessException ca) {
            Log.e("ERROR", ca.toString());
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() throws CameraAccessException {
        // カメラIDを取得
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String cameraId = mCameraManager.getCameraIdList()[0];
        // カメラ起動
        mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {

            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                texture.setDefaultBufferSize(360,360);
                mPreviewSurface = new Surface(texture);

                mCameraDevice = cameraDevice;
                try {
                    mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface),
                            new CameraCaptureSession.StateCallback() {
                                @Override
                                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                    mCaptureSession = cameraCaptureSession;
                                    CaptureRequest.Builder builder = null;
                                    try {
                                        builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                        builder.addTarget(mPreviewSurface);
                                        builder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
                                        CaptureRequest l = builder.build();
                                        mCaptureSession.setRepeatingRequest(builder.build(), null,null);
                                    } catch (CameraAccessException e) {
                                        Log.e("ERROR",e.toString());
                                    }
                                }

                                @Override
                                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                                }
                            },null);


                } catch (CameraAccessException e) {
                    Log.e("ERROR",e.toString());
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {

            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int i) {

            }
        }, null);


    }
}
