package org.nypl.simplified.tests.sandbox

import com.google.common.util.concurrent.FluentFuture
import com.io7m.junreachable.UnimplementedCodeException
import org.nypl.simplified.books.core.AccountCredentials
import org.nypl.simplified.books.core.AccountDataLoadListenerType
import org.nypl.simplified.books.core.AccountGetCachedCredentialsListenerType
import org.nypl.simplified.books.core.AccountLoginListenerType
import org.nypl.simplified.books.core.AccountLogoutListenerType
import org.nypl.simplified.books.core.AccountSyncListenerType
import org.nypl.simplified.books.core.BookDatabaseType
import org.nypl.simplified.books.core.BookFeedListenerType
import org.nypl.simplified.books.core.BookID
import org.nypl.simplified.books.core.BooksControllerConfigurationType
import org.nypl.simplified.books.core.BooksFeedSelection
import org.nypl.simplified.books.core.BooksStatusCacheType
import org.nypl.simplified.books.core.BooksType
import org.nypl.simplified.books.core.DeviceActivationListenerType
import org.nypl.simplified.books.core.FeedEntryOPDS
import org.nypl.simplified.books.core.FeedFacetPseudo
import org.nypl.simplified.books.core.FeedFacetPseudoTitleProviderType
import org.nypl.simplified.opds.core.DRMLicensor
import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry
import java.net.URI
import java.util.Calendar

class MockedBooks(
  val booksStatusCache: MockedBookStatusCache) : BooksType {

  override fun accountActivateDevice(device_listener: DeviceActivationListenerType?) {

  }

  override fun bookGetStatusCache(): BooksStatusCacheType {
    return this.booksStatusCache
  }

  override fun bookDeleteData(id: BookID?, needs_auth: Boolean) {

  }

  override fun bookReport(feed_entry: FeedEntryOPDS?, report_type: String?) {

  }

  override fun accountIsLoggedIn(): Boolean {
    throw UnimplementedCodeException()
  }

  override fun accountGetCachedLoginDetails(listener: AccountGetCachedCredentialsListenerType?) {

  }

  override fun accountSync(
    listener: AccountSyncListenerType?,
    device_listener: DeviceActivationListenerType?,
    needs_authentication: Boolean): FluentFuture<com.io7m.jfunctional.Unit> {
    throw UnimplementedCodeException()
  }

  override fun fulfillExistingBooks() {
  }

  override fun accountDeActivateDevice() {
  }

  override fun destroyBookStatusCache() {
  }

  override fun bookGetDatabase(): BookDatabaseType {
    throw UnimplementedCodeException()
  }

  override fun bookGetWritableDatabase(): BookDatabaseType {
    throw UnimplementedCodeException()
  }

  override fun accountLoadBooks(
    listener: AccountDataLoadListenerType?,
    needs_auch: Boolean): FluentFuture<com.io7m.jfunctional.Unit> {
    throw UnimplementedCodeException()
  }

  override fun accountActivateDeviceAndFulFillBook(
    in_book_id: BookID?,
    licensor: com.io7m.jfunctional.OptionType<DRMLicensor>?,
    listener: DeviceActivationListenerType?) {
  }

  override fun bookRevoke(
    id: BookID?,
    needsAuthentication: Boolean): FluentFuture<com.io7m.jfunctional.Unit> {
    throw UnimplementedCodeException()
  }

  override fun accountLogin(
    credentials: AccountCredentials?,
    listener: AccountLoginListenerType?): FluentFuture<com.io7m.jfunctional.Unit> {
    throw UnimplementedCodeException()
  }

  override fun accountRemoveCredentials() {
  }

  override fun bookBorrow(
    id: BookID?,
    entry: OPDSAcquisitionFeedEntry?,
    needs_auth: Boolean) {
  }

  override fun bookDownloadAcknowledge(id: BookID?) {
  }

  override fun bookGetLatestStatusFromDisk(id: BookID?) {
  }

  override fun accountActivateDeviceAndFulfillBooks(
    licensor: com.io7m.jfunctional.OptionType<DRMLicensor>?,
    device_listener: DeviceActivationListenerType?) {
  }

  override fun bookDownloadCancel(id: BookID?) {

  }

  override fun booksGetFeed(
    in_uri: URI?,
    in_id: String?,
    in_updated: Calendar?,
    in_title: String?,
    in_facet_active: FeedFacetPseudo.FacetType?,
    in_facet_group: String?,
    in_facet_titles: FeedFacetPseudoTitleProviderType?,
    in_search: com.io7m.jfunctional.OptionType<String>?,
    in_selection: BooksFeedSelection?,
    in_listener: BookFeedListenerType?) {

  }

  override fun booksGetConfiguration(): BooksControllerConfigurationType {
    throw UnimplementedCodeException()
  }

  override fun accountLogout(
    credentials: AccountCredentials?,
    listener: AccountLogoutListenerType?,
    sync_listener: AccountSyncListenerType?,
    device_listener: DeviceActivationListenerType?): FluentFuture<com.io7m.jfunctional.Unit> {
    throw UnimplementedCodeException()
  }

  override fun accountIsDeviceActive(): Boolean {
    throw UnimplementedCodeException()
  }
}
