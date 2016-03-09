package org.nypl.simplified.app.catalog;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import org.nypl.simplified.app.R;
import org.nypl.simplified.app.SimplifiedActivity;
import org.nypl.simplified.app.utilities.FadeUtilities;
import org.nypl.simplified.books.core.LogUtilities;
import org.nypl.simplified.stack.ImmutableStack;
import org.slf4j.Logger;

/**
 * An abstract activity providing <i>up</i> navigation using the home button.
 */

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

  /**
   * Set the arguments for the activity that will be created.
   *
   * @param b        The argument bundle
   * @param up_stack The up stack for the created activity
   */

  public static void setActivityArguments(
    final Bundle b,
    final ImmutableStack<CatalogFeedArgumentsType> up_stack)
  {
    NullCheck.notNull(b);
    b.putSerializable(
      CatalogActivity.CATALOG_UP_STACK_ID, NullCheck.notNull(up_stack));
  }

  private void configureUpButton(
    final ImmutableStack<CatalogFeedArgumentsType> up_stack)
  {
    CatalogActivity.LOG.debug("up stack: {}", up_stack);

    final ActionBar bar = this.getActionBar();
    if (up_stack.isEmpty() == false) {
      bar.setHomeAsUpIndicator(R.drawable.ic_drawer);
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(true);
    }
  }

  @SuppressWarnings("unchecked")
  protected final ImmutableStack<CatalogFeedArgumentsType> getUpStack()
  {
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = i.getExtras();
    if (a != null) {
      final ImmutableStack<CatalogFeedArgumentsType> stack =
        (ImmutableStack<CatalogFeedArgumentsType>) a.getSerializable(
          CatalogActivity.CATALOG_UP_STACK_ID);
      if (stack != null) {
        return stack;
      }
    }

    final ImmutableStack<CatalogFeedArgumentsType> empty =
      ImmutableStack.empty();
    return NullCheck.notNull(empty);
  }

  @Override protected void onResume()
  {
    super.onResume();
    this.configureUpButton(this.getUpStack());

    final FrameLayout content_area = this.getContentFrame();
    FadeUtilities.fadeIn(content_area, FadeUtilities.DEFAULT_FADE_DURATION);
  }

  /**
   * Decide which class is necessary to show a feed based on the given
   * arguments.
   *
   * @param args The arguments
   *
   * @return An activity class
   */

  private Class<? extends CatalogFeedActivity> getFeedClassForArguments(
    final CatalogFeedArgumentsType args)
  {
    NullCheck.notNull(args);
    return args.matchArguments(
      new CatalogFeedArgumentsMatcherType<Class<? extends
        CatalogFeedActivity>, UnreachableCodeException>()
      {
        @Override
        public Class<? extends CatalogFeedActivity> onFeedArgumentsLocalBooks(
          final CatalogFeedArgumentsLocalBooks c)
        {
          switch (c.getSelection()) {
            case BOOKS_FEED_LOANED:
              return MainBooksActivity.class;
            case BOOKS_FEED_HOLDS:
              return MainHoldsActivity.class;
          }

          throw new UnreachableCodeException();
        }

        @Override
        public Class<? extends CatalogFeedActivity> onFeedArgumentsRemote(
          final CatalogFeedArgumentsRemote c)
        {
          return MainCatalogActivity.class;
        }
      });
  }

  protected final void catalogActivityForkNew(
    final CatalogFeedArgumentsType args)
  {
    NullCheck.notNull(args);

    final Bundle b = new Bundle();
    CatalogFeedActivity.setActivityArguments(b, args);
    final Intent i = new Intent(this, this.getFeedClassForArguments(args));
    i.putExtras(b);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    this.startActivity(i);
  }

  protected final void catalogActivityForkNewReplacing(
    final CatalogFeedArgumentsType args)
  {
    NullCheck.notNull(args);

    this.catalogActivityForkNew(args);
    this.finish();
    this.overridePendingTransition(0, 0);
  }
}
