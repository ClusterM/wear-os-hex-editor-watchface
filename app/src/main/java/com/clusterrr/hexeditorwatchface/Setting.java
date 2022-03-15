package com.clusterrr.hexeditorwatchface;

import android.content.SharedPreferences;

public class Setting {
    private final SharedPreferences mPrefs;
    private final String mName;
    private final String[] mValueNames;
    private final String mKey;
    private final int mDefaultValue;

    public Setting(SharedPreferences prefs, String name, String[] valueNames, String key, int defaultValue) {
        mPrefs = prefs;
        mName = name;
        mValueNames = valueNames.clone();
        mKey = key;
        mDefaultValue = defaultValue;
    }

    public String getName() { return mName; }
    public String[] getValueNames() { return mValueNames.clone(); }
    public String getValueName() {
        try{
            return mValueNames[mPrefs.getInt(mKey, mDefaultValue)];
        }
        catch (Exception ex) {
            setValue(mDefaultValue);
            return mValueNames[mDefaultValue];
        }
    }
    public int getValue() { return mPrefs.getInt(mKey, mDefaultValue); }
    public void setValue(int value) { mPrefs.edit().putInt(mKey, value).apply(); }
}
