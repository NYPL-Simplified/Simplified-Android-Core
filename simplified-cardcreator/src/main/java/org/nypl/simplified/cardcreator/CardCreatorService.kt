package org.nypl.simplified.cardcreator

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import java.io.InputStream
import java.util.Properties

class CardCreatorService(
  private val username: String,
  private val password: String,
  private val clientId: String,
  private val clientSecret: String
) : CardCreatorServiceType {

  companion object {
    fun create(configFile: InputStream): CardCreatorServiceType {
      val properties = Properties()
      properties.load(configFile)
      val username = properties.getProperty("cardcreator.prod.username")
      val password = properties.getProperty("cardcreator.prod.password")
      val clientId = properties.getProperty("cardcreator.prod.client.id")
      val clientSecret = properties.getProperty("cardcreator.prod.client.secret")
      return CardCreatorService(username, password, clientId, clientSecret)
    }
  }

  override fun openCardCreatorActivity(
    fragment: Fragment,
    context: Context?,
    resultCode: Int,
    isLoggedIn: Boolean,
    userIdentifier: String
  ) {
    val intent = Intent(context, CardCreatorActivity::class.java)
    intent.putExtra("username", username)
    intent.putExtra("password", password)
    intent.putExtra("clientId", clientId)
    intent.putExtra("clientSecret", clientSecret)
    intent.putExtra("isLoggedIn", isLoggedIn)
    intent.putExtra("userIdentifier", userIdentifier)
    fragment.startActivityForResult(intent, resultCode)
  }
}
