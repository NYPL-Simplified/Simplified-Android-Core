package org.nypl.simplified.books.book_registry;

/**
 * <p> An enum representing the possible types of book status, to make it
 * slightly easier to place an ordering on the priorities of status values.
 * </p>
 *
 * <p> The reason that this is necessary is because status updates may be
 * received in an arbitrary order and, in particular, late. The system needs to
 * be able to determine if a status update is important enough to override an
 * existing status update that was received out of order. The numeric priority
 * values are arbitrary, meant only to provide a partial order, and are subject
 * to change. </p>
 */

public enum BookStatusPriorityOrdering
{
  /**
   * {@link BookStatus.Loaned.LoanRevokeFailed}
   */

  BOOK_STATUS_REVOKE_FAILED(90),

  /**
   * {@link BookStatus.Loaned.Downloading.DownloadFailed}
   */

  BOOK_STATUS_DOWNLOAD_FAILED(90),

  /**
   * {@link BookStatus.Loaned.Downloading.Downloading.DownloadExternalAuthenticationInProgress}
   */

  BOOK_STATUS_DOWNLOAD_EXTERNAL_AUTHENTICATION_IN_PROGRESS(80),

  /**
   * {@link BookStatus.Loaned.Downloading.Downloading.DownloadWaitingForExternalAuthentication}
   */

  BOOK_STATUS_WAITING_FOR_EXTERNAL_AUTHENTICATION(70),


  /**
   * {@link BookStatus.Loaned.Downloading.DownloadInProgress}
   */

  BOOK_STATUS_DOWNLOAD_IN_PROGRESS(60),

  /**
   * {@link BookStatus.Loaned.Downloading.RequestingDownload}
   */

  BOOK_STATUS_DOWNLOAD_REQUESTING(50),

  /**
   * {@link BookStatus.Loaned.LoanedDownloaded}
   */

  BOOK_STATUS_DOWNLOADED(100),

  /**
   * {@link BookStatus.Loanable}
   */

  BOOK_STATUS_LOANABLE(15),

  /**
   * {@link BookStatus.Held.HeldReady}
   */

  BOOK_STATUS_HELD_READY(12),

  /**
   * {@link BookStatus.Held}
   */

  BOOK_STATUS_HELD(10),

  /**
   * {@link BookStatus.Holdable}
   */

  BOOK_STATUS_HOLDABLE(0),

  /**
   * {@link BookStatus.RequestingLoan}
   */

  BOOK_STATUS_LOAN_IN_PROGRESS(20),

  /**
   * {@link BookStatus.Loaned}
   */

  BOOK_STATUS_LOANED(30),

  /**
   * {@link BookStatus.Held.RevokingHold}
   */

  BOOK_STATUS_REVOKE_IN_PROGRESS(20);

  private final int priority;

  BookStatusPriorityOrdering(
    final int in_priority)
  {
    this.priority = in_priority;
  }

  /**
   * @return The priority
   */

  public int getPriority()
  {
    return this.priority;
  }
}
