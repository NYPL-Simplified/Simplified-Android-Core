package org.nypl.simplified.ui.accounts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.ui.images.ImageAccountIcons
import org.nypl.simplified.ui.images.ImageLoaderType

/**
 * An adapter for a list of accounts.
 */

class AccountListAdapter(
  private val imageLoader: ImageLoaderType,
  private val accounts: List<AccountType>,
  private val onItemClicked: (AccountType) -> Unit,
  private val onItemLongClicked: (AccountType) -> Unit
) : RecyclerView.Adapter<AccountViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): AccountViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val itemView = inflater.inflate(R.layout.account_list_item, parent, false)
    return AccountViewHolder(
      itemView,
      imageLoader,
      onItemClicked,
      onItemLongClicked
    )
  }

  override fun getItemCount(): Int {
    return this.accounts.size
  }

  override fun onBindViewHolder(
    holder: AccountViewHolder,
    position: Int
  ) {
    holder.bind(this.accounts[position])
  }
}

class AccountViewHolder(
  val itemView: View,
  private val imageLoader: ImageLoaderType,
  private val onItemClicked: (AccountType) -> Unit,
  private val onItemLongClicked: (AccountType) -> Unit
) : RecyclerView.ViewHolder(itemView) {
  private val accountIcon =
    itemView.findViewById<ImageView>(R.id.accountIcon)
  private val accountTitleView =
    itemView.findViewById<TextView>(R.id.accountTitle)
  private val accountCaptionView =
    itemView.findViewById<TextView>(R.id.accountCaption)

  private var accountItem: AccountType? = null

  init {
    this.itemView.setOnClickListener {
      this.accountItem?.let { account ->
        this.onItemClicked.invoke(account)
      }
    }
    this.itemView.setOnLongClickListener {
      this.accountItem?.let { account ->
        this.onItemLongClicked.invoke(account)
      }
      true
    }
  }

  fun bind(item: AccountType) {
    this.accountTitleView.text = item.provider.displayName
    this.accountCaptionView.text = item.provider.subtitle

    item.preferences.catalogURIOverride?.let { uri ->
      this.accountCaptionView.text = uri.toString()
    }

    this.accountCaptionView.visibility =
      if (this.accountCaptionView.text.isNotEmpty()) {
        View.VISIBLE
      } else {
        View.GONE
      }

    ImageAccountIcons.loadAccountLogoIntoView(
      loader = this.imageLoader.loader,
      account = item.provider.toDescription(),
      defaultIcon = R.drawable.account_default,
      iconView = this.accountIcon
    )
    this.accountItem = item
  }
}
