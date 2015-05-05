package org.nypl.simplified.app.reader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nypl.simplified.app.utilities.LogUtilities;
import org.readium.sdk.android.components.navigation.NavigationElement;
import org.readium.sdk.android.components.navigation.NavigationPoint;
import org.readium.sdk.android.components.navigation.NavigationTable;
import org.slf4j.Logger;

import com.io7m.jnull.NullCheck;

public final class ReaderTOC implements Serializable
{
  public static final class TOCElement implements Serializable
  {
    private static final long serialVersionUID = 1L;

    private final String      title;
    private final String      content_ref;
    private final String      source_href;
    private final int         indent;

    private TOCElement(
      final int in_indent,
      final String in_title,
      final String in_content_ref,
      final String in_source_href)
    {
      this.indent = in_indent;
      this.title = NullCheck.notNull(in_title);
      this.content_ref = NullCheck.notNull(in_content_ref);
      this.source_href = NullCheck.notNull(in_source_href);
    }

    public int getIndent()
    {
      return this.indent;
    }

    public String getContentRef()
    {
      return this.content_ref;
    }

    public String getSourceHref()
    {
      return this.source_href;
    }

    public String getTitle()
    {
      return this.title;
    }

    @Override public String toString()
    {
      final StringBuilder b = new StringBuilder();
      b.append("[TOCElement title=");
      b.append(this.title);
      b.append(" content_ref=");
      b.append(this.content_ref);
      b.append(" source_href=");
      b.append(this.source_href);
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
    final NavigationTable toc = p.getTableOfContents();
    ReaderTOC.accumulate(rs, -1, toc, toc);
    return new ReaderTOC(rs);
  }

  private static void accumulate(
    final List<TOCElement> elements,
    final int indent,
    final NavigationTable parent,
    final NavigationElement e)
  {
    if (e instanceof NavigationPoint) {
      final NavigationPoint p = (NavigationPoint) e;
      final String title = p.getTitle();
      final String content_ref = p.getContent();
      final String source_href = parent.getSourceHref();
      ReaderTOC.LOG.debug("nav point: {} → {}", content_ref, title);
      final TOCElement te =
        new TOCElement(indent, title, content_ref, source_href);
      elements.add(te);

      for (final NavigationElement ec : p.getChildren()) {
        ReaderTOC.accumulate(elements, indent + 1, parent, ec);
      }

      return;
    }

    if (e instanceof NavigationTable) {
      final NavigationTable t = (NavigationTable) e;
      ReaderTOC.LOG.debug(
        "nav table: {} {} → {}",
        t.getSourceHref(),
        t.getType(),
        t.getTitle());

      // XXX: What's the correct thing to do here? There's no
      // content ref accessible from here...

      for (final NavigationElement ec : e.getChildren()) {
        ReaderTOC.accumulate(elements, indent + 1, t, ec);
      }
    }
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
