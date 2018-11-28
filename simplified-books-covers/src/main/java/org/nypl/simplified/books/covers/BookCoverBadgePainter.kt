package org.nypl.simplified.books.covers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.squareup.picasso.Transformation
import org.nypl.simplified.books.core.FeedEntryOPDS

/**
 * An image transformer that optionally adds a badge image to the loaded book cover.
 */

class BookCoverBadgePainter(
  val entry: FeedEntryOPDS,
  val badges: BookCoverBadgeLookupType) : Transformation {

  override fun key(): String {
    return "org.nypl.simplified.books.covers.BookCoverBadgePainter"
  }

  override fun transform(source: Bitmap): Bitmap {
    val badge = this.badges.badgeForEntry(this.entry)
    if (badge == null) {
      return source
    }

    val workingBitmap = Bitmap.createBitmap(source)
    val result = workingBitmap.copy(source.config, true)
    val canvas = Canvas(result)

    val left = source.width - badge.width
    val right = source.width
    val top = source.height - badge.height
    val bottom = source.height
    val targetRect = Rect(left, top, right, bottom)

    if (badge.backgroundColorRGBA != null) {
      val backgroundPaint = Paint()
      backgroundPaint.color = badge.backgroundColorRGBA
      backgroundPaint.isAntiAlias = true
      canvas.drawRect(targetRect, backgroundPaint)
    }

    val imagePaint = Paint()
    imagePaint.isAntiAlias = true
    val sourceRect = Rect(0, 0, badge.bitmap.width, badge.bitmap.height)
    canvas.drawBitmap(badge.bitmap, sourceRect, targetRect, imagePaint)

    source.recycle()
    return result
  }
}