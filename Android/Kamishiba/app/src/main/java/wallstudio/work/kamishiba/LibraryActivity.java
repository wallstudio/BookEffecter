package wallstudio.work.kamishiba;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.util.Map;

public class LibraryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liblary);
    }

    public void onClick_DL(View v){
        SetDownloadTask setDownloadTask = new SetDownloadTask();
        setDownloadTask.execute("http://192.168.0.10/smb/kamishiba/uphashi/kadamaki/index.yml");
    }

    public void onClick_CAM(View v){
        Intent intent = new Intent(getApplicationContext(), StandCameraActivity.class);
        intent.putExtra("audio", 0);
        intent.putExtra("yaml", "");
        startActivity(intent);
    }
}
