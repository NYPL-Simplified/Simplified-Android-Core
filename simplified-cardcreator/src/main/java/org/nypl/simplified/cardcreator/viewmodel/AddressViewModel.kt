package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.Address
import org.nypl.simplified.cardcreator.model.ValidateAddressResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.nypl.simplified.cardcreator.utils.Channel

class AddressViewModel(
  private val cardCreatorService: CardCreatorService
) : ViewModel() {

  val pendingRequest = MutableLiveData(false)

  val validateAddressResponse = Channel<ValidateAddressResponse>()

  fun validateAddress(address: Address) {
    viewModelScope.launch {
      pendingRequest.value = true
      val response = cardCreatorService.validateAddress(address)
      validateAddressResponse.send(response)
      pendingRequest.value = false
    }
  }
}
