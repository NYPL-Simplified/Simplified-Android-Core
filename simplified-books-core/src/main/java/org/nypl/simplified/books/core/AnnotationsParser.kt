package org.nypl.simplified.books.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.InputStream

class AnnotationsParser {

  companion object {
    val LOG = LoggerFactory.getLogger(AnnotationsParser::class.java)!!

    fun parseBookmarkArray(stream: InputStream): List<BookmarkAnnotation> {
      return try {
        val mapper = jacksonObjectMapper()
        mapper.readValue(stream)
      } catch (e: Exception) {
        LOG.error("Exception thrown while parsing bookmarks list from input stream: {}" +
            "\nReturning an empty list.", e)
        ArrayList()
      }
    }
  }
}
