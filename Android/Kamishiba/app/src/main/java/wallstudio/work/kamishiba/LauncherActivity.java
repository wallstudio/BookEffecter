package wallstudio.work.kamishiba;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
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

import org.opencv.core.Mat;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LauncherActivity extends AppCompatActivity {

    public static final String INTENT_KEY_PACKAGE_ID = "package_id";

    public static final String PACKAGE_DETAIL_PATH = "detail.yml";
    public static final String COVER_PATH = "cover.jpg";

    public Map yaml;
    
    private String mPackageId;
    private boolean mIsDownloaded = false;
    public void setIsDownloaded(boolean isDownloaded){
        this.mIsDownloaded = isDownloaded;
        refreshUIStatus();
    }

    private boolean mIsUpdateable = false;
    public void setIsUpdateable(boolean isUpdateable){
        this.mIsUpdateable = isUpdateable;
        refreshUIStatus();
    }

    private AdapterView.OnItemClickListener mOnItemClickListener =  new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            try {
                boolean isFront = mCameraSwitch.isChecked();
                Intent intent = new Intent(getApplication(), isFront ? StandCameraActivity.class : HandedCameraActivity.class);
                intent.putExtra("package", mPackageId);
                intent.putExtra("audio", position);

                Log.d("StartCamera", String.format(
                        "is_front=%s, audio_index=%s, package=%s",
                        isFront, position, mPackageId));

                startActivity(intent);
            } catch (Exception e) { }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        mPackageId = getIntent().getStringExtra(INTENT_KEY_PACKAGE_ID);
        setTitle(mPackageId);
        bindViews();
        setInfoToViewsAsync();

        mIsDownloaded = new File(getFilesDir() + "/" + mPackageId + "/" + LoadUtil.LOCAL_PACKAGE_FILENAME).exists();
        refreshUIStatus();

        // アクションバーの設定
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        try {
            String packageConfigPath = getFilesDir() + "/" + mPackageId + "/" + LoadUtil.PACKAGE_CONFIG_FILENAME;
            if (!new File(packageConfigPath).exists()) {
                LoadUtil.preferPackageConfigPath(packageConfigPath);
            }
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            int common = Integer.parseInt(sharedPreferences.getString("pref_default_cam", "0"));
            int pack = Integer.parseInt(((Map<String, String>)LoadUtil.getYamlFromPath(packageConfigPath)).get("camera"));
            mCameraSwitch.setChecked(pack < 0 ? common == 0 : pack == 0);
        }catch (Exception e){}
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // 上部
    private ImageView mCoverView;
    private TextView mTitleLabelView;
    private TextView mAuthorLabelView;
    private TextView mTagLabelView;
    private TextView mDescriptionLabelView;
    private TextView mPageCountLabelView;
    // 下部
    private ListView mAudioListView;
    private Button mDownloadOrUpdateButton;
    private Button mRemoveButton;
    private Switch mCameraSwitch;
    // ポップアップ
    private ViewGroup mDownloadPopupView;
    private ProgressBar mProgressGraphView;
    private TextView mProgressLabelView;
    protected void bindViews(){
        mCoverView = findViewById(R.id.lnc_book_cover);
        mTitleLabelView = findViewById(R.id.lnc_title);
        mAuthorLabelView = findViewById(R.id.lnc_author);
        mTagLabelView = findViewById(R.id.lnc_tag);
        mDescriptionLabelView = findViewById(R.id.lnc_description);
        mPageCountLabelView = findViewById(R.id.lnc_page_count);

        mAudioListView = findViewById(R.id.lnc_audio_list);
        mDownloadOrUpdateButton = findViewById(R.id.lnc_download_btn);
        mRemoveButton = findViewById(R.id.lnc_remove_btn);
        mCameraSwitch = findViewById(R.id.lnc_camera_dirction_switch);

        mDownloadPopupView = findViewById(R.id.lnc_dl_popup);
        mProgressGraphView = findViewById(R.id.lnc_progress_bar);
        mProgressLabelView = findViewById(R.id.lnc_progress_text);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onClickCancelButton(null);
    }

    private void setInfoToViewsAsync(){
        LoadUtil.PackageDetailDownloadTask task = new LoadUtil.PackageDetailDownloadTask(
              this, mPackageId, mAudioListView,
                mCoverView, mTitleLabelView, mAuthorLabelView,
                mTagLabelView, mDescriptionLabelView, mPageCountLabelView);
        task.execute();
    }

    public void refreshUIStatus(){
        if(!mIsDownloaded){
            mDownloadOrUpdateButton.setVisibility(View.VISIBLE);
            mDownloadOrUpdateButton.setText("ダウンロード");
            mAudioListView.setAlpha(0.3f);
            mRemoveButton.setVisibility(View.GONE);
            mAudioListView.setOnItemClickListener(null);
        }else if(mIsDownloaded && !mIsUpdateable){
            mDownloadOrUpdateButton.setVisibility(View.GONE);
            mAudioListView.setAlpha(1.0f);
            mRemoveButton.setVisibility(View.VISIBLE);
            mAudioListView.setOnItemClickListener(mOnItemClickListener);
        }else {
            mDownloadOrUpdateButton.setVisibility(View.VISIBLE);
            mAudioListView.setAlpha(1.0f);
            mDownloadOrUpdateButton.setText("更新");
            mRemoveButton.setVisibility(View.VISIBLE);
            mAudioListView.setOnItemClickListener(mOnItemClickListener);
        }
    }

    private static Map<String, LoadUtil.PackageDataDownloadTask> mTasks = new HashMap<>();
    public void onClickDownloadOrUpdateButtonAsync(View view){
        LoadUtil.PackageDataDownloadTask task = new LoadUtil.PackageDataDownloadTask(
                this, mPackageId,
                        mDownloadPopupView, mProgressGraphView, mProgressLabelView);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        mTasks.put(mPackageId, task);
        Log.d("Launcher", "DOWNLOAD " + mPackageId);
    }

    public void onClickCancelButton(View view){
        Log.d("Launcher", "CANCEL " + mPackageId);
        if(mTasks.get(mPackageId) != null && !mTasks.get(mPackageId).isCancelled()){
            mTasks.get(mPackageId).cancel(true);
            mTasks.remove(mPackageId);
        }
    }

    public void onClickRemoveButton(View view){
        LoadUtil.removeDirectoryRecursion(
                getFilesDir() + "/" + mPackageId,
                getCacheDir() + "/" + LoadUtil.getSha1Hash(LoadUtil.REMOTE_API_URL + "/" + mPackageId),
                getCacheDir() + "/" + LoadUtil.getSha1Hash(LoadUtil.REMOTE_DATA_URL + "/" + mPackageId + "0.jpg"));
        try {
            LoadUtil.removePackage(this, mPackageId);
        } catch (IOException e) { }
        setIsDownloaded(false);
        Toast.makeText(this, "Deleted " + mPackageId, Toast.LENGTH_SHORT).show();
        Log.d("Launcher", "DELETE " + mPackageId);
    }

    public void onClickCameraSwitch(View view){
        try {
            String packageConfigPath = getFilesDir() + "/" + mPackageId + "/" + LoadUtil.PACKAGE_CONFIG_FILENAME;
            Map<String, String> pack = (Map<String, String>)LoadUtil.getYamlFromPath(packageConfigPath);
            pack.put("camera", ((Switch)view).isChecked() ? "0": "1");
            LoadUtil.saveString(new Yaml().dump(pack), packageConfigPath);
        }catch (Exception e){}
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
