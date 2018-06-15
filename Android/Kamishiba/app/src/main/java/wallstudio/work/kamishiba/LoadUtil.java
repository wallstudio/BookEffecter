package wallstudio.work.kamishiba;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

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

    public static class bitmapDownloadWithCacheTask extends AsyncTask<Object, Double, Bitmap>{
        private ImageView mView;
        private Context mContext;

        @Override
        protected Bitmap doInBackground(Object... objects) {
            mContext = (Context) objects[0];
            mView = (ImageView) objects[1];
            String url = (String) objects[2];
            InputStream stream;
            try {
                String cachepath = "";
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                byte[] result = digest.digest(url.getBytes());
                for(byte b : result)
                    cachepath += String.valueOf(b);

                File file = new File(mContext.getCacheDir() + "/" + cachepath);
                if(!file.exists()){
                    stream = LoadUtil.getStreamFromUrl(url);
                    saveStream(stream, mContext.getCacheDir() + "/" + cachepath);
                    stream.close();
                }

                stream = getStreamFromPath(mContext.getCacheDir() + "/" + cachepath);
                Bitmap bitmap = LoadUtil.streamToBitmap(stream);
                stream.close();
                return bitmap;

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap){
            if(null != bitmap){
                mView.setImageBitmap(bitmap);
            }else {
                Toast.makeText(mContext, "Tumbnail download error", Toast.LENGTH_SHORT);
            }
        }
    }

    public static class SummaryDownloadTask extends AsyncTask<Object, Double, String>{
        private Context mContext;
        private TextView mTitle;
        private TextView mAutor;
        private TextView mDetail;

        @Override
        protected String doInBackground(Object... objects) {
            mContext = (Context) objects[0];
            mTitle = (TextView) objects[1];
            mAutor = (TextView) objects[2];
            mDetail = (TextView) objects[3];
            String url = (String) objects[4];

            try {
                InputStream stream = getStreamFromUrl(url);
                String string = streamToString(stream);
                stream.close();
                return  string;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return "";
        }

        @Override
        protected void onPostExecute(String string) {
            try {
                if(string != null) {
                    Package pac = new Package(mContext, "", null);
                    pac.fromYaml(string);
                    mTitle.setText(pac.book.title);
                    mAutor.setText(pac.book.author.name);
                    mDetail.setText(pac.book.page_count + " " + pac.audio.size());
                }
            } catch (ParseException | IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public static class CloudPackageListDownloadTask extends AsyncTask<Object, Double, List<String>>{

        private Context mContext;
        private GridView mGridView;

        @Override
        protected List<String> doInBackground(Object... param) {
            List<String> pacs = new ArrayList<>();
            try {
                mContext = (Context) param[0];
                mGridView = (GridView) param[1];
                String url = (String) param[2];
                LoadUtil.stringDownloadTask task = new LoadUtil.stringDownloadTask();
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
                String result = task.get();
                result = result.trim();
                pacs = Arrays.asList(result.split("\n"));
            } catch (InterruptedException | ExecutionException e) {
                Toast.makeText(mContext, "failed download list.", Toast.LENGTH_SHORT);
            }
            return pacs;
        }

        @Override
        protected void onPostExecute(List<String> pacs){
            LibraryTabFragment.PackageGridAdapter pga
                    = new LibraryTabFragment.PackageGridAdapter(
                            mContext, R.layout.fragment_package, pacs);
            mGridView.setAdapter(pga);
        }
    }

}
