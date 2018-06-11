package wallstudio.work.kamishiba;

import android.os.AsyncTask;
import android.util.Log;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SetDownloadTask extends AsyncTask<String, Double, String> {

    public String url;
    private HttpURLConnection mConnection;
    private Map setAddressMap;

    @Override
    protected String doInBackground(String... url) {

        this.url = url[0];
        try {
            setAddressMap = getYamlfromUrl(this.url);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return  "";
    }

    @Override
    protected void onProgressUpdate(Double... values) { }

    @Override
    protected void onPostExecute(String localPath) { }

    @Override
    protected void onCancelled() {}

    private static Map getYamlfromUrl(String url) throws IOException, ParseException {

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        String result = InputStreamToString(connection.getInputStream());
        Log.d("DOWNLOAD", result);

        Unit unit = Unit.fromYaml(result);
        return null;
    }

    private static String InputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + '\n');
        }
        br.close();
        return sb.toString();
    }
}
