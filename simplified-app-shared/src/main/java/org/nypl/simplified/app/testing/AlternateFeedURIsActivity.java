package org.nypl.simplified.app.testing;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.Resources;
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
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.books.core.BooksControllerConfigurationType;
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
    bar.setHomeAsUpIndicator(R.drawable.ic_drawer);
    bar.setDisplayHomeAsUpEnabled(true);
    bar.setHomeButtonEnabled(true);
    bar.setTitle("Alternate URIs");

    final SimplifiedCatalogAppServicesType app =
      Simplified.getCatalogAppServices();

    final LayoutInflater inflater = NullCheck.notNull(this.getLayoutInflater());
    final Resources resources = NullCheck.notNull(this.getResources());

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

    final BooksControllerConfigurationType books_config =
      app.getBooks().booksGetConfiguration();
    in_uri_feed.setText(books_config.getCurrentRootFeedURI().toString());
    in_uri_loans.setText(books_config.getCurrentLoansURI().toString());

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

          books_config.setCurrentLoansURI(new_loans);
          books_config.setCurrentRootFeedURI(new_root);

          final CharSequence text =
            "URIs configured! Please open the catalog from the navigation "
            + "drawer";
          final int duration = Toast.LENGTH_LONG;
          final Toast toast = Toast.makeText(context, text, duration);
          toast.show();
        }
      });
  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn)
  {
    final MenuItem item = NullCheck.notNull(item_mn);
    switch (item.getItemId()) {

      /**
       * Configure the home button to finish the activity.
       */

      case android.R.id.home: {
        this.finish();
        this.overridePendingTransition(0, 0);
        return true;
      }
    }

    return super.onOptionsItemSelected(item);
  }
}
