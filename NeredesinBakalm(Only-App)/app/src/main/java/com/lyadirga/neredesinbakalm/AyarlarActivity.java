package com.lyadirga.neredesinbakalm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

public class AyarlarActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences preferences;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.ayarlar);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        boolean isKonum = sharedPreferences.getBoolean("konumSwitch",false);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent servisIntent = new Intent(this,KonumServisi.class);
        PendingIntent pi = PendingIntent.getService(this, MainActivity.ALARM_MUHRU, servisIntent,0);

        if (isKonum){


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+1000,pi);

            }else {
                alarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+1000,pi);
            }

        } else {
            alarmManager.cancel(pi);

        }
    }
}
