package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.Username
import org.nypl.simplified.cardcreator.model.ValidateUsernameResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService

class UsernameViewModel(
  private val cardCreatorService: CardCreatorService
) : ViewModel() {

  val validateUsernameResponse = MutableLiveData<ValidateUsernameResponse>()

  fun validateUsername(username: String) {
    viewModelScope.launch {
      val response = cardCreatorService.validateUsername(Username(username))
      validateUsernameResponse.postValue(response)
    }
  }
}
