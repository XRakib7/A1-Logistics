package com.softcraft.a1logistics;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.Timestamp;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DownloadUtils {
    private static final String TAG = "DownloadUtils";
    private static final String PACKAGES_FOLDER = "Packages";
    private static final int STORAGE_PERMISSION_CODE = 100;

    public static void downloadPackageDetails(Context context, Map<String, Object> packageData, boolean isAdmin) {
        if (!checkStoragePermission(context)) {
            return;
        }

        try {
            File packagesDir = getPackagesDirectory(context);
            if (!packagesDir.exists() && !packagesDir.mkdirs()) {
                throw new IOException("Failed to create directory");
            }

            String fileName = packageData.get("orderId") + ".csv";
            File file = new File(packagesDir, fileName);

            String[][] fields = getFieldsForUser(isAdmin);

            try (FileWriter writer = new FileWriter(file)) {
                writeHeaders(writer, fields);
                writePackageData(writer, fields, packageData, isAdmin);
                notifyFileCreated(context, file);
            }
        } catch (Exception e) {
            handleError(context, "Failed to download package", e);
        }
    }

    public static void downloadPackageList(Context context, List<Map<String, Object>> packages, boolean isAdmin) {
        if (!checkStoragePermission(context)) {
            return;
        }

        try {
            File packagesDir = getPackagesDirectory(context);
            if (!packagesDir.exists() && !packagesDir.mkdirs()) {
                throw new IOException("Failed to create directory");
            }

            String fileName = "Bulk-" + packages.size() + "Packages.csv";
            File file = new File(packagesDir, fileName);

            String[][] fields = getFieldsForUser(isAdmin);

            try (FileWriter writer = new FileWriter(file)) {
                writeHeaders(writer, fields);
                for (Map<String, Object> pkg : packages) {
                    writePackageData(writer, fields, pkg, isAdmin);
                }
                notifyFileCreated(context, file);
            }
        } catch (Exception e) {
            handleError(context, "Failed to download packages", e);
        }
    }

    private static boolean checkStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No permission needed for app-specific storage on Android 10+
            return true;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        if (context instanceof Activity) {
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_CODE);
        } else {
            Toast.makeText(context, "Storage permission required", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private static File getPackagesDirectory(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use app-specific directory on Android 10+
            return new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), PACKAGES_FOLDER);
        } else {
            // Use public Downloads directory for older versions
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    PACKAGES_FOLDER);
        }
    }

    private static String[][] getFieldsForUser(boolean isAdmin) {
        if (isAdmin) {
            return new String[][]{
                    {"orderId", "Order ID"},
                    {"customerName", "Customer Name"},
                    {"customerNumber", "Phone"},
                    {"deliveryLocation", "Delivery Location"},
                    {"codPrice", "COD Price"},
                    {"status", "Status"},
                    {"pickupLocation", "Pickup Location"},
                    {"merchantId", "Merchant ID"},
                    {"merchantName", "Merchant Name"},
                    {"packageDetails", "Package Details"},
                    {"packageWeight", "Weight"},
                    {"createdDate", "Created Date"}
            };
        } else {
            return new String[][]{
                    {"orderId", "Order ID"},
                    {"customerName", "Customer Name"},
                    {"customerNumber", "Phone"},
                    {"deliveryLocation", "Delivery Location"},
                    {"codPrice", "COD Price"},
                    {"status", "Status"},
                    {"pickupLocation", "Pickup Location"},
                    {"packageDetails", "Package Details"},
                    {"packageWeight", "Weight"},
                    {"createdDate", "Created Date"}
            };
        }
    }

    private static void writeHeaders(FileWriter writer, String[][] fields) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            writer.append('"').append(fields[i][1]).append('"');
            if (i < fields.length - 1) {
                writer.append(",");
            }
        }
        writer.append("\n");
    }

    private static void writePackageData(FileWriter writer, String[][] fields, Map<String, Object> packageData,
                                         boolean isAdmin) throws IOException {
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i][0];
            Object value = packageData.get(fieldName);

            writer.append('"');
            if ("customerNumber".equals(fieldName)) {
                writer.append(getSafeString(value));
            } else if ("codPrice".equals(fieldName)) {
                writer.append("BDT ").append(getSafeString(value));
            } else if ("packageWeight".equals(fieldName)) {
                writer.append(getSafeString(value)).append(" KG");
            } else if ("createdDate".equals(fieldName)) {
                writer.append(formatDate(value));
            } else {
                writer.append(getSafeString(value));
            }
            writer.append('"');

            if (i < fields.length - 1) {
                writer.append(",");
            }
        }
        writer.append("\n");
    }

    private static String formatDate(Object dateObj) {
        if (dateObj == null) return "N/A";
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (dateObj instanceof Timestamp) {
            return dateFormat.format(((Timestamp) dateObj).toDate());
        } else if (dateObj instanceof Date) {
            return dateFormat.format((Date) dateObj);
        }
        return "N/A";
    }

    private static String getSafeString(Object value) {
        return value != null ? value.toString() : "N/A";
    }

    private static void notifyFileCreated(Context context, File file) {
        MediaScannerConnection.scanFile(context,
                new String[]{file.getAbsolutePath()},
                null,
                (path, uri) -> {
                    Log.d(TAG, "File scanned: " + path);
                    Toast.makeText(context,
                            "File saved to " + file.getParentFile().getName() + "/" + file.getName(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private static void handleError(Context context, String message, Exception e) {
        Log.e(TAG, message, e);
        Toast.makeText(context, message + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
}