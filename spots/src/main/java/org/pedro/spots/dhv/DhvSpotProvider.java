package org.pedro.spots.dhv;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.pedro.spots.Spot;
import org.pedro.spots.SpotProvider;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * 
 * @author pedro.m
 */
public final class DhvSpotProvider implements SpotProvider
{
  private static final String       URL_SITE_ID            = "siteId";
  private static final String       URL_SITE_ID_GROUP      = "\\{" + URL_SITE_ID + "\\}";
  private static final String       URL_SITE               = "http://www.dhv.de/db2/details.php?qi=glp_details&popup=1&item={" + URL_SITE_ID + "}";

  private static final String       SUFFIXE_COMPRESSION    = ".zip";

  private static final List<String> COUNTRIES              = new ArrayList<String>();

  private static final String       URL_COUNTRY_CODE       = "countryCode";
  private static final String       URL_COUNTRY_CODE_GROUP = "\\{" + URL_COUNTRY_CODE + "\\}";
  //private static final String       URL_SPOTS              = "file:D:/download/dhvgelaende_dhvxml_{" + URL_COUNTRY_CODE + "}.xml";
  private static final String       URL_SPOTS              = "http://www.dhv.de/web/fileadmin/user_upload/dbfiles/gelaendedaten/dhvxml/dhvgelaende_dhvxml_{" + URL_COUNTRY_CODE + "}.xml";

  private final DhvContentHandler   dhvHandler             = new DhvContentHandler();

  private SAXParserFactory          factory;
  private SAXParser                 parser;
  private XMLReader                 reader;

  private static final String[]     INFO_KEYS;

  /**
   * 
   * @author pedro.m
   */
  enum Info
  {
    SITE("site"), ACCES("acces"), CONTACT("contact"), REGLEMENTATION("reglementation"), METEO("meteo"), TREUILLAGE("treuillage"), REMARQUE("remarque"), HEBERGEMENT("hebergement"), WEBCAM("webcam");

    private String key;

    /**
     * 
     * @param key
     */
    private Info(final String key)
    {
      this.key = key;
    }

    /**
     * 
     * @return
     */
    public String getKey()
    {
      return key;
    }
  }

  /**
   * 
   */
  static
  {
    COUNTRIES.add("de");
    COUNTRIES.add("at");
    COUNTRIES.add("ch");
    COUNTRIES.add("fr");
    COUNTRIES.add("it");
    COUNTRIES.add("si");
    COUNTRIES.add("hr");
    COUNTRIES.add("li");
    COUNTRIES.add("lu");
    COUNTRIES.add("dk");
    COUNTRIES.add("nl");

    INFO_KEYS = new String[Info.values().length];
    int i = 0;
    for (final Info info : Info.values())
    {
      INFO_KEYS[i] = info.getKey();

      // Next
      i++;
    }
  }

  /**
   * 
   */
  public DhvSpotProvider()
  {
    // SAX
    initSax();
  }

  /**
   * 
   */
  private void initSax()
  {
    // Initialisation SAX
    try
    {
      factory = SAXParserFactory.newInstance();
      parser = factory.newSAXParser();
      reader = parser.getXMLReader();
    }
    catch (final SAXException se)
    {
      throw new RuntimeException(se);
    }
    catch (final ParserConfigurationException pce)
    {
      throw new RuntimeException(pce);
    }
  }

  /**
   * 
   * @param url
   * @return
   * @throws IOException
   */
  private static InputStream getUnzippedInputStream(final String url) throws IOException
  {
    // Donnees compressees ou pas ?
    final URL finalUrl = new URL(url.replaceAll("\\.xml", SUFFIXE_COMPRESSION));
    final ZipInputStream retour = new ZipInputStream(finalUrl.openStream());
    retour.getNextEntry();

    return retour;
  }

  @Override
  public List<Spot> getSpots(final String countryCode) throws IOException
  {
    // Initialisations
    InputStream input = null;

    try
    {
      final String finalUrl = URL_SPOTS.replaceAll(URL_COUNTRY_CODE_GROUP, countryCode);
      input = getUnzippedInputStream(finalUrl);

      // Analyse XML
      reader.setContentHandler(dhvHandler);
      reader.parse(new InputSource(input));

      // Stockage
      return dhvHandler.getSpots();
    }
    catch (final SAXException se)
    {
      final IOException ioe = new IOException(se.getMessage());
      ioe.setStackTrace(se.getStackTrace());
      throw ioe;
    }
    finally
    {
      if (input != null)
      {
        input.close();
      }
    }
  }

  @Override
  public List<String> getAvailableCountries()
  {
    return COUNTRIES;
  }

  @Override
  public String getLienDetail(final String id, final Object[] otherInfos)
  {
    // Verification
    if (id == null)
    {
      return null;
    }

    // URL finale
    return URL_SITE.replaceAll(URL_SITE_ID_GROUP, (String)otherInfos[0]);
  }

  @Override
  public String[] getInfoKeys()
  {
    return INFO_KEYS;
  }

  @Override
  public Spot newSpot()
  {
    return new DhvSpot();
  }

  /**
   * 
   * @param args
   */
  public static void main(String[] args)
  {
    try
    {
      SpotProvider provider = new DhvSpotProvider();
      List<Spot> spots = provider.getSpots("ch");
      System.out.println("spots : " + spots.size());
      //System.out.println("spots : " + spots);
      for (final Spot spot : spots)
      {
        System.out.println("========================== spot : " + spot.nom + ", infos : \n" + spot.infos);
      }
    }
    catch (Throwable th)
    {
      th.printStackTrace();
    }
  }
}
