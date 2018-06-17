package wallstudio.work.kamishiba;

import android.widget.GridView;

public class LoacalLibraryTabFragment extends LibraryTabFragment{

    public static final String TITLE = "ライブラリ ＞ ローカル";

    @Override
    protected void setAdapter(GridView gridView) {
        // アクセスできないと固まってしまうので…
        LoadUtil.LocalPackageListLoadTask task
                = new LoadUtil.LocalPackageListLoadTask(getContext(), gridView);
        task.execute(getContext().getFilesDir() + "/" + LOCAL_PACKAGE_SUMMATY_LIST_PATH);
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
