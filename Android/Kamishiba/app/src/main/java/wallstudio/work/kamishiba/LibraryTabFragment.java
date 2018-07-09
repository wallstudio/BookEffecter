package wallstudio.work.kamishiba;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class LibraryTabFragment extends TabFragment {

    public static class PackageGridAdapter extends ArrayAdapter<Map> {

        private Context mContext;
        private int mResource;
        private LayoutInflater mInflater;
        public List<Map> mPackagesData;
        private HashMap<Integer, AsyncTask> mImageLoadHolder = new HashMap<>();

        public PackageGridAdapter(@NonNull Context context, int resource, @NonNull List<Map> packagesData) {
            super(context, resource, packagesData);

            mContext = context;
            mResource = resource;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mPackagesData = packagesData;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View newView;
            if (convertView != null) {
                newView = convertView;
                if(mImageLoadHolder.containsKey(newView.hashCode()))
                    mImageLoadHolder.get(newView.hashCode()).cancel(true);
            } else {
                newView = mInflater.inflate(mResource, null);
            }

            // Bind view
            ImageView thumbnail = newView.findViewById(R.id.thumbnail);
            TextView title = newView.findViewById(R.id.pack_title);
            TextView author = newView.findViewById(R.id.pack_autor);
            TextView detail = newView.findViewById(R.id.pack_audio_count);
            TextView pageCount = newView.findViewById(R.id.pac_pcount);
            TextView audioCount = newView.findViewById(R.id.pac_acount);
            ViewGroup downloadStatus = newView.findViewById(R.id.download_status);
            // Initialize contents
            Drawable preImage = thumbnail.getDrawable();
            if(preImage != null && preImage instanceof BitmapDrawable)
                ((BitmapDrawable) preImage).getBitmap().recycle();
            thumbnail.setImageResource(R.drawable.ic_local_library_black_100dp);
            title.setText("-");
            author.setText("-");
            detail.setText("-");
            pageCount.setText("-");
            audioCount.setText("-");
            downloadStatus.setVisibility(View.INVISIBLE);

            Map packageData = mPackagesData.get(position);
            // Set contents
            LoadUtil.ImageDownloadAndShowTask imageTask
                    = new LoadUtil.ImageDownloadAndShowTask(getContext(), thumbnail,
                    LoadUtil.REMOTE_DATA_URL + "/" + packageData.get("id") + "/0.jpg");
            imageTask.execute();
            title.setText((String) packageData.get("title"));
            author.setText((String) packageData.get("author"));
            pageCount.setText(String.valueOf((int)(packageData.get("page_count"))));
            audioCount.setText(String.valueOf((int)(packageData.get("audio_count"))));
            downloadStatus.setVisibility(
                    ((boolean)(packageData.get("download_status"))) ? View.VISIBLE: View.INVISIBLE);
            // for canceling override task
            mImageLoadHolder.put(newView.hashCode(), imageTask);


            return newView;
        }
    }

    GridView mGrid;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_library_tab, container, false);

        // リストの中身を設定
        mGrid = v.findViewById(R.id.grid);
        setAdapter(mGrid);
        mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PackageGridAdapter adapter = (PackageGridAdapter) parent.getAdapter();
                String pacId = (String) adapter.mPackagesData.get(position).get("id");
                startLauncher(pacId);
            }
        });

        return v;
    }

    // 他Activityやバックグラウンドから復帰したら再ロードする
    @Override
    public void onResume() {
        super.onResume();
        setAdapter(mGrid);
    }

    protected abstract void setAdapter(GridView gridView);
}
