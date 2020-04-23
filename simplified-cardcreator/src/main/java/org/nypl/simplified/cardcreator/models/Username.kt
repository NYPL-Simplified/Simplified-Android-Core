package org.nypl.simplified.cardcreator.models

import com.squareup.moshi.Json

data class Username(@field:Json(name = "username") val username: String)
