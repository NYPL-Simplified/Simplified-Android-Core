package org.nypl.simplified.books.controller

import io.reactivex.subjects.Subject
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountEventCreationFailed
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountEventCreationSucceeded
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.metrics.api.MetricServiceType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable

class ProfileAccountCreateOrReturnExistingTask(
  private val accountEvents: Subject<AccountEvent>,
  private val accountProviderID: URI,
  private val accountProviders: AccountProviderRegistryType,
  private val profiles: ProfilesDatabaseType,
  private val strings: ProfileAccountCreationStringResourcesType,
  private val metrics: MetricServiceType?
) : Callable<TaskResult<AccountType>> {

  private val logger = LoggerFactory.getLogger(ProfileAccountCreateOrReturnExistingTask::class.java)
  private val taskRecorder = TaskRecorder.create()

  override fun call(): TaskResult<AccountType> {
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
        strings = this.strings,
        metrics = this.metrics
      ).call()
    } catch (e: Throwable) {
      this.logger.error("account creation failed: ", e)

      this.taskRecorder.currentStepFailedAppending(
        this.strings.unexpectedException,
        "unexpectedException",
        e
      )

      this.publishFailureEvent()
      this.taskRecorder.finishFailure()
    } finally {
      this.logger.debug("finished")
    }
  }

  private fun publishSuccessEvent(account: AccountType) =
    this.accountEvents.onNext(
      AccountEventCreationSucceeded(
        this.strings.creatingAccountSucceeded, account.id
      )
    )

  private fun publishFailureEvent() =
    this.accountEvents.onNext(
      AccountEventCreationFailed(
        this.taskRecorder.finishFailure<AccountType>()
      )
    )
}
