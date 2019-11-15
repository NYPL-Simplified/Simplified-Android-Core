package org.nypl.simplified.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.accounts.api.AccountProviderDescriptionType

/**
 * An adapter for a list of account descriptions.
 */

class SettingsAccountProviderDescriptionAdapter(
  private val accounts: List<AccountProviderDescriptionType>,
  private val onItemClicked: (AccountProviderDescriptionType) -> Unit
) : RecyclerView.Adapter<SettingsAccountProviderDescriptionAdapter.AccountViewHolder>() {

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
    holder.parent.setOnClickListener { this.onItemClicked.invoke(account) }
    holder.accountTitleView.text = account.metadata.title
    holder.accountSubtitleView.text = ""
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