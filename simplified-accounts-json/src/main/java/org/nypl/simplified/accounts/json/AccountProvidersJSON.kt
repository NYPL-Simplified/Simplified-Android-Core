package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.joda.time.DateTime
import org.nypl.simplified.accounts.api.AccountDistance
import org.nypl.simplified.accounts.api.AccountDistanceUnit
import org.nypl.simplified.accounts.api.AccountGeoLocation
import org.nypl.simplified.accounts.api.AccountLibraryLocation
import org.nypl.simplified.accounts.api.AccountProvider
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Anonymous
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Basic
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.COPPAAgeGate
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Companion.ANONYMOUS_TYPE
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Companion.BASIC_TYPE
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Companion.COPPA_TYPE
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Companion.OAUTH_INTERMEDIARY_TYPE
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.Companion.SAML_2_0_TYPE
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.KeyboardInput
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.OAuthWithIntermediary
import org.nypl.simplified.accounts.api.AccountProviderAuthenticationDescription.SAML2_0
import org.nypl.simplified.accounts.api.AccountProviderType
import org.nypl.simplified.announcements.Announcement
import org.nypl.simplified.announcements.AnnouncementJSON
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

    node.put("@version", "20200527")
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

    node.set<ObjectNode>(
      "authentication",
      this.serializeAuthentication(mapper, provider.authentication)
    )
    node.set<ObjectNode>(
      "announcements",
      this.serializeAnnouncements(mapper, provider.announcements)
    )
    node.set<ArrayNode>(
      "authenticationAlternatives",
      this.serializeAuthenticationAlternatives(mapper, provider.authenticationAlternatives)
    )
    return node
  }

  private fun serializeAnnouncements(
    mapper: ObjectMapper,
    announcements: List<Announcement>
  ): JsonNode {
    val array = mapper.createArrayNode()
    for (announcement in announcements) {
      array.add(AnnouncementJSON.serializeToJSON(mapper, announcement))
    }
    return array
  }

  private fun serializeAuthenticationAlternatives(
    mapper: ObjectMapper,
    authenticationAlternatives: List<AccountProviderAuthenticationDescription>
  ): ArrayNode {
    val array = mapper.createArrayNode()
    for (authentication in authenticationAlternatives) {
      array.add(this.serializeAuthentication(mapper, authentication))
    }
    return array
  }

  private fun serializeAuthentication(
    mapper: ObjectMapper,
    authentication: AccountProviderAuthenticationDescription
  ): ObjectNode {
    return when (authentication) {
      is COPPAAgeGate -> {
        val authObject = mapper.createObjectNode()
        authObject.put("type", COPPA_TYPE)
        authObject.put("greaterEqual13", authentication.greaterEqual13.toString())
        authObject.put("under13", authentication.under13.toString())
        authObject
      }
      is OAuthWithIntermediary -> {
        val authObject = mapper.createObjectNode()
        authObject.put("description", authentication.description)
        authObject.put("type", OAUTH_INTERMEDIARY_TYPE)
        authObject.put("authenticate", authentication.authenticate.toString())
        val logo = authentication.logoURI
        if (logo != null) {
          authObject.put("logo", logo.toString())
        }
        authObject
      }
      is Basic -> {
        val authObject = mapper.createObjectNode()
        authObject.put("type", BASIC_TYPE)
        this.putConditionally(authObject, "barcodeFormat", authentication.barcodeFormat?.toUpperCase(Locale.ROOT))
        this.putConditionally(authObject, "description", authentication.description)
        this.putConditionally(authObject, "keyboard", authentication.keyboard.name)
        this.putConditionally(authObject, "passwordKeyboard", authentication.passwordKeyboard.name)
        authObject.put("passwordMaximumLength", authentication.passwordMaximumLength)
        authObject.set<ObjectNode>("labels", this.mapToObject(mapper, authentication.labels))
        val logo = authentication.logoURI
        if (logo != null) {
          authObject.put("logo", logo.toString())
        }
        authObject
      }
      is Anonymous -> {
        val authObject = mapper.createObjectNode()
        authObject.put("type", ANONYMOUS_TYPE)
        authObject
      }
      is SAML2_0 -> {
        val authObject = mapper.createObjectNode()
        authObject.put("description", authentication.description)
        authObject.put("type", SAML_2_0_TYPE)
        authObject.put("authenticate", authentication.authenticate.toString())
        val logo = authentication.logoURI
        if (logo != null) {
          authObject.put("logo", logo.toString())
        }
        authObject
      }
    }
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
      val announcements =
        this.parseAnnouncements(obj)
      val authentication =
        this.parseAuthentication(obj)
      val authenticationAlternatives =
        this.parseAuthenticationAlternatives(obj)
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
      val isProduction =
        JSONParserUtilities.getBooleanDefault(obj, "isProduction", false)
      val idNumeric =
        JSONParserUtilities.getIntegerDefault(obj, "idNumeric", -1)

      val updated =
        JSONParserUtilities.getStringOrNull(obj, "updated")
          ?.let { text -> DateTime.parse(text) }
          ?: DateTime.now()

      val location: AccountLibraryLocation? =
        JSONParserUtilities.getObjectOrNull(obj, "location")
          ?.let(this::parseLocation)

      return AccountProvider(
        addAutomatically = addAutomatically,
        annotationsURI = annotationsURI,
        announcements = announcements,
        authentication = authentication,
        authenticationAlternatives = authenticationAlternatives,
        authenticationDocumentURI = authenticationDocumentURI,
        cardCreatorURI = cardCreatorURI,
        catalogURI = catalogURI,
        displayName = displayName,
        eula = eula,
        id = idUUID,
        idNumeric = idNumeric,
        isProduction = isProduction,
        license = license,
        loansURI = loansURI,
        logo = logo,
        mainColor = mainColor,
        patronSettingsURI = patronSettingsURI,
        privacyPolicy = privacyPolicy,
        subtitle = subtitle,
        supportEmail = supportEmail,
        supportsReservations = supportsReservations,
        updated = updated,
        location = location
      )
    } catch (e: JSONParseException) {
      throw JSONParseException("Unable to parse provider $idUUID", e)
    }
  }

  private fun parseLocation(
    obj: ObjectNode
  ): AccountLibraryLocation {
    val distanceObj =
      JSONParserUtilities.getObjectOrNull(obj, "distance")

    val distance =
      if (distanceObj != null) {
        val distanceLength =
          JSONParserUtilities.getDouble(distanceObj, "length")
        val distanceUnit =
          JSONParserUtilities.getString(distanceObj, "unit")
        AccountDistance(
          length = distanceLength,
          unit = AccountDistanceUnit.valueOf(distanceUnit)
        )
      } else {
        null
      }

    val latLong =
      JSONParserUtilities.getObjectOrNull(obj, "latitudeLongitude")
    if (latLong != null) {
      val latitude =
        JSONParserUtilities.getDouble(latLong, "latitude")
      val longitude =
        JSONParserUtilities.getDouble(latLong, "longitude")

      return AccountLibraryLocation(
        location = AccountGeoLocation.Coordinates(longitude, latitude),
        distance = distance
      )
    }

    throw JSONParseException("No recognized location type")
  }

  private fun parseAnnouncements(
    obj: ObjectNode
  ): List<Announcement> {
    return if (obj.has("announcements")) {
      val array = JSONParserUtilities.getArray(obj, "announcements")
      val items = mutableListOf<Announcement>()
      for (node in array) {
        try {
          items.add(AnnouncementJSON.deserializeFromJSON(node))
        } catch (e: Exception) {
          this.logger.error("unable to parse announcement: ", e)
        }
      }
      items.toList()
    } else {
      listOf()
    }
  }

  private fun parseAuthenticationAlternatives(
    obj: ObjectNode
  ): List<AccountProviderAuthenticationDescription> {
    return if (obj.has("authenticationAlternatives")) {
      val objAlt = JSONParserUtilities.getArray(obj, "authenticationAlternatives")
      val items = mutableListOf<AccountProviderAuthenticationDescription>()
      for (authObj in objAlt) {
        if (authObj is ObjectNode) {
          items.add(this.parseAuthenticationFromObject(authObj))
        }
      }
      items.toList()
    } else {
      listOf()
    }
  }

  private fun parseAuthentication(
    obj: ObjectNode
  ): AccountProviderAuthenticationDescription {
    return if (obj.has("authentication")) {
      this.parseAuthenticationFromObject(JSONParserUtilities.getObject(obj, "authentication"))
    } else {
      Anonymous
    }
  }

  private fun parseAuthenticationFromObject(
    container: ObjectNode
  ): AccountProviderAuthenticationDescription {
    return when (val authType = JSONParserUtilities.getString(container, "type")) {
      SAML_2_0_TYPE -> {
        val authURI =
          JSONParserUtilities.getURI(container, "authenticate")
        val logoURI =
          JSONParserUtilities.getURIOrNull(container, "logo")
        val description =
          JSONParserUtilities.getStringOrNull(container, "description") ?: ""

        SAML2_0(
          authenticate = authURI,
          description = description,
          logoURI = logoURI
        )
      }

      OAUTH_INTERMEDIARY_TYPE -> {
        val authURI =
          JSONParserUtilities.getURI(container, "authenticate")
        val logoURI =
          JSONParserUtilities.getURIOrNull(container, "logo")
        val description =
          JSONParserUtilities.getStringOrNull(container, "description") ?: ""

        OAuthWithIntermediary(
          authenticate = authURI,
          description = description,
          logoURI = logoURI
        )
      }

      BASIC_TYPE -> {
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
        val logoURI =
          JSONParserUtilities.getURIOrNull(container, "logo")

        Basic(
          barcodeFormat = barcodeFormat,
          description = description,
          keyboard = keyboard,
          labels = labels,
          logoURI = logoURI,
          passwordKeyboard = passwordKeyboard,
          passwordMaximumLength = passwordMaximumLength
        )
      }
      COPPA_TYPE -> {
        COPPAAgeGate(
          greaterEqual13 =
            JSONParserUtilities.getURIOrNull(container, "greaterEqual13"),
          under13 =
            JSONParserUtilities.getURIOrNull(container, "under13")
        )
      }
      else -> {
        this.logger.warn("encountered unrecognized authentication type: {}", authType)
        Anonymous
      }
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
