package org.nypl.simplified.ui.errorpage

/**
 * The interface that must be implemented by activities that host error page fragments.
 */

interface ErrorPageListenerType {

  /**
   * The user wants to send an error report.
   */

  fun onErrorPageSendReport(
    parameters: ErrorPageParameters
  )
}
