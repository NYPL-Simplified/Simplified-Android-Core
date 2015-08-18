package org.nypl.simplified.tenprint;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.io7m.jnull.NullCheck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The default implementation of the {@link TenPrintGeneratorType} interface.
 */

public final class TenPrintGenerator implements TenPrintGeneratorType
{
  private static final List<Character> C64_CHARACTER_LIST;
  private static final Set<Character>  C64_CHARACTER_SET;
  private static final int             TITLE_LENGTH_MAX;
  private static final int             TITLE_LENGTH_MIN;

  static {
    TITLE_LENGTH_MIN = 2;
    TITLE_LENGTH_MAX = 60;
    C64_CHARACTER_LIST = TenPrintGenerator.getC64Characters();
    C64_CHARACTER_SET =
      new HashSet<Character>(TenPrintGenerator.C64_CHARACTER_LIST);
  }

  private TenPrintGenerator()
  {
    // Nothing
  }

  private static int clampRangeI(
    final int v,
    final int x0,
    final int x1)
  {
    return Math.min(Math.max(v, x0), x1);
  }

  private static List<Character> getC64Characters()
  {
    final String base =
      " qQwWeErRtTyYuUiIoOpPaAsSdDfFgGhHjJkKlL:zZxXcCvVbBnNmM1234567890.";
    final List<Character> chars = new ArrayList<Character>(base.length());
    for (int index = 0; index < base.length(); ++index) {
      final Character c = Character.valueOf(base.charAt(index));
      chars.add(c);
    }
    return chars;
  }

  private static String getC64String(
    final String s)
  {
    final Set<Character> c64_set = TenPrintGenerator.C64_CHARACTER_SET;
    final List<Character> c64_seq = TenPrintGenerator.C64_CHARACTER_LIST;

    final StringBuilder sb = new StringBuilder(s.length());
    for (int index = 0; index < s.length(); ++index) {
      final char c = s.charAt(index);
      final Character cc = Character.valueOf(c);
      if (c64_set.contains(cc)) {
        sb.append(c);
      } else {
        sb.append(c64_seq.get((int) c % c64_seq.size()));
      }
    }
    return NullCheck.notNull(sb.toString());
  }

  private static int getColorBase(
    final TenPrintInput i,
    final int text_length)
  {
    final float base_hue =
      (float) TenPrintGenerator.mapRangeD(
        (double) text_length,
        2.0,
        80.0, 0.0, 360.0);
    final float base_saturation = i.getBaseSaturation();
    final float base_brightness = i.getBaseBrightness();
    final float[] base_hsv = {
      base_hue, base_saturation, base_brightness, };
    return Color.HSVToColor(base_hsv);
  }

  private static int getColorShape(
    final TenPrintInput i,
    final int text_length)
  {
    final double base =
      TenPrintGenerator.mapRangeD((double) text_length, 2.0, 80.0, 0.0, 360.0);
    final float shape_hue = (float) ((base + (double) i.getColorDistance()) % 360.0);

    final float shape_saturation = i.getBaseSaturation();
    final float shape_brightness = i.getBaseBrightness();
    final float[] shape_hsv = {
      shape_hue, shape_saturation, shape_brightness, };
    return Color.HSVToColor(shape_hsv);
  }

  private static int getGridCount(
    final TenPrintInput i)
  {
    final String title = i.getTitle();
    final int scaled = (int) ((float) title.length() * i.getGridScale());

    final int length = TenPrintGenerator.clampRangeI(
      scaled,
      TenPrintGenerator.TITLE_LENGTH_MIN,
      TenPrintGenerator.TITLE_LENGTH_MAX);

    final double r = TenPrintGenerator.mapRangeD(
      (double) length,
      (double) TenPrintGenerator.TITLE_LENGTH_MIN,
      (double) TenPrintGenerator.TITLE_LENGTH_MAX,
      2.0,
      11.0);

    return (int) r;
  }

  private static int getTextLength(
    final TenPrintInput i)
  {
    final String title = i.getTitle();
    final String author = i.getAuthor();
    return title.length() + author.length();
  }

  private static double mapRangeD(
    final double v,
    final double x0,
    final double x1,
    final double y0,
    final double y1)
  {
    return y0 + ((y1 - y0) * ((v - x0) / (x1 - x0)));
  }

  /**
   * @return A new cover generator
   */

  public static TenPrintGeneratorType newGenerator()
  {
    return new TenPrintGenerator();
  }

  private static void renderEllipse(
    final Canvas canvas,
    final int x,
    final int y,
    final int w,
    final int h,
    final Paint p)
  {
    final float right = (float) (x + w);
    final float bottom = (float) (y + h);
    final RectF oval = new RectF((float) x, (float) y, right, bottom);
    canvas.drawOval(oval, p);
  }

  private static void renderEllipseCenter(
    final Canvas canvas,
    final int x,
    final int y,
    final int w,
    final int h,
    final Paint p)
  {
    final float left = (float) (x - (w / 2));
    final float top = (float) (y - (h / 2));
    final float right = (float) (x + (w / 2));
    final float bottom = (float) (y + (h / 2));
    final RectF oval = new RectF(left, top, right, bottom);
    canvas.drawOval(oval, p);
  }

  private static void renderGridCharacter(
    final Canvas canvas,
    final TenPrintInput i,
    final Paint paint_base,
    final Paint paint_shape,
    final char c,
    final int x,
    final int y,
    final int grid_size)
  {
    final int thick = (grid_size * i.getShapeThickness()) / 100;
    final int thick2 = thick * 2;
    final int thick3 = thick * 3;
    final int x_max = x + grid_size;
    final int y_max = y + grid_size;
    final int x_center = x + (grid_size / 2);
    final int y_center = y + (grid_size / 2);

    canvas.clipRect(
      (float) x,
      (float) y_max,
      (float) x_max,
      (float) y, Op.REPLACE);

    final int grid_size_double = grid_size * 2;
    switch (c) {
      case 'q':
      case 'Q': {
        TenPrintGenerator.renderEllipse(
          canvas, x, y, grid_size, grid_size, paint_shape);
        break;
      }
      case 'W':
      case 'w': {
        TenPrintGenerator.renderEllipse(
          canvas, x, y, grid_size, grid_size, paint_shape);

        final int size_smaller = grid_size - thick2;
        TenPrintGenerator.renderEllipse(
          canvas, x + thick, y + thick, size_smaller, size_smaller, paint_base);
        break;
      }
      case 'E':
      case 'e': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y + thick, grid_size, thick, paint_shape);
        break;
      }
      case 'R':
      case 'r': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y + (grid_size - thick2), grid_size, thick, paint_shape);
        break;
      }
      case 'T':
      case 't': {
        TenPrintGenerator.renderRectangle(
          canvas, x + thick, y, thick, grid_size, paint_shape);
        break;
      }
      case 'Y':
      case 'y': {
        TenPrintGenerator.renderRectangle(
          canvas, x + (grid_size - thick2), y, thick, grid_size, paint_shape);
        break;
      }
      case 'U':
      case 'u': {
        TenPrintGenerator.renderRing(
          canvas,
          x,
          y,
          grid_size_double,
          grid_size_double,
          thick,
          paint_shape,
          paint_base);
        break;
      }
      case 'I':
      case 'i': {
        TenPrintGenerator.renderRing(
          canvas,
          x - grid_size,
          y,
          grid_size_double,
          grid_size_double,
          thick,
          paint_shape,
          paint_base);
        break;
      }
      case 'O':
      case 'o': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, grid_size, grid_size, paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas, x + thick, y + thick, grid_size, grid_size, paint_base);
        break;
      }
      case 'P':
      case 'p': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, grid_size, grid_size, paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas, x - thick, y + thick, grid_size, grid_size, paint_base);
        break;
      }
      case 'A':
      case 'a': {
        final Path p = new Path();
        p.moveTo((float) x, (float) y_max);
        p.lineTo((float) x_center, (float) y);
        p.lineTo((float) x_max, (float) y_max);
        p.lineTo((float) x, (float) y_max);
        p.close();
        canvas.drawPath(p, paint_shape);
        break;
      }
      case 'S':
      case 's': {
        final Path p = new Path();
        p.moveTo((float) x, (float) y);
        p.lineTo((float) x_center, (float) y_max);
        p.lineTo((float) x_max, (float) y);
        p.lineTo((float) x, (float) y);
        p.close();
        canvas.drawPath(p, paint_shape);
        break;
      }
      case 'D':
      case 'd': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y + thick2, grid_size, thick, paint_shape);
        break;
      }
      case 'F':
      case 'f': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y + thick3, grid_size, thick, paint_shape);
        break;
      }
      case 'G':
      case 'g': {
        TenPrintGenerator.renderRectangle(
          canvas, x + thick2, y, thick, grid_size, paint_shape);
        break;
      }
      case 'H':
      case 'h': {
        TenPrintGenerator.renderRectangle(
          canvas, x + (grid_size - thick3), y, thick, grid_size, paint_shape);
        break;
      }
      case 'J':
      case 'j': {
        TenPrintGenerator.renderRing(
          canvas,
          x,
          y - grid_size,
          grid_size_double,
          grid_size_double,
          thick,
          paint_shape,
          paint_base);
        break;
      }
      case 'K':
      case 'k': {
        TenPrintGenerator.renderRing(
          canvas,
          x - grid_size,
          y - grid_size,
          grid_size_double,
          grid_size_double,
          thick,
          paint_shape,
          paint_base);
        break;
      }
      case 'L':
      case 'l': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, grid_size, grid_size, paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas, x + thick, y - thick, grid_size, grid_size, paint_base);
        break;
      }
      case ':': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, grid_size, grid_size, paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas, x - thick, y - thick, grid_size, grid_size, paint_base);
        break;
      }

      case 'Z':
      case 'z': {
        {
          final Path p = new Path();
          p.moveTo((float) x, (float) y_center);
          p.lineTo((float) x_center, (float) y);
          p.lineTo((float) x_max, (float) y_center);
          p.lineTo((float) x, (float) y_center);
          p.close();
          canvas.drawPath(p, paint_shape);
        }

        {
          final Path p = new Path();
          p.moveTo((float) x, (float) y_center);
          p.lineTo((float) x_center, (float) y_max);
          p.lineTo((float) x_max, (float) y_center);
          p.lineTo((float) x, (float) y_center);
          p.close();
          canvas.drawPath(p, paint_shape);
        }
        break;
      }

      case 'X':
      case 'x': {
        final int gs_3 = grid_size / 3;
        TenPrintGenerator.renderEllipseCenter(
          canvas, x_center, y + gs_3, thick2, thick2, paint_shape);
        TenPrintGenerator.renderEllipseCenter(
          canvas, x + gs_3, y_max - gs_3, thick2, thick2, paint_shape);
        TenPrintGenerator.renderEllipseCenter(
          canvas, x_max - gs_3, y_max - gs_3, thick2, thick2, paint_shape);
        break;
      }

      case 'C':
      case 'c': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y + thick3, grid_size, thick, paint_shape);
        break;
      }

      case 'V':
      case 'v': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, grid_size, grid_size, paint_shape);

        {
          final Path p = new Path();
          p.moveTo((float) x, (float) (y + thick));
          p.lineTo((float) (x_center - thick), (float) y_center);
          p.lineTo((float) x, (float) (y_max - thick));
          p.lineTo((float) x, (float) (y + thick));
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo((float) x_max, (float) (y + thick));
          p.lineTo((float) (x_center + thick), (float) y_center);
          p.lineTo((float) x_max, (float) (y_max - thick));
          p.lineTo((float) x_max, (float) (y + thick));
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo((float) x_max, (float) (y + thick));
          p.lineTo((float) (x_center + thick), (float) y_center);
          p.lineTo((float) x_max, (float) (y_max - thick));
          p.lineTo((float) x_max, (float) (y + thick));
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo((float) (x + thick), (float) y_max);
          p.lineTo((float) x_center, (float) (y_center + thick));
          p.lineTo((float) (x_max - thick), (float) y_max);
          p.lineTo((float) (x + thick), (float) y_max);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo((float) (x + thick), (float) y);
          p.lineTo((float) x_center, (float) (y_center - thick));
          p.lineTo((float) (x_max - thick), (float) y);
          p.lineTo((float) (x + thick), (float) y);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        break;
      }

      case 'B':
      case 'b': {
        TenPrintGenerator.renderRectangle(
          canvas, x + thick3, y, thick, grid_size, paint_shape);
        break;
      }

      case 'N':
      case 'n': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, grid_size, grid_size, paint_shape);

        {
          final Path p = new Path();
          p.moveTo((float) x, (float) y);
          p.lineTo((float) (x_max - thick), (float) y);
          p.lineTo((float) x, (float) (y_max - thick));
          p.lineTo((float) x, (float) y);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo((float) (x + thick), (float) y_max);
          p.lineTo((float) (x_max + thick), (float) y_max);
          p.lineTo((float) (x_max + thick), (float) y);
          p.lineTo((float) (x + thick), (float) y_max);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        break;
      }

      case 'M':
      case 'm': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, grid_size, grid_size, paint_shape);

        {
          final Path p = new Path();
          p.moveTo((float) x, (float) (y + thick));
          p.lineTo((float) x, (float) (y_max + thick));
          p.lineTo((float) x_max, (float) (y_max + thick));
          p.lineTo((float) x, (float) (y + thick));
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo((float) x, (float) (y - thick));
          p.lineTo((float) x_max, (float) (y_max - thick));
          p.lineTo((float) x_max, (float) (y - thick));
          p.lineTo((float) x, (float) (y - thick));
          p.close();
          canvas.drawPath(p, paint_base);
        }

        break;
      }

      case '0': {
        TenPrintGenerator.renderRectangle(
          canvas,
          x_center - (thick / 2),
          y_center - (thick / 2),
          thick,
          (grid_size / 2) + (thick / 2),
          paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas,
          x_center - (thick / 2),
          y_center - (thick / 2),
          (grid_size / 2) + thick,
          thick,
          paint_shape);

        break;
      }

      case '1': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y_center - (thick / 2), grid_size, thick, paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas,
          x_center - (thick / 2),
          y,
          thick,
          (grid_size / 2) + (thick / 2),
          paint_shape);

        break;
      }

      case '2': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y_center - (thick / 2), grid_size, thick, paint_shape);

        TenPrintGenerator.renderRectangle(
          canvas,
          x_center - (thick / 2),
          y_center - (thick / 2),
          thick,
          (grid_size / 2) + thick,
          paint_shape);

        break;
      }

      case '3': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y_center - (thick / 2), grid_size / 2, thick, paint_shape);

        TenPrintGenerator.renderRectangle(
          canvas, x_center - (thick / 2), y, thick, grid_size, paint_shape);

        break;
      }

      case '4': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, thick2, grid_size, paint_shape);
        break;
      }

      case '5': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, thick3, grid_size, paint_shape);
        break;
      }

      case '6': {
        TenPrintGenerator.renderRectangle(
          canvas, x_max - thick3, y, thick3, grid_size, paint_shape);
        break;
      }

      case '7': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, grid_size, thick2, paint_shape);
        break;
      }

      case '8': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, grid_size, thick3, paint_shape);
        break;
      }

      case '9': {
        TenPrintGenerator.renderRectangle(
          canvas, x, y, thick, grid_size, paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas, x, y_max - thick3, grid_size, thick3, paint_shape);
        break;
      }

      case '.': {
        TenPrintGenerator.renderRectangle(
          canvas,
          x_center - (thick / 2),
          y_center - (thick / 2),
          thick,
          (grid_size / 2) + (thick / 2),
          paint_shape);

        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y_center - (thick / 2),
          (grid_size / 2) + (thick / 2),
          thick,
          paint_shape);
        break;
      }
    }

    if (i.debugArtworkEnabled()) {
      final Paint pt = new Paint();
      pt.setColor(Color.BLACK);
      pt.setTextSize(16.0f);
      canvas.drawText(Character.toString(c),
                      (float) (x + 10),
                      (float) (y_max - 16), pt);

      pt.setStyle(Style.STROKE);
      canvas.drawRect((float) x, (float) y_max, (float) x_max, (float) y, pt);
    }
  }

  private static String ellipsize(
    final String t,
    final int at)
  {
    if (t.length() > at) {
      return NullCheck.notNull(t.substring(0, at - 1) + "…");
    }
    return t;
  }

  private static void renderLabel(
    final Canvas canvas,
    final TenPrintInput i,
    final int cw,
    final int ch,
    final int start_y)
  {
    final int margin = (i.getCoverHeight() * i.getMargin()) / 100;
    final int margin_half = margin / 2;

    /**
     * Render the white book label.
     */

    {
      final Paint paint_label = new Paint();
      paint_label.setColor(Color.WHITE);
      paint_label.setAntiAlias(true);
      paint_label.setFilterBitmap(true);
      canvas.clipRect(
        (float) margin_half,
        (float) margin_half,
        (float) (cw - margin_half),
        (float) start_y, Op.REPLACE);
      canvas.drawRect(0.0F, 0.0F, (float) cw, (float) ch, paint_label);
    }

    /**
     * Render the title and author strings.
     */

    final float title_size = (float) i.getCoverWidth() * 0.08f;
    final TextPaint title_paint = new TextPaint();
    title_paint.setColor(Color.BLACK);
    title_paint.setTextSize(title_size);
    title_paint.setTextAlign(Align.LEFT);
    title_paint.setTypeface(
      Typeface.create(
        Typeface.SANS_SERIF, Typeface.BOLD));
    title_paint.setAntiAlias(true);

    final float author_size = (float) i.getCoverWidth() * 0.07f;
    final TextPaint author_paint = new TextPaint();
    author_paint.setColor(Color.BLACK);
    author_paint.setTextSize(author_size);
    author_paint.setTextAlign(Align.LEFT);
    author_paint.setTypeface(
      Typeface.create(
        Typeface.SANS_SERIF, Typeface.NORMAL));
    author_paint.setAntiAlias(true);

    final int text_width = canvas.getWidth() - (margin * 2);
    final StaticLayout title_layout = new StaticLayout(
      TenPrintGenerator.ellipsize(i.getTitle(), 30),
      title_paint,
      text_width,
      Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F,
      false);

    final StaticLayout author_layout = new StaticLayout(
      TenPrintGenerator.ellipsize(i.getAuthor(), 30),
      author_paint,
      text_width,
      Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F,
      false);

    try {
      canvas.save();
      canvas.translate((float) margin * 1.25f, (float) margin);
      title_layout.draw(canvas);
    } finally {
      canvas.restore();
    }

    try {
      canvas.save();
      final int ty = start_y - (author_layout.getHeight() + margin_half);
      canvas.translate((float) margin * 1.25f, (float) ty);
      author_layout.draw(canvas);
    } finally {
      canvas.restore();
    }
  }

  private static void renderRectangle(
    final Canvas canvas,
    final int x,
    final int y,
    final int w,
    final int h,
    final Paint p)
  {
    final float right = (float) (x + w);
    final float bottom = (float) (y + h);
    final RectF r = new RectF((float) x, (float) y, right, bottom);
    canvas.drawRect(r, p);
  }

  private static void renderRing(
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
      final float right = (float) (x + w);
      final float bottom = (float) (y + h);
      final RectF oval = new RectF((float) x, (float) y, right, bottom);
      c.drawOval(oval, p);
    }

    {
      final float left = (float) (x + thick);
      final float top = (float) (y + thick);
      final float right = (float) (x + (w - thick));
      final float bottom = (float) (y + (h - thick));
      final RectF oval = new RectF(left, top, right, bottom);
      c.drawOval(oval, q);
    }
  }

  @Override public Bitmap generate(
    final TenPrintInput i)
  {
    NullCheck.notNull(i);

    final int cw = i.getCoverWidth();
    final int ch = i.getCoverHeight();
    final Bitmap b =
      NullCheck.notNull(Bitmap.createBitmap(cw, ch, Config.RGB_565));

    final int start_y = ch - cw;
    final int text_length = TenPrintGenerator.getTextLength(i);
    final int color_base = TenPrintGenerator.getColorBase(i, text_length);
    final int color_shape = TenPrintGenerator.getColorShape(i, text_length);

    final Paint paint_base = new Paint();
    paint_base.setColor(color_base);
    paint_base.setAntiAlias(true);
    paint_base.setFilterBitmap(true);

    final Paint paint_shape = new Paint();
    paint_shape.setColor(color_shape);
    paint_shape.setAntiAlias(true);
    paint_shape.setFilterBitmap(true);

    final Canvas canvas = new Canvas(b);
    canvas.drawRect(0.0F, 0.0F, (float) cw, (float) ch, paint_base);

    final String c64_text = TenPrintGenerator.getC64String(i.getTitle());
    final int grid_count = TenPrintGenerator.getGridCount(i);
    final int grid_size = cw / grid_count;
    int grid_cell = 0;
    for (int y = 0; y < grid_count; ++y) {
      for (int x = 0; x < grid_count; ++x) {
        final char c = c64_text.charAt(grid_cell % c64_text.length());
        final int x_offset = x * grid_size;
        final int y_offset = start_y + (y * grid_size);
        TenPrintGenerator.renderGridCharacter(
          canvas, i, paint_base, paint_shape, c, x_offset, y_offset, grid_size);
        grid_cell = grid_cell + 1;
      }
    }

    TenPrintGenerator.renderLabel(canvas, i, cw, ch, start_y);
    return b;
  }
}
