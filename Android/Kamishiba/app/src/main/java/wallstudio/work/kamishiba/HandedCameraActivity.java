package wallstudio.work.kamishiba;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;

public class HandedCameraActivity extends StandCameraActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        mLayout = R.layout.activity_handed_camera;
        mIsLandscape = false;
        mCameraSide = CameraCharacteristics.LENS_FACING_FRONT;

        super.onCreate(savedInstanceState);
    }
}
