package org.nypl.simplified.ui.profiles

import androidx.lifecycle.ViewModel
import hu.akarnokd.rxjava2.subjects.UnicastWorkSubject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.joda.time.DateTime
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.profiles.api.ProfileAttributes
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfileID
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.profiles.api.ProfileReadableType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.reader.api.ReaderPreferences

class ProfileModificationDefaultViewModel : ViewModel() {

  private val services =
    Services.serviceDirectory()

  private val profilesController =
    services.requireService(ProfilesControllerType::class.java)

  private val accountProviderRegistry =
    services.requireService(AccountProviderRegistryType::class.java)

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

  fun findProfileById(id: ProfileID): ProfileReadableType? {
    return this.profilesController.profiles()[id]
  }

  fun findProfileByName(name: String): ProfileReadableType? {
    return this.profilesController.profiles()
      .values
      .find { profile -> profile.displayName == name }
  }

  fun renameProfile(id: ProfileID, newName: String) {
    this.profilesController.profileUpdateFor(id) { description ->
      description.copy(displayName = newName)
    }
  }

  fun createProfile(name: String) {
    val preferences =
      ProfilePreferences(
        dateOfBirth = ProfileDateOfBirth(DateTime.now(), true),
        showTestingLibraries = false,
        hasSeenLibrarySelectionScreen = false,
        readerPreferences = ReaderPreferences.builder().build(),
        mostRecentAccount = null
      )

    val attributes =
      ProfileAttributes(
        sortedMapOf(
          Pair(ProfileAttributes.GENDER_ATTRIBUTE_KEY, "")
        )
      )

    this.profilesController.profileCreate(
      accountProvider = this.accountProviderRegistry.defaultProvider,
      description = ProfileDescription(
        displayName = name,
        preferences = preferences,
        attributes = attributes
      )
    )
  }
}
