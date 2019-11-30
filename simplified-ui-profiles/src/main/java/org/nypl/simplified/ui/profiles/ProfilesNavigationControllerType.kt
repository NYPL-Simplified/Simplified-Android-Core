package org.nypl.simplified.ui.profiles

import org.nypl.simplified.navigation.api.NavigationControllerType
import org.nypl.simplified.profiles.api.ProfileID

interface ProfilesNavigationControllerType : NavigationControllerType {

  fun openMain()

  fun openProfileModify(id: ProfileID)

  fun openProfileCreate()

}
