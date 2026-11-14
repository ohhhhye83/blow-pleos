package app.blow.weather

/** 하늘 상태 4종. 안개 등 그 밖의 현상은 흐림으로 묶는다. */
enum class Sky { CLEAR, CLOUDY, RAIN, SNOW }

/**
 * 곡 선택에 쓰는 무드 = 1차 낮/밤 × 2차 맑음·흐림·비·눈 (8종).
 * 차량 센서(와이퍼 등)가 아닌 실제 기상 정보로만 결정한다.
 */
enum class WeatherMood(val isDay: Boolean, val sky: Sky, val label: String, val emoji: String) {
    DAY_CLEAR(true, Sky.CLEAR, "맑은 낮", "☀️"),
    DAY_CLOUDY(true, Sky.CLOUDY, "흐린 낮", "⛅"),
    DAY_RAIN(true, Sky.RAIN, "비 오는 낮", "🌧"),
    DAY_SNOW(true, Sky.SNOW, "눈 오는 낮", "❄️"),
    NIGHT_CLEAR(false, Sky.CLEAR, "맑은 밤", "🌙"),
    NIGHT_CLOUDY(false, Sky.CLOUDY, "흐린 밤", "☁️"),
    NIGHT_RAIN(false, Sky.RAIN, "비 오는 밤", "🌧"),
    NIGHT_SNOW(false, Sky.SNOW, "눈 오는 밤", "❄️");

    companion object {
        fun of(isDay: Boolean, sky: Sky): WeatherMood = entries.first { it.isDay == isDay && it.sky == sky }

        /** WMO weather code(Open-Meteo) + 낮/밤 → 무드. https://open-meteo.com/en/docs */
        fun fromWmo(code: Int, isDay: Boolean): WeatherMood = of(
            isDay,
            when (code) {
                0, 1 -> Sky.CLEAR
                in 51..67, in 80..82, in 95..99 -> Sky.RAIN
                in 71..77, 85, 86 -> Sky.SNOW
                else -> Sky.CLOUDY
            },
        )
    }
}

data class WeatherNow(
    val mood: WeatherMood,
    val temperatureC: Double?,
    val fetchedAtMillis: Long,
)
