package com.takumi.buttonbuddy;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ButtonBuddyService";
    private TextView statusTextView;
    private Button enableServiceButton;
    private Button ignoreBatteryOptimizationButton;
    String currentTargetApp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusTextView = findViewById(R.id.statusTextView);
        enableServiceButton = findViewById(R.id.enableServiceButton);
        ignoreBatteryOptimizationButton = findViewById(R.id.ignoreBatteryOptimizationButton);
        // Declare new button
        Button setAppLaunchButton = findViewById(R.id.setAppLaunchButton);
        Button launchAppButton = findViewById(R.id.launchAppButton); // Initialize new button

        enableServiceButton.setOnClickListener(v -> openAccessibilitySettings());
        ignoreBatteryOptimizationButton.setOnClickListener(v -> requestIgnoreBatteryOptimizations());
        setAppLaunchButton.setOnClickListener(v -> openAppPickerActivity());
        launchAppButton.setOnClickListener(v -> launchApp()); // Set listener for new button
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
    }

    private void launchApp() {
        if (currentTargetApp == null || currentTargetApp.isEmpty()) {
            Log.w(TAG, "No target package set or package name is empty. Cannot launch app.");
            return;
        }

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(currentTargetApp);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(launchIntent);
                Log.d(TAG, "Successfully launched " + currentTargetApp);
            } catch (Exception e) {
                Log.e(TAG, "Error launching " + currentTargetApp + ": " + e.getMessage());
            }
        } else {
            Log.e(TAG, "Could not find launch intent for package: " + currentTargetApp);
        }
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Find 'ButtonBuddy' in Accessibility settings and enable it.", Toast.LENGTH_LONG).show();
    }

    private void openAppPickerActivity() {
        Intent intent = new Intent(this, AppPickerActivity.class);
        startActivity(intent);
    }

    private void updateServiceStatus() {
        boolean isServiceEnabled = isAccessibilityServiceEnabled(this);
        boolean isIgnoringBattery = isIgnoringBatteryOptimizations();
        currentTargetApp = AppPrefs.getTargetPackage(this);
        String appName;

        // Check if the current target package is the default Google Wallet package
        if (currentTargetApp.equals(AppPrefs.DEFAULT_TARGET_PACKAGE)) {
            appName = "Google Wallet (Default)";
        } else {
            // Attempt to get the app name for a non-default package
            try {
                PackageManager pm = getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(currentTargetApp, 0);
                appName = pm.getApplicationLabel(ai).toString();
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("MainActivity", "Target app package not found: " + currentTargetApp + ", Error: " + e.getMessage());
                appName = "N/A (App not found)"; // Fallback if app is uninstalled or package is invalid
            }
        }

        if (isServiceEnabled) {
            String statusText = "Accessibility Service Status: ENABLED\n\n" +
                    "Current launch app: " + appName +
                    "\n\nPress and hold the Volume Down button for at least 1 second to launch " +
                    appName + ".";
            statusTextView.setText(statusText);
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            enableServiceButton.setEnabled(false);

            // Manage visibility and state of battery optimization button
            if (isIgnoringBattery) {
                String optimizationMessage = "Battery Optimization: Ignored (Good)";
                ignoreBatteryOptimizationButton.setText(optimizationMessage);
                ignoreBatteryOptimizationButton.setEnabled(false);
            } else {
                String optimizationMessage = "Request Ignore Battery Optimization";
                ignoreBatteryOptimizationButton.setText(optimizationMessage);
                ignoreBatteryOptimizationButton.setEnabled(true);
            }
            ignoreBatteryOptimizationButton.setVisibility(Button.VISIBLE);
        } else {
            String statusText = "Accessibility Service Status: DISABLED\n\nPlease enable 'ButtonBuddy' in Accessibility settings to start logging events.\n\n" +
                    "Current launch app: " + appName;
            statusTextView.setText(statusText);
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            enableServiceButton.setEnabled(true);
            ignoreBatteryOptimizationButton.setVisibility(Button.GONE);
        }
    }

    private boolean isAccessibilityServiceEnabled(Context context) {
        ComponentName cn = new ComponentName(context, ButtonBuddyAccessibilityService.class);
        String flat = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (flat != null && !flat.isEmpty()) {
            String[] enabledServices = flat.split(":");
            for (String enabledService : enabledServices) {
                if (enabledService.equals(cn.flattenToString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isIgnoringBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void requestIgnoreBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Could not open battery optimization settings automatically. Please do it manually.", Toast.LENGTH_LONG).show();
                Log.e("MainActivity", "Error opening battery optimization settings: " + e.getMessage());
            }
        } else {
            Toast.makeText(this, "App is already ignoring battery optimizations.", Toast.LENGTH_SHORT).show();
        }
    }
}