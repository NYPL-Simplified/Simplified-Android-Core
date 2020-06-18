package org.nypl.simplified.cardcreator.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.nypl.simplified.cardcreator.model.DependentEligibilityData
import org.nypl.simplified.cardcreator.model.JuvenilePatron
import org.nypl.simplified.cardcreator.model.JuvenilePatronResponse
import org.nypl.simplified.cardcreator.utils.Constants
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

internal interface NYPLPlatformService {

  /**
   * Gets whether or not user can create juvenile cards using barcode
   *
   * Docs: https://platformdocs.nypl.org/#/patrons/patrons_eligibilityV03
   */
  @GET("patrons/dependent-eligibility")
  suspend fun getDependentEligibilityWithBarcode(
    @Query("barcode") barcode: String
  ): DependentEligibilityData

  /**
   * Gets whether or not user can create juvenile cards using username
   *
   * Docs: https://platformdocs.nypl.org/#/patrons/patrons_eligibilityV03
   */
  @GET("patrons/dependent-eligibility")
  suspend fun getDependentEligibilityWithUsername(
    @Query("username") username: String
  ): DependentEligibilityData

  /**
   * Creates juvenile card
   *
   * Docs: https://platformdocs.nypl.org/#/patrons/patrons_dependentsV03
   */
  @POST("patrons/dependents")
  suspend fun createJuvenilePatron(
    @Body juvenilePatron: JuvenilePatron
  ): JuvenilePatronResponse

  companion object {
    operator fun invoke(token: String): NYPLPlatformService {

      val authInterceptor = Interceptor {
        val request = it.request().newBuilder()
          .addHeader("Authorization", "Bearer $token")
          .addHeader("Content-Type", "application/json")
          .addHeader("Accept", "application/json").build()
        it.proceed(request)
      }

      val logging = run {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        httpLoggingInterceptor.apply {
          httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS
        }
      }

      val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

      return Retrofit.Builder()
        .client(client)
        .baseUrl(Constants.NYPL_PROD_PLATFORM_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(NYPLPlatformService::class.java)
    }
  }
}
