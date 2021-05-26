package org.nypl.simplified.patron

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.joda.time.format.ISODateTimeFormat
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.links.Link
import org.nypl.simplified.links.json.LinkParsing
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.nypl.simplified.parser.api.ParseWarning
import org.nypl.simplified.patron.api.PatronAuthorization
import org.nypl.simplified.patron.api.PatronDRM
import org.nypl.simplified.patron.api.PatronDRMAdobe
import org.nypl.simplified.patron.api.PatronSettings
import org.nypl.simplified.patron.api.PatronUserProfile
import org.nypl.simplified.patron.api.PatronUserProfileParserType
import java.io.InputStream
import java.net.URI

/**
 * A patron user profile parser.
 */

internal class PatronUserProfileParser(
  private val mapper: ObjectMapper,
  private val uri: URI,
  private val stream: InputStream,
  private val warningsAsErrors: Boolean
) : PatronUserProfileParserType {

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

  private fun publishWarningMessage(message: String) {
    return this.publishWarning(
      ParseWarning(
        this.uri,
        message,
        line = 0,
        column = 0,
        exception = null
      )
    )
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

  override fun parse(): ParseResult<PatronUserProfile> {
    return try {
      val tree = this.mapper.readTree(this.stream)
      if (tree == null) {
        this.publishErrorForString("Document is empty")
        return ParseResult.Failure(warnings = this.warnings.toList(), errors = this.errors.toList())
      }

      val root =
        JSONParserUtilities.checkObject(null, tree)
      val links =
        this.parseLinks(root)
      val settings =
        this.parseSettings(root)
      val authorization =
        this.parseAuthorization(root)
      val drm =
        this.parseDRMs(root, links)

      if (this.errors.isEmpty()) {
        return ParseResult.Success(
          warnings = this.warnings.toList(),
          result = PatronUserProfile(
            settings = settings,
            links = links,
            drm = drm,
            authorization = authorization
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

  private fun parseLinks(
    root: ObjectNode
  ): List<Link> {
    val linksNode =
      JSONParserUtilities.getArrayOrNull(root, "links")
        ?: return listOf()

    val results = mutableListOf<Link>()
    for (node in linksNode) {
      when (val result = LinkParsing.parseLink(this.uri, node)) {
        is ParseResult.Failure -> {
          this.warnings.addAll(result.warnings)
          this.errors.addAll(result.errors)
        }
        is ParseResult.Success -> {
          this.warnings.addAll(result.warnings)
          results.add(result.result)
        }
      }
    }
    return results.toList()
  }

  private fun parseDRMs(
    root: ObjectNode,
    links: List<Link>
  ): List<PatronDRM> {
    return if (root.has("drm")) {
      try {
        val drms = JSONParserUtilities.getArray(root, "drm")
        drms.mapNotNull { node -> this.parseDRM(node, links) }
      } catch (e: Exception) {
        this.publishErrorForException(e)
        listOf()
      }
    } else {
      listOf()
    }
  }

  private fun parseDRM(
    node: JsonNode,
    links: List<Link>
  ): PatronDRM? {
    return try {
      val root =
        JSONParserUtilities.checkObject(null, node)
      val vendor =
        JSONParserUtilities.getString(root, "drm:vendor")
      val scheme =
        JSONParserUtilities.getURI(root, "drm:scheme")

      return when (scheme.toString()) {
        "http://librarysimplified.org/terms/drm/scheme/ACS" -> {
          this.parseDRMAdobe(root, vendor, scheme, links)
        }
        else -> {
          this.publishWarningMessage("Unrecognized DRM scheme: $scheme")
          null
        }
      }
    } catch (e: Exception) {
      this.publishErrorForException(e)
      null
    }
  }

  private fun parseDRMAdobe(
    root: ObjectNode,
    vendor: String,
    scheme: URI,
    links: List<Link>
  ): PatronDRMAdobe {
    val clientToken =
      JSONParserUtilities.getString(root, "drm:clientToken")

    val linksCombined = mutableListOf<Link>()
    linksCombined.addAll(links)
    linksCombined.addAll(this.parseLinks(root))

    val deviceManagerURI =
      linksCombined.find { link -> link.relation == "http://librarysimplified.org/terms/drm/rel/devices" }
        ?.hrefURI

    return PatronDRMAdobe(
      vendor = vendor,
      scheme = scheme,
      clientToken = clientToken,
      deviceManagerURI = deviceManagerURI
    )
  }

  private fun parseAuthorization(root: ObjectNode): PatronAuthorization? {
    return try {
      val identifier =
        JSONParserUtilities.getString(root, "simplified:authorization_identifier")
      val expires =
        JSONParserUtilities.getStringOrNull(root, "simplified:authorization_expires")
          ?.let { text -> ISODateTimeFormat.dateTimeParser().parseDateTime(text) }
          ?.let { time -> time.toInstant() }
      PatronAuthorization(identifier, expires)
    } catch (e: Exception) {
      this.publishErrorForException(e)
      null
    }
  }

  private fun parseSettings(root: ObjectNode): PatronSettings {
    return try {
      val settingsRoot = JSONParserUtilities.getObject(root, "settings")

      val synchronizeAnnotations =
        when (settingsRoot["simplified:synchronize_annotations"]) {
          is NullNode, null ->
            false
          else ->
            JSONParserUtilities.getBoolean(settingsRoot, "simplified:synchronize_annotations")
        }

      return PatronSettings(synchronizeAnnotations = synchronizeAnnotations)
    } catch (e: Exception) {
      this.publishErrorForException(e)
      PatronSettings(synchronizeAnnotations = false)
    }
  }
}
