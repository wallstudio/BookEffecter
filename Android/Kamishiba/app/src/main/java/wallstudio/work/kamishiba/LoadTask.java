package wallstudio.work.kamishiba;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

public class LoadTask extends AsyncTask<String, Double, String> {

    private Context context;

    public String url;
    private HttpURLConnection mConnection;
    private Map mSetAddressMap;

    public LoadTask(Context context){
        super();
        this.context = context;
    }

    @Override
    protected String doInBackground(String... url) {

        this.url = url[0];



        return  "";
    }

    @Override
    protected void onProgressUpdate(Double... values) { }

    @Override
    protected void onPostExecute(String localPath) {
        context = null;
        mConnection = null;
        mSetAddressMap = null;
    }

    @Override
    protected void onCancelled() {
    }

    // InputStream の close() はしない
    protected InputStream getStreamFromUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        InputStream stream = connection.getInputStream();
        return  stream;
    }

    protected Bitmap streamToBitmap(InputStream stream) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
        if (bitmap == null) throw new IOException("Stream から Bitmap が生成できません");
        return bitmap;
    }

    protected String streamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + '\n');
        }
        br.close();

        String string = sb.toString();
        string = string.substring(0, string.length() - 2);

        return string;
    }

    protected String saveStream(InputStream inputStream, String relativePath, boolean isExternal) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        String absolutePath = getDataDirPath(isExternal) + "/" + relativePath;
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(absolutePath));
        byte[] bytes = new byte[512];
        int count = 0;
        while (0 < (count = bufferedInputStream.read(bytes))){
            stream.write(bytes, 0, count);
        }
        stream.close();
        return  absolutePath;
    }

    protected String saveBitmap(Bitmap bitmap, String relativePath, boolean isExternal) throws IOException {
        String absolutePath = getDataDirPath(isExternal) + "/" + relativePath;
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(absolutePath));
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        stream.close();
        return  absolutePath;
    }

    protected String saveString(String string, String relativePath, boolean isExternal) throws IOException {
        String absolutePath = getDataDirPath(isExternal) + "/" + relativePath;
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(absolutePath));
        stream.write(string.getBytes());
        stream.close();
        return  absolutePath;
    }

    private  String getDataDirPath(boolean isExternal){
        // ref. https://qiita.com/flat-8-kiki/items/dd3dfdd49e4ee967d8f7#android
        String strageState = Environment.getExternalStorageState();
        if(isExternal && strageState.equals(Environment.MEDIA_MOUNTED))
            return context.getExternalFilesDir(null).getPath();
        else
            return context.getFilesDir().getPath();
    }
}
