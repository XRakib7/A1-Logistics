package com.softcraft.a1logistics;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreatePickupActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String merchantId, businessName, pickupLocation;
    private EditText customerNameEditText, customerNumberEditText,
            deliveryLocationEditText, codPriceEditText, packageValueEditText,
            itemQuantityEditText, packageDetailsEditText, weightEditText, pickupLocationEditText;
    private Button submitButton;
    private UIBlocker uiBlocker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pickup);
        uiBlocker = new UIBlocker(this);

        db = FirebaseFirestore.getInstance();

        // Initialize views
        customerNameEditText = findViewById(R.id.customerNameEditText);
        customerNumberEditText = findViewById(R.id.customerNumberEditText);
        deliveryLocationEditText = findViewById(R.id.deliveryLocationEditText);
        codPriceEditText = findViewById(R.id.codPriceEditText);
        packageValueEditText = findViewById(R.id.packageValueEditText);
        itemQuantityEditText = findViewById(R.id.itemQuantityEditText);
        packageDetailsEditText = findViewById(R.id.packageDetailsEditText);
        weightEditText = findViewById(R.id.weightEditText);
        pickupLocationEditText = findViewById(R.id.pickupLocationEditText);
        submitButton = findViewById(R.id.submitButton);

        // Get merchant details from Firestore
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        uiBlocker.blockUI("Loading merchant details...");
        db.collection("Merchants")
                .whereEqualTo("email", FirebaseAuth.getInstance().getCurrentUser().getEmail())
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    uiBlocker.unblockUI();
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot merchantDoc = task.getResult().getDocuments().get(0);
                        merchantId = merchantDoc.getId();
                        businessName = merchantDoc.getString("businessName");
                        pickupLocation = merchantDoc.getString("pickupLocation");
                        pickupLocationEditText.setText(pickupLocation);
                    } else {
                        uiBlocker.showError("Failed to load merchant data");
                    }
                });

        submitButton.setOnClickListener(v -> createPickupRequest());
    }

    private void createPickupRequest() {
        String customerName = customerNameEditText.getText().toString().trim();
        String customerNumber = customerNumberEditText.getText().toString().trim();
        String deliveryLocation = deliveryLocationEditText.getText().toString().trim();
        String codPrice = codPriceEditText.getText().toString().trim();
        String packageValue = packageValueEditText.getText().toString().trim();
        String itemQuantity = itemQuantityEditText.getText().toString().trim();
        String packageDetails = packageDetailsEditText.getText().toString().trim();
        String packageWeight = weightEditText.getText().toString().trim();
        String pickupLocation = pickupLocationEditText.getText().toString().trim();

        if (validateInputs(customerName, customerNumber, deliveryLocation, codPrice,
                packageValue, itemQuantity, packageWeight, packageDetails, pickupLocation)) {
            uiBlocker.blockUI("Creating pickup request...");
            submitButton.setEnabled(false);

            // Get next order number
            getNextOrderNumber(merchantId, new OrderNumberCallback() {
                @Override
                public void onSuccess(String orderId) {
                    // Create pickup request data
                    Map<String, Object> pickupRequest = new HashMap<>();
                    pickupRequest.put("orderId", orderId);
                    pickupRequest.put("merchantId", merchantId);
                    pickupRequest.put("customerName", customerName);
                    pickupRequest.put("customerNumber", customerNumber);
                    pickupRequest.put("deliveryLocation", deliveryLocation);
                    pickupRequest.put("codPrice", Double.parseDouble(codPrice));
                    pickupRequest.put("packageValue", packageValue.isEmpty() ? 0 : Double.parseDouble(packageValue));
                    pickupRequest.put("itemQuantity", itemQuantity.isEmpty() ? 1 : Integer.parseInt(itemQuantity));
                    pickupRequest.put("packageDetails", packageDetails);
                    pickupRequest.put("packageWeight", Double.parseDouble(packageWeight));
                    pickupRequest.put("pickupLocation", pickupLocation);
                    pickupRequest.put("merchantName", businessName);
                    pickupRequest.put("status", "Pickup Pending");
                    pickupRequest.put("lastUpdate", FieldValue.serverTimestamp());
                    pickupRequest.put("createdDate", FieldValue.serverTimestamp());

                    // Add initial status history
                    Map<String, Object> initialStatus = new HashMap<>();
                    initialStatus.put("status", "Pickup Pending");
                    initialStatus.put("updateTime", new Date());
                    initialStatus.put("updatedBy", merchantId);

                    pickupRequest.put("statusHistory", List.of(initialStatus));

                    // Add to Firestore
                    db.collection("PickupRequests").document(orderId)
                            .set(pickupRequest)
                            .addOnCompleteListener(task -> {
                                uiBlocker.unblockUI();
                                if (task.isSuccessful()) {
                                    showConfirmationDialog(orderId, customerName, customerNumber,
                                            deliveryLocation, pickupLocation, codPrice);
                                } else {
                                    submitButton.setEnabled(true);
                                    uiBlocker.showError("Failed to create pickup");
                                }
                            });
                }

                @Override
                public void onFailure(Exception e) {
                    uiBlocker.unblockUI();
                    submitButton.setEnabled(true);
                    uiBlocker.showError("Failed to generate order ID");
                }
            });
        }
    }
    private void showConfirmationDialog(String orderId, String customerName,
                                        String customerNumber, String deliveryLocation,
                                        String pickupLocation, String codPrice) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_order_confirmation, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        // Set the dialog data
        TextView tvOrderId = dialogView.findViewById(R.id.tvOrderId);
        TextView tvCustomerName = dialogView.findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = dialogView.findViewById(R.id.tvCustomerPhone);
        TextView tvDeliveryLocation = dialogView.findViewById(R.id.tvDeliveryLocation);
        TextView tvPickupLocation = dialogView.findViewById(R.id.tvPickupLocation);
        TextView tvCodAmount = dialogView.findViewById(R.id.tvCodAmount);
        Button btnDone = dialogView.findViewById(R.id.btnDone);

        tvOrderId.setText(String.format("Order ID: %s", orderId));
        tvCustomerName.setText(String.format("Customer: %s", customerName));
        tvCustomerPhone.setText(String.format("Phone: %s", customerNumber));
        tvDeliveryLocation.setText(String.format("Delivery To: %s", deliveryLocation));
        tvPickupLocation.setText(String.format("Pickup From: %s", pickupLocation));
        tvCodAmount.setText(String.format("COD Amount: ৳%s", codPrice));

        btnDone.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        dialog.show();
    }

    private boolean validateInputs(String customerName, String customerNumber,
                                   String deliveryLocation, String codPrice, String packageValue,
                                   String itemQuantity, String packageWeight,
                                   String packageDetails, String pickupLocation) {
        if (customerName.isEmpty()) {
            customerNameEditText.setError("Customer name is required");
            return false;
        }

        if (customerNumber.isEmpty() || customerNumber.length() < 10) {
            customerNumberEditText.setError("Valid phone number is required");
            return false;
        }

        if (deliveryLocation.isEmpty()) {
            deliveryLocationEditText.setError("Delivery location is required");
            return false;
        }

        if (codPrice.isEmpty()) {
            codPriceEditText.setError("COD price is required");
            return false;
        }

        if (packageDetails.isEmpty()) {
            packageDetailsEditText.setError("Package details are required");
            return false;
        }

        if (packageWeight.isEmpty()) {
            weightEditText.setError("Package weight is required");
            return false;
        }

        if (pickupLocation.isEmpty()) {
            pickupLocationEditText.setError("Pickup location is required");
            return false;
        }

        return true;
    }

    private interface OrderNumberCallback {
        void onSuccess(String orderId);
        void onFailure(Exception e);
    }

    private void getNextOrderNumber(String merchantId, OrderNumberCallback callback) {
        DocumentReference counterRef = db.collection("OrderCounters").document(merchantId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(counterRef);
            long nextNumber = 1;

            if (snapshot.exists()) {
                nextNumber = snapshot.getLong("lastOrderNumber") + 1;
            }

            // Update the counter
            Map<String, Object> updates = new HashMap<>();
            updates.put("lastOrderNumber", nextNumber);
            transaction.set(counterRef, updates, SetOptions.merge());

            // Format the order ID (A1-M001-0001)
            return "A1-" + merchantId + "-" + String.format(Locale.getDefault(), "%04d", nextNumber);
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess(task.getResult());
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (uiBlocker != null) {
            uiBlocker.unblockUI();
        }
        super.onDestroy();
    }
}