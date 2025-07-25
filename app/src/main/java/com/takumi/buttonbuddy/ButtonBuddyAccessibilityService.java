package com.takumi.buttonbuddy;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class ButtonBuddyAccessibilityService extends AccessibilityService {

    private static final String TAG = "ButtonBuddyService";
    private static final long VOLUME_DOWN_HOLD_DURATION = 1000; // 1 second in milliseconds

    // No need for currentTargetPackage as a member variable if we fetch it on every key event

    private Handler handler;
    private Runnable longPressRunnable;
    private boolean appLaunchedOnLongPress = false; // Flag to ensure app is launched only once per long press

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        longPressRunnable = () -> {
            if (!appLaunchedOnLongPress) {
                // Retrieve the latest target package when the long press timer completes
                String packageToLaunch = AppPrefs.getTargetPackage(getApplicationContext()); // Fetch here!
                Log.d(TAG, "Volume Down Button Held for 1+ second! Attempting to launch " + packageToLaunch);
                launchTargetApplication(packageToLaunch);
                appLaunchedOnLongPress = true;
            }
        };
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Log.d(TAG, "Accessibility Event: " + event.getEventType() + " - " + event.getPackageName());
        // For production, you might want to be more selective about logging frequent events
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) { // Ensure it's the initial press
                    handler.postDelayed(longPressRunnable, VOLUME_DOWN_HOLD_DURATION);
                    appLaunchedOnLongPress = false; // Reset flag for a new press cycle
                    Log.i(TAG, "Volume Down Key Down - scheduling long press check.");
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                handler.removeCallbacks(longPressRunnable);
                Log.i(TAG, "Volume Down Key Up - canceling long press check.");
                appLaunchedOnLongPress = false; // Reset for the next cycle
            }
        } else {
            // If any other key is pressed, cancel the volume down long press check
            handler.removeCallbacks(longPressRunnable);
            appLaunchedOnLongPress = false; // Reset flag
        }
        return false; // Return false to let other apps/system process the key event (e.g., adjust volume)
    }

    private void launchTargetApplication(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            Log.w(TAG, "No target package set or package name is empty. Cannot launch app.");
            return;
        }

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(launchIntent);
                Log.d(TAG, "Successfully launched " + packageName);
            } catch (Exception e) {
                Log.e(TAG, "Error launching " + packageName + ": " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Could not find launch intent for package: " + packageName);
        }
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "ButtonBuddyAccessibilityService interrupted.");
        if (handler != null) {
            handler.removeCallbacks(longPressRunnable);
        }
        appLaunchedOnLongPress = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(longPressRunnable);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.packageNames = null; // Listen to all packages

        this.setServiceInfo(info);
        Log.d(TAG, "ButtonBuddyAccessibilityService connected successfully.");

        // We no longer need to load currentTargetPackage here initially, as it's fetched just-in-time.
        // Log.d(TAG, "Initial target package set to: " + currentTargetPackage); // This line can be removed
    }
}