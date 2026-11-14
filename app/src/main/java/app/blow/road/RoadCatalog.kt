package app.blow.road

import app.blow.geo.LatLng
import app.blow.weather.Sky
import app.blow.weather.WeatherMood
import app.blow.weather.WeatherMood.DAY_CLEAR
import app.blow.weather.WeatherMood.DAY_CLOUDY
import app.blow.weather.WeatherMood.DAY_RAIN
import app.blow.weather.WeatherMood.DAY_SNOW
import app.blow.weather.WeatherMood.NIGHT_CLEAR
import app.blow.weather.WeatherMood.NIGHT_CLOUDY
import app.blow.weather.WeatherMood.NIGHT_RAIN
import app.blow.weather.WeatherMood.NIGHT_SNOW

/**
 * 기본 큐레이션 "음악 도로". 도로마다 8무드(낮/밤 × 맑음·흐림·비·눈) 곡을 지정한다.
 * 곡은 Apple Music ID 로 지정하고 공식 30초 미리듣기를 재생한다 — 음원 파일은 앱에 넣지 않는다.
 * TODO(데이터): 좌표는 대략값 — 지도에서 도로 중심선을 따서 교체할 것.
 */
object RoadCatalog {

    private fun apple(id: Long, title: String, artist: String) = Track("apple-$id", title, artist, appleId = id)

    // 큐레이션 곡 (Apple Music 한국 스토어 ID)
    private val fairyTale = apple(1818078188, "세상아 동화처럼", "최유리")
    private val lingLing = apple(1587319446, "Ling Ling", "검정치마")
    private val rainyDay = apple(1849575549, "Rainy day", "파테코")
    private val firstSnow = apple(854908536, "첫 눈", "EXO")
    private val anotherDay = apple(1071484927, "또 하루", "개리")
    private val echo = apple(1647771659, "Echo", "오월오일")
    private val dearMyAll = apple(1661490832, "나의 모든 이들에게", "밍기뉴")
    private val snowman = apple(1440098017, "Snowman", "Sia")
    private val swanSong = apple(1728928907, "Swan Song", "LE SSERAFIM")
    private val rain = apple(1080296146, "Rain", "태연")
    private val newDay = apple(1732909854, "New Day", "폴킴")
    private val goodbyeSummer = apple(1828393597, "시간을 달리네", "한로로")
    private val nightSea = apple(1713942141, "밤, 바다", "최유리")
    private val umbrella = apple(1610080888, "우산", "에픽하이")
    private val candyCane = apple(1660338866, "Candy Cane Lane", "Sia")

    /** 오프라인 데모 음원 — Pixabay Content License. 출처: docs/MUSIC_CREDITS.md */
    private val offline = mapOf(
        DAY_CLEAR to Track("city_sun", "Sunny Ocean Drive", "Noobehouse", assetPath = "music/city_sun.mp3"),
        DAY_CLOUDY to Track("grey_avenue", "Sweet Cloudy Lofi", "AbsoluteSound", assetPath = "music/grey_avenue.mp3"),
        DAY_RAIN to Track("rain_on_glass", "Rainy Day Lofi", "Kulakovka", assetPath = "music/rain_on_glass.mp3"),
        DAY_SNOW to Track("white_river", "Evening Snowfall", "Clavier-Music", assetPath = "music/white_river.mp3"),
        NIGHT_CLEAR to Track("neon_drive", "Midnight Drive", "MVNoCopyrightMusic", assetPath = "music/neon_drive.mp3"),
        NIGHT_CLOUDY to Track("han_river_lights", "City Lights", "LofCosmos", assetPath = "music/han_river_lights.mp3"),
        NIGHT_RAIN to Track("rain_on_glass", "Rainy Day Lofi", "Kulakovka", assetPath = "music/rain_on_glass.mp3"),
        NIGHT_SNOW to Track("white_river", "Evening Snowfall", "Clavier-Music", assetPath = "music/white_river.mp3"),
    )

    /** 미리듣기를 못 받을 때 같은 무드의 데모 음원. */
    fun offlineFor(mood: WeatherMood?): Track = offline.getValue(mood ?: WeatherMood.of(true, Sky.CLEAR))

    /** Pleos 에뮬레이터 기본 위치(42dot, 남부순환로 2621) 인근 — 데모 주행이 지나가는 구간. */
    val namburoDemo = RoadZone(
        id = "nambu-sunhwan",
        name = "남부순환로 양재 구간",
        tagline = "도심을 가르는 퇴근길",
        path = listOf(
            LatLng(37.48470, 127.03000),
            LatLng(37.48520, 127.03600),
            LatLng(37.48560, 127.04200),
        ),
        playlist = mapOf(
            DAY_CLEAR to fairyTale,
            DAY_CLOUDY to lingLing,
            DAY_RAIN to rainyDay,
            DAY_SNOW to firstSnow,
            NIGHT_CLEAR to anotherDay,
            NIGHT_CLOUDY to echo,
            NIGHT_RAIN to dearMyAll,
            NIGHT_SNOW to snowman,
        ),
        fallback = fairyTale,
    )

    val olympicBanpo = RoadZone(
        id = "olympic-banpo",
        name = "올림픽대로 반포 구간",
        tagline = "한강이 창밖으로 펼쳐지는 순간",
        path = listOf(
            LatLng(37.50850, 126.99500),
            LatLng(37.51300, 127.00800),
            LatLng(37.51750, 127.02000),
        ),
        corridorMeters = 80.0,
        playlist = mapOf(
            DAY_CLEAR to fairyTale,
            DAY_CLOUDY to swanSong,
            DAY_RAIN to rain,
            DAY_SNOW to newDay,
            NIGHT_CLEAR to goodbyeSummer,
            NIGHT_CLOUDY to nightSea,
            NIGHT_RAIN to umbrella,
            NIGHT_SNOW to candyCane,
        ),
        fallback = fairyTale,
    )

    val all: List<RoadZone> = listOf(namburoDemo, olympicBanpo)

    /** 앱 시작 시 미리듣기 주소를 한 번에 받아 두기 위한 전체 곡 목록. */
    val appleTracks: List<Track> = all.flatMap { it.playlist.values }.filter { it.appleId != null }.distinctBy { it.id }

    /** 데모 주행 경로: 구간 밖에서 출발 → 남부순환로 구간 통과 → 이탈. */
    val demoDrive: List<LatLng> = listOf(
        LatLng(37.48380, 127.02200),
        LatLng(37.48470, 127.03000),
        LatLng(37.48520, 127.03600),
        LatLng(37.48560, 127.04200),
        LatLng(37.48640, 127.05000),
    )
}
