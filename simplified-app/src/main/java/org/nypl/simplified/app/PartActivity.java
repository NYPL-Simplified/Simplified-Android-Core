package org.nypl.simplified.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.io7m.jfunctional.Unit;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;

/**
 * <p>
 * The type of activities that represent a major part of the application.
 * </p>
 * <p>
 * Each instance contains a layout that has buttons to navigate to each of the
 * other major parts.
 * </p>
 */

@SuppressWarnings("synthetic-access") abstract class PartActivity extends
  Activity implements PartActivityType
{
  private void buttonToBooks(
    final Button b)
  {
    b.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        final Intent i = new Intent(PartActivity.this, BooksActivity.class);

        int flags = 0;
        flags |= Intent.FLAG_ACTIVITY_NO_ANIMATION;

        i.setFlags(flags);
        PartActivity.this.startActivity(i);
      }
    });
  }

  protected void buttonToCatalog(
    final Button b)
  {
    b.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        final Intent i = new Intent(PartActivity.this, CatalogActivity.class);

        int flags = 0;
        flags |= Intent.FLAG_ACTIVITY_NO_ANIMATION;

        i.setFlags(flags);
        PartActivity.this.startActivity(i);
      }
    });
  }

  private void buttonToHolds(
    final Button b)
  {
    b.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        final Intent i = new Intent(PartActivity.this, HoldsActivity.class);

        int flags = 0;
        flags |= Intent.FLAG_ACTIVITY_NO_ANIMATION;

        i.setFlags(flags);
        PartActivity.this.startActivity(i);
      }
    });
  }

  private void buttonToSettings(
    final Button b)
  {
    b.setOnClickListener(new OnClickListener() {
      @Override public void onClick(
        final @Nullable View v)
      {
        final Intent i =
          new Intent(PartActivity.this, SettingsActivity.class);

        int flags = 0;
        flags |= Intent.FLAG_ACTIVITY_NO_ANIMATION;

        i.setFlags(flags);
        PartActivity.this.startActivity(i);
      }
    });
  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);
    this.setContentView(R.layout.part);

    Log.d("PartActivity", "onCreate: " + this);

    final Button catalog_button =
      NullCheck.notNull((Button) this.findViewById(R.id.catalog_button));
    final Button holds_button =
      NullCheck.notNull((Button) this.findViewById(R.id.holds_button));
    final Button books_button =
      NullCheck.notNull((Button) this.findViewById(R.id.books_button));
    final Button settings_button =
      NullCheck.notNull((Button) this.findViewById(R.id.settings_button));

    this
      .matchPartActivity(new PartActivityMatcherType<Unit, UnreachableCodeException>() {
        @Override public Unit books(
          final BooksActivity a)
          throws UnreachableCodeException
        {
          books_button.setEnabled(false);
          PartActivity.this.buttonToCatalog(catalog_button);
          PartActivity.this.buttonToHolds(holds_button);
          PartActivity.this.buttonToSettings(settings_button);
          return Unit.unit();
        }

        @Override public Unit catalog(
          final CatalogActivity a)
          throws UnreachableCodeException
        {
          PartActivity.this.buttonToBooks(books_button);
          catalog_button.setEnabled(false);
          PartActivity.this.buttonToHolds(holds_button);
          PartActivity.this.buttonToSettings(settings_button);
          return Unit.unit();
        }

        @Override public Unit holds(
          final HoldsActivity a)
        {
          PartActivity.this.buttonToBooks(books_button);
          PartActivity.this.buttonToCatalog(catalog_button);
          holds_button.setEnabled(false);
          PartActivity.this.buttonToSettings(settings_button);
          return Unit.unit();
        }

        @Override public Unit settings(
          final SettingsActivity a)
          throws UnreachableCodeException
        {
          PartActivity.this.buttonToBooks(books_button);
          PartActivity.this.buttonToCatalog(catalog_button);
          PartActivity.this.buttonToHolds(holds_button);
          settings_button.setEnabled(false);
          return Unit.unit();
        }
      });
  }

  @Override protected void onDestroy()
  {
    super.onDestroy();
    Log.d("PartActivity", "onDestroy: " + this);
  }

  @Override protected void onResume()
  {
    super.onResume();
    Log.d("PartActivity", "onResume: " + this);
  }
}
