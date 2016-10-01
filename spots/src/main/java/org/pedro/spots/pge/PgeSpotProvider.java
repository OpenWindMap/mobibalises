package org.pedro.spots.pge;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
public abstract class PgeSpotProvider implements SpotProvider
{
  private static final String       URL_SITE_ID            = "siteId";
  private static final String       URL_SITE_ID_GROUP      = "\\{" + URL_SITE_ID + "\\}";
  private static final String       URL_SITE               = "http://www.paraglidingearth.com/pgearth/index.php?site={" + URL_SITE_ID + "}";

  private static final List<String> COUNTRIES              = new ArrayList<String>();

  private static final String       URL_COUNTRY_CODE       = "countryCode";
  private static final String       URL_COUNTRY_CODE_GROUP = "\\{" + URL_COUNTRY_CODE + "\\}";

  private static final String       URL_SPOTS              = "http://www.paraglidingearth.com/api/getCountrySites.php?iso={" + URL_COUNTRY_CODE + "}&style=detailled";

  private final PgeContentHandler   handler                = new PgeContentHandler();

  private SAXParserFactory          factory;
  private SAXParser                 parser;
  private XMLReader                 reader;

  private static final String[]     INFO_KEYS;

  enum Info
  {
    DESCRIPTION("description"), ACCES("acces"), COMMENTAIRES("commentaires"), METEO("meteo"), REGLEMENTATION("reglementation"), CONTACT("contact"), SITE_WEB("site_web");

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

  static
  {
    COUNTRIES.add("fr");
    COUNTRIES.add("de");
    COUNTRIES.add("ch");
    COUNTRIES.add("at");
    COUNTRIES.add("es");
    COUNTRIES.add("ar");
    COUNTRIES.add("ma");

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
  public PgeSpotProvider()
  {
    // SAX
    initSax();
  }

  @Override
  public Spot newSpot()
  {
    return new Spot();
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

  @Override
  public final List<Spot> getSpots(final String countryCode) throws IOException
  {
    // Initialisations
    InputStream input = null;
    InputSource source = null;

    try
    {
      final String finalUrl = URL_SPOTS.replaceAll(URL_COUNTRY_CODE_GROUP, countryCode);
      input = new URL(finalUrl).openStream();

      // Analyse XML
      reader.setContentHandler(handler);
      source = new InputSource(input);
      source.setEncoding("ISO-8859-1");
      reader.parse(source);

      // Stockage
      return handler.getSpots();
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
  public final String getLienDetail(final String id, final Object[] otherInfos)
  {
    // Verification
    if (id == null)
    {
      return null;
    }

    // URL finale
    return URL_SITE.replaceAll(URL_SITE_ID_GROUP, id);
  }

  @Override
  public final String[] getInfoKeys()
  {
    return INFO_KEYS;
  }

  /**
   * 
   * @param args
   */
  /*
  public static void main(String[] args)
  {
    try
    {
      SpotProvider provider = new PgeEuropeOuestSpotProvider();
      List<Spot> spots = provider.getSpots("fr");
      System.out.println("spots : " + spots.size());
      System.out.println("spots : " + spots);
    }
    catch (Throwable th)
    {
      th.printStackTrace();
    }
  }
  */
}
