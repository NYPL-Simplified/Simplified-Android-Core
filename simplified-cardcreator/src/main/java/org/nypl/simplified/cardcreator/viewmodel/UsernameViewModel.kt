package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.Username
import org.nypl.simplified.cardcreator.model.ValidateUsernameResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.slf4j.LoggerFactory
import retrofit2.HttpException

class UsernameViewModel : ViewModel() {

  private val logger = LoggerFactory.getLogger(UsernameViewModel::class.java)

  val validateUsernameResponse = MutableLiveData<ValidateUsernameResponse>()
  val apiError = MutableLiveData<Int?>()

  fun validateUsername(username: String, authUsername: String, authPassword: String) {
    viewModelScope.launch {
      try {
        val cardCreatorService = CardCreatorService(authUsername, authPassword)
        val response = cardCreatorService.validateUsername(Username(username))
        validateUsernameResponse.postValue(response)
      } catch (e: Exception) {
        logger.error("validateUsername call failed!", e)
        when (e) {
          is HttpException -> { apiError.postValue(e.code()) }
          else -> { apiError.postValue(null) }
        }
      }
    }
  }
}
