package app.blow.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.blow.weather.Sky
import app.blow.weather.WeatherMood
import app.blow.weather.WeatherNow

/** 검색 화면의 장르 박스처럼: 채도 높은 단색 + 같은 색조의 진한 일러스트가 모서리 밖으로 흘러나감. */
private data class Tile(val bg: Color, val ink: Color)

private fun tileOf(mood: WeatherMood): Tile = when (mood) {
    WeatherMood.DAY_CLEAR -> Tile(Color(0xFFFFB31A), Color(0xFFD9770A))
    WeatherMood.DAY_CLOUDY -> Tile(Color(0xFF8E9AB2), Color(0xFF5E6A84))
    WeatherMood.DAY_RAIN -> Tile(Color(0xFF2E7CF6), Color(0xFF1548A6))
    WeatherMood.DAY_SNOW -> Tile(Color(0xFF3CC4E6), Color(0xFFFFFFFF))
    WeatherMood.NIGHT_CLEAR -> Tile(Color(0xFF6C55D9), Color(0xFF3B2A93))
    WeatherMood.NIGHT_CLOUDY -> Tile(Color(0xFF4A5468), Color(0xFF2B3242))
    WeatherMood.NIGHT_RAIN -> Tile(Color(0xFF1D3F8F), Color(0xFF0B1F57))
    WeatherMood.NIGHT_SNOW -> Tile(Color(0xFF2E6F9E), Color(0xFFE6F2FF))
}

@Composable
fun WeatherSheet(
    current: WeatherNow?,
    selected: WeatherMood?,
    onSelect: (WeatherMood?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(38.dp)
        Column(
            Modifier
                .fillMaxWidth(0.78f)
                .shadow(32.dp, shape)
                .clip(shape)
                .background(Ink.Page)
                .padding(40.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("날씨", color = Ink.Label, fontSize = 52.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                    Text("같은 도로라도 날씨에 따라 다른 노래가 불어와요", color = Ink.Secondary, fontSize = 21.sp)
                }
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(Ink.Fill).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Ink.Label, modifier = Modifier.size(30.dp)) }
            }
            Spacer(Modifier.height(28.dp))

            MoodTile(null, current, isSelected = selected == null, Modifier.fillMaxWidth().height(120.dp)) { onSelect(null); onDismiss() }
            Spacer(Modifier.height(16.dp))
            // 1차 낮/밤 × 2차 맑음·흐림·비·눈
            listOf(true to "낮", false to "밤").forEach { (isDay, title) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(title, color = Ink.Label, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                        WeatherMood.entries.filter { it.isDay == isDay }.forEach { m ->
                            MoodTile(m, current, isSelected = selected == m, Modifier.weight(1f).height(160.dp)) { onSelect(m); onDismiss() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodTile(mood: WeatherMood?, current: WeatherNow?, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(28.dp)).clickable(onClick = onClick)) {
        if (mood == null) {
            // 실시간: 지금 하늘을 그대로 — 흐르는 아트워크
            FlowingArtwork(Palettes.of(current?.mood), sunRise = if (current?.mood.showsSun()) 0.8f else null, modifier = Modifier.fillMaxSize())
        } else {
            val t = tileOf(mood)
            Box(Modifier.fillMaxSize().background(t.bg))
            Canvas(Modifier.fillMaxSize()) { illustrate(mood, t) }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
            if (mood == null) {
                Text(
                    current?.let { "${it.mood.emoji} ${it.temperatureC?.let { c -> "%.0f°".format(c) } ?: ""}" } ?: "📍",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                )
            }
            Text(mood?.label ?: "실시간 날씨", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        if (isSelected) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(14.dp).size(40.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Check, contentDescription = "선택됨", tint = Ink.Label, modifier = Modifier.size(26.dp)) }
        }
    }
}

/** 타일 오른쪽 위로 크게 걸쳐 그리는 듀오톤 날씨 일러스트. */
private fun DrawScope.illustrate(mood: WeatherMood, t: Tile) {
    val w = size.width
    val h = size.height
    val u = minOf(w, h)
    fun cloud(cx: Float, cy: Float, s: Float, color: Color) {
        drawCircle(color, s * 0.42f, Offset(cx - s * 0.38f, cy + s * 0.08f))
        drawCircle(color, s * 0.56f, Offset(cx + s * 0.05f, cy - s * 0.18f))
        drawCircle(color, s * 0.40f, Offset(cx + s * 0.52f, cy + s * 0.10f))
        drawRoundRect(color, Offset(cx - s * 0.80f, cy + s * 0.05f), Size(s * 1.72f, s * 0.46f), CornerRadius(s * 0.23f))
    }
    if (!mood.isDay && mood.sky != Sky.CLEAR) {
        // 밤: 작은 별 몇 개
        listOf(0.10f to 0.14f, 0.24f to 0.30f, 0.36f to 0.08f).forEach { (x, y) ->
            drawCircle(Color.White.copy(alpha = 0.7f), u * 0.02f, Offset(w * x, h * y))
        }
    }
    when (mood.sky) {
        Sky.CLEAR -> if (mood.isDay) {
            val c = Offset(w * 0.78f, h * 0.30f)
            drawCircle(t.ink.copy(alpha = 0.35f), u * 0.62f, c)
            drawCircle(t.ink, u * 0.42f, c)
        } else {
            val c = Offset(w * 0.74f, h * 0.34f)
            drawCircle(t.ink, u * 0.46f, c)
            drawCircle(t.bg, u * 0.40f, Offset(c.x + u * 0.20f, c.y - u * 0.14f)) // 초승달
            listOf(0.18f to 0.22f, 0.32f to 0.10f, 0.12f to 0.48f).forEach { (x, y) ->
                drawCircle(t.ink, u * 0.025f, Offset(w * x + w * 0.2f, h * y))
            }
        }
        Sky.CLOUDY -> {
            cloud(w * 0.86f, h * 0.22f, u * 0.55f, t.ink.copy(alpha = 0.55f))
            cloud(w * 0.66f, h * 0.40f, u * 0.62f, t.ink)
        }
        Sky.RAIN -> {
            cloud(w * 0.70f, h * 0.22f, u * 0.62f, t.ink)
            for (i in 0 until 5) {
                val x = w * (0.42f + i * 0.11f)
                drawLine(t.ink, Offset(x, h * 0.50f), Offset(x - u * 0.07f, h * 0.68f), strokeWidth = u * 0.035f, cap = StrokeCap.Round)
            }
        }
        Sky.SNOW -> {
            fun flake(c: Offset, r: Float, a: Float) {
                for (k in 0 until 3) {
                    val ang = Math.toRadians(k * 60.0)
                    val dx = (r * kotlin.math.cos(ang)).toFloat()
                    val dy = (r * kotlin.math.sin(ang)).toFloat()
                    drawLine(t.ink.copy(alpha = a), Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y + dy), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
                }
            }
            flake(Offset(w * 0.74f, h * 0.32f), u * 0.36f, 0.95f)
            flake(Offset(w * 0.40f, h * 0.16f), u * 0.14f, 0.7f)
            flake(Offset(w * 0.95f, h * 0.70f), u * 0.18f, 0.7f)
        }
    }
}
