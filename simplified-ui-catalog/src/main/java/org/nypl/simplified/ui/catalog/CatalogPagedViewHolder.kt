package org.nypl.simplified.ui.catalog

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.common.util.concurrent.FluentFuture
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookStatusEvent
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.feeds.api.FeedEntry
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryCorrupt
import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.ui.thread.api.UIThreadServiceType

class CatalogPagedViewHolder(
  private val bookRegistry: BookRegistryReadableType,
  private val bookCovers: BookCoverProviderType,
  private val compositeDisposable: CompositeDisposable,
  private val uiThread: UIThreadServiceType,
  private val parent: View,
  private val shortAnimationDuration: Int,
  private val onBookSelected: (FeedEntry.FeedEntryOPDS) -> Unit
) : RecyclerView.ViewHolder(parent) {

  private var thumbnailLoading: FluentFuture<Unit>? = null

  private val idle =
    this.parent.findViewById<ViewGroup>(R.id.bookCellIdle)
  private val loading =
    this.parent.findViewById<ViewGroup>(R.id.bookCellEndLoading)
  private val corrupt =
    this.parent.findViewById<ViewGroup>(R.id.bookCellCorrupt)

  private val idleCover =
    this.parent.findViewById<ImageView>(R.id.bookCellCover)
  private val idleProgress =
    this.parent.findViewById<ProgressBar>(R.id.bookCellCoverProgress)
  private val idleText =
    this.idle.findViewById<TextView>(R.id.bookCellTitle)

  private val loadingProgress =
    this.loading.findViewById<ProgressBar>(R.id.bookCellEndLoadingProgress)

  private var bookSubscription: Disposable? = null
  private var feedEntry: FeedEntry? = null

  fun bindTo(item: FeedEntry?) {
    this.feedEntry = item
    this.unsubscribeFromBookRegistry()

    return when (item) {
      is FeedEntryCorrupt -> {
        this.corrupt.visibility = View.VISIBLE
        this.loading.visibility = View.INVISIBLE
        this.idle.visibility = View.INVISIBLE

        this.idle.setOnClickListener(null)
        this.idleText.setOnClickListener(null)
        this.idleCover.setOnClickListener(null)
      }

      is FeedEntryOPDS -> {
        val subscription =
          this.bookRegistry.bookEvents().subscribe { bookEvent ->
            if (bookEvent.book() == item.bookID) {
              this.onBookChanged(bookEvent)
            }
          }
        this.bookSubscription = subscription
        this.compositeDisposable.add(subscription)

        this.corrupt.visibility = View.INVISIBLE
        this.loading.visibility = View.INVISIBLE
        this.idle.visibility = View.VISIBLE

        this.idleCover.visibility = View.INVISIBLE
        this.idleProgress.visibility = View.VISIBLE
        this.idleText.text = item.feedEntry.title

        this.thumbnailLoading =
          this.bookCovers.loadThumbnailInto(
            item,
            this.idleCover,
            this.idleCover.layoutParams.width,
            this.idleCover.layoutParams.height
          ).map {
            this.uiThread.runOnUIThread {
              this.idleProgress.visibility = View.INVISIBLE
              this.idleCover.visibility = View.VISIBLE
              this.idleCover.alpha = 0.0f
              this.idleCover.animate()
                .alpha(1f)
                .setDuration(this.shortAnimationDuration.toLong())
                .setListener(null)
            }
          }

        val onClick: (View) -> Unit = { this.onBookSelected.invoke(item) }
        this.idle.setOnClickListener(onClick)
        this.idleText.setOnClickListener(onClick)
        this.idleCover.setOnClickListener(onClick)
      }

      null -> {
        this.corrupt.visibility = View.INVISIBLE
        this.loading.visibility = View.VISIBLE
        this.idle.visibility = View.INVISIBLE

        this.idle.setOnClickListener(null)
        this.idleText.setOnClickListener(null)
        this.idleCover.setOnClickListener(null)
      }
    }
  }

  private fun onBookChanged(event: BookStatusEvent) {
    val status = this.bookRegistry.bookStatusOrNull(event.book())

  }

  fun unbind() {
    this.unsubscribeFromBookRegistry()
    this.thumbnailLoading =
      this.thumbnailLoading?.let { loading ->
        loading.cancel(true)
        null
      }
  }

  private fun unsubscribeFromBookRegistry() {
    val subscription = this.bookSubscription
    if (subscription != null && !subscription.isDisposed) {
      subscription.dispose()
    }
    this.bookSubscription = null
  }
}