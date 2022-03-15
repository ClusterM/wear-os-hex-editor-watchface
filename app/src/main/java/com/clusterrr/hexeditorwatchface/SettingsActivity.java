package com.clusterrr.hexeditorwatchface;

import static com.clusterrr.hexeditorwatchface.HexWatchFace.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
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
    public static final int PREF_KEY_BACKGROUND = 8;
    public static final int PREF_KEY_BARS = 9;
    public static final int PREF_KEY_VIGNETTING = 10;

    public static final int PREF_VALUE_HIDE = 0;

    public static final int PREF_TIME_FORMAT_12 = 0;
    public static final int PREF_TIME_FORMAT_24 = 1;

    public static final int PREF_VALUE_TIME_DEC = 0;
    public static final int PREF_VALUE_TIME_HEX = 1;
    public static final int PREF_VALUE_TIME_DEC_ON_TAP = 2;

    public static final int PREF_VALUE_COMMON_DEC = 1;
    public static final int PREF_VALUE_COMMON_HEX = 2;
    public static final int PREF_VALUE_COMMON_DEC_ON_TAP = 3;

    public static final int PREF_VALUE_DAY_SUNDAY_0 = 1;
    public static final int PREF_VALUE_DAY_SUNDAY_7 = 2;

    public static final int PREF_VALUE_BATTERY_DEC_0_100 = 1;
    public static final int PREF_VALUE_BATTERY_HEX_0_FF = 2;
    public static final int PREF_VALUE_BATTERY_HEX_0_64 = 3;
    public static final int PREF_VALUE_BATTERY_HEX_0_FF_TAP = 4;
    public static final int PREF_VALUE_BATTERY_HEX_0_64_TAP = 5;

    public static final int PREF_VALUE_ENDIANNESS_LITTLE_ENDIAN = 0;
    public static final int PREF_VALUE_ENDIANNESS_BIG_ENDIAN = 1;

    public static final int PREF_VALUE_BACKGROUND_RANDOM = 0;
    public static final int PREF_VALUE_BACKGROUND_RANDOM_ONCE = 1;
    public static final int PREF_VALUE_BACKGROUND_ZEROS = 2;

    public static final int PREF_VALUE_BARS_SHOW = 1;

    public static final int PREF_VALUE_VIGNETTING_ENABLED = 1;

    public static final int PREF_DEFAULT_TIME_FORMAT = PREF_TIME_FORMAT_24;
    public static final int PREF_DEFAULT_TIME_SYSTEM = PREF_VALUE_TIME_DEC;
    public static final int PREF_DEFAULT_DATE = PREF_VALUE_HIDE;
    public static final int PREF_DEFAULT_DAY_OF_THE_WEEK = PREF_VALUE_HIDE;
    public static final int PREF_DEFAULT_HEART_RATE = PREF_VALUE_HIDE;
    public static final int PREF_DEFAULT_STEPS = PREF_VALUE_HIDE;
    public static final int PREF_DEFAULT_BATTERY = PREF_VALUE_HIDE;
    public static final int PREF_DEFAULT_ENDIANNESS = PREF_VALUE_ENDIANNESS_LITTLE_ENDIAN;
    public static final int PREF_DEFAULT_BACKGROUND = PREF_VALUE_BACKGROUND_ZEROS;
    public static final int PREF_DEFAULT_BARS = PREF_VALUE_BARS_SHOW;
    //public static final int PREF_DEFAULT_VIGNETTING = PREF_VALUE_VIGNETTING_ENABLED;

    private Setting[] mSettings;
    SettingsMenuAdapter mSettingsMenuAdapter;

    private final ActivityResultLauncher<String> requestPermissionLauncherBody
            = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            Log.i(TAG, "BODY_SENSORS granted");
        } else {
            // Rollback
            Log.i(TAG, "BODY_SENSORS not granted");
            mSettings[PREF_KEY_HEART_RATE].setValue(PREF_VALUE_HIDE);
            mSettingsMenuAdapter.updateHolder(PREF_KEY_HEART_RATE);
        }
    });
    private final ActivityResultLauncher<String> requestPermissionActivity
            = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            Log.d(TAG, "ACTIVITY_RECOGNITION granted");
        } else {
            Log.d(TAG, "ACTIVITY_RECOGNITION not granted");
            mSettings[PREF_KEY_STEPS].setValue(PREF_VALUE_HIDE);
            mSettingsMenuAdapter.updateHolder(PREF_KEY_STEPS);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        Resources res = getResources();

        mSettings = new Setting[] {
                // TODO: move strings to resources
                new Setting(prefs, "Time format", new String[] {"12 hours", "24 hours"}, getString(R.string.pref_time_format), PREF_DEFAULT_TIME_FORMAT),
                new Setting(prefs, "Time system", new String[] {"Dec", "Hex", "Hex, dec on tap"}, getString(R.string.pref_time_system), PREF_DEFAULT_TIME_SYSTEM),
                new Setting(prefs, "Date", new String[] {"Do not show", "Dec", "Hex", "Hex, dec on tap"}, getString(R.string.pref_date), PREF_DEFAULT_DATE),
                new Setting(prefs, "Day of the week", new String[] {"Do not show", "Sunday=0", "Sunday=7"}, getString(R.string.pref_day_week), PREF_DEFAULT_DAY_OF_THE_WEEK),
                new Setting(prefs, "Heart rate", new String[] {"Do not show", "Dec", "Hex", "Hex, dec on tap"}, getString(R.string.pref_heart_rate), PREF_DEFAULT_HEART_RATE),
                new Setting(prefs, "Steps", new String[] {"Do not show", "Dec", "Hex", "Hex, dec on tap"}, getString(R.string.pref_steps), PREF_DEFAULT_STEPS),
                new Setting(prefs, "Battery", new String[] {"Do not show", "Dec (0-100)",
                        "Hex (0x00-0xFF)", "Hex (0-100)", "Hex (0x00-0xFF), dec on tap", "Hex (0-100), dec on tap"},
                        getString(R.string.pref_battery), PREF_DEFAULT_BATTERY),
                new Setting(prefs, "Endianness", new String[] {"Little endian", "Big endian"}, getString(R.string.pref_endianness), PREF_DEFAULT_ENDIANNESS),
                new Setting(prefs, "Background", new String[] {"Random every second", "Random once", "Zeros"}, getString(R.string.pref_background), PREF_DEFAULT_BACKGROUND),
                new Setting(prefs, "Vertical bars", new String[] {"Hide", "Show"}, getString(R.string.pref_bars), PREF_DEFAULT_BARS),
                new Setting(prefs, "Round vignetting", new String[] {"Disabled", "Enabled"}, getString(R.string.pref_vignetting), res.getInteger(R.integer.default_vignetting)),
        };

        WearableRecyclerView recyclerView = findViewById(R.id.settings_menu_view);
        if (res.getBoolean(R.bool.is_round)) {
            recyclerView.setEdgeItemsCenteringEnabled(true);
            CustomScrollingLayoutCallback customScrollingLayoutCallback =
                    new CustomScrollingLayoutCallback();
            recyclerView.setLayoutManager(
                    new WearableLinearLayoutManager(this, customScrollingLayoutCallback));
        } else {
            recyclerView.setLayoutManager(
                    new WearableLinearLayoutManager(this));
        }
        mSettingsMenuAdapter = new SettingsMenuAdapter(this, mSettings);
        recyclerView.setAdapter(mSettingsMenuAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        int setting = data.getIntExtra("setting", -1);
        int selected = data.getIntExtra("selected", -1);

        switch (setting) {
            case PREF_KEY_HEART_RATE:
                if (selected != PREF_VALUE_HIDE) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "BODY_SENSORS request required");
                        requestPermissionLauncherBody.launch(Manifest.permission.BODY_SENSORS);
                    }
                }
                break;
            case PREF_KEY_STEPS:
                if (selected != PREF_VALUE_HIDE) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "ACTIVITY_RECOGNITION request required");
                        requestPermissionActivity.launch(Manifest.permission.ACTIVITY_RECOGNITION);
                    }
                }
                break;
            case PREF_KEY_BACKGROUND:
                prefs.edit().putBoolean(getString(R.string.pref_background_redraw), true).apply();
                break;
        }

        mSettings[setting].setValue(selected);
        mSettingsMenuAdapter.updateHolder(setting);
    }
}

