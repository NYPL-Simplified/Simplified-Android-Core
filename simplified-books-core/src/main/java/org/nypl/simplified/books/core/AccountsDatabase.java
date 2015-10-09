/*
 * Copyright © 2015 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.nypl.simplified.books.core;

import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import org.nypl.simplified.files.DirectoryUtilities;
import org.nypl.simplified.files.FileLocking;
import org.nypl.simplified.files.FileUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The default implementation of the {@link AccountsDatabaseType} interface.
 */

public final class AccountsDatabase implements AccountsDatabaseType
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(AccountsDatabase.class));
  }

  private final File                                directory;
  private final File                                file_accounts;
  private final File                                file_accounts_tmp;
  private final File                                file_lock;
  private final AtomicReference<AccountCredentials> cached;

  private AccountsDatabase(final File in_directory)
  {
    this.directory = NullCheck.notNull(in_directory);
    this.file_lock = new File(in_directory, "lock");
    this.file_accounts = new File(in_directory, "account.json");
    this.file_accounts_tmp = new File(in_directory, "account.json.tmp");

    this.cached = new AtomicReference<AccountCredentials>();
    if (this.file_accounts.exists()) {
      try {
        final String text = FileUtilities.fileReadUTF8(this.file_accounts);
        this.cached.set(AccountCredentialsJSON.deserializeFromText(text));
      } catch (final IOException e) {
        AccountsDatabase.LOG.error("could not load account: ", e);
      }
    }
  }

  /**
   * Open the accounts database.
   *
   * @param directory The directory
   *
   * @return A database
   */

  public static AccountsDatabaseType openDatabase(final File directory)
  {
    return new AccountsDatabase(directory);
  }

  @Override public void accountSetCredentials(final AccountCredentials c)
    throws IOException
  {
    FileLocking.withFileThreadLocked(
      this.file_lock, 1000L, new PartialFunctionType<Unit, Unit, IOException>()
      {
        @Override public Unit call(
          final Unit x)
          throws IOException
        {
          AccountsDatabase.this.accountSetCredentialsLocked(c);
          return Unit.unit();
        }
      });
  }

  private void accountSetCredentialsLocked(final AccountCredentials c)
    throws IOException
  {
    DirectoryUtilities.directoryCreate(this.directory);
    final String text = AccountCredentialsJSON.serializeToText(c);
    FileUtilities.fileWriteUTF8Atomically(
      this.file_accounts, this.file_accounts_tmp, text);
    this.cached.set(c);
  }

  @Override public void accountRemoveCredentials()
    throws IOException
  {
    FileLocking.withFileThreadLocked(
      this.file_lock, 1000L, new PartialFunctionType<Unit, Unit, IOException>()
      {
        @Override public Unit call(
          final Unit x)
          throws IOException
        {
          AccountsDatabase.this.accountRemoveCredentialsLocked();
          return Unit.unit();
        }
      });
  }

  private void accountRemoveCredentialsLocked()
    throws IOException
  {
    FileUtilities.fileDelete(this.file_accounts);
    FileUtilities.fileDelete(this.file_accounts_tmp);
    this.cached.set(null);
  }

  @Override public OptionType<AccountCredentials> accountGetCredentials()
  {
    return Option.of(this.cached.get());
  }
}
