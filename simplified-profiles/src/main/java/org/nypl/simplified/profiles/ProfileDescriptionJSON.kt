package org.nypl.simplified.profiles

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.io7m.jfunctional.None
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.OptionVisitorType
import com.io7m.jfunctional.Some
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormatterBuilder
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.files.FileUtilities
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.json.core.JSONSerializerUtilities
import org.nypl.simplified.profiles.api.ProfileAttributes
import org.nypl.simplified.profiles.api.ProfileDateOfBirth
import org.nypl.simplified.profiles.api.ProfileDescription
import org.nypl.simplified.profiles.api.ProfilePreferences
import org.nypl.simplified.reader.api.ReaderPreferences
import org.nypl.simplified.reader.api.ReaderPreferencesJSON
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Functions to serialize and deserialize profile preferences to/from JSON.
 */

object ProfileDescriptionJSON {

  private fun standardDateFormatter(): DateTimeFormatter {
    return DateTimeFormatterBuilder()
      .appendYear(4, 5)
      .appendLiteral("-")
      .appendMonthOfYear(2)
      .appendLiteral("-")
      .appendDayOfMonth(2)
      .toFormatter()
  }

  /**
   * Deserialize profile preferences from the given file.
   *
   * @param jom A JSON object mapper
   * @param file A file
   * @return A parsed description
   * @throws IOException On I/O and/or parse errors
   */

  @Throws(IOException::class)
  fun deserializeFromFile(
    jom: ObjectMapper,
    file: File
  ): ProfileDescription {
    return this.deserializeFromText(jom, FileUtilities.fileReadUTF8(file))
  }

  /**
   * Deserialize profile preferences from the given text.
   *
   * @param jom A JSON object mapper
   * @param text A JSON string
   * @return A parsed description
   * @throws IOException On I/O and/or parse errors
   */

  @Throws(IOException::class)
  fun deserializeFromText(
    jom: ObjectMapper,
    text: String
  ): ProfileDescription {
    return this.deserializeFromJSON(jom, jom.readTree(text))
  }

  /**
   * Deserialize profile preferences from the given JSON node.
   *
   * @param objectMapper A JSON object mapper
   * @param node A JSON node
   * @return A parsed description
   * @throws JSONParseException On parse errors
   */

  @Throws(JSONParseException::class)
  fun deserializeFromJSON(
    objectMapper: ObjectMapper,
    node: JsonNode
  ): ProfileDescription {
    val obj =
      JSONParserUtilities.checkObject(null, node)
    val version =
      JSONParserUtilities.getIntegerOrNull(obj, "@version")

    return when (version) {
      null -> this.deserializeFromJSONUnversioned(objectMapper, obj)
      20191201 -> this.deserialize20191201(objectMapper, obj)
      20200504 -> this.deserialize20200504(objectMapper, obj)
      else -> throw JSONParseException("Unsupported profile format version: $version")
    }
  }

  private fun deserialize20200504(
    objectMapper: ObjectMapper,
    objectNode: ObjectNode
  ): ProfileDescription {
    val displayName =
      JSONParserUtilities.getString(objectNode, "displayName")
    val preferences =
      deserialize20200504Preferences(
        objectMapper,
        JSONParserUtilities.getObject(objectNode, "preferences")
      )
    val attributes =
      deserialize20191201Attributes(JSONParserUtilities.getObject(objectNode, "attributes"))

    return ProfileDescription(
      displayName = displayName,
      preferences = preferences,
      attributes = attributes
    )
  }

  private fun deserialize20191201(
    objectMapper: ObjectMapper,
    objectNode: ObjectNode
  ): ProfileDescription {
    val displayName =
      JSONParserUtilities.getString(objectNode, "displayName")
    val preferences =
      deserialize20191201Preferences(
        objectMapper,
        JSONParserUtilities.getObject(objectNode, "preferences")
      )
    val attributes =
      deserialize20191201Attributes(JSONParserUtilities.getObject(objectNode, "attributes"))

    return ProfileDescription(
      displayName = displayName,
      preferences = preferences,
      attributes = attributes
    )
  }

  private fun deserialize20191201Attributes(
    objectNode: ObjectNode
  ): ProfileAttributes {
    val attributes = mutableMapOf<String, String>()
    for (key in objectNode.fieldNames()) {
      attributes[key] = objectNode.get(key).asText()
    }
    return ProfileAttributes(attributes.toSortedMap())
  }

  private fun deserialize20200504Preferences(
    objectMapper: ObjectMapper,
    objectNode: ObjectNode
  ): ProfilePreferences {
    val dateFormatter =
      this.standardDateFormatter()

    val dateOfBirth =
      JSONParserUtilities.getObjectOrNull(objectNode, "dateOfBirth")
        ?.let { node ->
          ProfileDateOfBirth(
            date = dateFormatter.parseDateTime(JSONParserUtilities.getString(node, "date")),
            isSynthesized = JSONParserUtilities.getBoolean(node, "isSynthesized")
          )
        }

    val showTestingLibraries =
      JSONParserUtilities.getBoolean(objectNode, "showTestingLibraries")

    val hasSeenLibrarySelectionScreen =
      JSONParserUtilities.getBooleanDefault(objectNode, "hasSeenLibrarySelectionScreen", true)

    val useExperimentalR2 =
      JSONParserUtilities.getBooleanDefault(objectNode, "useExperimentalR2", false)

    val showDebugSettings =
      JSONParserUtilities.getBooleanDefault(objectNode, "showDebugSettings", false)

    val readerPreferences =
      deserializeReaderPreferences(objectMapper, objectNode)

    val mostRecentAccount =
      JSONParserUtilities.getStringOrNull(objectNode, "mostRecentAccount")
        ?.let { AccountID(UUID.fromString(it)) }

    return ProfilePreferences(
      dateOfBirth = dateOfBirth,
      showTestingLibraries = showTestingLibraries,
      readerPreferences = readerPreferences,
      mostRecentAccount = mostRecentAccount,
      hasSeenLibrarySelectionScreen = hasSeenLibrarySelectionScreen,
      useExperimentalR2 = useExperimentalR2,
      showDebugSettings = showDebugSettings
    )
  }

  private fun deserialize20191201Preferences(
    objectMapper: ObjectMapper,
    objectNode: ObjectNode
  ): ProfilePreferences {
    val dateFormatter =
      this.standardDateFormatter()

    val dateOfBirth =
      JSONParserUtilities.getObjectOrNull(objectNode, "dateOfBirth")
        ?.let { node ->
          ProfileDateOfBirth(
            date = dateFormatter.parseDateTime(JSONParserUtilities.getString(node, "date")),
            isSynthesized = JSONParserUtilities.getBoolean(node, "isSynthesized")
          )
        }

    val showTestingLibraries =
      JSONParserUtilities.getBoolean(objectNode, "showTestingLibraries")

    val readerPreferences =
      deserializeReaderPreferences(objectMapper, objectNode)

    val mostRecentAccount =
      JSONParserUtilities.getStringOrNull(objectNode, "mostRecentAccount")
        ?.let { AccountID(UUID.fromString(it)) }

    return ProfilePreferences(
      dateOfBirth = dateOfBirth,
      showTestingLibraries = showTestingLibraries,
      readerPreferences = readerPreferences,
      mostRecentAccount = mostRecentAccount,
      hasSeenLibrarySelectionScreen = true
    )
  }

  private fun deserializeFromJSONUnversioned(
    objectMapper: ObjectMapper,
    objectNode: ObjectNode
  ): ProfileDescription {
    val displayName =
      JSONParserUtilities.getString(objectNode, "display_name")

    val dateFormatter =
      this.standardDateFormatter()

    val preferencesNode =
      JSONParserUtilities.getObject(objectNode, "preferences")

    val gender =
      JSONParserUtilities.getStringOrNull(preferencesNode, "gender")
    val role =
      JSONParserUtilities.getStringOrNull(preferencesNode, "role")
    val school =
      JSONParserUtilities.getStringOrNull(preferencesNode, "school")
    val grade =
      JSONParserUtilities.getStringOrNull(preferencesNode, "grade")

    val showTestingLibraries =
      JSONParserUtilities.getBooleanDefault(preferencesNode, "show-testing-libraries", false)

    val dateOfBirthDate =
      JSONParserUtilities.getStringOptional(preferencesNode, "date-of-birth")
        .mapPartial<DateTime, JSONParseException> { text ->
          return@mapPartial try {
            dateFormatter.parseDateTime(text)
          } catch (e: IllegalArgumentException) {
            throw JSONParseException(e)
          }
        }

    val dateOfBirthSynthesized =
      JSONParserUtilities.getBooleanDefault(preferencesNode, "date-of-birth-synthesized", false)

    val dateOfBirth =
      dateOfBirthDate.map { dateValue ->
        ProfileDateOfBirth(dateValue, dateOfBirthSynthesized)
      }

    val readerPrefs =
      deserializeReaderPreferences(objectMapper, preferencesNode)

    val preferences =
      ProfilePreferences(
        dateOfBirth = this.someOrNull(dateOfBirth),
        showTestingLibraries = showTestingLibraries,
        readerPreferences = readerPrefs,
        mostRecentAccount = null,
        hasSeenLibrarySelectionScreen = true
      )

    val attributeMap = mutableMapOf<String, String>()
    gender?.let { attributeMap[ProfileAttributes.GENDER_ATTRIBUTE_KEY] = it }
    role?.let { attributeMap[ProfileAttributes.ROLE_ATTRIBUTE_KEY] = it }
    grade?.let { attributeMap[ProfileAttributes.GRADE_ATTRIBUTE_KEY] = it }
    school?.let { attributeMap[ProfileAttributes.SCHOOL_ATTRIBUTE_KEY] = it }

    val attributes =
      ProfileAttributes(attributeMap.toSortedMap())

    return ProfileDescription(
      displayName = displayName,
      preferences = preferences,
      attributes = attributes
    )
  }

  private fun deserializeReaderPreferences(
    objectMapper: ObjectMapper,
    node: ObjectNode?
  ): ReaderPreferences {
    return JSONParserUtilities.getObjectOptional(node, "readerPreferences")
      .mapPartial<ReaderPreferences, JSONParseException> { prefsNode ->
        ReaderPreferencesJSON.deserializeFromJSON(objectMapper, prefsNode)
      }
      .accept(object : OptionVisitorType<ReaderPreferences, ReaderPreferences> {
        override fun none(none: None<ReaderPreferences>): ReaderPreferences {
          return ReaderPreferences.builder().build()
        }

        override fun some(some: Some<ReaderPreferences>): ReaderPreferences {
          return some.get()
        }
      })
  }

  private fun <T> someOrNull(opt: OptionType<T>?): T? {
    return if (opt is Some) {
      opt.get()
    } else {
      null
    }
  }

  /**
   * Serialize profile preferences to JSON.
   *
   * @param objectMapper A JSON object mapper
   * @return A serialized object
   */

  fun serializeToJSON(
    objectMapper: ObjectMapper,
    description: ProfileDescription
  ): ObjectNode {
    val output = objectMapper.createObjectNode()
    serialize20200504(objectMapper, output, description)
    return output
  }

  private fun serialize20200504(
    objectMapper: ObjectMapper,
    output: ObjectNode,
    description: ProfileDescription
  ) {
    output.put("@version", 20200504)
    output.put("displayName", description.displayName)
    output.set<ObjectNode>(
      "preferences",
      serialize20200504Preferences(objectMapper, description.preferences)
    )
    output.set<ObjectNode>(
      "attributes",
      serialize20200504Attributes(objectMapper, description.attributes)
    )
  }

  private fun serialize20200504Attributes(
    objectMapper: ObjectMapper,
    attributes: ProfileAttributes
  ): ObjectNode {
    val output = objectMapper.createObjectNode()
    for (key in attributes.attributes.keys) {
      val value = attributes.attributes[key]!!
      output.put(key, value)
    }
    return output
  }

  private fun serialize20200504Preferences(
    objectMapper: ObjectMapper,
    preferences: ProfilePreferences
  ): ObjectNode {
    val output = objectMapper.createObjectNode()
    output.put("showTestingLibraries", preferences.showTestingLibraries)
    output.put("hasSeenLibrarySelectionScreen", preferences.hasSeenLibrarySelectionScreen)
    output.put("useExperimentalR2", preferences.useExperimentalR2)
    output.put("showDebugSettings", preferences.showDebugSettings)

    val mostRecentAccount = preferences.mostRecentAccount
    if (mostRecentAccount != null) {
      output.put("mostRecentAccount", mostRecentAccount.uuid.toString())
    }

    output.set<ObjectNode>(
      "readerPreferences",
      ReaderPreferencesJSON.serializeToJSON(objectMapper, preferences.readerPreferences)
    )

    val dateOfBirth = preferences.dateOfBirth
    if (dateOfBirth != null) {
      val dateNode = objectMapper.createObjectNode()
      dateNode.put("date", this.standardDateFormatter().print(dateOfBirth.date))
      dateNode.put("isSynthesized", dateOfBirth.isSynthesized)
      output.set<ObjectNode>("dateOfBirth", dateNode)
    }

    return output
  }

  /**
   * Serialize profile preferences to a JSON string.
   *
   * @param objectMapper A JSON object mapper
   * @return A JSON string
   * @throws IOException On serialization errors
   */

  @Throws(IOException::class)
  fun serializeToString(
    objectMapper: ObjectMapper,
    description: ProfileDescription
  ): String {
    val jo = this.serializeToJSON(objectMapper, description)
    val bao = ByteArrayOutputStream(1024)
    JSONSerializerUtilities.serialize(jo, bao)
    return bao.toString("UTF-8")
  }
}
