package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.nypl.simplified.accounts.api.AccountProviderDescriptionMetadata
import org.nypl.simplified.accounts.api.AccountProviderDescriptionParserType
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseWarning
import java.net.URI
import java.net.URISyntaxException

class AccountProviderDescriptionParser internal constructor(
  private val uri: URI,
  private val objectNode: () -> ObjectNode,
  private val warningsAsErrors: Boolean) : AccountProviderDescriptionParserType {

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

  private data class Metadata(
    val updated: DateTime,
    val description: String,
    val id: URI,
    val title: String,
    val isAutomatic: Boolean,
    val isProduction: Boolean)

  override fun parse(): ParseResult<AccountProviderDescriptionMetadata> {
    return try {
      val root = this.objectNode.invoke()
      val metadata = this.parseMetadata(root)
      val links = this.parseLinks(root)
      val images = this.parseImages(root)

      if (this.errors.isEmpty()) {
        return ParseResult.Success(
          warnings = this.warnings.toList(),
          result = AccountProviderDescriptionMetadata(
            id = metadata.id,
            title = metadata.title,
            updated = metadata.updated,
            links = links,
            images = images,
            isAutomatic = metadata.isAutomatic,
            isProduction = metadata.isProduction))
      } else {
        ParseResult.Failure(
          warnings = this.warnings.toList(),
          errors = this.errors.toList())
      }
    } catch (e: Exception) {
      this.publishErrorForException(e)
      ParseResult.Failure(
        warnings = this.warnings.toList(),
        errors = this.errors.toList())
    }
  }

  override fun close() {

  }

  private fun parseMetadata(root: ObjectNode): Metadata {
    return try {
      val metadata = JSONParserUtilities.getObject(root, "metadata")
      Metadata(
        updated = parseMetadataUpdated(metadata),
        description = parseMetadataDescription(metadata),
        id = parseMetadataId(metadata),
        title = parseMetadataTitle(metadata),
        isProduction = JSONParserUtilities.getBooleanDefault(root, "isProduction", false),
        isAutomatic = JSONParserUtilities.getBooleanDefault(root, "isAutomatic", false))
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'updated' field as timestamp",
        exception = e))
      Metadata(
        updated = DateTime.now(),
        description = "",
        id = URI.create("urn:invalid"),
        title = "",
        isProduction = false,
        isAutomatic = false)
    }
  }

  private fun parseMetadataUpdated(root: ObjectNode): DateTime {
    return try {
      ISODateTimeFormat.dateTimeParser()
        .parseDateTime(JSONParserUtilities.getString(root, "updated"))
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'updated' field as timestamp",
        exception = e))
      return DateTime.now()
    }
  }

  private fun parseMetadataTitle(root: ObjectNode): String {
    return try {
      JSONParserUtilities.getString(root, "title")
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'title' field as string",
        exception = e))
      return ""
    }
  }

  private fun parseMetadataDescription(root: ObjectNode): String {
    return try {
      JSONParserUtilities.getStringOrNull(root, "description") ?: ""
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'description' field as string",
        exception = e))
      return ""
    }
  }

  private fun parseMetadataId(root: ObjectNode): URI {
    return try {
      JSONParserUtilities.getURI(root, "id")
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'id' field as URI",
        exception = e))
      return URI.create("urn:invalid")
    }
  }

  private fun parseImages(root: ObjectNode): List<AccountProviderDescriptionMetadata.Link> {
    return try {
      val results =
        mutableListOf<AccountProviderDescriptionMetadata.Link>()
      val arrayNode =
        JSONParserUtilities.getArrayOrNull(root, "images") ?: return listOf()
      for (arrayElement in arrayNode) {
        parseLink(arrayElement)?.let { node -> results.add(node) }
      }
      results
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'images' field as array",
        exception = e))
      listOf()
    }
  }

  private fun parseLink(element: JsonNode): AccountProviderDescriptionMetadata.Link? {
    return try {
      val objectNode = JSONParserUtilities.checkObject("", element)
      return AccountProviderDescriptionMetadata.Link(
        href = JSONParserUtilities.getURI(objectNode, "href"),
        type = JSONParserUtilities.getStringOrNull(objectNode, "type"),
        templated = JSONParserUtilities.getBooleanDefault(objectNode, "templated", false),
        relation = JSONParserUtilities.getStringOrNull(objectNode, "relation"))
    } catch (e: JSONParseException) {
      when (e.cause) {
        is URISyntaxException -> {
          this.publishWarning(ParseWarning(
            source = this.uri,
            message = "Could not parse 'link' object: Encountered an invalid URI in the feed",
            exception = e))
          null
        }
        else -> {
          this.errors.add(ParseError(
            source = this.uri,
            message = "Could not parse 'link' object",
            exception = e))
          null
        }
      }
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'link' object",
        exception = e))
      null
    }
  }

  private fun parseLinks(root: ObjectNode): List<AccountProviderDescriptionMetadata.Link> {
    return try {
      val results =
        mutableListOf<AccountProviderDescriptionMetadata.Link>()
      val arrayNode =
        JSONParserUtilities.getArray(root, "links") ?: return listOf()
      for (arrayElement in arrayNode) {
        parseLink(arrayElement)?.let { node -> results.add(node) }
      }
      results
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'links' field as array",
        exception = e))
      listOf()
    }
  }
}
