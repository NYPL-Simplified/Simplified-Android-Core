package org.nypl.simplified.app.reader;

import com.io7m.jnull.NullCheck;
import org.nypl.simplified.books.core.LogUtilities;
import org.readium.sdk.android.components.navigation.NavigationElement;
import org.readium.sdk.android.components.navigation.NavigationPoint;
import org.readium.sdk.android.components.navigation.NavigationTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The table of contents.
 */

public final class ReaderTOC implements Serializable
{
  private static final Logger LOG = LoggerFactory.getLogger(ReaderTOC.class);
  private static final long serialVersionUID = 1L;

  private final List<TOCElement> elements;

  private ReaderTOC(
    final List<TOCElement> rs)
  {
    this.elements = NullCheck.notNull(rs);
  }

  private static void accumulate(
    final List<TOCElement> elements,
    final int indent,
    final NavigationTable parent,
    final NavigationElement e)
  {
    ReaderTOC.LOG.debug("accumulate: {}", e);

    if (e instanceof NavigationPoint) {
      final NavigationPoint p = (NavigationPoint) e;
      final String title = NullCheck.notNull(p.getTitle());
      final String content_ref = NullCheck.notNull(p.getContent());
      final String source_href = NullCheck.notNull(parent.getSourceHref());
      ReaderTOC.LOG.debug("nav point: {} → {}", content_ref, title);
      final TOCElement te =
        new TOCElement(indent, title, content_ref, source_href);
      elements.add(te);

      for (final NavigationElement ec : p.getChildren()) {
        ReaderTOC.accumulate(
          elements, indent + 1, parent, NullCheck.notNull(ec));
      }

      return;
    }

    if (e instanceof NavigationTable) {
      final NavigationTable t = (NavigationTable) e;
      ReaderTOC.LOG.debug(
        "nav table: {} {} → {}", t.getSourceHref(), t.getType(), t.getTitle());

      // XXX: What's the correct thing to do here? There's no
      // content ref accessible from here...

      final List<NavigationElement> child_elements = e.getChildren();
      ReaderTOC.LOG.debug(
        "nav table: {} child elements", child_elements.size());
      for (final NavigationElement ec : child_elements) {
        ReaderTOC.accumulate(elements, indent + 1, t, NullCheck.notNull(ec));
      }
    }
  }

  /**
   * Parse and return a table of contents from the given package.
   *
   * @param p The package
   *
   * @return A table of contents
   */

  public static ReaderTOC fromPackage(
    final org.readium.sdk.android.Package p)
  {
    NullCheck.notNull(p);

    ReaderTOC.LOG.debug("requesting toc");

    final ReaderNativeCodeReadLock read_lock = ReaderNativeCodeReadLock.get();

    final List<TOCElement> rs = new ArrayList<TOCElement>(32);
    final NavigationTable toc;

    synchronized (read_lock) {
      toc = NullCheck.notNull(p.getTableOfContents());
    }

    ReaderTOC.accumulate(rs, -1, toc, toc);
    return new ReaderTOC(rs);
  }

  /**
   * @return The list of elements in the table.
   */

  public List<TOCElement> getElements()
  {
    return this.elements;
  }

  /**
   * A TOC element.
   */

  public static final class TOCElement implements Serializable
  {
    private static final long serialVersionUID = 1L;

    private final String content_ref;
    private final int    indent;
    private final String source_href;
    private final String title;

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

    /**
     * @return The content ref for the element
     */

    public String getContentRef()
    {
      return this.content_ref;
    }

    /**
     * @return The indentation level of the element (used when rendering the
     * TOC)
     */

    public int getIndent()
    {
      return this.indent;
    }

    /**
     * @return The source href of the element
     */

    public String getSourceHref()
    {
      return this.source_href;
    }

    /**
     * @return The title of the element
     */

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
}
