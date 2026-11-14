package app.blow.player

import android.content.Context
import android.media.AudioAttributes as SysAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.blow.road.Track
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 곡은 갑자기 터지지 않고 "불어오듯" 페이드인한다.
 * 덱 두 개를 번갈아 써서, 곡이 나오는 중에 다음 음악 도로에 들어서면 두 곡을 겹쳐 자연스럽게 넘긴다(오토믹스 방식).
 * 오디오 포커스는 앱이 한 번만 잡는다 — 덱마다 잡으면 서로 포커스를 뺏어 멈춘다.
 * 내비 안내 음성이 나오면 시스템이 음악 볼륨을 자동으로 낮춘다(ducking).
 */
class BlowPlayer(context: Context, private val onEnded: (Track) -> Unit) {

    private val audio = context.getSystemService(AudioManager::class.java)
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            SysAudioAttributes.Builder()
                .setUsage(SysAudioAttributes.USAGE_MEDIA)
                .setContentType(SysAudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS) decks.forEach { it.pause() }
        }
        .build()

    private val decks: Array<ExoPlayer> = Array(2) { newDeck(context) }
    private var active = 0
    private val deck: ExoPlayer get() = decks[active]

    var nowPlaying: Track? = null
        private set

    private fun newDeck(context: Context): ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            /* handleAudioFocus = */ false,
        )
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && this@apply === deck) nowPlaying?.let(onEnded)
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "playback error", error)
            }
        })
    }

    /** uri: 미리듣기 https 주소 또는 asset:/// 데모 음원. 재생 중이면 두 곡을 겹쳐 넘긴다. */
    suspend fun blowIn(track: Track, uri: String, fadeMillis: Long = 4_000L) {
        if (nowPlaying?.id == track.id && deck.isPlaying) return
        audio.requestAudioFocus(focus)
        val old = deck
        val wasPlaying = old.isPlaying
        active = 1 - active
        val new = deck
        nowPlaying = track
        new.setMediaItem(MediaItem.fromUri(uri))
        new.volume = 0f
        new.prepare()
        new.play()
        coroutineScope {
            if (wasPlaying) launch { fade(old, old.volume, 0f, CROSSFADE_MILLIS); old.stop() }
            launch { fade(new, 0f, 1f, if (wasPlaying) CROSSFADE_MILLIS else fadeMillis) }
        }
    }

    /** 같은 곡을 처음부터 다시 — 음악 도로에 머무는 동안 미리듣기가 끝났을 때. */
    fun replay() {
        deck.seekTo(0)
        deck.play()
    }

    suspend fun blowOut(fadeMillis: Long = 3_000L) {
        val d = deck
        if (d.isPlaying) fade(d, d.volume, 0f, fadeMillis)
        decks.forEach { it.stop() }
        nowPlaying = null
        audio.abandonAudioFocusRequest(focus)
    }

    /** 곡이 끝나 자연히 멈췄을 때. */
    fun finished() {
        nowPlaying = null
        audio.abandonAudioFocusRequest(focus)
    }

    fun release() = decks.forEach { it.release() }

    private suspend fun fade(p: ExoPlayer, from: Float, to: Float, millis: Long) {
        val steps = 20
        for (i in 1..steps) {
            p.volume = from + (to - from) * i / steps
            delay(millis / steps)
        }
    }

    private companion object {
        const val TAG = "Blow/Player"
        const val CROSSFADE_MILLIS = 3_000L
    }
}
