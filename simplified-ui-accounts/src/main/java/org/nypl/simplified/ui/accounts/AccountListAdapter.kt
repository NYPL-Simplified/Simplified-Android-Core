package org.nypl.simplified.ui.accounts

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
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
  private val onItemDeleteClicked: (AccountType) -> Unit
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
      onItemDeleteClicked
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
  private val onItemDeleteClicked: (AccountType) -> Unit
) : RecyclerView.ViewHolder(itemView) {
  private val accountIcon =
    itemView.findViewById<ImageView>(R.id.accountIcon)
  private val accountTitleView =
    itemView.findViewById<TextView>(R.id.accountTitle)
  private val accountCaptionView =
    itemView.findViewById<TextView>(R.id.accountCaption)
  private val popupMenuIcon =
    itemView.findViewById<View>(R.id.popupMenuIcon)

  private var accountItem: AccountType? = null

  init {
    this.itemView.setOnClickListener {
      this.accountItem?.let { account ->
        this.onItemClicked.invoke(account)
      }
    }

    val popupMenu =
      PopupMenu(this.popupMenuIcon.context, this.popupMenuIcon, Gravity.END)
        .apply {
          inflate(R.menu.account_list_item)
        }

    popupMenu.setOnMenuItemClickListener { menuItem ->
      when (menuItem.itemId) {
        R.id.menuItemDelete -> {
          this.accountItem?.let { account ->
            this.onItemDeleteClicked.invoke(account)
          }
        }
      }
      true
    }

    this.popupMenuIcon
      .apply {
        visibility = View.VISIBLE
      }
      .setOnClickListener {
        popupMenu.show()
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
