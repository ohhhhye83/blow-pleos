package app.blow.road

import app.blow.geo.Geo
import app.blow.geo.LatLng

/**
 * 위치 스트림을 받아 "지금 어느 음악 도로 위에 있는지"를 판정한다.
 * GPS 튐으로 곡이 켜졌다 꺼졌다 하지 않도록 진입·이탈에 히스테리시스를 둔다.
 *  - 진입: corridor 안쪽 위치가 [enterFixes]번 연속
 *  - 이탈: corridor × [exitFactor] 바깥 위치가 [exitFixes]번 연속
 */
class ZoneTracker(
    private val zones: List<RoadZone>,
    private val enterFixes: Int = 2,
    private val exitFixes: Int = 3,
    private val exitFactor: Double = 1.5,
) {
    sealed interface Event {
        data class Entered(val zone: RoadZone) : Event
        data class Exited(val zone: RoadZone) : Event
    }

    var current: RoadZone? = null
        private set

    private var candidate: RoadZone? = null
    private var candidateCount = 0
    private var outsideCount = 0

    fun distancesMeters(p: LatLng): Map<RoadZone, Double> =
        zones.associateWith { Geo.distanceToPolylineMeters(p, it.path) }

    fun update(p: LatLng): Event? {
        val cur = current
        if (cur != null) {
            val d = Geo.distanceToPolylineMeters(p, cur.path)
            outsideCount = if (d > cur.corridorMeters * exitFactor) outsideCount + 1 else 0
            if (outsideCount < exitFixes) return null
            current = null
            outsideCount = 0
            candidate = null
            candidateCount = 0
            return Event.Exited(cur)
        }

        val nearest = zones
            .map { it to Geo.distanceToPolylineMeters(p, it.path) }
            .filter { (z, d) -> d <= z.corridorMeters }
            .minByOrNull { it.second }
            ?.first

        if (nearest == null) {
            candidate = null
            candidateCount = 0
            return null
        }
        candidateCount = if (nearest == candidate) candidateCount + 1 else 1
        candidate = nearest
        if (candidateCount < enterFixes) return null
        current = nearest
        candidate = null
        candidateCount = 0
        return Event.Entered(nearest)
    }
}
