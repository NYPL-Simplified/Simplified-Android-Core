package org.nypl.simplified.ui.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.librarysimplified.services.api.ServiceDirectoryProviderType
import org.slf4j.LoggerFactory

class HostViewModelFactory(
  private val services: ServiceDirectoryProviderType
) : ViewModelProvider.Factory  {

  private val logger = LoggerFactory.getLogger(HostViewModelFactory::class.java)

  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    this.logger.debug("requested creation of view model of $modelClass")

    return when {
      modelClass.isAssignableFrom(HostViewModel::class.java) ->
        HostViewModel(this.services.serviceDirectory()) as T
      else ->
        throw IllegalArgumentException(
          "This view model factory (${this.javaClass}) cannot produce view models of type $modelClass")
    }
  }
}
