package org.nypl.simplified.notifications

interface NotificationResourcesType {
    val notification_title_ready_content: Int
    val notification_title_ready_title: Int

    // Resource Id for icon
    val smallIcon: Int

    // Class we want to go to that lives in `app-shared`
    val intentClass: Class<*>
}