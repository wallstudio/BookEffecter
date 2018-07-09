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

    public boolean isUserVisible = false;

    public abstract String getTitle();

    protected void startLauncher(String id){
        Intent intent = new Intent(((Activity) (getContext())).getApplication(), LauncherActivity.class);
        intent.putExtra(LauncherActivity.INTENT_KEY_PACKAGE_ID, id);
        getActivity().startActivity(intent);
    }

    // タブの切り替えに対応してアクションバーにタイトルを表示
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        isUserVisible = isVisibleToUser;
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser && getActivity() != null)
            getActivity().setTitle(getTitle());
    }
}
