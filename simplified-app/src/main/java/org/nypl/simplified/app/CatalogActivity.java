package org.nypl.simplified.app;

import java.net.URI;
import java.util.List;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.common.collect.ImmutableList;
import com.io7m.jfunctional.Pair;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public abstract class CatalogActivity extends SimplifiedActivity
{
  private static final String CATALOG_UP_STACK_ID;

  static {
    CATALOG_UP_STACK_ID = "org.nypl.simplified.app.CatalogActivity.up_stack";
  }

  public static void setActivityArguments(
    final Bundle b,
    final ImmutableList<URI> up_stack)
  {
    NullCheck.notNull(b);
    b.putSerializable(
      CatalogActivity.CATALOG_UP_STACK_ID,
      NullCheck.notNull(up_stack));
  }

  private void configureUpButton(
    final List<URI> up_stack)
  {
    final ActionBar bar = this.getActionBar();
    if (up_stack.isEmpty() == false) {
      bar.setDisplayHomeAsUpEnabled(true);
      bar.setHomeButtonEnabled(true);
    } else {
      bar.setDisplayHomeAsUpEnabled(false);
      bar.setHomeButtonEnabled(false);
    }
  }

  @SuppressWarnings("unchecked") protected final List<URI> getUpStack()
  {
    final Intent i = NullCheck.notNull(this.getIntent());
    final Bundle a = i.getExtras();
    if (a != null) {
      return (List<URI>) NullCheck.notNull(a
        .getSerializable(CatalogActivity.CATALOG_UP_STACK_ID));
    }
    return ImmutableList.of();
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
  }

  @Override protected void onResume()
  {
    super.onResume();
    this.configureUpButton(this.getUpStack());
  }

  @Override public boolean onOptionsItemSelected(
    final @Nullable MenuItem item)
  {
    assert item != null;
    switch (item.getItemId()) {
      case android.R.id.home:
      {
        final List<URI> us = this.getUpStack();
        assert us.isEmpty() == false;
        final Pair<URI, ImmutableList<URI>> p = StackUtilities.stackPop(us);
        CatalogFeedActivity.startNewActivity(this, p.getRight(), p.getLeft());
        return true;
      }

      default:
      {
        return super.onOptionsItemSelected(item);
      }
    }
  }
}
