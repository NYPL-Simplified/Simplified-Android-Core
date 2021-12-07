package org.nypl.simplified.ui.accounts

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.ui.accounts.databinding.AccountListItemBinding
import org.nypl.simplified.ui.images.ImageAccountIcons
import org.nypl.simplified.ui.images.ImageLoaderType

/**
 * An adapter for a list of accounts.
 */

class AccountListAdapter(
  private val imageLoader: ImageLoaderType,
  private val onItemClicked: (AccountType) -> Unit,
  private val onItemDeleteClicked: (AccountType) -> Unit
) : ListAdapter<AccountType, AccountListAdapter.AccountViewHolder>(AccountDiff) {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): AccountViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding: AccountListItemBinding = DataBindingUtil.inflate(inflater, R.layout.account_list_item, parent, false)
//    val binding = AccountListItemOldBinding.inflate(inflater, parent, false)
    return AccountViewHolder(
      binding,
      imageLoader,
      onItemClicked,
      onItemDeleteClicked
    )
  }

  override fun onBindViewHolder(
    holder: AccountViewHolder,
    position: Int
  ) = holder.bind(getItem(position), itemCount != 1)

  class AccountViewHolder(
    val binding: AccountListItemBinding,
    private val imageLoader: ImageLoaderType,
    private val onItemClicked: (AccountType) -> Unit,
    private val onItemDeleteClicked: (AccountType) -> Unit
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(account: AccountType, hasOptionsMenu: Boolean) {
      binding.account = account
      binding.executePendingBindings()

      binding.root.setOnClickListener {
        onItemClicked(account)
      }

      imageLoader.loader.cancelRequest(binding.accountIcon)
      ImageAccountIcons.loadAccountLogoIntoView(
        loader = imageLoader.loader,
        account = account.provider.toDescription(),
        defaultIcon = R.drawable.account_default,
        iconView = binding.accountIcon
      )

      if (hasOptionsMenu) {
        binding.popupMenuIcon.setOnClickListener {
          showPopupMenu(account)
        }
      } else {
        binding.popupMenuIcon.visibility = View.GONE
      }
    }

    fun showPopupMenu(account: AccountType) {
      val popupMenu =
        PopupMenu(binding.popupMenuIcon.context, binding.popupMenuIcon, Gravity.END)
          .apply {
            inflate(R.menu.account_list_menu_item)
          }

      popupMenu.setOnMenuItemClickListener {
        when (it.itemId) {
          R.id.menuItemDelete -> {
            onItemDeleteClicked(account)
          }
        }
        true
      }

      popupMenu.show()
    }
  }

  object AccountDiff : DiffUtil.ItemCallback<AccountType>() {
    override fun areItemsTheSame(oldItem: AccountType, newItem: AccountType): Boolean {
      return oldItem.id.compareTo(newItem.id) == 0
    }

    override fun areContentsTheSame(oldItem: AccountType, newItem: AccountType): Boolean {
      return oldItem.provider.displayName == newItem.provider.displayName &&
        oldItem.provider.subtitle == newItem.provider.subtitle &&
        oldItem.preferences.catalogURIOverride == newItem.preferences.catalogURIOverride
    }
  }
}
