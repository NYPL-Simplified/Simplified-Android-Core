package org.nypl.simplified.cardcreator.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.nypl.simplified.cardcreator.model.CreatePatronResponse
import org.nypl.simplified.cardcreator.model.DependentEligibilityResponse
import org.nypl.simplified.cardcreator.model.IdentifierParent
import org.nypl.simplified.cardcreator.model.JuvenilePatronResponse
import org.nypl.simplified.cardcreator.model.Patron
import org.nypl.simplified.cardcreator.model.Username
import org.nypl.simplified.cardcreator.model.ValidateAddressRequest
import org.nypl.simplified.cardcreator.model.ValidateAddressResponse
import org.nypl.simplified.cardcreator.model.ValidateUsernameResponse
import org.nypl.simplified.cardcreator.utils.Constants
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

internal interface NYPLPlatformService {

  /**
   * Validates the patron's username, confirming both its format and its availability.
   *
   * Docs: https://platformdocs.nypl.org/#/validations/usernameV0_3
   */
  @POST("validations/username")
  suspend fun validateUsername(
    @Body username: Username
  ): Response<ValidateUsernameResponse.ValidateUsernameData>

  /**
   * Validates the patron's address.
   *
   * Docs: https://platformdocs.nypl.org/#/validations/addressV03
   */
  @POST("validations/address")
  suspend fun validateAddress(
    @Body address: ValidateAddressRequest
  ): Response<ValidateAddressResponse.ValidateAddressData>

  /**
   * Creates a library card for a patron.
   *
   * Docs: https://platformdocs.nypl.org/#/patrons/patrons_creatorV03
   */
  @POST("patrons")
  suspend fun createPatron(
    @Body patron: Patron
  ): Response<CreatePatronResponse.CreatePatronData>

  /**
   * Gets whether or not user can create juvenile cards using barcode
   *
   * Docs: https://platformdocs.nypl.org/#/patrons/patrons_eligibilityV03
   */
  @GET("patrons/dependent-eligibility")
  suspend fun getDependentEligibilityWithBarcode(
    @Query("barcode") barcode: String
  ): Response<DependentEligibilityResponse.DependentEligibilityData>

  /**
   * Gets whether or not user can create juvenile cards using username
   *
   * Docs: https://platformdocs.nypl.org/#/patrons/patrons_eligibilityV03
   */
  @GET("patrons/dependent-eligibility")
  suspend fun getDependentEligibilityWithUsername(
    @Query("username") username: String
  ): Response<DependentEligibilityResponse.DependentEligibilityData>

  /**
   * Creates juvenile card
   *
   * Docs: https://platformdocs.nypl.org/#/patrons/patrons_dependentsV03
   */
  @POST("patrons/dependents")
  suspend fun createJuvenilePatronWithBarcodeParent(
    @Body juvenilePatron: IdentifierParent.BarcodeParent
  ): Response<JuvenilePatronResponse.JuvenilePatronData>

  @POST("patrons/dependents")
  suspend fun createJuvenilePatronWithUsernameParent(
    @Body juvenilePatron: IdentifierParent.UsernameParent
  ): Response<JuvenilePatronResponse.JuvenilePatronData>

  companion object {
    operator fun invoke(token: String): NYPLPlatformService {
      val authInterceptor = Interceptor {
        val request = it.request().newBuilder()
          .addHeader("Authorization", "Bearer $token")
          .addHeader("Content-Type", "application/json")
          .addHeader("Accept", "application/json").build()
        it.proceed(request)
      }

      val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
      }

      val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

      val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

      return Retrofit.Builder()
        .client(client)
        .baseUrl(Constants.NYPL_PROD_PLATFORM_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(NYPLPlatformService::class.java)
    }
  }
}
