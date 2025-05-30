package com.softcraft.a1logistics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneVerificationActivity extends AppCompatActivity {
    private static final String TAG = "PhoneVerification";

    private TextView phoneNumberText;
    private TextInputEditText verificationCodeEditText;
    private Button verifyButton, resendCodeButton;
    private LottieAnimationView networkLoader;
    private FrameLayout networkOverlay;

    private FirebaseAuth mAuth;
    private String verificationId;
    private String phoneNumber;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verification);

        mAuth = FirebaseAuth.getInstance();
        phoneNumber = getIntent().getStringExtra("phoneNumber");

        initializeViews();
        setupListeners();

        // Start verification process
        sendVerificationCode(phoneNumber);
    }

    private void initializeViews() {
        phoneNumberText = findViewById(R.id.phoneNumberText);
        verificationCodeEditText = findViewById(R.id.verificationCodeEditText);
        verifyButton = findViewById(R.id.verifyButton);
        resendCodeButton = findViewById(R.id.resendCodeButton);
        networkLoader = findViewById(R.id.networkLoader);
        networkOverlay = findViewById(R.id.networkOverlay);

        phoneNumberText.setText("We've sent a verification code to\n" + phoneNumber);
    }

    private void setupListeners() {
        verifyButton.setOnClickListener(v -> {
            String code = verificationCodeEditText.getText().toString().trim();
            if (code.isEmpty() || code.length() < 6) {
                verificationCodeEditText.setError("Enter valid code");
                return;
            }
            verifyCode(code);
        });

        resendCodeButton.setOnClickListener(v -> resendVerificationCode(phoneNumber));
    }

    private void sendVerificationCode(String phoneNumber) {
        showLoading(true);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // Auto verification (instant verification on some devices)
                        verificationCodeEditText.setText(credential.getSmsCode());
                        verifyCode(credential.getSmsCode());
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        showLoading(false);
                        Toast.makeText(PhoneVerificationActivity.this,
                                "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
                        showLoading(false);
                        verificationId = id;
                        resendToken = token;
                        Toast.makeText(PhoneVerificationActivity.this,
                                "Verification code sent", Toast.LENGTH_SHORT).show();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendVerificationCode(String phoneNumber) {
        showLoading(true);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        verificationCodeEditText.setText(credential.getSmsCode());
                        verifyCode(credential.getSmsCode());
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        showLoading(false);
                        Toast.makeText(PhoneVerificationActivity.this,
                                "Resend failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(String id, PhoneAuthProvider.ForceResendingToken token) {
                        showLoading(false);
                        verificationId = id;
                        resendToken = token;
                        Toast.makeText(PhoneVerificationActivity.this,
                                "New verification code sent", Toast.LENGTH_SHORT).show();
                    }
                })
                .setForceResendingToken(resendToken)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyCode(String code) {
        showLoading(true);

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("verified", true);
                        resultIntent.putExtra("phoneNumber", phoneNumber);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Verification failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        networkOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        verifyButton.setEnabled(!show);
        resendCodeButton.setEnabled(!show);
    }
}