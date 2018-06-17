package wallstudio.work.kamishiba;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.Log;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class TabFragment extends Fragment {

    public static final String CLOUD_PACKAGE_SUMMARY_LIST_PATH = "packages.yml";
    public static final String LOCAL_PACKAGE_SUMMATY_LIST_PATH = "local_packages.yml";

    public abstract String getTitle();

    protected void startLauncher(String id, boolean isDownloaded){
        Intent intent = new Intent(((Activity) (getContext())).getApplication(), LauncherActivity.class);
        intent.putExtra("package_id", id);
        intent.putExtra("download_status", isDownloaded);
        getActivity().startActivity(intent);
    }
}
