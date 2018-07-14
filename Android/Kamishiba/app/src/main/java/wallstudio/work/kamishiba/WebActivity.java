package wallstudio.work.kamishiba;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends AppCompatActivity {

    private WebView mWebScreenView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        mWebScreenView = findViewById(R.id.web_screen);
        mWebScreenView.setWebChromeClient(new WebChromeClient());
        mWebScreenView.getSettings().setJavaScriptEnabled(true);
        mWebScreenView.getSettings().setAppCacheEnabled(true);
        mWebScreenView.getSettings().setSaveFormData(true);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        mWebScreenView.loadUrl(getIntent().getStringExtra("url"));
        mWebScreenView.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.contains("github"))
                    mWebScreenView.loadUrl(url);
                return true;
            }
        });

        // アクションバーの設定
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getIntent().getStringExtra("title"));
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
