package org.librarysimplified.r2.views.internal

import org.librarysimplified.r2.api.SR2ControllerType

internal data class SR2ControllerReference(
  val controller: SR2ControllerType,
  val isFirstStartup: Boolean
)
