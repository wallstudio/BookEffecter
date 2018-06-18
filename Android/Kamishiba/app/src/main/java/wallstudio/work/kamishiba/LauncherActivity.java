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
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public class LauncherActivity extends AppCompatActivity {

    public static final String PACKAGE_DETAIL_PATH = "detail.yml";
    public static final String COVER_PATH = "cover.jpg";

    public Map yaml;
    
    private String mPackageId;
    private Switch mCameraSwitch;
    private Button mDownloadOrUpdate;
    private Button mRemove;
    private ListView mListView;
    private boolean mIsDownloaded = false;
    private boolean mIsUpdateable = false;

    private AdapterView.OnItemClickListener mOnItemClickListener =  new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            boolean isFront = mCameraSwitch.isChecked();
            Intent intent = new Intent(getApplication(), isFront ? StandCameraActivity.class : HandedActivity.class);
            intent.putExtra("image_count", (int)yaml.get("page_count"));
            intent.putExtra("audio_index", position);
            intent.putExtra("package", mPackageId);
            intent.putExtra("title", (String) yaml.get("title"));
            intent.putExtra("author", (String) yaml.get("author"));
            Log.d("StartCamera", String.format(
                    "is_front=%s, audio_index=%s, package=%s",
                    isFront, position, mPackageId));
            startActivity(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        mPackageId = getIntent().getStringExtra("package_id");
        mIsDownloaded = getIntent().getBooleanExtra("download_status", false);
        setTitle(mPackageId);
        setPackageInfo();

        mDownloadOrUpdate = findViewById(R.id.lnc_download_btn);
        mRemove = findViewById(R.id.lnc_remove_btn);
        refleshUIStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mTask != null && !mTask.isCancelled()){
            mTask.cancel(true);
            mTask = null;
        }
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
                getResources().getString(R.string.root_url) + mPackageId + "/" + PACKAGE_DETAIL_PATH,
                getResources().getString(R.string.root_url) + mPackageId + "/" + COVER_PATH);

        mCameraSwitch = findViewById(R.id.lnc_camera_dirction_switch);
    }

    public void setIsDownloaded(boolean isDownloaded){
        this.mIsDownloaded = isDownloaded;
        refleshUIStatus();
    }

    public void setIsUpdateable(boolean isUpdateable){
        this.mIsUpdateable = isUpdateable;
        refleshUIStatus();
    }

    public void refleshUIStatus(){
        if(!mIsDownloaded){
            mDownloadOrUpdate.setVisibility(View.VISIBLE);
            mDownloadOrUpdate.setText("ダウンロード");
            mListView.setAlpha(0.3f);
            mRemove.setVisibility(View.GONE);
            mListView.setOnItemClickListener(null);
        }else if(mIsDownloaded && !mIsUpdateable){
            mDownloadOrUpdate.setVisibility(View.GONE);
            mListView.setAlpha(1.0f);
            mRemove.setVisibility(View.VISIBLE);
            mListView.setOnItemClickListener(mOnItemClickListener);
        }else {
            mDownloadOrUpdate.setVisibility(View.VISIBLE);
            mListView.setAlpha(1.0f);
            mDownloadOrUpdate.setText("更新");
            mRemove.setVisibility(View.VISIBLE);
            mListView.setOnItemClickListener(mOnItemClickListener);
        }
    }

    private LoadUtil.PackageDataDownloadTask mTask;

    public void onClickDownloadOrUpdateButton(View view){
        Log.d("Launcher", "DOWNLOAD " + mPackageId);
        if(yaml != null) {
            try {
                String url = getResources().getString(R.string.root_url) + mPackageId + "/";
                int imageCount = (int) yaml.get("page_count");
                int audioCount = (int) yaml.get("audio_count");
                mTask = new LoadUtil.PackageDataDownloadTask(this,
                        (ViewGroup) findViewById(R.id.lnc_dl_popup),
                        (ProgressBar) findViewById(R.id.lnc_progress_bar),
                        (TextView) findViewById(R.id.lnc_progress_text), mPackageId);
                mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        new LoadUtil.PackageDataDownloadTask.UrlAndCounts(url, imageCount, audioCount));
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage());
            }
        }
    }

    public void onClickCancelButton(View view){
        Log.d("Launcher", "CANCEL " + mPackageId);
        if(mTask != null && !mTask.isCancelled()){
            mTask.cancel(true);
            mTask = null;
        }
    }

    public void onClickRemoveButton(View view){
        removeSummary(mPackageId);
        removeDirectory(this.getFilesDir().getPath() + "/" + mPackageId);
        setIsDownloaded(false);
        Toast.makeText(this, "Deleted " + mPackageId, Toast.LENGTH_SHORT).show();
        Log.d("Launcher", "DELETE " + mPackageId);
    }

    public void removeSummary(String id){
        try {
            String localPath = getFilesDir() + "/" + TabFragment.LOCAL_PACKAGE_SUMMATY_LIST_PATH;
            List<Map> list = (List<Map>) LoadUtil.getYamlFromPath(localPath);
            if(list != null) {
                Map removee = LoadUtil.getSummaryYamlInList(list, id);
                list.remove(removee);
                LoadUtil.saveSummariesYaml(list, localPath, false);
            }
            Log.d("REMOVE", id + " in " + localPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeDirectory(String path){
        File file = new File(path);
        if(file.exists()){
            if(file.isDirectory()){
                String[] subfiles = file.list();
                for(String f : subfiles)
                    removeDirectory(file.getPath() + "/" + f);
            }else if(file.isFile()){
                file.delete();
                Log.d("DELETE", file.getPath());
            }else
                throw new RuntimeException("Invalid file type");
        }
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
