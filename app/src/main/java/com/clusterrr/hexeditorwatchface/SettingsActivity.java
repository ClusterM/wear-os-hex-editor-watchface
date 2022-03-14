package com.clusterrr.hexeditorwatchface;

import static com.clusterrr.hexeditorwatchface.HexWatchFace.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

public class SettingsActivity extends AppCompatActivity {
    public static final int PREF_KEY_TIME_FORMAT = 0;
    public static final int PREF_KEY_TIME_SYSTEM = 1;
    public static final int PREF_KEY_DATE = 2;
    public static final int PREF_KEY_DAY_OF_THE_WEEK = 3;
    public static final int PREF_KEY_HEART_RATE= 4;
    public static final int PREF_KEY_STEPS = 5;
    public static final int PREF_KEY_BATTERY = 6;
    public static final int PREF_KEY_ENDIANNESS = 7;
    public static final int PREF_KEY_VIGNETTING = 8;

    public static final int PREF_VALUE_NOT_SHOW = 0;

    public static final int PREF_TIME_FORMAT_12 = 0;
    public static final int PREF_TIME_FORMAT_24 = 1;

    private Setting[] mSettings;
    SettingsMenuAdapter mSettingsMenuAdapter;

    private ActivityResultLauncher<String> requestPermissionLauncherBody
            = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            Log.i(TAG, "BODY_SENSORS granted");
        } else {
            // Rollback
            Log.i(TAG, "BODY_SENSORS not granted");
            mSettings[PREF_KEY_HEART_RATE].setValue(PREF_VALUE_NOT_SHOW);
            mSettingsMenuAdapter.updateHolder(PREF_KEY_HEART_RATE);
        }
    });
    private ActivityResultLauncher<String> requestPermissionActivity
            = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            Log.d(TAG, "ACTIVITY_RECOGNITION granted");
        } else {
            Log.d(TAG, "ACTIVITY_RECOGNITION not granted");
            mSettings[PREF_KEY_STEPS].setValue(PREF_VALUE_NOT_SHOW);
            mSettingsMenuAdapter.updateHolder(PREF_KEY_STEPS);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);

        mSettings = new Setting[] {
                new Setting(prefs, "Time format", new String[] {"12 hours", "24 hours"}, getString(R.string.pref_time_format), 1),
                new Setting(prefs, "Time system", new String[] {"Dec", "Hex", "Hex, dec on tap"}, getString(R.string.pref_time_system), 2),
                new Setting(prefs, "Date", new String[] {"Do not show", "Dec", "Hex, dec on tap"}, getString(R.string.pref_date), 0),
                new Setting(prefs, "Day of the week", new String[] {"Do not show", "Sunday=0", "Sunday=7"}, getString(R.string.pref_day_week), 0),
                new Setting(prefs, "Heart rate", new String[] {"Do not show", "Dec", "Hex, dec on tap"}, getString(R.string.pref_heart_rate), 0),
                new Setting(prefs, "Steps", new String[] {"Do not show", "Dec", "Hex, dec on tap"}, getString(R.string.pref_steps), 0),
                new Setting(prefs, "Battery", new String[] {"Do not show", "Dec (0-100)",
                        "Hex (0x00-0xFF)", "Hex (0-100)", "Hex (0x00-0xFF), dec on tap", "Hex (0-100), dec on tap"},
                        getString(R.string.pref_battery), 0),
                new Setting(prefs, "Endianness", new String[] {"Little endian", "Big endian"}, getString(R.string.pref_endianness), 0),
                new Setting(prefs, "Round vignetting", new String[] {"Disabled", "Enabled"}, getString(R.string.pref_vignetting), 0),
        };

        WearableRecyclerView recyclerView = findViewById(R.id.settings_menu_view);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        CustomScrollingLayoutCallback customScrollingLayoutCallback =
                new CustomScrollingLayoutCallback();
        recyclerView.setLayoutManager(
                new WearableLinearLayoutManager(this, customScrollingLayoutCallback));
        mSettingsMenuAdapter = new SettingsMenuAdapter(this, mSettings);
        recyclerView.setAdapter(mSettingsMenuAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(HexWatchFace.TAG, "Result code: " + resultCode);
        if (resultCode != Activity.RESULT_OK) return;
        int setting = data.getIntExtra("setting", -1);
        int selected = data.getIntExtra("selected", -1);
        Log.d(HexWatchFace.TAG, "Setting " + setting + " set to " + selected);

        switch (setting) {
            case PREF_KEY_HEART_RATE:
                if (selected != PREF_VALUE_NOT_SHOW) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "BODY_SENSORS request required");
                        requestPermissionLauncherBody.launch(Manifest.permission.BODY_SENSORS);
                    }
                }
                break;
            case PREF_KEY_STEPS:
                if (selected != PREF_VALUE_NOT_SHOW) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "ACTIVITY_RECOGNITION request required");
                        requestPermissionLauncherBody.launch(Manifest.permission.ACTIVITY_RECOGNITION);
                    }
                }
                break;
        }

        mSettings[setting].setValue(selected);
        mSettingsMenuAdapter.updateHolder(setting);
    }
}

