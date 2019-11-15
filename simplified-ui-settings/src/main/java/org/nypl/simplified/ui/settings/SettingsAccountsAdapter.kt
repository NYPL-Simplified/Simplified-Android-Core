package org.nypl.simplified.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.accounts.database.api.AccountType

/**
 * An adapter for a list of accounts.
 */

class SettingsAccountsAdapter(
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
    holder.parent.setOnClickListener {
      this.onItemClicked.invoke(account)
    }
    holder.parent.setOnLongClickListener {
      this.onItemLongClicked.invoke(account)
      true
    }
    holder.accountTitleView.text = account.provider.displayName
    holder.accountSubtitleView.text = account.provider.subtitle
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