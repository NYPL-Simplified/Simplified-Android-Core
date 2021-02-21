package org.librarysimplified.r2.api

import android.webkit.WebView
import io.reactivex.Observable
import java.io.Closeable

/**
 * The type of R2 controllers.
 *
 * An R2 controller encapsulates all of the logic required to open an EPUB for reading,
 * track the current reading position, publish events related to EPUB reading, and to
 * relay commands to the appropriate components in order to facilitate reading (such as
 * sending commands to turn pages, sending commands to change chapters, etc).
 *
 * There is a 1:1 correspondence between controller instances and books open for reading;
 * a controller is created when a book is opened, and closed when the book is closed.
 */

interface SR2ControllerType : Closeable, SR2ControllerCommandQueueType {

  /**
   * Information on the opened book.
   */

  val bookMetadata: SR2BookMetadata

  /**
   * An observable stream of events relating to the currently open controller.
   */

  val events: Observable<SR2Event>

  /**
   * Connect a [WebView] to the controller.
   */

  fun viewConnect(webView: WebView)

  /**
   * Disconnect any [WebView] currently connected to the controller.
   *
   * This method is primarily used when an Android `Fragment` hosting a set of views is destroyed
   * due to changing screen orientations, or when the fragment is temporarily placed onto the back
   * navigation stack. This method is required to ensure that a long-running controller instance
   * does not keep any views from being garbage collected.
   *
   * This method _must_ be called by the [close] method.
   */

  fun viewDisconnect()

  /**
   * The list of bookmarks currently loaded into the controller. This list is an immutable
   * snapshots, and subsequent updates to bookmarks will not be reflected in the returned list.
   */

  fun bookmarksNow(): List<SR2Bookmark>

  /**
   * The current position of the reader.
   */

  fun positionNow(): SR2Locator
}
