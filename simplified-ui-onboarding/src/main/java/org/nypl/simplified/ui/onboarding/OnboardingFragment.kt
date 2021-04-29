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
import com.io7m.junreachable.UnreachableCodeException
import org.nypl.simplified.android.ktx.tryPopBackStack
import org.nypl.simplified.android.ktx.tryPopToRoot
import org.nypl.simplified.navigation.api.NavigationAwareViewModelFactory
import org.nypl.simplified.navigation.api.NavigationViewModel
import org.nypl.simplified.navigation.api.navViewModels
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.nypl.simplified.ui.accounts.AccountsNavigationCommand
import org.slf4j.Logger
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

  private val logger: Logger = LoggerFactory.getLogger(OnboardingFragment::class.java)

  private val navViewModel: NavigationViewModel<OnboardingNavigationCommand> by navViewModels()

  private val defaultViewModelFactory: ViewModelProvider.Factory by lazy {
    NavigationAwareViewModelFactory(
      OnboardingNavigationViewModel::class.java,
      super.getDefaultViewModelProviderFactory()
    )
  }

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
    this.navViewModel.registerHandler(this::handleNavigationCommand)
  }

  override fun onStop() {
    super.onStop()
    this.navViewModel.unregisterHandler()
  }

  override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
    return this.defaultViewModelFactory
  }

  private fun handleNavigationCommand(command: OnboardingNavigationCommand) {
    return when (command) {
      is OnboardingNavigationCommand.AccountsNavigationCommand ->
        this.handleAccountNavigationCommand(command.command)
    }
  }

  private fun handleAccountNavigationCommand(command: AccountsNavigationCommand) {
    return when (command) {
      AccountsNavigationCommand.OnAccountCreated ->
        this.closeOnboarding()
      AccountsNavigationCommand.OnSAMLEventAccessTokenObtained ->
        this.closeOnboarding()
      AccountsNavigationCommand.OpenSettingsAccountRegistry ->
        this.openSettingsAccountRegistry()
      AccountsNavigationCommand.OpenCatalogAfterAuthentication ->
        throw UnreachableCodeException()
      is AccountsNavigationCommand.OpenErrorPage ->
        throw UnreachableCodeException()
      is AccountsNavigationCommand.OpenSAML20Login ->
        throw UnreachableCodeException()
      is AccountsNavigationCommand.OpenSettingsAccount ->
        throw UnreachableCodeException()
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
