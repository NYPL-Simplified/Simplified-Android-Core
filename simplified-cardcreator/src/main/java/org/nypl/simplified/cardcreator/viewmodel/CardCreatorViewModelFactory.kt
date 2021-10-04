package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.nypl.simplified.cardcreator.network.CardCreatorService

class CardCreatorViewModelFactory(
  private val cardCreatorService: CardCreatorService
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    return when (modelClass) {
      CardCreatorViewModel::class.java -> CardCreatorViewModel(cardCreatorService) as T
      AddressViewModel::class.java -> AddressViewModel(cardCreatorService) as T
      DependentEligibilityViewModel::class.java -> DependentEligibilityViewModel(cardCreatorService) as T
      PatronViewModel::class.java -> PatronViewModel(cardCreatorService) as T
      UsernameViewModel::class.java -> UsernameViewModel(cardCreatorService) as T
      else -> throw IllegalStateException("Can't create values of $modelClass")
    }
  }
}
