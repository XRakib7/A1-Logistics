package com.softcraft.a1logistics;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private static final String VERSION_COLLECTION = "app_updates";
    private static final String VERSION_DOCUMENT = "latest_version";
    private static final int MAX_RETRY_COUNT = 3;

    private FirebaseFirestore db;
    private static SplashActivity instance;
    private int retryCount = 0;
    private RelativeLayout mainContent;
    private RelativeLayout errorContainer;
    private LottieAnimationView loadingAnimation;
    private Button btnRetry, btnSettings, btnExit;
    private TextView errorMessage, errorDescription;
    private static final String PREFS_NAME = "A1LogisticsPrefs";
    private static final String USER_KEY = "currentUser";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        instance = this;

        mainContent = findViewById(R.id.main_content);
        errorContainer = findViewById(R.id.error_container);
        loadingAnimation = findViewById(R.id.loading_animation);
        btnRetry = findViewById(R.id.btn_retry);
        btnSettings = findViewById(R.id.btn_settings);
        btnExit = findViewById(R.id.btn_exit);
        errorMessage = findViewById(R.id.error_message);
        errorDescription = findViewById(R.id.error_description);

        // Set up button listeners
        btnRetry.setOnClickListener(v -> checkNetworkAndProceed());
        btnSettings.setOnClickListener(v -> openNetworkSettings());
        btnExit.setOnClickListener(v -> finishAffinity());

        checkNetworkAndProceed();
    }

    private void checkNetworkAndProceed() {
        if (isNetworkAvailable()) {
            // Online: Show loading, check for updates
            mainContent.setVisibility(View.VISIBLE);
            errorContainer.setVisibility(View.GONE);
            loadingAnimation.playAnimation();

            db = FirebaseFirestore.getInstance();
            checkForUpdates();
        } else {
            // Offline: Show error (stays until retry/exit)
            showNetworkError();
        }
    }

    private void showNetworkError() {
        mainContent.setVisibility(View.GONE);
        errorContainer.setVisibility(View.VISIBLE);
        loadingAnimation.pauseAnimation();

        errorMessage.setText(R.string.network_error_title);
        errorDescription.setText(R.string.network_error_description);
    }

    private void openNetworkSettings() {
        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkForUpdates() {
        db.collection(VERSION_COLLECTION)
                .document(VERSION_DOCUMENT)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            processVersionCheck(document);
                        } else {
                            handleVersionCheckError("Version document not found");
                        }
                    } else {
                        handleVersionCheckError("Error fetching version: " + task.getException());
                    }
                });
    }

    private void processVersionCheck(DocumentSnapshot document) {
        try {
            int latestVersionCode = document.getLong("versionCode").intValue();
            String downloadUrl = document.getString("downloadUrl");
            boolean forceUpdate = Boolean.TRUE.equals(document.getBoolean("forceUpdate"));
            String releaseNotes = document.getString("releaseNotes");
            int currentVersionCode = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionCode;

            if (latestVersionCode > currentVersionCode) {
                proceedToUpdateActivity(downloadUrl, releaseNotes, forceUpdate);
            } else {
                proceedToMain(); // No delay, proceed immediately
            }
        } catch (Exception e) {
            handleVersionCheckError("Error parsing version data: " + e.getMessage());
        }
    }

    private void handleVersionCheckError(String error) {
        Log.e(TAG, error);
        if (retryCount < MAX_RETRY_COUNT && isNetworkAvailable()) {
            retryCount++;
            new Handler(Looper.getMainLooper()).postDelayed(this::checkForUpdates, 2000);
        } else {
            if (isNetworkAvailable()) {
                proceedToMain(); // Proceed if online (even if update check failed)
            } else {
                showNetworkError(); // Stay on error if offline
            }
        }
    }

    private void proceedToMain() {
        if (isUserLoggedIn()) {
            // User is logged in - proceed to appropriate dashboard
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String userJson = prefs.getString(USER_KEY, null);
            Map<String, String> userData = new Gson().fromJson(userJson, Map.class);
            String role = userData.get("role");

            Intent intent = "Admin".equals(role) ?
                    new Intent(this, AdminDashboardActivity.class) :
                    new Intent(this, MerchantDashboardActivity.class);

            startActivity(intent);
        } else {
            // User is not logged in - go to main activity
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(USER_KEY, null) != null;
    }
    private void proceedToUpdateActivity(String downloadUrl, String releaseNotes, boolean isMandatory) {
        Intent intent = new Intent(this, UpdateActivity.class);
        intent.putExtra("downloadUrl", downloadUrl);
        intent.putExtra("releaseNotes", releaseNotes);
        intent.putExtra("isMandatory", isMandatory);
        startActivity(intent);
        finish();
    }

    public static void downloadApk(String apkUrl, DownloadCallback callback) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            InputStream input = null;
            FileOutputStream output = null;
            long startTime = System.currentTimeMillis();
            long lastUpdateTime = startTime;
            long lastBytes = 0;

            try {
                URL url = new URL(apkUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Server returned HTTP " + connection.getResponseCode());
                }

                File apkFile = new File(instance.getExternalFilesDir(null), "update.apk");
                if (apkFile.exists()) {
                    apkFile.delete();
                }

                input = connection.getInputStream();
                output = new FileOutputStream(apkFile);

                byte[] buffer = new byte[4096];
                int count;
                long total = 0;
                final int fileLength = connection.getContentLength(); // Make final

                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                    total += count;

                    // Calculate download speed every second
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastUpdateTime >= 1000 || total == fileLength) {
                        final long currentTotal = total; // Create final copy
                        final long currentFileLength = fileLength; // Create final copy
                        double speed = (currentTotal - lastBytes) * 1000.0 / (currentTime - lastUpdateTime);
                        lastBytes = currentTotal;
                        lastUpdateTime = currentTime;

                        final int progress = fileLength > 0 ? (int) (currentTotal * 100 / currentFileLength) : 0;
                        final double finalSpeed = speed; // Create final copy

                        instance.runOnUiThread(() -> callback.onDownloadProgress(
                                progress, currentTotal, currentFileLength, finalSpeed));
                    }
                }

                instance.runOnUiThread(() -> callback.onDownloadComplete(apkFile));

            } catch (IOException e) {
                Log.e(TAG, "Download failed", e);
                final String error = e.getMessage(); // Create final copy
                instance.runOnUiThread(() -> callback.onDownloadFailed(error));
            } finally {
                try {
                    if (output != null) output.close();
                    if (input != null) input.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException ignored) {}
            }
        }).start();
    }

    public static void installApk(File apkFile) {
        if (apkFile != null && apkFile.exists()) {
            try {
                Uri apkUri = FileProvider.getUriForFile(
                        instance,
                        instance.getPackageName() + ".provider",
                        apkFile);

                Intent installIntent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
                installIntent.setData(apkUri);
                installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                installIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);

                instance.startActivityForResult(installIntent, 1001);

            } catch (Exception e) {
                Toast.makeText(instance, "Installation failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                // Delete file if installation fails
                if (apkFile.exists()) {
                    apkFile.delete();
                }
            }
        } else {
            Toast.makeText(instance, "APK file not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            // After installation attempt, delete the file
            File apkFile = new File(getExternalFilesDir(null), "update.apk");
            if (apkFile.exists()) {
                apkFile.delete();
            }
        }
    }

    public interface DownloadCallback {
        void onDownloadProgress(int progress, long downloadedBytes, long totalBytes, double speed);
        void onDownloadComplete(File apkFile);
        void onDownloadFailed(String error);
    }

}