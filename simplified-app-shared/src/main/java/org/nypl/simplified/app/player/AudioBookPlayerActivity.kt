package org.nypl.simplified.app.player

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.books.core.FeedEntryOPDS
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI

/**
 * The main activity for playing audio books.
 */

class AudioBookPlayerActivity : Activity() {

  private val log: Logger = LoggerFactory.getLogger(AudioBookPlayerActivity::class.java)

  companion object {

    private const val BOOK_ID =
      "org.nypl.simplified.app.player.AudioBookPlayerActivity.book_id"
    private const val FILE_ID =
      "org.nypl.simplified.app.player.AudioBookPlayerActivity.manifest_file_id"
    private const val ENTRY =
      "org.nypl.simplified.app.player.AudioBookPlayerActivity.entry"
    private const val URI =
      "org.nypl.simplified.app.player.AudioBookPlayerActivity.manifest_uri"

    /**
     * Start a new player for the given book.
     *
     * @param from The parent activity
     * @param book The unique ID of the book
     * @param file The manifest file
     * @param manifestURI The URI used to fetch new manifests
     * @param entry The OPDS feed entry
     */

    fun startActivity(
      from: Activity,
      book: BookID,
      manifestFile: File,
      manifestURI: URI,
      entry: FeedEntryOPDS) {

      val b = Bundle()
      b.putSerializable(this.BOOK_ID, book)
      b.putSerializable(this.FILE_ID, manifestFile)
      b.putSerializable(this.URI, manifestURI)
      b.putSerializable(this.ENTRY, entry)
      val i = Intent(from, AudioBookPlayerActivity::class.java)
      i.putExtras(b)
      from.startActivity(i)
    }
  }

  private lateinit var manifestFile: File
  private lateinit var manifestURI: URI
  private lateinit var bookID: BookID
  private lateinit var opdsEntry: OPDSAcquisitionFeedEntry

  override fun onCreate(state: Bundle?) {
    super.onCreate(state)

    this.log.debug("onCreate")

    val i = this.intent!!
    val a = i.extras

    this.manifestFile = a.getSerializable(FILE_ID) as File
    this.manifestURI = a.getSerializable(URI) as URI
    this.bookID = a.getSerializable(BOOK_ID) as BookID
    this.opdsEntry = (a.getSerializable(ENTRY) as FeedEntryOPDS).feedEntry!!

    this.log.debug("manifest file: {}", this.manifestFile)
    this.log.debug("manifest uri:  {}", this.manifestURI)
    this.log.debug("book id:       {}", this.bookID)
    this.log.debug("entry id:      {}", this.opdsEntry.id)
  }
}
