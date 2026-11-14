package app.blow.weather

import android.util.Log
import app.blow.geo.Geo
import app.blow.geo.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

/**
 * 현재 위치의 실시간 날씨. 키가 필요 없는 공개 API 두 곳을 차례로 쓴다:
 * 노르웨이 기상청(MET Norway) → 안 되면 Open-Meteo. (상용화 시 기상청 초단기실황으로 교체 예정)
 */
class WeatherRepository(
    private val refreshMillis: Long = 10 * 60_000L,
    private val refreshDistanceMeters: Double = 5_000.0,
) {
    private var cached: WeatherNow? = null
    private var cachedAt: LatLng? = null

    suspend fun current(at: LatLng): WeatherNow? {
        val c = cached
        val fresh = c != null && System.currentTimeMillis() - c.fetchedAtMillis < refreshMillis &&
            cachedAt?.let { Geo.distanceMeters(it, at) < refreshDistanceMeters } == true
        if (fresh) return c
        val now = listOf(::fetchMetNorway, ::fetchOpenMeteo).firstNotNullOfOrNull { source ->
            runCatching { source(at) }.onFailure { Log.w(TAG, "fetch failed", it) }.getOrNull()
        }
        return now?.also { cached = it; cachedAt = at } ?: c
    }

    private suspend fun fetchMetNorway(at: LatLng): WeatherNow {
        val body = get(
            "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.4f&lon=%.4f".format(at.lat, at.lng),
            // MET Norway 이용 약관: 앱을 식별하는 User-Agent 필수
            userAgent = "Blow/1.0 (Pleos Connect app contest)",
        )
        val first = JSONObject(body).getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0)
        val data = first.getJSONObject("data")
        val symbol = data.optJSONObject("next_1_hours")?.getJSONObject("summary")?.optString("symbol_code").orEmpty()
        val sky = when {
            "snow" in symbol || "sleet" in symbol -> Sky.SNOW
            "rain" in symbol || "thunder" in symbol -> Sky.RAIN
            symbol.startsWith("clearsky") || symbol.startsWith("fair") -> Sky.CLEAR
            else -> Sky.CLOUDY
        }
        // 맑음 계열은 _day/_night 가 붙어 오고, 비·눈처럼 안 붙는 코드는 현지 시각으로 낮/밤을 정한다
        val isDay = when {
            symbol.endsWith("_night") -> false
            symbol.endsWith("_day") -> true
            else -> Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 7..17
        }
        return WeatherNow(
            mood = WeatherMood.of(isDay, sky),
            temperatureC = data.getJSONObject("instant").getJSONObject("details").optDouble("air_temperature").takeUnless { it.isNaN() },
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }

    private suspend fun fetchOpenMeteo(at: LatLng): WeatherNow {
        val body = get(
            "https://api.open-meteo.com/v1/forecast?latitude=${at.lat}&longitude=${at.lng}" +
                "&current=weather_code,is_day,temperature_2m&timezone=auto"
        )
        val cur = JSONObject(body).getJSONObject("current")
        return WeatherNow(
            mood = WeatherMood.fromWmo(cur.getInt("weather_code"), cur.optInt("is_day", 1) == 1),
            temperatureC = cur.optDouble("temperature_2m").takeUnless { it.isNaN() },
            fetchedAtMillis = System.currentTimeMillis(),
        )
    }

    private suspend fun get(url: String, userAgent: String? = null): String = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            userAgent?.let { setRequestProperty("User-Agent", it) }
        }
        try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val TAG = "Blow/Weather"
    }
}
