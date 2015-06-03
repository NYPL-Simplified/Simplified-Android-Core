package org.nypl.simplified.app.testing;

import org.nypl.simplified.app.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

public final class DrawTest extends Activity
{
  @Override protected void onCreate(
    final @Nullable Bundle state)
  {
    super.onCreate(state);

    this.setContentView(R.layout.test_cover_gen);

    final ViewGroup container =
      NullCheck.notNull((ViewGroup) this
        .findViewById(R.id.test_cover_container));

    final Bitmap image = Bitmap.createBitmap(300, 450, Config.RGB_565);
    final Canvas canvas = new Canvas(image);

    {
      final Paint p = new Paint();
      p.setColor(Color.RED);
      final float left = 0;
      final float top = 0;
      final float right = 300;
      final float bottom = 451;
      final RectF oval = new RectF(left, top, right, bottom);
      canvas.drawOval(oval, p);
    }

    {
      final Paint p = new Paint();
      p.setColor(Color.RED);
      final float left = 0;
      final float top = 0;
      final float right = 10;
      final float bottom = 10;
      final RectF oval = new RectF(left, top, right, bottom);
      canvas.drawOval(oval, p);
    }

    final ImageView v = new ImageView(this);
    v.setLayoutParams(new FrameLayout.LayoutParams(image.getWidth(), image
      .getHeight()));
    v.setImageBitmap(image);
    container.addView(v);
  }
}
