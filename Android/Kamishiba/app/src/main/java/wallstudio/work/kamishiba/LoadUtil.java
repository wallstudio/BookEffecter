package wallstudio.work.kamishiba;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LoadUtil{
    public static final String BASIC_ACCOUNT_PASS = "uphashi:makichan_kawaii_yatta";

    public static final String REMOTE_API_URL = "https://kamishiba.wallstudio.work/api";
    public static final String REMOTE_DATA_URL = "https://kamishiba.wallstudio.work/packages";
    public static final String LOCAL_PACKAGE_FILENAME = "package.yml";
    public static final String LOCAL_INDEX_FILENAME = "index.yml";

    public static final String[] PACKAGE_SUMMARY_ENTRIES = new String[]{
            "id",
            "title",
            "author",
            "contact",
            "page_count",
            "audio_count",
            "publish_date",
            "genre",
            "sexy",
            "violence",
            "grotesque",
            "download_status"
    };

    public static void download(String url, String path) throws IOException {
        InputStream stream = getStreamFromUrl(url);
        saveStream(stream, path);
    }

    public static void removeDirectoryRecursion(String... paths){
        for(String path: paths) {
            File file = new File(path);
            if (file.exists()) {
                if (file.isDirectory()) {
                    String[] subFiles = file.list();
                    for (String f : subFiles)
                        removeDirectoryRecursion(file.getPath() + "/" + f);
                } else if (file.isFile()) {
                    file.delete();
                    Log.d("DELETE", file.getPath());
                } else
                    throw new RuntimeException("Invalid file type");
            }
        }
    }

    // InputStream; close() はしない
    public static InputStream getStreamFromPath(String path) throws IOException{
        InputStream stream = new FileInputStream(path);
        return stream;
    }

    public static InputStream getStreamFromUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        if(BASIC_ACCOUNT_PASS != null && BASIC_ACCOUNT_PASS.contains(":")){
            connection.setRequestProperty(
                    "Authorization", "Basic "
                    + Base64.encodeToString(BASIC_ACCOUNT_PASS.getBytes(), Base64.NO_WRAP));
        }
        connection.connect();
        InputStream stream = connection.getInputStream();
        return  stream;
    }

    public static void saveStream(InputStream inputStream, String path) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(path));
        byte[] bytes = new byte[512];
        int count = 0;
        while (0 < (count = bufferedInputStream.read(bytes))){
            stream.write(bytes, 0, count);
        }
        stream.close();
    }

    // Bitmap
    public static Bitmap streamToBitmap(InputStream stream) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
        if (bitmap == null) throw new IOException("Stream から Bitmap が生成できません");
        return bitmap;
    }

    public static Bitmap getBitmapFromPath(String path)throws IOException{
        InputStream stream = getStreamFromPath(path);
        Bitmap bitmap = streamToBitmap(stream);
        stream.close();
        return  bitmap;
    }

    public static Bitmap getBitmapFromUrl(String url) throws  IOException{
        InputStream stream = getStreamFromUrl(url);
        Bitmap bitmap = streamToBitmap(stream);
        stream.close();
        return  bitmap;
    }

    public static Bitmap getBitmapFromUrlWithCache(Context context, String url){
        InputStream stream;
        String sha1 = getSha1Hash(url);
        File file = new File(context.getCacheDir() + "/" + sha1);
        try {
            if(!file.exists()){
                stream = LoadUtil.getStreamFromUrl(url);
                saveStream(stream, context.getCacheDir() + "/" + sha1);
                stream.close();
            }
            stream = getStreamFromPath(context.getCacheDir() + "/" + sha1);
            Bitmap bitmap = LoadUtil.streamToBitmap(stream);
            stream.close();
            return bitmap;
        } catch (IOException e) {
            if(file.exists()){
                file.delete();
            }
        }
        return null;
    }

    public static void saveBitmap(Bitmap bitmap, String path) throws IOException {
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(path));
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        stream.close();
    }

    // String
    public static String streamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + '\n');
        }
        br.close();

        String string = sb.toString();
        //string = string.substring(0, string.length() - 2);

        return string;
    }

    public static String getStringFromPath(String path) throws IOException{
        InputStream stream = getStreamFromPath(path);
        String result = streamToString(stream);
        stream.close();
        return  result;
    }

    public static String getStringFromUrl(String url) throws IOException{
        InputStream stream = getStreamFromUrl(url);
        String result = streamToString(stream);
        stream.close();
        return  result;
    }

    public static String getStringFromUrlWithCache(Context context, String url) throws IOException {
        InputStream stream;
        String sha1 = getSha1Hash(url);
        File file = new File(context.getCacheDir() + "/" + sha1);
        try {
            if(!file.exists()){
                stream = LoadUtil.getStreamFromUrl(url);
                saveStream(stream, context.getCacheDir() + "/" + sha1);
                stream.close();
            }
            stream = getStreamFromPath(context.getCacheDir() + "/" + sha1);
            String string = LoadUtil.streamToString(stream);
            stream.close();
            return string;
        } catch (IOException e) {
            if(file.exists()){
                file.delete();
            }
            throw e;
        }
    }

    public static void saveString(String string, String path) throws IOException {
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(path));
        stream.write(string.getBytes());
        stream.close();
    }

    // YAML
    public static Object getYamlFromPath(String path) throws IOException {
        String yamlString = getStringFromPath(path);
        Yaml yaml = new Yaml();
        Object map = yaml.load(yamlString);
        return map;
    }

    public static Object getYamlFromUrl(String url) throws IOException {
        String yamlString = getStringFromUrl(url);
        Yaml yaml = new Yaml();
        Object map = yaml.load(yamlString);
        return map;
    }

    // YAML Util
    public static void checkIndexDownloaded(List<Map> clouds, List<Map> locals){
        if(clouds == null || locals == null)
            return;

        for (Map cloud : clouds){
            String cId = (String) cloud.get("id");
            for(Map local : locals){
                String lId = (String) local.get("id");

                if(cId.equals(lId)){
                    cloud.put("download_status", true);
                }
            }
        }
    }

    public static void adultFilter(List<Map> index, boolean isAllowSexy, boolean isAllowViolence, boolean isAllowGrotesque) {
        List<Map> trash = new ArrayList<>();
        for (Map pac : index){
            if(!isAllowSexy && (boolean)pac.get("sexy")
                    ||!isAllowViolence && (boolean)pac.get("violence")
                    ||!isAllowGrotesque && (boolean)pac.get("grotesque")){
                trash.add(pac);
            }
        }
        index.removeAll(trash);
    }

    public static void searchFilter(List<Map> index, String search){
        if(search == null || search.trim().equals("")) return;

        List<Map> trash = new ArrayList<>();
        for (Map pac : index){
            if(!(((String)pac.get("id")).contains(search)
                    || ((String)pac.get("title")).contains(search)
                    || ((String)pac.get("author")).contains(search)
                    || ((String)pac.get("contact")).contains(search)
                    || TextUtils.join(" ", (List<String>)pac.get("genre")).contains(search))){
                trash.add(pac);
            }
        }
        index.removeAll(trash);
    }

    public static void copyPackageRemoteToLocal(Activity context, String packageId) throws IOException {
        String localIndexPath = context.getFilesDir() + "/" + LOCAL_INDEX_FILENAME;
        String remoteIndexUrl = REMOTE_API_URL;

        List<Map> localIndex = new File(localIndexPath).exists() ?
                (List<Map>) getYamlFromPath(localIndexPath) : new ArrayList<Map>();
        List<Map> remoteIndex = (List<Map>) getYamlFromUrl(remoteIndexUrl);
        Map targetPackage = null;
        for (Map pac: remoteIndex) {
            if(packageId.equals(pac.get("id"))){
                targetPackage = pac;
                break;
            }
        }
        if(localIndex == null) localIndex = new ArrayList<>();
        localIndex.add(targetPackage);
        saveString(new Yaml().dump(localIndex), localIndexPath);
    }

    public static void removePackage(Activity context, String packageId) throws IOException {
        String localIndexPath = context.getFilesDir() + "/" + LOCAL_INDEX_FILENAME;
        if(!new File(localIndexPath).exists())
            saveString("", localIndexPath);

        List<Map> localIndex = new File(localIndexPath).exists() ?
                (List<Map>) getYamlFromPath(localIndexPath) : new ArrayList<Map>();
        for (Map pac: localIndex) {
            if(packageId.equals(pac.get("id"))){
                localIndex.remove(pac);
                break;
            }
        }
        saveString(new Yaml().dump(localIndex), localIndexPath);
    }

    // Util
    public static String getSha1Hash(String plain){
        String cypher = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5"); // <--> book's UUID
            byte[] result = digest.digest(plain.getBytes());
            for(byte b : result)
                cypher += String.format("%02X", b);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return  cypher;
    }

    public static String join(List<String> strings, String separator){
        StringBuilder builder = new StringBuilder();
        for (String t : strings){
            builder.append(t + separator);
        }
        return  builder.substring(0, builder.length() - separator.length());
    }

    // Task
    public static class ImageDownloadAndShowTask extends AsyncTask<Void, Double, Void>{

        private Context mContext;
        private ImageView mDestView;
        private String mUrl;
        private Bitmap mBitmap;

        public ImageDownloadAndShowTask(Context context, ImageView destView, String url){
            mContext = context;
            mDestView = destView;
            mUrl = url;
        }

        @Override
        protected Void doInBackground(Void... _void) {
            mBitmap = getBitmapFromUrlWithCache(mContext, mUrl);
            return null;
        }

        @Override
        protected void onPostExecute(Void _void){
            if(null != mBitmap){
                mDestView.setImageBitmap(mBitmap);
            }else {
                Toast.makeText(mContext, "Thumbnail download error", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static class CloudPackageListDownloadTask extends AsyncTask<Void, Double, Void>{

        protected Context mContext;
        private GridView mGridView;
        protected String mSearch;

        private boolean isAllowSexy;
        private boolean isAllowViolence;
        private boolean isAllowGrotesque;

        protected List<Map> mIndex = new ArrayList<>();

        public CloudPackageListDownloadTask(Context context, GridView grid, String search){
            super();
            mContext = context;
            mGridView = grid;
            mSearch = search;

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            isAllowSexy = sharedPreferences.getBoolean("pref_sexy", false);
            isAllowViolence = sharedPreferences.getBoolean("pref_violence", false);
            isAllowGrotesque = sharedPreferences.getBoolean("pref_grotesque",false);
        }

        @Override
        protected Void doInBackground(Void... _void) {
            try {
                String cloudIndexUrl = REMOTE_API_URL;
                String localPath = mContext.getFilesDir() + "/" + LOCAL_INDEX_FILENAME;
                mIndex = (List<Map>) getYamlFromUrl(cloudIndexUrl);
                List<Map> localSummaries = (List<Map>) getYamlFromPath(localPath);
                adultFilter(mIndex, isAllowSexy, isAllowViolence, isAllowGrotesque);
                searchFilter(mIndex, mSearch);
                checkIndexDownloaded(mIndex, localSummaries);
            } catch (IOException e){
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void _void){
            LibraryTabFragment.PackageGridAdapter pga
                    = new LibraryTabFragment.PackageGridAdapter(
                            mContext, R.layout.fragment_package, mIndex);
            mGridView.setAdapter(pga);
        }
    }

    public static class LocalPackageListLoadTask extends CloudPackageListDownloadTask{

        public LocalPackageListLoadTask(Context context, GridView grid, String search) {
            super(context, grid, search);
        }

        @Override
        protected Void doInBackground(Void... _void) {
            try {
                String path = mContext.getFilesDir() + "/" + LOCAL_INDEX_FILENAME;
                mIndex = new File(path).exists() ?
                        (List<Map>) getYamlFromPath(path) : new ArrayList<Map>();
            } catch (IOException e){
            }

            if(mIndex == null) mIndex = new ArrayList<>();
            searchFilter(mIndex, mSearch);
            for (Map pac : mIndex)
                pac.put("download_status", true);
            return null;
        }
    }

    public static class PackageDetailDownloadTask extends AsyncTask<Void, Double, Void>{
        private LauncherActivity mContext;
        private String mPackageId;
        private ListView mAudioListView;
        private ImageView mCoverView;
        private TextView mTitleLabelView;
        private TextView mAuthorLabelView;
        private TextView mTagLabalView;
        private TextView mDescriptionLabelView;
        private TextView mPageCountLabelView;

        private Map mPackageData;
        private Bitmap mCover;

        public PackageDetailDownloadTask(
                LauncherActivity context, String id, ListView audioListView,
                ImageView coverVIew, TextView titleLabelView, TextView authorLabelView,
                TextView tagLabelView, TextView descriptionLabelView, TextView pageCountLabelView){
            mContext = context;
            mPackageId = id;
            mAudioListView = audioListView;
             mCoverView = coverVIew;
            mTitleLabelView = titleLabelView;
            mAuthorLabelView = authorLabelView;
            mTagLabalView = tagLabelView;
            mDescriptionLabelView = descriptionLabelView;
            mPageCountLabelView = pageCountLabelView;
        }

        // まずローカルを見てなければキャッシュ付ダウンロード
        // IOExceptionが起きたら書き戻しはしない
        @Override
        protected Void doInBackground(Void... _void) {
            String packageUrl = REMOTE_API_URL + "/" + mPackageId;
            String packagePath = mContext.getFilesDir() + "/" + mPackageId + "/" + LOCAL_PACKAGE_FILENAME;
            String coverUrl = REMOTE_DATA_URL + "/" + mPackageId + "/" + "0.jpg";
            String coverPath = mContext.getFilesDir() + "/" + mPackageId + "/" + "0.jpg";

            try {
                String yaml = "";
                if (new File(packagePath).exists()) {
                    yaml = getStringFromPath(packagePath);
                    mCover = getBitmapFromPath(coverPath);
                } else {
                    yaml = getStringFromUrl(packageUrl);
                    mCover = getBitmapFromUrlWithCache(mContext, coverUrl);
                }
                mPackageData = new Yaml().load(yaml);
            }catch (Exception e){
                Log.e("ERROR", e.getMessage());
            }

            return  null;
        }

        @Override
        protected void onPostExecute(Void _void){
            if(mPackageData == null) return;

            List<Map> audios = null;
            try {
                mContext.yaml = mPackageData;

                mTitleLabelView.setText((String) mPackageData.get("title"));
                mAuthorLabelView.setText((String) mPackageData.get("author"));
                mTagLabalView.setText(join((List<String>) mPackageData.get("genre"), ", "));
                mDescriptionLabelView.setText((String) mPackageData.get("description"));
                mPageCountLabelView.setText(String.valueOf((int) mPackageData.get("page_count")));
                audios = (List<Map>) mPackageData.get("audio");
            }catch (NullPointerException e){ }

            if(mCover != null) mCoverView.setImageBitmap(mCover);
            if(audios != null) {
                LauncherActivity.AudioAdapter adapter
                        = new LauncherActivity.AudioAdapter(mContext, R.layout.fragment_audio, audios);
                mAudioListView.setAdapter(adapter);
            }
        }
    }

    public static class PackageDataDownloadTask extends AsyncTask<Void, Double, Void>{
        private LauncherActivity mContext;
        private String mPackageId;
        private ViewGroup mDownloadPopupView;
        private ProgressBar mProgressGraphView;
        private TextView mProgressLabelView;

        private  static List<String> singleTaskEachPackage = new ArrayList<>();

        public PackageDataDownloadTask(
                LauncherActivity context, String packageId,
               ViewGroup downloadPopupView, ProgressBar progressGraphView, TextView progressLabelView){
            mContext = context;
            mPackageId = packageId;
            if(singleTaskEachPackage.contains(mPackageId)) {
                cancel(true);
                Toast.makeText(mContext, "Already running task", Toast.LENGTH_LONG).show();
            } else {
                singleTaskEachPackage.add(mPackageId);
            }
            mDownloadPopupView = downloadPopupView;
            mProgressGraphView = progressGraphView;
            mProgressLabelView = progressLabelView;
        }

        @Override
        protected void onPreExecute(){
            mDownloadPopupView.setVisibility(View.VISIBLE);
            mProgressGraphView.setProgress(0);
        }

        @Override
        protected Void doInBackground(Void... _void) {
            if(isCancelled()) return  null;
            try {
                String url = REMOTE_API_URL + "/" + mPackageId;
                String yaml = getStringFromUrl(url);
                Map packageData = new Yaml().load(yaml);

                // DL数を計算
                int imageCount = (int)packageData.get("page_count");
                int audioCount = (int)packageData.get("audio_count");
                int totalCount = imageCount + audioCount + 2;
                int progressCount = 0;

                // ディレクトリの準備
                File packagePath = new File(mContext.getFilesDir() + "/" + mPackageId);
                if(!packagePath.exists()) packagePath.mkdirs();

                if(isCancelled()) return null;
                publishProgress((double)++progressCount/totalCount, (double)progressCount, (double)totalCount);

                // Download YAML
                download(REMOTE_API_URL + "/" + mPackageId,
                        mContext.getFilesDir() + "/" + mPackageId + "/" + LOCAL_PACKAGE_FILENAME);

                if(isCancelled()) return null;
                publishProgress((double)++progressCount/totalCount, (double)progressCount, (double)totalCount);

                // Download image set
                for (int i = 0; i < imageCount; i++){
                    String fileName = i + ".jpg";
                    download(REMOTE_DATA_URL + "/" + mPackageId + "/" + fileName,
                            mContext.getFilesDir() + "/" + mPackageId + "/" + fileName);

                    if(isCancelled()) return null;
                    publishProgress((double)++progressCount/totalCount, (double)progressCount, (double)totalCount);
                }

                // Download audios
                for (Map audio : (List<Map>) packageData.get("audio")){
                    String fileName = audio.get("id") + ".mp3";
                    download(REMOTE_DATA_URL + "/" + mPackageId + "/" + fileName,
                            mContext.getFilesDir() + "/" + mPackageId + "/" + fileName);

                    if(isCancelled()) return null;
                    publishProgress((double)++progressCount/totalCount, (double)progressCount, (double)totalCount);
                }

                // ローカルのインデックスの保存
                copyPackageRemoteToLocal(mContext, mPackageId);
                if(isCancelled()) return null;
                publishProgress((double)++progressCount/totalCount, (double)progressCount, (double)totalCount);

                // 0.5s 待たせて終わった感を出す
                if(isCancelled()) return null;
                publishProgress((double)1, (double)totalCount, (double)totalCount);

                Thread.sleep(500);

            } catch (Exception e) {
                cancel(true);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Double... doubles){
            double progress = doubles[0];
            int progressedFileCount = (int)(double)doubles[1];
            int totalFileCount = (int)(double)doubles[2];
            mProgressGraphView.setProgress((int)(progress*100));
            mProgressLabelView.setText(String.format("Download... (%d/%d)", progressedFileCount, totalFileCount));
        }

        @Override
        protected void onPostExecute(Void dummy){
            if(isCancelled()) return;

            mDownloadPopupView.setVisibility(View.INVISIBLE);
            mContext.setIsDownloaded(true);
            singleTaskEachPackage.remove(mPackageId);
            Toast.makeText(mContext, "Download completed!", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onCancelled(){
            mDownloadPopupView.setVisibility(View.INVISIBLE);
            mContext.setIsDownloaded(false);
            singleTaskEachPackage.remove(mPackageId);
            LoadUtil.removeDirectoryRecursion(
                    mContext.getFilesDir() + "/" + mPackageId,
                    mContext.getCacheDir() + "/" + getSha1Hash(REMOTE_API_URL + "/" + mPackageId),
                    mContext.getCacheDir() + "/" + getSha1Hash(REMOTE_DATA_URL + "/" + mPackageId + "0.jpg"));
            try {
                removePackage(mContext, mPackageId);
            } catch (IOException e) { }
            Toast.makeText(mContext, "Cancelled", Toast.LENGTH_LONG).show();
        }
    }
}
