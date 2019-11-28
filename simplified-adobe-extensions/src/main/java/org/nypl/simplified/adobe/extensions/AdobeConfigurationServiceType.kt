package org.nypl.simplified.adobe.extensions

import com.io7m.jfunctional.Option
import com.io7m.jfunctional.OptionType

/**
 * Configuration values for the Adobe DRM.
 */

interface AdobeConfigurationServiceType {

  val packageOverride: String?

  fun packageOverrideOption(): OptionType<String> {
    return Option.of(this.packageOverride)
  }

  val debugLogging: Boolean

  val dataDirectoryName: String
}
