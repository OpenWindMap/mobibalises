package org.pedro.spots.pge;

import java.util.ArrayList;
import java.util.List;

import org.pedro.spots.Orientation;
import org.pedro.spots.Pratique;
import org.pedro.spots.Spot;
import org.pedro.spots.TypeSpot;
import org.pedro.spots.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * 
 * @author pedro.m
 */
public final class PgeContentHandler implements ContentHandler
{
  private static final List<Pratique> PRATIQUES_ATTERRISSAGE     = new ArrayList<Pratique>();
  private static final String         STRING_VIDE                = "";

  private static final String         TAKEOFF_TAG                = "takeoff";
  private static final String         TAKEOFF_NAME_TAG           = "name";
  private static final String         TAKEOFF_ALTITUDE_TAG       = "takeoff_altitude";
  private static final String         TAKEOFF_LATITUDE_TAG       = "lat";
  private static final String         TAKEOFF_LONGITUDE_TAG      = "lng";
  private static final String         TAKEOFF_PARAGLIDING_TAG    = "paragliding";
  private static final String         TAKEOFF_HANGGLIDING_TAG    = "hanggliding";
  private static final String         TAKEOFF_ORIENTATIONS_TAG   = "orientations";
  private static final String         TAKEOFF_ORIENTATION_N_TAG  = "N";
  private static final String         TAKEOFF_ORIENTATION_NE_TAG = "NE";
  private static final String         TAKEOFF_ORIENTATION_E_TAG  = "E";
  private static final String         TAKEOFF_ORIENTATION_SE_TAG = "SE";
  private static final String         TAKEOFF_ORIENTATION_S_TAG  = "S";
  private static final String         TAKEOFF_ORIENTATION_SO_TAG = "SW";
  private static final String         TAKEOFF_ORIENTATION_O_TAG  = "W";
  private static final String         TAKEOFF_ORIENTATION_NO_TAG = "NW";
  private static final String         TAKEOFF_ID_TAG             = "pge_site_id";
  private static final String         TAKEOFF_DESCRIPTION_TAG    = "takeoff_description";
  private static final String         TAKEOFF_ACCES_TAG          = "going_there";
  private static final String         TAKEOFF_COMMENTAIRES_TAG   = "comments";
  private static final String         TAKEOFF_METEO_TAG          = "weather";
  private static final String         TAKEOFF_REGLEMENTATION_TAG = "flight_rules";
  private static final String         TAKEOFF_CONTACT_TAG        = "contacts";
  private static final String         TAKEOFF_WEBSITE_TAG        = "related_website";

  private static final String         LANDING_TAG                = "landing";
  private static final String         LANDING_NAME_TAG           = "landing_name";
  private static final String         LANDING_ALTITUDE_TAG       = "landing_altitude";
  private static final String         LANDING_DESCRIPTION_TAG    = "landing_description";
  private static final String         LANDING_LATITUDE_TAG       = "landing_lat";
  private static final String         LANDING_LONGITUDE_TAG      = "landing_lng";
  private static final String         LANDING_ID_TAG             = "landing_pge_site_id";

  private String                      currentString              = STRING_VIDE;
  private boolean                     flagOrientations           = false;

  // Membres
  private final List<Spot>            spots                      = new ArrayList<Spot>();
  private Spot                        spot;

  /**
   * 
   */
  static
  {
    PRATIQUES_ATTERRISSAGE.add(Pratique.PARAPENTE);
    PRATIQUES_ATTERRISSAGE.add(Pratique.DELTA);
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

    if (TAKEOFF_NAME_TAG.equals(finalName) || LANDING_NAME_TAG.equals(finalName))
    {
      spot.nom = currentString;
    }
    else if (TAKEOFF_ALTITUDE_TAG.equals(finalName) || LANDING_ALTITUDE_TAG.equals(finalName))
    {
      try
      {
        spot.altitude = Utils.parseInteger(currentString);
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (TAKEOFF_LATITUDE_TAG.equals(finalName) || LANDING_LATITUDE_TAG.equals(finalName))
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
    else if (TAKEOFF_LONGITUDE_TAG.equals(finalName) || LANDING_LONGITUDE_TAG.equals(finalName))
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
    else if (TAKEOFF_PARAGLIDING_TAG.equals(finalName))
    {
      try
      {
        final Integer integer = Utils.parseInteger(currentString);
        if ((integer != null) && (integer.intValue() > 0))
        {
          spot.addPratique(Pratique.PARAPENTE);
        }
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (TAKEOFF_HANGGLIDING_TAG.equals(finalName))
    {
      try
      {
        final Integer integer = Utils.parseInteger(currentString);
        if ((integer != null) && (integer.intValue() > 0))
        {
          spot.addPratique(Pratique.DELTA);
        }
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (TAKEOFF_ORIENTATIONS_TAG.equals(finalName))
    {
      flagOrientations = false;
    }
    else if (flagOrientations && TAKEOFF_ORIENTATION_N_TAG.equals(finalName))
    {
      try
      {
        final Integer integer = Utils.parseInteger(currentString);
        if ((integer != null) && (integer.intValue() > 0))
        {
          addOrientation(Orientation.N);
        }
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (flagOrientations && TAKEOFF_ORIENTATION_NE_TAG.equals(finalName))
    {
      try
      {
        final Integer integer = Utils.parseInteger(currentString);
        if ((integer != null) && (integer.intValue() > 0))
        {
          addOrientation(Orientation.NE);
        }
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (flagOrientations && TAKEOFF_ORIENTATION_E_TAG.equals(finalName))
    {
      try
      {
        final Integer integer = Utils.parseInteger(currentString);
        if ((integer != null) && (integer.intValue() > 0))
        {
          addOrientation(Orientation.E);
        }
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (flagOrientations && TAKEOFF_ORIENTATION_SE_TAG.equals(finalName))
    {
      try
      {
        final Integer integer = Utils.parseInteger(currentString);
        if ((integer != null) && (integer.intValue() > 0))
        {
          addOrientation(Orientation.SE);
        }
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (flagOrientations && TAKEOFF_ORIENTATION_S_TAG.equals(finalName))
    {
      try
      {
        final Integer integer = Utils.parseInteger(currentString);
        if ((integer != null) && (integer.intValue() > 0))
        {
          addOrientation(Orientation.S);
        }
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (flagOrientations && TAKEOFF_ORIENTATION_SO_TAG.equals(finalName))
    {
      final Integer integer = Utils.parseInteger(currentString);
      if ((integer != null) && (integer.intValue() > 0))
      {
        addOrientation(Orientation.SO);
      }
    }
    else if (flagOrientations && TAKEOFF_ORIENTATION_O_TAG.equals(finalName))
    {
      try
      {
        final Integer integer = Utils.parseInteger(currentString);
        if ((integer != null) && (integer.intValue() > 0))
        {
          addOrientation(Orientation.O);
        }
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (flagOrientations && TAKEOFF_ORIENTATION_NO_TAG.equals(finalName))
    {
      try
      {
        final Integer integer = Utils.parseInteger(currentString);
        if ((integer != null) && (integer.intValue() > 0))
        {
          addOrientation(Orientation.NO);
        }
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (TAKEOFF_ID_TAG.equals(finalName) || LANDING_ID_TAG.equals(finalName))
    {
      spot.setId(currentString);
    }
    else if (TAKEOFF_DESCRIPTION_TAG.equals(finalName) || LANDING_DESCRIPTION_TAG.equals(finalName))
    {
      if (!Utils.isStringVide(currentString))
      {
        spot.infos.put(PgeSpotProvider.Info.DESCRIPTION.getKey(), currentString);
      }
    }
    else if (TAKEOFF_ACCES_TAG.equals(finalName))
    {
      if (!Utils.isStringVide(currentString))
      {
        spot.infos.put(PgeSpotProvider.Info.ACCES.getKey(), currentString);
      }
    }
    else if (TAKEOFF_COMMENTAIRES_TAG.equals(finalName))
    {
      if (!Utils.isStringVide(currentString))
      {
        spot.infos.put(PgeSpotProvider.Info.COMMENTAIRES.getKey(), currentString);
      }
    }
    else if (TAKEOFF_METEO_TAG.equals(finalName))
    {
      if (!Utils.isStringVide(currentString))
      {
        spot.infos.put(PgeSpotProvider.Info.METEO.getKey(), currentString);
      }
    }
    else if (TAKEOFF_REGLEMENTATION_TAG.equals(finalName))
    {
      if (!Utils.isStringVide(currentString))
      {
        spot.infos.put(PgeSpotProvider.Info.REGLEMENTATION.getKey(), currentString);
      }
    }
    else if (TAKEOFF_CONTACT_TAG.equals(finalName))
    {
      if (!Utils.isStringVide(currentString))
      {
        spot.infos.put(PgeSpotProvider.Info.CONTACT.getKey(), currentString);
      }
    }
    else if (TAKEOFF_WEBSITE_TAG.equals(finalName))
    {
      if (!Utils.isStringVide(currentString))
      {
        spot.infos.put(PgeSpotProvider.Info.SITE_WEB.getKey(), currentString);
      }
    }

    // RAZ
    currentString = STRING_VIDE;
  }

  /**
   * 
   * @param orientation
   */
  private void addOrientation(final Orientation orientation)
  {
    spot.orientations.add(orientation);
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

    if (TAKEOFF_TAG.equals(finalName))
    {
      spot = new Spot();
      spot.type = TypeSpot.DECOLLAGE;
      spots.add(spot);
    }
    else if (LANDING_TAG.equals(finalName))
    {
      spot = new Spot();
      spot.type = TypeSpot.ATTERRISSAGE;
      spot.pratiques = PRATIQUES_ATTERRISSAGE;
      spots.add(0, spot);
    }
    else if (TAKEOFF_ORIENTATIONS_TAG.equals(finalName))
    {
      flagOrientations = true;
    }

    // RAZ
    currentString = STRING_VIDE;
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException
  {
    currentString += new String(ch, start, length);
    currentString = currentString.replace("\\'", "'");
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
