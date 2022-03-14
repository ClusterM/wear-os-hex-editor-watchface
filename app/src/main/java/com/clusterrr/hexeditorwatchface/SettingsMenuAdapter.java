package com.clusterrr.hexeditorwatchface;

import static com.clusterrr.hexeditorwatchface.HexWatchFace.TAG;

import android.Manifest;
import android.content.Context;
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
//            menuItem = view.findViewById(R.id.textView);
//            menuIcon = view.findViewById(R.id.menu_icon);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsMenuAdapter.RecyclerViewHolder holder, int position) {
        switch (position){
            case 0:
                holder.menuItemSettingKey.setText("Time format");
                holder.menuItemSettingValue.setText("Dec on tap");
                holder.menuContainer.setOnClickListener(v -> {
                    /*
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "ACCESS_BACKGROUND_LOCATION request required");
                        requestPermissionLauncherLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                    } else {
                        Log.d(TAG, "ACCESS_BACKGROUND_LOCATION request not required");
                    }
                     */
                    requestPermissionLauncherLocation.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                });
                break;
            case 1:
                holder.menuItemSettingKey.setText("Time numeral system");
                holder.menuItemSettingValue.setText("Dec on tap");
                holder.menuContainer.setOnClickListener(v -> {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "BODY_SENSORS request required");
                        requestPermissionLauncherBody.launch(Manifest.permission.BODY_SENSORS);
                    } else {
                        Log.d(TAG, "BODY_SENSORS request not required");
                    }
                });
                break;
            case 2:
                holder.menuItemSettingKey.setText("Date on the left");
                holder.menuItemSettingValue.setText("Dec on tap");
                holder.menuContainer.setOnClickListener(v -> {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                            != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "ACTIVITY_RECOGNITION request required");
                        requestPermissionLauncherSteps.launch(Manifest.permission.ACTIVITY_RECOGNITION);
                    } else {
                        Log.d(TAG, "ACTIVITY_RECOGNITION request not required");
                    }
                });
                break;
            case 3:
                holder.menuItemSettingKey.setText("Show heartrate");
                holder.menuItemSettingValue.setText("Dec on tap");
                break;
            case 4:
                holder.menuItemSettingKey.setText("Show steps");
                holder.menuItemSettingValue.setText("Little endian");
                break;
            case 5:
                holder.menuItemSettingKey.setText("Show temperate");
                holder.menuItemSettingValue.setText("Dec on tap");
                break;
            case 6:
                holder.menuItemSettingKey.setText("Show temperate");
                holder.menuItemSettingValue.setText("Dec on tap");
                break;
        }

        //MenuItem data_provider = dataSource.get(position);

        //holder.menuItem.setText("TEST");
        //holder.menuIcon.setImageResource(data_provider.getImage());


    }

    @Override
    public int getItemCount() {
        //return dataSource.size();
        return 7;
    }
}
/*
class MenuItem {
    private String text;
    private int image;

    public MenuItem(int image, String text) {
        this.image = image;
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public int getImage() {
        return image;
    }
}

 */