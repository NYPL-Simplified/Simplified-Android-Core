package org.nypl.simplified.books.covers

import android.graphics.Bitmap

/**
 * A book badge definition.
 */

data class BookCoverBadge(
  val bitmap: Bitmap,
  val width: Int,
  val height: Int,
  val backgroundColorRGBA: Int?)
