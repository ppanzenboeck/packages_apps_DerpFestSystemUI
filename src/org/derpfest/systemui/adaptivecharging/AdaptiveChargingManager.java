/*
 * Copyright (C) 2022 StatiXOS
 * SPDX-License-Identifer: Apache-2.0
 */

package org.derpfest.systemui.adaptivecharging;

import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.LocaleList;
import android.os.ParcelFormatException;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;

import vendor.google.google_battery.ChargingStage;
import vendor.google.google_battery.IGoogleBattery;

import java.util.Locale;
import java.util.NoSuchElementException;

public class AdaptiveChargingManager {

    private static final boolean DEBUG = Log.isLoggable("AdaptiveChargingManager", 3);
    private static final String TAG = "AdaptiveChargingManager";

    private Context mContext;

    public AdaptiveChargingManager(Context context) {
        mContext = context;
    }

    public interface AdaptiveChargingStatusReceiver {
        void onDestroyInterface();

        void onReceiveStatus(int seconds, String stage);
    }

    private Locale getLocale() {
        LocaleList locales = mContext.getResources().getConfiguration().getLocales();
        return (locales == null || locales.isEmpty()) ? Locale.getDefault() : locales.get(0);
    }

    public String formatTimeToFull(long j) {
        return DateFormat.format(
                        DateFormat.getBestDateTimePattern(
                                getLocale(), DateFormat.is24HourFormat(mContext) ? "Hm" : "hma"),
                        j)
                .toString();
    }

    public boolean hasAdaptiveChargingFeature() {
        return mContext.getPackageManager()
                .hasSystemFeature("com.google.android.feature.ADAPTIVE_CHARGING");
    }

    public boolean isAvailable() {
        return hasAdaptiveChargingFeature()
                && DeviceConfig.getBoolean("adaptive_charging", "adaptive_charging_enabled", true);
    }

    public boolean getEnabled() {
        return Settings.Secure.getInt(mContext.getContentResolver(), "adaptive_charging", 1) == 1;
    }

    public void setEnabled(boolean on) {
        Settings.Secure.putInt(mContext.getContentResolver(), "adaptive_charging", on ? 1 : 0);
    }

    public static boolean isStageActive(String stage) {
        return "Active".equals(stage);
    }

    public static boolean isStageEnabled(String stage) {
        return "Enabled".equals(stage);
    }

    public static boolean isStageActiveOrEnabled(String stage) {
        return isStageActive(stage) || isStageEnabled(stage);
    }

    public static boolean isActive(String state, int seconds) {
        return isStageActiveOrEnabled(state) && seconds > 0;
    }

    public boolean setAdaptiveChargingDeadline(int secondsFromNow) {
        IGoogleBattery googBatteryInterface = initHalInterface(null);
        if (googBatteryInterface == null) {
            return false;
        }
        boolean result = false;
        try {
            googBatteryInterface.setChargingDeadline(secondsFromNow);
            result = true;
        } catch (RemoteException e) {
            Log.e(TAG, "setChargingDeadline() failed");
        }
        destroyHalInterface(googBatteryInterface, null);
        return result;
    }

    public void queryStatus(final AdaptiveChargingStatusReceiver adaptiveChargingStatusReceiver) {
        IBinder.DeathRecipient deathRecipient =
                new IBinder.DeathRecipient() {
                    @Override
                    public final void binderDied() {
                        if (DEBUG) {
                            Log.d("AdaptiveChargingManager", "serviceDied");
                        }
                        adaptiveChargingStatusReceiver.onDestroyInterface();
                    }
                };
        IGoogleBattery googBatteryIntf = initHalInterface(deathRecipient);
        if (googBatteryIntf == null) {
            adaptiveChargingStatusReceiver.onDestroyInterface();
            return;
        }
        try {
            ChargingStage stage = googBatteryIntf.getChargingStageAndDeadline();
            adaptiveChargingStatusReceiver.onReceiveStatus(stage.deadlineSecs, stage.stage);
        } catch (RemoteException | ParcelFormatException e) {
            Log.e("AdaptiveChargingManager", "Failed to get Adaptive Charging status: ", e);
        }
        destroyHalInterface(googBatteryIntf, deathRecipient);
        adaptiveChargingStatusReceiver.onDestroyInterface();
    }

    private static void destroyHalInterface(
            IGoogleBattery iGoogleBattery, IBinder.DeathRecipient deathRecipient) {
        if (DEBUG) {
            Log.d("AdaptiveChargingManager", "destroyHalInterface");
        }
        if (deathRecipient != null && iGoogleBattery != null) {
            iGoogleBattery.asBinder().unlinkToDeath(deathRecipient, 0);
        }
    }

    private static IGoogleBattery initHalInterface(IBinder.DeathRecipient deathReceiver) {
        if (DEBUG) {
            Log.d("AdaptiveChargingManager", "initHalInterface");
        }
        try {
            IBinder binder =
                    Binder.allowBlocking(
                            ServiceManager.waitForDeclaredService(
                                    "vendor.google.google_battery.IGoogleBattery/default"));
            IGoogleBattery batteryInterface = null;
            if (binder != null) {
                batteryInterface = IGoogleBattery.Stub.asInterface(binder);
                if (batteryInterface != null && deathReceiver != null) {
                    binder.linkToDeath(deathReceiver, 0);
                }
            }
            return batteryInterface;
        } catch (RemoteException | NoSuchElementException | SecurityException e) {
            Log.e("AdaptiveChargingManager", "failed to get Google Battery HAL: ", e);
            return null;
        }
    }
}
