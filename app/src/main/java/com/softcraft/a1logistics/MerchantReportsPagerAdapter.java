package com.softcraft.a1logistics;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MerchantReportsPagerAdapter extends FragmentStateAdapter {

    private final String merchantId;

    public MerchantReportsPagerAdapter(@NonNull FragmentActivity fragmentActivity, String merchantId) {
        super(fragmentActivity);
        // Add validation to ensure merchantId is not null
        this.merchantId = merchantId != null ? merchantId : "";
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                // Use MerchantOverviewFragment for merchant-specific overview
                return MerchantOverviewFragment.newInstance(merchantId);
            case 1:
                // Use MerchantRevenueFragment for merchant-specific revenue data
                return MerchantRevenueFragment.newInstance(merchantId);
            case 2:
                // Use MerchantPackagesFragment for merchant-specific packages data
                return MerchantPackagesFragment.newInstance(merchantId);
            default:
                // Fallback to merchant overview
                return MerchantOverviewFragment.newInstance(merchantId);
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Only 3 tabs for merchant view: Overview, Revenue, Packages
    }
}