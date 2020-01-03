package org.nypl.simplified.ui.settings

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

class SettingsAccountsAdapter(
  private val imageLoader: ImageLoaderType,
  private val accounts: List<AccountType>,
  private val onItemClicked: (AccountType) -> Unit,
  private val onItemLongClicked: (AccountType) -> Unit
) : RecyclerView.Adapter<SettingsAccountsAdapter.AccountViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): AccountViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.settings_account_cell, parent, false)
    return this.AccountViewHolder(item)
  }

  override fun getItemCount(): Int {
    return this.accounts.size
  }

  override fun onBindViewHolder(
    holder: AccountViewHolder,
    position: Int
  ) {
    val account = this.accounts[position]

    holder.accountIcon.setImageDrawable(null)
    holder.accountTitleView.text = account.provider.displayName
    holder.accountSubtitleView.text = account.provider.subtitle
    holder.parent.setOnClickListener {
      this.onItemClicked.invoke(account)
    }
    holder.parent.setOnLongClickListener {
      this.onItemLongClicked.invoke(account)
      true
    }

    ImageAccountIcons.loadAccountLogoIntoView(
      loader = this.imageLoader.loader,
      account = account.provider.toDescription(),
      defaultIcon = R.drawable.account_default,
      iconView = holder.accountIcon
    )
  }

  inner class AccountViewHolder(val parent: View) : RecyclerView.ViewHolder(parent) {
    val accountIcon =
      parent.findViewById<ImageView>(R.id.accountCellIcon)
    val accountTitleView =
      parent.findViewById<TextView>(R.id.accountCellTitle)
    val accountSubtitleView =
      parent.findViewById<TextView>(R.id.accountCellSubtitle)
  }
}