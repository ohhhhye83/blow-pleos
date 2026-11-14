package app.blow.location

import android.content.Context
import android.os.SystemClock
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import app.blow.geo.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pleos NaviHelper SDK 로 차량 현재 위치를 받는다.
 * (FusedLocation SDK 는 아직 Coming Soon 이라 NaviHelper 의 위치 정보를 사용)
 * Navigation 앱이 위치를 푸시해 주지만, 상태에 따라 끊길 수 있어 조용할 때만 직접 조회한다.
 */
class NaviLocationSource(
    private val context: Context,
    private val quietMillis: Long = 2_500L,
    /** Pleos 내비에서 길안내가 시작되면 — "Blow 로 음악 들으실래요?" 를 띄우는 신호. */
    private val onRouteStarted: (RouteStartInfo) -> Unit = {},
    /** 다음 회전·남은 거리 등 안내 요약. 길안내가 끝나면 null. */
    private val onGuide: (NaviGuide?) -> Unit = {},
) : LocationSource {

    private var tbt: TBTInfo? = null
    private var destName: String? = null
    private var remainMeters: Int? = null
    private var remainSeconds: Int? = null

    private fun publishGuide() {
        val t = tbt ?: return
        val turn = when (t.type) {
            TurnType.TURN_ARRIVE -> NaviGuide.Turn.ARRIVE
            TurnType.TURN_UTURN -> NaviGuide.Turn.UTURN
            else -> when {
                t.type.name.endsWith("_R") || t.type == TurnType.TURN_RIGHT -> NaviGuide.Turn.RIGHT
                t.type.name.endsWith("_L") || t.type == TurnType.TURN_LEFT -> NaviGuide.Turn.LEFT
                else -> NaviGuide.Turn.STRAIGHT
            }
        }
        onGuide(NaviGuide(turn, t.distance, t.description, destName, remainMeters, remainSeconds))
    }

    private fun clearGuide() {
        tbt = null; destName = null; remainMeters = null; remainSeconds = null
        onGuide(null)
    }

    override val label = "Pleos 내비"

    private var navi: NaviHelper? = null

    override fun fixes(): Flow<Fix> = callbackFlow {
        val helper = NaviHelper(context.applicationContext)
        var lastEventAt = 0L
        val listener = object : NaviHelperEventListener {
            override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
                lastEventAt = SystemClock.elapsedRealtime()
                trySend(
                    Fix(
                        position = LatLng(
                            currentLocationInfo.latitude.toDouble(),
                            currentLocationInfo.longitude.toDouble(),
                        ),
                        headingDeg = currentLocationInfo.heading.toDouble(),
                        address = currentLocationInfo.address,
                    )
                )
            }

            override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
                Log.i(TAG, "route started: ${routeStartInfo.name} ${routeStartInfo.distance}m")
                onRouteStarted(routeStartInfo)
            }

            override fun onTBTInfo(tbtInfo: List<TBTInfo>) {
                tbt = tbtInfo.firstOrNull()
                if (tbt == null) clearGuide() else publishGuide()
            }

            override fun onDestinationInfo(destinationInfo: DestinationInfo) {
                destName = destinationInfo.name
                remainMeters = destinationInfo.remainDistance
                remainSeconds = destinationInfo.remainTime
                publishGuide()
            }

            override fun onDrivingInfo(drivingInfo: DrivingInfo) {
                drivingInfo.destination.let {
                    destName = it.name
                    remainMeters = it.distance
                    remainSeconds = it.duration
                }
                publishGuide()
            }

            override fun onRouteEnded() = clearGuide()
            override fun onRouteCancelled() = clearGuide()
            override fun onDestinationArrived(destinationArrivedInfo: ai.pleos.playground.navi.data.DestinationArrivedInfo) = clearGuide()

            override fun onError(errorCode: NaviErrorCode) {
                Log.w(TAG, "navi error: $errorCode")
            }
        }
        helper.initialize(
            onConnected = {
                navi = helper
                helper.addListener(listener)
                launch {
                    var tick = 0
                    while (isActive) {
                        if (SystemClock.elapsedRealtime() - lastEventAt > quietMillis) {
                            runCatching { helper.getCurrentLocationInfo() }
                                .onFailure { Log.w(TAG, "getCurrentLocationInfo failed", it) }
                        }
                        // 안내 요약은 3초마다 다시 묻는다(푸시가 없을 때 대비)
                        if (tick++ % 3 == 0) {
                            runCatching { helper.getTBTInfo(); helper.getDestinationInfo() }
                                .onFailure { Log.w(TAG, "guide info failed", it) }
                        }
                        delay(1_000L)
                    }
                }
            },
            onFailed = { t ->
                Log.e(TAG, "NaviHelper connection failed", t)
                close(t)
            },
        )
        awaitClose {
            navi = null
            runCatching { helper.removeListener(listener) }
            runCatching { helper.release() }
        }
    }

    /** Pleos 내비에 목적지를 설정해 음악 도로까지 길안내를 시작한다. */
    fun guideTo(target: LatLng, name: String): Boolean {
        val helper = navi ?: return false
        return runCatching {
            helper.requestRoute(
                RouteInfo(
                    latitude = target.lat,
                    longitude = target.lng,
                    displayLongitude = null,
                    displayLatitude = null,
                    poiName = name,
                    poiId = "",
                    poiSubId = "0",
                    address = name,
                )
            )
        }.onFailure { Log.e(TAG, "requestRoute failed", it) }.isSuccess
    }

    /** 실제 Pleos 내비 지도를 앞으로 띄운다(현재 위치로 이동). Blow 는 지도를 직접 그리지 않는다. */
    fun showMap(): Boolean {
        val helper = navi ?: return false
        return runCatching { helper.requestNavigationScreen(NavigationScreenRequest.MoveToCurrentLocationOnMap) }
            .onFailure { Log.e(TAG, "requestNavigationScreen failed", it) }.isSuccess
    }

    private companion object {
        const val TAG = "Blow/Navi"
    }
}
