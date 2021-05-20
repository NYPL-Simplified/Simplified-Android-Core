package org.nypl.simplified.ui.accounts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.ui.images.ImageAccountIcons
import org.nypl.simplified.ui.images.ImageLoaderType

/**
 * Adapter for showing a list of `AccountProviderDescription` items.
 *
 * Use [submitList] to add items to the adapter.
 */

class FilterableAccountListAdapter(
  private val imageLoader: ImageLoaderType,
  private val onItemClicked: (AccountProviderDescription) -> Unit
) : ListAdapter<AccountProviderDescription, AccountItemViewHolder>(DIFF_CALLBACK) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountItemViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val item = inflater.inflate(R.layout.account_list_item_card, parent, false)

    return AccountItemViewHolder(
      item,
      this.imageLoader,
      this.onItemClicked
    )
  }

  override fun onBindViewHolder(holder: AccountItemViewHolder, position: Int) {
    holder.bind(getItem(position))
  }
}

/**
 * Holder for rendering an `AccountProviderDescription` as a list item.
 */

class AccountItemViewHolder(
  val itemView: View,
  private val imageLoader: ImageLoaderType,
  private val onItemClicked: (AccountProviderDescription) -> Unit
) : RecyclerView.ViewHolder(itemView) {
  private val accountIcon: ImageView? =
    itemView.findViewById(R.id.accountIcon)
  private val accountTitleView =
    itemView.findViewById<TextView>(R.id.accountTitle)
  private val accountCaptionView =
    itemView.findViewById<TextView>(R.id.accountCaption)

  private var accountItem: AccountProviderDescription? = null

  init {
    this.itemView.findViewById<View>(R.id.account).setOnClickListener {
      this.accountItem?.let { account ->
        this.onItemClicked.invoke(account)
      }
    }
  }

  fun bind(item: AccountProviderDescription) {
    this.accountTitleView.text = item.title
    this.accountCaptionView.text = ""
    this.accountCaptionView.visibility =
      if (this.accountCaptionView.text.isNotEmpty()) {
        View.VISIBLE
      } else {
        View.GONE
      }
    if (this.accountIcon != null) {
      ImageAccountIcons.loadAccountLogoIntoView(
        loader = this.imageLoader.loader,
        account = item,
        defaultIcon = R.drawable.account_default,
        iconView = this.accountIcon
      )
    }
    this.accountItem = item
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
