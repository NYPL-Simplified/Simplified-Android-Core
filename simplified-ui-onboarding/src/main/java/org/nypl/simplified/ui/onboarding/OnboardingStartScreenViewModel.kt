package org.nypl.simplified.ui.onboarding

import androidx.lifecycle.ViewModel
import org.librarysimplified.services.api.Services
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.branding.BrandingSplashServiceType
import java.util.ServiceLoader

class OnboardingStartScreenViewModel : ViewModel() {

  /**
   * Splash image title resource.
   */

  val imageTitleResource: Int =
    ServiceLoader
      .load(BrandingSplashServiceType::class.java)
      .firstOrNull()
      ?.splashImageTitleResource()
      ?: throw IllegalStateException(
        "No available services of type ${BrandingSplashServiceType::class.java.canonicalName}"
      )

  /*
  * Store the fact that we've seen the selection screen.
  */

  fun setHasSeenOnboarding() {
    Services
      .serviceDirectory()
      .requireService(ProfilesControllerType::class.java)
      .profileUpdate { profileDescription ->
        profileDescription.copy(
          preferences = profileDescription.preferences.copy(hasSeenLibrarySelectionScreen = true)
        )
      }
  }
}
