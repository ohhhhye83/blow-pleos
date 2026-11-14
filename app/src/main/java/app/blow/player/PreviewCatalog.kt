package app.blow.player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import app.blow.road.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Apple Music 공식 30초 미리듣기 주소. iTunes Lookup API 는 키가 필요 없고,
 * 미리듣기는 대부분 곡의 하이라이트(후렴) 구간이라 "도로 진입 = 하이라이트" 경험과 맞는다.
 * 음원 파일을 앱에 넣지 않으므로 APK 를 공개 저장소에 올려도 저작권 문제가 없다.
 */
class PreviewCatalog {

    data class Preview(val audioUrl: String, val artworkUrl: String?, val storeUrl: String?)

    private val cache = mutableMapOf<Long, Preview>()
    private val lock = Mutex()

    /** 여러 곡을 한 번의 요청으로 받아 둔다(진입 순간 지연을 없애기 위해 앱 시작 시 호출). */
    suspend fun prefetch(tracks: List<Track>) {
        val ids = tracks.mapNotNull { it.appleId }.filterNot { it in cache }
        if (ids.isEmpty()) return
        lock.withLock { runCatching { lookup(ids) }.onFailure { Log.w(TAG, "prefetch failed", it) } }
    }

    suspend fun previewFor(track: Track): Preview? {
        val id = track.appleId ?: return null
        cache[id]?.let { return it }
        lock.withLock { runCatching { lookup(listOf(id)) }.onFailure { Log.w(TAG, "lookup failed $id", it) } }
        return cache[id]
    }

    /** 앨범 아트를 받아 둔다. 실패하면 null — 화면은 흐르는 아트워크로 대신한다. */
    suspend fun artwork(preview: Preview): Bitmap? {
        val url = preview.artworkUrl ?: return null
        artCache[url]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { URL(url).openStream().use(BitmapFactory::decodeStream) }.getOrNull()
        }?.also { artCache[url] = it }
    }

    private val artCache = mutableMapOf<String, Bitmap>()

    private suspend fun lookup(ids: List<Long>) = withContext(Dispatchers.IO) {
        val url = URL("https://itunes.apple.com/lookup?country=KR&entity=song&id=${ids.joinToString(",")}")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
        }
        try {
            val results = JSONObject(conn.inputStream.bufferedReader().use { it.readText() }).getJSONArray("results")
            for (i in 0 until results.length()) {
                val r = results.getJSONObject(i)
                val audio = r.optString("previewUrl")
                if (audio.isEmpty()) continue
                cache[r.getLong("trackId")] = Preview(
                    audioUrl = audio,
                    // 100x100 썸네일 주소를 큰 아트워크로 바꿔 쓴다
                    artworkUrl = r.optString("artworkUrl100").ifEmpty { null }?.replace("100x100bb", "600x600bb"),
                    storeUrl = r.optString("trackViewUrl").ifEmpty { null },
                )
            }
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val TAG = "Blow/Preview"
    }
}
