package org.nypl.simplified.tests.sandbox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.nypl.simplified.oauth.OAuthCallbackIntentParsing
import org.nypl.simplified.oauth.OAuthParseResult
import org.slf4j.LoggerFactory
import java.util.UUID

class OAuthActivity : AppCompatActivity() {

  private val logger = LoggerFactory.getLogger(OAuthActivity::class.java)
  private lateinit var text: TextView
  private lateinit var go: Button
  private val oauthScheme: String = "simplified-sandbox-oauth"
  private val account = UUID.randomUUID()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.setContentView(R.layout.oauth)

    this.go =
      this.findViewById(R.id.oaGo)
    this.text =
      this.findViewById(R.id.oaText)

    this.go.setOnClickListener {
      this.text.append("Starting login...\n")
      this.text.append("    ${this.account}\n")

      val callback =
        OAuthCallbackIntentParsing.createUri(this.oauthScheme, this.account)

      val url = buildString {
        this.append("https://circulation.openebooks.us/USOEI/oauth_authenticate?provider=Clever")
        this.append("&redirect_uri=$callback")
      }

      val intent = Intent(Intent.ACTION_VIEW)
      intent.data = Uri.parse(url)
      this.startActivity(intent)
    }
  }

  override fun onNewIntent(intent: Intent) {
    when (
      val result = OAuthCallbackIntentParsing.processIntent(
        intent = intent,
        requiredScheme = this.oauthScheme,
        parseUri = Uri::parse
      )
    ) {
      is OAuthParseResult.Failed ->
        this.logger.warn("failed to parse incoming intent: {}", result.message)
      is OAuthParseResult.Success -> {
        this.text.append("Received auth token:\n")
        this.text.append("    ${result.token}\n")
        this.text.append("    ${result.accountId}\n")
      }
    }

    super.onNewIntent(intent)
  }
}
