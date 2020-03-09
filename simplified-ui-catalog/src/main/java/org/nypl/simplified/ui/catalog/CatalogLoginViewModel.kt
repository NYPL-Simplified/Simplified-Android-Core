package org.nypl.simplified.ui.catalog

import androidx.lifecycle.ViewModel
import io.reactivex.subjects.PublishSubject

/**
 * A trivial view model for login dialogs.
 *
 * Typically, components will subscribe to events for the underlying accounts for accurate
 * information on the login state of a given account and this is normally sufficient. However,
 * components that want to open a short-lived login dialog and then execute a command when it
 * is closed will run into issues in that there is no safe way to pass listener methods between
 * fragments. Instead, the catalog login dialog will publish events to the [loginDialogCompleted]
 * subject, and catalog fragments can subscribe to this subject to know when dialogs have been
 * closed.
 */

class CatalogLoginViewModel : ViewModel() {

  /**
   * A mutable subject to which [Unit] will be written when a login dialog closes.
   */

  val loginDialogCompleted = PublishSubject.create<Unit>()
}
