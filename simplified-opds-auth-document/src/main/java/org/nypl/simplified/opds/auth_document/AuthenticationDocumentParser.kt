package org.nypl.simplified.opds.auth_document

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.mime.MIMEParser
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocument
import org.nypl.simplified.opds.auth_document.api.AuthenticationDocumentParserType
import org.nypl.simplified.opds.auth_document.api.AuthenticationObject
import org.nypl.simplified.opds.auth_document.api.AuthenticationObjectLink
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
      this.errors.add(ParseError(
        source = this.uri,
        message = warning.message,
        line = 0,
        column = 0,
        exception = warning.exception
      ))
    } else {
      this.warnings.add(warning)
    }
  }

  private fun publishErrorForException(e: Exception) {
    this.errors.add(ParseError(
      source = this.uri,
      message = e.message ?: "",
      exception = e
    ))
  }

  private fun publishErrorForString(message: String) {
    this.errors.add(ParseError(
      source = this.uri,
      message = message,
      exception = null
    ))
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
      val authentication =
        this.parseAuthentications(root)
      val links =
        this.parseLinks(root)

      if (this.errors.isEmpty()) {
        return ParseResult.Success(
          warnings = this.warnings.toList(),
          result = AuthenticationDocument(
            id = id,
            title = title,
            description = description,
            links = links,
            authentication = authentication))
      } else {
        ParseResult.Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
      }
    } catch (e: Exception) {
      this.publishErrorForException(e)
      ParseResult.Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
    }
  }

  private fun parseLinks(tree: ObjectNode): List<AuthenticationObjectLink> {
    if (!tree.has("links")) {
      return listOf()
    }

    val linksNodes = try {
      JSONParserUtilities.getArray(tree, "links")
    } catch (e: Exception) {
      this.publishErrorForException(e)
      this.mapper.createArrayNode()
    }

    return linksNodes.mapNotNull { node -> this.parseLink(node) }
  }

  private fun parseLink(node: JsonNode): AuthenticationObjectLink? {
    return try {
      val root =
        JSONParserUtilities.checkObject(null, node)
      val href =
        JSONParserUtilities.getURI(root, "href")
      val templated =
        JSONParserUtilities.getBooleanDefault(root, "templated", false)
      val mime =
        JSONParserUtilities.getStringOrNull(root, "type")
          ?.let { type -> MIMEParser.parseRaisingException(type) }
      val title =
        JSONParserUtilities.getStringOrNull(root, "title")
      val rel =
        JSONParserUtilities.getStringOrNull(root, "rel")
      val width =
        JSONParserUtilities.getIntegerOrNull(root, "width")
      val height =
        JSONParserUtilities.getIntegerOrNull(root, "height")
      val duration =
        JSONParserUtilities.getStringOrNull(root, "duration")?.toDouble()
      val bitrate =
        JSONParserUtilities.getStringOrNull(root, "bitrate")?.toDouble()

      AuthenticationObjectLink(
        href = href,
        templated = templated,
        type = mime,
        title = title,
        rel = rel,
        width = width,
        height = height,
        duration = duration,
        bitrate = bitrate)
    } catch (e: Exception) {
      this.publishErrorForException(e)
      null
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
      val links =
        JSONParserUtilities.getArrayOrNull(root, "links")
          ?.mapNotNull(this::parseLink)
          ?: listOf()
      val labels =
        JSONParserUtilities.getObjectOrNull(root, "labels")
          ?.let(this::parseLabels)
          ?: mapOf()
      AuthenticationObject(
        type = type,
        labels = labels,
        links = links)
    } catch (e: Exception) {
      this.publishErrorForException(e)
      null
    }
  }

  private fun parseLabels(root: ObjectNode): Map<String, String>? {
    val values = mutableMapOf<String, String>()
    for (key in root.fieldNames()) {
      try {
        values[key] = JSONParserUtilities.getString(root, key)
      } catch (e: Exception) {
        this.publishErrorForException(e)
        null
      }
    }
    return values.toMap()
  }
}