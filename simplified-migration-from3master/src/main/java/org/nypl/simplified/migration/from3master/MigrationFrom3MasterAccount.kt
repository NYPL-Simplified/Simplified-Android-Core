package org.nypl.simplified.migration.from3master

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

/**
 * Account information as it was expressed on the 3.0 master branch. We don't care about
 * most of it.
 */

@JsonDeserialize
class MigrationFrom3MasterAccount(

  @JvmField
  @JsonProperty("username")
  val username: String,

  @JvmField
  @JsonProperty("password")
  val password: String,

  @JvmField
  @JsonProperty("provider")
  val provider: String?,

  @JvmField
  @JsonProperty("user_id")
  val user_id: String?,

  @JvmField
  @JsonProperty("device_id")
  val device_id: String?,

  @JvmField
  @JsonProperty("adobe_token")
  val adobe_token: String?,

  @JvmField
  @JsonProperty("licensor_url")
  val licensor_url: String?,

  @JvmField
  @JsonProperty("adobe-vendor")
  val adobe_vendor: String?
)
