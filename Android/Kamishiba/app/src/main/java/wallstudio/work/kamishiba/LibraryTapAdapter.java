package wallstudio.work.kamishiba;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibraryTapAdapter extends FragmentPagerAdapter {

    private List<String> tabTitles;
    private List<Fragment> tabFragments;

    public LibraryTapAdapter(FragmentManager fm) {
        super(fm);
        tabTitles = Arrays.asList(/*"Favorite",*/ "Local", "Cloud", "QR");
        tabFragments = Arrays.asList(
                //new LibraryTabFragment(),
                new LibraryTabFragment(),
                new LibraryTabFragment(),
                new QRScanerFragment()
        );
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return (CharSequence)tabTitles.get(position);
    }

    @Override
    public Fragment getItem(int position) {
        if(position == 2)
            ((QRScanerFragment)(tabFragments.get(2))).setActive(true);
        else
            ((QRScanerFragment)(tabFragments.get(2))).setActive(false);

        return tabFragments.get(position);
    }

    @Override
    public int getCount() {
        return  tabTitles.size();
    }
}
