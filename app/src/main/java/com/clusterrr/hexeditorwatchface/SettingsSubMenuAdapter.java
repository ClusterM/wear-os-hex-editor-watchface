package com.clusterrr.hexeditorwatchface;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

public class SettingsSubMenuAdapter extends RecyclerView.Adapter<SettingsSubMenuAdapter.RecyclerViewHolder> {
    private final AppCompatActivity mContext;
    int mSetting;
    private final String[] mValues;
    private final int mSelected;

    public SettingsSubMenuAdapter(AppCompatActivity context, int setting, String[] values, int selected) {
        this.mContext = context;
        this.mSetting = setting;
        this.mValues = values;
        this.mSelected = selected;
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
    public void onBindViewHolder(@NonNull SettingsSubMenuAdapter.RecyclerViewHolder holder, int position) {
        final int pos = position;
        holder.menuItemSettingKey.setText(mValues[pos]);
        holder.menuItemSettingRadio.setChecked(pos == mSelected);
        holder.menuItemSettingRadio.setOnCheckedChangeListener((v, b) -> {
            if (b) {
                Intent result = new Intent();
                result.putExtra("setting", mSetting);
                result.putExtra("selected", pos);
                Log.d(HexWatchFace.TAG, "Return: " + mSetting + "=" + pos);
                mContext.setResult(Activity.RESULT_OK, result);
                mContext.finish();
            }
        });
        holder.menuContainer.setOnClickListener((v) -> {
            holder.menuItemSettingRadio.setChecked(false);
            holder.menuItemSettingRadio.setChecked(true);
        });
        holder.menuItemSettingKey.setVisibility(View.VISIBLE);
        holder.menuItemSettingValue.setVisibility(View.GONE);
        holder.menuItemSettingRadio.setVisibility(View.VISIBLE);
        holder.menuItemSettingExplanation.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return mValues.length;
    }
}
