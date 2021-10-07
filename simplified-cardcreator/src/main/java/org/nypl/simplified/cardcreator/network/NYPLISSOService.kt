package org.nypl.simplified.cardcreator.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.nypl.simplified.cardcreator.model.ISSOTokenData
import org.nypl.simplified.cardcreator.utils.Constants
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

internal interface NYPLISSOService {

  /**
   * Retrieves token needs for NYPL Platform API calls
   *
   * https://github.com/NYPL-Simplified/card-creator/wiki/API---V2#post-v2create_patron
   */
  @Multipart
  @POST("token")
  suspend fun getToken(
    @Part grant_type: MultipartBody.Part,
    @Part client_id: MultipartBody.Part,
    @Part client_secret: MultipartBody.Part
  ): Response<ISSOTokenData>

  companion object {
    operator fun invoke(): NYPLISSOService {
      val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
      }

      val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

      val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

      return Retrofit.Builder()
        .client(client)
        .baseUrl(Constants.NYPL_ISSO_BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(NYPLISSOService::class.java)
    }
  }
}
