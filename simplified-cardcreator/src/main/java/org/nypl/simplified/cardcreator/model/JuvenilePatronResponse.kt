package org.nypl.simplified.cardcreator.model

import org.nypl.simplified.cardcreator.utils.checkFieldNotNull
import java.lang.Exception

sealed class JuvenilePatronResponse {

  data class JuvenilePatronData(
    val `data`: Data,
  ) : JuvenilePatronResponse() {

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): JuvenilePatronData =
      this.apply {
        checkFieldNotNull(data, "data")
        data.validate()
      }
  }

  data class JuvenilePatronError(
    val status: Int,
    val type: String
  ) : JuvenilePatronResponse() {

    /**
     * Moshi might have set some missing fields to null instead of throwing an exception,
     * so we need to manually check every field is not null to prevent NullPointerExceptions where
     * no-one would expect them.
     */

    fun validate(): JuvenilePatronError =
      this.apply {
        checkFieldNotNull(status, "status")
        checkFieldNotNull(type, "type")
      }
  }

  data class JuvenilePatronException(
    val exception: Exception
  ) : JuvenilePatronResponse()
}
