package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.Address
import org.nypl.simplified.cardcreator.model.ValidateAddressResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService

class AddressViewModel(
  private val cardCreatorService: CardCreatorService
) : ViewModel() {

  val validateAddressResponse = MutableLiveData<ValidateAddressResponse>()

  fun validateAddress(address: Address) {
    viewModelScope.launch {
      val response = cardCreatorService.validateAddress(address)
      validateAddressResponse.postValue(response)
    }
  }
}
