package org.nypl.simplified.cardcreator.model

import com.squareup.moshi.Json

data class Username(@field:Json(name = "username") val username: String)
