package org.nypl.simplified.ui.catalog

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.accounts.database.api.AccountType

class CatalogAccountViewHolder(
  val parent: View
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
    this.parent.setOnClickListener { onItemClicked.invoke(account) }
    this.accountTitleView.text = account.provider.displayName
    this.accountSubtitleView.text = account.provider.subtitle
  }
}