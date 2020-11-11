package org.nypl.simplified.main

import android.content.res.Resources
import org.nypl.simplified.boot.api.BootStringResourcesType

internal class MainServicesStrings(
  private val resources: Resources
) : BootStringResourcesType {

  override val bootFailedGeneric: String =
    this.resources.getString(R.string.bootFailedGeneric)

  override val bootCompleted: String =
    "Startup completed!"

  fun bootingGeneral(kind: String): String =
    "Initializing $kind..."
}
