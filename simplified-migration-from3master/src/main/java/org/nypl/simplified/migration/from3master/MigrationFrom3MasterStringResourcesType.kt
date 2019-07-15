package org.nypl.simplified.migration.from3master

/**
 * The strings published during migration.
 */

interface MigrationFrom3MasterStringResourcesType {

  fun errorUnknownAccountProvider(
    id: Int): String

  fun errorAccountLoadFailure(
    id: Int): String

  fun errorBookLoadFailure(
    entry: String): String

  fun errorBookUnexpectedFormat(
    title: String,
    receivedFormat: String)
    : String

  fun errorBookLoadTitledFailure(
    title: String): String

  fun reportCreatedAccount(
    title: String): String

  fun reportCopiedBook(
    title: String): String
}
