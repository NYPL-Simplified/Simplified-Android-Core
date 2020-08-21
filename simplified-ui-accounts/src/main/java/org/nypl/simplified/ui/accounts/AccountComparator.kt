package org.nypl.simplified.ui.accounts

import org.nypl.simplified.accounts.api.AccountReadableType
import java.text.Collator

/**
 * Comparator used for sorting a list of accounts when displayed to a patron.
 */

class AccountComparator : Comparator<AccountReadableType> {

  private val stopwords = arrayOf("A ", "The ")

  private val collator = Collator.getInstance().apply {
    decomposition = Collator.CANONICAL_DECOMPOSITION
    strength = Collator.SECONDARY
  }

  override fun compare(a1: AccountReadableType, a2: AccountReadableType): Int {
    var name1 = a1.provider.displayName
    var name2 = a2.provider.displayName

    stopwords.forEach {
      name1 = name1.removePrefix(it)
      name2 = name2.removePrefix(it)
    }
    return collator.compare(name1, name2)
  }
}
