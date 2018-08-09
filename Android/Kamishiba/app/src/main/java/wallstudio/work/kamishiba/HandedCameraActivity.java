package wallstudio.work.kamishiba;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;

import java.util.List;

public class HandedCameraActivity extends StandCameraActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState){
        mLayout = R.layout.activity_handed_camera;
        mIsLandscape = false;
        mCameraSide = CameraCharacteristics.LENS_FACING_FRONT;

        super.onCreate(savedInstanceState);
    }

    @Override
    protected double[][] perseTiming(List<Number> trackTimingSQ){
        double[][] trackTiming = new double[trackTimingSQ.size() / 2][];
        for(int i = 0; i < trackTimingSQ.size() / 2; i++)
            trackTiming[i] = new double[]{trackTimingSQ.get(i * 2).doubleValue(), trackTimingSQ.get(i * 2 + 1).doubleValue()};
        return trackTiming;
    }
}
