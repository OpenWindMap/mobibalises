package org.pedro.balises.romma;

import org.pedro.balises.Utils;
import org.xml.sax.SAXException;

/**
 * 
 * @author pedro.m
 */
public final class ReleveRommaMobibalisesContentHandler extends ReleveRommaContentHandler
{
  // Constantes
  private static final String DATE_RELEVE_PRECEDENT_TAG = "datePrec";
  private static final String TENDANCE_VITESSE_MOY_TAG  = "tendVMoy";

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
        if (!Utils.isStringVide(currentString))
        {
          releve.dateRelevePrecedent = RELEVE_DATE_FORMAT.parse(currentString);
        }

        // Decalage dans le fuseau horaire UTC
        if (releve.dateRelevePrecedent != null)
        {
          Utils.toUTC(releve.dateRelevePrecedent, XmlRommaProvider.sourceTimeZone);
        }
      }
      else if (TENDANCE_VITESSE_MOY_TAG.equals(finalName))
      {
        releve.ventMoyenTendance = (STRING_MOINS_MOINS.equals(currentString) ? Double.NaN : Utils.parsePrimitiveDouble(currentString));
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
