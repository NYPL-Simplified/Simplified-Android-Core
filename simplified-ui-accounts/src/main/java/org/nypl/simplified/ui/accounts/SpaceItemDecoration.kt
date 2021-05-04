package org.nypl.simplified.ui.accounts

import android.content.Context
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpaceItemDecoration(
  @RecyclerView.Orientation private val direction: Int,
  context: Context,
  margin: Float = 8f,
  private val marginOnInitialItem: Boolean = true
) : RecyclerView.ItemDecoration() {
  private val space = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, margin, context.resources.displayMetrics)
    .toInt()

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State
  ) {
    val pos = parent.getChildLayoutPosition(view)

    val (hoz, vert) = when (direction) {
      RecyclerView.HORIZONTAL -> space to 0
      else -> 0 to space
    }
    when {
      !marginOnInitialItem && pos == 0 -> outRect.set(0, 0, 0, 0)
      pos == 0 -> outRect.set(hoz, vert, hoz, vert)
      else -> outRect.set(hoz / 2, vert / 2, hoz, vert)
    }
  }
}
