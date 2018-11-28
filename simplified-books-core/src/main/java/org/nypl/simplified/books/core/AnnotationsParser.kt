package org.nypl.simplified.books.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.Collections

class AnnotationsParser {

  companion object {
    val LOG = LoggerFactory.getLogger(AnnotationsParser::class.java)!!

    fun parseBookmarkArray(stream: InputStream): List<BookmarkAnnotation> {
      return Collections.unmodifiableList(parseFromStream(stream))
    }

    private fun parseFromStream(stream: InputStream): List<BookmarkAnnotation> {
      return try {
        val mapper = jacksonObjectMapper()
        val jsonObj: Map<String, List<BookmarkAnnotation>> = mapper.readValue(stream)
        jsonObj["bookmarks"] ?: ArrayList()
      } catch (e: Exception) {
        LOG.error("Exception thrown while parsing bookmarks list from input stream: {}" +
          "\nReturning an empty list.", e)
        ArrayList()
      }
    }
  }
}
