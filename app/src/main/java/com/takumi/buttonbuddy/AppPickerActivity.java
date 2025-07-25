package com.takumi.buttonbuddy;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView; // Import SearchView

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AppPickerActivity extends AppCompatActivity {

    private AppListAdapter appListAdapter;
    private List<AppInfo> fullAppList; // Store the complete list of apps

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_app_picker);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setTitle("Select App to Launch"); // Set activity title

        RecyclerView recyclerView = findViewById(R.id.app_list_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadInstalledApps(); // Load the full list
        appListAdapter = new AppListAdapter(this, fullAppList); // Pass the full list initially
        recyclerView.setAdapter(appListAdapter);

        // Declare SearchView
        SearchView searchView = findViewById(R.id.app_search_view); // Initialize SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Not typically used for filtering as it updates on text change
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter the list as the user types
                appListAdapter.filter(newText);
                return true;
            }
        });
    }

    private void loadInstalledApps() {
        fullAppList = new ArrayList<>(); // Initialize the full list
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> applications = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo appInfo : applications) {
            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) { // Only show launchable apps
                String appName = appInfo.loadLabel(pm).toString();
                String packageName = appInfo.packageName;
                Drawable appIcon = appInfo.loadIcon(pm);

                fullAppList.add(new AppInfo(appName, packageName, appIcon));
            }
        }

        // Sort the list alphabetically by app name
        fullAppList.sort((o1, o2) -> o1.getAppName().compareToIgnoreCase(o2.getAppName()));
    }

    // --- AppInfo Data Class (No changes needed here) ---
    private static class AppInfo {
        private final String appName;
        private final String packageName;
        private final Drawable appIcon;

        public AppInfo(String appName, String packageName, Drawable appIcon) {
            this.appName = appName;
            this.packageName = packageName;
            this.appIcon = appIcon;
        }

        public String getAppName() {
            return appName;
        }

        public String getPackageName() {
            return packageName;
        }

        public Drawable getAppIcon() {
            return appIcon;
        }
    }

    // --- RecyclerView Adapter ---
    private static class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

        private final List<AppInfo> originalAppList; // The complete, unfiltered list
        private final List<AppInfo> filteredAppList; // The list currently displayed in RecyclerView
        private final Context context;

        public AppListAdapter(Context context, List<AppInfo> appList) {
            this.context = context;
            this.originalAppList = new ArrayList<>(appList); // Copy the full list
            this.filteredAppList = new ArrayList<>(appList); // Initially, filtered list is also full list
        }

        @NonNull
        @Override
        public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_item, parent, false);
            return new AppViewHolder(view);
        }

        @Override
        public void onBindViewHolder(AppViewHolder holder, int position) {
            AppInfo app = filteredAppList.get(position); // Bind from the filtered list
            holder.appIcon.setImageDrawable(app.getAppIcon());
            holder.appName.setText(app.getAppName());
            holder.packageName.setText(app.getPackageName());

            holder.itemView.setOnClickListener(v -> {
                AppPrefs.saveTargetPackage(context, app.getPackageName());
                Toast.makeText(context, "Set " + app.getAppName() + " as launch target.", Toast.LENGTH_SHORT).show();
                ((AppPickerActivity) context).finish(); // Close the activity
            });
        }

        @Override
        public int getItemCount() {
            return filteredAppList.size(); // Count from the filtered list
        }

        // --- New Filter Method ---
        @SuppressLint("NotifyDataSetChanged")
        public void filter(String query) {
            filteredAppList.clear();
            if (query.isEmpty()) {
                filteredAppList.addAll(originalAppList);
            } else {
                query = query.toLowerCase(); // Case-insensitive search
                for (AppInfo app : originalAppList) {
                    if (app.getAppName().toLowerCase().contains(query)) {
                        filteredAppList.add(app);
                    }
                }
            }
            notifyDataSetChanged(); // Tell the RecyclerView to refresh its view
        }

        static class AppViewHolder extends RecyclerView.ViewHolder {
            ImageView appIcon;
            TextView appName;
            TextView packageName;

            public AppViewHolder(View itemView) {
                super(itemView);
                appIcon = itemView.findViewById(R.id.app_icon);
                appName = itemView.findViewById(R.id.app_name);
                packageName = itemView.findViewById(R.id.package_name);
            }
        }
    }
}