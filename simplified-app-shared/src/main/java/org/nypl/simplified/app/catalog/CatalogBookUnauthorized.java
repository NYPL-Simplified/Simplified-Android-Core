package org.nypl.simplified.app.catalog;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import org.nypl.simplified.books.core.BookStatusDownloadFailed;
import org.nypl.simplified.books.core.BookStatusRevokeFailed;
import org.nypl.simplified.books.core.FeedHTTPTransportException;
import org.nypl.simplified.http.core.HTTPProblemReport;

final class CatalogBookUnauthorized
{
  private CatalogBookUnauthorized()
  {
    throw new UnreachableCodeException();
  }

  /**
   * @param s Download status
   *
   * @return An appropriate failure string for the given status value.
   */

  public static boolean isUnAuthorized(
    final BookStatusDownloadFailed s)
  {
    NullCheck.notNull(s);

    final OptionType<Throwable> error_opt = s.getError();
    if (error_opt.isSome()) {
      final Some<Throwable> error_some = (Some<Throwable>) error_opt;
      final Throwable error = error_some.get();

      final Throwable cause = error.getCause();
      if (cause != null && cause instanceof FeedHTTPTransportException) {
        final OptionType<HTTPProblemReport> problem_opt = ((FeedHTTPTransportException) cause).getProblemReport();
        if (problem_opt.isSome()) {
          final HTTPProblemReport problem = ((Some<HTTPProblemReport>) problem_opt).get();

          return problem.getProblemStatus() == HTTPProblemReport.ProblemStatus.Unauthorized;
        }
      }
    }
    return false;
  }

  public static boolean isUnAuthorized(
    final BookStatusRevokeFailed s)
  {
    NullCheck.notNull(s);

    final OptionType<Throwable> error_opt = s.getError();
    if (error_opt.isSome()) {
      final Some<Throwable> error_some = (Some<Throwable>) error_opt;
      final Throwable error = error_some.get();

      if (error != null && error instanceof FeedHTTPTransportException) {
        final OptionType<HTTPProblemReport> problem_opt = ((FeedHTTPTransportException) error).getProblemReport();
        if (problem_opt.isSome()) {
          final HTTPProblemReport problem = ((Some<HTTPProblemReport>) problem_opt).get();

          return problem.getProblemStatus() == HTTPProblemReport.ProblemStatus.Unauthorized;
        }
      }
    }
    return false;
  }
}
