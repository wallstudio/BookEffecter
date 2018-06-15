package wallstudio.work.kamishiba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LibraryTabFragment extends Fragment {

    public static final String PACKAGE_ROOT_URL = "http://192.168.0.10/smb/kamishiba/package/";
    public static final String PACKAGE_SUMMARY_LIST_URL = "http://192.168.0.10/smb/kamishiba/books.yml";

    public static class PackageGridAdapter extends ArrayAdapter<Map> {

        private Context mContext;
        private int mResource;
        private LayoutInflater mInflater;
        private List<Map> mYaml;
        private HashMap<Integer, AsyncTask> mImageLoadHolder = new HashMap<>();
        private Bitmap mBlankBitmap;
        private ImageView mThumbnail;
        private TextView mTitle;
        private TextView mAuthor;
        private TextView mDetail;

        public PackageGridAdapter(@NonNull Context context, int resource, @NonNull List<Map> yaml) {
            super(context, resource, yaml);

            mContext = context;
            mResource = resource;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mYaml = yaml;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
                if(mImageLoadHolder.containsKey(view.hashCode()))
                    mImageLoadHolder.get(view.hashCode()).cancel(true);
            } else {
                view = mInflater.inflate(mResource, null);
            }

            // Bind view
            mThumbnail = view.findViewById(R.id.thumbnail);
            mTitle = view.findViewById(R.id.pack_title);
            mAuthor = view.findViewById(R.id.pack_autor);
            mDetail = view.findViewById(R.id.pack_audio_count);
            // Initialize contents
            Drawable preImage = mThumbnail.getDrawable();
            if(preImage != null && preImage instanceof BitmapDrawable)
                ((BitmapDrawable) preImage).getBitmap().recycle();
            mThumbnail.setImageResource(R.drawable.ic_local_library_black_100dp);
            mTitle.setText("TITLE");
            mAuthor.setText("AUTHOR");
            mDetail.setText("DETAIL");
            // Set contents
            LoadUtil.PackageSummaryDownloadTask imageTask
                    = new LoadUtil.PackageSummaryDownloadTask(getContext(), mThumbnail);
            imageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    PACKAGE_ROOT_URL + mYaml.get(position).get("id") + "/set/0.jpg");
            mTitle.setText((String)mYaml.get(position).get("title"));
            mAuthor.setText((String)mYaml.get(position).get("author"));
            //mDetail.setText((String)mYaml.get(position).get("page_count"));
            mImageLoadHolder.put(view.hashCode(), imageTask);

            return view;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // アクセスできないと固まってしまうので…
        View v = inflater.inflate(R.layout.fragment_library_tab, container, false);
        LoadUtil.CloudPackageListDownloadTask task
                = new LoadUtil.CloudPackageListDownloadTask(getContext(), (GridView) v.findViewById(R.id.grid));
        task.execute(PACKAGE_SUMMARY_LIST_URL);
        return v;
    }
}
