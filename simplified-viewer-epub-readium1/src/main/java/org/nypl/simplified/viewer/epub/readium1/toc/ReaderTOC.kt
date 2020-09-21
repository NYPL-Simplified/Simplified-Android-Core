package org.nypl.simplified.viewer.epub.readium1.toc

import org.nypl.simplified.viewer.epub.readium1.ReaderNativeCodeReadLock
import org.readium.sdk.android.components.navigation.NavigationElement
import org.readium.sdk.android.components.navigation.NavigationPoint
import org.readium.sdk.android.components.navigation.NavigationTable
import org.slf4j.LoggerFactory
import java.io.Serializable
import java.util.ArrayList
import java.util.Objects

/**
 * The table of contents.
 */

class ReaderTOC(val elements: List<ReaderTOCElement>) : Serializable {

  companion object {

    private val logger = LoggerFactory.getLogger(ReaderTOC::class.java)

    private fun accumulate(
      elements: MutableList<ReaderTOCElement>,
      indent: Int,
      parent: NavigationTable,
      currentElement: NavigationElement
    ) {
      logger.debug("accumulate: {}", currentElement)

      if (currentElement is NavigationPoint) {
        val title =
          Objects.requireNonNull(currentElement.title, "Title")
        val contentRef =
          Objects.requireNonNull(currentElement.content, "Content")
        val sourceHref =
          Objects.requireNonNull(parent.sourceHref, "Source HREF")

        logger.debug("nav point: {} → {}", contentRef, title)
        val tocElement =
          ReaderTOCElement(
            indent = indent,
            title = title,
            contentRef = contentRef,
            sourceHref = sourceHref
          )

        elements.add(tocElement)

        for (child in currentElement.children) {
          accumulate(
            elements,
            indent + 1,
            parent,
            Objects.requireNonNull(child, "Child element")
          )
        }

        return
      }

      if (currentElement is NavigationTable) {
        logger.debug(
          "nav table: {} {} → {}",
          currentElement.sourceHref,
          currentElement.type,
          currentElement.title
        )

        // XXX: What's the correct thing to do here? There's no
        // content ref accessible from here...

        val childElements = currentElement.getChildren()
        logger.debug("nav table: {} child elements", childElements.size)
        for (child in childElements) {
          accumulate(
            elements,
            indent + 1,
            currentElement,
            Objects.requireNonNull(child, "Child")
          )
        }
      }
    }

    /**
     * Parse and return a table of contents from the given package.
     *
     * @param p The package
     *
     * @return A table of contents
     */

    fun fromPackage(p: org.readium.sdk.android.Package): ReaderTOC {
      logger.debug("requesting toc")

      val readLock = ReaderNativeCodeReadLock.get()

      val elements = ArrayList<ReaderTOCElement>(32)
      val toc: NavigationTable

      synchronized(readLock) {
        toc = Objects.requireNonNull(p.tableOfContents, "TOC")
      }

      accumulate(elements, -1, toc, toc)
      return ReaderTOC(elements)
    }
  }
}
