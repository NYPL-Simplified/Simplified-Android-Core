package org.nypl.simplified.viewer.audiobook

import androidx.media2.session.MediaSession
import org.librarysimplified.audiobook.player.api.PlayerType
import org.nypl.simplified.books.book_database.api.BookDatabaseEntryFormatHandle
import org.nypl.simplified.viewer.audiobook.AudioBookPlayerParameters
import org.readium.navigator.media2.ExperimentalMedia2
import org.readium.navigator.media2.MediaNavigator

@OptIn(ExperimentalMedia2::class)
data class AudioBookPlayerSessionHolder(
  val parameters: AudioBookPlayerParameters,
  val navigator: MediaNavigator,
  val mediaSession: MediaSession,
  val player: PlayerType,
  val formatHandle: BookDatabaseEntryFormatHandle.BookDatabaseEntryFormatHandleAudioBook
) {

  fun close() {
    mediaSession.close()
    navigator.close()
    navigator.publication.close()
    player.close()
  }
}
