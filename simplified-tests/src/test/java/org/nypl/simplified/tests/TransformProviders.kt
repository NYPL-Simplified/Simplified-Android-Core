package org.nypl.simplified.tests

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.json.AccountProvidersJSON
import java.io.File
import java.io.FileOutputStream
import java.net.URI

class TransformProviders {

  companion object {

    @JvmStatic
    fun main(args: Array<String>) {
      val mapper = ObjectMapper()
      val file = args[0]
      val output = args[1]

      val entries =
        mapper.readValue<Array<Entry>>(File(file), object : TypeReference<Array<Entry>>() {})

      val providers = mutableListOf<AccountProvider>()
      for (entry in entries) {
        val authentication =
          if (entry.needsAuth) {
            AccountProviderAuthenticationDescription.Basic(
              barcodeFormat = if (entry.supportsBarcodeDisplay) "CodaBar" else null,
              keyboard = AccountProviderAuthenticationDescription.KeyboardInput.valueOf(
                entry.loginKeyboard
                  ?: "DEFAULT"
              ),
              passwordMaximumLength = entry.authPasscodeLength,
              passwordKeyboard = AccountProviderAuthenticationDescription.KeyboardInput.valueOf(
                entry.pinKeyboard
                  ?: "DEFAULT"
              ),
              description = "",
              labels = mapOf(),
              logoURI = null
            )
          } else {
            if (entry.catalogUrl13 != null) {
              AccountProviderAuthenticationDescription.COPPAAgeGate(
                greaterEqual13 = URI.create(entry.catalogUrl13),
                under13 = URI.create(entry.catalogUrlUnder13)
              )
            } else {
              AccountProviderAuthenticationDescription.Anonymous
            }
          }

        val provider =
          AccountProvider(
            addAutomatically = false,
            announcements = emptyList(),
            authenticationDocumentURI = null,
            authentication = authentication,
            authenticationAlternatives = listOf(),
            catalogURI = URI.create(entry.catalogUrl),
            cardCreatorURI = null,
            displayName = entry.name!!,
            eula = entry.eulaUrl?.let { URI.create(it) },
            id = URI.create(entry.id_uuid),
            idNumeric = entry.id_numeric,
            isProduction = entry.inProduction,
            license = entry.licenseUrl?.let { URI.create(it) },
            loansURI = null,
            logo = entry.logo,
            mainColor = entry.mainColor!!,
            patronSettingsURI = null,
            privacyPolicy = entry.privacyUrl,
            subtitle = entry.subtitle,
            supportEmail = entry.supportEmail,
            supportsReservations = entry.supportsReservations,
            updated = DateTime.parse(entry.updated),
            location = null
          )
        providers.add(provider)
      }

      val arrayNode = mapper.createArrayNode()
      for (provider in providers) {
        arrayNode.add(AccountProvidersJSON.serializeToJSON(provider))
      }

      FileOutputStream(output).use { stream ->
        mapper.writerWithDefaultPrettyPrinter().writeValue(stream, arrayNode)
        stream.flush()
      }
    }
  }
}

@JsonDeserialize
class Entry {
  @JvmField
  val authPasscodeLength: Int = -1

  @JvmField
  val authPasscodeAllowsLetters: Boolean = false

  @JvmField
  val authenticationDocument: String? = null

  @JvmField
  val cardCreatorUrl: String? = null

  @JvmField
  val catalogUrl: String? = null

  @JvmField
  val catalogUrl13: String? = null

  @JvmField
  val catalogUrlUnder13: String? = null

  @JvmField
  val eulaUrl: String? = null

  @JvmField
  val id_numeric: Int = -1

  @JvmField
  val id_uuid: String? = null

  @JvmField
  val inProduction: Boolean = false

  @JvmField
  val licenseUrl: String? = null

  @JvmField
  val loginKeyboard: String? = null

  @JvmField
  val logo: URI? = null

  @JvmField
  val mainColor: String? = null

  @JvmField
  val name: String? = null

  @JvmField
  val needsAuth: Boolean = false

  @JvmField
  val pinKeyboard: String? = null

  @JvmField
  val privacyUrl: URI? = null

  @JvmField
  val subtitle: String? = null

  @JvmField
  val supportEmail: String? = null

  @JvmField
  val supportsBarcodeDisplay: Boolean = false

  @JvmField
  val supportsBarcodeScanner: Boolean = false

  @JvmField
  val supportsCardCreator: Boolean = false

  @JvmField
  val supportsReservations: Boolean = false

  @JvmField
  val supportsSimplyESync: Boolean = false

  @JvmField
  val updated: String = "2019-07-08T19:17:00+00:00"
}
