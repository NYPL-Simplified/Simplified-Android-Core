package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.launch
import org.nypl.simplified.cardcreator.model.DependentEligibilityError
import org.nypl.simplified.cardcreator.model.DependentEligibilityData
import org.nypl.simplified.cardcreator.model.BarcodeParent
import org.nypl.simplified.cardcreator.model.UsernameParent
import org.nypl.simplified.cardcreator.model.JuvenilePatronResponse
import org.nypl.simplified.cardcreator.network.NYPLPlatformService
import org.slf4j.LoggerFactory
import retrofit2.HttpException

class PlatformViewModel : ViewModel() {

  private val logger = LoggerFactory.getLogger(PlatformViewModel::class.java)

  val dependentEligibilityData = MutableLiveData<DependentEligibilityData>()
  val juvenilePatronResponse = MutableLiveData<JuvenilePatronResponse>()

  val apiErrorMessage = MutableLiveData<String?>()
  val apiError = MutableLiveData<Int?>()

  fun getDependentEligibility(identifier: String, token: String) {
    val apiErrorAdapter: JsonAdapter<DependentEligibilityError> = Moshi.Builder().build().adapter(DependentEligibilityError::class.java)
    viewModelScope.launch {
      val nyplPlatformService = NYPLPlatformService(token)
      if (isBarcode(identifier)) {
        val response = nyplPlatformService.getDependentEligibilityWithBarcode(identifier)
        if (response.isSuccessful) {
          dependentEligibilityData.postValue(response.body())
        } else {
          val error = apiErrorAdapter.fromJson(response.errorBody()?.string())?.message
          apiErrorMessage.postValue(error)
        }
      } else {
        val response = nyplPlatformService.getDependentEligibilityWithUsername(identifier)
        if (response.isSuccessful) {
          dependentEligibilityData.postValue(response.body())
        } else {
          val error = apiErrorAdapter.fromJson(response.errorBody()?.string())?.message
          apiErrorMessage.postValue(error)
        }
      }
    }
  }

  fun createJuvenileCardWithUsernameParent(juvenilePatron: UsernameParent, token: String) {
    viewModelScope.launch {
      try {
        val nyplPlatformService = NYPLPlatformService(token)
        val response =
          nyplPlatformService.createJuvenilePatronWithUsernameParent(juvenilePatron)
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

  fun createJuvenileCardWithBarcodeParent(juvenilePatron: BarcodeParent, token: String) {
    val apiErrorAdapter: JsonAdapter<DependentEligibilityError> = Moshi.Builder().build().adapter(DependentEligibilityError::class.java)
    viewModelScope.launch {
      val nyplPlatformService = NYPLPlatformService(token)
      val response =
        nyplPlatformService.createJuvenilePatronWithBarcodeParent(juvenilePatron)
      if (response.isSuccessful) {
        juvenilePatronResponse.postValue(response.body())
      } else {
        val error = apiErrorAdapter.fromJson(response.errorBody()?.string())?.message
        apiErrorMessage.postValue(error)
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
