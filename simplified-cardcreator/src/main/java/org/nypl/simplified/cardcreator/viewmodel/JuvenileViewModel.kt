package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.DependentEligibilityResponse
import org.nypl.simplified.cardcreator.model.IdentifierParent
import org.nypl.simplified.cardcreator.model.JuvenilePatronResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService

class JuvenileViewModel(
  private val cardCreatorService: CardCreatorService
) : ViewModel() {

  val dependentEligibilityResponse = MutableLiveData<DependentEligibilityResponse>()
  val juvenilePatronResponse = MutableLiveData<JuvenilePatronResponse>()

  fun getDependentEligibility(identifier: String) {
    viewModelScope.launch {
      val response = cardCreatorService.getDependentEligibility(identifier)
      dependentEligibilityResponse.postValue(response)
    }
  }

  fun createJuvenileCard(juvenilePatron: IdentifierParent) {
    viewModelScope.launch {
      val response = cardCreatorService.createJuvenileCard(juvenilePatron)
      juvenilePatronResponse.postValue(response)
    }
  }
}
