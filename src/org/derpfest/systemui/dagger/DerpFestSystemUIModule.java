/*
 * Copyright (C) 2023 The Android Open Source Project
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

package org.derpfest.systemui.dagger;

import static com.android.systemui.Dependency.ALLOW_NOTIFICATION_LONG_PRESS_NAME;
import static com.android.systemui.Dependency.LEAK_REPORT_EMAIL_NAME;

import android.content.Context;
import android.hardware.SensorPrivacyManager;
import android.os.Handler;

import com.android.internal.logging.UiEventLogger;
import com.android.keyguard.KeyguardViewController;
import com.android.systemui.ScreenDecorationsModule;
import com.android.systemui.accessibility.AccessibilityModule;
import com.android.systemui.accessibility.SystemActionsModule;
import com.android.systemui.accessibility.data.repository.AccessibilityRepositoryModule;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.battery.BatterySaverModule;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.display.ui.viewmodel.ConnectingDisplayViewModel;
import com.android.systemui.dock.DockManager;
import com.android.systemui.dock.DockManagerImpl;
import com.android.systemui.doze.DozeHost;
import com.android.systemui.inputdevice.tutorial.KeyboardTouchpadTutorialModule;
import com.android.systemui.keyboard.shortcut.ShortcutHelperModule;
import com.android.systemui.keyguard.ui.composable.blueprint.DefaultBlueprintModule;
import com.android.systemui.keyguard.ui.view.layout.blueprints.KeyguardBlueprintModule;
import com.android.systemui.keyguard.ui.view.layout.sections.KeyguardSectionsModule;
import com.android.systemui.media.dagger.MediaModule;
import com.android.systemui.media.muteawait.MediaMuteAwaitConnectionCli;
import com.android.systemui.media.nearby.NearbyMediaDevicesManager;
import com.android.systemui.navigationbar.NavigationBarControllerModule;
import com.android.systemui.navigationbar.gestural.GestureModule;
import com.android.systemui.plugins.qs.QSFactory;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.dagger.QSModule;
import com.android.systemui.qs.tileimpl.QSFactoryImpl;
import com.android.systemui.reardisplay.RearDisplayModule;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.RecentsImplementation;
import com.android.systemui.rotationlock.RotationLockModule;
import com.android.systemui.rotationlock.RotationLockNewModule;
import com.android.systemui.scene.SceneContainerFrameworkModule;
import com.android.systemui.screenshot.ReferenceScreenshotModule;
import com.android.systemui.settings.MultiUserUtilsModule;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.shade.NotificationShadeWindowControllerImpl;
import com.android.systemui.shade.ShadeExpansionStateManager;
import com.android.systemui.shade.ShadeModule;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.KeyboardShortcutsModule;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationLockscreenUserManagerImpl;
import com.android.systemui.statusbar.NotificationShadeWindowController;
import com.android.systemui.statusbar.dagger.StartCentralSurfacesModule;
import com.android.systemui.statusbar.notification.collection.provider.VisualStabilityProvider;
import com.android.systemui.statusbar.notification.collection.render.GroupMembershipManager;
import com.android.systemui.statusbar.phone.DozeServiceHost;
import com.android.systemui.statusbar.phone.HeadsUpModule;
import com.android.systemui.statusbar.phone.KeyguardBypassController;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragmentStartableModule;
import com.android.systemui.statusbar.policy.AccessibilityManagerWrapper;
import com.android.systemui.statusbar.policy.AospPolicyModule;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.DeviceProvisionedControllerImpl;
import com.android.systemui.statusbar.policy.IndividualSensorPrivacyController;
import com.android.systemui.statusbar.policy.IndividualSensorPrivacyControllerImpl;
import com.android.systemui.statusbar.policy.SensorPrivacyController;
import com.android.systemui.statusbar.policy.SensorPrivacyControllerImpl;
import com.android.systemui.toast.ToastModule;
import com.android.systemui.touchpad.tutorial.TouchpadTutorialModule;
import com.android.systemui.unfold.SysUIUnfoldStartableModule;
import com.android.systemui.unfold.UnfoldTransitionModule;
import com.android.systemui.util.kotlin.SysUICoroutinesModule;
import com.android.systemui.volume.dagger.VolumeModule;
import com.android.systemui.wallpapers.dagger.WallpaperModule;

import org.derpfest.systemui.assist.DerpFestAssistManager;
import org.derpfest.systemui.keyguard.DerpFestKeyguardIndicationController;
import org.derpfest.systemui.qs.tileimpl.DerpFestQSModule;
import org.derpfest.systemui.power.dagger.DerpFestPowerModule;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

import javax.inject.Named;

/**
 * A dagger module for overriding the default implementations of injected System UI components on
 * DerpFest. This is forked from {@link ReferenceSystemUIModule}
 */
@Module(includes = {
        AccessibilityModule.class,
        AccessibilityRepositoryModule.class,
        AospPolicyModule.class,
        BatterySaverModule.class,
        CollapsedStatusBarFragmentStartableModule.class,
        ConnectingDisplayViewModel.StartableModule.class,
        DefaultBlueprintModule.class,
        GestureModule.class,
        HeadsUpModule.class,
        KeyboardShortcutsModule.class,
        KeyguardBlueprintModule.class,
        KeyguardSectionsModule.class,
        KeyboardTouchpadTutorialModule.class,
        MediaModule.class,
        MediaMuteAwaitConnectionCli.StartableModule.class,
        MultiUserUtilsModule.class,
        NavigationBarControllerModule.class,
        NearbyMediaDevicesManager.StartableModule.class,
        QSModule.class,
        RearDisplayModule.class,
        ReferenceScreenshotModule.class,
        RotationLockModule.class,
        RotationLockNewModule.class,
        ScreenDecorationsModule.class,
        SystemActionsModule.class,
        ShadeModule.class,
        StartCentralSurfacesModule.class,
        SceneContainerFrameworkModule.class,
        SysUICoroutinesModule.class,
        SysUIUnfoldStartableModule.class,
        UnfoldTransitionModule.Startables.class,
        ToastModule.class,
        TouchpadTutorialModule.class,
        VolumeModule.class,
        WallpaperModule.class,
        ShortcutHelperModule.class,
        DerpFestPowerModule.class,
        DerpFestQSModule.class
})
public abstract class DerpFestSystemUIModule {

    @SysUISingleton
    @Provides
    @Named(LEAK_REPORT_EMAIL_NAME)
    static String provideLeakReportEmail() {
        return "";
    }

    @Binds
    abstract NotificationLockscreenUserManager bindNotificationLockscreenUserManager(
            NotificationLockscreenUserManagerImpl notificationLockscreenUserManager);

    @Provides
    @SysUISingleton
    static SensorPrivacyController provideSensorPrivacyController(
            SensorPrivacyManager sensorPrivacyManager) {
        SensorPrivacyController spC = new SensorPrivacyControllerImpl(sensorPrivacyManager);
        spC.init();
        return spC;
    }

    @Provides
    @SysUISingleton
    static IndividualSensorPrivacyController provideIndividualSensorPrivacyController(
            SensorPrivacyManager sensorPrivacyManager, UserTracker userTracker) {
        IndividualSensorPrivacyController ispC = new IndividualSensorPrivacyControllerImpl(
                sensorPrivacyManager, userTracker);
        ispC.init();
        return ispC;
    }

    /** */
    @Binds
    @SysUISingleton
    public abstract QSFactory bindQSFactory(QSFactoryImpl qsFactoryImpl);

    @Binds
    abstract DockManager bindDockManager(DockManagerImpl dockManager);

    @SysUISingleton
    @Provides
    @Named(ALLOW_NOTIFICATION_LONG_PRESS_NAME)
    static boolean provideAllowNotificationLongPress() {
        return true;
    }

    @Provides
    @SysUISingleton
    static Recents provideRecents(Context context, RecentsImplementation recentsImplementation,
            CommandQueue commandQueue) {
        return new Recents(context, recentsImplementation, commandQueue);
    }

    @SysUISingleton
    @Provides
    static DeviceProvisionedController bindDeviceProvisionedController(
            DeviceProvisionedControllerImpl deviceProvisionedController) {
        deviceProvisionedController.init();
        return deviceProvisionedController;
    }

    @Binds
    abstract KeyguardViewController bindKeyguardViewController(
            StatusBarKeyguardViewManager statusBarKeyguardViewManager);

    @Binds
    abstract NotificationShadeWindowController bindNotificationShadeController(
            NotificationShadeWindowControllerImpl notificationShadeWindowController);

    @Binds
    abstract DozeHost provideDozeHost(DozeServiceHost dozeServiceHost);

    @Binds
    abstract AssistManager bindAssistManager(DerpFestAssistManager assistManager);
    
    @Binds
    abstract KeyguardIndicationController bindKeyguardIndicationController(
            DerpFestKeyguardIndicationController impl);
}
