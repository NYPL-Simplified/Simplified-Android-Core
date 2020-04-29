package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Basic
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.COPPAAgeGate
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Locale
import java.util.TreeMap

/**
 * Functions to load account providers from JSON data.
 */

object AccountProvidersJSON {

  private val logger = LoggerFactory.getLogger(AccountProvidersJSON::class.java)

  private fun <T> putConditionally(node: ObjectNode, name: String, value: T?) {
    value?.let { v -> node.put(name, v.toString()) }
  }

  /**
   * Serialize an account provider to a JSON node.
   */

  fun serializeToJSON(provider: AccountProviderType): ObjectNode {
    val mapper = ObjectMapper()
    val node = mapper.createObjectNode()

    node.put("@version", "20190708")
    node.put("addAutomatically", provider.addAutomatically)
    node.put("displayName", provider.displayName)
    node.put("idNumeric", provider.idNumeric)
    node.put("idUUID", provider.id.toString())
    node.put("isProduction", provider.isProduction)
    node.put("mainColor", provider.mainColor)
    node.put("supportsReservations", provider.supportsReservations)
    node.put("updated", provider.updated.toString())

    this.putConditionally(node, "annotationsURI", provider.annotationsURI)
    this.putConditionally(node, "authenticationDocumentURI", provider.authenticationDocumentURI)
    this.putConditionally(node, "cardCreatorURI", provider.cardCreatorURI)
    this.putConditionally(node, "catalogURI", provider.catalogURI)
    this.putConditionally(node, "eula", provider.eula)
    this.putConditionally(node, "license", provider.license)
    this.putConditionally(node, "loansURI", provider.loansURI)
    this.putConditionally(node, "logo", provider.logo)
    this.putConditionally(node, "patronSettingsURI", provider.patronSettingsURI)
    this.putConditionally(node, "privacyPolicy", provider.privacyPolicy)
    this.putConditionally(node, "subtitle", provider.subtitle)
    this.putConditionally(node, "supportEmail", provider.supportEmail)

    when (val auth = provider.authentication) {
      is COPPAAgeGate -> {
        val authObject = mapper.createObjectNode()
        authObject.put("type", AccountProviderAuthenticationDescription.COPPA_TYPE)
        authObject.put("greaterEqual13", auth.greaterEqual13.toString())
        authObject.put("under13", auth.under13.toString())
        node.set<ObjectNode>("authentication", authObject)
      }
      is Basic -> {
        val authObject = mapper.createObjectNode()
        authObject.put("type", AccountProviderAuthenticationDescription.BASIC_TYPE)
        this.putConditionally(authObject, "barcodeFormat", auth.barcodeFormat?.toUpperCase())
        this.putConditionally(authObject, "description", auth.description)
        this.putConditionally(authObject, "keyboard", auth.keyboard.name)
        this.putConditionally(authObject, "passwordKeyboard", auth.passwordKeyboard.name)
        authObject.put("passwordMaximumLength", auth.passwordMaximumLength)
        authObject.set<ObjectNode>("labels", this.mapToObject(mapper, auth.labels))
        node.set<ObjectNode>("authentication", authObject)
      }
      null -> {
      }
    }

    return node
  }

  private fun mapToObject(
    mapper: ObjectMapper,
    labels: Map<String, String>
  ): ObjectNode {
    val node = mapper.createObjectNode()
    for (key in labels.keys) {
      node.put(key, labels[key])
    }
    return node
  }

  /**
   * Deserialize an account provider from the given JSON node.
   *
   * @param jom A JSON object mapper
   * @param node A JSON node
   * @return A parsed account provider
   * @throws JSONParseException On parse errors
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(node: JsonNode): AccountProvider {

    val obj =
      JSONParserUtilities.checkObject(null, node)
    val idUUID =
      JSONParserUtilities.getURI(obj, "idUUID")

    try {
      val addAutomatically =
        JSONParserUtilities.getBooleanDefault(obj, "addAutomatically", false)
      val annotationsURI =
        JSONParserUtilities.getURIOrNull(obj, "annotationsURI")
      val authenticationDocumentURI =
        JSONParserUtilities.getURIOrNull(obj, "authenticationDocumentURI")
      val cardCreatorURI =
        JSONParserUtilities.getURIOrNull(obj, "cardCreatorURI")
      val catalogURI =
        JSONParserUtilities.getURIOrNull(obj, "catalogURI")!!
      val authentication =
        this.parseAuthentication(obj)
      val displayName =
        JSONParserUtilities.getString(obj, "displayName")
      val mainColor =
        JSONParserUtilities.getStringOrNull(obj, "mainColor") ?: "red"
      val eula =
        JSONParserUtilities.getURIOrNull(obj, "eula")
      val license =
        JSONParserUtilities.getURIOrNull(obj, "license")
      val logo =
        JSONParserUtilities.getURIOrNull(obj, "logo")
      val loansURI =
        JSONParserUtilities.getURIOrNull(obj, "loansURI")
      val patronSettingsURI =
        JSONParserUtilities.getURIOrNull(obj, "patronSettingsURI")
      val privacyPolicy =
        JSONParserUtilities.getURIOrNull(obj, "privacyPolicy")
      val subtitle =
        JSONParserUtilities.getStringOrNull(obj, "subtitle") ?: ""
      val supportEmail =
        JSONParserUtilities.getStringOrNull(obj, "supportEmail")
      val supportsReservations =
        JSONParserUtilities.getBooleanDefault(obj, "supportsReservations", false)
      val supportsSimplyESynchronization =
        JSONParserUtilities.getBooleanDefault(obj, "supportsSimplyESynchronization", false)
      val isProduction =
        JSONParserUtilities.getBooleanDefault(obj, "isProduction", false)
      val idNumeric =
        JSONParserUtilities.getIntegerDefault(obj, "idNumeric", -1)

      val updated =
        JSONParserUtilities.getStringOrNull(obj, "updated")
          ?.let { text -> DateTime.parse(text) }
          ?: DateTime.now()

      return AccountProvider(
        addAutomatically = addAutomatically,
        annotationsURI = annotationsURI,
        authentication = authentication,
        authenticationDocumentURI = authenticationDocumentURI,
        cardCreatorURI = cardCreatorURI,
        catalogURI = catalogURI,
        displayName = displayName,
        eula = eula,
        id = idUUID,
        idNumeric = idNumeric,
        isProduction = isProduction,
        license = license,
        logo = logo,
        mainColor = mainColor,
        loansURI = loansURI,
        patronSettingsURI = patronSettingsURI,
        privacyPolicy = privacyPolicy,
        subtitle = subtitle,
        supportEmail = supportEmail,
        supportsReservations = supportsReservations,
        supportsSimplyESynchronization = supportsSimplyESynchronization,
        updated = updated
      )
    } catch (e: JSONParseException) {
      throw JSONParseException("Unable to parse provider $idUUID", e)
    }
  }

  class AccountProviderParsedData(
    val id: URI,
    val idNumeric: Int,
    val isProduction: Boolean,
    val displayName: String,
    val subtitle: String?,
    val logo: URI?,
    val authentication: AccountProviderAuthenticationDescription?,
    val supportsSimplyESynchronization: Boolean,
    val supportsReservations: Boolean,
    val loansURI: URI?,
    val cardCreatorURI: URI?,
    val authenticationDocumentURI: URI?,
    val catalogURI: URI,
    val supportEmail: String?,
    val eula: URI?,
    val license: URI?,
    val privacyPolicy: URI?,
    val mainColor: String,
    val addAutomatically: Boolean,
    val patronSettingsURI: URI?,
    val annotationsURI: URI?,
    val updated: DateTime
  )

  private fun parseAuthentication(
    obj: ObjectNode
  ): AccountProviderAuthenticationDescription? {
    return if (obj.has("authentication")) {
      val container = JSONParserUtilities.getObject(obj, "authentication")
      when (val authType = JSONParserUtilities.getString(container, "type")) {
        AccountProviderAuthenticationDescription.BASIC_TYPE -> {
          val labels =
            this.toStringMap(JSONParserUtilities.getObject(container, "labels"))
          val barcodeFormat =
            JSONParserUtilities.getStringOrNull(container, "barcodeFormat")
              ?.toUpperCase(Locale.ROOT)
          val keyboard =
            this.parseKeyboardType(JSONParserUtilities.getStringOrNull(container, "keyboard"))
          val passwordMaximumLength =
            JSONParserUtilities.getIntegerDefault(container, "passwordMaximumLength", 0)
          val passwordKeyboard =
            this.parseKeyboardType(
              JSONParserUtilities.getStringOrNull(
                container,
                "passwordKeyboard"
              )
            )
          val description =
            JSONParserUtilities.getString(container, "description")

          Basic(
            barcodeFormat = barcodeFormat,
            keyboard = keyboard,
            passwordMaximumLength = passwordMaximumLength,
            passwordKeyboard = passwordKeyboard,
            description = description,
            labels = labels
          )
        }
        AccountProviderAuthenticationDescription.COPPA_TYPE -> {
          COPPAAgeGate(
            greaterEqual13 =
            JSONParserUtilities.getURIOrNull(container, "greaterEqual13"),
            under13 =
            JSONParserUtilities.getURIOrNull(container, "under13")
          )
        }
        else -> {
          this.logger.warn("encountered unrecognized authentication type: {}", authType)
          return null
        }
      }
    } else {
      null
    }
  }

  /**
   * Parse the keyboard type from the given string. Note that the cases containing spaces
   * here are for compatibility with old cached values that may be present on devices. Ideally
   * the cases containing spaces will never be encountered in practice, but we can't be
   * certain they won't.
   */

  private fun parseKeyboardType(
    text: String?
  ): KeyboardInput {
    return when (val keyboardText = text?.toUpperCase(Locale.ROOT)) {
      null ->
        KeyboardInput.DEFAULT
      "NO_INPUT",
      "NO INPUT" ->
        KeyboardInput.NO_INPUT
      "DEFAULT" ->
        KeyboardInput.DEFAULT
      "NUMBER_PAD",
      "NUMBER PAD" ->
        KeyboardInput.NUMBER_PAD
      "EMAIL_ADDRESS",
      "EMAIL ADDRESS" ->
        KeyboardInput.EMAIL_ADDRESS
      else -> {
        this.logger.warn("encountered unrecognized keyboard type: {}", keyboardText)
        return KeyboardInput.DEFAULT
      }
    }
  }

  private fun toStringMap(objectNode: ObjectNode): Map<String, String> {
    val map = mutableMapOf<String, String>()
    for (key in objectNode.fieldNames()) {
      map[key.toUpperCase()] = JSONParserUtilities.getString(objectNode, key)
    }
    return map.toMap()
  }

  /**
   * Deserialize a set of account providers from the given JSON array node.
   *
   * @param jom A JSON object mapper
   * @param node A JSON node
   * @return A parsed account provider collection
   * @throws JSONParseException On parse errors
   */

  @Throws(JSONParseException::class)
  fun deserializeCollectionFromJSONArray(node: ArrayNode): Map<URI, AccountProvider> {

    val providers = TreeMap<URI, AccountProvider>()
    var default_provider: AccountProvider? = null

    var ex: JSONParseException? = null
    for (index in 0 until node.size()) {
      try {
        val provider = this.deserializeFromJSON(node.get(index))
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
  fun deserializeCollectionFromStream(stream: InputStream): Map<URI, AccountProvider> {
    val jom = ObjectMapper()
    val node = this.mapNullToTextNode(jom.readTree(stream))
    return this.deserializeCollectionFromJSONArray(JSONParserUtilities.checkArray(null, node))
  }

  /**
   * Deserialize a single account provider from the given stream.
   *
   * @param stream An input stream
   * @return A parsed account provider
   * @throws IOException On I/O or parser errors
   */

  @Throws(IOException::class)
  fun deserializeOneFromStream(stream: InputStream): AccountProvider {
    val jom = ObjectMapper()
    val node = this.mapNullToTextNode(jom.readTree(stream))
    return this.deserializeFromJSON(JSONParserUtilities.checkObject(null, node))
  }

  /**
   * Deserialize a single account provider from the given file.
   *
   * @param stream An input stream
   * @return A parsed account provider
   * @throws IOException On I/O or parser errors
   */

  @Throws(IOException::class)
  fun deserializeOneFromFile(file: File): AccountProvider =
    FileInputStream(file).use { stream -> this.deserializeOneFromStream(stream) }
}
