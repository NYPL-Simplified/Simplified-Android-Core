package org.nypl.simplified.ui.splash

import androidx.lifecycle.ViewModel
import org.librarysimplified.services.api.Services
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType

class SplashSelectionFragmentViewModel : ViewModel() {

  val profilesController = Services.serviceDirectory()
    .requireService(ProfilesControllerType::class.java)
}
