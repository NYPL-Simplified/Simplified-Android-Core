package org.nypl.simplified.accounts.source.nyplregistry

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.io7m.junreachable.UnimplementedCodeException
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
import org.nypl.simplified.accounts.api.AccountProviderResolutionErrorDetails.HTTPRequestFailed
import org.nypl.simplified.accounts.api.AccountProviderResolutionListenerType
import org.nypl.simplified.accounts.api.AccountProviderResolutionResult
import org.nypl.simplified.accounts.api.AccountProviderResolutionStringsType
import org.nypl.simplified.http.core.HTTPResultError
import org.nypl.simplified.http.core.HTTPResultException
import org.nypl.simplified.http.core.HTTPResultOK
import org.nypl.simplified.http.core.HTTPType
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocument
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParsersType
import org.nypl.simplified.opds.auth_document.api.AuthenticationObject
import org.nypl.simplified.opds.auth_document.api.AuthenticationObject.Companion.LABEL_LOGIN
import org.nypl.simplified.opds.auth_document.api.AuthenticationObject.Companion.LABEL_PASSWORD
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.taskrecorder.api.TaskRecorder
import org.nypl.simplified.taskrecorder.api.TaskRecorderType
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.URI

/**
 * An account provider description augmented with the logic needed to resolve the description
 * into a full provider.
 */

class AccountProviderSourceNYPLRegistryDescription(
  private val stringResources: AccountProviderResolutionStringsType,
  private val authDocumentParsers: AuthenticationDocumentParsersType,
  private val http: HTTPType,
  override val metadata: AccountProviderDescriptionMetadata) : AccountProviderDescriptionType {

  private val logger =
    LoggerFactory.getLogger(AccountProviderSourceNYPLRegistryDescription::class.java)

  override fun resolve(onProgress: AccountProviderResolutionListenerType): AccountProviderResolutionResult {
    val taskRecorder =
      TaskRecorder.create<AccountProviderResolutionErrorDetails>()

    try {
      taskRecorder.beginNewStep(this.stringResources.resolving)
      onProgress.invoke(this.metadata.id, this.stringResources.resolving)

      val authDocument =
        this.fetchAuthDocument(taskRecorder, onProgress)
      val updated =
        DateTime.now()
      val authenticationDescription =
        this.extractAuthenticationDescription(taskRecorder, authDocument)

      /*
       * The catalog URI is required to be present, but we obviously have no guarantee that it
       * actually will be.
       */

      val catalogURI = authDocument.startURI
        ?: this.metadata.catalogURI
        ?: run {
          taskRecorder.currentStepFailed(this.stringResources.resolvingAuthDocumentNoStartURI)
          onProgress.invoke(this.metadata.id, this.stringResources.resolvingAuthDocumentNoStartURI)
          throw IOException()
        }

      /*
       * The annotations URI can only be located by an authenticated user. We'll update
       * this account provider instance when the user views their loans feed.
       */

      val annotationsURI = null

      val supportsReservations =
        this.supportsReservations(authDocument)

      val accountProvider =
        AccountProviderImmutable(
          addAutomatically = this.metadata.isAutomatic,
          annotationsURI = annotationsURI,
          authentication = authenticationDescription,
          authenticationDocumentURI = this.metadata.authenticationDocumentURI!!,
          cardCreatorURI = authDocument.cardCreatorURI,
          catalogURI = catalogURI,
          displayName = this.metadata.title,
          eula = authDocument.eulaURI,
          id = this.metadata.id,
          isProduction = this.metadata.isProduction,
          license = authDocument.licenseURI,
          loansURI = authDocument.loansURI,
          logo = authDocument.logoURI ?: this.metadata.logoURI,
          mainColor = authDocument.mainColor,
          patronSettingsURI = authDocument.patronSettingsURI,
          privacyPolicy = authDocument.privacyPolicyURI,
          subtitle = authDocument.description,
          supportEmail = authDocument.supportURI?.toString(),
          supportsReservations = supportsReservations,
          supportsSimplyESynchronization = false,
          updated = updated
        )

      return AccountProviderResolutionResult(
        result = accountProvider,
        steps = taskRecorder.finish())
    } catch (e: Exception) {
      val currentStep = taskRecorder.currentStep()!!
      if (currentStep.exception == null) {
        taskRecorder.currentStepFailed(
          message = this.pickUsableMessage(currentStep.resolution, e),
          errorValue = currentStep.errorValue,
          exception = e)
      }
      return AccountProviderResolutionResult(null, taskRecorder.finish())
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

    taskRecorder.currentStepFailed(
      this.stringResources.resolvingAuthDocumentNoUsableAuthenticationTypes)
    throw IOException(this.stringResources.resolvingAuthDocumentNoUsableAuthenticationTypes)
  }

  private fun extractAuthenticationDescriptionBasic(
    authObject: AuthenticationObject): AccountProviderAuthenticationDescription.Basic {

    val loginRestrictions =
      authObject.inputs[LABEL_LOGIN]
    val passwordRestrictions =
      authObject.inputs[LABEL_PASSWORD]

    return AccountProviderAuthenticationDescription.Basic(
      barcodeFormat = loginRestrictions ?.barcodeFormat,
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
      link.rel == "http://librarysimplified.org/terms/rel/authentication/restriction-not-met"
    }?.href
    val over13 = authObject.links.find { link ->
      link.rel == "http://librarysimplified.org/terms/rel/authentication/restriction-met"
    }?.href

    return if (under13 != null && over13 != null) {
      AccountProviderAuthenticationDescription.COPPAAgeGate(
        greaterEqual13 = over13,
        under13 = under13
      )
    } else {
      taskRecorder.currentStepFailed(this.stringResources.resolvingAuthDocumentCOPPAAgeGateMalformed)
      throw IOException(this.stringResources.resolvingAuthDocumentCOPPAAgeGateMalformed)
    }
  }

  private fun pickUsableMessage(message: String, e: Exception): String {
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

  private fun fetchAuthDocument(
    taskRecorder: TaskRecorderType<AccountProviderResolutionErrorDetails>,
    onProgress: AccountProviderResolutionListenerType
  ): AuthenticationDocument {
    taskRecorder.beginNewStep(this.stringResources.resolvingAuthDocument)
    onProgress.invoke(this.metadata.id, this.stringResources.resolvingAuthDocument)

    val targetURI = this.metadata.authenticationDocumentURI
    if (targetURI == null) {
      taskRecorder.currentStepFailed(this.stringResources.resolvingAuthDocumentMissingURI)
      onProgress.invoke(this.metadata.id, this.stringResources.resolvingAuthDocumentMissingURI)
      throw NoSuchElementException()
    }

    return when (val result = this.http.get(Option.none(), targetURI, 0L)) {
      is HTTPResultError -> {
        taskRecorder.currentStepFailed(
          message = this.stringResources.resolvingAuthDocumentRetrievalFailed,
          errorValue = HTTPRequestFailed(
            message = result.message,
            errorCode = result.status,
            problemReport = this.someOrNull(result.problemReport)))
        throw IOException(result.message)
      }

      is HTTPResultException ->
        throw result.error

      is HTTPResultOK ->
        this.parseAuthenticationDocument(targetURI, result.value, taskRecorder)

      else -> throw UnreachableCodeException()
    }
  }

  private fun parseAuthenticationDocument(
    targetURI: URI,
    stream: InputStream,
    taskRecorder: TaskRecorderType<AccountProviderResolutionErrorDetails>
  ): AuthenticationDocument {
    return this.authDocumentParsers.createParser(targetURI, stream).use { parser ->
      when (val parseResult = parser.parse()) {
        is ParseResult.Success -> {
          parseResult.warnings.forEach { warning -> this.logger.warn("{}", warning.message) }
          parseResult.result
        }
        is ParseResult.Failure -> {
          taskRecorder.currentStepFailed(
            message = this.stringResources.resolvingAuthDocumentParseFailed,
            errorValue = AccountProviderResolutionErrorDetails.AuthDocumentParseFailed(
              parseResult.warnings,
              parseResult.errors))
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