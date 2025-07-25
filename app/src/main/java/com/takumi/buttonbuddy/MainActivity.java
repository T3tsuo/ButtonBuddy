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

    private TextView statusTextView;
    private Button enableServiceButton;
    private Button ignoreBatteryOptimizationButton;
    private Button setAppLaunchButton; // Declare new button

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
        setAppLaunchButton = findViewById(R.id.setAppLaunchButton); // Initialize new button

        enableServiceButton.setOnClickListener(v -> openAccessibilitySettings());
        ignoreBatteryOptimizationButton.setOnClickListener(v -> requestIgnoreBatteryOptimizations());
        setAppLaunchButton.setOnClickListener(v -> openAppPickerActivity()); // Set listener for new button
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateServiceStatus();
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
        String currentTargetApp = AppPrefs.getTargetPackage(this);
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
            statusTextView.setText(
                    "Accessibility Service Status: ENABLED\n\n" +
                            "Current launch app: " + appName +
                            "\n\nPress and hold the Volume Down button for at least 1 second to launch " +
                            appName + ".");
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            enableServiceButton.setEnabled(false);

            // Manage visibility and state of battery optimization button
            if (isIgnoringBattery) {
                ignoreBatteryOptimizationButton.setText("Battery Optimization: Ignored (Good)");
                ignoreBatteryOptimizationButton.setEnabled(false);
            } else {
                ignoreBatteryOptimizationButton.setText("Request Ignore Battery Optimization");
                ignoreBatteryOptimizationButton.setEnabled(true);
            }
            ignoreBatteryOptimizationButton.setVisibility(Button.VISIBLE);
            setAppLaunchButton.setVisibility(Button.VISIBLE); // Make Set App Launch button visible
        } else {
            statusTextView.setText("Accessibility Service Status: DISABLED\n\nPlease enable 'ButtonBuddy' in Accessibility settings to start logging events.");
            statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            enableServiceButton.setEnabled(true);
            ignoreBatteryOptimizationButton.setVisibility(Button.GONE);
            setAppLaunchButton.setVisibility(Button.GONE); // Hide Set App Launch button if service is not enabled
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