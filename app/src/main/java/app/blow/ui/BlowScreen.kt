package app.blow.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.blow.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.blow.BlowUiState
import app.blow.ZoneDistance
import app.blow.road.RoadZone
import app.blow.weather.WeatherMood

/**
 * 차량 가로 화면. 운전 중 한눈에 읽히도록 모든 요소를 크게,
 * 조작은 커다란 타일과 하단 플레이어 바로만.
 */
@Composable
fun BlowScreen(
    state: BlowUiState,
    onStartDemo: () -> Unit,
    onEndDemo: () -> Unit,
    onOpenMap: () -> Unit,
    onRoutePrompt: (Boolean) -> Unit,
    onOverrideWeather: (WeatherMood?) -> Unit,
    onGuide: (RoadZone) -> Unit,
) {
    var showWeather by remember { mutableStateOf(false) }
    if (showWeather) {
        WeatherSheet(state.weather, state.weatherOverride, onSelect = onOverrideWeather, onDismiss = { showWeather = false })
    }
    state.routePrompt?.let { RoutePrompt(it, onRoutePrompt) }
    Box(Modifier.fillMaxSize().background(Ink.Canvas)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(start = 44.dp, end = 44.dp, top = 72.dp, bottom = 40.dp)) {
        // 정사각 히어로는 아래 미니 플레이어 바로 위까지 높이를 채운다 (가로는 화면의 절반 남짓까지만)
        val heroSize = minOf(maxHeight - HEADER_HEIGHT - HEADER_GAP, maxWidth * 0.6f)
        Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            // 왼쪽: 로고 줄 + 정사각 히어로
            Column(Modifier.width(heroSize)) {
                Header(
                    state, onWeatherClick = { showWeather = true },
                    onDemo = if (state.demo) onEndDemo else onStartDemo,
                    modifier = Modifier.height(HEADER_HEIGHT),
                )
                Spacer(Modifier.height(HEADER_GAP))
                Hero(state, Modifier.size(heroSize))
            }
            // 오른쪽: 히어로 위쪽 선에 맞춰 시작하고, 아래쪽 선에 맞춰 끝난다
            Column(Modifier.weight(1f).height(HEADER_HEIGHT + HEADER_GAP + heroSize)) {
                Spacer(Modifier.height(HEADER_HEIGHT + HEADER_GAP))
                // 흰 카드 + 테두리처럼 보이는 옅은 그림자
                val cardShape = RoundedCornerShape(32.dp)
                Column(Modifier.fillMaxWidth().whiteCard(cardShape).padding(32.dp)) {
                    SectionTitle("곧 불어올 노래")
                    Spacer(Modifier.height(14.dp))
                    Roads(state, onGuide)
                }
                Spacer(Modifier.height(34.dp))
                NaviCard(state, onOpenMap, Modifier.fillMaxWidth().weight(1f))
            }
        }
        }
    }
}

// ───────────────────────────── 왼쪽: 제목 + 히어로 ─────────────────────────────

@Composable
private fun Header(state: BlowUiState, onWeatherClick: () -> Unit, onDemo: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.blow_logo), contentDescription = "Blow", tint = Ink.Label,
            modifier = Modifier.height(64.dp),
        )
        Spacer(Modifier.width(24.dp))
        Text(
            state.sourceLabel?.let { "$it · ${state.fix?.address ?: "위치 수신 중"}" } ?: "주행 대기 중",
            color = Ink.Secondary, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(top = 14.dp),
        )
        // 날씨 칩 — 누르면 날씨 선택 팝업
        val mood = state.mood
        val temp = state.weather?.temperatureC?.takeIf { state.weatherOverride == null }
        Row(
            Modifier.clip(RoundedCornerShape(22.dp)).background(Color.White).clickable(onClick = onWeatherClick)
                .padding(start = 22.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                listOfNotNull(mood?.emoji, temp?.let { "%.0f°".format(it) }, mood?.label).joinToString(" ").ifEmpty { "날씨 확인 중" },
                color = Ink.Label, fontSize = 25.sp, fontWeight = FontWeight.SemiBold,
            )
            if (state.weatherOverride != null) {
                Text("  시연", color = Ink.Secondary, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "날씨 선택", tint = Ink.Secondary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(12.dp))
        // 시연용 — 실제 주행 없이 음악 도로 진입 장면을 재현
        Row(
            Modifier.clip(RoundedCornerShape(22.dp)).background(if (state.demo) Color.White else Ink.Label).clickable(onClick = onDemo)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (state.demo) Icons.Filled.Close else Icons.Filled.PlayArrow, contentDescription = null,
                tint = if (state.demo) Ink.Label else Color.White, modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(if (state.demo) "데모 종료" else "데모", color = if (state.demo) Ink.Label else Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 회색 바탕 위 흰 카드 — 테두리처럼 보이는 옅은 그림자. */
fun Modifier.whiteCard(shape: androidx.compose.ui.graphics.Shape): Modifier =
    shadow(16.dp, shape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.12f))
        .clip(shape).background(Color.White)

@Composable
private fun Hero(state: BlowUiState, modifier: Modifier) {
    val onAir = state.nowPlaying != null
    val shape = RoundedCornerShape(38.dp)
    Box(
        modifier
            .shadow(28.dp, shape, ambientColor = Ink.Blaze.copy(alpha = 0.25f), spotColor = Ink.Blaze.copy(alpha = 0.35f))
            .clip(shape),
    ) {
        FlowingArtwork(
            // 노래가 나오면 앨범 커버에서 뽑은 색으로 흐른다
            palette = if (onAir) state.artPalette ?: Palettes.of(state.mood) else Palettes.Sunset,
            // 해는 맑은 낮에만 — 비·눈·흐림·밤엔 띄우지 않는다
            sunRise = if (state.mood.showsSun()) (if (onAir) 1f else 0f) else null,
            modifier = Modifier.fillMaxSize(),
        )
        // 글자(왼쪽 아래)가 어떤 색 위에서도 읽히도록 그 모서리만 살짝 어둡게 — 오른쪽의 해는 밝게 둔다
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    0f to Color.Black.copy(alpha = 0.40f), 0.5f to Color.Transparent,
                    start = Offset(0f, Float.POSITIVE_INFINITY), end = Offset(Float.POSITIVE_INFINITY, 0f),
                )
            )
        )

        Column(Modifier.fillMaxSize().padding(42.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Glass {
                    if (onAir) Equalizer(Color.White) else Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (onAir) "NOW BLOWING" else if (state.fix != null) "CRUISING" else "READY",
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = Triple(state.zone?.id, state.nowPlaying?.id, state.fix != null),
                transitionSpec = { (fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 5 }) togetherWith fadeOut(tween(300)) },
                label = "hero",
            ) { _ ->
                when {
                    state.nowPlaying != null || (state.fix != null && state.distances.isNotEmpty()) -> SongText(state)
                    else -> IdleText()
                }
            }
        }
    }
}

/**
 * 히어로 문구: 위 작은 줄 = 지금 어디(곧 들어설 음악 도로까지 거리), 큰 글씨 = 지금 나오는 노래.
 * 다음 도로에서 무슨 노래가 나올지는 미리 알려 주지 않는다 — 들어서는 순간의 설렘을 남긴다.
 */
@Composable
private fun SongText(state: BlowUiState) {
    val now = state.nowPlaying
    val next = state.distances.firstOrNull { it.zone != state.zone }
    // 노래가 나오면: 어디서 + 곡 제목 + 가수 / 아직이면: 남은 거리 + 다음 음악 도로 이름 + 한 줄
    val top: String?
    val big: String
    val bottom: String?
    if (now != null) {
        top = when {
            state.zone != null -> "지금, ${state.zone.name}"
            else -> state.playingZone?.let { "방금 지나온 ${it.name}" }
        }
        big = now.title
        bottom = now.artist
    } else {
        top = next?.let { "${formatMeters(it.meters)} 뒤" }
        big = next?.zone?.name ?: ""
        bottom = APPROACH_LINE
    }
    Column {
        state.artwork?.takeIf { now != null }?.let { art ->
            Image(
                art.asImageBitmap(), contentDescription = "앨범 아트",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(320.dp).shadow(28.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)),
            )
            Spacer(Modifier.height(24.dp))
        }
        // 위(라벨) — 큰 제목 — 아래(부연): 크기 24 / 80 / 26, 간격 12 / 14 로 리듬을 맞춘다
        // 진입 전: 남은 거리를 유리 알약으로 / 재생 중: 지금 어디인지 라벨로
        if (now == null && next != null) {
            Glass(corner = 22.dp) {
                Text("▲", color = Color.White, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(formatMeters(next.meters), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, style = Tight)
                Text("  남음", color = Color.White.copy(alpha = 0.8f), fontSize = 22.sp, fontWeight = FontWeight.Medium, style = Tight)
            }
            Spacer(Modifier.height(18.dp))
        } else top?.let {
            Text(it, color = Color.White.copy(alpha = 0.78f), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, style = Tight)
            Spacer(Modifier.height(12.dp))
        }
        Text(
            big, color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Black,
            lineHeight = 86.sp, letterSpacing = (-2).sp, maxLines = 2, overflow = TextOverflow.Ellipsis, style = Tight,
        )
        bottom?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, color = Color.White.copy(alpha = 0.9f), fontSize = 26.sp, fontWeight = FontWeight.Medium, style = Tight)
            Spacer(Modifier.height(10.dp))
        }
        state.djLine?.let {
            Spacer(Modifier.height(28.dp))
            Glass(corner = 22.dp) {
                Text("🎙  $it", color = Color.White, fontSize = 21.sp, lineHeight = 28.sp)
            }
        }
    }
}

/** 글자 위아래 기본 여백을 잘라, 지정한 간격이 눈에 보이는 간격이 되게 한다. */
private val Tight = androidx.compose.ui.text.TextStyle(
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both,
    ),
)

/** 음악 도로에 들어서기 전, 도로 이름 아래 한 줄. */
private const val APPROACH_LINE = "이 길의 노래가 기다리고 있어요"

@Composable
private fun IdleText() = Column {
    Text("길이 노래를\n불러올 거예요", color = Color.White, fontSize = 76.sp, fontWeight = FontWeight.Black, lineHeight = 82.sp, letterSpacing = (-2).sp)
    Spacer(Modifier.height(10.dp))
    Text("아래에서 주행을 시작하세요", color = Color.White.copy(alpha = 0.85f), fontSize = 24.sp)
}

/** 아트워크 위에 얹는 반투명 유리 캡슐. */
@Composable
private fun Glass(corner: Dp = 18.dp, content: @Composable () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(corner)).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun Equalizer(color: Color) {
    val loop = rememberInfiniteTransition(label = "eq")
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(20.dp)) {
        listOf(520, 380, 640, 450).forEachIndexed { i, ms ->
            val h by loop.animateFloat(
                0.25f, 1f, infiniteRepeatable(tween(ms, delayMillis = i * 60, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bar$i",
            )
            Box(Modifier.width(4.dp).fillMaxHeight(h).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

// ───────────────────────────── 오른쪽: 도로 · 날씨 · 지도 ─────────────────────────────

@Composable
private fun SectionTitle(title: String, note: String? = null) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(title, color = Ink.Label, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
        Spacer(Modifier.weight(1f))
        note?.let { Text(it, color = Ink.Secondary, fontSize = 19.sp, modifier = Modifier.padding(bottom = 6.dp)) }
    }
}

@Composable
private fun Roads(state: BlowUiState, onGuide: (RoadZone) -> Unit) {
    if (state.distances.isEmpty()) {
        Text("주행을 시작하면 가까운 음악 도로가 보여요", color = Ink.Secondary, fontSize = 21.sp)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 가장 가까운 음악 도로 하나만
        state.distances.take(1).forEach { d ->
            val onAir = d.zone == state.zone
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 썸네일은 해·도형 없이 색만 흐르게 — 앨범 커버처럼 담백하게
                // 그 도로에서 나올 곡의 커버 색 — 곡 이름은 숨기고 분위기만 미리 보여 준다
                FlowingArtwork(
                    state.coverPalettes[d.zone.trackFor(state.mood).id] ?: Palettes.ofZone(d.zone.id),
                    animated = onAir, modifier = Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)),
                )
                Spacer(Modifier.width(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.zone.name, color = Ink.Label, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(16.dp))
                // 남은 거리 — 기다리는 숫자라 오른쪽에 크게
                if (!onAir) {
                    Text(
                        formatMeters(d.meters), color = Ink.Label, fontSize = 40.sp, fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp, maxLines = 1,
                    )
                    Spacer(Modifier.width(16.dp))
                }
                when {
                    onAir -> Capsule("ON AIR", Color.White, Ink.Label, dot = Ink.Blaze)
                }
            }
        }
    }
}

@Composable
private fun Capsule(text: String, color: Color, bg: Color, dot: Color? = null, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dot?.let {
            Box(Modifier.size(9.dp).clip(CircleShape).background(it))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = color, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

/** Pleos 내비에서 길안내가 시작되면 — 음악을 권하는 팝업. */
@Composable
private fun RoutePrompt(destination: String, onAnswer: (Boolean) -> Unit) {
    Dialog(onDismissRequest = { onAnswer(false) }) {
        Column(
            Modifier.width(720.dp).clip(RoundedCornerShape(38.dp)).background(Ink.Page).padding(40.dp),
        ) {
            FlowingArtwork(Palettes.Sunset, sunRise = 0.8f, modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(28.dp)))
            Spacer(Modifier.height(28.dp))
            Text("$destination 까지 가는 길,", color = Ink.Secondary, fontSize = 24.sp)
            Text("Blow로 음악 들으실래요?", color = Ink.Label, fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            Spacer(Modifier.height(8.dp))
            Text("지나는 음악 도로마다 날씨에 맞는 노래를 불러올게요", color = Ink.Secondary, fontSize = 21.sp)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.weight(1f)) { BarButton("괜찮아요", Icons.Filled.Close, filled = false, onClick = { onAnswer(false) }, fill = true) }
                Box(Modifier.weight(1f)) { BarButton("음악 켜기", Icons.Filled.PlayArrow, filled = true, onClick = { onAnswer(true) }, fill = true) }
            }
        }
    }
}

@Composable
private fun BarButton(label: String, icon: ImageVector, filled: Boolean, onClick: () -> Unit, fill: Boolean = false) {
    Row(
        Modifier
            .then(if (fill) Modifier.fillMaxWidth() else Modifier)
            .height(68.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (filled) Ink.Label else Ink.Fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) Color.White else Ink.Label, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = if (filled) Color.White else Ink.Label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

private val HEADER_HEIGHT = 72.dp
private val HEADER_GAP = 20.dp

private fun formatMeters(m: Double) = if (m < 1000) "${m.toInt()}m" else "%.1fkm".format(m / 1000)

/** 날씨를 모르면(브랜드 노을) 또는 맑은 낮일 때만 해를 그린다. */
fun app.blow.weather.WeatherMood?.showsSun(): Boolean = this == null || (isDay && sky == app.blow.weather.Sky.CLEAR)
