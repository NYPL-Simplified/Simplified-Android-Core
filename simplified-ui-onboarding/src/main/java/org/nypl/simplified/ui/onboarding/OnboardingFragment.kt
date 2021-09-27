package org.nypl.simplified.ui.onboarding

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import org.librarysimplified.services.api.Services
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.android.ktx.tryPopBackStack
import org.nypl.simplified.android.ktx.tryPopToRoot
import org.nypl.simplified.listeners.api.FragmentListenerType
import org.nypl.simplified.listeners.api.ListenerRepository
import org.nypl.simplified.listeners.api.fragmentListeners
import org.nypl.simplified.listeners.api.listenerRepositories
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.nypl.simplified.ui.accounts.AccountListRegistryEvent
import org.nypl.simplified.ui.errorpage.ErrorPageFragment
import org.nypl.simplified.ui.errorpage.ErrorPageParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OnboardingFragment :
  Fragment(R.layout.onboarding_fragment),
  FragmentManager.OnBackStackChangedListener {

  private val logger: Logger = LoggerFactory.getLogger(OnboardingFragment::class.java)

  private val listenerRepo: ListenerRepository<OnboardingListenedEvent, Unit> by listenerRepositories()
  private val listener: FragmentListenerType<OnboardingEvent> by fragmentListeners()

  private val defaultViewModelFactory: ViewModelProvider.Factory by lazy {
    OnboardingDefaultViewModelFactory(super.getDefaultViewModelProviderFactory())
  }

  private val profilesController: ProfilesControllerType =
    Services.serviceDirectory()
      .requireService(ProfilesControllerType::class.java)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    /*
    * Demand that onOptionsItemSelected be called.
    */

    setHasOptionsMenu(true)

    childFragmentManager.addOnBackStackChangedListener(this)

    /*
     * Handle back pressed event by popping from the back stack if possible.
     */

    requireActivity().onBackPressedDispatcher.addCallback(this) {
      if (childFragmentManager.tryPopBackStack()) {
        return@addCallback
      }

      try {
        isEnabled = false
        requireActivity().onBackPressed()
      } finally {
        isEnabled = true
      }
    }
  }

  override fun onBackStackChanged() {
    val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
    when (childFragmentManager.fragments.last()) {
      is OnboardingStartScreenFragment -> actionBar?.hide()
      is AccountListRegistryFragment -> actionBar?.show()
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home ->
        if (childFragmentManager.tryPopToRoot()) {
          return true
        }
    }

    return super.onOptionsItemSelected(item)
  }

  override fun onStart() {
    super.onStart()
    this.listenerRepo.registerHandler(this::handleEvent)
  }

  override fun onStop() {
    super.onStop()
    this.listenerRepo.unregisterHandler()
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return this.defaultViewModelFactory
  }

  private fun handleEvent(event: OnboardingListenedEvent, state: Unit) {
    return when (event) {
      is OnboardingListenedEvent.AccountListRegistryEvent ->
        this.handleAccountListRegistryEvent(event.event)
      is OnboardingListenedEvent.OnboardingStartScreenEvent ->
        this.handleOnboardingStartScreenEvent(event.event)
    }
  }

  private fun handleAccountListRegistryEvent(event: AccountListRegistryEvent) {
    return when (event) {
      is AccountListRegistryEvent.AccountCreated ->
        this.onAccountCreated(event.accountID)
      is AccountListRegistryEvent.OpenErrorPage ->
        this.openErrorPage(event.parameters)
    }
  }

  private fun onAccountCreated(accountID: AccountID) {
    this.profilesController.profileUpdate { description ->
      description.copy(
        preferences = description.preferences.copy(
          mostRecentAccount = accountID
        )
      )
    }.get()
    this.onOnboardingCompleted()
  }

  private fun handleOnboardingStartScreenEvent(event: OnboardingStartScreenEvent) {
    return when (event) {
      OnboardingStartScreenEvent.AddLibraryLater ->
        this.onOnboardingCompleted()
      OnboardingStartScreenEvent.FindLibrary ->
        this.openSettingsAccountRegistry()
    }
  }

  private fun onOnboardingCompleted() {
    this.listener.post(OnboardingEvent.OnboardingCompleted)
  }

  private fun openErrorPage(parameters: ErrorPageParameters) {
    this.logger.debug("openErrorPage")
    val fragment = ErrorPageFragment.create(parameters)
    this.childFragmentManager.commit {
      replace(R.id.onboarding_fragment_container, fragment)
      addToBackStack(null)
    }
  }

  private fun openSettingsAccountRegistry() {
    this.logger.debug("openSettingsAccountRegistry")
    val fragment = AccountListRegistryFragment()
    this.childFragmentManager.commit {
      replace(R.id.onboarding_fragment_container, fragment)
      addToBackStack(null)
    }
  }
}
