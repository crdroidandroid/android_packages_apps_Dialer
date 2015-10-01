/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.dialer.settings;

import android.content.Context;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SlimSeekBarPreference;
import android.preference.SwitchPreference;
import android.provider.Settings;

import android.text.TextUtils;
import com.android.dialer.R;
import com.android.phone.common.util.SettingsUtil;

import java.lang.Boolean;
import java.lang.CharSequence;
import java.lang.Object;
import java.lang.Override;
import java.lang.Runnable;
import java.lang.String;
import java.lang.Thread;
import java.util.Locale;

public class GeneralSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String CATEGORY_SOUNDS_KEY    = "dialer_general_sounds_category_key";
    private static final String CATEGORY_INCALL_VIBRATION_KEY =
            "dialer_general_incall_vibration_category_key";
    private static final String BUTTON_RINGTONE_KEY    = "button_ringtone_key";
    private static final String BUTTON_VIBRATE_ON_RING = "button_vibrate_on_ring";
    private static final String BUTTON_PLAY_DTMF_TONE  = "button_play_dtmf_tone";
    private static final String BUTTON_RESPOND_VIA_SMS_KEY = "button_respond_via_sms_key";
    private static final String BUTTON_SPEED_DIAL_KEY  = "speed_dial_settings";
    private static final String BUTTON_T9_SEARCH_INPUT_LOCALE = "button_t9_search_input";
    public static final String BUTTON_SMART_DIALER_KEY = "button_smart_dialer";
    private static final String USE_NON_INTRUSIVE_CALL_KEY = "use_non_intrusive_call";

    private static final String PROX_AUTO_SPEAKER  = "prox_auto_speaker";
    private static final String PROX_AUTO_SPEAKER_DELAY  = "prox_auto_speaker_delay";
    private static final String PROX_AUTO_SPEAKER_INCALL_ONLY  = "prox_auto_speaker_incall_only";

    private static final String FLIP_ACTION_KEY = "flip_action";
    private SwitchPreference mUseNonIntrusiveCall;

    private static final int MSG_UPDATE_RINGTONE_SUMMARY = 1;

    private Context mContext;

    private Preference mRingtonePreference;
    private SwitchPreference mVibrateWhenRinging;
    private SwitchPreference mPlayDtmfTone;
    private Preference mRespondViaSms;
    private Preference mSpeedDialSettings;
    private ListPreference mT9SearchInputLocale;

    private SwitchPreference mProxSpeaker;
    private SlimSeekBarPreference mProxSpeakerDelay;
    private SwitchPreference mProxSpeakerIncallOnly;

    private ListPreference mFlipAction;

    // t9 search input locales that we have a custom overlay for
    private static final Locale[] T9_SEARCH_INPUT_LOCALES = new Locale[] {
            new Locale("ko"), new Locale("el"), new Locale("ru"),
            new Locale("he"), new Locale("zh")
    };

    private Runnable mRingtoneLookupRunnable;
    private final Handler mRingtoneLookupComplete = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_RINGTONE_SUMMARY:
                    mRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity().getApplicationContext();

        addPreferencesFromResource(R.xml.general_settings);

        mRingtonePreference = findPreference(BUTTON_RINGTONE_KEY);
        mVibrateWhenRinging = (SwitchPreference) findPreference(BUTTON_VIBRATE_ON_RING);
        mPlayDtmfTone = (SwitchPreference) findPreference(BUTTON_PLAY_DTMF_TONE);
        mRespondViaSms = findPreference(BUTTON_RESPOND_VIA_SMS_KEY);
        mSpeedDialSettings = findPreference(BUTTON_SPEED_DIAL_KEY);
        mT9SearchInputLocale = (ListPreference) findPreference(BUTTON_T9_SEARCH_INPUT_LOCALE);

        PreferenceCategory soundCategory = (PreferenceCategory) findPreference(CATEGORY_SOUNDS_KEY);
        Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        boolean hasVibrator = vibrator != null && vibrator.hasVibrator();

        mProxSpeaker = (SwitchPreference) findPreference(PROX_AUTO_SPEAKER);
        mProxSpeakerIncallOnly = (SwitchPreference) findPreference(PROX_AUTO_SPEAKER_INCALL_ONLY);
        mProxSpeakerDelay = (SlimSeekBarPreference) findPreference(PROX_AUTO_SPEAKER_DELAY);
        if (mProxSpeakerDelay != null) {
            mProxSpeakerDelay.setDefault(100);
            mProxSpeakerDelay.isMilliseconds(true);
            mProxSpeakerDelay.setInterval(1);
            mProxSpeakerDelay.minimumValue(100);
            mProxSpeakerDelay.multiplyValue(100);
            mProxSpeakerDelay.setOnPreferenceChangeListener(this);
        }

        mFlipAction = (ListPreference) findPreference(FLIP_ACTION_KEY);
        if (mFlipAction != null) {
            mFlipAction.setOnPreferenceChangeListener(this);
        }

        mUseNonIntrusiveCall = (SwitchPreference) findPreference(USE_NON_INTRUSIVE_CALL_KEY);
        if (mUseNonIntrusiveCall != null) {
            mUseNonIntrusiveCall.setOnPreferenceChangeListener(this);
        }

        if (mVibrateWhenRinging != null) {
            if (hasVibrator) {
                mVibrateWhenRinging.setOnPreferenceChangeListener(this);
            } else {
                soundCategory.removePreference(mVibrateWhenRinging);
                mVibrateWhenRinging = null;
            }
        }
        if (!hasVibrator) {
            getPreferenceScreen().removePreference(findPreference(CATEGORY_INCALL_VIBRATION_KEY));
        }

        if (mPlayDtmfTone != null) {
            mPlayDtmfTone.setOnPreferenceChangeListener(this);
            mPlayDtmfTone.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.DTMF_TONE_WHEN_DIALING, 1) != 0);
        }

        if (mT9SearchInputLocale != null) {
            initT9SearchInputPreferenceList();
            mT9SearchInputLocale.setOnPreferenceChangeListener(this);
        }

        mRingtoneLookupRunnable = new Runnable() {
            @Override
            public void run() {
                if (mRingtonePreference != null) {
                    SettingsUtil.updateRingtoneName(
                            mContext,
                            mRingtoneLookupComplete,
                            RingtoneManager.TYPE_RINGTONE,
                            mRingtonePreference.getKey(),
                            MSG_UPDATE_RINGTONE_SUMMARY);
                }
            }
        };
    }

    /**
     * Supports onPreferenceChangeListener to look for preference changes.
     *
     * @param preference The preference to be changed
     * @param objValue The value of the selection, NOT its localized display value.
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mVibrateWhenRinging) {
            boolean doVibrate = (Boolean) objValue;
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.VIBRATE_WHEN_RINGING, doVibrate ? 1 : 0);
        } else if (preference == mT9SearchInputLocale) {
            saveT9SearchInputLocale(preference, (String) objValue);
        } else if (preference == mProxSpeakerDelay) {
            int delay = Integer.valueOf((String) objValue);
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.PROXIMITY_AUTO_SPEAKER_DELAY, delay);
        } else if (preference == mFlipAction) {
            int action = /*mFlipAction.findIndexOfValue*/Integer.valueOf((String) objValue);
            Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.CALL_FLIP_ACTION_KEY, action);
            updateFlipActionSummary(action);
        } else if (preference == mUseNonIntrusiveCall) {
            final boolean val = (Boolean) objValue;
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.USE_NON_INTRUSIVE_CALL, val ? 1 : 0);
        }
        return true;
    }

    /**
     * Click listener for toggle events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPlayDtmfTone) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.DTMF_TONE_WHEN_DIALING, mPlayDtmfTone.isChecked() ? 1 : 0);
        } else if (preference == mRespondViaSms || preference == mSpeedDialSettings) {
            // Needs to return false for the intent to launch.
            return false;
        } else if (preference == mProxSpeaker) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.PROXIMITY_AUTO_SPEAKER,
                    mProxSpeaker.isChecked() ? 1 : 0);
        } else if (preference == mProxSpeakerIncallOnly) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.PROXIMITY_AUTO_SPEAKER_INCALL_ONLY,
                    mProxSpeakerIncallOnly.isChecked() ? 1 : 0);
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mVibrateWhenRinging != null) {
            mVibrateWhenRinging.setChecked(SettingsUtil.getVibrateWhenRingingSetting(mContext));
        }

        if (mProxSpeaker != null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            if (pm.isWakeLockLevelSupported(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)
                    && getResources().getBoolean(R.bool.config_enabled_speakerprox)) {
                mProxSpeaker.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                        Settings.System.PROXIMITY_AUTO_SPEAKER, 0) == 1);
                if (mProxSpeakerIncallOnly != null) {
                    mProxSpeakerIncallOnly.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                            Settings.System.PROXIMITY_AUTO_SPEAKER_INCALL_ONLY, 0) == 1);
                }
                if (mProxSpeakerDelay != null) {
                    final int proxDelay = Settings.System.getInt(mContext.getContentResolver(),
                            Settings.System.PROXIMITY_AUTO_SPEAKER_DELAY, 100);
                    // minimum 100 is 1 interval of the 100 multiplier
                    mProxSpeakerDelay.setInitValue((proxDelay / 100) - 1);
                }
            } else {
                getPreferenceScreen().removePreference(mProxSpeaker);
                mProxSpeaker = null;
                if (mProxSpeakerIncallOnly != null) {
                    getPreferenceScreen().removePreference(mProxSpeakerIncallOnly);
                    mProxSpeakerIncallOnly = null;
                }
                if (mProxSpeakerDelay != null) {
                    getPreferenceScreen().removePreference(mProxSpeakerDelay);
                    mProxSpeakerDelay = null;
                }
            }
        }

        if (mFlipAction != null) {
            int flipAction = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.CALL_FLIP_ACTION_KEY, 2);
            mFlipAction.setValue(String.valueOf(flipAction));
            updateFlipActionSummary(flipAction);
        }

        if (mUseNonIntrusiveCall != null) {
            mUseNonIntrusiveCall.setChecked(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.USE_NON_INTRUSIVE_CALL, 1) != 0);
        }

        // Lookup the ringtone name asynchronously.
        new Thread(mRingtoneLookupRunnable).start();
    }

    private void saveT9SearchInputLocale(Preference preference, String newT9Locale) {
        String lastT9Locale = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.T9_SEARCH_INPUT_LOCALE);

        if (!TextUtils.equals(lastT9Locale, newT9Locale)) {
            Settings.System.putString(mContext.getContentResolver(),
                    Settings.System.T9_SEARCH_INPUT_LOCALE, newT9Locale);
        }
    }

    private void initT9SearchInputPreferenceList() {
        int len = T9_SEARCH_INPUT_LOCALES.length + 1;
        String[] entries = new String[len];
        String[] values = new String[len];

        entries[0] = getString(R.string.t9_search_input_locale_default);
        values[0] = "";

        // add locales programatically so we can use locale.getDisplayName
        for (int i = 0; i < T9_SEARCH_INPUT_LOCALES.length; i++) {
            Locale locale = T9_SEARCH_INPUT_LOCALES[i];
            entries[i + 1] = locale.getDisplayName();
            values[i + 1] = locale.toString();
        }

        // Set current entry from global system setting
        String settingsT9Locale = Settings.System.getString(mContext.getContentResolver(),
                Settings.System.T9_SEARCH_INPUT_LOCALE);
        if (settingsT9Locale != null) {
            mT9SearchInputLocale.setValue(settingsT9Locale);
        }

        mT9SearchInputLocale.setEntries(entries);
        mT9SearchInputLocale.setEntryValues(values);
    }

    private void updateFlipActionSummary(int value) {
        if (mFlipAction != null) {
            String[] summaries = getResources().getStringArray(R.array.flip_action_summary_entries);
            mFlipAction.setSummary(getString(R.string.flip_action_summary, summaries[value]));
        }
    }
}
