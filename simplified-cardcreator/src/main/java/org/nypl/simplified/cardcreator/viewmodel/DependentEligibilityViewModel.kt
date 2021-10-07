package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.DependentEligibilityResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.nypl.simplified.cardcreator.utils.Channel

class DependentEligibilityViewModel(
  private val cardCreatorService: CardCreatorService
) : ViewModel() {

  val pendingRequest = MutableLiveData(false)

  val dependentEligibilityResponse = Channel<DependentEligibilityResponse>()

  fun getDependentEligibility(identifier: String) {
    viewModelScope.launch {
      pendingRequest.value = true
      val response = cardCreatorService.getDependentEligibility(identifier)
      dependentEligibilityResponse.send(response)
      pendingRequest.value = false
    }
  }
}
