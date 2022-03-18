package com.clusterrr.hexeditorwatchface;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class SettingsMenuAdapter extends RecyclerView.Adapter<SettingsMenuAdapter.RecyclerViewHolder> {
    private final Setting[] mSettings;
    private final AppCompatActivity mContext;
    private final SettingsMenuAdapter.RecyclerViewHolder[] mHolders;
    private final SensorManager mSensorManager;

    public SettingsMenuAdapter(AppCompatActivity context, Setting[] settings) {
        mContext = context;
        mSettings = settings;
        mHolders = new SettingsMenuAdapter.RecyclerViewHolder[settings.length + 1];
        mSensorManager = ((SensorManager)context.getSystemService(Context.SENSOR_SERVICE));
    }

    public void updateHolder(int pos)
    {
        Resources res = mContext.getResources();
        SettingsMenuAdapter.RecyclerViewHolder holder = mHolders[pos];
        if (pos < mSettings.length) {
            // Settings with values
            holder.menuItemSettingKey.setText(mSettings[pos].getName());
            holder.menuItemSettingValue.setText(mSettings[pos].getValueName());
            holder.menuContainer.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, SettingsSubActivity.class);
                intent.putExtra("setting", pos);
                intent.putExtra("values", mSettings[pos].getValueNames());
                intent.putExtra("selected", mSettings[pos].getValue());
                mContext.startActivityForResult(intent, 0);
            });
            holder.menuItemSettingKey.setVisibility(View.VISIBLE);
            holder.menuItemSettingValue.setVisibility(View.VISIBLE);
            holder.menuItemSettingRadio.setVisibility(View.GONE);
            holder.menuItemSettingExplanation.setVisibility(View.GONE);
        } else {
                // Show some explanation
            holder.menuItemSettingKey.setText("Legend");
            holder.menuItemSettingValue.setText("");

            holder.menuContainer.setOnClickListener(v -> {
                Intent intent = new Intent(mContext, ExplanationActivity.class);
                mContext.startActivity(intent);
            });
            holder.menuItemSettingKey.setVisibility(View.GONE);
            holder.menuItemSettingValue.setVisibility(View.GONE);
            holder.menuItemSettingRadio.setVisibility(View.GONE);
            holder.menuItemSettingExplanation.setVisibility(View.VISIBLE);
        }
        boolean enabled = true;
        if ((pos == SettingsActivity.PREF_KEY_VIGNETTING) && !res.getBoolean(R.bool.is_round))
            enabled = false;
        if ((pos == SettingsActivity.PREF_KEY_HEART_RATE) &&
            (mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) == null))
            enabled = false;
        if ((pos == SettingsActivity.PREF_KEY_STEPS) &&
            (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) == null))
            enabled = false;
        // Disable vignetting for rectangular screens
        holder.menuContainer.setEnabled(enabled);
        holder.menuItemSettingKey.setEnabled(enabled);
        holder.menuItemSettingValue.setEnabled(enabled);
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item, parent, false);
        return new RecyclerViewHolder(view);
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout menuContainer;
        TextView menuItemSettingKey;
        TextView menuItemSettingValue;
        TextView menuItemSettingExplanation;
        RadioButton menuItemSettingRadio;

        public RecyclerViewHolder(View view) {
            super(view);
            menuContainer = view.findViewById(R.id.settingsContainer);
            menuItemSettingKey = view.findViewById(R.id.textViewSettingKey);
            menuItemSettingValue = view.findViewById(R.id.textViewSettingValue);
            menuItemSettingExplanation = view.findViewById(R.id.textViewSettingExplanation);
            menuItemSettingRadio = view.findViewById(R.id.radioButtonSettingValue);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsMenuAdapter.RecyclerViewHolder holder, int position) {
        mHolders[position] = holder;
        updateHolder(position);
    }

    @Override
    public int getItemCount() {
        return mSettings.length + 1;
    }
}
