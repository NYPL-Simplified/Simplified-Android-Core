package org.nypl.simplified.accounts.source.nyplregistry

import org.nypl.simplified.accounts.api.AccountProviderDescriptionMetadata
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import org.nypl.simplified.accounts.api.AccountProviderResolutionErrorDetails
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderResolutionResult
import org.nypl.simplified.taskrecorder.api.TaskRecorder

/**
 * An account provider description augmented with the logic needed to resolve the description
 * into a full provider.
 */

class AccountProviderSourceNYPLRegistryDescription(
  override val metadata: AccountProviderDescriptionMetadata) : AccountProviderDescriptionType {

  override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
    val taskRecorder =
      TaskRecorder.create<AccountProviderResolutionErrorDetails>()

    taskRecorder.beginNewStep("Resolving...")
    taskRecorder.currentStepFailed("Failed!", null, null)
    onProgress.invoke(this.metadata.id, "Resolving...")
    return AccountProviderResolutionResult(null, taskRecorder.finish())
  }
}