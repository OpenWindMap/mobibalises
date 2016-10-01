package org.pedro.balises.metar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.pedro.balises.Balise;
import org.pedro.balises.Utils;

/**
 * 
 * @author pedro.m
 */
public abstract class AviationWeatherListMetarProvider extends NewMetarProvider
{
  private static final char                             CHAR_SPACE        = ' ';
  private static final int                              PAYS_LENGTH       = 19;
  private static final int                              REGION_LENGTH     = 3;
  private static final String                           STRING_ISO_8859_1 = "ISO-8859-1";
  private static final DateFormat                       FORMAT_DATE_PAYS  = new SimpleDateFormat("dd-MMM-yy", Locale.US);

  private static final String                           LONGITUDE_EST     = "E";
  private static final String                           COLONNE_10_METAR  = "  X";
  private static final String                           SPACE             = " ";
  private static final String                           LATITUDE_NORD     = "N";
  private static final String                           COUNTRY_US        = "US";
  private static final String                           COUNTRY_CA        = "CA";

  //private static final String       URL_BALISES_UPDATE          = "http://mobibalises.free.fr/data/metar.update";

  //private static final String       URL_BALISES       = "file:D:/downloads/stations.txt";
  //private static final String       URL_BALISES                 = "http://www.aviationweather.gov/adds/metars/stations.txt";
  private static final String                           URL_BALISES       = "http://www.aviationweather.gov/static/adds/metars/stations.txt";

  private static final int[]                            LONGUEURS         = { 2, 16, 5, 5, 5, 3, 3, 4, 3, 4, 3, 2, 2, 2, 2, 2, 1, 2 };

  private final List<String>                            lignesBalises     = new ArrayList<String>();
  private final Map<String, Balise>                     newBalises        = new HashMap<String, Balise>();
  private long                                          balisesUpdateDate;

  private static final Map<String, String>              codesPays         = new HashMap<String, String>();
  private static final Map<String, String>              libellesPays      = new HashMap<String, String>();
  private static final Map<String, Map<String, String>> libellesRegions   = new HashMap<String, Map<String, String>>();

  /**
   * 
   */
  static
  {
    codesPays.put("VG", "VI");
    codesPays.put("GF", "GY");
    codesPays.put("PF", "WS");
    codesPays.put("MG", "KM");
    codesPays.put("CS", "RS");
    codesPays.put("RE", "FR");

    libellesPays.put("VG", "VIRGIN ISLANDS");
    libellesPays.put("VI", "VIRGIN ISLANDS");
    libellesPays.put("RU", "RUSSIAN FEDERATION");
    libellesPays.put("IR", "IRAN, ISLAMIC REP");
    libellesPays.put("BA", "BOSNIA-HERZEGOVINA");
    libellesPays.put("KY", "GRAND CAYMAN IS");
    libellesPays.put("CF", "CENTRAL AFRICAN RE");
    libellesPays.put("CG", "CONGO, DEMOCRATIC");
    libellesPays.put("CD", "ZAIRE");
    libellesPays.put("CI", "COTE D'IVOIRE");
    libellesPays.put("DM", "DOMINICA ISLAND");
    libellesPays.put("TL", "EAST TIMOR");
    libellesPays.put("FJ", "FIJI/TONGA/TUVALU");
    libellesPays.put("GF", "GUYANA");
    libellesPays.put("GY", "GUYANA");
    libellesPays.put("PF", "SAMOA/POLYNESIA");
    libellesPays.put("WS", "SAMOA/POLYNESIA");
    libellesPays.put("GP", "LESSER ANTILLES");
    libellesPays.put("GN", "GUINEA-BISSAU");
    libellesPays.put("HK", "HONK KONG");
    libellesPays.put("KG", "KYRGYZSTAN US-MIL");
    libellesPays.put("KR", "KOREA");
    libellesPays.put("LA", "LAO");
    libellesPays.put("LY", "LIBYAN ARAB JAMAHI");
    libellesPays.put("MQ", "LESSER ANTILLES");
    libellesPays.put("MO", "MACAU");
    libellesPays.put("MM", "MYANMAR/BURMA");
    libellesPays.put("AN", "LESSER ANTILLES");
    libellesPays.put("KN", "LESSER ANTILLES");
    libellesPays.put("VC", "LESSER ANTILLES");
    libellesPays.put("LC", "LESSER ANTILLES");
    libellesPays.put("ST", "SAO TOME/PRINCIPE");
    libellesPays.put("RS", "YUGOSLAVIA");
    libellesPays.put("TJ", "TADJIKISTAN");
    libellesPays.put("TT", "TRINIDAD TOBAGO");
    libellesPays.put("AE", "U. ARAB EMIRATES");
    libellesPays.put("VN", "VIET NAM");
    libellesPays.put("EH", "SAHARA OCCIDENTAL");
    libellesPays.put("RE", "REUNION");

    // Initialisation regions
    staticInitRegions();
  }

  /**
   * 
   */
  private static void staticInitRegions()
  {
    // Nom du properties "en dur" car proguard va changer le package de la classe sans changer le properties !
    final ResourceBundle bundle = ResourceBundle.getBundle("org.pedro.balises.metar.region-names");
    final Enumeration<String> enume = bundle.getKeys();
    while (enume.hasMoreElements())
    {
      final String key = enume.nextElement();
      final String countryKey = key.substring(0, 2);
      final String regionKey = key.substring(3, 5);
      final String label = bundle.getString(key);
      addRegion(countryKey, regionKey, label);
    }
  }

  /**
   * 
   * @param countryKey
   * @param regionKey
   * @param label
   */
  private static void addRegion(final String countryKey, final String regionKey, final String label)
  {
    // Recuperation de la table pour le pays
    Map<String, String> libellesForCountry = libellesRegions.get(countryKey);

    // Creation de la table pour le pays si besoin
    if (libellesForCountry == null)
    {
      libellesForCountry = new HashMap<String, String>();
      libellesRegions.put(countryKey, libellesForCountry);
    }

    // Ajout du libelle de la region
    libellesForCountry.put(regionKey, label);
  }

  /**
   * 
   * @param name
   * @param country
   * @param region
   */
  public AviationWeatherListMetarProvider(final String name, final String country, final String region)
  {
    super(name, country, region);
  }

  /**
   * 
   * @return
   */
  private String getLibellePaysOuRegion()
  {
    // Region ?
    if (region != null)
    {
      final Map<String, String> libellesRegionsForCountry = libellesRegions.get(country);
      if (libellesRegionsForCountry != null)
      {
        final String libelleRegion = libellesRegionsForCountry.get(region);
        if (!Utils.isStringVide(libelleRegion))
        {
          return libelleRegion.toUpperCase();
        }
      }
    }

    // Sinon pays avec un libelle specifique ?
    final String libelle = libellesPays.get(country);
    if (!Utils.isStringVide(libelle))
    {
      return libelle.toUpperCase();
    }

    // Sinon pays avec un libelle standard
    return new Locale(country, country).getDisplayCountry(Locale.US).toUpperCase();
  }

  /**
   * 
   * @return
   */
  protected static final boolean isAviationWeatherAvailable()
  {
    boolean retour = false;

    try
    {
      final URL url = new URL(URL_BALISES);
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
    // Initialisations
    final String libellePays = getLibellePaysOuRegion();
    if (libellePays == null)
    {
      return false;
    }
    boolean updated = false;

    // Lecture des donnees
    final StringBuilder buffer = new StringBuilder(Utils.READ_BUFFER_SIZE + 10);
    Utils.readData(new URL(URL_BALISES), STRING_ISO_8859_1, buffer);

    // Initialisations
    BufferedReader reader = null;

    try
    {
      // Initialisations
      reader = new BufferedReader(new StringReader(buffer.toString()));
      lignesBalises.clear();

      // Pour chaque ligne
      String ligne = reader.readLine();
      final String start = Utils.formatLeft(libellePays, PAYS_LENGTH, CHAR_SPACE);
      while (ligne != null)
      {
        // Filtrage
        if (ligne.startsWith(start))
        {
          // Analyse de la date
          try
          {
            balisesUpdateDate = Utils.toUTC(FORMAT_DATE_PAYS.parse(ligne.substring(PAYS_LENGTH)).getTime());
          }
          catch (final ParseException pe)
          {
            pe.printStackTrace(System.err);
            return false;
          }

          // Fin
          updated = true;
          break;
        }

        // Next
        ligne = reader.readLine();
      }

      // Pays non trouve
      if (!updated)
      {
        System.err.println("Pays non trouve : " + country + " (" + libellePays + ")");
      }
    }
    finally
    {
      if (reader != null)
      {
        reader.close();
      }
    }

    return updated;
  }

  @Override
  public long getBalisesUpdateDate() throws IOException
  {
    return balisesUpdateDate;
  }

  @Override
  public boolean updateBalises() throws IOException
  {
    // Analyse
    parseBalises();

    // Tout est OK
    setBalisesMap(newBalises);

    return true;
  }

  /**
   * 
   * @throws IOException
   */
  private void parseBalises() throws IOException
  {
    // Recuperation des donnees brutes (pour le code pays)
    readLignesBalises();

    // Analyse
    parseLignesBalises();
  }

  /**
   * 
   * @throws IOException
   */
  private void readLignesBalises() throws IOException
  {
    // Lecture des donnees
    final StringBuilder buffer = new StringBuilder(Utils.READ_BUFFER_SIZE + 10);
    Utils.readData(new URL(URL_BALISES), STRING_ISO_8859_1, buffer);

    // Initialisations
    BufferedReader reader = null;

    try
    {
      // Initialisations
      reader = new BufferedReader(new StringReader(buffer.toString()));
      lignesBalises.clear();

      final String end = SPACE + getCountry().toUpperCase();
      final String countryStart = Utils.formatLeft(getLibellePaysOuRegion(), PAYS_LENGTH, CHAR_SPACE);
      final boolean regional = !Utils.isStringVide(region);
      final String regionStart = (region == null ? null : Utils.formatLeft(region, REGION_LENGTH, CHAR_SPACE));
      boolean started = false;
      String ligne = reader.readLine();
      while (ligne != null)
      {
        // Filtrage debut
        if (ligne.startsWith(countryStart))
        {
          started = true;
        }
        // Filtrage
        else if (started && ligne.endsWith(end) && (!regional || ligne.startsWith(regionStart)))
        {
          lignesBalises.add(ligne);
        }

        // Next
        ligne = reader.readLine();
      }
    }
    finally
    {
      if (reader != null)
      {
        reader.close();
      }
    }
  }

  /**
   * 
   */
  private void parseLignesBalises()
  {
    // Initialisations
    newBalises.clear();

    // Analyse des lignes
    for (final String ligne : lignesBalises)
    {
      final Balise balise = parseBalise(ligne);
      if (balise != null)
      {
        newBalises.put(balise.id, balise);
      }
    }
  }

  /**
   * 
   * @param ligne
   * @return
   */
  private static Balise parseBalise(final String ligne)
  {
    final String[] colonnes = parseColonnes(ligne);

    // Balise METAR ?
    if (!COLONNE_10_METAR.equalsIgnoreCase(colonnes[10]))
    {
      return null;
    }

    // ICAO ?
    if (colonnes[2].trim().length() < 4)
    {
      return null;
    }

    // OK !
    final Balise balise = new Balise();
    balise.active = Utils.BOOLEAN_TRUE;

    // Nom
    balise.nom = colonnes[1].trim();

    // ID
    balise.setId(colonnes[2].trim());

    // Latitude
    try
    {
      final double degres = Double.parseDouble(colonnes[5].trim());
      final double minutes = Double.parseDouble(colonnes[6].substring(0, 2));
      final int signe = (LATITUDE_NORD.equalsIgnoreCase(colonnes[6].substring(2, 3)) ? 1 : -1);
      balise.latitude = signe * (degres + minutes / 60);
    }
    catch (final NumberFormatException nfe)
    {
      // Nothing
    }

    // Longitude
    try
    {
      final double degres = Double.parseDouble(colonnes[7].trim());
      final double minutes = Double.parseDouble(colonnes[8].substring(0, 2));
      final int signe = (LONGITUDE_EST.equalsIgnoreCase(colonnes[8].substring(2, 3)) ? 1 : -1);
      balise.longitude = signe * (degres + minutes / 60);
    }
    catch (final NumberFormatException nfe)
    {
      // Nothing
    }

    // Altitude
    try
    {
      balise.altitude = Integer.parseInt(colonnes[9].trim(), 10);
    }
    catch (final NumberFormatException nfe)
    {
      // Nothing
    }

    return balise;
  }

  /**
   * 
   * @param ligne
   * @return
   */
  private static String[] parseColonnes(final String ligne)
  {
    final String[] colonnes = new String[LONGUEURS.length];

    int start = 0;
    for (int i = 0; i < LONGUEURS.length; i++)
    {
      colonnes[i] = ligne.substring(start, start + LONGUEURS[i]);

      start += LONGUEURS[i] + 1;
    }

    return colonnes;
  }

  @Override
  public Balise newBalise()
  {
    return new Balise();
  }

  @Override
  public String getCountry()
  {
    if (!codesPays.containsKey(country.toUpperCase()))
    {
      return country;
    }

    return codesPays.get(country);
  }

  /**
   * 
   * @param inCountry
   * @return
   */
  public static boolean isRegional(final String inCountry)
  {
    return (COUNTRY_US.equalsIgnoreCase(inCountry) || COUNTRY_CA.equalsIgnoreCase(inCountry));
  }
}
