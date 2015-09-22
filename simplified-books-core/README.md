Books Core
==========

The `simplified-books-core` package implements all of the code to
manage books.

The package provides access to an on-disk _book database_, and
an in-memory _status cache_. The _book database_ provides the
authoritative view of the current state of a given book, and the
_status cache_ allows user interface code to quickly access the latest
published state of a book without having to perform any blocking I/O.

The package provides the main _feed_ abstraction. The application
mostly deals with `FeedType` values directly, and so is mostly
insulated from the details of `OPDS` feeds. This also allows the
application to construct feeds of books programmatically without
having to go to the trouble of generating full `OPDS` feeds.

The _book controller_ interface exposes a set of asynchronous
operations such as borrowing books, deleting books, returning or
cancelling loans and holds, synchronizing accounts, etc. It is also
capable of generating feeds of the books in the current _book database_
to allow the application to show the user the current list of loans
or holds.

