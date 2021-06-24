package org.nypl.simplified.ui.announcements

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.announcements.Announcement
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

class AnnouncementsViewModel(
  private val profileController: ProfilesControllerType
) : ViewModel() {

  val account: AccountType =
    this.profileController.profileCurrent().mostRecentAccount()

  val announcements: List<Announcement> =
    this.account.provider.announcements
      .filterNot { this.account.preferences.announcementsAcknowledged.contains(it.id) }

  val currentAnnouncement: LiveData<Int?>
    get() = currentAnnouncementMutable

  private var currentAnnouncementMutable: MutableLiveData<Int?> =
    MutableLiveData(if (this.announcements.isNotEmpty()) 0 else null)

  fun acknowledgeCurrentAnnouncement() {
    val oldAnnouncementsAcknowledged = this.account.preferences.announcementsAcknowledged
    val currentAnnouncementIndex = checkNotNull(this.currentAnnouncement.value!!)
    val currentAnnouncement = this.announcements[currentAnnouncementIndex]
    this.account.setPreferences(
      this.account.preferences.copy(
        announcementsAcknowledged = oldAnnouncementsAcknowledged + currentAnnouncement.id
      )
    )
    this.currentAnnouncementMutable.value =
      if (currentAnnouncementIndex + 1 >= this.announcements.size) {
        null
      } else {
        currentAnnouncementIndex + 1
      }
  }
}
