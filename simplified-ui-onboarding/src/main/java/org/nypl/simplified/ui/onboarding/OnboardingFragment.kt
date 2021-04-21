package org.nypl.simplified.ui.onboarding

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import io.reactivex.disposables.CompositeDisposable
import org.nypl.simplified.android.ktx.tryPopBackStack
import org.nypl.simplified.android.ktx.tryPopToRoot
import org.nypl.simplified.navigation.api.NavigationAwareViewModelFactory
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.slf4j.LoggerFactory

class OnboardingFragment :
  Fragment(R.layout.onboarding_fragment),
  FragmentManager.OnBackStackChangedListener {

  companion object {

    private const val resultKeyKey = "org.nypl.simplified.onboarding.result.key"

    fun newInstance(resultKey: String) = OnboardingFragment().apply {
      arguments = bundleOf(resultKeyKey to resultKey)
    }
  }

  private val logger = LoggerFactory.getLogger(OnboardingFragment::class.java)
  private val subscriptions = CompositeDisposable()
  private lateinit var resultKey: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    resultKey =
      requireNotNull(requireArguments().getString(resultKeyKey))

    /*
    * Demand that onOptionsItemSelected be called.
    */

    setHasOptionsMenu(true)

    childFragmentManager.addOnBackStackChangedListener(this)

    /*
    * Finish the onboarding when a child fragment explicitly terminates.
    */

    childFragmentManager.setFragmentResultListener("", this) { _, _ ->
      requireActivity().supportFragmentManager.setFragmentResult(resultKey, Bundle())
    }

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

    NavigationControllers
      .findViewModel<OnboardingNavigationViewModel>(this)
      .commandQueue
      .subscribe(this::handleNavigationCommand)
      .let { subscriptions.add(it) }
  }

  override fun onStop() {
    super.onStop()
    subscriptions.clear()
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return NavigationAwareViewModelFactory(
      OnboardingNavigationViewModel::class.java,
      super.getDefaultViewModelProviderFactory()
    )
  }

  private fun handleNavigationCommand(command: OnboardingNavigationCommand) {
    when(command) {
      OnboardingNavigationCommand.AccountNavigationCommand.OnAccountCreated ->
        closeOnboarding()
      OnboardingNavigationCommand.AccountNavigationCommand.OnSAMLCommandAccessTokenObtained ->
        closeOnboarding()
      OnboardingNavigationCommand.AccountNavigationCommand.OpenSettingsAccountRegistry ->
        openSettingsAccountRegistry()
    }
  }

  private fun closeOnboarding() {
    requireActivity().supportFragmentManager.setFragmentResult(resultKey, Bundle())
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
