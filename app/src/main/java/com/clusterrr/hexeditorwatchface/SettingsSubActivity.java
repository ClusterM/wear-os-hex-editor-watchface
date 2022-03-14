package com.clusterrr.hexeditorwatchface;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

public class SettingsSubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        int setting = intent.getIntExtra("setting", 0);
        String[] values = intent.getStringArrayExtra("values");
        int selected = intent.getIntExtra("selected", 0);
        setContentView(R.layout.settings_activity);
        WearableRecyclerView recyclerView = findViewById(R.id.settings_menu_view);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        CustomScrollingLayoutCallback customScrollingLayoutCallback =
                new CustomScrollingLayoutCallback();
        recyclerView.setLayoutManager(
                new WearableLinearLayoutManager(this, customScrollingLayoutCallback));
        recyclerView.setAdapter(new SettingsSubMenuAdapter(this, setting, values, selected));
        //Intent resultIntent = new Intent();
        //resultIntent.putExtra("selected", 0);
        //setResult(Activity.RESULT_OK, 0);
    }
}
