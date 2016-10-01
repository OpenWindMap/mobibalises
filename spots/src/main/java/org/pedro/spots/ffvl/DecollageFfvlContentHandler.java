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
public final class DecollageFfvlContentHandler extends FfvlContentHandler
{
  private static final String SITE_TAG              = "site";
  private static final String NOM_TAG               = "nom";
  private static final String COORD_TAG             = "coord";
  private static final String ACCES_TAG             = "acces";
  private static final String PRATIQUE_TAG          = "pratique";
  private static final String ALTITUDE_TAG          = "altitude";
  private static final String ORIENTATION_TAG       = "orientation";
  private static final String ID_SITE_TAG           = "id";
  private static final String STRUCTURE_TAG         = "structure";

  private static final String DECOLLAGE_ID_ATT      = "identifiant";
  private static final String COORD_LATITUDE_ATT    = "lat";
  private static final String COORD_LONGITUDE_ATT   = "lon";
  private static final String PRATIQUE_VALUE_ATT    = "value";
  private static final String ALTITUDE_VALUE_ATT    = "value";
  private static final String ORIENTATION_VALUE_ATT = "value";
  private static final String ID_SITE_VALUE_ATT     = "value";
  private static final String STRUCTURE_VALUE_ATT   = "value";

  // Membres
  private final List<Spot>    spots                 = new ArrayList<Spot>();
  private FfvlSpot            decollage;

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

    if (SITE_TAG.equals(finalName))
    {
      if ((decollage.pratiques != null) && (decollage.pratiques.size() == 1) && decollage.pratiques.contains(Pratique.KITE))
      {
        // En fait, c'est un spot de Kite !
        decollage.type = TypeSpot.SPOT;
      }
      else
      {
        decollage.type = TypeSpot.DECOLLAGE;
      }
    }
    else if (NOM_TAG.equals(finalName))
    {
      decollage.nom = currentString;
    }
    else if (ACCES_TAG.equals(finalName))
    {
      if (!Utils.isStringVide(currentString))
      {
        decollage.infos.put(FfvlSpotProvider.Info.ACCES.getKey(), currentString);
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

    if (SITE_TAG.equals(finalName))
    {
      decollage = new FfvlSpot();
      decollage.idSite = atts.getValue(DECOLLAGE_ID_ATT);
      spots.add(decollage);
    }
    else if (COORD_TAG.equals(finalName))
    {
      try
      {
        decollage.latitude = Utils.parseDouble(atts.getValue(COORD_LATITUDE_ATT));
        decollage.longitude = Utils.parseDouble(atts.getValue(COORD_LONGITUDE_ATT));
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (PRATIQUE_TAG.equals(finalName))
    {
      decollage.addPratique(getPratique(atts.getValue(PRATIQUE_VALUE_ATT)));
    }
    else if (ALTITUDE_TAG.equals(finalName))
    {
      try
      {
        decollage.altitude = Utils.parseInteger(atts.getValue(ALTITUDE_VALUE_ATT));
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (ORIENTATION_TAG.equals(finalName))
    {
      getOrientations(atts.getValue(ORIENTATION_VALUE_ATT), decollage.orientations);
    }
    else if (STRUCTURE_TAG.equals(finalName))
    {
      decollage.idStructure = atts.getValue(STRUCTURE_VALUE_ATT);
    }
    else if (ID_SITE_TAG.equals(finalName))
    {
      decollage.setId(atts.getValue(ID_SITE_VALUE_ATT));
    }

    // RAZ
    currentString = STRING_VIDE;
  }
}
