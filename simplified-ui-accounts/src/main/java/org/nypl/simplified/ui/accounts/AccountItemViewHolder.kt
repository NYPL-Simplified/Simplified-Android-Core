package org.nypl.simplified.ui.accounts

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.accounts.api.AccountProviderDescription
import org.nypl.simplified.ui.images.ImageAccountIcons
import org.nypl.simplified.ui.images.ImageLoaderType

/**
 * Holder for rendering an `AccountProviderDescription` as a list item.
 */

class AccountItemViewHolder(
  val itemView: View,
  private val imageLoader: ImageLoaderType,
  private val onItemClicked: (AccountProviderDescription) -> Unit
) : RecyclerView.ViewHolder(itemView) {
  private val accountIcon =
    itemView.findViewById<ImageView>(R.id.accountCellIcon)
  private val accountTitleView =
    itemView.findViewById<TextView>(R.id.accountCellTitle)
  private val accountSubtitleView =
    itemView.findViewById<TextView>(R.id.accountCellSubtitle)

  private var accountItem: AccountProviderDescription? = null

  init {
    this.itemView.setOnClickListener {
      this.accountItem?.let { account ->
        this.onItemClicked.invoke(account)
      }
    }
  }

  fun bind(item: AccountProviderDescription) {
    this.accountTitleView.text = item.title
    this.accountSubtitleView.text = ""

    ImageAccountIcons.loadAccountLogoIntoView(
      loader = this.imageLoader.loader,
      account = item,
      defaultIcon = R.drawable.account_default,
      iconView = this.accountIcon
    )

    this.accountItem = item
  }
}
