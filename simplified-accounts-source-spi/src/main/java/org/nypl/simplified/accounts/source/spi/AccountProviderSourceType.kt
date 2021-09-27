package org.nypl.simplified.accounts.source.spi

import android.content.Context
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountSearchQuery
import org.nypl.simplified.taskrecorder.api.TaskResult
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
      val results: Map<URI, AccountProviderDescription>
    ) : SourceResult()

    /**
     * Querying the source failed. Sources are permitted to result partial results
     * in the case of failure.
     */

    data class SourceFailed(
      val results: Map<URI, AccountProviderDescription>,
      val exception: Exception
    ) : SourceResult()
  }

  /**
   * Retrieve everything the source provides.
   *
   * @param includeTestingLibraries A hint for the provider indicating whether
   * testing libraries should be loaded. May be ignored by some providers.
   */

  fun load(context: Context, includeTestingLibraries: Boolean): SourceResult

  /**
   * Retrieve everything the source provides.
   *
   * @param includeTestingLibraries A hint for the provider indicating whether
   * testing libraries should be loaded. May be ignored by some providers.
   */

  fun query(context: Context, query: AccountSearchQuery): SourceResult

  /**
   * Clear any cached providers.
   */

  fun clear(context: Context)

  /**
   * @return `true` if this source can resolve the given description
   */

  fun canResolve(description: AccountProviderDescription): Boolean

  /**
   * Resolve the description into a full account provider. The given `onProgress` function
   * will be called repeatedly during the resolution process to report on the status of the
   * resolution.
   */

  fun resolve(
    onProgress: AccountProviderResolutionListenerType,
    description: AccountProviderDescription
  ): TaskResult<AccountProviderType>
}
