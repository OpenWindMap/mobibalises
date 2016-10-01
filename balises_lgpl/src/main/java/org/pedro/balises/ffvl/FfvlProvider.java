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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.pedro.balises.AbstractBaliseProvider;
import org.pedro.balises.Balise;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * 
 * @author pedro.m
 */
public class FfvlProvider extends AbstractBaliseProvider
{
  protected static final String              URL_FFVL_KEY            = "ffvlKey";
  public static final String                 URL_FFVL_KEY_GROUP      = "\\{" + URL_FFVL_KEY + "\\}";

  //private static final String                URL_BALISES             = "file:C:/Temp/balise_list.xml";
  //private static final String                URL_BALISES             = "http://www.balisemeteo.com/xml/{" + URL_FFVL_KEY + "}/balise_list.xml";
  private static final String                URL_BALISES             = "http://data.ffvl.fr/xml/{" + URL_FFVL_KEY + "}/meteo/balise_list.xml";

  //private static final String                URL_RELEVES             = "file:C:/Temp/relevemeteo-20110721.xml";
  //private static final String                URL_RELEVES             = "http://www.balisemeteo.com/xml/{" + URL_FFVL_KEY + "}/relevemeteo.xml";
  protected static final String              URL_RELEVES             = "http://data.ffvl.fr/xml/{" + URL_FFVL_KEY + "}/meteo/relevemeteo.xml";

  public static final String                 URL_LAST_UPDATE         = "http://data.ffvl.fr/xml/{" + URL_FFVL_KEY + "}/lastupdate.xml";

  public static final String                 URL_INFOS               = "http://data.ffvl.fr/xml/{" + URL_FFVL_KEY + "}/infos/infos.xml";

  private static final String                SUFFIXE_COMPRESSION     = ".gz";

  private static final String                URL_BALISE_ID_KEY       = "idBalise";
  private static final String                URL_BALISE_ID_KEY_GROUP = "\\{" + URL_BALISE_ID_KEY + "\\}";
  private static final String                URL_DETAIL_BALISE       = "http://www.balisemeteo.com/balise.php?idBalise={" + URL_BALISE_ID_KEY + "}";
  private static final String                URL_HISTORIQUE_BALISE   = "http://www.balisemeteo.com/balise_histo.php?idBalise={" + URL_BALISE_ID_KEY + "}";

  private static final List<String>          COUNTRIES               = new ArrayList<String>();

  // Fuseaux hoaires
  private static final String                TIME_ZONE_ID            = "Europe/Paris";
  public static final TimeZone               sourceTimeZone          = TimeZone.getTimeZone(TIME_ZONE_ID);

  private final SAXParserFactory             factory;
  private final SAXParser                    parser;
  private final BaliseFfvlContentHandler     baliseHandler;
  protected final ReleveFfvlContentHandler   releveHandler;
  private final LastUpdateFfvlContentHandler lastUpdateHandler;

  protected final String                     ffvlKey;
  private final boolean                      useZippedData;

  private boolean                            balisesUpdateDateCalled = false;
  private boolean                            lastUpdateUpdateDatesReturn;
  protected final Map<String, Long>          updateDates             = new HashMap<String, Long>();

  /**
   * 
   */
  static
  {
    COUNTRIES.add(Locale.FRANCE.getCountry());
  }

  /**
   * 
   * @param name
   * @param country
   * @param ffvlKey
   * @param useZippedData
   */
  public FfvlProvider(final String name, final String country, final String ffvlKey, final boolean useZippedData)
  {
    this(name, country, ffvlKey, useZippedData, new ReleveFfvlContentHandler());
  }

  /**
   * 
   * @param name
   * @param country
   * @param ffvlKey
   * @param useZippedData
   * @param releveHandler
   */
  protected FfvlProvider(final String name, final String country, final String ffvlKey, final boolean useZippedData, final ReleveFfvlContentHandler releveHandler)
  {
    // Initialisation
    super(name, country, null, 150);
    this.ffvlKey = ffvlKey;
    this.useZippedData = useZippedData;

    // Initialisation SAX
    try
    {
      factory = SAXParserFactory.newInstance();
      parser = factory.newSAXParser();
      baliseHandler = new BaliseFfvlContentHandler();
      this.releveHandler = releveHandler;
      releveHandler.setListener(this);
      lastUpdateHandler = new LastUpdateFfvlContentHandler();
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
  public boolean isAvailable()
  {
    boolean retour = false;

    try
    {
      final String finalUrl = URL_BALISES.replaceAll(URL_FFVL_KEY_GROUP, ffvlKey);
      final URL url = new URL(finalUrl);
      final URLConnection cnx = url.openConnection();
      if (cnx != null)
      {
        cnx.setConnectTimeout(Utils.CONNECT_TIMEOUT);
        cnx.setReadTimeout(Utils.READ_TIMEOUT);
        cnx.connect();
        retour = true;
      }
    }
    catch (final IOException ioe)
    {
      retour = false;
    }

    return retour;
  }

  @Override
  public boolean updateBalisesUpdateDate() throws IOException
  {
    balisesUpdateDateCalled = true;

    lastUpdateUpdateDatesReturn = updateUpdateDates();
    return lastUpdateUpdateDatesReturn;
  }

  /**
   * 
   * @return
   * @throws IOException
   */
  protected boolean updateUpdateDates() throws IOException
  {
    // Initialisations
    boolean updated = false;
    InputStream input = null;

    try
    {
      final String finalUrl = URL_LAST_UPDATE.replaceAll(URL_FFVL_KEY_GROUP, ffvlKey);
      input = getUnzippedInputStream(finalUrl);
      final Map<String, Long> newUpdateDates = parseUpdateDatesMap(new InputSource(input));

      // Tout est OK
      updateDates.clear();
      updateDates.putAll(newUpdateDates);
      updated = true;
    }
    finally
    {
      if (input != null)
      {
        input.close();
      }
    }

    return updated;
  }

  /**
   * 
   * @param source
   * @return
   * @throws IOException
   */
  protected Map<String, Long> parseUpdateDatesMap(final InputSource source) throws IOException
  {
    try
    {
      // Analyse XML
      final XMLReader reader = parser.getXMLReader();
      reader.setContentHandler(lastUpdateHandler);
      reader.parse(source);

      // Stockage
      return lastUpdateHandler.getUpdateDates();
    }
    catch (final SAXException se)
    {
      final IOException ioe = new IOException(se.getMessage());
      ioe.setStackTrace(se.getStackTrace());
      throw ioe;
    }
  }

  /**
   * 
   * @param key
   * @return
   */
  private long getUpdateDate(final String key)
  {
    final Long updateDate = updateDates.get(key);
    if (updateDate == null)
    {
      return -1;
    }

    return updateDate.longValue();
  }

  /**
   * 
   * @param url
   * @return
   * @throws IOException
   */
  private InputStream getUnzippedInputStream(final String url) throws IOException
  {
    return getUnzippedInputStream(url, useZippedData);
  }

  /**
   * 
   * @param url
   * @param inUseZippedData
   * @return
   * @throws IOException
   */
  public static InputStream getUnzippedInputStream(final String url, final boolean inUseZippedData) throws IOException
  {
    // Initialisations
    InputStream retour = null;

    // Donnees compressees ou pas ?
    if (inUseZippedData)
    {
      final URLConnection cnx = new URL(url + SUFFIXE_COMPRESSION).openConnection();
      cnx.setRequestProperty(REQUEST_PROPERTY_ACCEPT_ENCODING, REQUEST_PROPERTY_ACCEPT_ENCODING_IDENTITY);
      cnx.setConnectTimeout(Utils.CONNECT_TIMEOUT);
      cnx.setReadTimeout(Utils.READ_TIMEOUT);
      retour = new GZIPInputStream(cnx.getInputStream());
    }
    else
    {
      final URLConnection cnx = new URL(url).openConnection();
      cnx.setConnectTimeout(Utils.CONNECT_TIMEOUT);
      cnx.setReadTimeout(Utils.READ_TIMEOUT);
      retour = cnx.getInputStream();
    }

    return retour;
  }

  @Override
  public long getBalisesUpdateDate() throws IOException
  {
    return getUpdateDate(LastUpdateFfvlContentHandler.BALISES_KEY);
  }

  @Override
  public boolean updateBalises() throws IOException
  {
    // Initialisations
    boolean updated = false;
    InputStream input = null;

    try
    {
      final String finalUrl = URL_BALISES.replaceAll(URL_FFVL_KEY_GROUP, ffvlKey);
      input = getUnzippedInputStream(finalUrl);
      final Map<String, Balise> newBalises = parseBalisesMap(new InputSource(input));

      // Tout est OK
      setBalisesMap(newBalises);
      updated = true;
    }
    finally
    {
      if (input != null)
      {
        input.close();
      }
    }

    return updated;
  }

  /**
   * 
   * @param source
   * @return
   * @throws IOException
   */
  private Map<String, Balise> parseBalisesMap(final InputSource source) throws IOException
  {
    try
    {
      // Analyse XML
      final XMLReader reader = parser.getXMLReader();
      reader.setContentHandler(baliseHandler);
      reader.parse(source);

      // Stockage
      return baliseHandler.getBalises();
    }
    catch (final SAXException se)
    {
      final IOException ioe = new IOException(se.getMessage());
      ioe.setStackTrace(se.getStackTrace());
      throw ioe;
    }
  }

  @Override
  public String getBaliseDetailUrl(final String id)
  {
    return URL_DETAIL_BALISE.replaceAll(URL_BALISE_ID_KEY_GROUP, id);
  }

  @Override
  public String getBaliseHistoriqueUrl(final String id)
  {
    return URL_HISTORIQUE_BALISE.replaceAll(URL_BALISE_ID_KEY_GROUP, id);
  }

  @Override
  public boolean updateRelevesUpdateDate() throws IOException
  {
    final boolean retour = (balisesUpdateDateCalled ? lastUpdateUpdateDatesReturn : updateUpdateDates());

    balisesUpdateDateCalled = false;

    return retour;
  }

  @Override
  public long getRelevesUpdateDate() throws IOException
  {
    return getUpdateDate(LastUpdateFfvlContentHandler.RELEVES_KEY);
  }

  @Override
  public boolean updateReleves() throws IOException
  {
    // Initialisations
    InputStream input = null;

    try
    {
      updatedReleves.clear();
      final String finalUrl = URL_RELEVES.replaceAll(URL_FFVL_KEY_GROUP, ffvlKey);
      input = getUnzippedInputStream(finalUrl);
      parseReleves(new InputSource(input));
    }
    finally
    {
      if (input != null)
      {
        input.close();
      }
    }

    return !updatedReleves.isEmpty();
  }

  /**
   * 
   * @param source
   * @throws IOException
   */
  protected void parseReleves(final InputSource source) throws IOException
  {
    try
    {
      // Analyse XML
      final XMLReader reader = parser.getXMLReader();
      reader.setContentHandler(releveHandler);
      reader.parse(source);
    }
    catch (final SAXException se)
    {
      final IOException ioe = new IOException(se.getMessage());
      ioe.setStackTrace(se.getStackTrace());
      throw ioe;
    }
  }

  @Override
  public Balise newBalise()
  {
    return new BaliseFfvl();
  }

  @Override
  public Releve newReleve()
  {
    return new Releve();
  }

  @Override
  public String filterExceptionMessage(final String message)
  {
    // Null
    if (message == null)
    {
      return null;
    }

    return message.replaceAll(ffvlKey, "xxx");
  }

  /**
   * 
   * @return
   */
  public static List<String> getAvailableCountries()
  {
    return COUNTRIES;
  }

  @Override
  public boolean isMultiCountries()
  {
    return false;
  }

  @Override
  public int getDefaultDeltaReleves()
  {
    return 20;
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
      BaliseProvider provider = new FfvlProvider("4D6F626942616C69736573", false);

      boolean balisesUpdateDateUpdated = provider.updateBalisesUpdateDate();
      System.out.println("balisesUpdateDateUpdated : " + balisesUpdateDateUpdated);
      System.out.println("MAJ Balises : " + new Date(provider.getBalisesUpdateDate()));

      boolean relevesUpdateDateUpdated = provider.updateRelevesUpdateDate();
      System.out.println("relevesUpdateDateUpdated : " + relevesUpdateDateUpdated);
      System.out.println("MAJ Releves : " + new Date(provider.getRelevesUpdateDate()));

      provider.updateBalises();
      provider.updateReleves();
      System.out.println("balises : " + provider.getBalises().size());
      for (Balise balise : provider.getBalises())
      {
        System.out.println("releve for " + balise.nom + " (" + balise.id + ") : " + provider.getReleveById(balise.id));
      }
    }
    catch (Throwable th)
    {
      th.printStackTrace();
    }
    
    // Test IOException
    final IOException ioe = new IOException("toto");
    final StackTraceElement[] elements = new StackTraceElement[3];
    elements[0] = new StackTraceElement("arg0.0", "arg1.0", "arg2.0", 0);
    elements[1] = new StackTraceElement("arg0.1", "arg1.1", "arg2.1", 2);
    elements[2] = new StackTraceElement("arg0.2", "arg1.2", "arg2.2", 2);
    ioe.setStackTrace(elements);
    System.out.println("done : " + ioe);
    ioe.printStackTrace();
  }
  */
}
