package org.nypl.simplified.accounts.database

import net.jcip.annotations.GuardedBy
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentials
import org.nypl.simplified.accounts.api.AccountAuthenticationCredentialsStoreType
import org.nypl.simplified.accounts.api.AccountID
import org.nypl.simplified.accounts.json.AccountAuthenticationCredentialsStoreJSON
import org.nypl.simplified.files.FileUtilities
import java.io.File

/**
 * A trivial credential store that serializes credentials to an on-disk JSON file.
 */

class AccountAuthenticationCredentialsStore(
  private val file: File,
  private val fileTemp: File,
  initialCredentials: Map<AccountID, AccountAuthenticationCredentials>
) : AccountAuthenticationCredentialsStoreType {

  companion object {

    /**
     * Open a credential store, or create a new one if it does not exist.
     */

    fun open(
      file: File,
      fileTemp: File
    ): AccountAuthenticationCredentialsStoreType {
      val initialCredentials =
        if (file.isFile) {
          val text = FileUtilities.fileReadUTF8(file)
          if (text.isNotEmpty()) {
            AccountAuthenticationCredentialsStoreJSON.deserializeFromText(text)
          } else {
            mapOf()
          }
        } else {
          mapOf()
        }

      val store =
        AccountAuthenticationCredentialsStore(
          file = file,
          fileTemp = fileTemp,
          initialCredentials = initialCredentials
        )

      synchronized(store.storeLock) {
        store.writeLocked()
      }
      return store
    }
  }

  private val storeLock = Object()
  @GuardedBy("storeLock")
  private var store = initialCredentials.toMap()

  override fun get(account: AccountID): AccountAuthenticationCredentials? {
    return synchronized(this.storeLock) {
      this.store[account]
    }
  }

  override fun size(): Int {
    return synchronized(this.storeLock) {
      this.store.size
    }
  }

  override fun put(
    account: AccountID,
    credentials: AccountAuthenticationCredentials
  ) {
    synchronized(this.storeLock) {
      this.store = this.store.plus(Pair(account, credentials))
      this.writeLocked()
    }
  }

  override fun delete(account: AccountID) {
    synchronized(this.storeLock) {
      this.store = this.store.minus(account)
      this.writeLocked()
    }
  }

  private fun writeLocked() {
    FileUtilities.fileWriteUTF8Atomically(
      this.file,
      this.fileTemp,
      AccountAuthenticationCredentialsStoreJSON.serializeToText(this.store)
    )
  }
}
