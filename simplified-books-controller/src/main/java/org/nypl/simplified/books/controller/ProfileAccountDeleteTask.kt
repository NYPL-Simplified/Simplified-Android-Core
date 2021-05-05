package org.nypl.simplified.books.controller

import io.reactivex.subjects.Subject
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventDeletion
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.database.api.AccountsDatabaseLastAccountException
import org.nypl.simplified.metrics.api.MetricEvent
import org.nypl.simplified.metrics.api.MetricServiceType
import org.nypl.simplified.profiles.api.ProfileEvent
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.controller.api.ProfileAccountDeletionStringResourcesType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable

class ProfileAccountDeleteTask(
  private val accountEvents: Subject<AccountEvent>,
  private val accountProviderID: URI,
  private val profiles: ProfilesDatabaseType,
  private val profileEvents: Subject<ProfileEvent>,
  private val strings: ProfileAccountDeletionStringResourcesType,
  private val metrics: MetricServiceType?
) : Callable<TaskResult<Unit>> {

  private val logger = LoggerFactory.getLogger(ProfileAccountDeleteTask::class.java)
  private val taskRecorder = TaskRecorder.create()

  private fun publishFailureEvent() =
    this.accountEvents.onNext(
      AccountEventDeletion.AccountEventDeletionFailed(
        this.taskRecorder.finishFailure<Unit>()
      )
    )

  private fun publishProgressEvent(step: TaskStep) =
    this.accountEvents.onNext(AccountEventDeletion.AccountEventDeletionInProgress(step.description))

  private fun publishSuccessEvent(accountThen: AccountID) =
    this.accountEvents.onNext(
      AccountEventDeletion.AccountEventDeletionSucceeded(
        this.strings.deletionSucceeded, accountThen
      )
    )

  override fun call(): TaskResult<Unit> {
    return try {
      this.logger.debug("deleting account for provider {}", this.accountProviderID)
      this.publishProgressEvent(this.taskRecorder.beginNewStep(this.strings.deletingAccount))

      val profile = this.profiles.currentProfileUnsafe()
      val account = profile.deleteAccountByProvider(this.accountProviderID)

      this.metrics?.logMetric(MetricEvent.LibraryRemoved(this.accountProviderID.toString()))
      this.publishSuccessEvent(account)
      this.taskRecorder.finishSuccess(Unit)
    } catch (e: AccountsDatabaseLastAccountException) {
      this.publishFailureEvent()
      this.taskRecorder.finishFailure()
    } catch (e: Throwable) {
      this.publishFailureEvent()
      this.taskRecorder.finishFailure()
    }
  }
}
