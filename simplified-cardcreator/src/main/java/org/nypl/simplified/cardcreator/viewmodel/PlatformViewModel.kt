package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.DependentEligibilityData
import org.nypl.simplified.cardcreator.model.JuvenilePatron
import org.nypl.simplified.cardcreator.model.JuvenilePatronResponse
import org.nypl.simplified.cardcreator.network.NYPLPlatformService
import org.slf4j.LoggerFactory
import retrofit2.HttpException
import java.lang.Exception

class PlatformViewModel : ViewModel() {

  private val logger = LoggerFactory.getLogger(PlatformViewModel::class.java)

  val dependentEligibilityData = MutableLiveData<DependentEligibilityData>()
  val juvenilePatronResponse = MutableLiveData<JuvenilePatronResponse>()

  val apiError = MutableLiveData<Int?>()

  fun getDependentEligibility(identifier: String, token: String) {
    viewModelScope.launch {
      try {
        val nyplPlatformService = NYPLPlatformService(token)
        if (isBarcode(identifier)) {
          val response =
            nyplPlatformService.getDependentEligibilityWithBarcode(identifier)
          dependentEligibilityData.postValue(response)
        } else {
          val response =
            nyplPlatformService.getDependentEligibilityWithUsername(identifier)
          dependentEligibilityData.postValue(response)
        }
      } catch (e: Exception) {
        logger.error("attempt to check dependent eligibility call failed!", e)
        when (e) {
          is HttpException -> { apiError.postValue(e.code()) }
          else -> { apiError.postValue(null) }
        }
      }
    }
  }

  fun createJuvenileCard(juvenilePatron: JuvenilePatron, token: String) {
    viewModelScope.launch {
      try {
        val nyplPlatformService = NYPLPlatformService(token)
        val response =
          nyplPlatformService.createJuvenilePatron(juvenilePatron)
        juvenilePatronResponse.postValue(response)
      } catch (e: Exception) {
        logger.error("attempt to create a juvenile patron call failed!", e)
        when (e) {
          is HttpException -> { apiError.postValue(e.code()) }
          else -> { apiError.postValue(null) }
        }
      }
    }
  }

  /**
   * Determines whether or not the user identifier is a barcode or username
   */
  private fun isBarcode(identifier: String): Boolean {
    return identifier.toDoubleOrNull() != null
  }
}
