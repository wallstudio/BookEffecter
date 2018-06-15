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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;


public class LibraryTabFragment extends Fragment {

    public static final String PACKAGE_ROOT_URL = "http://192.168.0.10/smb/kamishiba/package/";
    public static final String PACKAGE_SUMMARY_LIST_URL = "http://192.168.0.10/smb/kamishiba/books.list";

    public static class PackageGridAdapter extends ArrayAdapter<String> {

        private Context mContext;
        private int mResource;
        private LayoutInflater mInflater;
        private List<String> mPacs;
        private HashMap<Integer, AsyncTask> mImageLoadHolder = new HashMap<>();
        private Bitmap mBlankBitmap;

        public PackageGridAdapter(@NonNull Context context, int resource, @NonNull List<String> pacs) {
            super(context, resource, pacs);

            mContext = context;
            mResource = resource;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mPacs = pacs;
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

            ((TextView)(view.findViewById(R.id.pack_title))).setText(mPacs.get(position));

            ImageView imageView = view.findViewById(R.id.thumbnail);
            // Recycle
            Drawable preImage = imageView.getDrawable();
            if(preImage != null && preImage instanceof BitmapDrawable)
                ((BitmapDrawable) preImage).getBitmap().recycle();
            imageView.setImageResource(R.drawable.ic_local_library_black_100dp);
            LoadUtil.bitmapDownloadWithCacheTask task = new LoadUtil.bitmapDownloadWithCacheTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    getContext(), imageView, PACKAGE_ROOT_URL + mPacs.get(position) + "/set/0.jpg");

            TextView title = view.findViewById(R.id.pack_title);
            TextView author = view.findViewById(R.id.pack_autor);
            TextView ditail = view.findViewById(R.id.pack_audio_count);
            LoadUtil.SummaryDownloadTask task1 = new LoadUtil.SummaryDownloadTask();
            task1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    getContext(), title, author, ditail, PACKAGE_ROOT_URL + mPacs.get(position) + "/index.yml");
            mImageLoadHolder.put(view.hashCode(), task);

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
        LoadUtil.CloudPackageListDownloadTask task = new LoadUtil.CloudPackageListDownloadTask();
        task.execute(getContext(), v.findViewById(R.id.grid), PACKAGE_SUMMARY_LIST_URL);
        return v;
    }
}
