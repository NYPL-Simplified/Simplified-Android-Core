package org.nypl.simplified.ui.catalog.withoutGroups

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
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

class BookIdleViewHolder(
  private val binding: BookCellIdleBinding,
  private val bookCoverProvider: BookCoverProviderType,
  private val showFormatLabel: Boolean,
  private val viewLifecycleScope: CoroutineScope,
) : RecyclerView.ViewHolder(binding.root) {

  companion object {
    const val DOWNLOAD_COMPLETE_FOOTER_DElAY = 5000L
  }

  private var footerAnimationJob: Job? = null

  fun bind(item: BookItem.Idle) {
    binding.idleItem = item
    binding.executePendingBindings()

    loadCover(item)
    toggleFormatLabelVisibility()
    configureDownloadViews(item)
    configureButtons(item)
  }

  private fun animatedFooterCollapse(view: View) {
    val fullHeight = view.measuredHeight

    val animation = object : Animation() {
      override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        if (interpolatedTime == 1f) {
          view.visibility = View.GONE
        } else {
          view.layoutParams.height = fullHeight - (fullHeight * interpolatedTime).toInt()
          view.requestLayout()
        }
      }
    }

    animation.duration = (fullHeight / view.context.resources.displayMetrics.density).toLong()
    view.startAnimation(animation)
  }

  private fun animatedFooterExpand(view: View) {
    view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    val fullHeight = view.measuredHeight

    view.layoutParams.height = 0
    view.visibility = View.VISIBLE

    val animation = object : Animation() {
      override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
        view.layoutParams.height =
          if (interpolatedTime == 1f) ViewGroup.LayoutParams.WRAP_CONTENT
          else (fullHeight * interpolatedTime).toInt()
        view.requestLayout()
      }

      override fun willChangeBounds() = true
    }

    animation.duration = (fullHeight / view.context.resources.displayMetrics.density).toLong()
    view.startAnimation(animation)
  }

  private fun configureDownloadViews(item: BookItem.Idle) {
    when (val state = item.downloadState) {
      DownloadState.Complete -> {
        binding.bookCellIdleDownloadFooter.visibility = View.VISIBLE
        binding.bookCellIdleDownloadInProgress.visibility = View.INVISIBLE
        binding.bookCellIdleDownloadComplete.visibility = View.VISIBLE

        footerAnimationJob = viewLifecycleScope.launch {
          delay(DOWNLOAD_COMPLETE_FOOTER_DElAY)
          animatedFooterCollapse(binding.bookCellIdleDownloadFooter)
        }
        footerAnimationJob?.invokeOnCompletion {
          binding.bookCellIdleDownloadFooter.isVisible = true
        }
      }
      is DownloadState.InProgress -> {
        binding.bookCellIdleDownloadFooter.visibility = View.INVISIBLE
        binding.bookCellIdleDownloadFooter.let {
          if (state.isStarting) {
            viewLifecycleScope.launch {
              delay(250)
              animatedFooterExpand(it)
            }
          } else it.visibility = View.VISIBLE
        }
        binding.bookCellIdleDownloadInProgress.visibility = View.VISIBLE
        binding.bookCellIdleDownloadComplete.visibility = View.GONE

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
    if (item.downloadState is DownloadState.InProgress) {
      binding.bookCellIdlePrimaryButton.visibility = View.GONE
      binding.bookCellIdleSecondaryButton.visibility = View.GONE
    }

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
