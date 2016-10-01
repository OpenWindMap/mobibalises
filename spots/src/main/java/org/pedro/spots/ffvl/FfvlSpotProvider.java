package org.pedro.spots.ffvl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

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
public final class FfvlSpotProvider implements SpotProvider
{
  private static final String                  URL_FFVL_KEY        = "ffvlKey";
  private static final String                  URL_FFVL_KEY_GROUP  = "\\{" + URL_FFVL_KEY + "\\}";

  private static final String                  URL_SITE_ID         = "siteId";
  private static final String                  URL_SITE_ID_GROUP   = "\\{" + URL_SITE_ID + "\\}";
  //private static final String                  URL_STRUCTURE_ID       = "structureId";
  //private static final String                  URL_STRUCTURE_ID_GROUP = "\\{" + URL_STRUCTURE_ID + "\\}";
  //private static final String                  URL_SITE               = "http://federation.ffvl.fr/structure/{" + URL_STRUCTURE_ID + "}/sites/{" + URL_SITE_ID + "}";
  private static final String                  URL_SITE            = "http://federation.ffvl.fr/sites_pratique/voir/{" + URL_SITE_ID + "}";

  private static final String                  SUFFIXE_COMPRESSION = ".gz";

  private static final List<String>            COUNTRIES           = new ArrayList<String>(1);

  //private static final String               URL_DECOLLAGES      = "file:C:/temp/decollages.xml";
  private static final String                  URL_DECOLLAGES      = "http://data.ffvl.fr/xml/{" + URL_FFVL_KEY + "}/sites/decollages.xml";

  //private static final String               URL_ATTERRISSAGES      = "file:C:/temp/atterrissages.xml";
  private static final String                  URL_ATTERRISSAGES   = "http://data.ffvl.fr/xml/{" + URL_FFVL_KEY + "}/sites/atterrissages.xml";

  //private static final String               URL_SPOTS_KITE      = "file:C:/temp/spots_kite.xml";
  private static final String                  URL_SPOTS_KITE      = "http://data.ffvl.fr/xml/{" + URL_FFVL_KEY + "}/sites/spots_kite.xml";

  private final DecollageFfvlContentHandler    decollageHandler    = new DecollageFfvlContentHandler();
  private final AtterrissageFfvlContentHandler atterrissageHandler = new AtterrissageFfvlContentHandler();
  private final SpotKiteFfvlContentHandler     spotKiteHandler     = new SpotKiteFfvlContentHandler();

  private SAXParserFactory                     factory;
  private SAXParser                            parser;
  private XMLReader                            reader;

  private final String                         key;
  private final boolean                        useZippedData;

  private static final String[]                INFO_KEYS;

  enum Info
  {
    ACCES("acces");

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
   * @param key
   * @param useZippedData
   */
  public FfvlSpotProvider(final String key, final boolean useZippedData)
  {
    // Initialisations
    this.key = key;
    this.useZippedData = useZippedData;

    // SAX
    initSax();
  }

  @Override
  public Spot newSpot()
  {
    return new FfvlSpot();
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
  private InputStream getUnzippedInputStream(final String url) throws IOException
  {
    // Initialisations
    final InputStream retour;

    // Donnees compressees ou pas ?
    if (useZippedData)
    {
      final URL finalUrl = new URL(url + SUFFIXE_COMPRESSION);
      retour = new GZIPInputStream(finalUrl.openStream());
    }
    else
    {
      final URL finalUrl = new URL(url);
      retour = finalUrl.openStream();
    }

    return retour;
  }

  @Override
  public List<Spot> getSpots(final String countryCode) throws IOException
  {
    // Initialisations
    final List<Spot> spots = new ArrayList<Spot>();

    // Kite
    // Avant on ne les prenait pas, ils etaient deja dans decollages.xml
    final List<Spot> spotsKite = getSpotsKite();
    spots.addAll(spotsKite);

    // Atterros
    final List<Spot> atterros = getAtterrissages();
    spots.addAll(atterros);

    // Decos
    final List<Spot> decos = getDecollages();
    spots.addAll(decos);

    return spots;
  }

  /**
   * 
   * @return
   * @throws IOException
   */
  private List<Spot> getDecollages() throws IOException
  {
    // Initialisations
    InputStream input = null;

    try
    {
      final String finalUrl = URL_DECOLLAGES.replaceAll(URL_FFVL_KEY_GROUP, key);
      input = getUnzippedInputStream(finalUrl);

      // Analyse XML
      reader.setContentHandler(decollageHandler);
      reader.parse(new InputSource(input));

      // Stockage
      return decollageHandler.getSpots();
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

  /**
   * 
   * @return
   * @throws IOException
   */
  private List<Spot> getAtterrissages() throws IOException
  {
    // Initialisations
    InputStream input = null;

    try
    {
      final String finalUrl = URL_ATTERRISSAGES.replaceAll(URL_FFVL_KEY_GROUP, key);
      input = getUnzippedInputStream(finalUrl);

      // Analyse XML
      reader.setContentHandler(atterrissageHandler);
      reader.parse(new InputSource(input));

      // Stockage
      return atterrissageHandler.getSpots();
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

  /**
   * 
   * @return
   * @throws IOException
   */
  private List<Spot> getSpotsKite() throws IOException
  {
    // Initialisations
    InputStream input = null;

    try
    {
      final String finalUrl = URL_SPOTS_KITE.replaceAll(URL_FFVL_KEY_GROUP, key);
      input = getUnzippedInputStream(finalUrl);

      // Analyse XML
      reader.setContentHandler(spotKiteHandler);
      reader.parse(new InputSource(input));

      // Stockage
      return spotKiteHandler.getSpots();
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
    if ((id == null) || (otherInfos == null) || (otherInfos.length == 0))
    {
      return null;
    }

    // Verification structure
    final String idStructure = (String)otherInfos[0];
    if (idStructure == null)
    {
      return null;
    }

    // URL finale
    return URL_SITE.replaceAll(URL_SITE_ID_GROUP, id);
  }

  @Override
  public String[] getInfoKeys()
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
      SpotProvider provider = new FfvlSpotProvider("4D6F626942616C69736573", false);
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
