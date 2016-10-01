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

import org.pedro.balises.Releve;
import org.pedro.balises.ReleveParserListener;
import org.pedro.balises.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * 
 * @author pedro.m
 */
public class ReleveFfvlContentHandler implements ContentHandler
{
  // Constantes
  private static final String    STRING_VIDE        = "";
  private static final String    RELEVE_TAG         = "releve";
  private static final String    ID_BALISE_TAG      = "idbalise";
  private static final String    DATE_TAG           = "date";
  private static final String    VITESSE_MOY_TAG    = "vitesseVentMoy";
  private static final String    VITESSE_MAX_TAG    = "vitesseVentMax";
  private static final String    VITESSE_MIN_TAG    = "vitesseVentMin";
  private static final String    DIRECTION_MOY_TAG  = "directVentMoy";
  private static final String    DIRECTION_INST_TAG = "directVentInst";
  private static final String    TEMPERATURE_TAG    = "temperature";
  private static final String    HYDROMETRIE_TAG    = "hydrometrie";
  private static final String    PRESSION_TAG       = "pression";
  private static final String    LUMINOSITE_TAG     = "luminosite";

  // Membres
  protected final Releve         releve             = new Releve();
  protected String               currentString      = STRING_VIDE;
  protected ReleveParserListener listener;

  /**
   * 
   * @param listener
   */
  protected void setListener(final ReleveParserListener listener)
  {
    this.listener = listener;
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException
  {
    currentString += new String(ch, start, length);
  }

  @Override
  public void endElement(final String uri, final String localName, final String qName) throws SAXException
  {
    String finalName = qName;
    if ((finalName == null) || (finalName.length() == 0))
    {
      finalName = localName;
    }

    try
    {
      if (RELEVE_TAG.equals(finalName))
      {
        listener.onReleveParsed(releve);
      }

      else if (ID_BALISE_TAG.equals(finalName))
      {
        releve.setId(currentString);
      }

      else if (DATE_TAG.equals(finalName))
      {
        releve.date = FfvlUtils.parseDate(currentString);

        // Decalage dans le fuseau horaire UTC
        if (releve.date != null)
        {
          Utils.toUTC(releve.date, FfvlProvider.sourceTimeZone);
        }
      }

      else if (VITESSE_MOY_TAG.equals(finalName))
      {
        releve.ventMoyen = Utils.parsePrimitiveDouble(currentString);
      }

      else if (VITESSE_MAX_TAG.equals(finalName))
      {
        releve.ventMaxi = Utils.parsePrimitiveDouble(currentString);
      }

      else if (VITESSE_MIN_TAG.equals(finalName))
      {
        releve.ventMini = Utils.parsePrimitiveDouble(currentString);
      }

      else if (DIRECTION_MOY_TAG.equals(finalName))
      {
        releve.directionMoyenne = Utils.parsePrimitiveInteger(currentString);
      }

      else if (DIRECTION_INST_TAG.equals(finalName))
      {
        releve.directionInstantanee = Utils.parsePrimitiveInteger(currentString);
      }

      else if (TEMPERATURE_TAG.equals(finalName))
      {
        releve.temperature = Utils.parsePrimitiveDouble(currentString);
      }

      else if (HYDROMETRIE_TAG.equals(finalName))
      {
        releve.hydrometrie = Utils.parsePrimitiveDouble(currentString);
      }

      else if (PRESSION_TAG.equals(finalName))
      {
        releve.pression = Utils.parsePrimitiveDouble(currentString);
      }

      else if (LUMINOSITE_TAG.equals(finalName))
      {
        releve.luminosite = Utils.isStringVide(currentString) ? null : currentString;
      }
    }
    catch (final Throwable th)
    {
      System.err.println("Error parsing '" + currentString + "' for <" + finalName + "> (" + th.getClass().getSimpleName() + ")");
      //th.printStackTrace(System.err);
    }

    // RAZ
    currentString = STRING_VIDE;
  }

  @Override
  public void startDocument() throws SAXException
  {
    currentString = STRING_VIDE;
  }

  @Override
  public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException
  {
    String finalName = qName;
    if ((finalName == null) || (finalName.length() == 0))
    {
      finalName = localName;
    }

    if (RELEVE_TAG.equals(finalName))
    {
      releve.clear();
    }

    // RAZ
    currentString = STRING_VIDE;
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
