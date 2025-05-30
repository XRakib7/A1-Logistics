package com.softcraft.a1logistics;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllMerchantsActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private LottieAnimationView progressLoader;
    private MerchantAdapter adapter;
    private final List<Map<String, Object>> merchantsList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_merchants);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerView);
        progressLoader = findViewById(R.id.progressBar);
        progressLoader.setVisibility(View.GONE);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MerchantAdapter(merchantsList);
        recyclerView.setAdapter(adapter);

        loadMerchants();
    }

    private void loadMerchants() {
        progressLoader.setVisibility(View.VISIBLE);
        progressLoader.playAnimation();
        recyclerView.setVisibility(View.GONE);

        db.collection("Merchants").get()
                .addOnCompleteListener(task -> {
                    progressLoader.setVisibility(View.GONE);
                    progressLoader.pauseAnimation();

                    if (task.isSuccessful()) {
                        merchantsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            merchantsList.add(document.getData());
                        }

                        if (merchantsList.isEmpty()) {
                            Toast.makeText(AllMerchantsActivity.this,
                                    "No merchants found", Toast.LENGTH_SHORT).show();
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(AllMerchantsActivity.this,
                                "Error loading merchants: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}