package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.Username
import org.nypl.simplified.cardcreator.model.UsernameVerificationResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.nypl.simplified.cardcreator.utils.Channel

class AccountInformationViewModel(
  private val cardCreatorService: CardCreatorService
) : ViewModel() {
  val validateUsernameResponse = Channel<UsernameVerificationResponse>()

  private val _state = MutableStateFlow(State())
  val state = _state.asStateFlow()

  private val passwordRegex = "^[a-zA-Z0-9]{8,32}$".toRegex()
  private val usernameRegex = "^\\S{5,25}$".toRegex()

  fun verifyUsername(username: String) {
    viewModelScope.launch {
      _state.value = _state.value.copy(pendingRequest = true)
      val response = cardCreatorService.validateUsername(Username(username))
      validateUsernameResponse.send(response)
      _state.value = _state.value.copy(pendingRequest = false)
    }
  }

  fun validatePassword(password: String) {
    _state.value = _state.value.copy(validPassword = password.matches(passwordRegex))
  }

  fun validateUsername(username: String) {
    _state.value = _state.value.copy(validUsername = username.matches(usernameRegex))
  }

  data class State(
    val validPassword: Boolean = false,
    val validUsername: Boolean = false,
    val pendingRequest: Boolean = false
  ) {
    fun isReadyToVerify() = validPassword && validUsername
  }
}
