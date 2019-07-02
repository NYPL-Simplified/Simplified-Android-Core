package org.nypl.simplified.tests

import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType

class MockAccountDeletionStringResources : ProfileAccountDeletionStringResourcesType {

  override val deletionFailed: String
    get() = "deletionFailed"

  override val onlyOneAccountRemaining: String
    get() = "onlyOneAccountRemaining"

  override val deletingAccount: String
    get() = "deletingAccount"

}