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
        public List<Map> yaml;
        private HashMap<Integer, AsyncTask> mImageLoadHolder = new HashMap<>();

        public PackageGridAdapter(@NonNull Context context, int resource, @NonNull List<Map> yaml) {
            super(context, resource, yaml);

            mContext = context;
            mResource = resource;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.yaml = yaml;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
                if(mImageLoadHolder.containsKey(view.hashCode()))
                    mImageLoadHolder.get(view.hashCode()).cancel(true);
            } else {
                view = mInflater.inflate(mResource, null);
            }

            // Bind view
            ImageView thumnail = view.findViewById(R.id.thumbnail);
            TextView title = view.findViewById(R.id.pack_title);
            TextView autor = view.findViewById(R.id.pack_autor);
            TextView detail = view.findViewById(R.id.pack_audio_count);
            TextView pageCount = view.findViewById(R.id.pac_pcount);
            TextView audioCount = view.findViewById(R.id.pac_acount);
            ViewGroup downloadStatus = view.findViewById(R.id.download_status);
            // Initialize contents
            Drawable preImage = thumnail.getDrawable();
            if(preImage != null && preImage instanceof BitmapDrawable)
                ((BitmapDrawable) preImage).getBitmap().recycle();
            thumnail.setImageResource(R.drawable.ic_local_library_black_100dp);
            title.setText("-");
            autor.setText("-");
            detail.setText("-");
            pageCount.setText("-");
            audioCount.setText("-");
            downloadStatus.setVisibility(View.INVISIBLE);
            // Set contents
            LoadUtil.PackageSummaryDownloadTask imageTask
                    = new LoadUtil.PackageSummaryDownloadTask(getContext(), thumnail);
            imageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                     getContext().getResources().getString(R.string.root_url)
                             + yaml.get(position).get("id") + "/thumbnail.jpg");
            title.setText((String) yaml.get(position).get("title"));
            autor.setText((String) yaml.get(position).get("author"));
//            mDetail.setText((String)mYaml.get(position).get("page_count"));
            pageCount.setText(String.valueOf((int)(yaml.get(position).get("page_count"))));
            audioCount.setText(String.valueOf((int)(yaml.get(position).get("audio_count"))));
            downloadStatus.setVisibility(
                    ((boolean)(yaml.get(position).get("download_status"))) ? View.VISIBLE: View.INVISIBLE);
            // for canceling override task
            mImageLoadHolder.put(view.hashCode(), imageTask);


            return view;
        }
    }

    GridView mGrid;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_library_tab, container, false);
        mGrid = v.findViewById(R.id.grid);
        setAdapter(mGrid);

        mGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PackageGridAdapter adapter = (PackageGridAdapter) parent.getAdapter();
                String pacId = (String) adapter.yaml.get(position).get("id");
                startLauncher(pacId, (boolean)adapter.yaml.get(position).get("download_status"));
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        setAdapter(mGrid);
    }

    protected abstract void setAdapter(GridView gridView);
}
