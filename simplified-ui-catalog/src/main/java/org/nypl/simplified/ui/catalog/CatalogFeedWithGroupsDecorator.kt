package org.nypl.simplified.ui.catalog

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * A decorator that adds a bottom margin to the last item in a recycler view.
 */

internal class CatalogFeedWithGroupsDecorator(
  private val bottomOffset: Int
) : RecyclerView.ItemDecoration() {

  override fun getItemOffsets(
    outRect: Rect,
    view: View,
    parent: RecyclerView,
    state: RecyclerView.State
  ) {
    super.getItemOffsets(outRect, view, parent, state)
    val dataSize = state.getItemCount()
    val position = parent.getChildAdapterPosition(view)
    if (dataSize > 0 && position == dataSize - 1) {
      outRect.set(0, 0, 0, this.bottomOffset)
    } else {
      outRect.set(0, 0, 0, 0)
    }
  }
}
