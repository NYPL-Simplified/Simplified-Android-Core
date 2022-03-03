package org.nypl.simplified.cardcreator

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

  sealed class Output {
    data class CardCreated(
      val barcode: String,
      val pin: String,
    ) : Output()
    object ChildCardCreated : Output()
    object CardCreationNoOp : Output() // Covers cancellation and error
  }

  override fun createIntent(context: Context, input: Input): Intent {
    val extras = createExtras(input)
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

  override fun parseResult(resultCode: Int, intent: Intent?): Output =
    when {
      intent == null -> Output.CardCreationNoOp // ??
      intent.extras == null -> Output.CardCreationNoOp // ??
      resultCode == CardCreatorActivity.CHILD_CARD_CREATED -> Output.ChildCardCreated
      resultCode == CardCreatorActivity.CARD_CREATED -> {
        Output.CardCreated(
          intent.extras!!.getString(BARCODE_KEY)!!,
          intent.extras!!.getString(PIN_KEY)!!
        )
      }
      else -> Output.CardCreationNoOp
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

    fun resultBundle(barcode: String, pin: String): Bundle {
      return bundleOf(BARCODE_KEY to barcode, PIN_KEY to pin)
    }
  }
}
