package org.nypl.simplified.accounts.api

import org.nypl.simplified.taskrecorder.api.TaskResult

/**
 * A description of an account provider. Descriptions are _resolved_ to produce [AccountProviderType]
 * values.
 */

interface AccountProviderDescriptionType : Comparable<AccountProviderDescriptionType> {

  /**
   * Metadata associated with the provider description.
   */

  val metadata: AccountProviderDescriptionMetadata

  /**
   * Resolve the description into a full account provider. The given `onProgress` function
   * will be called repeatedly during the resolution process to report on the status of the
   * resolution.
   */

  fun resolve(onProgress: AccountProviderResolutionListenerType)
    : TaskResult<AccountProviderResolutionErrorDetails, AccountProviderType>

  override fun compareTo(other: AccountProviderDescriptionType): Int =
    this.metadata.compareTo(other.metadata)

}
