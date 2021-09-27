package org.nypl.simplified.ui.profiles

import androidx.lifecycle.ViewModel
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.librarysimplified.services.api.Services
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

class ProfileSelectionViewModel : ViewModel() {

  private val services =
    Services.serviceDirectory()

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val subscriptions =
    CompositeDisposable(
      this.profilesController.profileEvents()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(this::onProfileEvent)
    )

  private fun onProfileEvent(event: ProfileEvent) {
    this.profileEvents.onNext(event)
  }

  override fun onCleared() {
    super.onCleared()
    subscriptions.clear()
  }

  val profileEvents: UnicastWorkSubject<ProfileEvent> =
    UnicastWorkSubject.create()

  val profiles: List<ProfileReadableType>
    get() = this.profilesController.profiles().values.toList()

  fun selectProfile(id: ProfileID) {
    this.profilesController.profileSelect(id)
  }

  fun deleteProfile(id: ProfileID) {
    this.profilesController.profileDelete(id)
  }
}
