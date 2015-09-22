package org.nypl.simplified.app.testing;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.nypl.simplified.app.R;
import org.nypl.simplified.tenprint.TenPrintGenerator;
import org.nypl.simplified.tenprint.TenPrintGeneratorType;
import org.nypl.simplified.tenprint.TenPrintInput;
import org.nypl.simplified.tenprint.TenPrintInputBuilderType;

/**
 * A cover generation activity.
 */

public final class CoverGeneration extends Activity
{
  /**
   * Construct an activity.
   */

  public CoverGeneration()
  {

  }

  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final TenPrintGeneratorType g = TenPrintGenerator.newGenerator();

    final TenPrintInputBuilderType b = TenPrintInput.newBuilder();
    b.setAuthor("G. Mercer Adam, A Ethelwyn Wetherby");
    b.setTitle("An Algonquin Maiden: A Romance ...");
    b.setBaseBrightness(0.9f);
    b.setBaseSaturation(0.9f);
    b.setCoverHeight(120);
    b.setGridScale(1.0f);
    b.setShapeThickness(15);
    b.setDebuggingArtwork(false);

    final TenPrintInput i = b.build();
    final Bitmap image = g.generate(i);
    this.setContentView(R.layout.test_cover_gen);

    final ViewGroup container = NullCheck.notNull(
      (ViewGroup) this.findViewById(R.id.test_cover_container));

    final ImageView v = new ImageView(this);
    v.setLayoutParams(
      new FrameLayout.LayoutParams(
        image.getWidth(), image.getHeight()));
    v.setImageBitmap(image);
    container.addView(v);
  }
}
