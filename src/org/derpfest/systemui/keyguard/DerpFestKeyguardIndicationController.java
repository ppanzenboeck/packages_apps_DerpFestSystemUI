/*
 * Copyright (C) 2022 StatiXOS
 * SPDX-License-Identifer: Apache-2.0
 */

package org.derpfest.systemui.keyguard;

import android.app.AlarmManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IBatteryStats;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.logging.KeyguardLogger;
import com.android.settingslib.fuelgauge.BatteryStatus;
import com.android.systemui.R;
import com.android.systemui.biometrics.AuthController;
import com.android.systemui.biometrics.FaceHelpMessageDeferral;
import com.android.systemui.biometrics.FaceHelpMessageDeferralFactory;
import com.android.systemui.bouncer.domain.interactor.AlternateBouncerInteractor;
import com.android.systemui.bouncer.domain.interactor.BouncerMessageInteractor;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.deviceentry.domain.interactor.BiometricMessageInteractor;
import com.android.systemui.deviceentry.domain.interactor.DeviceEntryFaceAuthInteractor;
import com.android.systemui.deviceentry.domain.interactor.DeviceEntryFingerprintAuthInteractor;
import com.android.systemui.dock.DockManager;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.domain.interactor.KeyguardInteractor;
import com.android.systemui.keyguard.util.IndicationHelper;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.phone.KeyguardBypassController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.util.DeviceConfigProxy;
import com.android.systemui.util.concurrency.DelayableExecutor;
import com.android.systemui.util.wakelock.WakeLock;

import org.derpfest.systemui.adaptivecharging.AdaptiveChargingManager;

import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

@SysUISingleton
public class DerpFestKeyguardIndicationController extends KeyguardIndicationController {

    private boolean mAdaptiveChargingActive;
    private boolean mAdaptiveChargingEnabledInSettings;
    @VisibleForTesting private AdaptiveChargingManager mAdaptiveChargingManager;

    @VisibleForTesting
    private AdaptiveChargingManager.AdaptiveChargingStatusReceiver mAdaptiveChargingStatusReceiver;

    private int mBatteryLevel;
    private final BroadcastDispatcher mBroadcastDispatcher;
    private final BroadcastReceiver mBroadcastReceiver;
    private final Context mContext;
    private final DeviceConfigProxy mDeviceConfig;
    private long mEstimatedChargeCompletion;
    private boolean mInited;
    private boolean mIsCharging;
    private DerpFestKeyguardCallback mUpdateMonitorCallback;

    private class DerpFestKeyguardCallback extends KeyguardIndicationController.BaseKeyguardCallback {
        private DerpFestKeyguardCallback() {
            super();
        }

        @Override
        public final void onRefreshBatteryInfo(BatteryStatus batteryStatus) {
            super.onRefreshBatteryInfo(batteryStatus);
            mIsCharging = batteryStatus.status == BatteryManager.BATTERY_STATUS_CHARGING;
            mBatteryLevel = batteryStatus.level;
            if (mIsCharging) {
                triggerAdaptiveChargingStatusUpdate();
            } else {
                mAdaptiveChargingActive = false;
            }
        }
    }

    @Inject
    public DerpFestKeyguardIndicationController(
            Context context,
            @Main Looper mainLooper,
            WakeLock.Builder wakeLockBuilder,
            KeyguardStateController keyguardStateController,
            StatusBarStateController statusBarStateController,
            KeyguardUpdateMonitor keyguardUpdateMonitor,
            DockManager dockManager,
            BroadcastDispatcher broadcastDispatcher,
            DevicePolicyManager devicePolicyManager,
            IBatteryStats iBatteryStats,
            UserManager userManager,
            @Main DelayableExecutor executor,
            @Background DelayableExecutor bgExecutor,
            FalsingManager falsingManager,
            AuthController authController,
            LockPatternUtils lockPatternUtils,
            ScreenLifecycle screenLifecycle,
            KeyguardBypassController keyguardBypassController,
            AccessibilityManager accessibilityManager,
            FaceHelpMessageDeferralFactory faceHelpMessageDeferralFactory,
            DeviceConfigProxy deviceConfigProxy,
            KeyguardLogger keyguardLogger,
            AlternateBouncerInteractor alternateBouncerInteractor,
            AlarmManager alarmManager,
            UserTracker userTracker,
            BouncerMessageInteractor bouncerMessageInteractor,
            FeatureFlags flags,
            IndicationHelper indicationHelper,
            KeyguardInteractor keyguardInteractor,
            BiometricMessageInteractor biometricMessageInteractor,
            DeviceEntryFingerprintAuthInteractor deviceEntryFingerprintAuthInteractor,
            DeviceEntryFaceAuthInteractor deviceEntryFaceAuthInteractor) {
        super(
                context,
                mainLooper,
                wakeLockBuilder,
                keyguardStateController,
                statusBarStateController,
                keyguardUpdateMonitor,
                dockManager,
                broadcastDispatcher,
                devicePolicyManager,
                iBatteryStats,
                userManager,
                executor,
                bgExecutor,
                falsingManager,
                authController,
                lockPatternUtils,
                screenLifecycle,
                keyguardBypassController,
                accessibilityManager,
                faceHelpMessageDeferralFactory,
                keyguardLogger,
                alternateBouncerInteractor,
                alarmManager,
                userTracker,
                bouncerMessageInteractor,
                flags,
                indicationHelper,
                keyguardInteractor,
                biometricMessageInteractor,
                deviceEntryFingerprintAuthInteractor,
                deviceEntryFaceAuthInteractor);
        mBroadcastReceiver =
                new BroadcastReceiver() {
                    @Override
                    public final void onReceive(Context context, Intent intent) {
                        if ("com.google.android.systemui.adaptivecharging.ADAPTIVE_CHARGING_DEADLINE_SET"
                                .equals(intent.getAction())) {
                            triggerAdaptiveChargingStatusUpdate();
                        }
                    }
                };
        mAdaptiveChargingStatusReceiver =
                new AdaptiveChargingManager.AdaptiveChargingStatusReceiver() {
                    @Override
                    public void onDestroyInterface() {}

                    @Override
                    public void onReceiveStatus(int seconds, String stage) {
                        boolean wasActive = mAdaptiveChargingActive;
                        mAdaptiveChargingActive = AdaptiveChargingManager.isActive(stage, seconds);
                        long currentEstimation = mEstimatedChargeCompletion;
                        long currentTimeMillis = System.currentTimeMillis();
                        mEstimatedChargeCompletion =
                                TimeUnit.SECONDS.toMillis(seconds + 29) + currentTimeMillis;
                        long abs = Math.abs(mEstimatedChargeCompletion - currentEstimation);
                        if (mAdaptiveChargingActive != wasActive
                                || (mAdaptiveChargingActive
                                        && abs > TimeUnit.SECONDS.toMillis(30L))) {
                            updateDeviceEntryIndication(true);
                        }
                    }
                };
        mContext = context;
        mBroadcastDispatcher = broadcastDispatcher;
        mDeviceConfig = deviceConfigProxy;
        mAdaptiveChargingManager = new AdaptiveChargingManager(context);
    }

    @Override
    public String computePowerIndication() {
        if (mIsCharging && mAdaptiveChargingEnabledInSettings && mAdaptiveChargingActive) {
            String formatTimeToFull =
                    mAdaptiveChargingManager.formatTimeToFull(mEstimatedChargeCompletion);
            return mContext.getResources()
                    .getString(
                            R.string.adaptive_charging_time_estimate,
                            NumberFormat.getPercentInstance().format(mBatteryLevel / 100.0f),
                            formatTimeToFull);
        }
        return super.computePowerIndication();
    }

    @Override
    public final KeyguardUpdateMonitorCallback getKeyguardCallback() {
        if (mUpdateMonitorCallback == null) {
            mUpdateMonitorCallback = new DerpFestKeyguardCallback();
        }
        return mUpdateMonitorCallback;
    }

    private void refreshAdaptiveChargingEnabled() {
        boolean supported = mAdaptiveChargingManager.isAvailable();
        if (supported) {
            mAdaptiveChargingEnabledInSettings = mAdaptiveChargingManager.getEnabled();
        } else {
            mAdaptiveChargingEnabledInSettings = false;
        }
    }

    @Override
    public final void init() {
        super.init();
        if (mInited) {
            return;
        }
        mInited = true;
        DelayableExecutor delayableExecutor = mExecutor;
        mDeviceConfig.addOnPropertiesChangedListener(
                "adaptive_charging",
                delayableExecutor,
                (properties) -> {
                    if (properties.getKeyset().contains("adaptive_charging_enabled")) {
                        triggerAdaptiveChargingStatusUpdate();
                    }
                });
        triggerAdaptiveChargingStatusUpdate();
        mBroadcastDispatcher.registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(
                        "com.google.android.systemui.adaptivecharging.ADAPTIVE_CHARGING_DEADLINE_SET"),
                null,
                UserHandle.ALL);
    }

    public void triggerAdaptiveChargingStatusUpdate() {
        refreshAdaptiveChargingEnabled();
        if (mAdaptiveChargingEnabledInSettings) {
            mAdaptiveChargingManager.queryStatus(mAdaptiveChargingStatusReceiver);
        } else {
            mAdaptiveChargingActive = false;
        }
    }
}
