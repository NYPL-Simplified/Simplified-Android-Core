package org.nypl.simplified.ui.catalog

import android.graphics.Rect
import android.view.View
import androidx.annotation.Px
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * An `ItemDecoration` that draws a space between items in a `RecyclerView`.
 *
 * Works with any horizontally or vertically oriented `LinearLayoutManager`.
 */
class SpaceItemDecoration(
  @Px private val size: Int
) : RecyclerView.ItemDecoration() {

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    super.getItemOffsets(outRect, view, parent, state)

    val layoutManager = parent.layoutManager

    if (layoutManager is LinearLayoutManager) {
      val position = parent.getChildViewHolder(view).layoutPosition

      // No position
      if (position == RecyclerView.NO_POSITION) {
        return
      }

      when (layoutManager.orientation) {
        LinearLayoutManager.HORIZONTAL -> {
          outRect.left = if (position > 0) size else 0
        }
        LinearLayoutManager.VERTICAL -> {
          outRect.top = if (position > 0) size else 0
        }
      }
    } else {
      throw IllegalStateException("Unsupported LayoutManager '${layoutManager?.javaClass?.name}'")
    }
  }
}
