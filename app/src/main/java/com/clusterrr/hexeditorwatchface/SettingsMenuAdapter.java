package com.clusterrr.hexeditorwatchface;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
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
    private Setting[] mSettings;
    private AppCompatActivity mContext;
    private SettingsMenuAdapter.RecyclerViewHolder[] mHolders;

    public SettingsMenuAdapter(AppCompatActivity context, Setting[] settings) {
        this.mContext = context;
        mSettings = settings;
        this.mHolders = new SettingsMenuAdapter.RecyclerViewHolder[settings.length + 1];
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
            holder.menuItemSettingKey.setText("Explanation");
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
        ConstraintLayout menuContainer;
        TextView menuItemSettingKey;
        TextView menuItemSettingValue;
        TextView menuItemSettingExplanation;
        RadioButton menuItemSettingRadio;

        public RecyclerViewHolder(AppCompatActivity context, View view) {
            super(view);
            this.mContext = context;
            menuContainer = view.findViewById(R.id.settingsContainer);
            menuItemSettingKey = view.findViewById(R.id.textViewSettingKey);
            menuItemSettingValue = view.findViewById(R.id.textViewSettingValue);
            menuItemSettingExplanation = view.findViewById(R.id.textViewSettingExplanation);
            menuItemSettingRadio = view.findViewById(R.id.radioButtonSettingValue);
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
        return mSettings.length + 1;
    }
}
