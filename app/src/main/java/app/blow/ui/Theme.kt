package app.blow.ui

import androidx.compose.ui.graphics.Color
import app.blow.weather.WeatherMood

/** 흰 바탕 + 큰 검정 타이포 + 지는 해 오렌지 포인트. (Apple Music 의 밝은 화면 감도) */
object Ink {
    val Page = Color(0xFFFFFFFF)
    /** 메인 화면 바탕 — 흰 카드가 떠 보이도록 아주 옅은 회색 */
    val Canvas = Color(0xFFF4F5F7)
    val Fill = Color(0xFFF4F4F6)       // 카드·타일 바탕
    val FillHi = Color(0xFFEAEAEE)
    val Separator = Color(0xFFE3E3E8)

    val Label = Color(0xFF111111)
    val Secondary = Color(0xFF8A8A8E)
    val Tertiary = Color(0xFFB4B4B8)

    val Blaze = Color(0xFFFF4F1A)      // 메인 포인트
    val Amber = Color(0xFFFFA22E)
}

/** 흐르는 그라데이션 아트워크 팔레트: 바탕 + 번지는 색 3개. */
data class Palette(val base: Color, val a: Color, val b: Color, val c: Color)

object Palettes {
    /** Blow 시그니처 — 지는 해 */
    val Sunset = Palette(Color(0xFFFF3D17), Color(0xFFFFC21A), Color(0xFFE0123A), Color(0xFFFF7A1A))

    fun of(mood: WeatherMood?): Palette = when (mood) {
        WeatherMood.DAY_CLEAR -> Palette(Color(0xFFFF6A1A), Color(0xFFFFE15A), Color(0xFFFF2E4D), Color(0xFFFFB02E))
        WeatherMood.DAY_CLOUDY -> Palette(Color(0xFF9AA3B2), Color(0xFFFFB38A), Color(0xFF6E7A8F), Color(0xFFD9DCE3))
        WeatherMood.DAY_RAIN -> Palette(Color(0xFF3A74C2), Color(0xFF9CC8F0), Color(0xFF24489A), Color(0xFFFF8A4C))
        WeatherMood.DAY_SNOW -> Palette(Color(0xFF9EC3E6), Color(0xFFFFFFFF), Color(0xFFB9A8E8), Color(0xFFFFC4A3))
        WeatherMood.NIGHT_CLEAR -> Palette(Color(0xFFE8361C), Color(0xFFFF9E2C), Color(0xFF8A1FA8), Color(0xFFC8174F))
        WeatherMood.NIGHT_CLOUDY -> Palette(Color(0xFF4A5468), Color(0xFFFF9E6D), Color(0xFF2B3242), Color(0xFF7D879C))
        WeatherMood.NIGHT_RAIN -> Palette(Color(0xFF1C2F66), Color(0xFF5E8FD8), Color(0xFF0E1A40), Color(0xFFFF7A3C))
        WeatherMood.NIGHT_SNOW -> Palette(Color(0xFF3B5C8C), Color(0xFFE6F0FF), Color(0xFF6B5BB8), Color(0xFFFFB38A))
        null -> Sunset
    }

    /**
     * 앨범 커버에서 그라데이션 색을 뽑는다: 커버를 가장 넓게 차지하는 색 3가지(면적 순).
     * 비슷한 색은 한 덩어리로 묶고(채널당 8단계), 서로 너무 닮은 덩어리는 건너뛴다.
     */
    fun fromArtwork(art: android.graphics.Bitmap): Palette {
        val small = android.graphics.Bitmap.createScaledBitmap(art, 48, 48, true)
        val px = IntArray(48 * 48).also { small.getPixels(it, 0, 48, 0, 0, 48, 48) }
        // 덩어리별 픽셀 수와 색 합계
        data class Bin(var n: Int = 0, var r: Long = 0, var g: Long = 0, var b: Long = 0)
        val bins = HashMap<Int, Bin>()
        px.forEach { c ->
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            val bin = bins.getOrPut((r shr 5 shl 6) or (g shr 5 shl 3) or (b shr 5)) { Bin() }
            bin.n++; bin.r += r; bin.g += g; bin.b += b
        }
        val ranked = bins.values.sortedByDescending { it.n }.map { Triple((it.r / it.n).toInt(), (it.g / it.n).toInt(), (it.b / it.n).toInt()) }
        fun far(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>) =
            kotlin.math.abs(a.first - b.first) + kotlin.math.abs(a.second - b.second) + kotlin.math.abs(a.third - b.third) > 90
        val picked = mutableListOf<Triple<Int, Int, Int>>()
        for (c in ranked) {
            if (picked.all { far(it, c) }) picked += c
            if (picked.size == 3) break
        }
        while (picked.size < 3) picked += picked.last()
        val (top, second, third) = picked.map { Color(it.first, it.second, it.third) }
        return Palette(base = top, a = second, b = third, c = top)
    }

    /** 음악 도로별 대표 색 (썸네일용) */
    fun ofZone(zoneId: String): Palette = when (zoneId) {
        "olympic-banpo" -> Palette(Color(0xFF1E6FD9), Color(0xFFFFB347), Color(0xFF34C3E8), Color(0xFFFF5B2E))
        else -> Sunset
    }
}
