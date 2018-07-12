package wallstudio.work.kamishiba;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
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
        switch (item.getItemId()) {
            case R.id.help:
                Toast.makeText(this, "Help", Toast.LENGTH_SHORT).show();
                break;
            case R.id.config:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case R.id.upload:
                Toast.makeText(this, "Upload", Toast.LENGTH_SHORT).show();
                break;
            case R.id.about:
                Toast.makeText(this, "About", Toast.LENGTH_SHORT).show();
                break;
            case R.id.donate:
                Toast.makeText(this, "Donate", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, "Invalid", Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
