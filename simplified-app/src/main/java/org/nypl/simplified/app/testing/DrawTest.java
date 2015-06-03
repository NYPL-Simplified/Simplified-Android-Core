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
  private static void drawEllipse(
    final Canvas c,
    final int x,
    final int y,
    final int w,
    final int h,
    final Paint p)
  {
    final float left = x;
    final float top = y;
    final float right = x + w;
    final float bottom = y + h;
    final RectF oval = new RectF(left, top, right, bottom);
    c.drawOval(oval, p);
  }

  private static void drawRing(
    final Canvas c,
    final int x,
    final int y,
    final int w,
    final int h,
    final int thick,
    final Paint p,
    final Paint q)
  {
    {
      final float left = x;
      final float top = y;
      final float right = x + w;
      final float bottom = y + h;
      final RectF oval = new RectF(left, top, right, bottom);
      c.drawOval(oval, p);
    }

    {
      final float left = x + thick;
      final float top = y + thick;
      final float right = x + (w - thick);
      final float bottom = y + (h - thick);
      final RectF oval = new RectF(left, top, right, bottom);
      c.drawOval(oval, q);
    }
  }

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

    final Paint p = new Paint();
    p.setColor(Color.RED);
    final Paint q = new Paint();
    p.setColor(Color.BLUE);
    DrawTest.drawRing(canvas, 10, 10, 200, 200, 20, p, q);

    final ImageView v = new ImageView(this);
    v.setLayoutParams(new FrameLayout.LayoutParams(image.getWidth(), image
      .getHeight()));
    v.setImageBitmap(image);
    container.addView(v);
  }
}
