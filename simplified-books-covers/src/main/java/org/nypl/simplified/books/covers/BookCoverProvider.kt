package org.nypl.simplified.books.covers

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.google.common.util.concurrent.FluentFuture
import com.google.common.util.concurrent.SettableFuture
import com.io7m.jfunctional.OptionType
import com.io7m.jfunctional.Some
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.nypl.simplified.books.book_registry.BookRegistryReadableType
import org.nypl.simplified.books.book_registry.BookWithStatus
import org.nypl.simplified.feeds.api.FeedEntry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.ExecutorService

/**
 * The default implementation of the book cover provider interface.
 */

class BookCoverProvider private constructor(
  private val bookRegistry: BookRegistryReadableType,
  private val coverGenerator: BookCoverGeneratorType,
  private val picasso: Picasso,
  private val badgeLookup: BookCoverBadgeLookupType
) : BookCoverProviderType {

  private val log: Logger = LoggerFactory.getLogger(BookCoverProvider::class.java)
  private val coverTag: String = "cover"
  private val thumbnailTag: String = "thumbnail"

  private fun generateCoverURI(entry: FeedEntry.FeedEntryOPDS): URI {
    val feedEntry = entry.feedEntry
    val title = feedEntry.title
    val author: String
    val authors = feedEntry.authors
    if (authors.isEmpty()) {
      author = ""
    } else {
      author = authors[0]
    }
    return this.coverGenerator.generateURIForTitleAuthor(title, author)
  }

  private fun doLoad(
    entry: FeedEntry.FeedEntryOPDS,
    imageView: ImageView,
    width: Int,
    height: Int,
    tag: String,
    uriSpecified: URI?): FluentFuture<Unit> {

    val future = SettableFuture.create<Unit>()
    val uriGenerated = this.generateCoverURI(entry)

    val callbackFinal = object : Callback {
      override fun onSuccess() {
        future.set(Unit)
      }

      override fun onError() {
        future.setException(IOException(
          StringBuilder(128)
            .append("Failed to load image.\n")
            .append("  URI (specified): ")
            .append(uriSpecified)
            .append('\n')
            .append("  URI (generated): ")
            .append(uriGenerated)
            .append('\n')
            .toString()))
      }
    }

    val badgePainter = BookCoverBadgePainter(entry, this.badgeLookup)
    if (uriSpecified != null) {
      this.log.debug("{}: {}: loading specified uri {}", tag, entry.bookID, uriSpecified)

      val fallbackToGeneration = object : Callback {
        override fun onSuccess() {
          future.set(Unit)
        }

        override fun onError() {
          this@BookCoverProvider.log.debug(
            "{}: {}: failed to load uri {}, falling back to generation",
            tag, entry.bookID, uriSpecified)

          val fallbackRequest = this@BookCoverProvider.picasso.load(uriGenerated.toString())
          fallbackRequest.tag(tag)
          fallbackRequest.resize(width, height)
          fallbackRequest.transform(badgePainter)
          fallbackRequest.into(imageView, callbackFinal)
          fallbackRequest.noFade()
        }
      }

      val request = this.picasso.load(uriSpecified.toString())
      request.tag(tag)
      request.resize(width, height)
      request.transform(badgePainter)
      request.into(imageView, fallbackToGeneration)
      request.noFade()
    } else {
      this.log.debug("{}: {}: loading generated uri {}", tag, entry.bookID, uriGenerated)

      val request = this.picasso.load(uriGenerated.toString())
      request.tag(tag)
      request.resize(width, height)
      request.transform(badgePainter)
      request.into(imageView, callbackFinal)
      request.noFade()
    }

    return FluentFuture.from(future)
  }

  private fun coverURIOf(entry: FeedEntry.FeedEntryOPDS): URI? {
    val bookOpt = this.bookRegistry.book(entry.bookID)
    if (bookOpt is Some<BookWithStatus>) {
      val book = bookOpt.get()
      return book.book.cover?.toURI()
    }
    return mapOptionToNull(entry.feedEntry.cover)
  }

  private fun thumbnailURIOf(entry: FeedEntry.FeedEntryOPDS): URI? {
    val bookOpt = this.bookRegistry.book(entry.bookID)
    if (bookOpt is Some<BookWithStatus>) {
      val book = bookOpt.get()
      return book.book.cover?.toURI()
    }
    return mapOptionToNull(entry.feedEntry.thumbnail)
  }

  override fun loadThumbnailInto(
    entry: FeedEntry.FeedEntryOPDS,
    imageView: ImageView,
    width: Int,
    height: Int): FluentFuture<Unit> {

    return doLoad(
      entry = entry,
      imageView = imageView,
      width = width,
      height = height,
      tag = thumbnailTag,
      uriSpecified = thumbnailURIOf(entry))
  }

  override fun loadCoverInto(
    entry: FeedEntry.FeedEntryOPDS,
    imageView: ImageView,
    width: Int,
    height: Int): FluentFuture<Unit> {

    return doLoad(
      entry = entry,
      imageView = imageView,
      width = width,
      height = height,
      tag = coverTag,
      uriSpecified = coverURIOf(entry))
  }

  private fun <T> mapOptionToNull(option: OptionType<T>): T? {
    if (option is Some<T>) {
      return option.get()
    } else {
      return null
    }
  }

  override fun loadingThumbailsPause() {
    this.picasso.pauseTag(this.thumbnailTag)
  }

  override fun loadingThumbnailsContinue() {
    this.picasso.resumeTag(this.thumbnailTag)
  }

  companion object {

    /**
     * Create a new cover provider.
     *
     * @param context         The application context
     * @param badgeLookup     A function used to look up badge images
     * @param bookRegistry    The book registry
     * @param coverGenerator  A cover generator
     * @param executor        An executor
     *
     * @return A new cover provider
     */

    fun newCoverProvider(
      context: Context,
      bookRegistry: BookRegistryReadableType,
      coverGenerator: BookCoverGeneratorType,
      badgeLookup: BookCoverBadgeLookupType,
      executor: ExecutorService,
      debugCacheIndicators: Boolean,
      debugLogging: Boolean): BookCoverProviderType {

      val picassoBuilder = Picasso.Builder(context)
      picassoBuilder.defaultBitmapConfig(Bitmap.Config.RGB_565)
      picassoBuilder.indicatorsEnabled(debugCacheIndicators)
      picassoBuilder.loggingEnabled(debugLogging)
      picassoBuilder.addRequestHandler(BookCoverGeneratorRequestHandler(coverGenerator))
      picassoBuilder.executor(executor)

      val picasso = picassoBuilder.build()
      return BookCoverProvider(bookRegistry, coverGenerator, picasso, badgeLookup)
    }
  }
}