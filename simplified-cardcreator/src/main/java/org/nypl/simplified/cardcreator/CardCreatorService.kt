package org.nypl.simplified.cardcreator

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.io.InputStream
import java.util.Properties

class CardCreatorService(private val username: String, private val password: String) : CardCreatorServiceType {

  companion object {
    fun create(configFile: InputStream): CardCreatorServiceType {
      val properties = Properties()
      properties.load(configFile)
      val username = properties.getProperty("cardcreator.prod.username")
      val password = properties.getProperty("cardcreator.prod.password")
      return CardCreatorService(username, password)
    }
  }

  override fun openCardCreatorActivity(activity: Activity?, context: Context?, resultCode: Int) {
    val intent = Intent(context, CardCreatorActivity::class.java)
    intent.putExtra("username", username)
    intent.putExtra("password", password)
    activity?.startActivityForResult(intent, resultCode, null)
  }
}
