package org.nypl.simplified.ui.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.ui.images.ImageLoaderType
import org.slf4j.LoggerFactory

/**
 * Adapter for showing a list of `AccountProviderDescription` items.
 *
 * Use [submitList] to add items to the adapter.
 */

class FilterableAccountListAdapter(
  private val imageLoader: ImageLoaderType,
  private val onItemClicked: (AccountProviderDescription) -> Unit
) : ListAdapter<AccountProviderDescription, AccountItemViewHolder>(DIFF_CALLBACK) {

  private val logger =
    LoggerFactory.getLogger(FilterableAccountListAdapter::class.java)
  private var listCopy =
    mutableListOf<AccountProviderDescription>()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountItemViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val item = inflater.inflate(R.layout.account_cell, parent, false)

    return AccountItemViewHolder(
      item,
      this.imageLoader,
      this.onItemClicked
    )
  }

  override fun onBindViewHolder(holder: AccountItemViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  override fun submitList(list: List<AccountProviderDescription>?) {
    this.listCopy.clear()
    super.submitList(list)
  }

  override fun submitList(
    list: List<AccountProviderDescription>?,
    commitCallback: Runnable?
  ) {
    this.listCopy.clear()
    super.submitList(list, commitCallback)
  }

  /** Returns true if the adapter is currently filtered. */

  fun isFiltered(): Boolean {
    return listCopy.isNotEmpty()
  }

  /**
   * Filter the original list of items. Each time this method is called the
   * original 'unfiltered' list is used as the base.
   */

  fun filterList(filter: (AccountProviderDescription) -> Boolean) {
    if (this.listCopy.isEmpty()) {
      this.listCopy.addAll(this.currentList)
    }

    super.submitList(this.listCopy.filter(filter)) {
      this.logger.debug("{} matching items", this.currentList.size)
    }
  }

  /** Reset the filter and show the original list of items. */

  fun resetFilter() {
    if (!isFiltered()) return
    super.submitList(this.listCopy.toList()) {
      this.listCopy.clear()
    }
  }
}

/**
 * Callback for calculating the diff between two non-null items in a list.
 */

val DIFF_CALLBACK =
  object : DiffUtil.ItemCallback<AccountProviderDescription>() {
    override fun areItemsTheSame(
      oldItem: AccountProviderDescription,
      newItem: AccountProviderDescription
    ): Boolean {
      return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
      oldItem: AccountProviderDescription,
      newItem: AccountProviderDescription
    ): Boolean {
      return oldItem.title == newItem.title
    }
  }
