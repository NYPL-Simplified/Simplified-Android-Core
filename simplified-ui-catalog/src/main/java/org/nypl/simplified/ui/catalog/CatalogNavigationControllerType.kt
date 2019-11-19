package org.nypl.simplified.ui.catalog

import org.nypl.simplified.feeds.api.FeedEntry.FeedEntryOPDS
import org.nypl.simplified.presentableerror.api.PresentableErrorType
import org.nypl.simplified.ui.errorpage.ErrorPageParameters

/**
 * Navigation functions for the catalog screens.
 */

interface CatalogNavigationControllerType {

  /**
   * A catalog screen wants to pop the current screen from the stack.
   */

  fun popBackStack()

  /**
   * The catalog wants to open a book detail page.
   */

  fun openBookDetail(entry: FeedEntryOPDS)

  /**
   * A catalog screen wants to open the error page.
   */

  fun <E : PresentableErrorType> openErrorPage(
    parameters: ErrorPageParameters<E>)
}
