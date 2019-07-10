package org.nypl.simplified.books.controller.api

/**
 * An exception indicating that book revoking failed because server returned an unusable feed.
 */

class BookRevokeExceptionBadFeed : BookRevokeException("Unusable feed")
