package org.nypl.simplified.app.testing;

import android.app.ActionBar;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.Simplified;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.SimplifiedCatalogAppServicesType;
import org.nypl.simplified.app.SimplifiedPart;
import org.nypl.simplified.books.core.BooksControllerConfigurationType;
import org.nypl.simplified.books.core.LogUtilities;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * An activity allowing the specification of alternative URLs.
 */

public final class AlternateFeedURIsActivity extends SimplifiedActivity
{
  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(AlternateFeedURIsActivity.class);
  }

  /**
   * Construct an activity.
   */

  public AlternateFeedURIsActivity()
  {

  }

  @Override protected SimplifiedPart navigationDrawerGetPart()
  {
    return SimplifiedPart.PART_SETTINGS;
  }

  @Override protected boolean navigationDrawerShouldShowIndicator()
  {
    return false;
  }

  @Override protected void onCreate(final @Nullable Bundle state)
  {
    super.onCreate(state);

    final ActionBar bar = this.getActionBar();
    bar.setTitle("Alternate URIs");
    if (android.os.Build.VERSION.SDK_INT < 21) {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(true);
      bar.setIcon(R.drawable.ic_arrow_back);
    }
    else
    {
      bar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(false);
    }

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());

    final FrameLayout content_area = this.getContentFrame();
    final ViewGroup layout = NullCheck.notNull(
      (ViewGroup) inflater.inflate(
        R.layout.alternate_feed_uris, content_area, false));
    content_area.addView(layout);
    content_area.requestLayout();

    final EditText in_uri_feed =
      NullCheck.notNull((EditText) layout.findViewById(R.id.alt_root_url));
    final EditText in_uri_loans =
      NullCheck.notNull((EditText) layout.findViewById(R.id.alt_loans_url));
    final Button in_set =
      NullCheck.notNull((Button) layout.findViewById(R.id.alt_set));
    final Button in_reset =
      NullCheck.notNull((Button) layout.findViewById(R.id.alt_reset));

    final BooksControllerConfigurationType books_config =
      app.getBooks().booksGetConfiguration();
    if (books_config.getAlternateRootFeedURI() != null) {
      in_uri_feed.setText(books_config.getAlternateRootFeedURI().toString());
    }

    in_set.setOnClickListener(
      new View.OnClickListener()
      {
        @Override public void onClick(final View v)
        {
          final Context context =
            AlternateFeedURIsActivity.this.getApplicationContext();

          final URI new_root;
          final URI new_loans;

          try {
            new_root = new URI(in_uri_feed.getText().toString());
          } catch (final URISyntaxException e) {
            AlternateFeedURIsActivity.LOG.error("invalid feed uri: ", e);
            final CharSequence text = "Invalid feed URI: " + e.getMessage();
            final int duration = Toast.LENGTH_LONG;
            final Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
          }

          try {
            new_loans = new URI(in_uri_loans.getText().toString());
          } catch (final URISyntaxException e) {
            AlternateFeedURIsActivity.LOG.error("invalid feed uri: ", e);
            final CharSequence text = "Invalid loans URI: " + e.getMessage();
            final int duration = Toast.LENGTH_LONG;
            final Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
          }

//          books_config.setAlternateLoansURI(new_loans);
          books_config.setAlternateRootFeedURI(new_root);

          final CharSequence text =
            "URIs configured! Please open the catalog from the navigation "
            + "drawer";
          final int duration = Toast.LENGTH_LONG;
          final Toast toast = Toast.makeText(context, text, duration);
          toast.show();
        }
      });

    in_reset.setOnClickListener(
      new View.OnClickListener()
      {
        @Override public void onClick(final View v)
        {
          final Context context =
            AlternateFeedURIsActivity.this.getApplicationContext();

          in_uri_loans.setText(null);
          in_uri_feed.setText(null);
//          books_config.setAlternateLoansURI(null);
          books_config.setAlternateRootFeedURI(null);

        }
      });
  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn)
  {
    final MenuItem item = NullCheck.notNull(item_mn);
    switch (item.getItemId()) {

      case android.R.id.home: {
        onBackPressed();
        return true;
      }

      default: {
        return super.onOptionsItemSelected(item);
      }
    }
  }
}
