package org.nypl.simplified.cardcreator.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.models.CreatePatronResponse
import org.nypl.simplified.cardcreator.models.Patron
import org.nypl.simplified.cardcreator.network.CardCreatorService
import org.slf4j.LoggerFactory
import java.lang.Exception

class PatronViewModel : ViewModel() {

  private val logger = LoggerFactory.getLogger(PatronViewModel::class.java)
  private val cardCreatorService = CardCreatorService()

  val createPatronResponse = MutableLiveData<CreatePatronResponse>()

  fun createPatron(patron: Patron) {
    viewModelScope.launch {
      try {
        val response = cardCreatorService.createPatron(patron)
        createPatronResponse.postValue(response)
      } catch (e: Exception) {
        logger.debug("createPatron call failed!")
        e.printStackTrace()
      }
    }
  }

}
