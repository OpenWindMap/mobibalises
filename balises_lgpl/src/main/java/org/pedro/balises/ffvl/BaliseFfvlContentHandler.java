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

import java.util.HashMap;
import java.util.Map;

import org.pedro.balises.Balise;
import org.pedro.balises.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * 
 * @author pedro.m
 */
public final class BaliseFfvlContentHandler implements ContentHandler
{
  // Constantes
  private static final String       STRING_VIDE              = "";
  private static final String       BALISE_TAG               = "balise";
  private static final String       ID_BALISE_TAG            = "idBalise";
  private static final String       NOM_TAG                  = "nom";
  private static final String       ALTITUDE_TAG             = "altitude";
  private static final String       ALTITUDE_VALUE_ATT       = "value";
  private static final String       COORD_TAG                = "coord";
  private static final String       COORD_LATITUDE_ATT       = "lat";
  private static final String       COORD_LONGITUDE_ATT      = "lon";
  private static final String       DEPARTEMENT_TAG          = "departement";
  private static final String       DEPARTEMENT_VALUE_ATT    = "value";
  private static final String       DESCRIPTION_TAG          = "description";
  private static final String       REMARQUES_TAG            = "remarques";
  private static final String       URL_TAG                  = "url";
  private static final String       URL_VALUE_ATT            = "value";
  private static final String       URL_HISTORIQUE_TAG       = "urlHisto";
  private static final String       URL_HISTORIQUE_VALUE_ATT = "value";
  private static final String       ACTIVE_TAG               = "active";
  private static final String       FOR_KYTE_TAG             = "forKyte";
  private static final String       BOOLEAN_ONE              = "1";

  // Membres
  private final Map<String, Balise> balises                  = new HashMap<String, Balise>();
  private BaliseFfvl                balise;
  private String                    currentString            = STRING_VIDE;

  /**
   * Recuperation de la liste des balises
   * 
   * @return
   */
  public Map<String, Balise> getBalises()
  {
    return balises;
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

    if (ID_BALISE_TAG.equals(finalName))
    {
      balise.setId(currentString);
      balises.put(currentString, balise);
    }
    else if (NOM_TAG.equals(finalName))
    {
      balise.nom = currentString;
    }
    else if (DESCRIPTION_TAG.equals(finalName))
    {
      balise.description = currentString;
    }
    else if (REMARQUES_TAG.equals(finalName))
    {
      balise.commentaire = currentString;
    }
    else if (ACTIVE_TAG.equals(finalName))
    {
      balise.active = Utils.parsePrimitiveBoolean(currentString);
    }
    else if (FOR_KYTE_TAG.equals(finalName))
    {
      balise.kite = Boolean.valueOf(BOOLEAN_ONE.equals(currentString));
    }

    // RAZ
    currentString = STRING_VIDE;
  }

  @Override
  public void startDocument() throws SAXException
  {
    balises.clear();
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

    if (BALISE_TAG.equals(finalName))
    {
      balise = new BaliseFfvl();
    }
    else if (ALTITUDE_TAG.equals(finalName))
    {
      balise.altitude = Utils.parsePrimitiveInteger(atts.getValue(ALTITUDE_VALUE_ATT));
    }
    else if (COORD_TAG.equals(finalName))
    {
      balise.latitude = Utils.parsePrimitiveDouble(atts.getValue(COORD_LATITUDE_ATT));
      balise.longitude = Utils.parsePrimitiveDouble(atts.getValue(COORD_LONGITUDE_ATT));
    }
    else if (DEPARTEMENT_TAG.equals(finalName))
    {
      balise.departement = atts.getValue(DEPARTEMENT_VALUE_ATT);
    }
    else if (URL_TAG.equals(finalName))
    {
      balise.urlDetail = atts.getValue(URL_VALUE_ATT);
    }
    else if (URL_HISTORIQUE_TAG.equals(finalName))
    {
      balise.urlHistorique = atts.getValue(URL_HISTORIQUE_VALUE_ATT);
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
