package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.CreatePatronResponse
import org.nypl.simplified.cardcreator.model.Patron
import org.nypl.simplified.cardcreator.network.CardCreatorService

class PatronViewModel(
  private val cardCreatorService: CardCreatorService
) : ViewModel() {

  val createPatronResponse = MutableLiveData<CreatePatronResponse>()

  fun createPatron(patron: Patron) {
    viewModelScope.launch {
      val response = cardCreatorService.createPatron(patron)
      createPatronResponse.postValue(response)
    }
  }
}
