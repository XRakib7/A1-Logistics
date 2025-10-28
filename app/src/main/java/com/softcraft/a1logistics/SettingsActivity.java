package com.softcraft.a1logistics;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.animation.DecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends BaseActivity {

    private SharedPreferences prefs;
    private SwitchMaterial darkModeSwitch, notificationsSwitch, autoSyncSwitch;
    private SeekBar dataRetentionSeekbar;
    private TextView dataRetentionText, appVersionText, lastSyncText;
    private MaterialCardView profileCard, securityCard, aboutCard, helpCard, logoutCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        initializeViews();
        loadCurrentSettings();
        setupClickListeners();
    }

    private void initializeViews() {
        // Setup toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize switches
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        notificationsSwitch = findViewById(R.id.notificationsSwitch);
        autoSyncSwitch = findViewById(R.id.autoSyncSwitch);

        // Initialize seekbar and text views
        dataRetentionSeekbar = findViewById(R.id.dataRetentionSeekbar);
        dataRetentionText = findViewById(R.id.dataRetentionText);
        appVersionText = findViewById(R.id.appVersionText);
        lastSyncText = findViewById(R.id.lastSyncText);

        // Initialize cards
        profileCard = findViewById(R.id.profileCard);
        securityCard = findViewById(R.id.securityCard);
        aboutCard = findViewById(R.id.aboutCard);
        helpCard = findViewById(R.id.helpCard);
        logoutCard = findViewById(R.id.logoutCard);

        // Set app version
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            appVersionText.setText(String.format("Version %s", versionName));
        } catch (Exception e) {
            appVersionText.setText("Version 1.0.0");
        }
        // Set appropriate title based on role
        Map<String, String> user = getCurrentUser();
        if (user != null) {
            String role = user.get("role");
            String title = "Admin".equals(role) ? "Admin Settings" : "Settings";
            getSupportActionBar().setTitle(title);
        }
        // Set last sync time
        updateLastSyncTime();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // No menu for settings
        return false;
    }
    private void loadCurrentSettings() {
        // Load dark mode setting
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        darkModeSwitch.setChecked(isDarkMode);

        // Load notification setting
        boolean notificationsEnabled = prefs.getBoolean("notifications", true);
        notificationsSwitch.setChecked(notificationsEnabled);

        // Load auto sync setting
        boolean autoSyncEnabled = prefs.getBoolean("auto_sync", true);
        autoSyncSwitch.setChecked(autoSyncEnabled);

        // Load data retention setting
        int dataRetention = prefs.getInt("data_retention", 12);
        dataRetentionSeekbar.setProgress(dataRetention);
        updateDataRetentionText(dataRetention);
    }

    private void setupClickListeners() {
        // Dark mode switch
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("dark_mode", isChecked);
            applyDarkMode(isChecked);
            animateBackgroundChange(isChecked);
        });

        // Notifications switch
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("notifications", isChecked);
            showConfirmation("Notifications " + (isChecked ? "enabled" : "disabled"));
        });

        // Auto sync switch
        autoSyncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("auto_sync", isChecked);
            showConfirmation("Auto sync " + (isChecked ? "enabled" : "disabled"));
        });

        // Data retention seekbar
        dataRetentionSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateDataRetentionText(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                saveSetting("data_retention", seekBar.getProgress());
                showConfirmation("Data retention set to " + seekBar.getProgress() + " months");
            }
        });

        // Profile card
        profileCard.setOnClickListener(v -> {
            animateCardClick(profileCard);
            startActivity(new Intent(this, ProfileActivity.class));
        });

        // Security card
        securityCard.setOnClickListener(v -> {
            animateCardClick(securityCard);
            showSecurityDialog();
        });

        // About card
        aboutCard.setOnClickListener(v -> {
            animateCardClick(aboutCard);
            showAboutDialog();
        });

        // Help card
        helpCard.setOnClickListener(v -> {
            animateCardClick(helpCard);
            showHelpOptions();
        });

        // Logout card
        logoutCard.setOnClickListener(v -> {
            animateCardClick(logoutCard);
            showLogoutConfirmation();
        });
    }

    private void applyDarkMode(boolean enabled) {
        int mode = enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(mode);

        // Save theme preference
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("dark_mode", enabled);
        editor.apply();
    }

    private void animateBackgroundChange(boolean darkMode) {
        int colorFrom = darkMode ? Color.WHITE : Color.BLACK;
        int colorTo = darkMode ? Color.BLACK : Color.WHITE;

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(1000);
        colorAnimation.addUpdateListener(animator -> {
            // This would typically animate the root layout background
        });
        colorAnimation.start();
    }

    private void animateCardClick(MaterialCardView card) {
        card.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() -> card.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start())
                .start();
    }

    private void updateDataRetentionText(int months) {
        String text;
        if (months == 0) {
            text = "Keep data: Forever";
        } else if (months == 1) {
            text = "Keep data: 1 month";
        } else if (months >= 60) {
            text = "Keep data: 5+ years";
        } else {
            text = String.format("Keep data: %d months", months);
        }
        dataRetentionText.setText(text);
    }

    private void updateLastSyncTime() {
        String lastSync = prefs.getString("last_sync", null);
        if (lastSync == null) {
            lastSync = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
            prefs.edit().putString("last_sync", lastSync).apply();
        }
        lastSyncText.setText("Last sync: " + lastSync);
    }

    private void saveSetting(String key, Object value) {
        SharedPreferences.Editor editor = prefs.edit();

        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        }

        editor.apply();
    }

    private void showConfirmation(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Setting Updated")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSecurityDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Security Settings")
                .setItems(new String[]{"Change Password", "Two-Factor Authentication", "Session Management"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showChangePasswordDialog();
                            break;
                        case 1:
                            showTwoFactorDialog();
                            break;
                        case 2:
                            showSessionManagement();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("About A1Logistics")
                .setMessage("A1Logistics Admin Panel v1.0\n\n" +
                        "A comprehensive logistics management solution for efficient package tracking, " +
                        "revenue analytics, and merchant management.\n\n" +
                        "Built with modern Android architecture and real-time Firebase integration.")
                .setPositiveButton("Visit Website", (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://a1logistics.site"));
                    startActivity(browserIntent);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showHelpOptions() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Get Help")
                .setItems(new String[]{
                        "User Guide",
                        "Contact Support",
                        "Report Issue",
                        "Feature Request"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showUserGuide();
                            break;
                        case 1:
                            contactSupport();
                            break;
                        case 2:
                            reportIssue();
                            break;
                        case 3:
                            requestFeature();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePasswordDialog() {
        // Implement password change logic
        showConfirmation("Password change feature coming soon");
    }

    private void showTwoFactorDialog() {
        showConfirmation("Two-factor authentication setup");
    }

    private void showSessionManagement() {
        showConfirmation("Session management options");
    }

    private void showUserGuide() {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://a1logistics.site"));
        startActivity(intent);
    }

    private void contactSupport() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:ezio.wizard@gmail.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "A1Logistics Support Request");
        startActivity(intent);
    }

    private void reportIssue() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:ezio.wizard@gmail.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Bug Report - A1Logistics Admin");
        startActivity(intent);
    }

    private void requestFeature() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:ezio.wizard@gmail.com"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Feature Request - A1Logistics Admin");
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}