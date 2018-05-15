package wallstudio.work.cameraprocessing;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextureView mTextureView;
    private Surface mPreviewSurface;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;

    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                        Bitmap bitmap = mTextureView.getBitmap();
                        int[] buffer = new int[bitmap.getWidth() * bitmap.getHeight()];
                        for (int j = 0; j < bitmap.getHeight(); j++){
                            for(int i = 0; i < bitmap.getWidth(); i++){
                                int pixel = bitmap.getPixel(i, j);
                                int gray = (int)(0.299 * Color.red(pixel) + 0.587 *  Color.green(pixel) + 0.114 * Color.blue(pixel));
                                gray = gray < 63 ? 10 : 244;
                                buffer[j * bitmap.getWidth() + i] = Color.argb(
                                        Color.alpha(pixel),
                                        gray,
                                        gray,
                                        gray);
                            }
                        }
                        bitmap.setPixels(buffer,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
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
