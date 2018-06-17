package wallstudio.work.kamishiba;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;
import java.util.Map;


public class LauncherActivity extends AppCompatActivity {

    public static final String PACKAGE_DETAIL_PATH = "detail.yml";
    public static final String COVER_PATH = "cover.jpg";

    private String packageId;
    private Switch mCameraSwitch;
    private Button mDownloadOrUpdate;
    private Button mRemove;
    private ListView mListView;
    public Map yaml;
    public boolean isDownloaded = false;
    public boolean isUpdateable = false;

    private AdapterView.OnItemClickListener mOnItemClickListener =  new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            boolean isFront = mCameraSwitch.isChecked();
            Intent intent = new Intent();
            intent.putExtra("is_front",isFront);
            intent.putExtra("audio_index", position);
            intent.putExtra("package", packageId);
            Log.d("StartCamera", "is_front=" + isFront + ", audio_index=" + position + ", package=" + packageId);
            //startActivity(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        packageId = getIntent().getStringExtra("package_id");
        setTitle(packageId);
        setPackageInfo();

        mDownloadOrUpdate = findViewById(R.id.lnc_download_btn);
        mRemove = findViewById(R.id.lnc_remove_btn);
        setUIStatus();
    }

    private void setPackageInfo(){
        mListView = findViewById(R.id.lnc_audio_list);

        LoadUtil.PackageDetailDownloadTask task = new LoadUtil.PackageDetailDownloadTask(
              this, mListView,
                (ImageView) findViewById(R.id.lnc_book_cover),
                (TextView) findViewById(R.id.lnc_title),
                (TextView) findViewById(R.id.lnc_author),
                (TextView) findViewById(R.id.lnc_tag),
                (TextView) findViewById(R.id.lnc_description),
                (TextView) findViewById(R.id.lnc_page_count));
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                getResources().getString(R.string.root_url) + packageId + "/" + PACKAGE_DETAIL_PATH,
                getResources().getString(R.string.root_url) + packageId + "/" + COVER_PATH);

        mCameraSwitch = findViewById(R.id.lnc_camera_dirction_switch);
    }

    public void setUIStatus(){
        if(!isDownloaded){
            mDownloadOrUpdate.setVisibility(View.VISIBLE);
            mDownloadOrUpdate.setText("ダウンロード");
            mRemove.setVisibility(View.GONE);
            mListView.setOnItemClickListener(null);
        }else if(isDownloaded && !isUpdateable){
            mDownloadOrUpdate.setVisibility(View.GONE);
            mRemove.setVisibility(View.VISIBLE);
            mListView.setOnItemClickListener(mOnItemClickListener);
        }else {
            mDownloadOrUpdate.setVisibility(View.VISIBLE);
            mDownloadOrUpdate.setText("更新");
            mRemove.setVisibility(View.VISIBLE);
            mListView.setOnItemClickListener(mOnItemClickListener);
        }
    }

    private LoadUtil.PackageDataDownloadTask mTask;

    public void onClickDownloadOrUpdateButton(View view){
        Log.d("Launcher", "DOWNLOAD " + packageId);
        if(yaml != null) {
            String url = "";
            int imageCount = (int) yaml.get("page_count");
            int audioCount = (int) yaml.get("audio_count");
            mTask = new LoadUtil.PackageDataDownloadTask(this,
                    (ViewGroup) findViewById(R.id.lnc_dl_popup),
                    (ProgressBar) findViewById(R.id.lnc_progress_bar),
                    (TextView) findViewById(R.id.lnc_progress_text));
            mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    new LoadUtil.PackageDataDownloadTask.UrlAndCounts(url, imageCount, audioCount));
        }
    }

    public void onClickCancelButton(View view){
        Log.d("Launcher", "CANCEL " + packageId);
        if(mTask != null && mTask.isCancelled()){
            mTask.cancel(true);
        }
    }

    public void onClickRemoveButton(View view){
        Log.d("Launcher", "DELETE " + packageId);
    }


    public static class AudioAdapter extends ArrayAdapter<Map> {

        private Context mContext;
        private int mResource;
        private LayoutInflater mInflater;
        public List<Map> yamls;

        public AudioAdapter(@NonNull Context context, int resource, @NonNull List<Map> yamls) {
            super(context, resource, yamls);

            mContext = context;
            mResource = resource;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.yamls = yamls;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = mInflater.inflate(mResource, null);
            }

            // Bind view
            TextView count = view.findViewById(R.id.aud_count);
            TextView title = view.findViewById(R.id.aud_title);
            TextView autor = view.findViewById(R.id.aud_author);
            ImageView badge = view.findViewById(R.id.aud_badge);
            // Initialize contents
            count.setText("0.");
            title.setText("TITLE");
            autor.setText("AUTHOR");
            badge.setVisibility(View.INVISIBLE);
            // Set contents
            count.setText(String.valueOf(position + 1) + ".");
            title.setText((String) yamls.get(position).get("title"));
            autor.setText((String) yamls.get(position).get("author"));
            badge.setVisibility(
                    ((boolean)(yamls.get(position).get("official"))) ? View.VISIBLE: View.INVISIBLE);

            return view;
        }
    }
}
