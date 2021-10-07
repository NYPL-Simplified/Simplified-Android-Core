package org.nypl.simplified.cardcreator.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import org.nypl.simplified.cardcreator.model.Address
import org.nypl.simplified.cardcreator.model.CreatePatronResponse
import org.nypl.simplified.cardcreator.model.DependentEligibilityResponse
import org.nypl.simplified.cardcreator.model.ISSOTokenData
import org.nypl.simplified.cardcreator.model.IdentifierParent
import org.nypl.simplified.cardcreator.model.JuvenilePatronResponse
import org.nypl.simplified.cardcreator.model.Patron
import org.nypl.simplified.cardcreator.model.Username
import org.nypl.simplified.cardcreator.model.ValidateAddressRequest
import org.nypl.simplified.cardcreator.model.ValidateAddressResponse
import org.nypl.simplified.cardcreator.model.ValidateUsernameResponse
import org.slf4j.LoggerFactory

class CardCreatorService(
  private val clientId: String,
  private val clientSecret: String
) {

  private val logger = LoggerFactory.getLogger(CardCreatorService::class.java)

  private var timedPlatformToken: Pair<ISSOTokenData, Long>? = null

  private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  /**
   * Returns whether the token is (very likely to be) still valid.
   */
  private val Pair<ISSOTokenData, Long>.isValid
    get() = System.currentTimeMillis() - this.second < this.first.expires_in * 1000 - 6000

  private suspend fun getToken(): String {
    if (timedPlatformToken?.isValid != true) {
      val response = NYPLISSOService().getToken(
        MultipartBody.Part.createFormData("grant_type", "client_credentials"),
        MultipartBody.Part.createFormData("client_id", clientId),
        MultipartBody.Part.createFormData("client_secret", clientSecret)
      )
      timedPlatformToken = response.body()!! to response.raw().receivedResponseAtMillis
    }
    return timedPlatformToken!!.first.access_token
  }

  suspend fun validateUsername(username: Username): ValidateUsernameResponse {
    return try {
      val token = getToken()
      val nyplPlatformService = NYPLPlatformService(token)
      val response = nyplPlatformService.validateUsername(username)
      if (response.isSuccessful) {
        response.body()!!.validate()
      } else {
        val errorBody = response.errorBody()!!.string()
        logger.error("validateUsername call returned an error!\n$errorBody")
        val adapter = moshi.adapter(ValidateUsernameResponse.ValidateUsernameError::class.java)
        adapter.fromJson(errorBody)!!.validate()
      }
    } catch (e: Exception) {
      logger.error("an unexpected exception occurred while trying to validate username", e)
      ValidateUsernameResponse.ValidateUsernameException(e)
    }
  }

  suspend fun validateAddress(address: Address): ValidateAddressResponse {
    return try {
      val token = getToken()
      val nyplPlatformService = NYPLPlatformService(token)
      val response = nyplPlatformService.validateAddress(ValidateAddressRequest(address))
      return if (response.isSuccessful) {
        response.body()!!.validate()
      } else try {
        val errorBody = response.errorBody()!!.string()
        logger.error("validateAddress call returned an error!\n$errorBody")
        val adapter = moshi.adapter(ValidateAddressResponse.AlternateAddressesError::class.java)
        adapter.fromJson(errorBody)!!.validate()
      } catch (e: Exception) {
        val errorBody = response.errorBody()!!.string()
        val adapter = moshi.adapter(ValidateAddressResponse.ValidateAddressError::class.java)
        adapter.fromJson(errorBody)!!.validate()
      }
    } catch (e: Exception) {
      logger.error("an unexpected exception occurred while trying to validate address", e)
      ValidateAddressResponse.ValidateAddressException(e)
    }
  }

  suspend fun createPatron(patron: Patron): CreatePatronResponse {
    return try {
      val token = getToken()
      val nyplPlatformService = NYPLPlatformService(token)
      val response = nyplPlatformService.createPatron(patron)
      if (response.isSuccessful) {
        response.body()!!.validate()
      } else {
        val errorBody = response.errorBody()!!.string()
        logger.error("createPatron call returned an error!\n$errorBody")
        val adapter = moshi.adapter(CreatePatronResponse.CreatePatronHttpError::class.java)
        adapter.fromJson(errorBody)!!.validate()
      }
    } catch (e: Exception) {
      logger.error("an unexpected exception occurred while trying to create a patron", e)
      CreatePatronResponse.CreatePatronException(e)
    }
  }

  suspend fun getDependentEligibility(
    identifier: String
  ): DependentEligibilityResponse {

    /**
     * Determines whether or not the user identifier is a barcode or username
     */
    fun String.isBarcode(): Boolean {
      return this.toDoubleOrNull() != null
    }

    return try {
      val token = getToken()
      val nyplPlatformService = NYPLPlatformService(token)
      val response =
        if (identifier.isBarcode()) {
          nyplPlatformService.getDependentEligibilityWithBarcode(identifier)
        } else {
          nyplPlatformService.getDependentEligibilityWithUsername(identifier)
        }
      if (response.isSuccessful) {
        response.body()!!.validate()
      } else {
        val errorBody = response.errorBody()!!.string()
        logger.error("getDependentEligibility call returned an error!\n$errorBody")
        val adapter = moshi.adapter(DependentEligibilityResponse.DependentEligibilityError::class.java)
        adapter.fromJson(errorBody)!!.validate()
      }
    } catch (e: Exception) {
      logger.error("an unexpected exception occurred while trying to create a patron", e)
      DependentEligibilityResponse.DependentEligibilityException(e)
    }
  }

  suspend fun createJuvenileCard(
    juvenileParent: IdentifierParent
  ): JuvenilePatronResponse {
    return try {
      val token = getToken()
      val nyplPlatformService = NYPLPlatformService(token)
      val response = when (juvenileParent) {
        is IdentifierParent.BarcodeParent ->
          nyplPlatformService.createJuvenilePatronWithBarcodeParent(juvenileParent)
        is IdentifierParent.UsernameParent ->
          nyplPlatformService.createJuvenilePatronWithUsernameParent(juvenileParent)
      }
      if (response.isSuccessful) {
        response.body()!!.validate()
      } else {
        val errorBody = response.errorBody()!!.string()
        logger.error("createJuvenileCard call returned an error!\n$errorBody")
        val adapter = moshi.adapter(JuvenilePatronResponse.JuvenilePatronError::class.java)
        adapter.fromJson(errorBody)!!.validate()
      }
    } catch (e: Exception) {
      logger.error("an unexpected exception occurred while trying to create a dependent patron", e)
      JuvenilePatronResponse.JuvenilePatronException(e)
    }
  }
}
