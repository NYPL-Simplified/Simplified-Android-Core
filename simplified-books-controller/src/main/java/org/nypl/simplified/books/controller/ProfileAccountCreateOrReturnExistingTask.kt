package org.nypl.simplified.books.controller

import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountEventCreationFailed
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountEventCreationSucceeded
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable

class ProfileAccountCreateOrReturnExistingTask(
  private val accountEvents: ObservableType<AccountEvent>,
  private val accountProviderID: URI,
  private val accountProviders: AccountProviderRegistryType,
  private val profiles: ProfilesDatabaseType,
  private val strings: ProfileAccountCreationStringResourcesType
) : Callable<TaskResult<AccountCreateErrorDetails, AccountType>> {

  private val logger = LoggerFactory.getLogger(ProfileAccountCreateOrReturnExistingTask::class.java)
  private val taskRecorder = TaskRecorder.create<AccountCreateErrorDetails>()

  override fun call(): TaskResult<AccountCreateErrorDetails, AccountType> {
    return try {
      this.taskRecorder.beginNewStep(this.strings.creatingAccount)

      val profile = this.profiles.currentProfileUnsafe()
      val existingAccount = profile.accountsByProvider().get(this.accountProviderID)
      if (existingAccount != null) {
        this.taskRecorder.currentStepSucceeded(this.strings.creatingAccountSucceeded)
        this.publishSuccessEvent(existingAccount)
        return this.taskRecorder.finishSuccess(existingAccount)
      }

      ProfileAccountCreateTask(
        accountEvents = this.accountEvents,
        accountProviderID = this.accountProviderID,
        accountProviders = this.accountProviders,
        profiles = this.profiles,
        strings = this.strings
      ).call()
    } catch (e: Throwable) {
      this.logger.error("account creation failed: ", e)

      this.taskRecorder.currentStepFailedAppending(
        this.strings.unexpectedException,
        AccountCreateErrorDetails.UnexpectedException(this.strings.unexpectedException, e),
        e)

      this.publishFailureEvent(this.taskRecorder.currentStep()!!)
      this.taskRecorder.finishFailure()
    } finally {
      this.logger.debug("finished")
    }
  }

  private fun publishSuccessEvent(account: AccountType) =
    this.accountEvents.send(AccountEventCreationSucceeded(this.strings.creatingAccountSucceeded, account.id))

  private fun publishFailureEvent(step: TaskStep<AccountCreateErrorDetails>) =
    this.accountEvents.send(AccountEventCreationFailed(
      step.resolution.message, this.taskRecorder.finishFailure<AccountType>()))
}
