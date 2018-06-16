package wallstudio.work.kamishiba;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class LauncherActivity extends AppCompatActivity {

    private String packageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        packageId = getIntent().getStringExtra("package_id");
        setTitle(packageId);

    }
}
