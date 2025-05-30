package com.softcraft.a1logistics;

import android.content.Context;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

public class UserUtils {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface UserDetailsCallback {
        void onSuccess(String name, String role);
        void onFailure();
    }

    public static void fetchUserDetails(String uid, UserDetailsCallback callback) {
        // First check Admins collection
        db.collection("Admins").document(uid).get()
                .addOnCompleteListener(adminTask -> {
                    if (adminTask.isSuccessful() && adminTask.getResult().exists()) {
                        DocumentSnapshot adminDoc = adminTask.getResult();
                        String name = adminDoc.getString("adminName");
                        callback.onSuccess(name, "Admin");
                    } else {
                        // If not admin, check Merchants
                        db.collection("Merchants").document(uid).get()
                                .addOnCompleteListener(merchantTask -> {
                                    if (merchantTask.isSuccessful() && merchantTask.getResult().exists()) {
                                        DocumentSnapshot merchantDoc = merchantTask.getResult();
                                        String name = merchantDoc.getString("businessName");
                                        callback.onSuccess(name, "Merchant");
                                    } else {
                                        callback.onFailure();
                                    }
                                });
                    }
                });
    }

    public static void showUserDetails(Context context, String uid) {
        fetchUserDetails(uid, new UserDetailsCallback() {
            @Override
            public void onSuccess(String name, String role) {
                new AlertDialog.Builder(context)
                        .setTitle(role + " Details")
                        .setMessage("Name: " + name + "\nUID: " + uid)
                        .setPositiveButton("OK", null)
                        .show();
            }

            @Override
            public void onFailure() {
                Toast.makeText(context,
                        "Could not fetch user details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static boolean isAdmin(Map<String, String> user) {
        return user != null && "Admin".equals(user.get("role"));
    }
}