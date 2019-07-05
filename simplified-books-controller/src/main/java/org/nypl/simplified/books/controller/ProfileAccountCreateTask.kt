package org.nypl.simplified.books.controller

import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation.*
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.observable.ObservableType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.controller.api.AccountCreateErrorDetails
import org.nypl.simplified.profiles.controller.api.AccountCreateErrorDetails.*
import org.nypl.simplified.profiles.controller.api.AccountCreateTaskResult
import org.nypl.simplified.profiles.controller.api.AccountUnknownProviderException
import org.nypl.simplified.profiles.controller.api.AccountUnresolvableProviderException
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.concurrent.Callable

class ProfileAccountCreateTask(
  private val accountEvents: ObservableType<AccountEvent>,
  private val accountProviderID: URI,
  private val accountProviders: org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType,
  private val profiles: ProfilesDatabaseType,
  private val strings: ProfileAccountCreationStringResourcesType
) : Callable<AccountCreateTaskResult> {

  private val logger = LoggerFactory.getLogger(ProfileAccountCreateTask::class.java)
  private val taskRecorder = TaskRecorder.create<AccountCreateErrorDetails>()

  override fun call(): AccountCreateTaskResult {
    return try {
      this.logger.debug("creating account for provider {}", this.accountProviderID)
      val accountProvider = this.resolveAccountProvider()
      val account = this.createAccount(accountProvider)
      this.publishSuccessEvent(account)
      AccountCreateTaskResult(this.taskRecorder.finish())
    } catch (e: Throwable) {
      this.logger.error("account creation failed: ", e)

      val step = this.taskRecorder.currentStep()!!
      if (step.exception == null) {
        this.taskRecorder.currentStepFailed(
          message = this.pickUsableMessage(step.resolution, e),
          errorValue = step.errorValue,
          exception = e)
      }

      this.publishFailureEvent(step)
      AccountCreateTaskResult(this.taskRecorder.finish())
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
      this.publishFailureEvent(this.taskRecorder.currentStepFailed(
        this.strings.creatingAccountFailed, null, e))
      throw e
    }
  }

  private fun publishSuccessEvent(account: AccountType) =
    this.accountEvents.send(AccountEventCreationSucceeded(account.id))

  private fun publishFailureEvent(step: TaskStep<AccountCreateErrorDetails>) =
    this.accountEvents.send(AccountEventCreationFailed(step.resolution))

  private fun publishProgressEvent(step: TaskStep<AccountCreateErrorDetails>) =
    this.accountEvents.send(AccountEventCreationInProgress(step.description))

  private fun resolveAccountProvider(): AccountProviderType {
    this.publishProgressEvent(this.taskRecorder.beginNewStep(this.strings.resolvingAccountProvider))

    try {
      val description =
        this.accountProviders.findAccountProviderDescription(this.accountProviderID)
          ?: throw AccountUnknownProviderException()

      val resolution =
        description.resolve { _, status ->
          this.publishProgressEvent(this.taskRecorder.beginNewStep(status))
        }

      if (resolution.failed) {
        val failure = resolution.steps.last()
        this.taskRecorder.currentStepFailed(
          message = failure.resolution,
          errorValue = AccountProviderResolutionFailed(failure.errorValue),
          exception = failure.exception)
        throw AccountUnresolvableProviderException()
      }

      return resolution.result!!
    } catch (e: Exception) {
      this.publishFailureEvent(this.taskRecorder.currentStepFailed(
        this.strings.resolvingAccountProviderFailed, null, e))
      throw e
    }
  }

  private fun pickUsableMessage(message: String, e: Throwable): String {
    val exMessage = e.message
    return if (message.isEmpty()) {
      if (exMessage != null) {
        exMessage
      } else {
        e.javaClass.simpleName
      }
    } else {
      message
    }
  }
}
