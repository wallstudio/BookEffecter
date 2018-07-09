package wallstudio.work.kamishiba;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.Arrays;
import java.util.List;

public class LibraryTapAdapter extends FragmentPagerAdapter {

    private List<String> tabTitles;
    private List<TabFragment> tabFragments;

    public LibraryTapAdapter(FragmentManager fm) {
        super(fm);
        tabTitles = Arrays.asList("Local", "Cloud", "QR");

        // 各タブのエントリ
        TabFragment local = new LocalLibraryTabFragment();
        TabFragment cloud = new CloudLibraryTabFragment();
        TabFragment qr = new QRScanerFragment();
        tabFragments = Arrays.asList(local, cloud, qr);
    }

    public String getTitle(int position){
        return tabFragments.get(position).getTitle();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return tabTitles.get(position);
    }

    @Override
    public Fragment getItem(int position) {
        return tabFragments.get(position);
    }

    @Override
    public int getCount() {
        return  tabTitles.size();
    }
}
