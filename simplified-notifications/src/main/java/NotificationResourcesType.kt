package org.nypl.simplified.notifications

interface NotificationResourcesType {
    val titleReadyNotificationContent: String
    val titleReadyNotificationTitle: String

    // Resource Id for icon
    val smallIcon: Int

    // Class we want to go to that lives in `app-shared`
    val intentClass: Class<*>
}