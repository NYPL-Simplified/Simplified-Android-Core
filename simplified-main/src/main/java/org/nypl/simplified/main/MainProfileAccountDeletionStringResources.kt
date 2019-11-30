package org.nypl.simplified.main

import android.content.res.Resources
import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType

class MainProfileAccountDeletionStringResources(
  private val resources: Resources
) : ProfileAccountDeletionStringResourcesType {

  override val deletionSucceeded: String
    get() = this.resources.getString(R.string.profileAccountDeletionSucceeded)

  override val unexpectedException: String
    get() = this.resources.getString(R.string.unexpectedException)

  override val deletionFailed: String
    get() = this.resources.getString(R.string.profileAccountDeletionFailed)

  override val onlyOneAccountRemaining: String
    get() = this.resources.getString(R.string.profileAccountDeletionOnlyOneAccountRemaining)

  override val deletingAccount: String
    get() = this.resources.getString(R.string.profileAccountDeleting)
}
