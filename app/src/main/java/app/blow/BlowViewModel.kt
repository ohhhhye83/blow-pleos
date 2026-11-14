package app.blow

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.blow.dj.DjVoice
import app.blow.location.DemoDriveSource
import app.blow.location.Fix
import app.blow.location.LocationSource
import app.blow.location.NaviLocationSource
import app.blow.player.BlowPlayer
import app.blow.player.PreviewCatalog
import app.blow.road.RoadCatalog
import app.blow.road.RoadZone
import app.blow.road.Track
import app.blow.road.ZoneTracker
import app.blow.ui.Palette
import app.blow.ui.Palettes
import app.blow.weather.WeatherMood
import app.blow.weather.WeatherNow
import app.blow.weather.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ZoneDistance(val zone: RoadZone, val meters: Double)

data class BlowUiState(
    val sourceLabel: String? = null,
    val fix: Fix? = null,
    val weather: WeatherNow? = null,
    /** 시연용 날씨 강제 지정. null 이면 실제 날씨. */
    val weatherOverride: WeatherMood? = null,
    val zone: RoadZone? = null,
    val nowPlaying: Track? = null,
    /** 지금 나오는 곡을 켠 도로. 도로를 벗어나도 곡이 끝날 때까지 유지된다. */
    val playingZone: RoadZone? = null,
    val artwork: Bitmap? = null,
    /** 앨범 커버에서 뽑은 색 — 히어로 그라데이션에 쓴다. */
    val artPalette: Palette? = null,
    /** 곡별 커버 색(곡 id → 팔레트). 다가오는 도로 썸네일에 쓴다. */
    val coverPalettes: Map<String, Palette> = emptyMap(),
    val djLine: String? = null,
    val distances: List<ZoneDistance> = emptyList(),
    val error: String? = null,
    /** Pleos 내비 연동 중이면 음악 도로로 길안내를 요청할 수 있다. */
    val canGuide: Boolean = false,
    val guidingTo: RoadZone? = null,
    /** Pleos 내비에서 길안내가 시작됐을 때 띄우는 제안. 값 = 목적지 이름. */
    val routePrompt: String? = null,
    /** Pleos 내비 안내 요약(다음 회전·남은 거리). 길안내가 없으면 null. */
    val guide: app.blow.location.NaviGuide? = null,
    /** 시연용 데모 주행 중인지. 아니면 Pleos 내비 위치를 따라간다. */
    val demo: Boolean = false,
) {
    val mood: WeatherMood? get() = weatherOverride ?: weather?.mood
}

/** Blow 의 두뇌: 위치 → 음악 도로 판정, 실시간 날씨 → 곡 선택, DJ 멘트 → 페이드인. */
class BlowViewModel(app: Application) : AndroidViewModel(app) {

    private val zones = RoadCatalog.all
    private val weatherRepo = WeatherRepository()
    private val previews = PreviewCatalog()
    private val player = BlowPlayer(app, onEnded = ::onTrackEnded)
    private val dj = DjVoice(app)
    private val notifier = RoutePromptNotifier(app)

    /** Blow 화면이 앞에 있는지. 뒤에 있으면 길안내 제안을 알림 팝업으로 띄운다. */
    var foreground = false
        set(value) {
            field = value
            // 길안내 요청 직후엔 시작 신호와 화면 전환 순서가 뒤바뀔 수 있어, 뒤로 갈 때도 한 번 더 확인한다
            val prompt = _state.value.routePrompt
            if (value) notifier.dismiss() else if (prompt != null) notifier.show(prompt)
        }

    private val _state = MutableStateFlow(BlowUiState())
    val state: StateFlow<BlowUiState> = _state.asStateFlow()

    private var driveJob: Job? = null
    private var djJob: Job? = null
    private var weatherJob: Job? = null
    private var naviSource: NaviLocationSource? = null

    init {
        viewModelScope.launch {
            previews.prefetch(RoadCatalog.appleTracks)
            // 커버 색을 미리 뽑아 둔다(썸네일용)
            RoadCatalog.appleTracks.forEach { t ->
                val art = previews.previewFor(t)?.let { previews.artwork(it) } ?: return@forEach
                val p = Palettes.fromArtwork(art)
                _state.update { it.copy(coverPalettes = it.coverPalettes + (t.id to p)) }
            }
        }
        // Blow 는 음악 앱 — 내비는 사용자가 따로 쓰고, Blow 는 켜지는 순간부터 내비 위치를 뒤에서 따라간다
        startNavi()
    }

    fun startNavi() {
        val src = NaviLocationSource(getApplication(), onRouteStarted = { info ->
            val dest = info.name.ifBlank { "목적지" }
            // 내비가 같은 시작 신호를 여러 번 보낼 수 있어 한 번만 묻는다
            if (_state.value.routePrompt != dest) {
                _state.update { it.copy(routePrompt = dest) }
                if (!foreground) notifier.show(dest)
            }
        }, onGuide = { g -> _state.update { it.copy(guide = g) } })
        start(src)
        naviSource = src
        _state.update { it.copy(canGuide = true, demo = false) }
    }

    /** 실제 Pleos 내비 지도 화면을 띄운다. */
    fun openNaviMap() {
        if (naviSource?.showMap() == true) return
        // 데모 주행 중처럼 내비와 연결이 없을 때는 Pleos 내비 앱을 그냥 앞으로 띄운다
        val ctx = getApplication<Application>()
        ctx.packageManager.getLaunchIntentForPackage(PLEOS_NAVI_PACKAGE)
            ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let(ctx::startActivity)
            ?: _state.update { it.copy(error = "내비 지도를 열 수 없어요") }
    }

    /** 길안내 시작 제안에 대한 답. 수락하면 경로 위 음악 도로를 준비했다고 알린다. */
    fun answerRoutePrompt(accept: Boolean) {
        val dest = _state.value.routePrompt
        notifier.dismiss()
        _state.update { it.copy(routePrompt = null) }
        if (accept && dest != null && _state.value.nowPlaying == null) {
            _state.update { it.copy(djLine = "$dest 까지, 지나는 음악 도로마다 어울리는 노래를 불러올게요.") }
        }
    }

    /** 데모 주행을 끝내고 다시 내비를 따라간다. */
    fun endDemo() = startNavi()

    /** 음악 도로 시작점으로 Pleos 내비 길안내. */
    fun guideTo(zone: RoadZone) {
        val ok = naviSource?.guideTo(zone.path.first(), zone.name) == true
        _state.update {
            if (ok) it.copy(guidingTo = zone, error = null) else it.copy(error = "길안내 요청 실패 — 내비 연결을 확인해 주세요")
        }
    }

    fun startDemo() {
        start(DemoDriveSource(RoadCatalog.demoDrive))
        _state.update { it.copy(demo = true) }
    }

    fun stop() {
        driveJob?.cancel()
        djJob?.cancel()
        naviSource = null
        viewModelScope.launch { player.blowOut() }
        _state.update {
            it.copy(
                sourceLabel = null, zone = null, nowPlaying = null, playingZone = null, artwork = null, guide = null,
                djLine = null, canGuide = false, guidingTo = null,
            )
        }
    }

    fun overrideWeather(mood: WeatherMood?) {
        _state.update { it.copy(weatherOverride = mood) }
        _state.value.zone?.let { play(it, announce = false) }
    }

    private fun start(source: LocationSource) {
        stop()
        val tracker = ZoneTracker(zones)
        _state.update { it.copy(sourceLabel = source.label, error = null) }
        driveJob = viewModelScope.launch {
            source.fixes()
                .catch { e -> _state.update { it.copy(error = "${source.label} 연결 실패: ${e.message}") } }
                .collect { fix -> onFix(fix, tracker) }
        }
    }

    private fun onFix(fix: Fix, tracker: ZoneTracker) {
        _state.update { s ->
            s.copy(
                fix = fix,
                distances = tracker.distancesMeters(fix.position)
                    .map { (z, d) -> ZoneDistance(z, d) }
                    .sortedBy { it.meters },
            )
        }
        refreshWeather(fix)
        when (val e = tracker.update(fix.position)) {
            is ZoneTracker.Event.Entered -> {
                _state.update { it.copy(zone = e.zone) }
                play(e.zone, announce = true)
            }
            // 도로는 노래를 켜는 스위치 — 구간을 빨리 지나도 곡은 끝까지 들려준다
            is ZoneTracker.Event.Exited -> _state.update { it.copy(zone = null, djLine = null) }
            null -> Unit
        }
    }

    private fun refreshWeather(fix: Fix) {
        if (weatherJob?.isActive == true) return
        weatherJob = viewModelScope.launch {
            weatherRepo.current(fix.position)?.let { w -> _state.update { it.copy(weather = w) } }
        }
    }

    private fun play(zone: RoadZone, announce: Boolean) {
        val s = _state.value
        val track = zone.trackFor(s.mood)
        val line = dj.script(zone, s.mood, track)
        djJob?.cancel()
        djJob = viewModelScope.launch {
            val preview = previews.previewFor(track)
            // 미리듣기를 못 받으면(오프라인) 같은 무드의 데모 음원으로
            val (playing, uri) = if (preview != null) track to preview.audioUrl
            else RoadCatalog.offlineFor(s.mood).let { it to "asset:///${it.assetPath}" }
            _state.update { it.copy(nowPlaying = playing, playingZone = zone, djLine = line, artwork = null, artPalette = null) }
            launch {
                preview?.let { previews.artwork(it) }?.let { art ->
                    _state.update { it.copy(artwork = art, artPalette = Palettes.fromArtwork(art)) }
                }
            }
            if (announce) dj.say(line)
            player.blowIn(playing, uri)
        }
    }

    /** 곡이 끝났을 때: 아직 음악 도로 안이면 다시, 벗어났으면 조용히 마무리. */
    private fun onTrackEnded(track: Track) {
        if (_state.value.zone != null) {
            player.replay()
        } else {
            player.finished()
            _state.update { if (it.nowPlaying?.id == track.id) it.copy(nowPlaying = null, playingZone = null, artwork = null) else it }
        }
    }

    private companion object {
        const val PLEOS_NAVI_PACKAGE = "ai.umos.maps.android.navigation.app"
    }

    override fun onCleared() {
        player.release()
        dj.release()
    }
}
