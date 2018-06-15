package wallstudio.work.kamishiba;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

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

        FragmentPagerAdapter fpa = new LibraryTapAdapter(getSupportFragmentManager());
        ViewPager pager = findViewById(R.id.viewPager);
        pager.setOffscreenPageLimit(fpa.getCount());
        pager.setAdapter(fpa);

        TabLayout tab = findViewById(R.id.tabLayout);
        tab.setupWithViewPager(pager);
        for(int i = 0; i < fpa.getCount(); i++){
            tab.getTabAt(i).setIcon(TAB_ICONS[i]);
        }
    }

    public void onClick_CAM(View v){
        Intent intent = new Intent(getApplicationContext(), StandCameraActivity.class);
        intent.putExtra("audio", 0);
        intent.putExtra("yaml", "");
        startActivity(intent);
    }
}
