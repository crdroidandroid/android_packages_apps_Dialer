/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2023 The LineageOS Project
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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.android.dialer.R;

public class DisplayOptionsSettingsFragment extends PreferenceFragmentCompat
    implements Preference.OnPreferenceChangeListener {

  private static final String FULLSCREEN_CALLER_PHOTO = "fullscreen_caller_photo";

  private SharedPreferences mPrefs;
  private boolean mEnabled;

  private SwitchPreferenceCompat mFullscreenCallerPhoto;

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    addPreferencesFromResource(R.xml.display_options_settings);

    Context context = getActivity();

    mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

    mFullscreenCallerPhoto = findPreference(FULLSCREEN_CALLER_PHOTO);
    mFullscreenCallerPhoto.setChecked(mPrefs.getBoolean(FULLSCREEN_CALLER_PHOTO, false));
    mFullscreenCallerPhoto.setOnPreferenceChangeListener(this);

  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object objValue) {
    if (preference == mFullscreenCallerPhoto) {
        boolean value = (Boolean) objValue;
        mPrefs
          .edit()
          .putBoolean(FULLSCREEN_CALLER_PHOTO, value)
          .apply();
        return true;
    }
    return false;
  }
}
