package org.nypl.simplified.cardcreator.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.nypl.simplified.cardcreator.model.ISSOTokenData
import org.nypl.simplified.cardcreator.network.NYPLISSOService
import org.slf4j.LoggerFactory
import retrofit2.HttpException
import java.lang.Exception

class TokenViewModel : ViewModel() {

  private val logger = LoggerFactory.getLogger(TokenViewModel::class.java)

  val issoTokenData = MutableLiveData<ISSOTokenData>()
  val apiError = MutableLiveData<Int?>()

  fun getToken(clientId: String, clientSecret: String) {
    viewModelScope.launch {
      try {
        val nyplISSOService = NYPLISSOService()
        val response = nyplISSOService.getToken(
          MultipartBody.Part.createFormData("grant_type", "client_credentials"),
          MultipartBody.Part.createFormData("client_id", clientId),
          MultipartBody.Part.createFormData("client_secret", clientSecret)
        )
        issoTokenData.postValue(response)
      } catch (e: Exception) {
        logger.error("attempt to get NYPL platform token call failed!", e)
        when (e) {
          is HttpException -> { apiError.postValue(e.code()) }
          else -> { apiError.postValue(null) }
        }
      }
    }
  }
}
