package org.nypl.simplified.books.reader.bookmarks

import org.nypl.simplified.books.reader.ReaderBookmarkID

data class ReaderBookmarkEngineOperationResult(
  val state: ReaderBookmarkEngineState,
  val outputs: List<ReaderBookmarkOutput>)

data class ReaderBookmarkEngineOperation(
  private val op: (ReaderBookmarkEngineState) -> ReaderBookmarkEngineOperationResult) {

  fun evaluateFor(state: ReaderBookmarkEngineState): ReaderBookmarkEngineOperationResult {
    return this.op.invoke(state)
  }

  fun andThen(op: ReaderBookmarkEngineOperation): ReaderBookmarkEngineOperation {
    return sequence(this, op)
  }

  companion object {

    fun sequence(
      a: ReaderBookmarkEngineOperation,
      b: ReaderBookmarkEngineOperation): ReaderBookmarkEngineOperation {

      return ReaderBookmarkEngineOperation(op = { state ->
        val resultA = a.op.invoke(state)
        val resultB = b.op.invoke(resultA.state)
        ReaderBookmarkEngineOperationResult(
          state = resultB.state,
          outputs = resultA.outputs.plus(resultB.outputs))
      })
    }

    fun emitOutputs(outputs: List<ReaderBookmarkOutput>) : ReaderBookmarkEngineOperation {
      return ReaderBookmarkEngineOperation(op = {
        state -> ReaderBookmarkEngineOperationResult(state, outputs)
      })
    }

    fun emitOutput(output: ReaderBookmarkOutput): ReaderBookmarkEngineOperation {
      return emitOutputs(listOf(output))
    }

    fun updateState(state: ReaderBookmarkEngineState): ReaderBookmarkEngineOperation {
      return map { state }
    }

    fun map(f: (ReaderBookmarkEngineState) -> ReaderBookmarkEngineState): ReaderBookmarkEngineOperation {
      return ReaderBookmarkEngineOperation { state ->
        val newState = f.invoke(state)
        ReaderBookmarkEngineOperationResult(newState, listOf())
      }
    }

    fun updateBookmarkState(bookmarkState: ReaderBookmarkState): ReaderBookmarkEngineOperation {
      return map { state ->
        state.copy(state.bookmarks.plus(Pair(bookmarkState.bookmarkID, bookmarkState)))
      }
    }

    fun evaluateInput(input: ReaderBookmarkInput): ReaderBookmarkEngineOperation {
      return when (input) {
        is ReaderBookmarkInput.Event.BookmarkLocalCreated ->
          this.onReceiveEventBookmarkLocalCreated(input)
        is ReaderBookmarkInput.Event.BookmarkLocalDeleted ->
          TODO()
        is ReaderBookmarkInput.Event.BookmarkRemoteReceived ->
          this.onReceiveEventBookmarkRemoteReceived(input)
      }
    }

    private fun onReceiveEventBookmarkRemoteReceived(
      event: ReaderBookmarkInput.Event.BookmarkRemoteReceived): ReaderBookmarkEngineOperation {

      return ReaderBookmarkEngineOperation { initialState ->
        val bookmarkState = initialState.bookmarks[event.bookmark]
        if (bookmarkState != null) {
          when (bookmarkState.localState) {

            /*
             * If the bookmark has been deleted locally, but the server has sent it to us, then
             * tell the server we want it deleted.
             */

            ReaderBookmarkLocalState.Deleted -> {
              val newBookmarkState =
                ReaderBookmarkState(
                  event.bookmark,
                  ReaderBookmarkLocalState.Deleted,
                  ReaderBookmarkRemoteState.Deleting)

              this.updateBookmarkState(newBookmarkState)
                .andThen(this.emitOutput(ReaderBookmarkOutput.Command.DeleteBookmark(event.bookmark)))
                .evaluateFor(initialState)
            }

            /*
             * If the bookmark has been saved locally, then ignore it.
             */

            ReaderBookmarkLocalState.Saved -> {
              val newBookmarkState =
                ReaderBookmarkState(
                  event.bookmark,
                  ReaderBookmarkLocalState.Saved,
                  ReaderBookmarkRemoteState.Saved)

              this.updateBookmarkState(newBookmarkState)
                .andThen(this.emitOutput(ReaderBookmarkOutput.Event.LocalBookmarkAlreadyExists(event.bookmark)))
                .evaluateFor(initialState)
            }
          }
        } else {
          TODO()
        }
      }
    }

    private fun onReceiveEventBookmarkLocalCreated(
      event: ReaderBookmarkInput.Event.BookmarkLocalCreated): ReaderBookmarkEngineOperation {

      return ReaderBookmarkEngineOperation { initialState ->

        val bookmarkState = initialState.bookmarks[event.bookmark]
        if (bookmarkState != null) {
          when (bookmarkState.localState) {

            /*
             * If the bookmark was previously deleted, then recreate it and tell the server
             * that we want it saved.
             */

            ReaderBookmarkLocalState.Deleted -> {
              val newBookmarkState =
                ReaderBookmarkState(
                  event.bookmark,
                  ReaderBookmarkLocalState.Saved,
                  ReaderBookmarkRemoteState.Sending)

              this.updateBookmarkState(newBookmarkState)
                .andThen(this.emitOutput(ReaderBookmarkOutput.Command.SendBookmark(event.bookmark)))
                .evaluateFor(initialState)
            }

            /*
             * If the bookmark is already locally saved, then ignore it.
             */

            ReaderBookmarkLocalState.Saved -> {
              this.emitOutput(ReaderBookmarkOutput.Event.LocalBookmarkAlreadyExists(event.bookmark))
                .evaluateFor(initialState)
            }
          }
        } else {

          /*
           * If nothing is known about the bookmark, then save it locally and tell the server
           * that we want it saved.
           */

          val newBookmarkState =
            ReaderBookmarkState(
              event.bookmark,
              ReaderBookmarkLocalState.Saved,
              ReaderBookmarkRemoteState.Sending)

          this.updateBookmarkState(newBookmarkState)
            .andThen(this.emitOutput(ReaderBookmarkOutput.Command.SendBookmark(event.bookmark)))
            .evaluateFor(initialState)
        }
      }
    }
  }
}

data class ReaderBookmarkEngineState(
  val bookmarks: Map<ReaderBookmarkID, ReaderBookmarkState>) {

  companion object {
    fun create(locallySaved: Set<ReaderBookmarkID>): ReaderBookmarkEngineState {
      val states =
        locallySaved.map { id ->
          Pair(id, ReaderBookmarkState(
            bookmarkID = id,
            localState = ReaderBookmarkLocalState.Saved,
            remoteState = ReaderBookmarkRemoteState.Unknown))
        }
      return ReaderBookmarkEngineState(bookmarks = states.toMap())
    }
  }
}

data class ReaderBookmarkState(
  val bookmarkID: ReaderBookmarkID,
  val localState: ReaderBookmarkLocalState,
  val remoteState: ReaderBookmarkRemoteState)

sealed class ReaderBookmarkLocalState {
  object Deleted : ReaderBookmarkLocalState()
  object Saved : ReaderBookmarkLocalState()
}

sealed class ReaderBookmarkRemoteState {
  object Sending : ReaderBookmarkRemoteState()
  object Deleting : ReaderBookmarkRemoteState()
  object Unknown : ReaderBookmarkRemoteState()
  object Saved : ReaderBookmarkRemoteState()
}

sealed class ReaderBookmarkOutput {

  sealed class Command : ReaderBookmarkOutput() {

    data class SaveBookmark(
      val id: ReaderBookmarkID)
      : Command()

    data class SendBookmark(
      val id: ReaderBookmarkID)
      : Command()

    data class DeleteBookmark(
      val id: ReaderBookmarkID)
      : Command()

  }

  sealed class Event : ReaderBookmarkOutput() {

    data class LocalBookmarkAlreadyExists(
      val id: ReaderBookmarkID)
      : Event()

  }

}

sealed class ReaderBookmarkInput {

  sealed class Event : ReaderBookmarkInput() {

    data class BookmarkLocalCreated(
      val bookmark: ReaderBookmarkID)
      : Event()

    data class BookmarkLocalDeleted(
      val bookmark: ReaderBookmarkID)
      : Event()

    data class BookmarkRemoteReceived(
      val bookmark: ReaderBookmarkID)
      : Event()

  }

}
