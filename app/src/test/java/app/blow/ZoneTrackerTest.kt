package app.blow

import app.blow.geo.Geo
import app.blow.geo.LatLng
import app.blow.road.RoadCatalog
import app.blow.road.ZoneTracker
import app.blow.weather.WeatherMood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneTrackerTest {

    private val zone = RoadCatalog.namburoDemo
    private val onRoad = zone.path[1]
    private val farAway = LatLng(37.40, 127.10)

    @Test fun `도로 위 점은 거리 0 근처, 먼 점은 수 km`() {
        assertTrue(Geo.distanceToPolylineMeters(onRoad, zone.path) < 1.0)
        assertTrue(Geo.distanceToPolylineMeters(farAway, zone.path) > 5_000)
    }

    @Test fun `연속 2회 진입해야 Entered`() {
        val t = ZoneTracker(listOf(zone))
        assertNull(t.update(onRoad))
        assertEquals(ZoneTracker.Event.Entered(zone), t.update(onRoad))
    }

    @Test fun `GPS 가 한 번 튀어도 곡이 끊기지 않는다`() {
        val t = ZoneTracker(listOf(zone))
        t.update(onRoad); t.update(onRoad)
        assertNull(t.update(farAway))
        assertNull(t.update(onRoad))
        assertEquals(zone, t.current)
    }

    @Test fun `연속 3회 이탈해야 Exited`() {
        val t = ZoneTracker(listOf(zone))
        t.update(onRoad); t.update(onRoad)
        assertNull(t.update(farAway))
        assertNull(t.update(farAway))
        assertEquals(ZoneTracker.Event.Exited(zone), t.update(farAway))
    }

    @Test fun `데모 주행 경로는 데모 구간을 지난다`() {
        val t = ZoneTracker(RoadCatalog.all)
        val events = RoadCatalog.demoDrive.zipWithNext().flatMap { (a, b) ->
            (0 until 20).map { Geo.lerp(a, b, it / 20.0) }
        }.mapNotNull { t.update(it) }
        assertEquals(ZoneTracker.Event.Entered(zone), events.first())
    }

    @Test fun `8무드 모두 곡이 있고, 날씨를 모르면 기본 곡`() {
        RoadCatalog.all.forEach { z -> WeatherMood.entries.forEach { m -> assertEquals(true, m in z.playlist) } }
        assertEquals("우산", RoadCatalog.olympicBanpo.trackFor(WeatherMood.NIGHT_RAIN).title)
        assertEquals(zone.fallback, zone.trackFor(null))
    }

    @Test fun `WMO 코드 매핑 — 낮밤 × 맑음·흐림·비·눈, 안개는 흐림`() {
        assertEquals(WeatherMood.NIGHT_CLEAR, WeatherMood.fromWmo(0, isDay = false))
        assertEquals(WeatherMood.DAY_RAIN, WeatherMood.fromWmo(61, isDay = true))
        assertEquals(WeatherMood.NIGHT_SNOW, WeatherMood.fromWmo(73, isDay = false))
        assertEquals(WeatherMood.DAY_CLOUDY, WeatherMood.fromWmo(45, isDay = true))
    }
}
