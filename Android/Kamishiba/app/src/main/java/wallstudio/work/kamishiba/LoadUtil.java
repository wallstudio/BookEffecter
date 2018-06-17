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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadUtil{

    // InputStream の close() はしない
    public static InputStream getStreamFromUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        InputStream stream = connection.getInputStream();
        return  stream;
    }

    public static InputStream getStreamFromPath(String path) throws IOException{
        InputStream stream = new FileInputStream(path);
        return stream;
    }

    public static Bitmap streamToBitmap(InputStream stream) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
        if (bitmap == null) throw new IOException("Stream から Bitmap が生成できません");
        return bitmap;
    }

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

    public static void saveStream(InputStream inputStream, String path, boolean isExternal) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(path));
        byte[] bytes = new byte[512];
        int count = 0;
        while (0 < (count = bufferedInputStream.read(bytes))){
            stream.write(bytes, 0, count);
        }
        stream.close();
    }

    public static void saveBitmap(Bitmap bitmap, String path, boolean isExternal) throws IOException {
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(path));
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        stream.close();
    }

    public static void saveString(String string, String path, boolean isExternal) throws IOException {
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(path));
        stream.write(string.getBytes());
        stream.close();
    }

    public static String getDataDirPath(Context context, boolean isExternal){
        // ref. https://qiita.com/flat-8-kiki/items/dd3dfdd49e4ee967d8f7#android
        String strageState = Environment.getExternalStorageState();
        if(isExternal && strageState.equals(Environment.MEDIA_MOUNTED))
            return context.getExternalFilesDir(null).getPath();
        else
            return context.getFilesDir().getPath();
    }

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
            List<Map> pacs = new ArrayList<>();
            try {
                String url = param[0];
                String result = getStringFromUrl(url);
                result = result.trim();
                Yaml yaml = new Yaml();
                pacs = yaml.load(result);
            } catch (IOException e){
            }
            return pacs;
        }

        @Override
        protected void onPostExecute(List<Map> pacs){
            LibraryTabFragment.PackageGridAdapter pga
                    = new LibraryTabFragment.PackageGridAdapter(
                            mContext, R.layout.fragment_package, pacs);
            mGridView.setAdapter(pga);
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
            try {
                yamlString = getStringFromUrl(url);
                Yaml yaml = new Yaml();
                result.map = yaml.load(yamlString);
            } catch (IOException e) { }
            result.bitmap = getBitmapFromUrlWithCache(mContext, coverUrl);
            return  result;
        }

        @Override
        protected void onPostExecute(MapBitmapPair pac){
            mContext.yaml = pac.map;

            List<Map> audios = new ArrayList<>();
            if(pac.map != null){
                try {
                    mTitle.setText((String) pac.map.get("title"));
                    mAuthor.setText((String) pac.map.get("author"));
                    mTags.setText(join((List<String>) pac.map.get("genre"), ", "));
                    mDescription.setText((String) pac.map.get("description"));
                    mPageCount.setText(String.valueOf((int) pac.map.get("page_count")));
                    audios = (List<Map>) pac.map.get("audio");
                }catch (NullPointerException e){ }
            }
            if(pac.bitmap != null){
                mCover.setImageBitmap(pac.bitmap);
            }

            LauncherActivity.AudioAdapter aa
                    = new LauncherActivity.AudioAdapter(
                    mContext, R.layout.fragment_audio, audios);
            mList.setAdapter(aa);
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

        private LauncherActivity mContext;
        private ProgressBar mProgressBar;
        private ViewGroup mPopup;
        private TextView mProgressText;

        public PackageDataDownloadTask(LauncherActivity context, ViewGroup popup, ProgressBar progressBar, TextView progressText){
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
            for (int i=0;i< 10;i++){
                try {
                    Thread.sleep(1000);
                    publishProgress(i/10.0, (double)i, (double)imageCount + audioCount);
                } catch (InterruptedException e) { }
            }

            publishProgress((double)1, (double)imageCount + audioCount, (double)imageCount + audioCount);
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
            Toast.makeText(mContext, "Download completed!", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onCancelled(){
            mPopup.setVisibility(View.INVISIBLE);
            Toast.makeText(mContext, "Cancelled", Toast.LENGTH_LONG).show();
        }
    }
}
