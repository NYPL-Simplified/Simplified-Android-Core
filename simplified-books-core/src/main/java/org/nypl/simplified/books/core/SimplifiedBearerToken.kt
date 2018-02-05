package org.nypl.simplified.books.core

import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.net.URISyntaxException
import java.util.Calendar
import java.util.Date

internal data class SimplifiedBearerToken(
    val accessToken: String,
    val expiration: Date,
    val location: URI) {

  companion object Factory {
    fun withJSONObject(jsonObject: JSONObject): SimplifiedBearerToken? {
      try {
        if (jsonObject.getString("token_type") != "Bearer") {
          return null
        }

        val accessToken = jsonObject.getString("access_token")

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, jsonObject.getInt("expires_in"))
        val expiration = calendar.time

        val location = URI(jsonObject.getString("location"))

        return SimplifiedBearerToken(accessToken, expiration, location)
      } catch (_: JSONException) {
        return null
      } catch (_: URISyntaxException) {
        return null
      }
    }
  }
}
