/*
 * Copyright (C) 2018 The Android Open Source Project
 * Copyright (C) 2023-2025 The LineageOS Project
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
 * limitations under the License.
 */

package com.android.dialer.theme.base.impl;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

import com.android.dialer.R;
import com.android.dialer.common.Assert;
import com.android.dialer.common.LogUtil;
import com.android.dialer.theme.base.Theme;

import javax.inject.Singleton;

/**
 * Resolves Dialer theme colors.
 */
@Singleton
public class AospThemeImpl implements Theme {

  private static final String TAG = "AospThemeImpl";

  private static final int IDX_COLOR_PRIMARY = 0;
  private static final int IDX_COLOR_PRIMARY_DARK = 1;
  private static final int IDX_COLOR_ACCENT = 2;
  private static final int IDX_TEXT_COLOR_PRIMARY = 3;
  private static final int IDX_TEXT_COLOR_SECONDARY = 4;
  private static final int IDX_TEXT_COLOR_PRIMARY_INVERSE = 5;
  private static final int IDX_TEXT_COLOR_HINT = 6;
  private static final int IDX_COLOR_BACKGROUND = 7;
  private static final int IDX_COLOR_BACKGROUND_FLOATING = 8;
  private static final int IDX_COLOR_ICON = 9;
  private static final int IDX_COLOR_ICON_SECONDARY = 10;
  private static final int IDX_COLOR_TEXT_ON_UNTHEMED_DARK_BACKGROUND = 11;
  private static final int IDX_COLOR_ICON_ON_UNTHEMED_DARK_BACKGROUND = 12;
  private static final int IDX_COLOR_CALL_NOTIFICATION_BACKGROUND = 13;

  /** Attributes to resolve. Order must match the IDX_* constants above. */
  private static final int[] ATTRS =
      new int[] {
        android.R.attr.colorPrimary,
        android.R.attr.colorPrimaryDark,
        android.R.attr.colorAccent,
        android.R.attr.textColorPrimary,
        android.R.attr.textColorSecondary,
        android.R.attr.textColorPrimaryInverse,
        android.R.attr.textColorHint,
        android.R.attr.colorBackground,
        android.R.attr.colorBackgroundFloating,
        R.attr.colorIcon,
        R.attr.colorIconSecondary,
        R.attr.colorTextOnUnthemedDarkBackground,
        R.attr.colorIconOnUnthemedDarkBackground,
        R.attr.colorCallNotificationBackground
      };

  private static final int[] FALLBACK_COLORS =
      new int[] {
        0xFF1A73E8, // colorPrimary
        0xFF1A73E8, // colorPrimaryDark
        0xFF1A73E8, // colorAccent
        0xDE000000, // textColorPrimary
        0x8A000000, // textColorSecondary
        0xFFDADCE0, // textColorPrimaryInverse
        0xFF80868B, // textColorHint
        Color.WHITE, // colorBackground
        Color.WHITE, // colorBackgroundFloating
        0xFF5F6368, // colorIcon
        0xFFBDC1C6, // colorIconSecondary
        Color.WHITE, // colorTextOnUnthemedDarkBackground
        Color.WHITE, // colorIconOnUnthemedDarkBackground
        0xFF1A73E8 // colorCallNotificationBackground
      };

  private final Context appContext;
  private final Object lock = new Object();

  private Configuration cachedConfig;
  private int[] cachedColors;

  public AospThemeImpl(Context context) {
    Assert.checkArgument(ATTRS.length == FALLBACK_COLORS.length);
    appContext = context.getApplicationContext();
    appContext.setTheme(getApplicationThemeRes());
    getColors();
  }

  @NonNull
  private int[] getColors() {
    Configuration current = appContext.getResources().getConfiguration();
    synchronized (lock) {
      if (cachedColors == null || cachedConfig == null || !cachedConfig.equals(current)) {
        cachedColors = resolveColors(appContext.getResources());
        cachedConfig = new Configuration(current);
      }
      return cachedColors;
    }
  }

  @NonNull
  private int[] resolveColors(@NonNull Resources resources) {
    Resources.Theme theme = resources.newTheme();
    theme.applyStyle(getApplicationThemeRes(), true);

    int[] colors = new int[ATTRS.length];
    TypedArray array = theme.obtainStyledAttributes(getApplicationThemeRes(), ATTRS);
    try {
      for (int i = 0; i < ATTRS.length; i++) {
        colors[i] = readColor(array, i);
      }
    } finally {
      array.recycle();
    }
    return colors;
  }

  @ColorInt
  private static int readColor(@NonNull TypedArray array, int index) {
    if (array.hasValue(index)) {
      try {
        return array.getColor(index, FALLBACK_COLORS[index]);
      } catch (RuntimeException e) {
        LogUtil.e(TAG + ".readColor", "failed to resolve color attr at index " + index, e);
      }
    } else {
      LogUtil.w(TAG + ".readColor", "color attr at index %d is not defined, using fallback", index);
    }
    return FALLBACK_COLORS[index];
  }

  /**
   * Returns the {@link Theme} that the application is using. Activities should check this value if
   * their custom style needs to customize further based on the application theme.
   */
  @Override
  public @Type int getTheme() {
    // TODO(a bug): add share prefs check to configure this
    return LIGHT;
  }

  @Override
  public @StyleRes int getApplicationThemeRes() {
    switch (getTheme()) {
      case DARK:
        return R.style.Dialer_Dark_ThemeBase_NoActionBar;
      case LIGHT:
        return R.style.Dialer_ThemeBase_NoActionBar;
      case UNKNOWN:
      default:
        throw Assert.createIllegalStateFailException("Theme hasn't been set yet.");
    }
  }

  @Override
  public Context getThemedContext(Context context) {
    return new ContextThemeWrapper(context, getApplicationThemeRes());
  }

  @Override
  public LayoutInflater getThemedLayoutInflator(LayoutInflater inflater) {
    return inflater.cloneInContext(getThemedContext(inflater.getContext()));
  }

  @Override
  public @ColorInt int getColorIcon() {
    return getColors()[IDX_COLOR_ICON];
  }

  @Override
  public @ColorInt int getColorIconSecondary() {
    return getColors()[IDX_COLOR_ICON_SECONDARY];
  }

  @Override
  public @ColorInt int getColorPrimary() {
    return getColors()[IDX_COLOR_PRIMARY];
  }

  @Override
  public @ColorInt int getColorPrimaryDark() {
    return getColors()[IDX_COLOR_PRIMARY_DARK];
  }

  @Override
  public @ColorInt int getColorAccent() {
    return getColors()[IDX_COLOR_ACCENT];
  }

  @Override
  public @ColorInt int getTextColorSecondary() {
    return getColors()[IDX_TEXT_COLOR_SECONDARY];
  }

  @Override
  public @ColorInt int getTextColorPrimary() {
    return getColors()[IDX_TEXT_COLOR_PRIMARY];
  }

  @Override
  public @ColorInt int getColorTextOnUnthemedDarkBackground() {
    return getColors()[IDX_COLOR_TEXT_ON_UNTHEMED_DARK_BACKGROUND];
  }

  @Override
  public @ColorInt int getColorIconOnUnthemedDarkBackground() {
    return getColors()[IDX_COLOR_ICON_ON_UNTHEMED_DARK_BACKGROUND];
  }

  @Override
  public @ColorInt int getColorCallNotificationBackground() {
    return getColors()[IDX_COLOR_CALL_NOTIFICATION_BACKGROUND];
  }
}
