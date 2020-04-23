package org.nypl.simplified.main

import android.app.Application
import org.librarysimplified.instabug.spi.InstabugProviderType
import org.librarysimplified.instabug.spi.InstabugType
import org.slf4j.LoggerFactory
import java.util.ServiceLoader

object MainInstabugService {

  private val logger =
    LoggerFactory.getLogger(MainInstabugService::class.java)

  fun create(application: Application): InstabugType? {
    val providers =
      ServiceLoader.load(InstabugProviderType::class.java).toList()

    this.logger.debug("found {} instabug providers", providers.size)
    for (provider in providers) {
      try {
        val service = provider.create(application)
        if (service != null) {
          this.logger.debug("provider {} initialized", provider)
          return service
        }
      } catch (e: Exception) {
        this.logger.error("provider {} crashed: ", provider, e)
      }
    }
    this.logger.debug("no usable providers, instabug will not be enabled")
    return null
  }
}
