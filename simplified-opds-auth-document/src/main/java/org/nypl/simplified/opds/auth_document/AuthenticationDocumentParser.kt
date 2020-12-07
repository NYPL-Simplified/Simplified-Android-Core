package org.nypl.simplified.opds.auth_document

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.announcements.Announcement
import org.nypl.simplified.announcements.AnnouncementJSON
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.links.Link
import org.nypl.simplified.links.json.LinkParsing
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocument
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParserType
import org.nypl.simplified.opds.auth_document.api.AuthenticationObject
import org.nypl.simplified.opds.auth_document.api.AuthenticationObjectNYPLFeatures
import org.nypl.simplified.opds.auth_document.api.AuthenticationObjectNYPLInput
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseWarning
import java.io.InputStream
import java.net.URI

internal class AuthenticationDocumentParser(
  private val mapper: ObjectMapper,
  private val uri: URI,
  private val stream: InputStream,
  private val warningsAsErrors: Boolean
) : AuthenticationDocumentParserType {

  override fun close() {
    this.stream.close()
  }

  private val errors =
    mutableListOf<ParseError>()
  private val warnings =
    mutableListOf<ParseWarning>()

  private fun publishWarning(warning: ParseWarning) {
    if (this.warningsAsErrors) {
      this.errors.add(
        ParseError(
          source = this.uri,
          message = warning.message,
          line = 0,
          column = 0,
          exception = warning.exception
        )
      )
    } else {
      this.warnings.add(warning)
    }
  }

  private fun publishErrorForException(e: Exception) {
    this.errors.add(
      ParseError(
        source = this.uri,
        message = e.message ?: "",
        exception = e
      )
    )
  }

  private fun publishErrorForString(message: String) {
    this.errors.add(
      ParseError(
        source = this.uri,
        message = message,
        exception = null
      )
    )
  }

  override fun parse(): ParseResult<AuthenticationDocument> {
    return try {
      val tree = this.mapper.readTree(this.stream)
      if (tree == null) {
        this.publishErrorForString("Document is empty")
        return ParseResult.Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
      }

      val root =
        JSONParserUtilities.checkObject(null, tree)
      val id =
        JSONParserUtilities.getURI(root, "id")
      val title =
        JSONParserUtilities.getString(root, "title")
      val description =
        JSONParserUtilities.getStringOrNull(root, "description")
      val mainColor =
        JSONParserUtilities.getStringOrNull(root, "color_scheme") ?: "red"
      val features =
        JSONParserUtilities.getObjectOrNull(root, "features")
          ?.let { obj -> parseFeatures(obj) }
          ?: AuthenticationObjectNYPLFeatures(setOf(), setOf())

      val announcements =
        this.parseAnnouncements(root)
      val authentication =
        this.parseAuthentications(root)
      val links =
        this.parseLinks(root)

      if (this.errors.isEmpty()) {
        return ParseResult.Success(
          warnings = this.warnings.toList(),
          result = AuthenticationDocument(
            authentication = authentication,
            description = description,
            features = features,
            id = id,
            links = links,
            mainColor = mainColor,
            title = title,
            announcements = announcements
          )
        )
      } else {
        ParseResult.Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
      }
    } catch (e: Exception) {
      this.publishErrorForException(e)
      ParseResult.Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
    }
  }

  private fun parseAnnouncements(root: ObjectNode): List<Announcement> {
    return try {
      val announcements = JSONParserUtilities.getArrayOrNull(root, "announcements")
      if (announcements != null) {
        val results = mutableListOf<Announcement>()
        for (announcement in announcements) {
          try {
            results.add(AnnouncementJSON.deserializeFromJSON(announcement))
          } catch (e: Exception) {
            this.publishErrorForException(e)
          }
        }
        results.toList()
      } else {
        listOf()
      }
    } catch (e: java.lang.Exception) {
      this.publishErrorForException(e)
      emptyList()
    }
  }

  private fun parseFeatures(obj: ObjectNode): AuthenticationObjectNYPLFeatures {
    return try {
      val enabled =
        JSONParserUtilities.getArrayOrNull(obj, "enabled")
          ?.map { node -> JSONParserUtilities.checkString(node) }
          ?: setOf<String>()

      val disabled =
        JSONParserUtilities.getArrayOrNull(obj, "disabled")
          ?.map { node -> JSONParserUtilities.checkString(node) }
          ?: setOf<String>()

      AuthenticationObjectNYPLFeatures(
        enabled = enabled.toSet(),
        disabled = disabled.toSet()
      )
    } catch (e: Exception) {
      this.publishErrorForException(e)
      AuthenticationObjectNYPLFeatures(setOf(), setOf())
    }
  }

  private fun parseLinks(tree: ObjectNode): List<Link> {
    if (!tree.has("links")) {
      return listOf()
    }

    val linksNodes = try {
      JSONParserUtilities.getArray(tree, "links")
    } catch (e: Exception) {
      this.publishErrorForException(e)
      this.mapper.createArrayNode()
    }

    return parseLinksArray(linksNodes)
  }

  private fun parseLinksArray(linksNodes: ArrayNode?): List<Link> {
    if (linksNodes == null) {
      return listOf()
    }

    return linksNodes.mapNotNull { node -> LinkParsing.parseLink(this.uri, node) }
      .mapNotNull { result ->
        when (result) {
          is ParseResult.Success -> {
            result.warnings.forEach { warn -> this.publishWarning(warn) }
            result.result
          }
          is ParseResult.Failure -> {
            result.warnings.forEach { warn -> this.publishWarning(warn) }
            result.errors.forEach { error -> this.publishWarning(error.toWarning()) }
            null
          }
        }
      }
  }

  private fun parseAuthentications(tree: ObjectNode): List<AuthenticationObject> {
    val authenticationNodes = try {
      JSONParserUtilities.getArray(tree, "authentication")
    } catch (e: Exception) {
      this.publishErrorForException(e)
      this.mapper.createArrayNode()
    }

    return authenticationNodes.mapNotNull { node -> this.parseAuthentication(node) }
  }

  private fun parseAuthentication(node: JsonNode): AuthenticationObject? {
    return try {
      val root =
        JSONParserUtilities.checkObject(null, node)
      val type =
        JSONParserUtilities.getURI(root, "type")
      val description =
        JSONParserUtilities.getStringOrNull(root, "description") ?: ""

      val links =
        parseLinksArray(JSONParserUtilities.getArrayOrNull(root, "links"))

      val labels =
        JSONParserUtilities.getObjectOrNull(root, "labels")
          ?.let(this::parseLabels)
          ?: mapOf()

      val inputs =
        JSONParserUtilities.getObjectOrNull(root, "inputs")
          ?.let(this::parseInputs)
          ?: mapOf()

      AuthenticationObject(
        description = description,
        inputs = inputs,
        labels = labels,
        links = links,
        type = type
      )
    } catch (e: Exception) {
      this.publishErrorForException(e)
      null
    }
  }

  private fun parseInputs(root: ObjectNode): Map<String, AuthenticationObjectNYPLInput>? {
    val values = mutableMapOf<String, AuthenticationObjectNYPLInput>()
    for (key in root.fieldNames()) {
      try {
        val keyUpper = key.toUpperCase()
        val input = this.parseInput(keyUpper, JSONParserUtilities.getObject(root, key))
        if (input != null) {
          values[keyUpper] = input
        }
      } catch (e: Exception) {
        this.publishErrorForException(e)
      }
    }
    return values.toMap()
  }

  private fun parseInput(
    fieldName: String,
    root: ObjectNode?
  ): AuthenticationObjectNYPLInput? {
    return try {
      AuthenticationObjectNYPLInput(
        fieldName = fieldName,
        keyboardType =
        JSONParserUtilities.getStringOrNull(root, "keyboard")?.toUpperCase(),
        maximumLength =
        JSONParserUtilities.getIntegerDefault(root, "maximum_length", 0),
        barcodeFormat =
        JSONParserUtilities.getStringOrNull(root, "barcode_format")?.toUpperCase()
      )
    } catch (e: Exception) {
      this.publishErrorForException(e)
      null
    }
  }

  private fun parseLabels(root: ObjectNode): Map<String, String>? {
    val values = mutableMapOf<String, String>()
    for (key in root.fieldNames()) {
      try {
        values[key.toUpperCase()] = JSONParserUtilities.getString(root, key)
      } catch (e: Exception) {
        this.publishErrorForException(e)
      }
    }
    return values.toMap()
  }
}
