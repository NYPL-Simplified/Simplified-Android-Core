package org.nypl.simplified.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.slf4j.LoggerFactory

class NotificationsWrapper(
  private val context: Context
) {
  private val logger = LoggerFactory.getLogger(NotificationsService::class.java)

  /**
   * Posts a notification with the provided data from a [NotificationResourcesType].
   * In addition to the metadata from the parameter, the builder sets the following:
   *  - setOnlyAlertOnce(true)
   *  - setPriority(NotificationCompat.PRIORITY_Default)
   *  - setAutoCancel(true)
   */
  fun postDefaultNotification(notificationResourcesType: NotificationResourcesType) {
    logger.debug("postDefaultNotification")

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    createNotificationChannel(notificationManager, notificationResourcesType.notificationChannelName, notificationResourcesType.notificationChannelDescription)

    var builder = NotificationCompat.Builder(context, NotificationsService.NOTIFICATION_PRIMARY_CHANNEL_ID)
      .setSmallIcon(notificationResourcesType.smallIcon)
      .setContentTitle(notificationResourcesType.titleReadyNotificationTitle)
      .setContentText(notificationResourcesType.titleReadyNotificationContent)
      .setOnlyAlertOnce(true)
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)

    if (notificationResourcesType.intentClass != null) {
      val intent = Intent(context, notificationResourcesType.intentClass)
      val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
      builder.setContentIntent(pendingIntent)
    }

    notificationManager.notify(0, builder.build())
  }

  /**
   * Creates and adds a [NotificationChannel] to the provided [NotificationManager]
   * with passed in [channelName] and [channelDescription].
   */
  private fun createNotificationChannel(
    notificationManager: NotificationManager,
    channelName: String,
    channelDescription: String
  ) {
    logger.debug(
      "NotificationsService::createNotificationChannel " +
        "with channel name $channelName and description $channelDescription"
    )

    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_DEFAULT
      val channel = NotificationChannel(NotificationsService.NOTIFICATION_PRIMARY_CHANNEL_ID, channelName, importance).apply {
        description = channelDescription
      }

      // Register the channel with the system
      notificationManager.createNotificationChannel(channel)
    }
  }
}
