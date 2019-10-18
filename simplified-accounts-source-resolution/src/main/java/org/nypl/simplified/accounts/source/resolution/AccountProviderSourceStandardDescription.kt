package org.nypl.simplified.accounts.source.resolution

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnreachableCodeException
import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Companion.ANONYMOUS_TYPE
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Companion.BASIC_TYPE
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Companion.COPPA_TYPE
import org.nypl.simplified.accounts.api.AccountProviderDescriptionMetadata
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType
import org.nypl.simplified.accounts.api.AccountProviderImmutable
import org.nypl.simplified.accounts.api.AccountProviderResolutionErrorDetails
import org.nypl.simplified.accounts.api.AccountProviderResolutionErrorDetails.AuthDocumentParseFailed
import org.nypl.simplified.accounts.api.AccountProviderResolutionErrorDetails.AuthDocumentUnusable
import org.nypl.simplified.accounts.api.AccountProviderResolutionErrorDetails.HTTPRequestFailed
import org.nypl.simplified.accounts.api.AccountProviderResolutionErrorDetails.UnexpectedException
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.links.Link
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocument
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.opds.auth_document.api.AuthenticationObject
import org.nypl.simplified.opds.auth_document.api.AuthenticationObject.Companion.LABEL_LOGIN
import org.nypl.simplified.opds.auth_document.api.AuthenticationObject.Companion.LABEL_PASSWORD
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.nypl.simplified.taskrecorder.api.TaskResult
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * An account provider description augmented with the logic needed to resolve the description
 * into a full provider using standard NYPL logic.
 */

class AccountProviderSourceStandardDescription(
  private val stringResources: AccountProviderResolutionStringsType,
  private val authDocumentParsers: AuthenticationDocumentParsersType,
  private val http: HTTPType,
  override val metadata: AccountProviderDescriptionMetadata) : AccountProviderDescriptionType {

  private val logger =
    LoggerFactory.getLogger(AccountProviderSourceStandardDescription::class.java)

  override fun resolve(onProgress: AccountProviderResolutionListenerType)
    : TaskResult<AccountProviderResolutionErrorDetails, AccountProviderType> {
    val taskRecorder =
      TaskRecorder.create<AccountProviderResolutionErrorDetails>()

    return try {
      this.logger.debug("starting resolution")
      taskRecorder.beginNewStep(this.stringResources.resolving)
      onProgress.invoke(this.metadata.id, this.stringResources.resolving)

      val authDocument =
        this.fetchAuthDocument(taskRecorder, onProgress)

      val supportsReservations =
        if (authDocument != null) {
          this.supportsReservations(authDocument)
        } else {
          false
        }

      val authenticationDescription =
        if (authDocument != null) {
          this.extractAuthenticationDescription(taskRecorder, authDocument)
        } else {
          null
        }

      val updated =
        DateTime.now()

      /*
       * The catalog URI is required to be present, but we obviously have no guarantee that it
       * actually will be.
       */

      val catalogURI =
        authDocument?.startURI
          ?: this.metadata.catalogURI
            ?.hrefURI
          ?: run {
            val message = this.stringResources.resolvingAuthDocumentNoStartURI
            taskRecorder.currentStepFailed(
              message = message,
              errorValue = AuthDocumentUnusable(
                message = message,
                accountProviderID = this.metadata.id.toASCIIString(),
                accountProviderTitle = this.metadata.title
              ))
            onProgress.invoke(this.metadata.id, message)
            throw IOException()
          }

      /*
       * The annotations URI can only be located by an authenticated user. We'll update
       * this account provider instance when the user views their loans feed.
       */

      val annotationsURI = null

      val accountProvider =
        AccountProviderImmutable(
          addAutomatically = this.metadata.isAutomatic,
          annotationsURI = annotationsURI,
          authentication = authenticationDescription,
          authenticationDocumentURI = this.metadata.authenticationDocumentURI?.hrefURI,
          cardCreatorURI = authDocument?.cardCreatorURI,
          catalogURI = catalogURI,
          displayName = this.metadata.title,
          eula = authDocument?.eulaURI,
          id = this.metadata.id,
          isProduction = this.metadata.isProduction,
          license = authDocument?.licenseURI,
          loansURI = authDocument?.loansURI,
          logo = authDocument?.logoURI ?: this.metadata.logoURI?.hrefURI,
          mainColor = authDocument?.mainColor ?: "red",
          patronSettingsURI = authDocument?.patronSettingsURI,
          privacyPolicy = authDocument?.privacyPolicyURI,
          subtitle = authDocument?.description,
          supportEmail = authDocument?.supportURI?.toString(),
          supportsReservations = supportsReservations,
          supportsSimplyESynchronization = false,
          updated = updated)

      taskRecorder.finishSuccess(accountProvider)
    } catch (e: Exception) {
      this.logger.error("failed to resolve account provider: ", e)
      taskRecorder.currentStepFailedAppending(
        this.stringResources.resolvingUnexpectedException,
        errorValue = UnexpectedException(
          message = this.stringResources.resolvingUnexpectedException,
          exception = e,
          accountProviderID = this.metadata.id.toASCIIString(),
          accountProviderTitle = this.metadata.title
        ),
        exception = e)
      taskRecorder.finishFailure()
    }
  }

  private fun supportsReservations(authDocument: AuthenticationDocument): Boolean {
    return authDocument.features.enabled.contains("https://librarysimplified.org/rel/policy/reservations")
  }

  private fun extractAuthenticationDescription(
    taskRecorder: TaskRecorderType<AccountProviderResolutionErrorDetails>,
    authDocument: AuthenticationDocument
  ): AccountProviderAuthenticationDescription? {

    if (authDocument.authentication.isEmpty()) {
      return null
    }

    for (authObject in authDocument.authentication) {
      when (val authType = authObject.type.toASCIIString()) {
        BASIC_TYPE ->
          return this.extractAuthenticationDescriptionBasic(authObject)
        COPPA_TYPE ->
          return this.extractAuthenticationDescriptionCOPPA(taskRecorder, authObject)
        ANONYMOUS_TYPE ->
          return null
        else ->
          this.logger.warn("encountered unrecognized authentication type: {}", authType)
      }
    }

    val message = this.stringResources.resolvingAuthDocumentNoUsableAuthenticationTypes
    taskRecorder.currentStepFailed(message, AuthDocumentUnusable(
      message = message,
      accountProviderTitle = this.metadata.title,
      accountProviderID = this.metadata.id.toASCIIString()
    ))
    throw IOException(message)
  }

  private fun extractAuthenticationDescriptionBasic(
    authObject: AuthenticationObject): AccountProviderAuthenticationDescription.Basic {

    val loginRestrictions =
      authObject.inputs[LABEL_LOGIN]
    val passwordRestrictions =
      authObject.inputs[LABEL_PASSWORD]

    return AccountProviderAuthenticationDescription.Basic(
      barcodeFormat = loginRestrictions?.barcodeFormat,
      keyboard = loginRestrictions?.keyboardType,
      passwordMaximumLength = passwordRestrictions?.maximumLength ?: 0,
      passwordKeyboard = passwordRestrictions?.keyboardType,
      description = authObject.description,
      labels = authObject.labels)
  }

  private fun extractAuthenticationDescriptionCOPPA(
    taskRecorder: TaskRecorderType<AccountProviderResolutionErrorDetails>,
    authObject: AuthenticationObject): AccountProviderAuthenticationDescription.COPPAAgeGate {
    val under13 = authObject.links.find { link ->
      link.relation == "http://librarysimplified.org/terms/rel/authentication/restriction-not-met"
    }?.hrefURI
    val over13 = authObject.links.find { link ->
      link.relation == "http://librarysimplified.org/terms/rel/authentication/restriction-met"
    }?.hrefURI

    return if (under13 != null && over13 != null) {
      AccountProviderAuthenticationDescription.COPPAAgeGate(
        greaterEqual13 = over13,
        under13 = under13
      )
    } else {
      val message = this.stringResources.resolvingAuthDocumentCOPPAAgeGateMalformed
      taskRecorder.currentStepFailed(message, AuthDocumentUnusable(
        message = message,
        accountProviderID = this.metadata.id.toASCIIString(),
        accountProviderTitle = this.metadata.title
      ))
      throw IOException(message)
    }
  }

  private fun fetchAuthDocument(
    taskRecorder: TaskRecorderType<AccountProviderResolutionErrorDetails>,
    onProgress: AccountProviderResolutionListenerType
  ): AuthenticationDocument? {
    this.logger.debug("fetching authentication document")
    taskRecorder.beginNewStep(this.stringResources.resolvingAuthDocument)
    onProgress.invoke(this.metadata.id, this.stringResources.resolvingAuthDocument)

    val targetLink =
      this.metadata.authenticationDocumentURI ?: return null

    return when (targetLink) {
      is Link.LinkBasic -> {
        when (val result = this.http.get(Option.none(), targetLink.href, 0L)) {
          is HTTPResultError -> {
            taskRecorder.currentStepFailed(
              message = this.stringResources.resolvingAuthDocumentRetrievalFailed,
              errorValue = HTTPRequestFailed(
                message = result.message,
                errorCode = result.status,
                accountProviderID = this.metadata.id.toASCIIString(),
                accountProviderTitle = this.metadata.title,
                problemReport = this.someOrNull(result.problemReport)))
            throw IOException(result.message)
          }

          is HTTPResultException ->
            throw result.error

          is HTTPResultOK ->
            this.parseAuthenticationDocument(targetLink.href, result.value, taskRecorder)

          else -> throw UnreachableCodeException()
        }
      }

      is Link.LinkTemplated -> {
        val message = this.stringResources.resolvingAuthDocumentUnusableLink
        taskRecorder.currentStepFailed(
          message = message,
          errorValue = AccountProviderResolutionErrorDetails.AuthDocumentUnusableLink(
            message = message,
            accountProviderID = this.metadata.id.toASCIIString(),
            accountProviderTitle = this.metadata.title))
        throw IOException(message)
      }
    }
  }

  private fun parseAuthenticationDocument(
    targetURI: URI,
    stream: InputStream,
    taskRecorder: TaskRecorderType<AccountProviderResolutionErrorDetails>
  ): AuthenticationDocument {
    this.logger.debug("parsing authentication document")
    return this.authDocumentParsers.createParser(targetURI, stream).use { parser ->
      when (val parseResult = parser.parse()) {
        is ParseResult.Success -> {
          parseResult.warnings.forEach { warning -> this.logger.warn("{}", warning.message) }
          parseResult.result
        }
        is ParseResult.Failure -> {
          taskRecorder.currentStepFailed(
            message = this.stringResources.resolvingAuthDocumentParseFailed,
            errorValue = AuthDocumentParseFailed(
              message = this.stringResources.resolvingAuthDocumentParseFailed,
              accountProviderTitle = this.metadata.title,
              accountProviderID = this.metadata.id.toASCIIString(),
              warnings = parseResult.warnings,
              errors = parseResult.errors))
          throw IOException(this.stringResources.resolvingAuthDocumentParseFailed)
        }
      }
    }
  }

  private fun <T> someOrNull(option: OptionType<T>): T? {
    return if (option is Some<T>) {
      option.get()
    } else {
      null
    }
  }
}