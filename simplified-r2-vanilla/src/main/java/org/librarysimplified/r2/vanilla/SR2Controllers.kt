package org.librarysimplified.r2.vanilla

import org.librarysimplified.r2.api.SR2ControllerConfiguration
import org.librarysimplified.r2.api.SR2ControllerProviderType
import org.librarysimplified.r2.api.SR2ControllerType
import org.librarysimplified.r2.vanilla.internal.SR2Controller

/**
 * The default provider of R2 controllers.
 */

class SR2Controllers : SR2ControllerProviderType {

  override fun createHere(configuration: SR2ControllerConfiguration): SR2ControllerType {
    return SR2Controller.create(configuration)
  }
}
