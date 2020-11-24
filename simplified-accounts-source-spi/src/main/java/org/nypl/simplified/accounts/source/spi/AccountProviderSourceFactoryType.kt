package org.nypl.simplified.accounts.source.spi

import android.content.Context
import org.librarysimplified.http.api.LSHTTPClientType

/**
 * A factory for creating account sources.
 */

interface AccountProviderSourceFactoryType {

  /**
   * Create a source of accounts.
   */

  fun create(
    context: Context,
    http: LSHTTPClientType
  ): AccountProviderSourceType
}
