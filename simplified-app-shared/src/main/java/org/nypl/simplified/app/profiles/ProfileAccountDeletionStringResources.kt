package org.nypl.simplified.app.profiles

import android.content.res.Resources
import org.nypl.simplified.app.R
import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType

class ProfileAccountDeletionStringResources(
  val resources: Resources) : ProfileAccountDeletionStringResourcesType {

  override val deletionFailed: String
    get() = resources.getString(R.string.profileAccountDeletionFailed)

  override val onlyOneAccountRemaining: String
    get() = resources.getString(R.string.profileAccountDeletionOnlyOneAccountRemaining)

  override val deletingAccount: String
    get() = resources.getString(R.string.profileAccountDeleting)

}