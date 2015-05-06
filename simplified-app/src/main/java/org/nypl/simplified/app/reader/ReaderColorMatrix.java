package org.nypl.simplified.app.reader;

import org.nypl.simplified.assertions.Assertions;

import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

/**
 * Functions for producing specific color filter matrices.
 */

public final class ReaderColorMatrix extends ColorMatrix
{
  public static ReaderColorMatrix fromRows(
    final float row_0[],
    final float row_1[],
    final float row_2[],
    final float row_3[])
  {
    Assertions.checkPrecondition(row_0.length == 5, "Row 0 has 5 elements");
    Assertions.checkPrecondition(row_1.length == 5, "Row 1 has 5 elements");
    Assertions.checkPrecondition(row_2.length == 5, "Row 2 has 5 elements");
    Assertions.checkPrecondition(row_3.length == 5, "Row 3 has 5 elements");

    final float[] actual = new float[4 * 5];
    int actual_index = 0;
    for (int index = 0; index < 5; ++index) {
      actual[actual_index] = row_0[index];
      actual_index = actual_index + 1;
    }
    for (int index = 0; index < 5; ++index) {
      actual[actual_index] = row_1[index];
      actual_index = actual_index + 1;
    }
    for (int index = 0; index < 5; ++index) {
      actual[actual_index] = row_2[index];
      actual_index = actual_index + 1;
    }
    for (int index = 0; index < 5; ++index) {
      actual[actual_index] = row_3[index];
      actual_index = actual_index + 1;
    }

    Assertions.checkInvariant(
      actual_index == (4 * 5),
      "%d == %d",
      actual_index,
      4 * 5);
    return new ReaderColorMatrix(actual);
  }

  /**
   * Construct a color matrix that inverts a given bitmap and then multiplies
   * the resulting colors by the current foreground color.
   *
   * @param c
   *          The base color
   */

  public static ColorMatrixColorFilter getImageFilterMatrix(
    final int c)
  {
    final ReaderColorMatrix inversion;
    final ReaderColorMatrix tint;

    {
      final float[] row_0 = { -1, 0, 0, 0, 255 };
      final float[] row_1 = { 0, -1, 0, 0, 255 };
      final float[] row_2 = { 0, 0, -1, 0, 255 };
      final float[] row_3 = { 0, 0, 0, 1, 0 };
      inversion = ReaderColorMatrix.fromRows(row_0, row_1, row_2, row_3);
    }

    {
      final float r = Color.red(c) / 256.0f;
      final float g = Color.green(c) / 256.0f;
      final float b = Color.blue(c) / 256.0f;
      final float[] row_0 = { r, 0, 0, 0, 0 };
      final float[] row_1 = { 0, g, 0, 0, 0 };
      final float[] row_2 = { 0, 0, b, 0, 0 };
      final float[] row_3 = { 0, 0, 0, 1, 0 };
      tint = ReaderColorMatrix.fromRows(row_0, row_1, row_2, row_3);
    }

    tint.preConcat(inversion);

    final ColorMatrixColorFilter filter =
      new ColorMatrixColorFilter(tint.getArray());
    return filter;
  }

  private ReaderColorMatrix(
    final float[] actual)
  {
    super(actual);
  }
}
