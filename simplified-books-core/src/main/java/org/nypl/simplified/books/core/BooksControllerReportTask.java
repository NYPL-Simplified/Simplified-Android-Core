package org.nypl.simplified.books.core;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.io7m.jfunctional.Option;
import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.Some;
import com.io7m.jnull.NullCheck;

import org.nypl.simplified.http.core.HTTPAuthBasic;
import org.nypl.simplified.http.core.HTTPAuthOAuth;
import org.nypl.simplified.http.core.HTTPAuthType;
import org.nypl.simplified.http.core.HTTPType;
import org.nypl.simplified.json.core.JSONSerializerUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * <p>The logic for reporting a problem with a book.</p>
 */
public class BooksControllerReportTask
  implements Runnable
{
  private static final Logger LOG;

  static {
    LOG = NullCheck.notNull(
      LoggerFactory.getLogger(BooksControllerReportTask.class));
  }

  private final String                             report_type;
  private final FeedEntryOPDS                      feed_entry;
  private final HTTPType                           http;
  private final AccountsDatabaseReadableType       accounts_database;

  BooksControllerReportTask(
    final String in_report_type,
    final FeedEntryOPDS in_feed_entry,
    final HTTPType in_http,
    final AccountsDatabaseReadableType in_accounts_database)
  {
    this.report_type = NullCheck.notNull(in_report_type);
    this.feed_entry = NullCheck.notNull(in_feed_entry);
    this.http = NullCheck.notNull(in_http);
    this.accounts_database = NullCheck.notNull(in_accounts_database);
  }

  @Override
  public void run()
  {
    final ObjectNode report = JsonNodeFactory.instance.objectNode();
    report.set("type", JsonNodeFactory.instance.textNode(this.report_type));
    final OptionType<AccountCredentials> credentials_opt =
      this.accounts_database.accountGetCredentials();
    OptionType<HTTPAuthType> http_auth = Option.none();
    if (credentials_opt.isSome()) {
      final AccountCredentials account_credentials = ((Some<AccountCredentials>) credentials_opt).get();
      final AccountBarcode barcode = account_credentials.getUser();
      final AccountPIN pin = account_credentials.getPassword();

      http_auth =
        Option.some((HTTPAuthType) new HTTPAuthBasic(barcode.toString(), pin.toString()));

      if (account_credentials.getAuthToken().isSome()) {
        final AccountAuthToken token = ((Some<AccountAuthToken>) account_credentials.getAuthToken()).get();
        if (token != null) {
          http_auth = Option.some((HTTPAuthType)new HTTPAuthOAuth(token.toString()));
        }
      }

    }

    try {
      final String report_string = JSONSerializerUtilities.serializeToString(report);
      final OptionType<URI> issues_opt = this.feed_entry.getFeedEntry().getIssues();
      if (issues_opt.isSome()) {
        final Some<URI> issues_some = (Some<URI>) issues_opt;
        this.http.post(http_auth, issues_some.get(), report_string.getBytes(), "application/problem+json");
      }
    } catch (IOException e) {
      this.LOG.warn("Failed to submit problem report.");
    }
  }
}
