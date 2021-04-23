package org.nypl.simplified.opds.core;

import com.io7m.jfunctional.OptionType;
import com.io7m.jfunctional.PartialFunctionType;
import com.io7m.junreachable.UnreachableCodeException;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

import java.io.Serializable;
import java.text.ParseException;

final class OPDSAtom implements Serializable
{
  private static final long serialVersionUID = 1L;

  private OPDSAtom()
  {
    throw new UnreachableCodeException();
  }

  static String findID(
    final Element ee)
    throws OPDSParseException
  {
    return OPDSXML.getFirstChildElementTextWithName(
      ee, OPDSFeedConstants.ATOM_URI, "id");
  }

  static OptionType<DateTime> findPublished(
    final Element e)
    throws DOMException, ParseException
  {
    final OptionType<Element> e_opt =
      OPDSXML.getFirstChildElementWithNameOptional(
        e, OPDSFeedConstants.DUBLIN_CORE_TERMS_URI, "issued");

    return e_opt.mapPartial(
      (PartialFunctionType<Element, DateTime, ParseException>) er -> {
        final String text = er.getTextContent();
        final String trimmed = text.trim();
        return ISODateTimeFormat.dateTimeParser().parseDateTime(trimmed);
      });
  }

  static String findTitle(
    final Element e)
    throws OPDSParseException
  {
    return OPDSXML.getFirstChildElementTextWithName(
      e, OPDSFeedConstants.ATOM_URI, "title");
  }

  static DateTime findUpdated(
    final Element e)
    throws OPDSParseException {
    final String e_updated_raw =
      OPDSXML.getFirstChildElementTextWithName(e, OPDSFeedConstants.ATOM_URI, "updated");
    return ISODateTimeFormat.dateTimeParser().parseDateTime(e_updated_raw);
  }
}
