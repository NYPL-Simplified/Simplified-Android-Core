package org.librarysimplified.r2.views

import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2ControllerType

interface SR2ControllerHostType : SR2NavigationControllerType {

  fun onControllerRequired(): SR2ControllerProviderType

  fun onControllerBecameAvailable(
    controller: SR2ControllerType,
    isFirstStartup: Boolean
  )
}
