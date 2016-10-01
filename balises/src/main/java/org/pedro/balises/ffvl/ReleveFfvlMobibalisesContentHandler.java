package org.pedro.balises.ffvl;

import org.pedro.balises.Utils;
import org.xml.sax.SAXException;

/**
 * 
 * @author pedro.m
 */
public final class ReleveFfvlMobibalisesContentHandler extends ReleveFfvlContentHandler
{
  private static final String DATE_RELEVE_PRECEDENT_TAG = "datePrec";
  private static final String TENDANCE_VITESSE_MIN_TAG  = "tendVMin";
  private static final String TENDANCE_VITESSE_MOY_TAG  = "tendVMoy";
  private static final String TENDANCE_VITESSE_MAX_TAG  = "tendVMax";

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
      if (DATE_RELEVE_PRECEDENT_TAG.equals(finalName))
      {
        releve.dateRelevePrecedent = FfvlUtils.parseDate(currentString);

        // Decalage dans le fuseau horaire UTC
        if (releve.dateRelevePrecedent != null)
        {
          Utils.toUTC(releve.dateRelevePrecedent, FfvlProvider.sourceTimeZone);
        }
      }
      else if (TENDANCE_VITESSE_MIN_TAG.equals(finalName))
      {
        releve.ventMiniTendance = Utils.parsePrimitiveDouble(currentString);
      }
      else if (TENDANCE_VITESSE_MOY_TAG.equals(finalName))
      {
        releve.ventMoyenTendance = Utils.parsePrimitiveDouble(currentString);
      }
      else if (TENDANCE_VITESSE_MAX_TAG.equals(finalName))
      {
        releve.ventMaxiTendance = Utils.parsePrimitiveDouble(currentString);
      }
    }
    catch (final Throwable th)
    {
      System.err.println("Error parsing '" + currentString + "' for <" + finalName + "> (" + th.getClass().getSimpleName() + ")");
      //th.printStackTrace(System.err);
    }

    super.endElement(uri, localName, qName);
  }
}
