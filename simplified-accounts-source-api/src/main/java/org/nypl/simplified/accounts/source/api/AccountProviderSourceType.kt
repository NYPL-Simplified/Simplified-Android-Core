package org.nypl.simplified.accounts.source.api

import android.content.Context
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import java.lang.Exception
import java.net.URI

/**
 * A source of account provider descriptions.
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
     * Querying the source failed.
     */

    data class SourceFailed(
      val exception: Exception)
      : SourceResult()
  }

  /**
   * Retrieve everything the source provides.
   */

  fun load(context: Context): SourceResult

}
