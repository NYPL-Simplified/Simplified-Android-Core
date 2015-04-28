package org.nypl.simplified.app.reader;

import android.graphics.ColorMatrix;

public final class ReaderColorMatrix extends ColorMatrix
{
  private ReaderColorMatrix(
    final float[] actual)
  {
    super(actual);
  }

  public static ReaderColorMatrix fromRows(
    final float row_0[],
    final float row_1[],
    final float row_2[],
    final float row_3[])
  {
    assert row_0.length == 5;
    assert row_1.length == 5;
    assert row_2.length == 5;
    assert row_3.length == 5;

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
    assert actual_index == (4 * 5);
    return new ReaderColorMatrix(actual);
  }
}
