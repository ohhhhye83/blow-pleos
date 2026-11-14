package app.blow.dj

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import app.blow.road.RoadZone
import app.blow.road.Track
import app.blow.weather.WeatherMood
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * "현대가 DJ" — 도로 진입 순간 한 마디.
 * Pleos TextToSpeech SDK 가 Coming Soon 이라 우선 Android 기본 TTS 를 쓴다.
 */
class DjVoice(context: Context) {

    private var ready = false

    // 엔진 바인딩에 실패하면 onInit 이 생성자 안에서 동기 호출되므로, tts 참조는 다음 루프로 미룬다
    private val main = Handler(Looper.getMainLooper())
    private val tts: TextToSpeech = TextToSpeech(
        context.applicationContext,
        { status -> main.post { onInit(status) } },
        PLEOS_TTS_ENGINE,
    )

    private fun onInit(status: Int) {
        // 언어 지정은 엔진 초기화가 끝난 뒤에만 유효하다
        val lang = if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.KOREAN) else null
        ready = lang != null && lang >= TextToSpeech.LANG_AVAILABLE
        Log.i(TAG, "tts init status=$status engine=${tts.defaultEngine} lang=$lang ready=$ready")
    }

    fun script(zone: RoadZone, weather: WeatherMood?, track: Track): String {
        val mood = when (weather) {
            WeatherMood.DAY_CLEAR -> "햇살 좋은 날이네요."
            WeatherMood.DAY_CLOUDY -> "구름이 낮게 깔린 오후예요."
            WeatherMood.DAY_RAIN -> "창밖에 비가 내리네요."
            WeatherMood.DAY_SNOW -> "하얀 눈이 내리고 있어요."
            WeatherMood.NIGHT_CLEAR -> "별이 보이는 밤이에요."
            WeatherMood.NIGHT_CLOUDY -> "구름 낀 조용한 밤이에요."
            WeatherMood.NIGHT_RAIN -> "비 내리는 밤이에요."
            WeatherMood.NIGHT_SNOW -> "눈 내리는 밤이에요."
            null -> ""
        }
        return "지금부터 ${zone.name}. $mood 이 길엔 이 노래, ${track.title}."
    }

    /** 말하기가 끝날 때까지 대기. TTS 가 준비 안 됐으면 바로 반환. */
    suspend fun say(text: String) {
        if (!ready) return
        val id = UUID.randomUUID().toString()
        suspendCancellableCoroutine { cont ->
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == id && cont.isActive) cont.resume(Unit)
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == id && cont.isActive) cont.resume(Unit)
                }
            })
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
            cont.invokeOnCancellation { tts.stop() }
        }
    }

    fun release() = tts.shutdown()

    private companion object {
        const val TAG = "Blow/DJ"
        /** 에뮬레이터에 기본 TTS 엔진이 지정돼 있지 않아 Pleos 엔진을 명시한다 (없으면 시스템 기본으로 폴백). */
        const val PLEOS_TTS_ENGINE = "ai.pleos.playground.caas"
    }
}
