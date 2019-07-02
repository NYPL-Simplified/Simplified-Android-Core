package org.nypl.simplified.accounts.source.filebased

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.TextNode
import com.io7m.jnull.NullCheck

import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderCollectionType
import org.nypl.simplified.accounts.api.AccountProviderImmutable
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities

import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.TreeMap

/**
 * Functions to load account providers from JSON data.
 */

object AccountProvidersJSON {

  /**
   * Deserialize an account provider from the given JSON node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed account provider
   * @throws JSONParseException On parse errors
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    jom: ObjectMapper,
    node: JsonNode): AccountProviderType {

    NullCheck.notNull(jom, "Object mapper")
    NullCheck.notNull(node, "JSON")

    val obj =
      JSONParserUtilities.checkObject(null, node)
    val id_uuid =
      JSONParserUtilities.getURI(obj, "id_uuid")

    try {
      val catalogUrl =
        JSONParserUtilities.getURI(obj, "catalogUrl")
      val patronSettingsURI =
        applyPatronSettingHack(catalogUrl)
      val annotationsURI =
        applyAnnotationsHack(catalogUrl)
      val authenticationDocument =
        JSONParserUtilities.getURIOrNull(obj, "authenticationDocument")
      val catalogUrlUnder13 =
        JSONParserUtilities.getURIOrNull(obj, "catalogUrlUnder13")
      val catalogUrl13 =
        JSONParserUtilities.getURIOrNull(obj, "catalogUrl13")
      val name =
        JSONParserUtilities.getString(obj, "name")
      val subtitle =
        JSONParserUtilities.getStringOrNull(obj, "subtitle")
      val logo =
        JSONParserUtilities.getURIOrNull(obj, "logo")

      val authentication =
        if (JSONParserUtilities.getBooleanDefault(obj, "needsAuth", false)) {
          val authenticationBuilder =
            AccountProviderAuthenticationDescription.builder()

          authenticationBuilder.setRequiresPin(
            JSONParserUtilities.getBooleanDefault(obj, "pinRequired", true))
          authenticationBuilder.setPassCodeLength(
            JSONParserUtilities.getIntegerDefault(obj, "authPasscodeLength", 0))
          authenticationBuilder.setPassCodeMayContainLetters(
            JSONParserUtilities.getBooleanDefault(obj, "authPasscodeAllowsLetters", true))
          authenticationBuilder.setLoginURI(
            JSONParserUtilities.getURIDefault(obj, "loginUrl", applyLoansHack(catalogUrl)))

          authenticationBuilder.build()
        } else {
          null
        }

      val addAutomatically =
        JSONParserUtilities.getBooleanDefault(obj, "addAutomatically", false)
      val isProduction =
        JSONParserUtilities.getBooleanDefault(obj, "isProduction", false)
      val supportsSimplyESync =
        JSONParserUtilities.getBooleanDefault(obj, "supportsSimplyESync", false)
      val supportsBarcodeScanner =
        JSONParserUtilities.getBooleanDefault(obj, "supportsBarcodeScanner", false)
      val supportsBarcodeDisplay =
        JSONParserUtilities.getBooleanDefault(obj, "supportsBarcodeDisplay", false)
      val supportsReservations =
        JSONParserUtilities.getBooleanDefault(obj, "supportsReservations", false)
      val supportsCardCreator =
        JSONParserUtilities.getBooleanDefault(obj, "supportsCardCreator", false)
      val supportsHelpCenter =
        JSONParserUtilities.getBooleanDefault(obj, "supportsHelpCenter", false)
      val supportEmail =
        JSONParserUtilities.getStringOrNull(obj, "supportEmail")
      val eula =
        JSONParserUtilities.getURIOrNull(obj, "eulaUrl")
      val licenseURI =
        JSONParserUtilities.getURIOrNull(obj, "licenseUrl")
      val privacyURL =
        JSONParserUtilities.getURIOrNull(obj, "privacyUrl")
      val mainColor =
        JSONParserUtilities.getString(obj, "mainColor")
      val styleNameOverride =
        JSONParserUtilities.getStringOrNull(obj, "styleNameOverride")
      val updated =
        JSONParserUtilities.getTimestamp(obj, "updated")

      return AccountProviderImmutable(
        id = id_uuid,
        isProduction = isProduction,
        displayName = name,
        subtitle = subtitle,
        logo = logo,
        authentication = authentication,
        supportEmail = supportEmail,
        supportsSimplyESynchronization = supportsSimplyESync,
        supportsBarcodeScanner = supportsBarcodeScanner,
        supportsBarcodeDisplay = supportsBarcodeDisplay,
        supportsReservations = supportsReservations,
        supportsCardCreator = supportsCardCreator,
        supportsHelpCenter = supportsHelpCenter,
        authenticationDocumentURI = authenticationDocument,
        catalogURI = catalogUrl,
        catalogURIForOver13s = catalogUrl13,
        catalogURIForUnder13s = catalogUrlUnder13,
        eula = eula,
        license = licenseURI,
        privacyPolicy = privacyURL,
        mainColor = mainColor,
        styleNameOverride = styleNameOverride,
        addAutomatically = addAutomatically,
        patronSettingsURI = patronSettingsURI,
        annotationsURI = annotationsURI,
        updated = updated)
    } catch (e: JSONParseException) {
      throw JSONParseException("Unable to parse provider $id_uuid", e)
    }
  }

  private fun applyAnnotationsHack(catalogUrl: URI): URI {
    val text = catalogUrl.toString().replace("/+$".toRegex(), "")
    return URI.create("$text/annotations/")
  }

  private fun applyPatronSettingHack(catalogUrl: URI): URI {
    val text = catalogUrl.toString().replace("/+$".toRegex(), "")
    return URI.create("$text/patrons/me/")
  }

  private fun applyLoansHack(catalogUrl: URI): URI {
    val text = catalogUrl.toString().replace("/+$".toRegex(), "")
    return URI.create("$text/loans/")
  }

  /**
   * Deserialize a set of account providers from the given JSON array node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed account provider collection
   * @throws JSONParseException On parse errors
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSONArray(
    jom: ObjectMapper,
    node: ArrayNode): AccountProviderCollectionType {

    val providers = TreeMap<URI, AccountProviderType>()
    var default_provider: AccountProviderType? = null

    var ex: JSONParseException? = null
    for (index in 0 until node.size()) {
      try {
        val provider = deserializeFromJSON(jom, node.get(index))
        if (default_provider == null) {
          default_provider = provider
        }
        if (providers.containsKey(provider.id)) {
          throw JSONParseException("Duplicate provider ID: " + provider.id)
        }
        providers[provider.id] = provider
      } catch (e: JSONParseException) {
        if (ex == null) {
          ex = e
        } else {
          ex.addSuppressed(e)
        }
      }

    }

    if (ex != null) {
      throw ex
    }

    if (providers.isEmpty()) {
      throw JSONParseException("No providers were parsed.")
    }

    return AccountProviderCollection(default_provider!!, providers)
  }

  /**
   * Deserialize a set of account providers from the given JSON array node.
   *
   * @param text A JSON string
   * @return A parsed account provider collection
   * @throws IOException On I/O or parser errors
   */

  @Throws(IOException::class)
  fun deserializeFromString(
    text: String): AccountProviderCollectionType {
    NullCheck.notNull(text, "Text")

    val jom = ObjectMapper()
    val node = mapNullToTextNode(jom.readTree(text))
    return deserializeFromJSONArray(jom, JSONParserUtilities.checkArray(null, node))
  }

  private fun mapNullToTextNode(jsonNode: JsonNode?): JsonNode {
    return jsonNode ?: TextNode("")
  }

  /**
   * Deserialize a set of account providers from the given JSON array node.
   *
   * @param stream An input stream
   * @return A parsed account provider collection
   * @throws IOException On I/O or parser errors
   */

  @Throws(IOException::class)
  fun deserializeFromStream(
    stream: InputStream): AccountProviderCollectionType {
    NullCheck.notNull(stream, "Stream")

    val jom = ObjectMapper()
    val node = mapNullToTextNode(jom.readTree(stream))
    return deserializeFromJSONArray(jom, JSONParserUtilities.checkArray(null, node))
  }
}
