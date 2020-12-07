package org.nypl.simplified.ui.announcements

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventUpdated
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.announcements.Announcement
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.api.ProfileSelection
import org.nypl.simplified.profiles.api.ProfileUpdated
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.thread.api.UIThreadServiceType
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A controller for the announcements UI. This just listens for particular events and
 * opens dialog boxes as necessary.
 */

class AnnouncementsController(
  private val context: Context,
  private val uiThread: UIThreadServiceType,
  private val profileController: ProfilesControllerType
) : LifecycleObserver {

  private val logger =
    LoggerFactory.getLogger(AnnouncementsController::class.java)

  private var profile: ProfileReadableType? = null
  private var subscriptions = CompositeDisposable()
  private val handlingUpdate = AtomicBoolean(false)

  @Volatile
  private var lifecycleOwner: LifecycleOwner? = null

  @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
  fun onViewAvailable(owner: LifecycleOwner) {
    this.lifecycleOwner = owner
    this.subscriptions = CompositeDisposable()
    this.subscriptions.add(this.profileController.accountEvents().subscribe(this::onAccountEvent))
    this.subscriptions.add(this.profileController.profileEvents().subscribe(this::onProfileEvent))
    this.profile = this.profileController.profileCurrent()
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
  fun onViewUnavailable(owner: LifecycleOwner) {
    this.lifecycleOwner = owner
    this.subscriptions.dispose()
    this.profile = null
  }

  private fun onAccountEvent(event: AccountEvent) {
    return when (event) {
      is AccountEventUpdated -> {
        checkForAnnouncements()
      }
      else -> {
        // Nothing to do
      }
    }
  }

  /*
   * Check to see if there are any unread announcements on whatever is the most recent account.
   */

  private fun checkForAnnouncements() {
    if (this.handlingUpdate.compareAndSet(false, true)) {
      try {
        val profileNow = this.profile ?: return
        val accountViewing = profileNow.preferences().mostRecentAccount ?: return
        val accountUpdated = profileNow.account(accountViewing)
        this.tryPublishingAnnouncements(accountUpdated)
      } finally {
        this.handlingUpdate.set(false)
      }
    }
  }

  private fun tryPublishingAnnouncements(account: AccountType) {
    val acknowledged =
      account.preferences.announcementsAcknowledged.toSet()
    val notYetAcknowledged =
      account.provider.announcements.filter { !acknowledged.contains(it.id) }

    if (notYetAcknowledged.isNotEmpty()) {
      this.showDialogForAnnouncements(account, notYetAcknowledged)
      account.setPreferences(
        account.preferences.copy(
          announcementsAcknowledged = account.provider.announcements.map(Announcement::id)
        )
      )
    }
  }

  private fun showDialogForAnnouncements(
    account: AccountType,
    announcements: List<Announcement>
  ) {
    this.uiThread.runOnUIThread {
      val owner = this.lifecycleOwner ?: return@runOnUIThread
      if (owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        showDialogForAnnouncementsUI(account, announcements)
      }
    }
  }

  @UiThread
  private fun showDialogForAnnouncementsUI(
    account: AccountType,
    announcements: List<Announcement>
  ) {
    var indexNumber = 0

    val title =
      context.getString(
        R.string.announcementTitle,
        account.provider.displayName,
        indexNumber + 1,
        announcements.size
      )

    val dialog =
      AlertDialog.Builder(this.context)
        .setTitle(title)
        .setMessage(announcements[indexNumber].content)
        .create()

    dialog.setButton(
      DialogInterface.BUTTON_POSITIVE,
      context.getText(R.string.ok)
    ) { _, _ ->
      if (indexNumber + 1 >= announcements.size) {
        dialog.dismiss()
      } else {
        indexNumber += 1

        val newTitle =
          context.getString(
            R.string.announcementTitle,
            account.provider.displayName,
            indexNumber + 1,
            announcements.size
          )
        dialog.setMessage(announcements[indexNumber].content)
        dialog.setTitle(newTitle)
      }
    }

    dialog.show()
  }

  private fun onProfileEvent(event: ProfileEvent) {
    return when (event) {
      is ProfileSelection.ProfileSelectionCompleted -> {
        this.profile = this.profileController.profileCurrent()
      }
      is ProfileUpdated.Succeeded -> {
        this.checkForAnnouncements()
      }
      else -> {
        // Nothing else matters
      }
    }
  }
}
