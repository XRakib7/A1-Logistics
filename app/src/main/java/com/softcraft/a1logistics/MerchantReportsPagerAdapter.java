package com.softcraft.a1logistics;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MerchantReportsPagerAdapter extends FragmentStateAdapter {

    private String merchantId;

    public MerchantReportsPagerAdapter(@NonNull FragmentActivity fragmentActivity, String merchantId) {
        super(fragmentActivity);
        this.merchantId = merchantId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return MerchantOverviewFragment.newInstance(merchantId);
            case 1:
                return MerchantRevenueFragment.newInstance(merchantId);
            case 2:
                return MerchantPackagesFragment.newInstance(merchantId);
            default:
                return MerchantOverviewFragment.newInstance(merchantId);
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}