package org.nypl.simplified.ui.catalog

import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.slf4j.LoggerFactory

/**
 * A recycler view scroll listener that pauses the loading of thumbnails during
 * scrolling. This is purely for improving scroll performance.
 */

class CatalogScrollListener(
  private val bookCovers: BookCoverProviderType
) : RecyclerView.OnScrollListener() {

  private val logger =
    LoggerFactory.getLogger(CatalogScrollListener::class.java)

  override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
    when (newState) {
      RecyclerView.SCROLL_STATE_DRAGGING -> {
        this.logger.debug("scrolling: pausing thumbnail loading")
        this.bookCovers.loadingThumbnailsPause()
      }
      RecyclerView.SCROLL_STATE_IDLE,
      RecyclerView.SCROLL_STATE_SETTLING -> {
        this.logger.debug("idling: resuming thumbnail loading")
        this.bookCovers.loadingThumbnailsContinue()
      }
    }
  }
}
