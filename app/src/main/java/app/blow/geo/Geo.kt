package app.blow.geo

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class LatLng(val lat: Double, val lng: Double)

object Geo {
    private const val METERS_PER_DEG_LAT = 111_320.0

    /** 점 p 에서 폴리라인까지의 최단 거리(m). 도로 구간 판정용이라 수 km 범위의 평면 근사로 충분하다. */
    fun distanceToPolylineMeters(p: LatLng, line: List<LatLng>): Double {
        if (line.isEmpty()) return Double.MAX_VALUE
        if (line.size == 1) return distanceMeters(p, line[0])
        val mPerDegLng = METERS_PER_DEG_LAT * cos(Math.toRadians(p.lat))
        fun x(q: LatLng) = (q.lng - p.lng) * mPerDegLng
        fun y(q: LatLng) = (q.lat - p.lat) * METERS_PER_DEG_LAT

        var best = Double.MAX_VALUE
        for (i in 0 until line.size - 1) {
            val ax = x(line[i]); val ay = y(line[i])
            val bx = x(line[i + 1]); val by = y(line[i + 1])
            val dx = bx - ax; val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else max(0.0, min(1.0, -(ax * dx + ay * dy) / len2))
            best = min(best, hypot(ax + t * dx, ay + t * dy))
        }
        return best
    }

    fun distanceMeters(a: LatLng, b: LatLng): Double {
        val mPerDegLng = METERS_PER_DEG_LAT * cos(Math.toRadians((a.lat + b.lat) / 2))
        return hypot((a.lat - b.lat) * METERS_PER_DEG_LAT, (a.lng - b.lng) * mPerDegLng)
    }

    /** a→b 사이를 fraction(0..1) 만큼 선형 보간. */
    fun lerp(a: LatLng, b: LatLng, fraction: Double) =
        LatLng(a.lat + (b.lat - a.lat) * fraction, a.lng + (b.lng - a.lng) * fraction)
}
