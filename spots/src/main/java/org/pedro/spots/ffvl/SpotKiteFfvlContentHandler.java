package org.pedro.spots.ffvl;

import java.util.ArrayList;
import java.util.List;

import org.pedro.spots.Pratique;
import org.pedro.spots.Spot;
import org.pedro.spots.TypeSpot;
import org.pedro.spots.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * 
 * @author pedro.m
 */
public final class SpotKiteFfvlContentHandler extends FfvlContentHandler
{
  private static final List<Pratique> PRATIQUES     = new ArrayList<Pratique>();

  private static final String         SPOT_TAG      = "spot";
  private static final String         ID_TAG        = "id";
  private static final String         NOM_TAG       = "nom";
  private static final String         LATITUDE_TAG  = "latitude";
  private static final String         LONGITUDE_TAG = "longitude";

  // Membres
  private final List<Spot>            spots         = new ArrayList<Spot>();
  private FfvlSpot                    spot;

  /**
   * Constructeur statique
   */
  static
  {
    PRATIQUES.add(Pratique.KITE);
  }

  /**
   * Recuperation de la liste des spots
   * 
   * @return
   */
  public List<Spot> getSpots()
  {
    return spots;
  }

  @Override
  public void endElement(final String uri, final String localName, final String qName) throws SAXException
  {
    String finalName = qName;
    if ((finalName == null) || (finalName.length() == 0))
    {
      finalName = localName;
    }

    if (NOM_TAG.equals(finalName))
    {
      spot.nom = currentString;
    }
    else if (ID_TAG.equals(finalName))
    {
      spot.setId(currentString);
    }
    else if (LATITUDE_TAG.equals(finalName))
    {
      try
      {
        spot.latitude = Utils.parseDouble(currentString);
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (LONGITUDE_TAG.equals(finalName))
    {
      try
      {
        spot.longitude = Utils.parseDouble(currentString);
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }

    // RAZ
    currentString = STRING_VIDE;
  }

  @Override
  public void startDocument() throws SAXException
  {
    spots.clear();
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

    if (SPOT_TAG.equals(finalName))
    {
      spot = new FfvlSpot();
      spot.type = TypeSpot.SPOT;
      spot.pratiques = PRATIQUES;
      spots.add(spot);
    }

    // RAZ
    currentString = STRING_VIDE;
  }
}
