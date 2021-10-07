package org.nypl.simplified.cardcreator.utils

import com.squareup.moshi.JsonDataException

fun <T> checkFieldNotNull(fieldValue: T, fieldName: String): T =
  fieldValue ?: throw JsonDataException("Field $fieldName is missing in the server's response.")
