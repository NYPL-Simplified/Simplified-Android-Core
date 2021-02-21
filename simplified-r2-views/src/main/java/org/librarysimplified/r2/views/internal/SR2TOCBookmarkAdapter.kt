package org.librarysimplified.r2.views.internal

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import org.librarysimplified.r2.api.SR2Bookmark
import org.librarysimplified.r2.views.R
import org.librarysimplified.r2.views.internal.SR2Adapters.bookmarkDiffCallback

internal class SR2TOCBookmarkAdapter(
  private val resources: Resources,
  private val onBookmarkSelected: (SR2Bookmark) -> Unit,
  private val onBookmarkDeleteRequested: (SR2Bookmark) -> Unit
) : ListAdapter<SR2Bookmark, SR2TOCBookmarkViewHolder>(bookmarkDiffCallback) {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): SR2TOCBookmarkViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val bookmarkView =
      inflater.inflate(R.layout.sr2_toc_bookmark_item, parent, false)
    return SR2TOCBookmarkViewHolder(bookmarkView)
  }

  override fun onBindViewHolder(
    holder: SR2TOCBookmarkViewHolder,
    position: Int
  ) {
    holder.bindTo(
      resources = this.resources,
      onBookmarkSelected = this.onBookmarkSelected,
      onBookmarkDeleteRequested = this.onBookmarkDeleteRequested,
      bookmark = this.getItem(position)
    )
  }

  fun setBookmarks(bookmarksNow: List<SR2Bookmark>) {
    this.submitList(bookmarksNow)
  }
}
