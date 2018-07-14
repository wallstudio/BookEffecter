package wallstudio.work.kamishiba;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

public class LibraryActivity extends AppCompatActivity {

    public static final int[] TAB_ICONS = {
            //R.drawable.ic_star_black_24dp,
            R.drawable.ic_sd_card_black_24dp,
            R.drawable.ic_cloud_download_black_24dp,
            R.drawable.ic_center_focus_weak_black_24dp};

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liblary);

        // タブの中身を設置
        LibraryTapAdapter fpa = new LibraryTapAdapter(getSupportFragmentManager());
        ViewPager pager = findViewById(R.id.viewPager);
        pager.setOffscreenPageLimit(fpa.getCount());
        pager.setAdapter(fpa);

        // 初期のタブを設定
        pager.setCurrentItem(1, true);
        setTitle(fpa.getTitle(1));
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.kamishiba_main_icon);

        // タブのインジケーター（？）の設定
        TabLayout tab = findViewById(R.id.tabLayout);
        tab.setupWithViewPager(pager);
        for(int i = 0; i < fpa.getCount(); i++){
            tab.getTabAt(i).setIcon(TAB_ICONS[i]);
        }
    }

    // アクションバーのメニュー（:）
    private Menu mMenue;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menue_attion, menu);
        mMenue = menu;
        return super.onCreateOptionsMenu(menu);
    }

    // メニューの各項目を押したときの動作
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
            case R.id.help:
                intent = new Intent(this, WebActivity.class);
                intent.putExtra("title", "ヘルプ");
                intent.putExtra("url", "https://wallstudio.github.io/BookEffecter/help");;
                break;
            case R.id.config:
                intent = new Intent(this, SettingsActivity.class);
                break;
            case R.id.upload:
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://kamishiba.wallstudio.work/Books"));
                break;
            case R.id.about:
                intent = new Intent(this, WebActivity.class);
                intent.putExtra("title", "このアプリについて");
                intent.putExtra("url", "https://wallstudio.github.io/BookEffecter/about");
                break;
            case R.id.donate:
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wallstudio.hateblo.jp/"));
                break;
            default:
                Toast.makeText(this, "Invalid", Toast.LENGTH_SHORT).show();
        }

        if(intent != null) startActivity(intent);
        return true;
    }
}
