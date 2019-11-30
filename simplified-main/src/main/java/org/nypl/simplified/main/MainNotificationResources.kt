package org.nypl.simplified.main

import android.content.Context
import org.nypl.simplified.notifications.NotificationResourcesType

/**
 * Resources used by the notifications service.
 */

class MainNotificationResources(
  private val context: Context
) : NotificationResourcesType {

  override val notificationChannelName: String
    get() = context.getString(R.string.notification_channel_name)

  override val notificationChannelDescription: String
    get() = context.getString(R.string.notification_channel_description)

  override val intentClass: Class<*>
    get() = MainActivity::class.java

  override val titleReadyNotificationContent: String
    get() = context.getString(R.string.notification_title_ready_content)

  override val titleReadyNotificationTitle: String
    get() = context.getString(R.string.notification_title_ready_title)

  override val smallIcon: Int
    get() = R.drawable.main_icon
}
