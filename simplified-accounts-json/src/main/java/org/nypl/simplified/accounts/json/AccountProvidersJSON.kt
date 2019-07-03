package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderImmutable
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.TreeMap

/**
 * Functions to load account providers from JSON data.
 */

object AccountProvidersJSON {

  /**
   * Serialize an account provider to a JSON node.
   */

  fun serializeToJSON(provider: AccountProviderType): ObjectNode {
    val mapper = ObjectMapper()

    val node = mapper.createObjectNode()
    node.put("id_uuid", provider.id.toString())
    node.put("catalogUrl", provider.catalogURI.toString())

    provider.authenticationDocumentURI?.let {
      node.put("authenticationDocument", it.toString()) }
    provider.catalogURIForUnder13s?.let {
      node.put("catalogUrlUnder13", it.toString()) }
    provider.catalogURIForOver13s?.let {
      node.put("catalogUrl13", it.toString()) }
    provider.patronSettingsURI?.let {
      node.put("patronSettingsUrl", it.toString()) }
    provider.annotationsURI?.let {
      node.put("annotationsUrl", it.toString()) }

    node.put("name", provider.displayName)

    provider.subtitle?.let {
      node.put("subtitle", it)
    }
    provider.logo?.let {
      node.put("logo", it.toString())
    }

    provider.authentication?.let { auth ->
      node.put("needsAuth", true)
      node.put("pinRequired", auth.requiresPin())
      node.put("authPasscodeLength", auth.passCodeLength())
      node.put("authPasscodeAllowsLetters", auth.passCodeMayContainLetters())
      node.put("loginUrl", auth.loginURI().toString())
    }

    node.put("addAutomatically", provider.addAutomatically)
    node.put("isProduction", provider.isProduction)
    node.put("supportsSimplyESync", provider.supportsSimplyESynchronization)
    node.put("supportsBarcodeScanner", provider.supportsBarcodeScanner)
    node.put("supportsBarcodeDisplay", provider.supportsBarcodeDisplay)
    node.put("supportsReservations", provider.supportsReservations)
    node.put("supportsCardCreator", provider.supportsCardCreator)
    node.put("supportsHelpCenter", provider.supportsHelpCenter)
    node.put("supportEmail", provider.supportEmail)

    provider.eula.let { node.put("eulaUrl", it.toString()) }
    provider.license.let { node.put("licenseUrl", it.toString()) }
    provider.privacyPolicy.let { node.put("privacyUrl", it.toString()) }

    node.put("mainColor", provider.mainColor)
    provider.styleNameOverride?.let { node.put("styleNameOverride", it) }

    node.put("updated", provider.updated.toString())
    return node
  }

  /**
   * Deserialize an account provider from the given JSON node.
   *
   * @param jom  A JSON object mapper
   * @param node A JSON node
   * @return A parsed account provider
   * @throws JSONParseException On parse errors
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: JsonNode): AccountProviderType {

    val obj =
      JSONParserUtilities.checkObject(null, node)
    val id_uuid =
      JSONParserUtilities.getURI(obj, "id_uuid")

    try {
      val catalogUrl =
        JSONParserUtilities.getURI(obj, "catalogUrl")

      val patronSettingsURI =
        applyPatronSettingHack(obj, catalogUrl)
      val annotationsURI =
        applyAnnotationsHack(obj, catalogUrl)

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
            applyLoansHack(obj, catalogUrl))
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

  private fun applyAnnotationsHack(
    objectNode: ObjectNode,
    catalogUrl: URI
  ): URI {
    val text = catalogUrl.toString().replace("/+$".toRegex(), "")
    val result = URI.create("$text/annotations/")
    return JSONParserUtilities.getURIDefault(objectNode, "annotationsUrl", result)
  }

  private fun applyPatronSettingHack(
    objectNode: ObjectNode,
    catalogUrl: URI
  ): URI {
    val text = catalogUrl.toString().replace("/+$".toRegex(), "")
    val result = URI.create("$text/patrons/me/")
    return JSONParserUtilities.getURIDefault(objectNode, "patronSettingsUrl", result)
  }

  private fun applyLoansHack(
    objectNode: ObjectNode,
    catalogUrl: URI
  ): URI {
    val text = catalogUrl.toString().replace("/+$".toRegex(), "")
    val result = URI.create("$text/loans/")
    return JSONParserUtilities.getURIDefault(objectNode, "loginUrl", result)
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
  fun deserializeCollectionFromJSONArray(node: ArrayNode): Map<URI, AccountProviderType> {

    val providers = TreeMap<URI, AccountProviderType>()
    var default_provider: AccountProviderType? = null

    var ex: JSONParseException? = null
    for (index in 0 until node.size()) {
      try {
        val provider = deserializeFromJSON(node.get(index))
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

    return providers
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
  fun deserializeCollectionFromStream(stream: InputStream): Map<URI, AccountProviderType> {
    val jom = ObjectMapper()
    val node = mapNullToTextNode(jom.readTree(stream))
    return deserializeCollectionFromJSONArray(JSONParserUtilities.checkArray(null, node))
  }

  /**
   * Deserialize a single account provider from the given stream.
   *
   * @param stream An input stream
   * @return A parsed account provider
   * @throws IOException On I/O or parser errors
   */

  @Throws(IOException::class)
  fun deserializeOneFromStream(stream: InputStream): AccountProviderType {
    val jom = ObjectMapper()
    val node = mapNullToTextNode(jom.readTree(stream))
    return deserializeFromJSON(JSONParserUtilities.checkObject(null, node))
  }

  /**
   * Deserialize a single account provider from the given file.
   *
   * @param stream An input stream
   * @return A parsed account provider
   * @throws IOException On I/O or parser errors
   */

  @Throws(IOException::class)
  fun deserializeOneFromFile(file: File): AccountProviderType =
    FileInputStream(file).use { stream -> this.deserializeOneFromStream(stream) }
}
