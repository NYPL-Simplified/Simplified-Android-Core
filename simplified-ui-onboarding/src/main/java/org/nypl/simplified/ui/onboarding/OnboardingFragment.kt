package org.nypl.simplified.ui.onboarding

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType

class OnboardingFragment :
  Fragment(R.layout.onboarding_fragment),
  FragmentManager.OnBackStackChangedListener {

  companion object {

    private const val resultKeyKey = "org.nypl.simplified.onboarding.result.key"

    fun newInstance(resultKey: String) = OnboardingFragment().apply {
      arguments = bundleOf(resultKeyKey to resultKey)
    }
  }

  private lateinit var resultKey: String
  private lateinit var navControllerDirectory: NavigationControllerDirectoryType
  private lateinit var onboardingNavController: OnboardingNavigationController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    resultKey =
      requireNotNull(requireArguments().getString(resultKeyKey))
    navControllerDirectory =
      NavigationControllers.findDirectory(requireActivity())
    onboardingNavController =
      OnboardingNavigationController(childFragmentManager)

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
      if (onboardingNavController.popBackStack()) {
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
    return when (item.itemId) {
      android.R.id.home -> onboardingNavController.popToRoot()
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onStart() {
    super.onStart()

    navControllerDirectory
      .updateNavigationController(
        AccountNavigationControllerType::class.java,
        AccountNavigationController(childFragmentManager) {
          requireActivity().supportFragmentManager.setFragmentResult(resultKey, Bundle())
        }
      )

    navControllerDirectory
      .updateNavigationController(
        OnboardingNavigationController::class.java,
        onboardingNavController
      )
  }

  override fun onStop() {
    super.onStop()

    navControllerDirectory
      .removeNavigationController(AccountNavigationControllerType::class.java)

    navControllerDirectory
      .removeNavigationController(OnboardingNavigationController::class.java)
  }
}
