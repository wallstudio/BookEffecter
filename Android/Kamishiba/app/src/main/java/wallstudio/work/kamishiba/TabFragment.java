package wallstudio.work.kamishiba;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

import static android.content.Intent.FLAG_ACTIVITY_NO_HISTORY;

public abstract class TabFragment extends Fragment {

    public static final int REQUEST_CODE = 1000;

    public abstract String getTitle();

    protected void startLauncher(String id){
        Intent intent = new Intent(((Activity) (getContext())).getApplication(), LauncherActivity.class);
        intent.putExtra("package_id", id);
        intent.setFlags(FLAG_ACTIVITY_NO_HISTORY);
        getActivity().startActivityForResult(intent, REQUEST_CODE);
    }
}
