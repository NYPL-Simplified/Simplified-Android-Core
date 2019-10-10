package org.nypl.simplified.accounts.source.spi

import android.content.Context
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import java.lang.Exception
import java.net.URI

/**
 * A source of account provider descriptions.
 *
 * An account provider source delivers lists of account providers on request, and is responsible
 * for implementing the logic required to resolve an account provider description into a full
 * account provider.
 */

interface AccountProviderSourceType {

  /**
   * The result of querying the source.
   */

  sealed class SourceResult {

    /**
     * Querying the source succeeded.
     */

    data class SourceSucceeded(
      val results: Map<URI, AccountProviderDescriptionType>)
      : SourceResult()

    /**
     * Querying the source failed. Sources are permitted to result partial results
     * in the case of failure.
     */

    data class SourceFailed(
      val results: Map<URI, AccountProviderDescriptionType>,
      val exception: Exception)
      : SourceResult()
  }

  /**
   * Retrieve everything the source provides.
   */

  fun load(context: Context): SourceResult

}