package org.nypl.simplified.books.api

/**
 * Progress through a specific chapter.
 */

data class BookChapterProgress(

  /**
   * The index of the chapter.
   */

  val chapterIndex: Int,

  /**
   * The progress through the chapter.
   */

  val chapterProgress: Double
)
