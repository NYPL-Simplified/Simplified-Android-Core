package org.nypl.simplified.accounts.source.filebased

import android.content.Context
import org.librarysimplified.http.api.LSHTTPClientType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceFactoryType
import org.nypl.simplified.accounts.source.spi.AccountProviderSourceType
import org.nypl.simplified.buildconfig.api.BuildConfigurationAccountsType

/**
 * A factory for file-based sources.
 */

class AccountProviderSourceFileBasedFactory : AccountProviderSourceFactoryType {
  override fun create(
    context: Context,
    http: LSHTTPClientType,
    buildConfig: BuildConfigurationAccountsType
  ): AccountProviderSourceType {
    return AccountProviderSourceFileBased { c -> c.assets.open("Accounts.json") }
  }
}
