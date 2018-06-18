package wallstudio.work.kamishiba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadUtil{

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
            "vaiolence",
            "grotesque",
            "download_status"
    };

    // InputStream; close() はしない
    public static InputStream getStreamFromPath(String path) throws IOException{
        InputStream stream = new FileInputStream(path);
        return stream;
    }

    public static InputStream getStreamFromUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
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

    public static void saveBitmap(Bitmap bitmap, String path, boolean isExternal) throws IOException {
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

    public static String getStringFromUrlWithCashe(Context context, String url){
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
        }
        return "";
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

    public static void saveSummariesYaml(List<Map> summaries, String path, boolean isAppend) throws IOException {
        if(summaries == null) return;

        StringBuilder builder = new StringBuilder();
        if(new File(path).exists()) {
            if (isAppend) {
                InputStream stream = getStreamFromPath(path);
                String preFile = streamToString(stream);
                stream.close();
                builder.append(preFile);
            }
            new File(path).delete();
        }

        for(Map s : summaries){
            builder.append(summaryYamlToString(s));
        }

        saveString(builder.toString(), path);
    }

    // YAML Util
    public static String summaryYamlToString(Map summary) throws IOException {
        if(summary == null) return "";

        StringBuilder builder = new StringBuilder();
        builder.append("\n-\n");
        for (String key : PACKAGE_SUMMARY_ENTRIES){
            builder.append(String.format("    %s: %s\n", key, summary.get(key)));
        }
        return builder.toString();
    }

    public static Map getSummaryYamlInList(List<Map> list, String id){
        if (list == null)
            return  null;

        for (Map m : list){
            String id2 = (String)m.get("id");
            if(id.equals(id2)){
                return m;
            }
        }
        return  null;
    }

    public static void summaryDownloadCheck(List<Map> clouds, List<Map> locals){
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

    public static String getLocalPackageSummaryListPath(Context context){
        return context.getFilesDir() + "/" + TabFragment.LOCAL_PACKAGE_SUMMATY_LIST_PATH;
    }

    public static String getCloudPackageSummaryListUrl(Context context){
        return context.getResources().getString(R.string.root_url)
                + TabFragment.CLOUD_PACKAGE_SUMMARY_LIST_PATH;
    }

    public static String getPackagePath(Context context, String id){
        return context.getFilesDir().getPath() + "/" + id + "/";
    }

    // Task
    public static class PackageSummaryDownloadTask extends AsyncTask<String, Double, Bitmap>{

        private Context mContext;
        private ImageView mImage;

        public PackageSummaryDownloadTask(Context context, ImageView image){
            mContext = context;
            mImage = image;
        }

        @Override
        protected Bitmap doInBackground(String... param) {
            String url = param[0];
            return getBitmapFromUrlWithCache(mContext, url);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap){
            if(null != bitmap){
                mImage.setImageBitmap(bitmap);
            }else {
                Toast.makeText(mContext, "Thumbnail download error", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static class CloudPackageListDownloadTask extends AsyncTask<String, Double, List<Map>>{

        private Context mContext;
        private GridView mGridView;

        public CloudPackageListDownloadTask(Context context, GridView grid){
            super();
            mContext = context;
            mGridView = grid;
        }

        @Override
        protected List<Map> doInBackground(String... param) {
            List<Map> cloucSummaries = null;
            try {
                String cloudUrl = param[0];
                String localPath = getLocalPackageSummaryListPath(mContext);
                cloucSummaries = (List<Map>) getYamlFromUrl(cloudUrl);
                List<Map> localSummaries = (List<Map>) getYamlFromPath(localPath);
                summaryDownloadCheck(cloucSummaries, localSummaries);
            } catch (IOException e){
            }

            if(cloucSummaries == null) cloucSummaries = new ArrayList<>();
            return cloucSummaries;
        }

        @Override
        protected void onPostExecute(List<Map> pacs){
            LibraryTabFragment.PackageGridAdapter pga
                    = new LibraryTabFragment.PackageGridAdapter(
                            mContext, R.layout.fragment_package, pacs);
            mGridView.setAdapter(pga);
        }
    }

    public static class LocalPackageListLoadTask extends CloudPackageListDownloadTask{

        public LocalPackageListLoadTask(Context context, GridView grid) {
            super(context, grid);
        }

        @Override
        protected List<Map> doInBackground(String... param) {
            List<Map> pacs = new ArrayList<>();
            try {
                String path = param[0];
                pacs = (List<Map>) getYamlFromPath(path);
            } catch (IOException e){
            }

            if(pacs == null) pacs = new ArrayList<>();
            return pacs;
        }
    }

    public static class PackageDetailDownloadTask extends AsyncTask<String, Double, PackageDetailDownloadTask.MapBitmapPair>{

        public static class MapBitmapPair{
            Map map;
            Bitmap bitmap;
        }

        private LauncherActivity mContext;
        private ListView mList;
        private ImageView mCover;
        private TextView mTitle;
        private TextView mAuthor;
        private TextView mTags;
        private TextView mDescription;
        private TextView mPageCount;
        private List<Map> mAudios;

        public PackageDetailDownloadTask(LauncherActivity context, ListView list, ImageView cover,
                 TextView title, TextView author, TextView tags, TextView description, TextView pageCount){
            mContext = context;
            mList = list;
            mCover = cover;
            mTitle = title;
            mAuthor = author;
            mTags = tags;
            mDescription = description;
            mPageCount = pageCount;
        }

        @Override
        protected MapBitmapPair doInBackground(String... strings) {
            MapBitmapPair result = new MapBitmapPair();
            result.map = new HashMap();
            String url = strings[0];
            String coverUrl = strings[1];
            String yamlString = null;
            yamlString = getStringFromUrlWithCashe(mContext, url);
            Yaml yaml = new Yaml();
            result.map = yaml.load(yamlString);
            result.bitmap = getBitmapFromUrlWithCache(mContext, coverUrl);
            return  result;
        }

        @Override
        protected void onPostExecute(MapBitmapPair pac){
            if(pac.map != null){
                try {
                    mContext.yaml = pac.map;

                    mTitle.setText((String) pac.map.get("title"));
                    mAuthor.setText((String) pac.map.get("author"));
                    mTags.setText(join((List<String>) pac.map.get("genre"), ", "));
                    mDescription.setText((String) pac.map.get("description"));
                    mPageCount.setText(String.valueOf((int) pac.map.get("page_count")));
                    mAudios = (List<Map>) pac.map.get("audio");
                }catch (NullPointerException e){ }
            }
            if(pac.bitmap != null) mCover.setImageBitmap(pac.bitmap);
            if(mAudios == null) mAudios = new ArrayList<>();
            LauncherActivity.AudioAdapter adapter
                    = new LauncherActivity.AudioAdapter(
                    mContext, R.layout.fragment_audio, mAudios);
            mList.setAdapter(adapter);
        }
    }

    public static class PackageDataDownloadTask extends AsyncTask<PackageDataDownloadTask.UrlAndCounts, Double, Void>{

        public static class UrlAndCounts{
            public String url;
            public int imageCount;
            public int audioCount;

            public UrlAndCounts(String url, int imageCount, int audioCount) {
                this.url = url;
                this.imageCount = imageCount;
                this.audioCount = audioCount;
            }
        }

        public  static List<String> singleTaskEachPackage = new ArrayList<>();
        private LauncherActivity mContext;
        private ProgressBar mProgressBar;
        private ViewGroup mPopup;
        private TextView mProgressText;
        private String mPackageId;

        public PackageDataDownloadTask(LauncherActivity context, ViewGroup popup,
                                       ProgressBar progressBar, TextView progressText,
                                       String id) throws Exception {
            mPackageId = id;
            if(singleTaskEachPackage.contains(mPackageId))
                throw new Exception("Already running task " + mPackageId);
            else
                singleTaskEachPackage.add(mPackageId);
            mContext = context;
            mPopup = popup;
            mProgressBar = progressBar;
            mProgressText = progressText;

        }

        @Override
        protected void onPreExecute(){
            mPopup.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(0);
        }

        @Override
        protected Void doInBackground(UrlAndCounts... urlAndCounts) {
            String url = urlAndCounts[0].url;
            int imageCount = urlAndCounts[0].imageCount;
            int audioCount = urlAndCounts[0].audioCount;

            int totalCount = imageCount + audioCount + 2;
            int progressCount = 1;

            // Storage prepare
            String saveDirPath = getPackagePath(mContext, mPackageId);
            String saveImageDirPath = saveDirPath + "set/";
            String saveAudioDirPath = saveDirPath + "audio/";
            File saveDir = new File(saveDirPath);
            File saveImageDir = new File(saveImageDirPath);
            File saveAudioDir = new File(saveAudioDirPath);
            if(!saveDir.exists()) saveDir.mkdirs();
            if(!saveImageDir.exists()) saveImageDir.mkdirs();
            if(!saveAudioDir.exists()) saveAudioDir.mkdirs();

            if(isCancelled()) return null;
            publishProgress((double)progressCount/totalCount, (double)progressCount, (double)totalCount);

            try {
                // Download YAML
                InputStream stream = getStreamFromUrl(url + "detail.yml");
                saveStream(stream, saveDirPath + "detail.yml");
                stream.close();
                progressCount++;

                if(isCancelled()) return null;
                publishProgress((double)progressCount/totalCount, (double)progressCount, (double)totalCount);

                // Download image set
                for (int i = 0; i < imageCount; i++){
                    String fileNeme =  i + ".jpg";
                    InputStream imageStream = getStreamFromUrl(url + "set/" + fileNeme);
                    saveStream(imageStream, saveImageDirPath + fileNeme);
                    imageStream.close();
                    progressCount++;


                    if(isCancelled()) return null;
                    publishProgress((double)progressCount/totalCount, (double)progressCount, (double)totalCount);
                }

                // Download audios
                for (int i = 0; i < audioCount; i++){
                    String fileName = i + ".mp3";
                    InputStream audioStream = getStreamFromUrl(url + "audio/" + fileName);
                    saveStream(audioStream, saveAudioDirPath + fileName);
                    audioStream.close();
                    progressCount++;

                    if(isCancelled()) return null;
                    publishProgress((double)progressCount/totalCount, (double)progressCount, (double)totalCount);
                }

                // Download summary
                String cloudUrl = getCloudPackageSummaryListUrl(mContext);
                String localPath = getLocalPackageSummaryListPath(mContext);
                List<Map> cloudSummaries = (List<Map>) getYamlFromUrl(cloudUrl);
                Map cloudSummary = getSummaryYamlInList(cloudSummaries, mPackageId);
                cloudSummary.put("download_status", true);
                Map localSummary = null;
                if(new File(localPath).exists()) {
                    List<Map> localSummaries = (List<Map>) getYamlFromPath(localPath);
                    localSummary = getSummaryYamlInList(localSummaries, mPackageId);
                }
                if(localSummary == null)
                    saveSummariesYaml(Arrays.asList(cloudSummary), localPath, true);

            } catch (IOException e) {
                cancel(true);
                return null;
            }

            // 0.5s 待たせて終わった感を出す
            if(isCancelled()) return null;
            publishProgress((double)1, (double)totalCount, (double)totalCount);

            try{ Thread.sleep(500); }catch (InterruptedException e){ }

            return null;
        }

        @Override
        protected void onProgressUpdate(Double... doubles){
            double progress = doubles[0];
            int progressedFileCount = (int)(double)doubles[1];
            int totalFileCount = (int)(double)doubles[2];
            mProgressBar.setProgress((int)(progress*100));
            mProgressText.setText(String.format("Download... (%d/%d)", progressedFileCount, totalFileCount));
        }

        @Override
        protected void onPostExecute(Void dummy){
            mPopup.setVisibility(View.INVISIBLE);
            mContext.setIsDownloaded(true);
            singleTaskEachPackage.remove(mPackageId);
            Toast.makeText(mContext, "Download completed!", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onCancelled(){
            mPopup.setVisibility(View.INVISIBLE);
            mContext.setIsDownloaded(false);
            singleTaskEachPackage.remove(mPackageId);
            mContext.removeSummary(mPackageId);
            mContext.removeDirectory(getPackagePath(mContext, mPackageId));
            Toast.makeText(mContext, "Cancelled", Toast.LENGTH_LONG).show();
        }
    }
}
