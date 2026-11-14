package app.blow.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Apple Music 의 움직이는 커버처럼 색이 번지며 흐르는 그라데이션.
 * sunRise 가 있으면 그 위로 해가 떠오른다 (0 = 지평선 아래, 1 = 떠오름).
 */
@Composable
fun FlowingArtwork(
    palette: Palette,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    sunRise: Float? = null,
) {
    val base by animateColorAsState(palette.base, tween(1_600), label = "base")
    val a by animateColorAsState(palette.a, tween(1_600), label = "a")
    val b by animateColorAsState(palette.b, tween(1_600), label = "b")
    val c by animateColorAsState(palette.c, tween(1_600), label = "c")
    val rise by animateFloatAsState(sunRise ?: 0f, tween(2_600, easing = FastOutSlowInEasing), label = "rise")

    val phase = if (animated) {
        val loop = rememberInfiniteTransition(label = "flow")
        val p by loop.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(16_000, easing = LinearEasing)), label = "phase")
        p
    } else 0.9f

    Canvas(modifier) {
        drawRect(base)
        blob(a, 0.28f + 0.14f * sin(phase), 0.30f + 0.12f * cos(phase), 0.75f)
        blob(b, 0.80f + 0.10f * cos(phase * 0.8f), 0.18f + 0.14f * sin(phase * 1.3f), 0.65f)
        blob(c, 0.55f + 0.18f * sin(phase * 0.6f + 1.5f), 0.90f + 0.08f * cos(phase), 0.80f)
        if (sunRise != null) sun(rise)
    }
}

private fun DrawScope.blob(color: Color, fx: Float, fy: Float, radiusOfMax: Float) {
    val center = Offset(size.width * fx, size.height * fy)
    val r = max(size.width, size.height) * radiusOfMax
    drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, r), r, center)
}

/** 부드럽게 빛나는 해. 떠오를수록 커지고 밝아진다. */
private fun DrawScope.sun(rise: Float) {
    // 지평선(아래 가장자리)에 반쯤 잠긴 붉은 해 → 떠오르며 희고 밝아진다
    val r = min(size.width, size.height) * (0.18f + 0.04f * rise)
    val center = Offset(size.width * 0.72f, size.height * (1.0f - 0.62f * rise))
    drawCircle(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.18f + 0.40f * rise), Color.White.copy(alpha = 0f)), center, r * 3.2f),
        r * 3.2f, center,
    )
    drawCircle(
        Brush.verticalGradient(
            listOf(lerp(Color(0xFFFFE6A8), Color(0xFFFFFBEA), rise), lerp(Color(0xFFFF8A1F), Color(0xFFFFD36B), rise)),
            startY = center.y - r, endY = center.y + r,
        ),
        r, center, alpha = 0.92f,
    )
}
