package org.nypl.simplified.ui.accounts

import android.view.View
import android.widget.TextView
import androidx.databinding.BindingAdapter
import org.nypl.simplified.accounts.database.api.AccountType

@BindingAdapter("accountCaptionText")
fun TextView.accountCaptionText(account: AccountType) {
  text = account.preferences.catalogURIOverride?.toString() ?: account.provider.subtitle
  visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
}
