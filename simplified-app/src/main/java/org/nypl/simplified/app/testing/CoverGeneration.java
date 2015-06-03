package org.nypl.simplified.app.testing;

import org.nypl.simplified.app.R;
import org.nypl.simplified.tenprint.TenPrintGenerator;
import org.nypl.simplified.tenprint.TenPrintGeneratorType;
import org.nypl.simplified.tenprint.TenPrintInput;
import org.nypl.simplified.tenprint.TenPrintInputBuilderType;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class CoverGeneration extends Activity
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    final TenPrintGeneratorType g = TenPrintGenerator.newGenerator();

    final TenPrintInputBuilderType b = TenPrintInput.newBuilder();
    b.setAuthor("An author");
    b.setTitle(" qwertyuiopasdfghjkl:zxcvbnm1234567890.");
    b.setBaseBrightness(0.9f);
    b.setBaseSaturation(0.9f);
    b.setCoverHeight(600);
    b.setGridScale(1.0f);

    final TenPrintInput i = b.build();

    final Bitmap image = g.generate(i);

    this.setContentView(R.layout.test_cover_gen);

    final ViewGroup container =
      NullCheck.notNull((ViewGroup) this
        .findViewById(R.id.test_cover_container));

    final ImageView v = new ImageView(this);
    v.setLayoutParams(new FrameLayout.LayoutParams(image.getWidth(), image
      .getHeight()));
    v.setImageBitmap(image);

    container.addView(v);
  }
}
