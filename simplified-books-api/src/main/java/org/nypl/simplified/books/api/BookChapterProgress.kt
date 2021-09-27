package org.nypl.simplified.books.api

import java.io.Serializable

/**
 * Progress through a specific chapter.
 */

data class BookChapterProgress(

  /**
   * The href of the chapter.
   */

  val chapterHref: String,

  /**
   * The progress through the chapter.
   */

  val chapterProgress: Double
) : Serializable
