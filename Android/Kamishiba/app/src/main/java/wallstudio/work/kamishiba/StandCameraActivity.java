package wallstudio.work.kamishiba;

import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.widget.ImageView;
import android.widget.TextView;

public class StandCameraActivity extends AppCompatActivity {

    public static final Size INPUT_IMAGE_SIZE = new Size(1280, 720);

    private ImageView mInputPreviewView;
    private ImageView mMatchPreviewView;
    private ImageView mCoverView;
    private TextView mPageLabelView;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_standed_camera);

        mInputPreviewView = findViewById(R.id.inputPreviewView);
        mMatchPreviewView = findViewById(R.id.matchPreviewView);
        mCoverView = findViewById(R.id.coverView);
        mPageLabelView = findViewById(R.id.pageLabelView);

        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
