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

        LibraryTapAdapter fpa = new LibraryTapAdapter(getSupportFragmentManager());
        ViewPager pager = findViewById(R.id.viewPager);
        pager.setOffscreenPageLimit(fpa.getCount());
        pager.setAdapter(fpa);

        // Welcam Tab
        pager.setCurrentItem(1, true);
        setTitle(fpa.getTitle(1));

        TabLayout tab = findViewById(R.id.tabLayout);
        tab.setupWithViewPager(pager);
        for(int i = 0; i < fpa.getCount(); i++){
            tab.getTabAt(i).setIcon(TAB_ICONS[i]);
        }
    }

    private Menu mMenue;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menue_attion, menu);

        mMenue = menu;

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                Log.d("MENU", "Help");
                break;
            case R.id.config:
                Log.d("MENU", "Config");
                break;
            case R.id.upload:
                Log.d("MENU", "Upload");
                break;
            case R.id.about:
                Log.d("MENU", "About");
                break;
            case R.id.donate:
                Log.d("MENU", "Donate");
                String path = getFilesDir() + "/" + TabFragment.LOCAL_PACKAGE_SUMMATY_LIST_PATH;
                Log.d("DEB", path);
                try {
                    Log.d("DEB", LoadUtil.getStringFromPath(path));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                Log.d("MENU", "Invalid");
        }
        return true;
    }
}
