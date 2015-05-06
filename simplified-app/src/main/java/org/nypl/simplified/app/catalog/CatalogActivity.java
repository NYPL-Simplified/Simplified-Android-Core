package org.nypl.simplified.app.catalog;

import java.util.List;

import org.nypl.simplified.app.R;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.utilities.FadeUtilities;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.app.utilities.StackUtilities;
import org.slf4j.Logger;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public abstract class CatalogActivity extends SimplifiedActivity
{
  private static final String CATALOG_UP_STACK_ID;

  private static final Logger LOG;

  static {
    LOG = LogUtilities.getLog(CatalogActivity.class);
  }

  static {
    CATALOG_UP_STACK_ID = "org.nypl.simplified.app.CatalogActivity.up_stack";
  }

  public static void setActivityArguments(
    final Bundle b,
    final ImmutableList<CatalogUpStackEntry> up_stack)
  {
    NullCheck.notNull(b);
    b.putSerializable(
      CatalogActivity.CATALOG_UP_STACK_ID,
      NullCheck.notNull(up_stack));
  }

  private void configureUpButton(
    final List<CatalogUpStackEntry> up_stack)
  {
    CatalogActivity.LOG.debug("up stack: {}", up_stack);

    final ActionBar bar = this.getActionBar();
    if (up_stack.isEmpty() == false) {
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(true);
    } else {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(false);
    }
  }

  @SuppressWarnings("unchecked") protected final
    List<CatalogUpStackEntry>
    getUpStack()
  {
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = i.getExtras();
    if (a != null) {
      return (List<CatalogUpStackEntry>) NullCheck.notNull(a
        .getSerializable(CatalogActivity.CATALOG_UP_STACK_ID));
    }

    final ImmutableList<CatalogUpStackEntry> empty = ImmutableList.of();
    return NullCheck.notNull(empty);
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item_mn)
  {
    final MenuItem item = NullCheck.notNull(item_mn);
    switch (item.getItemId()) {

    /**
     * Configure the home button to start a new activity with a popped
     * up-stack.
     */

      case android.R.id.home:
      {
        final List<CatalogUpStackEntry> us = this.getUpStack();
        Preconditions.checkArgument(us.isEmpty() == false);

        CatalogActivity.LOG.debug("up stack before pop: {}", us);

        final Pair<CatalogUpStackEntry, ImmutableList<CatalogUpStackEntry>> p =
          StackUtilities.stackPop(us);

        final ImmutableList<CatalogUpStackEntry> stack = p.getRight();
        final CatalogUpStackEntry top = p.getLeft();

        final CatalogFeedArgumentsRemote remote =
          new CatalogFeedArgumentsRemote(
            false,
            stack,
            top.getTitle(),
            top.getURI());
        CatalogFeedActivity.startNewActivity(this, remote);
        return true;
      }

      /**
       * Rotate the screen, for debugging purposes.
       */

      case R.id.tilt:
      {
        CatalogActivity.LOG.debug("flipping orientation");
        final int o = this.getRequestedOrientation();
        CatalogActivity.LOG.debug("current orientation: {}", o);
        if ((o == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
          || (o == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)) {
          this
            .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
          this
            .setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        return true;
      }

      default:
      {
        return super.onOptionsItemSelected(item);
      }
    }
  }

  @Override protected void onResume()
  {
    super.onResume();
    this.configureUpButton(this.getUpStack());

    final FrameLayout content_area = this.getContentFrame();
    FadeUtilities.fadeIn(content_area, FadeUtilities.DEFAULT_FADE_DURATION);
  }
}
