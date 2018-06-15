package wallstudio.work.kamishiba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.GridView;
import android.widget.ImageView;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

    public static class stringDownloadTask extends AsyncTask<String, Double, String> {
        @Override
        protected String doInBackground(String... strings) {

            String url = strings[0];
            String result = "";
            try {
                InputStream stream = LoadUtil.getStreamFromUrl(url);
                result = LoadUtil.streamToString(stream);
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
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
            InputStream stream;
            String sha1 = getSha1Hash(url);
            File file = new File(mContext.getCacheDir() + "/" + sha1);
            try {
                if(!file.exists()){
                    stream = LoadUtil.getStreamFromUrl(url);
                    saveStream(stream, mContext.getCacheDir() + "/" + sha1);
                    stream.close();
                }
                stream = getStreamFromPath(mContext.getCacheDir() + "/" + sha1);
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

        @Override
        protected void onPostExecute(Bitmap bitmap){
            if(null != bitmap){
                mImage.setImageBitmap(bitmap);
            }else {
                Toast.makeText(mContext, "Thumbnail download error", Toast.LENGTH_SHORT);
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
                LoadUtil.stringDownloadTask task = new LoadUtil.stringDownloadTask();
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
                String result = task.get();
                result = result.trim();
                Yaml yaml = new Yaml();
                pacs = yaml.load(result);
            } catch (InterruptedException e){
            } catch (ExecutionException e) {
                Log.e("", e.toString());
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
}
