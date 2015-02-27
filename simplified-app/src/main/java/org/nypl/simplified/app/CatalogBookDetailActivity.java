package org.nypl.simplified.app;

import java.net.URI;

import org.nypl.simplified.opds.core.OPDSAcquisitionFeedEntry;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.common.collect.ImmutableList;
import com.io7m.jnull.NullCheck;

public final class CatalogBookDetailActivity extends CatalogActivity
{
  public static void setActivityArguments(
    final Bundle b,
    final boolean drawer_open,
    final ImmutableList<URI> up_stack,
    final OPDSAcquisitionFeedEntry e)
  {
    NullCheck.notNull(b);
    SimplifiedActivity.setActivityArguments(b, drawer_open);
    CatalogActivity.setActivityArguments(b, up_stack);
  }

  public static void startNewActivity(
    final Activity from,
    final ImmutableList<URI> up_stack,
    final OPDSAcquisitionFeedEntry e)
  {
    final Bundle b = new Bundle();
    CatalogBookDetailActivity.setActivityArguments(b, false, up_stack, e);
    final Intent i = new Intent(from, CatalogBookDetailActivity.class);
    i.putExtras(b);
    i.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    from.startActivity(i);
  }
}
