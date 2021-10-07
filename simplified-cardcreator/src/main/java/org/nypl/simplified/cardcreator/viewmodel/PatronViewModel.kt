package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.CreatePatronResponse
import org.nypl.simplified.cardcreator.model.IdentifierParent
import org.nypl.simplified.cardcreator.model.JuvenilePatronResponse
import org.nypl.simplified.cardcreator.model.Patron
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.nypl.simplified.cardcreator.utils.Channel

class PatronViewModel(
  private val cardCreatorService: CardCreatorService
) : ViewModel() {

  val pendingRequest = MutableLiveData(false)

  val createPatronResponse = Channel<CreatePatronResponse>()

  fun createPatron(patron: Patron) {
    viewModelScope.launch {
      pendingRequest.value = true
      val response = cardCreatorService.createPatron(patron)
      createPatronResponse.send(response)
      pendingRequest.value = false
    }
  }

  val juvenilePatronResponse = Channel<JuvenilePatronResponse>()

  fun createJuvenileCard(juvenilePatron: IdentifierParent) {
    viewModelScope.launch {
      pendingRequest.value = true
      val response = cardCreatorService.createJuvenileCard(juvenilePatron)
      juvenilePatronResponse.send(response)
      pendingRequest.value = false
    }
  }
}
