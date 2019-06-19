package org.nypl.simplified.books.controller.api

/**
 * An exception indicating that book revoking failed because the book is not revocable.
 */

class BookRevokeExceptionNotRevocable : BookRevokeException("Not revocable!")
