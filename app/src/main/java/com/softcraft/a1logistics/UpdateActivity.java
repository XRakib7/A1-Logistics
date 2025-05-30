package com.softcraft.a1logistics;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.io.File;

public class UpdateActivity extends AppCompatActivity implements SplashActivity.DownloadCallback {
    private ProgressBar progressBar;
    private ProgressBar circularProgress;
    private Button downloadButton;
    private Button btn_settings;
    private Button installButton;
    private Button cancelButton;
    private TextView statusText;
    private TextView progressText;
    private TextView instalinstruction;
    private TextView releaseNotesText;
    private TextView downloadStatsText;
    private MaterialCardView updateCard;
    private String downloadUrl;
    private File downloadedApk;
    private boolean isMandatory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        progressBar = findViewById(R.id.progressBar);
        circularProgress = findViewById(R.id.circularProgress);
        downloadButton = findViewById(R.id.downloadButton);
        cancelButton = findViewById(R.id.cancelButton);
        btn_settings = findViewById(R.id.btn_settings);
        installButton = findViewById(R.id.installButton);
        statusText = findViewById(R.id.statusText);
        progressText = findViewById(R.id.progressText);
        releaseNotesText = findViewById(R.id.releaseNotesText);
        downloadStatsText = findViewById(R.id.downloadStatsText);
        updateCard = findViewById(R.id.updateCard);
        instalinstruction = findViewById(R.id.instalinstruction);

        downloadUrl = getIntent().getStringExtra("downloadUrl");
        isMandatory = getIntent().getBooleanExtra("isMandatory", false);
        String releaseNotes = getIntent().getStringExtra("releaseNotes");

        setupUI(releaseNotes);

        downloadButton.setOnClickListener(v -> startDownload());
        btn_settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)));
        installButton.setOnClickListener(v -> {
            SplashActivity.installApk(downloadedApk);

        });

        // Check if update file already exists
        downloadedApk = new File(getExternalFilesDir(null), "update.apk");
        if (downloadedApk.exists()) {
            onDownloadComplete(downloadedApk);
        }
    }

    private void setupUI(String releaseNotes) {
        downloadButton.setText(isMandatory ? "Download Now" : "Download Update");
        cancelButton.setVisibility(View.VISIBLE);
        cancelButton.setOnClickListener(v -> finish());
        releaseNotesText.setText(releaseNotes != null ? releaseNotes : "No release notes available");
        installButton.setEnabled(false);
        progressBar.setVisibility(View.GONE);
        circularProgress.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
        downloadStatsText.setVisibility(View.GONE);
    }


    private void startDownload() {
        downloadButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        circularProgress.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        downloadButton.setVisibility(View.GONE);
        downloadStatsText.setVisibility(View.VISIBLE);
        statusText.setText("Downloading update...");
        SplashActivity.downloadApk(downloadUrl, this);
    }

    @Override
    public void onDownloadProgress(int progress, long downloadedBytes, long totalBytes, double speed) {
        progressBar.setProgress(progress);
        circularProgress.setProgress(progress);
        progressText.setText(String.format("%d%%", progress));

        // Format file size
        String sizeInfo;
        if (totalBytes > 0) {
            sizeInfo = formatFileSize(downloadedBytes) + " / " + formatFileSize(totalBytes);
        } else {
            sizeInfo = formatFileSize(downloadedBytes);
        }

        String speedInfo = formatSpeed(speed).replace("/s", "") + "/s";

        downloadStatsText.setText(String.format("%s\n%s", speedInfo, sizeInfo));
        installButton.setVisibility(View.GONE);
    }

    @Override
    public void onDownloadComplete(File apkFile) {
        downloadedApk = apkFile;
        progressBar.setVisibility(View.GONE);
        circularProgress.setVisibility(View.GONE);
        statusText.setText("Download complete!");
        progressText.setText("Ready to install");
        downloadStatsText.setText(formatFileSize(apkFile.length())); // Show final size
        installButton.setEnabled(true);
        installButton.setVisibility(View.VISIBLE);
        instalinstruction.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDownloadFailed(String error) {
        progressBar.setVisibility(View.GONE);
        circularProgress.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
        downloadStatsText.setVisibility(View.GONE);
        statusText.setText("Download failed: Please turn your internet ON");
        downloadButton.setEnabled(true);
        instalinstruction.setVisibility(View.GONE);
        installButton.setVisibility(View.GONE);
        btn_settings.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (!isMandatory) {
            super.onBackPressed();
        }
        // Block back button for mandatory updates
    }

    // Helper method to format file size
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    // Helper method to format speed
    private String formatSpeed(double bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return String.format("%.0f B/s", bytesPerSecond);
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0));
        }
    }
}