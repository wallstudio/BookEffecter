package wallstudio.work.kamishiba;

import android.widget.GridView;

public class CloudLibraryTabFragment extends LibraryTabFragment {

    public static final String TITLE = "ライブラリ ＞ クラウド";
    @Override public String getTitle() {
        return TITLE;
    }

    @Override
    protected void setAdapter(GridView gridView) {
        // アクセスできないと固まってしまうので…
        LoadUtil.CloudPackageListDownloadTask task
                = new LoadUtil.CloudPackageListDownloadTask(getContext(), gridView, getSearchText());
        task.execute();
    }
}
