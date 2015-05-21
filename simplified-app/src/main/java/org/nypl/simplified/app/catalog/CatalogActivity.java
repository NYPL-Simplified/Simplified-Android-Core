package org.nypl.simplified.app.catalog;

import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.utilities.FadeUtilities;
import org.nypl.simplified.app.utilities.LogUtilities;
import org.nypl.simplified.stack.ImmutableStack;
import org.slf4j.Logger;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

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
    final ImmutableStack<CatalogUpStackEntry> up_stack)
  {
    NullCheck.notNull(b);
    b.putSerializable(
      CatalogActivity.CATALOG_UP_STACK_ID,
      NullCheck.notNull(up_stack));
  }

  private void configureUpButton(
    final ImmutableStack<CatalogUpStackEntry> up_stack)
  {
    CatalogActivity.LOG.debug("up stack: {}", up_stack);

    final ActionBar bar = this.getActionBar();
    if (up_stack.isEmpty() == false) {
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(true);
    }
  }

  @SuppressWarnings("unchecked") protected final
    ImmutableStack<CatalogUpStackEntry>
    getUpStack()
  {
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = i.getExtras();
    if (a != null) {
      final ImmutableStack<CatalogUpStackEntry> stack =
        (ImmutableStack<CatalogUpStackEntry>) a
          .getSerializable(CatalogActivity.CATALOG_UP_STACK_ID);
      if (stack != null) {
        return stack;
      }
    }

    final ImmutableStack<CatalogUpStackEntry> empty = ImmutableStack.empty();
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
        final ImmutableStack<CatalogUpStackEntry> us = this.getUpStack();

        /**
         * If the stack is non-empty, then the user is not at the root of the
         * catalog. If it is empty, then the button is only enabled to control
         * the navigation drawer and therefore the event should be propagated.
         */

        if (us.isEmpty() == false) {
          CatalogActivity.LOG.debug("up stack before pop: {}", us);

          final Pair<CatalogUpStackEntry, ImmutableStack<CatalogUpStackEntry>> p =
            us.pop();

          final ImmutableStack<CatalogUpStackEntry> stack = p.getRight();
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

        return super.onOptionsItemSelected(item);
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
