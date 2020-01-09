package org.nypl.simplified.ui.catalog

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.ui.images.ImageAccountIcons
import org.nypl.simplified.ui.images.ImageLoaderType

class CatalogAccountViewHolder(
  val parent: View,
  val imageLoader: ImageLoaderType
) : RecyclerView.ViewHolder(parent) {

  private val accountIcon: ImageView =
    this.parent.findViewById(R.id.accountCellIcon)
  private val accountTitleView: TextView =
    this.parent.findViewById(R.id.accountCellTitle)
  private val accountSubtitleView: TextView =
    this.parent.findViewById(R.id.accountCellSubtitle)

  fun bindTo(
    account: AccountType,
    onItemClicked: (AccountType) -> Unit
  ) {
    ImageAccountIcons.loadAccountLogoIntoView(
      loader = this.imageLoader.loader,
      account = account.provider.toDescription(),
      defaultIcon = R.drawable.account_default,
      iconView = this.accountIcon
    )

    this.parent.setOnClickListener { onItemClicked.invoke(account) }
    this.accountTitleView.text = account.provider.displayName
    this.accountTitleView.visibility = View.VISIBLE
    this.accountSubtitleView.text = account.provider.subtitle
    this.accountSubtitleView.visibility = View.VISIBLE
  }
}
