package org.nypl.simplified.books.controller

import io.reactivex.subjects.Subject
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails.AccountProviderResolutionFailed
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountEventCreationFailed
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountEventCreationInProgress
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountEventCreationSucceeded
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.api.AccountUnknownProviderException
import org.nypl.simplified.accounts.api.AccountUnresolvableProviderException
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.http.core.HTTPHasProblemReportType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable

class ProfileAccountCreateTask(
  private val accountEvents: Subject<AccountEvent>,
  private val accountProviderID: URI,
  private val accountProviders: AccountProviderRegistryType,
  private val profiles: ProfilesDatabaseType,
  private val strings: ProfileAccountCreationStringResourcesType
) : Callable<TaskResult<AccountCreateErrorDetails, AccountType>> {

  private val logger = LoggerFactory.getLogger(ProfileAccountCreateTask::class.java)
  private val taskRecorder = TaskRecorder.create<AccountCreateErrorDetails>()

  override fun call(): TaskResult<AccountCreateErrorDetails, AccountType> {
    return try {
      this.logger.debug("creating account for provider {}", this.accountProviderID)
      val accountProvider = this.resolveAccountProvider()
      val account = this.createAccount(accountProvider)
      this.publishSuccessEvent(account)
      this.taskRecorder.finishSuccess(account)
    } catch (e: Throwable) {
      this.logger.error("account creation failed: ", e)

      this.taskRecorder.currentStepFailedAppending(
        this.strings.unexpectedException,
        AccountCreateErrorDetails.UnexpectedException(this.strings.unexpectedException, e),
        e
      )

      this.publishFailureEvent(this.taskRecorder.currentStep()!!)
      this.taskRecorder.finishFailure()
    } finally {
      this.logger.debug("finished")
    }
  }

  private fun createAccount(accountProvider: AccountProviderType): AccountType {
    this.publishProgressEvent(this.taskRecorder.beginNewStep(this.strings.creatingAccount))

    return try {
      val profile = this.profiles.currentProfileUnsafe()
      profile.createAccount(accountProvider)
    } catch (e: Exception) {
      this.publishFailureEvent(
        this.taskRecorder.currentStepFailedAppending(
          this.strings.creatingAccountFailed + "(" + e.javaClass.canonicalName + ")",
          AccountCreateErrorDetails.UnexpectedException(this.strings.unexpectedException, e),
          e
        )
      )
      throw e
    }
  }

  private fun publishSuccessEvent(account: AccountType) =
    this.accountEvents.onNext(
      AccountEventCreationSucceeded(
        this.strings.creatingAccountSucceeded, account.id
      )
    )

  private fun publishFailureEvent(step: TaskStep<AccountCreateErrorDetails>) =
    this.accountEvents.onNext(
      AccountEventCreationFailed(
        step.resolution.message, this.taskRecorder.finishFailure<AccountType>()
      )
    )

  private fun publishProgressEvent(step: TaskStep<AccountCreateErrorDetails>) =
    this.accountEvents.onNext(AccountEventCreationInProgress(step.description))

  private fun resolveAccountProvider(): AccountProviderType {
    this.publishProgressEvent(this.taskRecorder.beginNewStep(this.strings.resolvingAccountProvider))

    try {
      val description =
        this.accountProviders.findAccountProviderDescription(this.accountProviderID)
          ?: throw AccountUnknownProviderException()

      val resolution =
        this.accountProviders.resolve({ _, status ->
          this.publishProgressEvent(this.taskRecorder.beginNewStep(status))
        }, description)

      return when (resolution) {
        is TaskResult.Success -> resolution.result
        is TaskResult.Failure -> {
          val message = StringBuilder()
          for (error in resolution.errors()) {
            message.append(error.message)
            message.append("\n")

            if (error is HTTPHasProblemReportType) {
              val report = error.problemReport
              if (report != null) {
                message.append(report.problemTitle)
                message.append("\n")
                message.append(report.problemStatus)
                message.append(": ")
                message.append(report.problemDetail)
                message.append("\n")
              }
            }
          }

          this.taskRecorder.currentStepFailed(
            message = message.toString(),
            errorValue = AccountProviderResolutionFailed(resolution.errors())
          )
          throw AccountUnresolvableProviderException(message.toString())
        }
      }
    } catch (e: Exception) {
      this.publishFailureEvent(
        this.taskRecorder.currentStepFailedAppending(
          this.strings.resolvingAccountProviderFailed,
          AccountCreateErrorDetails.UnexpectedException(this.strings.unexpectedException, e),
          e
        )
      )
      throw e
    }
  }
}
