package app.blow.location

import app.blow.geo.Geo
import app.blow.geo.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * PT 시연용 가상 주행. 경로를 일정 속도로 따라가며 1초마다 위치를 낸다.
 * 실제 차량/내비 없이도 "도로 진입 → 곡이 불어오는" 장면을 재현한다.
 */
class DemoDriveSource(
    private val route: List<LatLng>,
    private val speedKmh: Double = 60.0,
    private val tickMillis: Long = 1_000L,
) : LocationSource {

    override val label = "데모 주행"

    override fun fixes(): Flow<Fix> = flow {
        val stepMeters = speedKmh / 3.6 * (tickMillis / 1000.0)
        for (i in 0 until route.size - 1) {
            val a = route[i]
            val b = route[i + 1]
            val steps = (Geo.distanceMeters(a, b) / stepMeters).toInt().coerceAtLeast(1)
            for (s in 0 until steps) {
                emit(Fix(Geo.lerp(a, b, s.toDouble() / steps), address = "가상 경로 ${speedKmh.toInt()}km/h"))
                delay(tickMillis)
            }
        }
        emit(Fix(route.last(), address = "도착"))
    }
}
