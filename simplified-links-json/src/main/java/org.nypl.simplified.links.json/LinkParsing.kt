package org.nypl.simplified.links.json

import com.fasterxml.jackson.databind.JsonNode
import one.irradia.mime.vanilla.MIMEParser
import org.nypl.simplified.json.core.JSONParseException
import org.nypl.simplified.json.core.JSONParserUtilities
import org.nypl.simplified.links.Link
import org.nypl.simplified.parser.api.ParseError
import org.nypl.simplified.parser.api.ParseResult
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Functions to parse links.
 */

object LinkParsing {

  private val logger = LoggerFactory.getLogger(LinkParsing::class.java)

  /**
   * Parse a link from the given source and JSON object.
   */

  fun parseLink(
    source: URI,
    element: JsonNode
  ): ParseResult<Link> {
    return try {
      val objectNode = JSONParserUtilities.checkObject("", element)

      val templated =
        JSONParserUtilities.getBooleanDefault(objectNode, "templated", false)
      val relation =
        JSONParserUtilities.getStringOrNull(objectNode, "rel")

      val mime =
        JSONParserUtilities.getStringOrNull(objectNode, "type")
          ?.let { type -> MIMEParser.parseRaisingException(type) }

      val title =
        JSONParserUtilities.getStringOrNull(objectNode, "title")
      val width =
        JSONParserUtilities.getIntegerOrNull(objectNode, "width")
      val height =
        JSONParserUtilities.getIntegerOrNull(objectNode, "height")
      val duration =
        JSONParserUtilities.getStringOrNull(objectNode, "duration")?.toDouble()
      val bitrate =
        JSONParserUtilities.getStringOrNull(objectNode, "bitrate")?.toDouble()

      ParseResult.Success(
        warnings = listOf(),
        result =
        if (templated) {
          Link.LinkTemplated(
            href = JSONParserUtilities.getString(objectNode, "href"),
            type = mime,
            relation = relation,
            title = title,
            width = width,
            height = height,
            duration = duration,
            bitrate = bitrate
          )
        } else {
          Link.LinkBasic(
            href = JSONParserUtilities.getURI(objectNode, "href"),
            type = mime,
            relation = relation,
            title = title,
            width = width,
            height = height,
            duration = duration,
            bitrate = bitrate
          )
        }
      )
    } catch (e: JSONParseException) {
      this.logger.error("error parsing link object: ", e)
      ParseResult.Failure(
        warnings = listOf(),
        errors = listOf(
          ParseError(
            source = source,
            message = "Could not parse 'link' object: " + e.message,
            exception = e
          )
        )
      )
    } catch (e: Exception) {
      this.logger.error("error parsing link object: ", e)
      ParseResult.Failure(
        warnings = listOf(),
        errors = listOf(
          ParseError(
            source = source,
            message = "Could not parse 'link' object: " + e.message,
            exception = e
          )
        )
      )
    }
  }
}
