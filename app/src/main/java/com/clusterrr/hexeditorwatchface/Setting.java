package com.clusterrr.hexeditorwatchface;

import android.content.SharedPreferences;

public class Setting {
    private SharedPreferences mPrefs;
    private String mName;
    private String[] mValueNames;
    private String mKey;
    private int mDefaultValue;

    public Setting(SharedPreferences prefs, String name, String[] valueNames, String key, int defaultValue) {
        mPrefs = prefs;
        mName = name;
        mValueNames = valueNames.clone();
        mKey = key;
        mDefaultValue = defaultValue;
    }

    public String[] getValueNames() { return mValueNames.clone(); }
    public String getValueName(int i) { return mValueNames[i]; }
    public int getValueCount() { return mValueNames.length; }
    public String getValueName() { return mValueNames[mPrefs.getInt(mKey, mDefaultValue)]; }
    public int getValue(int i) { return mPrefs.getInt(mKey, mDefaultValue); }
    public void setValue(int i, int value) { mPrefs.edit().putInt(mKey, value).apply(); }
}
