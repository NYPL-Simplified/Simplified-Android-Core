package org.nypl.simplified.ui.catalog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.accounts.database.api.AccountType

/**
 * An adapter for a list of accounts.
 */

class CatalogAccountsAdapter(
  private val accounts: List<AccountType>,
  private val onItemClicked: (AccountType) -> Unit
) : RecyclerView.Adapter<CatalogAccountViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): CatalogAccountViewHolder {
    val inflater =
      LayoutInflater.from(parent.context)
    val item =
      inflater.inflate(R.layout.account_cell, parent, false)
    return CatalogAccountViewHolder(item)
  }

  override fun getItemCount(): Int {
    return this.accounts.size
  }

  override fun onBindViewHolder(
    holder: CatalogAccountViewHolder,
    position: Int
  ) {
    holder.bindTo(this.accounts[position], this.onItemClicked)
  }
}
