package org.nypl.simplified.migration.spi

import android.content.Context
import io.reactivex.Observable
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountEvent
import org.nypl.simplified.accounts.database.api.AccountType
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

  val createAccount: (URI) -> TaskResult<AccountType>,

  /**
   * A function that, given an account, tries to log in.
   */

  val loginAccount: (AccountType, AccountAuthenticationCredentials) -> TaskResult<Unit>,

  /**
   * A source of account events.
   */

  val accountEvents: Observable<AccountEvent>,

  /**
   * `true` if the application is running in anonymous profile mode.
   */

  val applicationProfileIsAnonymous: Boolean,

  /**
   * The application name and version (such as `org.nypl.simplified.vanilla 0.0.1 (2000)`).
   */

  val applicationVersion: String,

  /**
   * The Android application context.
   */

  val context: Context
)
