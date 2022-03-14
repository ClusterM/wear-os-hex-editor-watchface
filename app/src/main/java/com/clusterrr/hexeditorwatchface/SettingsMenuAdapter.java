package com.clusterrr.hexeditorwatchface;

import static com.clusterrr.hexeditorwatchface.HexWatchFace.TAG;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class SettingsMenuAdapter extends RecyclerView.Adapter<SettingsMenuAdapter.RecyclerViewHolder> {
    //private ArrayList<MenuItem> dataSource = new ArrayList<MenuItem>();

    private Setting[] mSettings;

    public interface AdapterCallback {
        void onItemClicked(Integer menuPosition);
    }

    private AdapterCallback callback;
    private String drawableIcon;
    private AppCompatActivity context;
    private ActivityResultLauncher<String> requestPermissionLauncherLocation;
    private ActivityResultLauncher<String> requestPermissionLauncherBody;
    private ActivityResultLauncher<String> requestPermissionLauncherSteps;

    public SettingsMenuAdapter(AppCompatActivity context /*, ArrayList<MenuItem> dataArgs, AdapterCallback callback*/) {
        this.context = context;
        SharedPreferences prefs = context.getPreferences(Context.MODE_PRIVATE);
        mSettings = new Setting[] {
                new Setting(prefs, "Time format", new String[] {"12 hours", "24 hours"}, context.getString(R.string.pref_time_format), 1),
                new Setting(prefs, "Time system", new String[] {"Dec", "Hex", "Hex, dec on tap"}, context.getString(R.string.pref_time_system), 2),
                new Setting(prefs, "Date", new String[] {"Do not show", "Dec", "Hex, dec on tap"}, context.getString(R.string.pref_date), 0),
                new Setting(prefs, "Day of the week", new String[] {"Do not show", "Sunday = 0", "Sunday = 7"}, context.getString(R.string.pref_day_week), 0),
                new Setting(prefs, "Heart rate", new String[] {"Do not show", "Dec", "Hex, dec on tap"}, context.getString(R.string.pref_heart_rate), 0),
                new Setting(prefs, "Steps", new String[] {"Do not show", "Dec", "Hex, dec on tap"}, context.getString(R.string.pref_steps), 0),
                new Setting(prefs, "Battery", new String[] {"Do not show", "Dec (0-100)", 
                        "Hex (0x00-0xFF)", "Hex (0-100)", "Hex (0x00-0xFF), dec on tap", "Hex (0-100), dec on tap"},
                        context.getString(R.string.pref_battery), 0),
                new Setting(prefs, "Endianness", new String[] {"Little endian", "Big endian"}, context.getString(R.string.pref_endianness), 0),
                new Setting(prefs, "Round vignetting", new String[] {"Disabled", "Enabled>"}, context.getString(R.string.pref_vignetting), 0),
        };
//        this.dataSource = dataArgs;
//        this.callback = callback;
        requestPermissionLauncherLocation = context.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                Log.d(TAG, "ACCESS_BACKGROUND_LOCATION granted");
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Log.d(TAG, "ACCESS_BACKGROUND_LOCATION not granted");
            }
        });
        requestPermissionLauncherBody = context.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                Log.d(TAG, "BODY_SENSORS granted");
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Log.d(TAG, "BODY_SENSORS not granted");
            }
        });
        requestPermissionLauncherSteps  = context.registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                Log.d(TAG, "ACTIVITY_RECOGNITION granted");
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Log.d(TAG, "ACTIVITY_RECOGNITION not granted");
            }
        });
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item, parent, false);
        return new RecyclerViewHolder(context, view);
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private Context context;
        LinearLayout menuContainer;
        TextView menuItemSettingKey;
        TextView menuItemSettingValue;

        public RecyclerViewHolder(Context context, View view) {
            super(view);
            this.context = context;
            menuContainer = view.findViewById(R.id.settingsContainer);
            menuItemSettingKey = view.findViewById(R.id.textViewSettingKey);
            menuItemSettingValue = view.findViewById(R.id.textViewSettingValue);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsMenuAdapter.RecyclerViewHolder holder, int position) {
        holder.menuItemSettingKey.setText(mSettings[position].getName());
        holder.menuItemSettingValue.setText(mSettings[position].getValueName());
        holder.menuContainer.setOnClickListener(v -> {

        });
    }

    @Override
    public int getItemCount() {
        return mSettings.length;
    }
}
