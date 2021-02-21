package org.librarysimplified.r2.api

/**
 * Information about a chapter within a loaded book.
 */

data class SR2BookChapter(
  val chapterIndex: Int,
  val title: String
)
