package org.nypl.simplified.books.controller

import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import io.reactivex.subjects.Subject
import one.irradia.mime.api.MIMEType
import org.joda.time.DateTime
import org.librarysimplified.http.api.LSHTTPClientType
import org.librarysimplified.http.api.LSHTTPRequestBuilderType.AllowRedirects.ALLOW_REDIRECTS
import org.librarysimplified.http.api.LSHTTPResponseStatus
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountEventCreationFailed
import org.nypl.simplified.accounts.api.AccountEventCreation.AccountEventCreationInProgress
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.accounts.registry.api.AccountProviderRegistryType
import org.nypl.simplified.links.Link
import org.nypl.simplified.opds.core.OPDSAcquisitionFeed
import org.nypl.simplified.opds.core.OPDSFeedConstants
import org.nypl.simplified.opds.core.OPDSFeedParserType
import org.nypl.simplified.profiles.api.ProfilesDatabaseType
import org.nypl.simplified.profiles.controller.api.ProfileAccountCreationStringResourcesType
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.nypl.simplified.taskrecorder.api.TaskStep
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.UUID
import java.util.concurrent.Callable

/**
 * A task that creates an account based on a custom OPDS feed.
 */

class ProfileAccountCreateCustomOPDSTask(
  private val accountEvents: Subject<AccountEvent>,
  private val accountProviderRegistry: AccountProviderRegistryType,
  private val httpClient: LSHTTPClientType,
  private val opdsURI: URI,
  private val opdsFeedParser: OPDSFeedParserType,
  private val profiles: ProfilesDatabaseType,
  private val strings: ProfileAccountCreationStringResourcesType
) : Callable<TaskResult<AccountType>> {

  private var title: String = ""
  private val logger = LoggerFactory.getLogger(ProfileAccountCreateCustomOPDSTask::class.java)
  private val taskRecorder = TaskRecorder.create()

  override fun call(): TaskResult<AccountType> {
    this.publishProgressEvent(this.taskRecorder.beginNewStep(this.strings.creatingAccount))
    this.logger.debug("creating a custom OPDS account")

    return try {
      val accountProviderDescription =
        this.createAccountProviderDescription()

      val resolutionResult =
        this.accountProviderRegistry.resolve(
          { _, message ->
            this.publishProgressEvent(this.taskRecorder.beginNewStep(message))
          },
          accountProviderDescription
        )

      when (resolutionResult) {
        is TaskResult.Success ->
          this.createAccount(accountProviderDescription)
        is TaskResult.Failure ->
          this.accountResolutionFailed(resolutionResult)
      }
    } catch (e: Throwable) {
      this.logger.error("account creation failed: ", e)
      this.taskRecorder.currentStepFailedAppending(this.strings.unexpectedException, "unexpectedException", e)
      this.publishFailureEvent()
      this.taskRecorder.finishFailure()
    }
  }

  private fun accountResolutionFailed(
    resolutionResult: TaskResult.Failure<AccountProviderType>
  ): TaskResult.Failure<AccountType> {
    this.logger.error("could not resolve an account provider description")
    this.taskRecorder.addAll(resolutionResult.steps)
    this.taskRecorder.addAttributes(resolutionResult.attributes)
    this.taskRecorder.currentStepFailed(
      this.strings.resolvingAccountProviderFailed, "resolvingAccountProviderFailed"
    )
    return this.taskRecorder.finishFailure()
  }

  private fun createAccount(
    accountProviderDescription: AccountProviderDescription
  ): TaskResult<AccountType> {
    val createResult =
      ProfileAccountCreateTask(
        accountEvents = this.accountEvents,
        accountProviderID = accountProviderDescription.id,
        accountProviders = this.accountProviderRegistry,
        profiles = this.profiles,
        strings = this.strings
      ).call()

    return when (createResult) {
      is TaskResult.Success<AccountType> -> {
        this.taskRecorder.addAll(createResult.steps)
        val account = createResult.result
        account.setPreferences(account.preferences.copy(catalogURIOverride = this.opdsURI))
        this.publishProgressEvent(this.taskRecorder.currentStepSucceeded(this.strings.creatingAccountSucceeded))
        this.taskRecorder.finishSuccess(createResult.result)
      }
      is TaskResult.Failure<AccountType> -> {
        this.taskRecorder.addAll(createResult.steps)
        this.taskRecorder.finishFailure()
      }
    }
  }

  /**
   * Create a custom account provider description.
   */

  private fun createAccountProviderDescription(): AccountProviderDescription {
    this.logger.debug("creating an account provider description")
    this.publishProgressEvent(this.taskRecorder.beginNewStep(this.strings.creatingAnAccountProviderDescription))

    val authDocumentURI: URI? = this.findAuthenticationDocumentURI()
    val id = URI.create("urn:custom:" + UUID.randomUUID().toString())
    this.logger.debug("account id will be {}", id)

    val links =
      mutableListOf<Link>()

    links.add(
      Link.LinkBasic(
        href = this.opdsURI,
        relation = "http://opds-spec.org/catalog"
      )
    )

    if (authDocumentURI != null) {
      links.add(
        Link.LinkBasic(
          href = authDocumentURI,
          type = MIMEType("application", "vnd.opds.authentication.v1.0+json", mapOf()),
          relation = OPDSFeedConstants.AUTHENTICATION_DOCUMENT_RELATION_URI_TEXT
        )
      )
    }

    val description =
      AccountProviderDescription(
        id = id,
        title = this.title,
        updated = DateTime.now(),
        links = links,
        images = listOf(),
        isProduction = true,
        isAutomatic = false,
        location = null
      )

    /*
     * Publish the description to the account provider registry. It is now possible
     * to create an account using the description; the account creation task will ask
     * the registry to resolve the account provider description.
     */

    this.accountProviderRegistry.updateDescription(description)
    return description
  }

  private fun findAuthenticationDocumentURI(): URI? {
    this.logger.debug("creating an account provider description")

    this.publishProgressEvent(this.taskRecorder.beginNewStep(this.strings.findingAuthDocumentURI))
    this.publishProgressEvent(this.taskRecorder.beginNewStep(this.strings.fetchingOPDSFeed))

    val request =
      this.httpClient.newRequest(this.opdsURI)
        .allowRedirects(ALLOW_REDIRECTS)
        .build()

    return request.execute().use { response ->
      this.taskRecorder.addAttributes(response.status.properties?.problemReport?.toMap() ?: emptyMap())

      when (val status = response.status) {
        is LSHTTPResponseStatus.Responded.OK -> {
          this.logger.debug("fetched opds feed")

          /*
           * Parse the result as an OPDS feed and then try to find the authentication document
           * link inside it.
           */

          try {
            this.publishProgressEvent(this.taskRecorder.beginNewStep(this.strings.parsingOPDSFeed))
            val feed = this.opdsFeedParser.parse(this.opdsURI, status.bodyStream)
            this.title = feed.feedTitle
            this.findAuthenticationDocumentLink(feed)
          } catch (e: Exception) {
            this.taskRecorder.currentStepFailed(
              e.message
                ?: e.javaClass.name,
              "parsingOPDSFeedFailed"
            )
            this.publishFailureEvent()
            throw e
          }
        }

        is LSHTTPResponseStatus.Responded.Error -> {
          this.logger.debug("failed to fetch opds feed")

          /*
           * If the server returns an error but delivers an authentication document as a result,
           * well... We've found the authentication document.
           */

          val contentTypes = status.properties.headers["content-type"]
          if (contentTypes != null) {
            if (contentTypes.contains("application/vnd.opds.authentication.v1.0+json")) {
              this.logger.debug("delivered authentication document instead of error")
              return this.opdsURI
            }
          }

          /*
           * Any other error is fatal.
           */

          this.taskRecorder.currentStepFailed(this.strings.fetchingOPDSFeedFailed, "fetchingOPDSFeedFailed")
          this.publishFailureEvent()
          throw IOException()
        }

        is LSHTTPResponseStatus.Failed -> {
          this.logger.debug("failed to fetch opds feed: ", status.exception)

          /*
           * An exception is fatal.
           */

          this.taskRecorder.currentStepFailed(
            this.strings.fetchingOPDSFeedFailed,
            "httpRequestFailed",
            status.exception
          )

          this.publishFailureEvent()
          throw status.exception
        }
      }
    }
  }

  private fun findAuthenticationDocumentLink(feed: OPDSAcquisitionFeed): URI? {
    this.publishProgressEvent(this.taskRecorder.beginNewStep(this.strings.searchingFeedForAuthenticationDocument))
    return this.someOrNull(feed.authDocument)
  }

  private fun <T> someOrNull(opt: OptionType<T>?): T? {
    return if (opt is Some<T>) {
      opt.get()
    } else {
      null
    }
  }

  private fun publishFailureEvent() =
    this.accountEvents.onNext(AccountEventCreationFailed(this.taskRecorder.finishFailure<AccountType>()))

  private fun publishProgressEvent(step: TaskStep) =
    this.accountEvents.onNext(AccountEventCreationInProgress(step.description))
}
