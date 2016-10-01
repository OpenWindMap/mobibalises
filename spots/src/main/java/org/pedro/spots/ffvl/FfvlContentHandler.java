package org.pedro.spots.ffvl;

import java.util.List;

import org.pedro.spots.Orientation;
import org.pedro.spots.Pratique;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * 
 * @author pedro.m
 */
public abstract class FfvlContentHandler implements ContentHandler
{
  // Constantes
  protected static final String STRING_VIDE           = "";

  protected static final String PRATIQUE_PARAPENTE    = "parapente";
  protected static final String PRATIQUE_DELTA        = "delta";
  protected static final String PRATIQUE_SPEED_RIDING = "speed riding";
  protected static final String PRATIQUE_RIGIDE       = "rigide";
  protected static final String PRATIQUE_KITE         = "kite";
  protected static final String PRATIQUE_CERF_VOLANT  = "cerf volant";

  protected static final String ORIENTATION_N         = "N";
  protected static final String ORIENTATION_NNE       = "NNE";
  protected static final String ORIENTATION_NE        = "NE";
  protected static final String ORIENTATION_ENE       = "ENE";
  protected static final String ORIENTATION_E         = "E";
  protected static final String ORIENTATION_ESE       = "ESE";
  protected static final String ORIENTATION_SE        = "SE";
  protected static final String ORIENTATION_SSE       = "SSE";
  protected static final String ORIENTATION_S         = "S";
  protected static final String ORIENTATION_SSO       = "SSO";
  protected static final String ORIENTATION_SO        = "SO";
  protected static final String ORIENTATION_OSO       = "OSO";
  protected static final String ORIENTATION_O         = "O";
  protected static final String ORIENTATION_ONO       = "ONO";
  protected static final String ORIENTATION_NO        = "NO";
  protected static final String ORIENTATION_NNO       = "NNO";
  protected static final String ORIENTATION_TOUTES    = "TOUTES";

  protected String              currentString         = STRING_VIDE;

  @Override
  public final void characters(final char[] ch, final int start, final int length) throws SAXException
  {
    currentString += new String(ch, start, length);
  }

  /**
   * 
   * @param value
   * @param orientations
   */
  protected static final void getOrientations(final String value, final List<Orientation> orientations)
  {
    if (ORIENTATION_N.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.N);
    }
    else if (ORIENTATION_NNE.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.NNE);
    }
    else if (ORIENTATION_NE.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.NE);
    }
    else if (ORIENTATION_ENE.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.ENE);
    }
    else if (ORIENTATION_E.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.E);
    }
    else if (ORIENTATION_ESE.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.ESE);
    }
    else if (ORIENTATION_SE.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.SE);
    }
    else if (ORIENTATION_SSE.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.SSE);
    }
    else if (ORIENTATION_S.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.S);
    }
    else if (ORIENTATION_SSO.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.SSO);
    }
    else if (ORIENTATION_SO.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.SO);
    }
    else if (ORIENTATION_OSO.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.OSO);
    }
    else if (ORIENTATION_O.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.O);
    }
    else if (ORIENTATION_ONO.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.ONO);
    }
    else if (ORIENTATION_NO.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.NO);
    }
    else if (ORIENTATION_NNO.equalsIgnoreCase(value))
    {
      orientations.add(Orientation.NNO);
    }
    else if (ORIENTATION_TOUTES.equalsIgnoreCase(value))
    {
      for (final Orientation orientation : Orientation.values())
      {
        orientations.add(orientation);
      }
    }
  }

  /**
   * 
   * @param value
   * @return
   */
  protected static final Pratique getPratique(final String value)
  {
    if (PRATIQUE_CERF_VOLANT.equalsIgnoreCase(value))
    {
      return Pratique.CERF_VOLANT;
    }
    else if (PRATIQUE_DELTA.equalsIgnoreCase(value))
    {
      return Pratique.DELTA;
    }
    else if (PRATIQUE_SPEED_RIDING.equalsIgnoreCase(value))
    {
      return Pratique.SPEED_RIDING;
    }
    else if (PRATIQUE_KITE.equalsIgnoreCase(value))
    {
      return Pratique.KITE;
    }
    else if (PRATIQUE_PARAPENTE.equalsIgnoreCase(value))
    {
      return Pratique.PARAPENTE;
    }
    else if (PRATIQUE_RIGIDE.equalsIgnoreCase(value))
    {
      return Pratique.RIGIDE;
    }

    return null;
  }

  @Override
  public final void startPrefixMapping(final String arg0, final String arg1) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public final void endDocument() throws SAXException
  {
    // Nothing to do
  }

  @Override
  public final void endPrefixMapping(final String arg0) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public final void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public final void processingInstruction(final String arg0, final String arg1) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public final void setDocumentLocator(final Locator arg0)
  {
    // Nothing to do
  }

  @Override
  public final void skippedEntity(final String arg0) throws SAXException
  {
    // Nothing to do
  }
}
