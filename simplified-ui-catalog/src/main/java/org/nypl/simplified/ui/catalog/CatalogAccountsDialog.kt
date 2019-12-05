package org.nypl.simplified.ui.catalog

import android.content.Context
import android.view.LayoutInflater
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.profiles.controller.api.ProfilesControllerType
import org.nypl.simplified.ui.images.ImageLoaderType
import org.slf4j.LoggerFactory

/**
 * Functions to open account selection dialogs.
 */

object CatalogAccountsDialog {

  private val logger = LoggerFactory.getLogger(CatalogAccountsDialog::class.java)

  @UiThread
  fun openAccountsDialog(
    context: Context,
    toolbar: Toolbar,
    profilesController: ProfilesControllerType,
    imageLoader: ImageLoaderType,
    onAccountSelected: (AccountType) -> Unit
  ) {
    val inflater =
      LayoutInflater.from(context)
    val dialogView =
      inflater.inflate(R.layout.accounts_dialog, toolbar, false)

    val listView = dialogView.findViewById<RecyclerView>(R.id.accountsDialogList)
    listView.setHasFixedSize(true)
    listView.setItemViewCacheSize(32)
    listView.layoutManager = LinearLayoutManager(context)
    (listView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

    val alertBuilder = AlertDialog.Builder(context)
    alertBuilder.setTitle(R.string.catalogAccounts)
    alertBuilder.setView(dialogView)

    val alertDialog = alertBuilder.create()

    listView.adapter =
      CatalogAccountsAdapter(
        accounts = this.listAccounts(profilesController),
        imageLoader = imageLoader,
        onItemClicked = { account ->
          profilesController.profileAccountSelectByProvider(account.provider.id)
          onAccountSelected.invoke(account)
          alertDialog.dismiss()
        })

    alertDialog.show()
  }

  private fun listAccounts(
    profilesController: ProfilesControllerType
  ): List<AccountType> {
    return profilesController.profileCurrent()
      .accounts()
      .values
      .filter { account -> !this.accountIsCurrent(profilesController, account) }
      .sortedBy { account -> account.provider.displayName }
  }

  private fun accountIsCurrent(
    profilesController: ProfilesControllerType,
    account: AccountType
  ): Boolean {
    return try {
      profilesController.profileAccountCurrent().id == account.id
    } catch (e: Exception) {
      this.logger.error("could not retrieve current account: ", e)
      false
    }
  }
}