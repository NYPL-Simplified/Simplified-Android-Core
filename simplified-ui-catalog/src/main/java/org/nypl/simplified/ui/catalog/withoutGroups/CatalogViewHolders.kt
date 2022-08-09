package org.nypl.simplified.ui.catalog.withoutGroups

import android.view.View
import androidx.core.view.doOnNextLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.futures.FluentFutureExtensions.map
import org.nypl.simplified.ui.catalog.R
import org.nypl.simplified.ui.catalog.databinding.BookCellCorruptBinding
import org.nypl.simplified.ui.catalog.databinding.BookCellErrorBinding
import org.nypl.simplified.ui.catalog.databinding.BookCellIdleBinding
import org.nypl.simplified.ui.catalog.databinding.BookCellInProgressBinding
import org.slf4j.LoggerFactory

class BookIdleViewHolder(
  private val binding: BookCellIdleBinding,
  private val bookCoverProvider: BookCoverProviderType,
  private val showFormatLabel: Boolean,
  private val viewLifecycleScope: CoroutineScope
) : RecyclerView.ViewHolder(binding.root) {

  companion object {
    const val DOWNLOAD_COMPLETE_FOOTER_DElAY = 5000L
  }

  private val logger = LoggerFactory.getLogger(javaClass)

  private var footerAnimationJob: Job? = null
  private var originalHeight: Int? = null
  private var expandedHeight: Int? = null

  fun bind(item: BookItem.Idle) {
    binding.idleItem = item
    binding.executePendingBindings()

    loadCover(item)
    toggleFormatLabelVisibility()
    configureDownloadViews(item)
    configureButtons(item)
  }

  fun setupAnimation() {
    logger.debug("setupAnimation")
    binding.bookCellIdle.doOnPreDraw { view ->
      logger.debug("doOnPreDraw")
      originalHeight = view.height

      binding.bookCellIdleDownloadFooter.visibility = View.VISIBLE
      binding.bookCellIdle.doOnNextLayout {
        logger.debug("doOnNextLayout")
        expandedHeight = it.height
        binding.bookCellIdleDownloadFooter.visibility = View.GONE
      }
    }
  }

  fun unbind() {
    footerAnimationJob?.cancel()
  }

  private fun configureDownloadViews(item: BookItem.Idle) {
    when (val state = item.downloadState) {
      DownloadState.Complete -> {
        binding.bookCellIdleDownloadFooter.visibility = View.VISIBLE
        binding.bookCellIdleDownloadInProgress.visibility = View.INVISIBLE
        binding.bookCellIdleDownloadComplete.visibility = View.VISIBLE

        footerAnimationJob = viewLifecycleScope.launch {
          delay(DOWNLOAD_COMPLETE_FOOTER_DElAY)
          binding.bookCellIdleDownloadFooter.visibility = View.GONE
        }
      }
      is DownloadState.InProgress -> {
        binding.bookCellIdleDownloadFooter.visibility = View.VISIBLE
        binding.bookCellIdleDownloadInProgress.visibility = View.VISIBLE
        binding.bookCellIdleDownloadComplete.visibility = View.INVISIBLE

        state.progress?.let { progress ->
          binding.bookCellIdleCoverProgress.isIndeterminate = false
          binding.bookCellIdleCoverProgress.progress = progress
        } ?: run {
          binding.bookCellIdleCoverProgress.isIndeterminate = true
        }
      }
      null -> {
        binding.bookCellIdleDownloadFooter.visibility = View.GONE
      }
    }
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
    binding.bookCellIdleButtons.isVisible = item.downloadState !is DownloadState.InProgress

    item.actions.primaryButton()?.let {
      binding.bookCellIdlePrimaryButton.visibility = View.VISIBLE
      binding.bookCellIdlePrimaryButton.text = it.getText(itemView.context)
      binding.bookCellIdlePrimaryButton.contentDescription = it.getDescription(itemView.context)
      binding.bookCellIdlePrimaryButton.setOnClickListener { _ -> it.onClick() }
    } ?: run {
      binding.bookCellIdlePrimaryButton.visibility = View.GONE
    }

    item.actions.secondaryButton()?.let {
      binding.bookCellIdleSecondaryButton.visibility = View.VISIBLE
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
  private val bookCoverProvider: BookCoverProviderType,
  private val showFormatLabel: Boolean
) : RecyclerView.ViewHolder(binding.root) {
  fun bind(item: BookItem.InProgress) {
    binding.inProgressItem = item
    binding.executePendingBindings()

    loadCover(item)
    toggleFormatLabelVisibility()
  }

  private fun toggleFormatLabelVisibility() {
    if (!showFormatLabel) binding.bookCellInProgressMeta.visibility = View.GONE
  }

  private fun loadCover(item: BookItem.InProgress) {
    binding.bookCellInProgressCover.setImageDrawable(null)
    binding.bookCellInProgressCover.visibility = View.INVISIBLE

    bookCoverProvider.loadThumbnailInto(
      item.entry,
      binding.bookCellInProgressCover,
      0,
      itemView.context.resources.getDimensionPixelOffset(R.dimen.cover_thumbnail_height)
    ).map {
      binding.bookCellInProgressCover.visibility = View.VISIBLE
    }
  }
}
