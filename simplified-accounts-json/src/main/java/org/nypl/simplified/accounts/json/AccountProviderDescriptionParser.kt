package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.node.ObjectNode
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.nypl.simplified.accounts.api.AccountProviderDescriptionMetadata
import org.nypl.simplified.accounts.api.AccountProviderDescriptionParserType
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseWarning
import java.net.URI

class AccountProviderDescriptionParser internal constructor(
  private val uri: URI,
  private val objectNode: () -> ObjectNode,
  private val warningsAsErrors: Boolean) : AccountProviderDescriptionParserType {

  private val errors =
    mutableListOf<ParseError>()
  private val warnings =
    mutableListOf<ParseWarning>()

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
    val title: String)

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
            isAutomatic = false,
            isProduction = false))
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
        title = parseMetadataTitle(metadata))
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'updated' field as timestamp",
        exception = e))
      Metadata(
        updated = DateTime.now(),
        description = "",
        id = URI.create("urn:invalid"),
        title = "")
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
    return listOf()
  }

  private fun parseLinks(root: ObjectNode): List<AccountProviderDescriptionMetadata.Link> {
    return listOf()
  }
}
