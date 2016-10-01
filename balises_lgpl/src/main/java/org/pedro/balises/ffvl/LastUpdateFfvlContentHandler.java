/*******************************************************************************
 * BalisesLib is Copyright 2012 by Pedro M.
 * 
 * This file is part of BalisesLib.
 *
 * BalisesLib is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * BalisesLib is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * Commercial Distribution License
 * If you would like to distribute BalisesLib (or portions thereof) under a
 * license other than the "GNU Lesser General Public License, version 3", please
 * contact Pedro M (pedro.pub@free.fr).
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with BalisesLib. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.pedro.balises.ffvl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.pedro.balises.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * 
 * @author pedro.m
 */
public final class LastUpdateFfvlContentHandler implements ContentHandler
{
  public static final String      INFOS_KEY         = "infos";
  public static final String      DECOLLAGES_KEY    = "decollages";
  public static final String      ATTERRISSAGES_KEY = "atterrissages";
  public static final String      BALISES_KEY       = "balises";
  public static final String      RELEVES_KEY       = "releves_meteo";
  public static final String      SPOTS_KEY         = "spots_kite";

  private static final DateFormat DATE_FORMAT       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final String     INFOS_TAG         = INFOS_KEY;
  private static final String     DECOLLAGES_TAG    = DECOLLAGES_KEY;
  private static final String     ATTERRISSAGES_TAG = ATTERRISSAGES_KEY;
  private static final String     BALISES_TAG       = BALISES_KEY;
  private static final String     RELEVES_TAG       = RELEVES_KEY;
  private static final String     SPOTS_TAG         = SPOTS_KEY;
  private static final String     VALUE_ATT         = "value";

  private final Map<String, Long> updateDates       = new HashMap<String, Long>();

  /**
   * 
   * @return
   */
  public Map<String, Long> getUpdateDates()
  {
    return updateDates;
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException
  {
    //Nothing
  }

  @Override
  public void endElement(final String uri, final String localName, final String qName) throws SAXException
  {
    //Nothing
  }

  @Override
  public void startDocument() throws SAXException
  {
    updateDates.clear();
  }

  @Override
  public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException
  {
    String finalName = qName;
    if ((finalName == null) || (finalName.length() == 0))
    {
      finalName = localName;
    }

    // La valeur de la date
    final String valueString = atts.getValue(VALUE_ATT);
    Long timestamp = null;
    if (valueString != null)
    {
      try
      {
        // Analyse date
        final Date date = DATE_FORMAT.parse(valueString);

        // Decalage dans le fuseau horaire UTC
        Utils.toUTC(date, FfvlProvider.sourceTimeZone);

        // Timestamp final
        timestamp = Long.valueOf(date.getTime());
      }
      catch (final Throwable th)
      {
        System.err.println("Error parsing for <" + finalName + "> (" + th.getClass().getSimpleName() + ")");
        //th.printStackTrace(System.err);
      }
    }

    // Infos
    if (INFOS_TAG.equals(finalName) && (timestamp != null))
    {
      updateDates.put(INFOS_KEY, timestamp);
    }
    else if (DECOLLAGES_TAG.equals(finalName) && (timestamp != null))
    {
      updateDates.put(DECOLLAGES_KEY, timestamp);
    }
    else if (ATTERRISSAGES_TAG.equals(finalName) && (timestamp != null))
    {
      updateDates.put(ATTERRISSAGES_KEY, timestamp);
    }
    else if (BALISES_TAG.equals(finalName) && (timestamp != null))
    {
      updateDates.put(BALISES_KEY, timestamp);
    }
    else if (RELEVES_TAG.equals(finalName) && (timestamp != null))
    {
      updateDates.put(RELEVES_KEY, timestamp);
    }
    else if (SPOTS_TAG.equals(finalName) && (timestamp != null))
    {
      updateDates.put(SPOTS_KEY, timestamp);
    }
  }

  @Override
  public void startPrefixMapping(final String arg0, final String arg1) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public void endDocument() throws SAXException
  {
    // Nothing to do
  }

  @Override
  public void endPrefixMapping(final String arg0) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public void processingInstruction(final String arg0, final String arg1) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public void setDocumentLocator(final Locator arg0)
  {
    // Nothing to do
  }

  @Override
  public void skippedEntity(final String arg0) throws SAXException
  {
    // Nothing to do
  }
}
