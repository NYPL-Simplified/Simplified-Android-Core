package org.nypl.simplified.documentation;

import com.io7m.jstructural.tools.JSCMain;

/**
 * Documentation generator main function.
 */

public final class MakeDocumentation
{
  public static void main(
    final String[] args)
    throws Throwable
  {
    JSCMain.run(JSCMain.getLog(false), args);
  }
}
