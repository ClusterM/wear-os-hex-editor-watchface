package com.clusterrr.hexeditorwatchface;

import static com.clusterrr.hexeditorwatchface.HexWatchFace.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class SettingsMenuAdapter extends RecyclerView.Adapter<SettingsMenuAdapter.RecyclerViewHolder> {
    private Setting[] mSettings;
    private AppCompatActivity mContext;
    private ActivityResultLauncher<String> requestPermissionLauncherLocation;
    private ActivityResultLauncher<String> requestPermissionLauncherBody;
    private ActivityResultLauncher<String> requestPermissionLauncherSteps;
    private View.OnClickListener onClickListener;
    private SettingsMenuAdapter.RecyclerViewHolder[] mHolders;

    public SettingsMenuAdapter(AppCompatActivity context, Setting[] settings) {
        this.mContext = context;
        mSettings = settings;
        this.mHolders = new SettingsMenuAdapter.RecyclerViewHolder[settings.length];

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

    public void updateHolder(int pos)
    {
        SettingsMenuAdapter.RecyclerViewHolder holder = mHolders[pos];
        holder.menuItemSettingKey.setText(mSettings[pos].getName());
        holder.menuItemSettingValue.setText(mSettings[pos].getValueName());
        holder.menuContainer.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, SettingsSubActivity.class);
            intent.putExtra("setting", pos);
            intent.putExtra("values", mSettings[pos].getValueNames());
            intent.putExtra("selected", mSettings[pos].getValue());
            mContext.startActivityForResult(intent, 0);
        });
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item, parent, false);
        return new RecyclerViewHolder(mContext, view);
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;
        LinearLayout menuContainer;
        TextView menuItemSettingKey;
        TextView menuItemSettingValue;
        RadioButton menuItemSettingRadio;

        public RecyclerViewHolder(AppCompatActivity context, View view) {
            super(view);
            this.mContext = context;
            menuContainer = view.findViewById(R.id.settingsContainer);
            menuItemSettingKey = view.findViewById(R.id.textViewSettingKey);
            menuItemSettingValue = view.findViewById(R.id.textViewSettingValue);
            menuItemSettingRadio = view.findViewById(R.id.radioButtonSettingValue);
            menuItemSettingValue.setVisibility(View.VISIBLE);
            menuItemSettingRadio.setVisibility(View.GONE);
            Log.i(TAG, "RecyclerViewHolder created");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsMenuAdapter.RecyclerViewHolder holder, int position) {
        final int pos = position;
        mHolders[pos] = holder;
        updateHolder(pos);
    }

    @Override
    public int getItemCount() {
        return mSettings.length;
    }
}
