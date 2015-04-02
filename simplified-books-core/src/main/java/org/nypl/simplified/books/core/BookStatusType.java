package org.nypl.simplified.books.core;

public interface BookStatusType
{
  BookID getID();

  <A, E extends Exception> A matchBookStatus(
    final BookStatusMatcherType<A, E> m)
    throws E;
}
