package wallstudio.work.cameraprocessing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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
                        int previewWidth = 1280;
                        int previewHeight = 720;
                        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                        Matrix matrix = new Matrix();
                        RectF viewRect = new RectF(0, 0, mTextureView.getWidth(), mTextureView.getHeight());
                        RectF bufferRect = new RectF(0, 0, previewHeight, previewWidth);
                        PointF center = new PointF(viewRect.centerX(), viewRect.centerY());
                        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                            bufferRect.offset(center.x - bufferRect.centerX(), center.y - bufferRect.centerY());
                            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                            float scale = Math.max(
                                    (float) mTextureView.getHeight() / previewHeight,
                                    (float) mTextureView.getWidth() / previewHeight);
                            matrix.postScale(scale, scale, center.x, center.y);
                            matrix.postRotate(90 * (rotation - 2), center.x, center.y);
                        } else if (Surface.ROTATION_180 == rotation) {
                            matrix.postRotate(180, center.x, center.y);
                        }
                        mTextureView.setTransform(matrix);
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                        return false;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                        Bitmap bitmap = mTextureView.getBitmap();
                        int w = bitmap.getWidth();
                        int h = bitmap.getHeight();
                        Mat frame = new Mat();
                        Utils.bitmapToMat(bitmap, frame, false);

                        Point vanising = new Point(0.5, 0.2);
                        double pageAearRatio = 0.65;
                        CorrectedImage correctedImage = new CorrectedImage(frame, vanising, pageAearRatio, true, 256);
                        LearndImage learndImage = new LearndImage(correctedImage.resultImage);
                        frame = learndImage.image;

                        bitmap = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(frame, bitmap,false);
                        mImageView.setImageBitmap(bitmap);
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
        String cameraId = mCameraManager.getCameraIdList()[1];
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

    Activity getActivity(){
        return  this;
    }
}
