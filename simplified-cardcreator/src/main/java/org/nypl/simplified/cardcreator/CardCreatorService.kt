package org.nypl.simplified.cardcreator

import android.content.Context
import org.slf4j.LoggerFactory
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Properties

class CardCreatorService(
  private val username: String,
  private val password: String,
  private val clientId: String,
  private val clientSecret: String
) : CardCreatorServiceType {

  companion object {

    private val logger =
      LoggerFactory.getLogger(CardCreatorService::class.java)

    fun createConditionally(
      context: Context
    ): CardCreatorServiceType? {
      return try {
        context.assets.open("secrets.conf").use(::create)
      } catch (e: FileNotFoundException) {
        this.logger.warn("failed to initialize card creator; secrets.conf not found")
        null
      } catch (e: Exception) {
        this.logger.warn("failed to initialize card creator", e)
        null
      }
    }

    fun create(
      stream: InputStream
    ): CardCreatorServiceType {
      val properties =
        Properties().apply { load(stream) }

      val username =
        properties.getProperty("cardcreator.prod.username")
          ?: throw NullPointerException("cardcreator.prod.username is missing")
      val password =
        properties.getProperty("cardcreator.prod.password")
          ?: throw NullPointerException("cardcreator.prod.password is missing")
      val clientId =
        properties.getProperty("cardcreator.prod.client.id")
          ?: throw NullPointerException("cardcreator.prod.client.id is missing")
      val clientSecret =
        properties.getProperty("cardcreator.prod.client.secret")
          ?: throw NullPointerException("cardcreator.prod.client.secret is missing")

      return CardCreatorService(username, password, clientId, clientSecret)
    }
  }

  override fun getCardCreatorContract(): CardCreatorContract =
    CardCreatorContract(
      this.username,
      this.password,
      this.clientId,
      this.clientSecret
    )
}
