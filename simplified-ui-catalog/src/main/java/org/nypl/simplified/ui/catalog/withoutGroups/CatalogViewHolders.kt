package org.nypl.simplified.ui.catalog.withoutGroups

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.ui.catalog.R
import org.nypl.simplified.ui.catalog.databinding.BookCellCorruptBinding
import org.nypl.simplified.ui.catalog.databinding.BookCellErrorBinding
import org.nypl.simplified.ui.catalog.databinding.BookCellIdleBinding
import org.nypl.simplified.ui.catalog.databinding.BookCellInProgressBinding

class BookIdleViewHolder(
  private val binding: BookCellIdleBinding,
  private val bookCoverProvider: BookCoverProviderType,
  private val showFormatLabel: Boolean
) : RecyclerView.ViewHolder(binding.root) {

  fun bind(item: BookItem.Idle) {
    binding.idleItem = item
    binding.executePendingBindings()

    loadCover(item)
    configureButtons(item)
    toggleFormatLabelVisibility()
  }

  private fun toggleFormatLabelVisibility() {
    if (!showFormatLabel) binding.bookCellIdleMeta.visibility = View.GONE
  }

  private fun loadCover(item: BookItem.Idle) {
    binding.bookCellIdleCover.setImageDrawable(null)
    binding.bookCellIdleCover.visibility = View.INVISIBLE
    binding.bookCellIdleCoverProgress.visibility = View.VISIBLE

    bookCoverProvider.loadThumbnailInto(
      item.entry,
      binding.bookCellIdleCover,
      0,
      itemView.context.resources.getDimensionPixelOffset(R.dimen.cover_thumbnail_height)
    ).map {
      binding.bookCellIdleCoverProgress.visibility = View.INVISIBLE
      binding.bookCellIdleCover.visibility = View.VISIBLE
    }
  }

  private fun configureButtons(item: BookItem.Idle) {
    item.actions.primaryButton()?.let {
      binding.bookCellIdlePrimaryButton.text = it.getText(itemView.context)
      binding.bookCellIdlePrimaryButton.contentDescription = it.getDescription(itemView.context)
      binding.bookCellIdlePrimaryButton.setOnClickListener { _ -> it.onClick() }
    } ?: run {
      binding.bookCellIdlePrimaryButton.visibility = View.GONE
    }

    item.actions.secondaryButton()?.let {
      binding.bookCellIdleSecondaryButton.text = it.getText(itemView.context)
      binding.bookCellIdleSecondaryButton.contentDescription = it.getDescription(itemView.context)
      binding.bookCellIdleSecondaryButton.setOnClickListener { _ -> it.onClick() }
    } ?: run {
      binding.bookCellIdleSecondaryButton.visibility = View.GONE
    }
  }
}

// Doesn't seem like these exist in current apps - maybe a LFA remnant?
class BookCorruptViewHolder(
  private val binding: BookCellCorruptBinding,
) : RecyclerView.ViewHolder(binding.root) {
  fun bind(item: BookItem.Corrupt) {}
}

class BookErrorViewHolder(
  private val binding: BookCellErrorBinding,
) : RecyclerView.ViewHolder(binding.root) {
  fun bind(item: BookItem.Error) {
    binding.errorItem = item
    binding.executePendingBindings()
  }
}

class BookInProgressViewHolder(
  private val binding: BookCellInProgressBinding,
) : RecyclerView.ViewHolder(binding.root) {
  fun bind(item: BookItem.InProgress) {
    binding.inProgressItem = item
    binding.executePendingBindings()
  }
}
