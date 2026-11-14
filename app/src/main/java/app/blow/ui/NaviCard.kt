package app.blow.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.blow.BlowUiState
import app.blow.geo.LatLng
import app.blow.road.RoadCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * 내비 카드: 실제 지도(OpenStreetMap) 위에 내 위치·음악 도로를 그리고,
 * Pleos 내비가 안내 중이면 다음 회전과 남은 시간·거리를 내비와 같은 모양으로 얹는다. 누르면 Pleos 내비 지도를 연다.
 */
@Composable
fun NaviCard(state: BlowUiState, onOpenMap: () -> Unit, modifier: Modifier) {
    Box(modifier.whiteCard(RoundedCornerShape(32.dp)).clickable(onClick = onOpenMap)) {
        state.fix?.position?.let { MapTiles(it, state) }
        state.guide?.let { g ->
            // 다음 회전 — Pleos 내비 TBT 와 같은 검정 박스
            Row(
                Modifier.padding(20.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF111111))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(g.turn.glyph, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(formatDistance(g.turnDistanceMeters), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
                    if (g.turnDescription.isNotBlank()) {
                        Text(g.turnDescription, color = Color.White.copy(alpha = 0.75f), fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        // 아래: 목적지·남은 시간 또는 내비 열기 안내
        Row(
            Modifier.align(Alignment.BottomCenter).padding(20.dp).fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)).background(Color.White).padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val g = state.guide
            Column(Modifier.weight(1f)) {
                Text(
                    g?.destination ?: "길안내 없음", color = Ink.Label, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (g?.remainSeconds != null && g.remainMeters != null) "${g.remainSeconds / 60}분 · ${formatDistance(g.remainMeters.toDouble())}"
                    else "Pleos 내비에서 목적지를 설정해 보세요",
                    color = Ink.Secondary, fontSize = 18.sp, maxLines = 1,
                )
            }
            Text("내비 열기 ›", color = Ink.Blaze, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "© OpenStreetMap", color = Ink.Secondary, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

// 실제 내비처럼 가까이: zoom 16 타일을 2배로 그려 글씨도 크게 (≈ zoom 17)
/**
 * 내비 기본 지도 톤: 색을 거의 빼고(채도 8%) 살짝 밝게 — 회색 바탕에 흰 도로, 글자만 또렷한 차량 내비 느낌.
 */
private val NaviTone = androidx.compose.ui.graphics.ColorFilter.colorMatrix(
    androidx.compose.ui.graphics.ColorMatrix().apply {
        setToSaturation(0.08f)
        // 밝기 +12, 대비 소폭 ↓ (글자는 유지)
        val lift = androidx.compose.ui.graphics.ColorMatrix(
            floatArrayOf(
                0.92f, 0f, 0f, 0f, 22f,
                0f, 0.92f, 0f, 0f, 22f,
                0f, 0f, 0.92f, 0f, 24f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
        timesAssign(lift)
    },
)

private const val ZOOM = 16
private const val TILE = 512

/** 위경도 → zoom 단계의 세계 픽셀 좌표 (Web Mercator). */
private fun world(p: LatLng): Offset {
    val n = TILE * (1 shl ZOOM).toDouble()  // 화면 픽셀 기준 세계 크기
    val x = (p.lng + 180) / 360 * n
    val latRad = Math.toRadians(p.lat)
    val y = (1 - ln(tan(latRad) + 1 / kotlin.math.cos(latRad)) / PI) / 2 * n
    return Offset(x.toFloat(), y.toFloat())
}

@Composable
private fun MapTiles(center: LatLng, state: BlowUiState) {
    val tiles = remember { mutableStateMapOf<Pair<Int, Int>, Bitmap>() }
    val c = world(center)
    // 진행 방향: 직전 위치에서 지금 위치로의 방위(5m 이상 움직였을 때), 없으면 내비가 준 방위
    val track = remember { arrayOf<Offset?>(null) }
    val heading = remember { mutableFloatStateOf(0f) }
    track[0]?.let { prev ->
        val d = c - prev
        if (d.getDistance() > 2f) heading.floatValue = Math.toDegrees(atan2(d.x.toDouble(), -d.y.toDouble())).toFloat()
    } ?: state.fix?.headingDeg?.let { heading.floatValue = it.toFloat() }
    track[0] = c
    // 내 위치 둘레 5×5 타일 — 지도를 돌려도 모서리가 비지 않게 (지도 사용 정책: 앱 식별 User-Agent 필수)
    val cx = floor(c.x / TILE).toInt()
    val cy = floor(c.y / TILE).toInt()
    LaunchedEffect(cx, cy) {
        for (dx in -2..2) for (dy in -2..2) {
            val key = (cx + dx) to (cy + dy)
            if (key in tiles) continue
            fetchTile(key.first, key.second)?.let { tiles[key] = it }
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        // 실제 내비처럼: 내 차는 아래쪽 가운데, 진행 방향이 화면 위
        val me = Offset(size.width / 2, size.height * 0.70f)
        val origin = me - c
        rotate(-heading.floatValue, pivot = me) {
            tiles.forEach { (k, bmp) ->
                drawImage(
                    bmp.asImageBitmap(),
                    dstOffset = IntOffset((origin.x + k.first * TILE).toInt(), (origin.y + k.second * TILE).toInt()),
                    dstSize = IntSize(TILE, TILE),
                    colorFilter = NaviTone,
                )
            }
            fun line(points: List<LatLng>) = Path().apply {
                points.forEachIndexed { i, p -> (world(p) + origin).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) } }
            }
            // 안내 경로 — 흐리게 (데모 주행은 가상 경로를 그린다)
            if (state.demo) drawPath(line(RoadCatalog.demoDrive), Color(0xFF1A73E8).copy(alpha = 0.28f), style = Stroke(22f, cap = StrokeCap.Round))
            // 음악 도로 — 진하게
            state.distances.forEach { d ->
                val path = line(d.zone.path)
                drawPath(path, Ink.Blaze.copy(alpha = if (d.zone == state.zone) 0.4f else 0.25f), style = Stroke(30f, cap = StrokeCap.Round))
                drawPath(path, Ink.Blaze, style = Stroke(12f, cap = StrokeCap.Round))
            }
        }
        // 내 차 — 위를 향한 화살표
        drawCircle(Color(0xFF1A73E8).copy(alpha = 0.18f), 50f, me)
        drawCircle(Color.White, 26f, me)
        drawPath(
            Path().apply {
                moveTo(me.x, me.y - 18f); lineTo(me.x + 13f, me.y + 14f); lineTo(me.x, me.y + 7f); lineTo(me.x - 13f, me.y + 14f); close()
            },
            Color(0xFF1A73E8),
        )
    }
}

private suspend fun fetchTile(x: Int, y: Int): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val conn = (URL("https://tile.openstreetmap.org/$ZOOM/$x/$y.png").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("User-Agent", "Blow/1.0 (Pleos Connect app contest demo)")
        }
        try { conn.inputStream.use(BitmapFactory::decodeStream) } finally { conn.disconnect() }
    }.getOrNull()
}

private fun formatDistance(m: Double) = if (m < 1000) "${m.toInt()}m" else "%.1fkm".format(m / 1000)
