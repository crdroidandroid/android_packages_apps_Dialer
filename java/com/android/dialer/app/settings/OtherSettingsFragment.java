/*
 * Copyright (C) 2024 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.dialer.app.settings;

import static android.hardware.Sensor.TYPE_PROXIMITY;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.android.dialer.app.R;

public class OtherSettingsFragment extends PreferenceFragmentCompat
    implements Preference.OnPreferenceChangeListener {

  private static final String ENABLE_POST_CALL = "enable_post_call";
  private static final String DISABLE_PROXIMITY_SENSOR = "disable_proximity_sensor";

  private SharedPreferences mPrefs;
  private boolean mEnabled;

  private SwitchPreferenceCompat mEnablePostcall;

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    addPreferencesFromResource(R.xml.other_settings);

    Context context = getActivity();

    mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    mEnabled = mPrefs.getBoolean(ENABLE_POST_CALL, true);

    mEnablePostcall = findPreference(ENABLE_POST_CALL);
    mEnablePostcall.setChecked(mEnabled);
    mEnablePostcall.setOnPreferenceChangeListener(this);

    if (!showSensorOptions()) {
        SwitchPreferenceCompat disableProximity = findPreference(DISABLE_PROXIMITY_SENSOR);
        getPreferenceScreen().removePreference(disableProximity);
    }
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object objValue) {
    if (preference == mEnablePostcall) {
        boolean value = (Boolean) objValue;
        mPrefs
          .edit()
          .putBoolean(ENABLE_POST_CALL, value)
          .apply();
        return true;
    }
    return false;
  }

  private boolean showSensorOptions() {
    SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
    return sm.getDefaultSensor(TYPE_PROXIMITY) != null;
  }
}
