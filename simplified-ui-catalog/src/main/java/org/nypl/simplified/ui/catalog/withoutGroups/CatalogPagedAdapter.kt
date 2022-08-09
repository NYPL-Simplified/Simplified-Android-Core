package org.nypl.simplified.ui.catalog.withoutGroups

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import org.nypl.simplified.books.covers.BookCoverProviderType
import org.nypl.simplified.buildconfig.api.BuildConfigurationServiceType
import org.nypl.simplified.ui.catalog.databinding.BookCellCorruptBinding
import org.nypl.simplified.ui.catalog.databinding.BookCellErrorBinding
import org.nypl.simplified.ui.catalog.databinding.BookCellIdleBinding
import org.nypl.simplified.ui.catalog.databinding.BookCellInProgressBinding
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem.Type.CORRUPT
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem.Type.ERROR
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem.Type.IDLE
import org.nypl.simplified.ui.catalog.withoutGroups.BookItem.Type.LOADING
import org.slf4j.LoggerFactory

class CatalogPagedAdapter(
  private val bookCoverProvider: BookCoverProviderType,
  private val buildConfig: BuildConfigurationServiceType,
  private val viewLifecycleScope: CoroutineScope
) : PagingDataAdapter<BookItem, RecyclerView.ViewHolder>(
  CatalogPagedAdapterDiffing.comparisonCallback
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val item = getItem(position)
    item?.let {
      when (it) {
        is BookItem.Corrupt -> (holder as BookCorruptViewHolder).bind(it)
        is BookItem.Error -> (holder as BookErrorViewHolder).bind(it)
        is BookItem.Idle -> (holder as BookIdleViewHolder).bind(it)
        is BookItem.InProgress -> (holder as BookInProgressViewHolder).bind(it)
      }
    } ?: run {
      // probably need to do something if/when item is null as paging is using placeholders
      // maybe loadingviewholders can take null data for an empty loading state?
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)

    return when (viewType) {
      IDLE.ordinal -> {
        val binding = BookCellIdleBinding.inflate(inflater, parent, false)
        BookIdleViewHolder(binding, bookCoverProvider, buildConfig.showFormatLabel, viewLifecycleScope)
      }
      CORRUPT.ordinal -> {
        val binding = BookCellCorruptBinding.inflate(inflater, parent, false)
        BookCorruptViewHolder(binding)
      }
      ERROR.ordinal -> {
        val binding = BookCellErrorBinding.inflate(inflater, parent, false)
        BookErrorViewHolder(binding)
      }
      LOADING.ordinal -> {
        val binding = BookCellInProgressBinding.inflate(inflater, parent, false)
        BookInProgressViewHolder(binding, bookCoverProvider, buildConfig.showFormatLabel)
      }
      else -> throw IllegalStateException("ViewType must match known ViewHolder type")
    }
  }

  override fun getItemViewType(position: Int): Int {
    return getItem(position)?.type?.ordinal ?: -1
  }

  override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
    super.onViewRecycled(holder)
    if (holder is BookIdleViewHolder) {
      logger.debug("Unbind called for $holder")
      holder.unbind()
    }
  }
}

object CatalogPagedAdapterDiffing {
  val comparisonCallback =
    object : DiffUtil.ItemCallback<BookItem>() {
      override fun areItemsTheSame(
        oldItem: BookItem,
        newItem: BookItem
      ): Boolean {
        return when {
          oldItem is BookItem.Idle && newItem is BookItem.Idle -> {
            oldItem.entry == newItem.entry
          }
          oldItem is BookItem.Corrupt && newItem is BookItem.Corrupt -> {
            oldItem.entry == newItem.entry
          }
          oldItem is BookItem.Error && newItem is BookItem.Error -> {
            oldItem.entry == newItem.entry && oldItem.failure == newItem.failure
          }
          oldItem is BookItem.InProgress && newItem is BookItem.InProgress -> {
            oldItem.title == newItem.title
          }
          else -> false
        }
      }

      override fun areContentsTheSame(
        oldItem: BookItem,
        newItem: BookItem
      ): Boolean {
        return oldItem == newItem
      }
    }
}
