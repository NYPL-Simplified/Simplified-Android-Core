package org.nypl.simplified.tests.mocking

import org.librarysimplified.services.api.ServiceDirectoryBuilderType
import org.nypl.simplified.accounts.api.AccountLoginStringResourcesType
import org.nypl.simplified.accounts.api.AccountLogoutStringResourcesType
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType
import org.nypl.simplified.books.controller.api.BookRevokeStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType

object MockStrings {

  fun populate(builder: ServiceDirectoryBuilderType) {
    builder.addService(ProfileAccountCreationStringResourcesType::class.java, MockAccountCreationStringResources())
    builder.addService(ProfileAccountDeletionStringResourcesType::class.java, MockAccountDeletionStringResources())
    builder.addService(AccountLoginStringResourcesType::class.java, MockAccountLoginStringResources())
    builder.addService(AccountLogoutStringResourcesType::class.java, MockAccountLogoutStringResources())
    builder.addService(AccountProviderResolutionStringsType::class.java, MockAccountProviderResolutionStrings())
    builder.addService(BookRevokeStringResourcesType::class.java, MockRevokeStringResources())
  }
}
