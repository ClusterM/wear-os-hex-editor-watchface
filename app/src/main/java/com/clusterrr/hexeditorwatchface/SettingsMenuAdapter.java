package com.clusterrr.hexeditorwatchface;

import static com.clusterrr.hexeditorwatchface.HexWatchFace.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

public class SettingsMenuAdapter extends RecyclerView.Adapter<SettingsMenuAdapter.RecyclerViewHolder> {
    private Setting[] mSettings;
    private AppCompatActivity mContext;
    private SettingsMenuAdapter.RecyclerViewHolder[] mHolders;

    public SettingsMenuAdapter(AppCompatActivity context, Setting[] settings) {
        this.mContext = context;
        mSettings = settings;
        this.mHolders = new SettingsMenuAdapter.RecyclerViewHolder[settings.length];
    }

    public void updateHolder(int pos)
    {
        Resources res = mContext.getResources();
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
        // Disable vignetting for rectangular screens
        holder.menuContainer.setEnabled(res.getBoolean(R.bool.is_round) || pos != SettingsActivity.PREF_KEY_VIGNETTING);
        holder.menuItemSettingKey.setEnabled(res.getBoolean(R.bool.is_round) || pos != SettingsActivity.PREF_KEY_VIGNETTING);
        holder.menuItemSettingValue.setEnabled(res.getBoolean(R.bool.is_round) || pos != SettingsActivity.PREF_KEY_VIGNETTING);
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
