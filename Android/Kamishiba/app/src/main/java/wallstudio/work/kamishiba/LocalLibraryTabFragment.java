package wallstudio.work.kamishiba;

import android.widget.GridView;

public class LocalLibraryTabFragment extends LibraryTabFragment{

    public static final String TITLE = "ライブラリ ＞ ローカル";
    @Override public String getTitle() {
        return TITLE;
    }

    // リストの中身を設定
    @Override
    protected void setAdapter(GridView gridView) {
        // アクセスできないと固まってしまうので…
        LoadUtil.LocalPackageListLoadTask task
                = new LoadUtil.LocalPackageListLoadTask(getContext(), gridView);
        task.execute();
    }
}
