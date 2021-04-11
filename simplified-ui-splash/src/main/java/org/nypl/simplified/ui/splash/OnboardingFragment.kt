package org.nypl.simplified.ui.splash

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.nypl.simplified.navigation.api.NavigationControllerDirectoryType
import org.nypl.simplified.navigation.api.NavigationControllers
import org.nypl.simplified.ui.accounts.AccountListRegistryFragment
import org.nypl.simplified.ui.accounts.AccountNavigationControllerType

class OnboardingFragment: Fragment(R.layout.onboarding_fragment) {

  companion object {

    private const val resultKeyKey = "org.nypl.simplified.onboarding.result.key"

    fun newInstance(resultKey: String): OnboardingFragment {
      return OnboardingFragment().apply {
        arguments = bundleOf(resultKeyKey to resultKey)
      }
    }
  }

  private lateinit var resultKey: String
  private lateinit var navControllerDirectory: NavigationControllerDirectoryType
  private lateinit var onboardingNavController: OnboardingNavigationController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true) // Required to get a call to onOptionsItemSelected

    resultKey =
      requireNotNull(requireArguments().getString(resultKeyKey))
    navControllerDirectory =
      NavigationControllers.findDirectory(requireActivity())
    onboardingNavController =
      OnboardingNavigationController(childFragmentManager)

    childFragmentManager.addOnBackStackChangedListener {
      val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
      when (childFragmentManager.fragments.last()) {
        is OnboardingStartScreenFragment -> actionBar?.hide()
        is AccountListRegistryFragment -> actionBar?.show()
      }
    }

    childFragmentManager.setFragmentResultListener("", this) { _, _->
      requireActivity().supportFragmentManager.setFragmentResult(resultKey, Bundle())
    }

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
