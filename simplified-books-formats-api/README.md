org.librarysimplified.books.formats.api
===

The `org.librarysimplified.books.formats.api` module specifies the API for book format support.
It essentially provides an API that unambiguously states whether or not books of a given format
are supported by the current application configuration. This information is used to filter out
entries from OPDS feeds, and to control how books are actually acquired.
