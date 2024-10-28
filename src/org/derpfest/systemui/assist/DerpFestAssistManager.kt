package org.derpfest.systemui.assist

import android.app.ActivityManager
import android.content.Context
import android.os.Handler
import com.android.internal.app.AssistUtils
import com.android.systemui.assist.AssistManager
import com.android.systemui.assist.AssistLogger
import com.android.systemui.assist.PhoneStateMonitor
import com.android.systemui.assist.domain.interactor.AssistInteractor
import com.android.systemui.assist.ui.DefaultUiController
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.model.SysUiState
import com.android.systemui.recents.OverviewProxyService
import com.android.systemui.settings.DisplayTracker
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.CommandQueue
import com.android.systemui.statusbar.policy.DeviceProvisionedController
import com.android.systemui.user.domain.interactor.SelectedUserInteractor
import com.android.systemui.util.settings.SecureSettings
import dagger.Lazy
import javax.inject.Inject

@SysUISingleton
class DerpFestAssistManager
@Inject
constructor(
  controller: DeviceProvisionedController,
  context: Context,
  assistUtils: AssistUtils,
  commandQueue: CommandQueue,
  phoneStateMonitor: PhoneStateMonitor,
  overviewProxyService: OverviewProxyService,
  sysUiState: Lazy<SysUiState>,
  defaultUiController: DefaultUiController,
  assistLogger: AssistLogger,
  @Main uiHandler: Handler,
  userTracker: UserTracker,
  displayTracker: DisplayTracker,
  secureSettings: SecureSettings,
  selectedUserInteractor: SelectedUserInteractor,
  activityManager: ActivityManager,
  interactor: AssistInteractor,
) :
  AssistManager(
    controller,
    context,
    assistUtils,
    commandQueue,
    phoneStateMonitor,
    overviewProxyService,
    sysUiState,
    defaultUiController,
    assistLogger,
    uiHandler,
    userTracker,
    displayTracker,
    secureSettings,
    selectedUserInteractor,
    activityManager,
    interactor,
  ) {}
