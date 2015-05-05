package org.nypl.simplified.app.tests.reader;

import java.io.IOException;
import java.io.InputStreamReader;

import junit.framework.Assert;

import org.json.JSONObject;
import org.nypl.simplified.app.reader.ReaderPaginationChangedEvent;
import org.nypl.simplified.app.reader.ReaderPaginationChangedEvent.OpenPage;

import android.test.InstrumentationTestCase;

public final class ReaderPaginationChangedEventTest extends
  InstrumentationTestCase
{
  private String slurp(
    final String file)
    throws IOException
  {
    final InputStreamReader in =
      new InputStreamReader(this.getClass().getResourceAsStream(file));
    final StringBuilder contents = new StringBuilder();
    final char[] buffer = new char[4096];
    int read = 0;
    do {
      contents.append(buffer, 0, read);
      read = in.read(buffer);
    } while (read >= 0);
    return contents.toString();
  }

  public void testOK_0()
    throws Exception
  {
    final String text = this.slurp("pagination-ok-0.json");
    final JSONObject o = new JSONObject(text);
    final ReaderPaginationChangedEvent e =
      ReaderPaginationChangedEvent.fromJSON(o);

    Assert.assertEquals(false, e.isRightToLeft());
    Assert.assertEquals(false, e.isFixedLayout());
    Assert.assertEquals(10, e.getSpineItemCount());
    Assert.assertEquals(1, e.getOpenPages().size());
    final OpenPage op = e.getOpenPages().get(0);
    Assert.assertEquals(12, op.getSpineItemPageIndex());
    Assert.assertEquals(144, op.getSpineItemPageCount());
    Assert.assertEquals("item7", op.getIDRef());
    Assert.assertEquals(2, op.getSpineItemIndex());
  }

  public void testOK_1()
    throws Exception
  {
    final String text = this.slurp("pagination-ok-1.json");
    final JSONObject o = new JSONObject(text);
    final ReaderPaginationChangedEvent e =
      ReaderPaginationChangedEvent.fromJSON(o);

    Assert.assertEquals(false, e.isRightToLeft());
    Assert.assertEquals(false, e.isFixedLayout());
    Assert.assertEquals(10, e.getSpineItemCount());
    Assert.assertEquals(0, e.getOpenPages().size());
  }
}
