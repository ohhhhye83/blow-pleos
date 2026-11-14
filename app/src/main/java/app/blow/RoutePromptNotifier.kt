package app.blow

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.Notification

/**
 * Pleos 내비에서 길안내가 시작됐는데 Blow 가 뒤에 있을 때 — 내비 화면 위로
 * "Blow로 음악 들으실래요?" 알림 팝업(heads-up)을 띄운다. 누르면 Blow 가 앞으로 나와 음악을 켠다.
 */
class RoutePromptNotifier(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java).apply {
        createNotificationChannel(
            NotificationChannel(CHANNEL, "길안내 시작 시 음악 제안", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    fun show(destination: String) {
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_ACCEPT, true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Blow로 음악 들으실래요?")
            .setContentText("$destination 까지, 지나는 음악 도로마다 날씨에 맞는 노래를 불러올게요")
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "음악 켜기", open).build())
            .setAutoCancel(true)
            .build()
        manager.notify(ID, notification)
    }

    fun dismiss() = manager.cancel(ID)

    companion object {
        const val EXTRA_ACCEPT = "blow.route_accept"
        private const val CHANNEL = "route_prompt"
        private const val ID = 1
    }
}
