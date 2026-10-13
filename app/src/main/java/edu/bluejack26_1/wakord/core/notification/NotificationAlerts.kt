package edu.bluejack26_1.wakord.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.AppNotification
import edu.bluejack26_1.wakord.domain.model.NotificationType
import edu.bluejack26_1.wakord.domain.repository.NotificationRepository
import edu.bluejack26_1.wakord.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

object NotificationAlerts {
    fun fresh(seenIds: Set<String>?, current: List<AppNotification>): List<AppNotification> =
        if (seenIds == null) emptyList() else current.filter { !it.isRead && it.id !in seenIds }
}

class NotificationWatcher(
    private val context: Context,
    private val notificationRepository: NotificationRepository,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun start() {
        if (job != null) return
        createChannel()
        var seen: Set<String>? = null
        job = scope.launch {
            combine(notificationRepository.notifications, settingsRepository.pushEnabled) { list, enabled -> list to enabled }
                .collect { (list, enabled) ->
                    if (enabled) NotificationAlerts.fresh(seen, list).forEach(::show)
                    seen = list.map { it.id }.toSet()
                }
        }
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun show(notification: AppNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val text = when (notification.type) {
            NotificationType.LIKE -> context.getString(R.string.notification_like, notification.actorName, notification.targetTitle)
            NotificationType.COMMENT -> context.getString(R.string.notification_comment, notification.actorName, notification.message)
            NotificationType.REPLY -> context.getString(R.string.notification_reply, notification.actorName, notification.message)
            NotificationType.FOLLOW -> context.getString(R.string.notification_follow, notification.actorName)
        }
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_TYPE, notification.targetType.name)
            putExtra(EXTRA_TARGET_ID, notification.targetId)
        }
        val pending = launch?.let {
            PendingIntent.getActivity(context, notification.id.hashCode(), it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val built = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wakord)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(context).notify(notification.id.hashCode(), built)
    }

    companion object {
        const val CHANNEL_ID = "activity"
        const val EXTRA_TARGET_TYPE = "targetType"
        const val EXTRA_TARGET_ID = "targetId"
    }
}
