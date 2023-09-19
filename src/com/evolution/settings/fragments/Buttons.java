/*
 * Copyright (C) 2016 The CyanogenMod project
 *               2017-2022 The LineageOS project
 *               2018 The PixelExperience Project
 *               2023 Evolution X
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
package com.evolution.settings.fragments;

import static android.os.UserHandle.USER_CURRENT;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.om.IOverlayManager;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.Handler;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.View;

import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.lineage.hardware.LineageHardwareManager;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.evolution.EvolutionUtils;
import com.android.internal.util.hwkeys.ActionConstants;
import com.android.internal.util.hwkeys.ActionUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.evolution.settings.preference.SystemSettingListPreference;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.evolution.settings.preference.ActionFragment;
import com.evolution.settings.preference.ButtonBacklightBrightness;
import com.evolution.settings.preference.CustomDialogPreference;
import com.evolution.settings.preference.SecureSettingSwitchPreference;
import com.evolution.settings.preference.SystemSettingSwitchPreference;
import com.evolution.settings.utils.ButtonSettingsUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SearchIndexable
public class Buttons extends ActionFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String ALERT_SLIDER_CAT = "alert_slider_cat";
    private static final String BLOCK_ALERT = "block_alert";
    private static final String HWKEY_DISABLE = "hardware_keys_disable";
    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";
    private static final String KEY_NAVBAR_INVERSE = "navigation_bar_inverse";
    private static final String KEY_NAVIGATION_COMPACT_LAYOUT = "navigation_bar_compact_layout";
    private static final String KEY_SWAP_CAPACITIVE_KEYS = "swap_capacitive_keys";
    private static final String NAVBAR_VISIBILITY = "navbar_visibility";

    // category keys
    private static final String CATEGORY_HWKEY = "hardware_keys";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_VOLUME = "volume_keys";
    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_BACKLIGHT = "key_backlight";

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;
    public static final int KEY_MASK_VOLUME = 0x40;

    private PreferenceCategory mButtonBackLightCategory;
    private PreferenceCategory mHwKeyCategory;
    private SecureSettingSwitchPreference mSwapCapacitiveKeys;
    private SwitchPreference mHwKeyDisable;
    private SwitchPreference mNavbarVisibility;
    private SystemSettingSwitchPreference mAlertBlock;
    private SystemSettingSwitchPreference mNavbarInverse;
    private SystemSettingSwitchPreference mNavigationCompactLayout;

    private boolean mIsNavSwitchingMode = false;

    private Handler mHandler;

    private static final String VOLUMEBAR_STYLES = "VOLUME_BAR_STYLES";

    private static final String VOLUMEBAR_OVERLAY_STYLE1 = "com.custom.overlay.systemui.volume1";
    private static final String VOLUMEBAR_OVERLAY_STYLE2 = "com.custom.overlay.systemui.volume2"; 
    private static final String VOLUMEBAR_OVERLAY_STYLE3 = "com.custom.overlay.systemui.volume3";        
    private static final String VOLUMEBAR_OVERLAY_STYLE4 = "com.custom.overlay.systemui.volume4"; 
    private static final String VOLUMEBAR_OVERLAY_STYLE5 = "com.custom.overlay.systemui.volume5";

    private SystemSettingListPreference CustomVolumeStyle;
    private IOverlayManager mOverlayService; 
    private Context mContext;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.evolution_settings_buttons);

        final Resources res = getResources();
        final ContentResolver resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();
        mContext = getActivity();
        final PreferenceScreen screen = getPreferenceScreen();

        mOverlayService = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));

        CustomVolumeStyle = (SystemSettingListPreference) findPreference("VOLUME_BAR_STYLES");
        int CuVolumeStyle = Settings.System.getIntForUser(getContentResolver(),
                "VOLUME_BAR_STYLES", 0, UserHandle.USER_CURRENT);
        int valueIndexvol = CustomVolumeStyle.findIndexOfValue(String.valueOf(CuVolumeStyle));
        CustomVolumeStyle.setValueIndex(valueIndexvol >= 0 ? valueIndexvol : 0);
        CustomVolumeStyle.setSummary(CustomVolumeStyle.getEntry());
        CustomVolumeStyle.setOnPreferenceChangeListener(this);

        mHandler = new Handler();

        mSwapCapacitiveKeys = findPreference(KEY_SWAP_CAPACITIVE_KEYS);
        if (mSwapCapacitiveKeys != null && !isKeySwapperSupported(getActivity())) {
            prefScreen.removePreference(mSwapCapacitiveKeys);
            mSwapCapacitiveKeys = null;
        }

        final boolean needsNavbar = ActionUtils.hasNavbarByDefault(getActivity());
        mHwKeyCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_HWKEY);
        int keysDisabled = 0;
        if (!needsNavbar) {
            mHwKeyDisable = (SwitchPreference) findPreference(HWKEY_DISABLE);
            keysDisabled = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.HARDWARE_KEYS_DISABLE, 0,
                    UserHandle.USER_CURRENT);
            mHwKeyDisable.setChecked(keysDisabled != 0);
            mHwKeyDisable.setOnPreferenceChangeListener(this);
        } else {
            prefScreen.removePreference(mHwKeyCategory);
        }

        // bits for hardware keys present on device
        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        // read bits for present hardware keys
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;
        // load categories and init/remove preferences based on device
        // configuration
        final PreferenceCategory backCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_BACK);
        final PreferenceCategory homeCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory = (PreferenceCategory) prefScreen
                .findPreference(CATEGORY_APPSWITCH);
        // back key
        if (!hasBackKey) {
            prefScreen.removePreference(backCategory);
        }
        // home key
        if (!hasHomeKey) {
            prefScreen.removePreference(homeCategory);
        }
        // App switch key (recents)
        if (!hasAppSwitchKey) {
            prefScreen.removePreference(appSwitchCategory);
        }
        // menu key
        if (!hasMenuKey) {
            prefScreen.removePreference(menuCategory);
        }
        // search/assist key
        if (!hasAssistKey) {
            prefScreen.removePreference(assistCategory);
        }
        // let super know we can load ActionPreferences
        onPreferenceScreenLoaded(ActionConstants.getDefaults(ActionConstants.HWKEYS));

        // load preferences first
        setActionPreferencesEnabled(keysDisabled == 0);

        final ButtonBacklightBrightness backlight = findPreference(KEY_BUTTON_BACKLIGHT);
        if (!ButtonSettingsUtils.hasButtonBacklightSupport(getActivity())
                && !ButtonSettingsUtils.hasKeyboardBacklightSupport(getActivity())) {
            prefScreen.removePreference(backlight);
        }

        mNavbarVisibility = (SwitchPreference) findPreference(NAVBAR_VISIBILITY);

        boolean showing = Settings.System.getIntForUser(resolver,
                Settings.System.FORCE_SHOW_NAVBAR,
                ActionUtils.hasNavbarByDefault(getActivity()) ? 1 : 0, UserHandle.USER_CURRENT) != 0;
        mNavbarVisibility.setChecked(showing);
        mNavbarVisibility.setOnPreferenceChangeListener(this);

        final boolean isThreeButtonNavbarEnabled = EvolutionUtils.isThemeEnabled("com.android.internal.systemui.navbar.threebutton");
        mNavbarInverse = (SystemSettingSwitchPreference) findPreference(KEY_NAVBAR_INVERSE);
        mNavbarInverse.setEnabled(isThreeButtonNavbarEnabled);
        mNavigationCompactLayout = (SystemSettingSwitchPreference) findPreference(KEY_NAVIGATION_COMPACT_LAYOUT);
        mNavigationCompactLayout.setEnabled(isThreeButtonNavbarEnabled);

        final PreferenceCategory alertSliderCat =
        (PreferenceCategory) findPreference(ALERT_SLIDER_CAT);
        mAlertBlock = (SystemSettingSwitchPreference) findPreference(BLOCK_ALERT);
        boolean mAlertSliderAvailable = res.getBoolean(
            com.android.internal.R.bool.config_hasAlertSlider);
        boolean isPocketEnabled = Settings.System.getInt(resolver, Settings.System.POCKET_JUDGE, 0) == 1;
        mAlertBlock.setEnabled(isPocketEnabled);
        if (!mAlertSliderAvailable && alertSliderCat != null)
            prefScreen.removePreference(alertSliderCat);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == CustomVolumeStyle) {
            int VolumeStyle = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(getContentResolver(),
                    "VOLUME_BAR_STYLES", VolumeStyle, UserHandle.USER_CURRENT);
            CustomVolumeStyle.setSummary(CustomVolumeStyle.getEntries()[VolumeStyle]);
                if (VolumeStyle == 0) {
                   try {
                      mOverlayService.setEnabled(VOLUMEBAR_OVERLAY_STYLE1, false, USER_CURRENT);
                      mOverlayService.setEnabled(VOLUMEBAR_OVERLAY_STYLE2, false, USER_CURRENT);
                      mOverlayService.setEnabled(VOLUMEBAR_OVERLAY_STYLE3, false, USER_CURRENT);
                      mOverlayService.setEnabled(VOLUMEBAR_OVERLAY_STYLE4, false, USER_CURRENT);
                      mOverlayService.setEnabled(VOLUMEBAR_OVERLAY_STYLE5, false, USER_CURRENT);     
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
               } else if (VolumeStyle == 1) {
                   try {
                      mOverlayService.setEnabledExclusiveInCategory(VOLUMEBAR_OVERLAY_STYLE1, USER_CURRENT);   
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
               } else if (VolumeStyle == 2) {
                   try {
                      mOverlayService.setEnabledExclusiveInCategory(VOLUMEBAR_OVERLAY_STYLE2, USER_CURRENT);   
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
                } else if (VolumeStyle == 3) {
                   try {
                      mOverlayService.setEnabledExclusiveInCategory(VOLUMEBAR_OVERLAY_STYLE3, USER_CURRENT);     
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
                } else if (VolumeStyle == 4) {
                   try {
                      mOverlayService.setEnabledExclusiveInCategory(VOLUMEBAR_OVERLAY_STYLE4, USER_CURRENT);     
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
                } else if (VolumeStyle == 5) {
                   try {
                      mOverlayService.setEnabledExclusiveInCategory(VOLUMEBAR_OVERLAY_STYLE5, USER_CURRENT);     
                   } catch (RemoteException re) {
                      throw re.rethrowFromSystemServer();
                   }
                }    
            return true;
          }
        if (preference == mHwKeyDisable) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(), Settings.System.HARDWARE_KEYS_DISABLE,
                    value ? 1 : 0);
            setActionPreferencesEnabled(!value);
            return true;
        } else if (preference == mNavbarVisibility) {
            if (mIsNavSwitchingMode) {
                return false;
            }
            mIsNavSwitchingMode = true;
            boolean showing = ((Boolean)newValue);
            Settings.System.putIntForUser(resolver, Settings.System.FORCE_SHOW_NAVBAR,
                    showing ? 1 : 0, UserHandle.USER_CURRENT);
            mNavbarVisibility.setChecked(showing);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIsNavSwitchingMode = false;
                }
            }, 1500);
            return true;
        }
        return false;
    }

    private static boolean isKeyDisablerSupported(Context context) {
        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return hardware.isSupported(LineageHardwareManager.FEATURE_KEY_DISABLE);
    }

    private static boolean isKeySwapperSupported(Context context) {
        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return hardware.isSupported(LineageHardwareManager.FEATURE_KEY_SWAP);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference.getKey() == null) {
            // Auto-key preferences that don't have a key, so the dialog can find them.
            preference.setKey(UUID.randomUUID().toString());
        }
        DialogFragment f = null;
        if (preference instanceof CustomDialogPreference) {
            f = CustomDialogPreference.CustomPreferenceDialogFragment
                    .newInstance(preference.getKey());
        } else {
            super.onDisplayPreferenceDialog(preference);
            return;
        }
        f.setTargetFragment(this, 0);
        f.show(getFragmentManager(), "dialog_preference");
        onDialogShowing();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EVOLVER;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.evolution_settings_buttons) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    LineageHardwareManager mLineageHardware = LineageHardwareManager.getInstance(context);

                    if (!isKeyDisablerSupported(context)) {
                        keys.add(HWKEY_DISABLE);
                        keys.add(CATEGORY_HWKEY);
                        keys.add(CATEGORY_BACK);
                        keys.add(CATEGORY_HOME);
                        keys.add(CATEGORY_MENU);
                        keys.add(CATEGORY_ASSIST);
                        keys.add(CATEGORY_APPSWITCH);
                    }

                    return keys;
                }
            };
}
