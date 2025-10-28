package com.softcraft.a1logistics;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userRole;
    private String userId;

    // Views
    private ImageView profileImage;
    private MaterialButton editProfileButton;
    private FloatingActionButton editProfileImageButton;
    private TextView userNameText, userRoleText, userEmailText, phoneNumberText;
    private TextView businessNameText, registrationDateText, lastLoginText;
    private LinearLayout businessNameLayout;
    private CardView changePasswordCard, notificationSettingsCard;
    private CardView statisticsCard;
    private TextView totalPackagesStat, successRateStat, totalRevenueStat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get current user
        Map<String, String> user = getCurrentUser();
        if (user == null) {
            logout();
            return;
        }

        userRole = user.get("role");
        userId = user.get("uid");

        initializeViews();
        setupClickListeners();
        loadUserProfile();

        // Show/hide merchant-specific sections
        setupRoleSpecificViews();
    }

    private void initializeViews() {
        // Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("My Profile");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Profile views
        profileImage = findViewById(R.id.profileImage);
        editProfileButton = findViewById(R.id.editProfileButton);
        editProfileImageButton = findViewById(R.id.editProfileImageButton);
        userNameText = findViewById(R.id.userNameText);
        userRoleText = findViewById(R.id.userRoleText);
        userEmailText = findViewById(R.id.userEmailText);
        phoneNumberText = findViewById(R.id.phoneNumberText);

        // Merchant-specific views
        businessNameLayout = findViewById(R.id.businessNameLayout);
        businessNameText = findViewById(R.id.businessNameText);

        // Common views
        registrationDateText = findViewById(R.id.registrationDateText);
        lastLoginText = findViewById(R.id.lastLoginText);

        // Action cards
        changePasswordCard = findViewById(R.id.changePasswordCard);
        notificationSettingsCard = findViewById(R.id.notificationSettingsCard);

        // Statistics (merchant only)
        statisticsCard = findViewById(R.id.statisticsCard);
        totalPackagesStat = findViewById(R.id.totalPackagesStat);
        successRateStat = findViewById(R.id.successRateStat);
        totalRevenueStat = findViewById(R.id.totalRevenueStat);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // No menu for settings
        return false;
    }
    private void setupRoleSpecificViews() {
        if ("Merchant".equals(userRole)) {
            // Show merchant-specific sections
            businessNameLayout.setVisibility(LinearLayout.VISIBLE);
            findViewById(R.id.businessDivider).setVisibility(LinearLayout.VISIBLE);
            findViewById(R.id.statisticsTitle).setVisibility(TextView.VISIBLE);
            statisticsCard.setVisibility(CardView.VISIBLE);

            // Load merchant statistics
            loadMerchantStatistics();
        } else {
            // Hide merchant-specific sections for admin
            businessNameLayout.setVisibility(LinearLayout.GONE);
            findViewById(R.id.businessDivider).setVisibility(LinearLayout.GONE);
            findViewById(R.id.statisticsTitle).setVisibility(TextView.GONE);
            statisticsCard.setVisibility(CardView.GONE);
        }
    }

    private void setupClickListeners() {
        editProfileButton.setOnClickListener(v -> openEditProfile());
        editProfileImageButton.setOnClickListener(v -> changeProfilePicture());
        changePasswordCard.setOnClickListener(v -> openChangePassword());
        notificationSettingsCard.setOnClickListener(v -> openNotificationSettings());
    }

    private void loadUserProfile() {
        String collectionName = "Admin".equals(userRole) ? "Admins" : "Merchants";

        db.collection(collectionName).document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        updateProfileUI(document);
                    }
                });
    }

    private void updateProfileUI(DocumentSnapshot document) {
        // Basic user info
        String name = document.getString("Admin".equals(userRole) ? "adminName" : "businessName");
        String email = document.getString("email");
        String phone = document.getString("phone");
        String businessName = document.getString("businessName");

        userNameText.setText(name != null ? name : "User");
        userRoleText.setText(userRole);
        userEmailText.setText(email != null ? email : "No email");
        phoneNumberText.setText(phone != null ? phone : "Not provided");

        if (businessName != null) {
            businessNameText.setText(businessName);
        }

        // Registration date and last login
        if (document.getTimestamp("createdAt") != null) {
            String regDate = new SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    .format(document.getTimestamp("createdAt").toDate());
            registrationDateText.setText(regDate);
        }

        if (document.getTimestamp("lastLogin") != null) {
            String lastLogin = formatLastLogin(document.getTimestamp("lastLogin").toDate());
            lastLoginText.setText(lastLogin);
        }
    }

    private void loadMerchantStatistics() {
        if (!"Merchant".equals(userRole)) return;

        // Load package statistics for merchant
        db.collection("PickupRequests")
                .whereEqualTo("merchantId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalPackages = queryDocumentSnapshots.size();
                    int delivered = 0;
                    int returned = 0;
                    double totalRevenue = 0.0;

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String status = document.getString("status");
                        Double codPrice = document.getDouble("codPrice");

                        if ("Delivered".equals(status)) {
                            delivered++;
                            if (codPrice != null) {
                                totalRevenue += codPrice;
                            }
                        } else if ("Returned".equals(status)) {
                            returned++;
                        }
                    }

                    updateStatisticsUI(totalPackages, delivered, returned, totalRevenue);
                });
    }

    private void updateStatisticsUI(int totalPackages, int delivered, int returned, double totalRevenue) {
        totalPackagesStat.setText(String.valueOf(totalPackages));
        totalRevenueStat.setText(String.format("৳%.0f", totalRevenue));

        // Calculate success rate
        if (delivered + returned > 0) {
            double successRate = (delivered * 100.0) / (delivered + returned);
            successRateStat.setText(String.format("%.1f%%", successRate));
        } else {
            successRateStat.setText("0%");
        }
    }

    private String formatLastLogin(Date lastLogin) {
        long diff = System.currentTimeMillis() - lastLogin.getTime();
        long minutes = diff / (60 * 1000);
        long hours = minutes / 60;
        long days = hours / 24;

        if (minutes < 1) return "Just now";
        else if (minutes < 60) return minutes + " minutes ago";
        else if (hours < 24) return hours + " hours ago";
        else return days + " days ago";
    }

    private void openEditProfile() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        intent.putExtra("userRole", userRole);
        startActivity(intent);
    }

    private void changeProfilePicture() {
        // Implement profile picture change logic
        Toast.makeText(this, "Profile picture change feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void openChangePassword() {
        Intent intent = new Intent(this, ChangePasswordActivity.class);
        startActivity(intent);
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}