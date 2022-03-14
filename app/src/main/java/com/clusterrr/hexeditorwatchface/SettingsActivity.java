package com.clusterrr.hexeditorwatchface;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

public class SettingsActivity extends AppCompatActivity {
    private Setting[] mSettings;
    SettingsMenuAdapter settingsMenuAdapter;

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
        settingsMenuAdapter = new SettingsMenuAdapter(this, mSettings);
        recyclerView.setAdapter(settingsMenuAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(HexWatchFace.TAG, "Result code: " + resultCode);
        if (resultCode != Activity.RESULT_OK) return;
        int setting = data.getIntExtra("setting", -1);
        int selected = data.getIntExtra("selected", -1);
        Log.d(HexWatchFace.TAG, "Setting " + setting + " set to " + selected);
        mSettings[setting].setValue(selected);
        settingsMenuAdapter.updateHolder(setting);
    }
}

