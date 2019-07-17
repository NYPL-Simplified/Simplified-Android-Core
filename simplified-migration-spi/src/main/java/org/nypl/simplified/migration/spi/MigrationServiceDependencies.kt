package org.nypl.simplified.migration.spi

import android.content.Context
import org.nypl.simplified.accounts.api.AccountCreateErrorDetails
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.api.AccountLoginState
import org.nypl.simplified.accounts.api.AccountLoginState.*
import org.nypl.simplified.accounts.database.api.AccountType
import org.nypl.simplified.observable.ObservableReadableType
import org.nypl.simplified.taskrecorder.api.TaskResult
import java.net.URI

/**
 * The services required by migration services.
 */

data class MigrationServiceDependencies(

  /**
   * A function that, given the URI of an account provider, tries to create an account in the
   * current profile.
   */

  val createAccount: (URI) -> TaskResult<AccountCreateErrorDetails, AccountType>,

  /**
   * A function that, given an account, tries to log in.
   */

  val loginAccount: (AccountType) -> TaskResult<AccountLoginErrorData, Unit>,

  /**
   * A source of account events.
   */

  val accountEvents: ObservableReadableType<AccountEvent>,

  /**
   * `true` if the application is running in anonymous profile mode.
   */

  val applicationProfileIsAnonymous: Boolean,

  /**
   * The Android application context.
   */

  val context: Context)