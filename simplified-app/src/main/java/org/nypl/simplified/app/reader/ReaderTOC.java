package org.nypl.simplified.app.reader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.readium.sdk.android.SpineItem;
import org.slf4j.Logger;

import com.io7m.jnull.NullCheck;

public final class ReaderTOC implements Serializable
{
  public static final class TOCElement implements Serializable
  {
    private static final long serialVersionUID = 1L;
    private final String      id_ref;
    private final String      title;

    private TOCElement(
      final String in_id_ref,
      final String in_title)
    {
      this.id_ref = NullCheck.notNull(in_id_ref);
      this.title = NullCheck.notNull(in_title);
    }

    public String getIDRef()
    {
      return this.id_ref;
    }

    public String getTitle()
    {
      return this.title;
    }

    @Override public String toString()
    {
      final StringBuilder b = new StringBuilder();
      b.append("[TOCElement id_ref=");
      b.append(this.id_ref);
      b.append(" title=");
      b.append(this.title);
      b.append("]");
      return NullCheck.notNull(b.toString());
    }
  }

  private static final Logger LOG;
  private static final long   serialVersionUID = 1L;

  static {
    LOG = LogUtilities.getLog(ReaderTOC.class);
  }

  public static ReaderTOC fromPackage(
    final org.readium.sdk.android.Package p)
  {
    NullCheck.notNull(p);

    final List<TOCElement> rs = new ArrayList<TOCElement>();
    final List<SpineItem> toc = p.getSpineItems();
    for (final SpineItem i : toc) {
      ReaderTOC.LOG.debug("spine item: {} {}", i.getIdRef(), i.getTitle());
      rs.add(new TOCElement(i.getIdRef(), i.getTitle()));
    }

    return new ReaderTOC(rs);
  }

  private final List<TOCElement> elements;

  private ReaderTOC(
    final List<TOCElement> rs)
  {
    this.elements = NullCheck.notNull(rs);
  }

  public List<TOCElement> getElements()
  {
    return this.elements;
  }
}
