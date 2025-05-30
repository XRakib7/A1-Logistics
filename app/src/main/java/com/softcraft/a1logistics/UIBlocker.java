package com.softcraft.a1logistics;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

public class UIBlocker {
    private final View overlay;
    private final LottieAnimationView loadingAnimation;
    private final LottieAnimationView errorAnimation;
    private final LottieAnimationView successAnimation; // Add this
    private final TextView statusMessage;
    private final Activity activity;

    public UIBlocker(Activity activity) {
        this.activity = activity;
        ViewGroup rootView = activity.findViewById(android.R.id.content);

        overlay = LayoutInflater.from(activity)
                .inflate(R.layout.ui_blocker_overlay, rootView, false);
        rootView.addView(overlay);

        loadingAnimation = overlay.findViewById(R.id.loadingAnimation);
        errorAnimation = overlay.findViewById(R.id.errorAnimation);
        successAnimation = overlay.findViewById(R.id.successAnimation); // Add this
        statusMessage = overlay.findViewById(R.id.statusMessage);
    }

    public void showSuccess(String message, Runnable onComplete) {
        activity.runOnUiThread(() -> {
            loadingAnimation.setVisibility(View.GONE);
            errorAnimation.setVisibility(View.GONE);
            successAnimation.setVisibility(View.VISIBLE);
            successAnimation.playAnimation();
            statusMessage.setText(message);

            successAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                    unblockUI();
                }
            });
        });
    }


    public void blockUI(String message) {
        activity.runOnUiThread(() -> {
            overlay.setVisibility(View.VISIBLE);
            loadingAnimation.setVisibility(View.VISIBLE);
            loadingAnimation.playAnimation();
            errorAnimation.setVisibility(View.GONE);
            statusMessage.setText(message);
            overlay.setClickable(true);
        });
    }


    public void showError(String errorMessage) {
        activity.runOnUiThread(() -> {
            loadingAnimation.setVisibility(View.GONE);
            errorAnimation.setVisibility(View.VISIBLE);
            errorAnimation.playAnimation();
            statusMessage.setText(errorMessage);

            // Auto-hide after error animation completes
            errorAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    unblockUI();
                }
            });
        });
    }

    public void unblockUI() {
        activity.runOnUiThread(() -> {
            overlay.setVisibility(View.GONE);
            overlay.setClickable(false);
            overlay.setFocusable(false);
        });
    }

    public void showSignupSuccess(Runnable onComplete) {
        activity.runOnUiThread(() -> {
            // First hide the standard blocker overlay
            overlay.setVisibility(View.GONE);

            // Only proceed if this is SignupActivity
            if (activity instanceof SignupActivity) {
                SignupActivity signupActivity = (SignupActivity) activity;
                FrameLayout successOverlay = signupActivity.findViewById(R.id.signupSuccessOverlay);
                LottieAnimationView successAnimation = signupActivity.findViewById(R.id.signupSuccessAnimation);

                if (successOverlay != null && successAnimation != null) {
                    successOverlay.setVisibility(View.VISIBLE);
                    successAnimation.playAnimation();

                    successAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            successOverlay.setVisibility(View.GONE);
                            if (onComplete != null) onComplete.run();
                        }
                    });
                    return;
                }
            }

            // Fallback for non-SignupActivity or if views not found
            showGenericSuccess(onComplete);
        });
    }
    // Add this new method

    private void showGenericSuccess(Runnable onComplete) {
        activity.runOnUiThread(() -> {
            loadingAnimation.setVisibility(View.VISIBLE);
            loadingAnimation.setAnimation(R.raw.success_with_confetti);
            loadingAnimation.playAnimation();
            statusMessage.setText("Success!");

            loadingAnimation.addAnimatorListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    unblockUI();
                    if (onComplete != null) onComplete.run();
                    // Reset to loading animation
                    loadingAnimation.setAnimation(R.raw.loading);
                }
            });
        });
    }
}