package wallstudio.work.kamishiba;

import android.widget.GridView;
import android.support.v4.app.Fragment;

public class LoacalLibraryTabFragment extends LibraryTabFragment{

    public static final String TITLE = "ライブラリ ＞ ローカル";

    @Override
    protected void setAdapter(GridView gridView) {
        // アクセスできないと固まってしまうので…
        LoadUtil.CloudPackageListDownloadTask task
                = new LoadUtil.CloudPackageListDownloadTask(getContext(), gridView);
        task.execute(getContext().getCacheDir() + LOCAL_PACKAGE_SUMMATY_LIST_DIR);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser && getActivity() != null)
            getActivity().setTitle(TITLE);
    }

    @Override
    public String getTitle() {
        return TITLE;
    }
}
