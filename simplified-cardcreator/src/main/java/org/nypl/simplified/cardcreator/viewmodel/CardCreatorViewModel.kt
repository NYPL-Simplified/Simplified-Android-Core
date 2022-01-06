package org.nypl.simplified.cardcreator.viewmodel

import android.location.Address
import androidx.lifecycle.ViewModel
import org.nypl.simplified.cardcreator.network.CardCreatorService

class CardCreatorViewModel(
  val cardCreatorService: CardCreatorService
) : ViewModel() {

  var userLocationAddress: Address? = null
}
