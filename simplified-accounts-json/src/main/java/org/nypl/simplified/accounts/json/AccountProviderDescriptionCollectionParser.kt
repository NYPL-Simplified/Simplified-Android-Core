package org.nypl.simplified.accounts.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.nypl.drm.core.AdobeVendorID
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollection
import org.nypl.simplified.accounts.api.AccountProviderDescriptionCollectionParserType
import org.nypl.simplified.accounts.api.AccountProviderDescriptionMetadata
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseWarning
import java.io.InputStream
import java.net.URI

/**
 * A parser of provider description collections.
 */

class AccountProviderDescriptionCollectionParser internal constructor(
  private val uri: URI,
  private val stream: InputStream,
  private val warningsAsErrors: Boolean
) : AccountProviderDescriptionCollectionParserType {

  private val mapper = ObjectMapper()
  private val metaParsers = AccountProviderDescriptionParsers()
  private val errors = mutableListOf<ParseError>()
  private val warnings = mutableListOf<ParseWarning>()

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

  private fun publishWarningMessage(message: String) {
    return this.publishWarning(ParseWarning(
      uri,
      message,
      line = 0,
      column = 0,
      exception = null))
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

  override fun parse(): ParseResult<AccountProviderDescriptionCollection> {
    return try {
      val tree = this.mapper.readTree(this.stream)
      if (tree == null) {
        this.publishErrorForString("Document is empty")
        return ParseResult.Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
      }

      val root =
        JSONParserUtilities.checkObject(null, tree)

      val catalogs = parseCatalogs(root)
      val links = parseLinks(root)
      val metadata = parseMetadata(root)

      if (this.errors.isEmpty()) {
        return ParseResult.Success(
          warnings = this.warnings.toList(),
          result = AccountProviderDescriptionCollection(
            providers = catalogs,
            links = links,
            metadata = metadata))
      } else {
        ParseResult.Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
      }
    } catch (e: Exception) {
      this.publishErrorForException(e)
      ParseResult.Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
    }
  }

  private fun parseMetadata(root: ObjectNode): AccountProviderDescriptionCollection.Metadata {
    try {
      val metaNode = JSONParserUtilities.getObject(root, "metadata")!!
      return AccountProviderDescriptionCollection.Metadata(
        adobeVendorID = parseAdobeVendorID(metaNode),
        title = parseTitle(metaNode))
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'metadata' field as an object",
        exception = e))
      return AccountProviderDescriptionCollection.Metadata(
        adobeVendorID = null,
        title = "")
    }
  }

  private fun parseTitle(metaNode: ObjectNode): String {
    return try {
      JSONParserUtilities.getStringOrNull(metaNode, "title") ?: ""
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'title' field as a string",
        exception = e))
      ""
    }
  }

  private fun parseAdobeVendorID(metaNode: ObjectNode): AdobeVendorID? {
    return try {
      JSONParserUtilities.getStringOrNull(metaNode, "adobe_vendor_id")
        ?.let(::AdobeVendorID)
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'adobe_vendor_id' field as a vendor ID",
        exception = e))
      null
    }
  }

  private fun parseLinks(root: ObjectNode): List<AccountProviderDescriptionCollection.Link> {
    return listOf()
  }

  private fun parseCatalogs(root: ObjectNode): List<AccountProviderDescriptionMetadata> {
    val array = try {
      JSONParserUtilities.getArray(root, "catalogs")
    } catch (e: Exception) {
      this.errors.add(ParseError(
        source = this.uri,
        message = "Could not parse 'catalogs' field as an array",
        exception = e))
      null
    }

    return if (array != null) {
      array.mapNotNull { node -> parseAccountProviderDescriptionMetadata(node) }
    } else {
      listOf()
    }
  }

  private fun parseAccountProviderDescriptionMetadata(node: JsonNode): AccountProviderDescriptionMetadata? {
    return this.metaParsers.createParserForObject(
      uri = this.uri,
      objectNode = JSONParserUtilities.checkObject(null, node),
      warningsAsErrors = warningsAsErrors
    ).use { parser ->
      when (val result = parser.parse()) {
        is ParseResult.Success -> {
          this.warnings.addAll(result.warnings)
          result.result
        }
        is ParseResult.Failure -> {
          this.warnings.addAll(result.warnings)
          this.errors.addAll(result.errors)
          null
        }
      }
    }
  }

  override fun close() {
    this.stream.close()
  }
}
