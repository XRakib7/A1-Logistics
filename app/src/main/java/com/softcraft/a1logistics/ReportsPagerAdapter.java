package com.softcraft.a1logistics;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ReportsPagerAdapter extends FragmentStateAdapter {

    public ReportsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new OverviewFragment();
            case 1:
                return new RevenueFragment();
            case 2:
                return new PackagesFragment();
            case 3:
                return new MerchantsFragment();
            default:
                return new OverviewFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}