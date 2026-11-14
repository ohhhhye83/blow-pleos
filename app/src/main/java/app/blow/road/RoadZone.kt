package app.blow.road

import app.blow.geo.LatLng
import app.blow.weather.WeatherMood

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    /** Apple Music 곡 ID. 있으면 공식 30초 미리듣기(하이라이트)를 스트리밍한다. */
    val appleId: Long? = null,
    /** app/src/main/assets 기준 데모 음원. 미리듣기를 못 받을 때(오프라인) 쓴다. 예: "music/city_sun.mp3" */
    val assetPath: String? = null,
)

/**
 * "음악 도로" 한 구간. 진입하면 그때의 무드(낮/밤 × 날씨)에 맞는 곡이 불어온다.
 * path 는 도로 중심선, corridorMeters 는 도로 폭 + GPS 오차 여유.
 */
data class RoadZone(
    val id: String,
    val name: String,
    val tagline: String,
    val path: List<LatLng>,
    val corridorMeters: Double = 60.0,
    val playlist: Map<WeatherMood, Track>,
    val fallback: Track,
) {
    fun trackFor(mood: WeatherMood?): Track = mood?.let { playlist[it] } ?: fallback
}
