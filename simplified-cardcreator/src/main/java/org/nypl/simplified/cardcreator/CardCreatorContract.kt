package org.nypl.simplified.cardcreator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf

class CardCreatorContract(
  private val username: String,
  private val password: String,
  private val clientId: String,
  private val clientSecret: String
) : ActivityResultContract<CardCreatorContract.Input, CardCreatorContract.Output>() {

  class Input(
    val userIdentifier: String,
    val isLoggedIn: Boolean
  )

  data class Output(
    val barcode: String,
    val pin: String
  )

  override fun createIntent(context: Context, input: Input): Intent {
    val extras = this.createExtras(input)
    val intent = Intent(context, CardCreatorActivity::class.java)
    intent.putExtras(extras)
    return intent
  }

  private fun createExtras(input: Input): Bundle =
    bundleOf(
      USERNAME_KEY to this.username,
      PASSWORD_KEY to this.password,
      CLIENT_ID_KEY to this.clientId,
      CLIENT_SECRET_KEY to this.clientSecret,
      IS_LOGGED_IN_KEY to input.isLoggedIn,
      USER_IDENTIFIER_KEY to input.userIdentifier
    )

  override fun parseResult(resultCode: Int, intent: Intent?): Output? =
    if (intent == null || resultCode != Activity.RESULT_OK) {
      null
    } else {
      this.parseResult(intent.extras!!)
    }

  private fun parseResult(result: Bundle): Output {
    val barcode = result.getString(BARCODE_KEY)!!
    val pin = result.getString(PIN_KEY)!!

    return Output(barcode, pin)
  }

  companion object {
    private const val USERNAME_KEY = "username"
    private const val PASSWORD_KEY = "password"
    private const val CLIENT_ID_KEY = "clientId"
    private const val CLIENT_SECRET_KEY = "clientSecret"
    private const val IS_LOGGED_IN_KEY = "isLoggedIn"
    private const val USER_IDENTIFIER_KEY = "userIdentifier"

    private const val BARCODE_KEY = "barcode"
    private const val PIN_KEY = "pin"

    fun createResult(barcode: String, pin: String): Bundle {
      return bundleOf(BARCODE_KEY to barcode, PIN_KEY to pin)
    }
  }
}
