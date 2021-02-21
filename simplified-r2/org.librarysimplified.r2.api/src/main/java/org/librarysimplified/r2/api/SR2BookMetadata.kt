package org.librarysimplified.r2.api

/**
 * Information about the currently loaded book.
 */

data class SR2BookMetadata(

  /**
   * The unique identifier of the book.
   */

  val id: String,

  /**
   * The chapters of the book in reading order.
   */

  val readingOrder: List<SR2BookChapter>
) {

  init {
    require(this.readingOrder.sortedBy { it.chapterIndex } == this.readingOrder) {
      "The reading order must be sorted"
    }
    require(this.readingOrder.associateBy { it.chapterIndex }.size == this.readingOrder.size) {
      "The reading order indices must be unique"
    }
  }
}
