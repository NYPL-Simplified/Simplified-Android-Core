package org.nypl.simplified.cardcreator.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.models.Username
import org.nypl.simplified.cardcreator.models.ValidateUsernameResponse
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.slf4j.LoggerFactory
import java.lang.Exception

class UsernameViewModel : ViewModel() {

  private val logger = LoggerFactory.getLogger(UsernameViewModel::class.java)
  private val cardCreatorService = CardCreatorService()

  val validateUsernameResponse = MutableLiveData<ValidateUsernameResponse>()

  fun validateUsername(username: String) {
    viewModelScope.launch {
      try {
        val response = cardCreatorService.validateUsername(Username(username))
        validateUsernameResponse.postValue(response)
      } catch (e: Exception) {
        logger.debug("validateUsername call failed!")
        e.printStackTrace()
      }
    }
  }

}
