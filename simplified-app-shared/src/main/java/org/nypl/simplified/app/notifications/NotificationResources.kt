package org.nypl.simplified.app.notifications

import android.content.Context
import org.nypl.simplified.app.R
import org.nypl.simplified.app.splash.SplashActivity
import org.nypl.simplified.notifications.NotificationResourcesType

/**
 * Resources used by the notifications service.
 */

class NotificationResources(
  private val context: Context
) : NotificationResourcesType {

  override val notificationChannelName: String
    get() = context.getString(R.string.notification_channel_name)

  override val notificationChannelDescription: String
    get() = context.getString(R.string.notification_channel_description)

  override val intentClass: Class<*>
    get() = SplashActivity::class.java

  override val titleReadyNotificationContent: String
    get() = context.getString(R.string.notification_title_ready_content)

  override val titleReadyNotificationTitle: String
    get() = context.getString(R.string.notification_title_ready_title)

  override val smallIcon: Int
    get() = R.mipmap.ic_launcher
}
