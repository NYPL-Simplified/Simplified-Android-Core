package org.nypl.simplified.viewer.epub.readium1.toc

import java.io.Serializable

/**
 * A TOC element.
 */

data class ReaderTOCElement(

  /**
   * @return The indentation level of the element (used when rendering the TOC)
   */

  val indent: Int,

  /**
   * @return The content ref for the element
   */

  val contentRef: String,

  /**
   * @return The source href of the element
   */

  val sourceHref: String,

  /**
   * @return The title of the element
   */

  val title: String
) : Serializable
