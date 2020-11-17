package org.nypl.simplified.opds.core

import java.io.IOException

/**
 * An [IOException] wrapper.
 */

class OPDSFeedTransportIOException(
  message: String,
  cause: IOException
) : OPDSFeedTransportException(message, cause)
