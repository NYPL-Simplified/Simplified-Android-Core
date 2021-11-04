package org.nypl.simplified.books.api

import java.lang.IllegalStateException
import java.lang.StringBuilder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

internal fun String.sha256(): String {
  return try {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(this.toByteArray())
    val dg = md.digest()
    val b = StringBuilder(64)
    for (index in dg.indices) {
      val bb = dg[index]
      b.append(String.format("%02x", bb))
    }
    b.toString()
  } catch (e: NoSuchAlgorithmException) {
    throw IllegalStateException(e)
  }
}
