package org.nypl.simplified.tenprint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region.Op;

import com.io7m.jnull.NullCheck;

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
    C64_CHARACTER_SET = TenPrintGenerator.getC64Characters();
    C64_CHARACTER_LIST =
      new ArrayList<Character>(TenPrintGenerator.C64_CHARACTER_SET);
  }

  private static int clampRangeI(
    final int v,
    final int x0,
    final int x1)
  {
    return Math.min(Math.max(v, x0), x1);
  }

  private static Set<Character> getC64Characters()
  {
    final String base =
      " qQwWeErRtTyYuUiIoOpPaAsSdDfFgGhHjJkKlL:zZxXcCvVbBnNmM1234567890.";
    final Set<Character> chars = new HashSet<Character>();
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

    final StringBuilder sb = new StringBuilder();
    for (int index = 0; index < s.length(); ++index) {
      final char c = s.charAt(index);
      final Character cc = Character.valueOf(c);
      if (c64_set.contains(cc)) {
        sb.append(c);
      } else {
        sb.append(c64_seq.get(c % c64_seq.size()));
      }
    }
    return NullCheck.notNull(sb.toString());
  }

  private static int getColorBase(
    final TenPrintInput i,
    final int text_length)
  {
    final float base_hue =
      (float) TenPrintGenerator.mapRangeD(text_length, 2, 80, 0.0, 360.0);
    final float base_saturation = i.getBaseSaturation();
    final float base_brightness = i.getBaseBrightness();
    final float[] base_hsv =
      new float[] { base_hue, base_saturation, base_brightness };
    return Color.HSVToColor(base_hsv);
  }

  private static int getColorShape(
    final TenPrintInput i,
    final int text_length)
  {
    final double base =
      TenPrintGenerator.mapRangeD(text_length, 2, 80, 0.0, 360.0);
    final float shape_hue = (float) ((base + i.getColorDistance()) % 360.0f);

    final float shape_saturation = i.getBaseSaturation();
    final float shape_brightness = i.getBaseBrightness();
    final float[] shape_hsv =
      new float[] { shape_hue, shape_saturation, shape_brightness };
    return Color.HSVToColor(shape_hsv);
  }

  private static int getGridCount(
    final TenPrintInput i)
  {
    final String title = i.getTitle();
    final int scaled = (int) (title.length() * i.getGridScale());

    final int length =
      TenPrintGenerator.clampRangeI(
        scaled,
        TenPrintGenerator.TITLE_LENGTH_MIN,
        TenPrintGenerator.TITLE_LENGTH_MAX);

    final double r =
      TenPrintGenerator.mapRangeD(
        length,
        TenPrintGenerator.TITLE_LENGTH_MIN,
        TenPrintGenerator.TITLE_LENGTH_MAX,
        2,
        11);

    return (int) r;
  }

  private static int getTextLength(
    final TenPrintInput i)
  {
    final String title = i.getTitle();
    final String author = i.getAuthor();
    final int text_length = title.length() + author.length();
    return text_length;
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
    final float left = x;
    final float top = y;
    final float right = x + w;
    final float bottom = y + h;
    final RectF oval = new RectF(left, top, right, bottom);
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
    final float left = x - (w / 2);
    final float top = y - (h / 2);
    final float right = x + (w / 2);
    final float bottom = y + (h / 2);
    final RectF oval = new RectF(left, top, right, bottom);
    canvas.drawOval(oval, p);
  }

  private static void renderGridCharacter(
    final TenPrintInput i,
    final Canvas canvas,
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

    canvas.clipRect(x, y_max, x_max, y, Op.REPLACE);

    switch (c) {
      case 'q':
      case 'Q':
      {
        TenPrintGenerator.renderEllipse(
          canvas,
          x,
          y,
          grid_size,
          grid_size,
          paint_shape);
        break;
      }
      case 'W':
      case 'w':
      {
        TenPrintGenerator.renderEllipse(
          canvas,
          x,
          y,
          grid_size,
          grid_size,
          paint_shape);

        final int size_smaller = grid_size - thick2;
        TenPrintGenerator.renderEllipse(
          canvas,
          x + thick,
          y + thick,
          size_smaller,
          size_smaller,
          paint_base);
        break;
      }
      case 'E':
      case 'e':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y + thick,
          grid_size,
          thick,
          paint_shape);
        break;
      }
      case 'R':
      case 'r':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y + (grid_size - thick2),
          grid_size,
          thick,
          paint_shape);
        break;
      }
      case 'T':
      case 't':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x + thick,
          y,
          thick,
          grid_size,
          paint_shape);
        break;
      }
      case 'Y':
      case 'y':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x + (grid_size - thick2),
          y,
          thick,
          grid_size,
          paint_shape);
        break;
      }
      case 'U':
      case 'u':
      {
        TenPrintGenerator.renderEllipse(
          canvas,
          x,
          y,
          grid_size * 2,
          grid_size * 2,
          paint_shape);

        TenPrintGenerator.renderEllipse(
          canvas,
          x + thick,
          y + thick,
          grid_size * 2,
          grid_size * 2,
          paint_base);
        break;
      }
      case 'I':
      case 'i':
      {
        TenPrintGenerator.renderEllipse(
          canvas,
          x - grid_size,
          y,
          grid_size * 2,
          grid_size * 2,
          paint_shape);
        TenPrintGenerator.renderEllipse(canvas, (x - grid_size) - thick, y
          + thick, grid_size * 2, grid_size * 2, paint_base);
        break;
      }
      case 'O':
      case 'o':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          grid_size,
          grid_size,
          paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas,
          x + thick,
          y + thick,
          grid_size,
          grid_size,
          paint_base);
        break;
      }
      case 'P':
      case 'p':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          grid_size,
          grid_size,
          paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas,
          x - thick,
          y + thick,
          grid_size,
          grid_size,
          paint_base);
        break;
      }
      case 'A':
      case 'a':
      {
        final Path p = new Path();
        p.moveTo(x, y_max);
        p.lineTo(x_center, y);
        p.lineTo(x_max, y_max);
        p.lineTo(x, y_max);
        p.close();
        canvas.drawPath(p, paint_shape);
        break;
      }
      case 'S':
      case 's':
      {
        final Path p = new Path();
        p.moveTo(x, y);
        p.lineTo(x_center, y_max);
        p.lineTo(x_max, y);
        p.lineTo(x, y);
        p.close();
        canvas.drawPath(p, paint_shape);
        break;
      }
      case 'D':
      case 'd':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y + thick2,
          grid_size,
          thick,
          paint_shape);
        break;
      }
      case 'F':
      case 'f':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y + thick3,
          grid_size,
          thick,
          paint_shape);
        break;
      }
      case 'G':
      case 'g':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x + thick2,
          y,
          thick,
          grid_size,
          paint_shape);
        break;
      }
      case 'H':
      case 'h':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x + (grid_size - thick3),
          y,
          thick,
          grid_size,
          paint_shape);
        break;
      }
      case 'J':
      case 'j':
      {
        TenPrintGenerator.renderEllipse(
          canvas,
          x,
          y - grid_size,
          grid_size * 2,
          grid_size * 2,
          paint_shape);
        TenPrintGenerator.renderEllipse(canvas, x + thick, y
          - (grid_size + thick), grid_size * 2, grid_size * 2, paint_base);
        break;
      }
      case 'K':
      case 'k':
      {
        TenPrintGenerator.renderEllipse(
          canvas,
          x - grid_size,
          y - grid_size,
          grid_size * 2,
          grid_size * 2,
          paint_shape);
        TenPrintGenerator.renderEllipse(canvas, x - (grid_size + thick), y
          - (grid_size + thick), grid_size * 2, grid_size * 2, paint_base);
        break;
      }
      case 'L':
      case 'l':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          grid_size,
          grid_size,
          paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas,
          x + thick,
          y - thick,
          grid_size,
          grid_size,
          paint_base);
        break;
      }
      case ':':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          grid_size,
          grid_size,
          paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas,
          x - thick,
          y - thick,
          grid_size,
          grid_size,
          paint_base);
        break;
      }

      case 'Z':
      case 'z':
      {
        {
          final Path p = new Path();
          p.moveTo(x, y_center);
          p.lineTo(x_center, y);
          p.lineTo(x_max, y_center);
          p.lineTo(x, y_center);
          p.close();
          canvas.drawPath(p, paint_shape);
        }

        {
          final Path p = new Path();
          p.moveTo(x, y_center);
          p.lineTo(x_center, y_max);
          p.lineTo(x_max, y_center);
          p.lineTo(x, y_center);
          p.close();
          canvas.drawPath(p, paint_shape);
        }
        break;
      }

      case 'X':
      case 'x':
      {
        final int gs_3 = grid_size / 3;
        TenPrintGenerator.renderEllipseCenter(
          canvas,
          x_center,
          y + gs_3,
          thick2,
          thick2,
          paint_shape);
        TenPrintGenerator.renderEllipseCenter(
          canvas,
          x + gs_3,
          y_max - gs_3,
          thick2,
          thick2,
          paint_shape);
        TenPrintGenerator.renderEllipseCenter(canvas, x_max - gs_3, y_max
          - gs_3, thick2, thick2, paint_shape);
        break;
      }

      case 'C':
      case 'c':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y + thick3,
          grid_size,
          thick,
          paint_shape);
        break;
      }

      case 'V':
      case 'v':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          grid_size,
          grid_size,
          paint_shape);

        {
          final Path p = new Path();
          p.moveTo(x, y + thick);
          p.lineTo(x_center - thick, y_center);
          p.lineTo(x, y_max - thick);
          p.lineTo(x, y + thick);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo(x_max, y + thick);
          p.lineTo(x_center + thick, y_center);
          p.lineTo(x_max, y_max - thick);
          p.lineTo(x_max, y + thick);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo(x_max, y + thick);
          p.lineTo(x_center + thick, y_center);
          p.lineTo(x_max, y_max - thick);
          p.lineTo(x_max, y + thick);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo(x + thick, y_max);
          p.lineTo(x_center, y_center + thick);
          p.lineTo(x_max - thick, y_max);
          p.lineTo(x + +thick, y_max);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo(x + thick, y);
          p.lineTo(x_center, y_center - thick);
          p.lineTo(x_max - thick, y);
          p.lineTo(x + thick, y);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        break;
      }

      case 'B':
      case 'b':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x + thick3,
          y,
          thick,
          grid_size,
          paint_shape);
        break;
      }

      case 'N':
      case 'n':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          grid_size,
          grid_size,
          paint_shape);

        {
          final Path p = new Path();
          p.moveTo(x, y);
          p.lineTo(x_max - thick, y);
          p.lineTo(x, y_max - thick);
          p.lineTo(x, y);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo(x + thick, y_max);
          p.lineTo(x_max + thick, y_max);
          p.lineTo(x_max + thick, y);
          p.lineTo(x + thick, y_max);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        break;
      }

      case 'M':
      case 'm':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          grid_size,
          grid_size,
          paint_shape);

        {
          final Path p = new Path();
          p.moveTo(x, y + thick);
          p.lineTo(x, y_max + thick);
          p.lineTo(x_max, y_max + thick);
          p.lineTo(x, y + thick);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        {
          final Path p = new Path();
          p.moveTo(x, y - thick);
          p.lineTo(x_max, y_max - thick);
          p.lineTo(x_max, y - thick);
          p.lineTo(x, y - thick);
          p.close();
          canvas.drawPath(p, paint_base);
        }

        break;
      }

      case '0':
      {
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

      case '1':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y_center - (thick / 2),
          grid_size,
          thick,
          paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas,
          x_center - (thick / 2),
          y,
          thick,
          (grid_size / 2) + (thick / 2),
          paint_shape);

        break;
      }

      case '2':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y_center - (thick / 2),
          grid_size,
          thick,
          paint_shape);

        TenPrintGenerator.renderRectangle(
          canvas,
          x_center - (thick / 2),
          y_center - (thick / 2),
          thick,
          (grid_size / 2) + thick,
          paint_shape);

        break;
      }

      case '3':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y_center - (thick / 2),
          grid_size / 2,
          thick,
          paint_shape);

        TenPrintGenerator.renderRectangle(
          canvas,
          x_center - (thick / 2),
          y,
          thick,
          grid_size,
          paint_shape);

        break;
      }

      case '4':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          thick2,
          grid_size,
          paint_shape);
        break;
      }

      case '5':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          thick3,
          grid_size,
          paint_shape);
        break;
      }

      case '6':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x_max - thick3,
          y,
          thick3,
          grid_size,
          paint_shape);
        break;
      }

      case '7':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          grid_size,
          thick2,
          paint_shape);
        break;
      }

      case '8':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          grid_size,
          thick3,
          paint_shape);
        break;
      }

      case '9':
      {
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y,
          thick,
          grid_size,
          paint_shape);
        TenPrintGenerator.renderRectangle(
          canvas,
          x,
          y_max - thick3,
          grid_size,
          thick3,
          paint_shape);
        break;
      }

      case '.':
      {
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

    {
      final Paint pt = new Paint();
      pt.setColor(Color.BLACK);
      pt.setTextSize(16.0f);
      canvas.drawText("" + c, x + 10, y_max - 16, pt);

      pt.setStyle(Style.STROKE);
      canvas.drawRect(x, y_max, x_max, y, pt);
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
    final float left = x;
    final float top = y;
    final float right = x + w;
    final float bottom = y + h;
    final RectF r = new RectF(left, top, right, bottom);
    canvas.drawRect(r, p);
  }

  private TenPrintGenerator()
  {
    // Nothing
  }

  @Override public Bitmap generate(
    final TenPrintInput i)
  {
    NullCheck.notNull(i);

    final int cw = i.getCoverWidth();
    final int ch = i.getCoverHeight();
    final Bitmap b =
      NullCheck.notNull(Bitmap.createBitmap(cw, ch, Config.RGB_565));

    final int margin = i.getMargin();
    final int start_y = ch - cw;
    final int title_font_size = (int) (cw * 0.08);
    final int author_font_size = (int) (ch * 0.07);
    final int offset = (ch * margin) / 100;
    final int title_height = (int) ((start_y - offset) * 0.75);
    final int author_height = (int) ((start_y - offset) * 0.25);

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
    canvas.drawRect(0, 0, cw, ch, paint_base);

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
          i,
          canvas,
          paint_base,
          paint_shape,
          c,
          x_offset,
          y_offset,
          grid_size);
        grid_cell = grid_cell + 1;
      }
    }

    return b;
  }
}
