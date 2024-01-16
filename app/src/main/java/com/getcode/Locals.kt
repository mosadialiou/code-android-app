package com.getcode

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.getcode.manager.AnalyticsService
import com.getcode.manager.AnalyticsServiceNull
import com.getcode.util.CurrencyUtils
import com.getcode.util.DeeplinkHandler
import com.getcode.util.PhoneUtils
import com.getcode.utils.network.NetworkConnectivityListener
import com.getcode.utils.network.NetworkObserverStub

val LocalAnalytics: ProvidableCompositionLocal<AnalyticsService> = staticCompositionLocalOf { AnalyticsServiceNull() }
val LocalNetworkObserver: ProvidableCompositionLocal<NetworkConnectivityListener> = staticCompositionLocalOf { NetworkObserverStub() }
val LocalPhoneFormatter: ProvidableCompositionLocal<PhoneUtils?> = staticCompositionLocalOf { null }
val LocalCurrencyUtils: ProvidableCompositionLocal<CurrencyUtils?> = staticCompositionLocalOf { null }
val LocalDeeplinks: ProvidableCompositionLocal<DeeplinkHandler?> = staticCompositionLocalOf { null }
