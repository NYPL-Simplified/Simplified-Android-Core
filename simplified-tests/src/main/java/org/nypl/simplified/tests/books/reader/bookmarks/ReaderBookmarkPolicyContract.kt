package org.nypl.simplified.tests.books.reader.bookmarks

import com.io7m.jfunctional.Option
import org.joda.time.LocalDateTime
import org.junit.Assert
import org.junit.Test
import org.nypl.simplified.books.accounts.AccountID
import org.nypl.simplified.books.reader.ReaderBookLocation
import org.nypl.simplified.books.reader.ReaderBookmark
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkKind
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicy
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyAccountState
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Local.BookmarkCreated
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Remote.BookmarkReceived
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyInput.Event.Remote.SyncingEnabled
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput.Command
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyOutput.Event
import org.nypl.simplified.books.reader.bookmarks.ReaderBookmarkPolicyState
import java.util.Random

open class ReaderBookmarkPolicyContract {

  val bookmark0 =
    ReaderBookmark(
      opdsId = "opdsid",
      location = ReaderBookLocation.create(Option.none(), "id"),
      time = LocalDateTime.now(),
      kind = ReaderBookmarkKind.ReaderBookmarkExplicit,
      chapterTitle = "A Title",
      chapterProgress = 0.5,
      bookProgress = 0.25,
      uri = null,
      deviceID = "urn:uuid:28cad755-2a0e-48bc-b5c8-1d43d57ac3e9")

  val bookmark1 =
    ReaderBookmark(
      opdsId = "opdsid-x",
      location = ReaderBookLocation.create(Option.none(), "id"),
      time = LocalDateTime.now(),
      kind = ReaderBookmarkKind.ReaderBookmarkExplicit,
      chapterTitle = "A Title",
      chapterProgress = 0.5,
      bookProgress = 0.25,
      uri = null,
      deviceID = "urn:uuid:28cad755-2a0e-48bc-b5c8-1d43d57ac3e9")

  /**
   * If a local bookmark is created, it should be saved locally. It should not be synced remotely
   * if the account does not have syncing enabled.
   */

  @Test
  fun testBookmarkLocalCreatedRemoteNotEnabled()
  {
    val accountID = AccountID.create(23)

    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = false,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(BookmarkCreated(accountID, bookmark0)),
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(1, result.outputs.size)
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
  }

  /**
   * If a local bookmark is created, it should be saved locally. It should not be synced remotely
   * if the account does not have syncing permitted.
   */

  @Test
  fun testBookmarkLocalCreatedRemoteNotPermitted()
  {
    val accountID = AccountID.create(23)

    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = true,
            syncPermittedByUser = false)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(BookmarkCreated(accountID, bookmark0)),
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(1, result.outputs.size)
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
  }

  /**
   * If a local bookmark is created, it should be saved locally. It should not be synced remotely
   * if the account does not have syncing supported.
   */

  @Test
  fun testBookmarkLocalCreatedRemoteNotSupported()
  {
    val accountID = AccountID.create(23)

    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = false,
            syncEnabledOnServer = true,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(BookmarkCreated(accountID, bookmark0)),
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(1, result.outputs.size)
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
  }

  /**
   * If a local bookmark is created, it should be saved locally. It should be synced remotely
   * if the account syncing is permitted, supported, and enabled.
   */

  @Test
  fun testBookmarkLocalCreatedRemoteOK()
  {
    val accountID = AccountID.create(23)

    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = true,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(BookmarkCreated(accountID, bookmark0)),
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(2, result.outputs.size)
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
    Assert.assertEquals(Command.RemotelySendBookmark(accountID, bookmark0), result.outputs[1])
  }

  /**
   * If a local bookmark is received, and syncing is permitted, all bookmarks that are not
   * either currently being sent to the server, or are already on the server, should be sent
   * to the server.
   */

  @Test
  fun testBookmarkRemoteReceivedSyncMissing()
  {
    val accountID = AccountID.create(23)

    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(Pair(accountID, setOf(bookmark1))),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = true,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(BookmarkReceived(accountID, bookmark0)),
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark1.bookmarkId))
    Assert.assertEquals(1, result.outputs.size)
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
  }

  /**
   * If a local bookmark is created multiple times, an event is published notifying anyone
   * that cares.
   */

  @Test
  fun testBookmarkLocalCreatedTwice()
  {
    val accountID = AccountID.create(23)

    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf())

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(BookmarkCreated(accountID, bookmark0))
          .flatMap { ReaderBookmarkPolicy.evaluateInput(BookmarkCreated(accountID, bookmark0)) },
        state)

    Assert.assertTrue(result.newState.bookmarksAll.value.containsKey(bookmark0.bookmarkId))
    Assert.assertEquals(Command.LocallySaveBookmark(accountID, bookmark0), result.outputs[0])
    Assert.assertEquals(Event.LocalBookmarkAlreadyExists(accountID, bookmark0), result.outputs[1])
  }

  /**
   * If an account exists but syncing is disabled on the server, and then syncing is enabled,
   * an attempt should be made to fetch bookmarks.
   */

  @Test
  fun testBookmarkSyncingRemotelyEnabled()
  {
    val accountID = AccountID.create(23)

    val state =
      ReaderBookmarkPolicyState.create(
        locallySaved = mapOf(),
        initialAccounts = setOf(
          ReaderBookmarkPolicyAccountState(
            accountID = accountID,
            syncSupportedByAccount = true,
            syncEnabledOnServer = false,
            syncPermittedByUser = true)))

    val result =
      ReaderBookmarkPolicy.evaluatePolicy(
        ReaderBookmarkPolicy.evaluateInput(SyncingEnabled(accountID, true)),
        state)

    Assert.assertEquals(Command.RemotelyFetchBookmarks(accountID), result.outputs[0])
  }

  /**
   * `forall a, f. return a >>= f = f a`
   */

  @Test
  fun testMonadLeftIdentity()
  {
    val state =
      ReaderBookmarkPolicyState.empty()

    val random = Random()
    val values  = (1..100).map { random.nextInt() }
    val f = { z: Int -> ReaderBookmarkPolicy.unit(z * 2) }

    for (x in values) {
      val m = ReaderBookmarkPolicy.unit(x)
      Assert.assertEquals(
        ReaderBookmarkPolicy.evaluatePolicy(m.flatMap(f), state),
        ReaderBookmarkPolicy.evaluatePolicy(f.invoke(x), state))
    }
  }

  /**
   * `forall m. m >>= return = m`
   */

  @Test
  fun testMonadRightIdentity()
  {
    val state =
      ReaderBookmarkPolicyState.empty()

    val random = Random()
    val values  = (1..100).map { random.nextInt() }

    for (x in values) {
      val m = ReaderBookmarkPolicy.unit(x)
      Assert.assertEquals(
        ReaderBookmarkPolicy.evaluatePolicy(m.flatMap({ n -> ReaderBookmarkPolicy.unit (n)}), state),
        ReaderBookmarkPolicy.evaluatePolicy(m, state))
    }
  }

  /**
   * `forall m, f, g. (m >>= f) >>= g = m >>= {x -> (f x) >>= g}`
   */

  @Test
  fun testMonadAssociativity()
  {
    val state =
      ReaderBookmarkPolicyState.empty()

    val random = Random()
    val values  = (1..100).map { random.nextInt() }

    for (x in values) {
      val f = { z: Int -> ReaderBookmarkPolicy.unit(z + 1) }
      val g = { z: Int -> ReaderBookmarkPolicy.unit(z * 2) }

      val m = ReaderBookmarkPolicy.unit(x)
      val r0 = m.flatMap(f).flatMap(g)
      val r1 = m.flatMap({ z -> (f.invoke(z)).flatMap(g) })

      Assert.assertEquals(
        ReaderBookmarkPolicy.evaluatePolicy(r0, state),
        ReaderBookmarkPolicy.evaluatePolicy(r1, state))
    }
  }
}