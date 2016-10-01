package org.pedro.balises.synop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pedro.balises.Balise;
import org.pedro.balises.HistoryBaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;

/**
 * 
 * @author pedro.m
 */
public final class OgimetSynopProvider extends SynopProvider implements HistoryBaliseProvider
{
  // Environ la taille des donnees pour la france...
  private static final int                 LINES_BUFFER_SIZE       = 4 * 1024;

  private static final String              UTF_8                   = "UTF-8";

  private static final String              STRING_VIDE             = "";
  private static final String              _19_ESPACES             = "                   ";
  private static final String              CARACTERE_EGAL          = "=";
  private static final String              CARACTERE_DEBUT_HTML    = "<";
  private static final String              CARACTERE_COMMENTAIRE   = "#";
  private static final char                CARACTERE_PIPE          = '|';

  private static final String              LONGITUDE_EST           = "E";
  private static final String              LATITUDE_NORD           = "N";

  private static final Pattern             OGIMET_SYNOP_PATTERN    = Pattern.compile("^(\\d{12})\\s(.*)$");

  private static final String              URL_PAYS_KEY            = "paysKey";
  private static final String              URL_PAYS_KEY_GROUP      = "\\{" + URL_PAYS_KEY + "\\}";
  private static final String              URL_ANNEE_DEB_KEY       = "anneeDebKey";
  private static final String              URL_ANNEE_DEB_KEY_GROUP = "\\{" + URL_ANNEE_DEB_KEY + "\\}";
  private static final String              URL_ANNEE_FIN_KEY       = "anneeFinKey";
  private static final String              URL_ANNEE_FIN_KEY_GROUP = "\\{" + URL_ANNEE_FIN_KEY + "\\}";
  private static final String              URL_MOIS_DEB_KEY        = "moisDebKey";
  private static final String              URL_MOIS_DEB_KEY_GROUP  = "\\{" + URL_MOIS_DEB_KEY + "\\}";
  private static final String              URL_MOIS_FIN_KEY        = "moisFinKey";
  private static final String              URL_MOIS_FIN_KEY_GROUP  = "\\{" + URL_MOIS_FIN_KEY + "\\}";
  private static final String              URL_JOUR_DEB_KEY        = "jourDebKey";
  private static final String              URL_JOUR_DEB_KEY_GROUP  = "\\{" + URL_JOUR_DEB_KEY + "\\}";
  private static final String              URL_JOUR_FIN_KEY        = "jourFinKey";
  private static final String              URL_JOUR_FIN_KEY_GROUP  = "\\{" + URL_JOUR_FIN_KEY + "\\}";
  private static final String              URL_HEURE_DEB_KEY       = "heureDebKey";
  private static final String              URL_HEURE_DEB_KEY_GROUP = "\\{" + URL_HEURE_DEB_KEY + "\\}";
  private static final String              URL_HEURE_FIN_KEY       = "heureFinKey";
  private static final String              URL_HEURE_FIN_KEY_GROUP = "\\{" + URL_HEURE_FIN_KEY + "\\}";

  //private static final String              URL_BALISES             = "file:D:/_workspaces/perso/mobibalises/balises/res/synops_list.htm";
  private static final String              URL_BALISES             = "http://www.ogimet.com/display_synopsc2.php?lang=en&estado={" + URL_PAYS_KEY + "}&tipo=ALL&ord=REV&nil=SI&fmt=txt&ano={" + URL_ANNEE_DEB_KEY + "}&mes={"
                                                                       + URL_MOIS_DEB_KEY + "}&day={" + URL_JOUR_DEB_KEY + "}&hora={" + URL_HEURE_DEB_KEY + "}&anof={" + URL_ANNEE_FIN_KEY + "}&mesf={" + URL_MOIS_FIN_KEY + "}&dayf={"
                                                                       + URL_JOUR_FIN_KEY + "}&horaf={" + URL_HEURE_FIN_KEY + "}&send=send";

  //private static final String              URL_RELEVES             = "file:D:/_workspaces/perso/mobibalises/balises/res/latest_synops.htm";
  private static final String              URL_RELEVES             = "http://www.ogimet.com/ultimos_synops2.php?lang=en&estado={" + URL_PAYS_KEY + "}&fmt=txt&Send=Send";

  private static final DateFormat          OGIMET_DATE_FORMAT      = new SimpleDateFormat("yyyyMMddHHmm");
  private static final DateFormat          HISTO_ANNEE_FORMAT      = new SimpleDateFormat("yyyy");
  private static final DateFormat          HISTO_MOIS_FORMAT       = new SimpleDateFormat("MM");
  private static final DateFormat          HISTO_JOUR_FORMAT       = new SimpleDateFormat("dd");
  private static final DateFormat          HISTO_HEURE_FORMAT      = new SimpleDateFormat("HH");
  private static final String              URL_STATION_KEY         = "stationKey";
  private static final String              URL_STATION_KEY_GROUP   = "\\{" + URL_STATION_KEY + "\\}";
  //private static final String              URL_HISTO               = "file:D:/_workspaces/perso/mobibalises/balises/res/synops_histo.htm";
  private static final String              URL_HISTO               = "http://www.ogimet.com/display_synops2.php?lang=en&lugar={" + URL_STATION_KEY + "}&tipo=ALL&ord=DIR&nil=SI&fmt=txt&ano={" + URL_ANNEE_DEB_KEY + "}&mes={"
                                                                       + URL_MOIS_DEB_KEY + "}&day={" + URL_JOUR_DEB_KEY + "}&hora={" + URL_HEURE_DEB_KEY + "}&anof={" + URL_ANNEE_FIN_KEY + "}&mesf={" + URL_MOIS_FIN_KEY + "}&dayf={"
                                                                       + URL_JOUR_FIN_KEY + "}&horaf={" + URL_HEURE_FIN_KEY + "}&send=send";

  private static final Map<String, String> PAYS_CODES              = new HashMap<String, String>();
  private static final Map<String, String> PAYS_LABELS             = new HashMap<String, String>();

  private static final List<String>        COUNTRIES               = new ArrayList<String>();

  private final List<String>               lignesBalises           = new ArrayList<String>();
  private final Map<String, Balise>        newBalises              = new HashMap<String, Balise>();
  @SuppressWarnings("unused")
  private long                             balisesUpdateDate;
  private final Pattern                    balisePatternDegMin;
  private final Pattern                    balisePatternDegMinSec;

  private final List<String>               lignes                  = new ArrayList<String>();

  /**
   * 
   */
  static
  {
    // Nom du properties "en dur" car proguard va changer le package de la classe sans changer le properties !
    final ResourceBundle bundle = ResourceBundle.getBundle("org.pedro.balises.ogimet-countries");
    final Enumeration<String> enume = bundle.getKeys();
    while (enume.hasMoreElements())
    {
      final String key = enume.nextElement();
      final String value = bundle.getString(key);
      final int pipeIndex = value.indexOf(CARACTERE_PIPE);
      final String ogimetCode = value.substring(0, pipeIndex);
      final String label = value.substring(pipeIndex + 1);
      addCountry(key, ogimetCode, label);
    }
  }

  /**
   * 
   * @param country
   * @param ogimetCode
   * @param label
   */
  private static void addCountry(final String country, final String ogimetCode, final String label)
  {
    COUNTRIES.add(country);
    PAYS_CODES.put(country, ogimetCode);
    PAYS_LABELS.put(country, label);
  }

  /**
   * 
   * @param name
   * @param country
   */
  public OgimetSynopProvider(final String name, final String country)
  {
    super(name, country, null);

    String balisePatternStringDegMin = "#  SYNOPS from (\\d{5})"; // Indicatif
    String balisePatternStringDegMinSec = "#  SYNOPS from (\\d{5})"; // Indicatif
    balisePatternStringDegMin += ", ([\\w\\s/\\.,-]+)"; // Nom
    balisePatternStringDegMinSec += ", ([\\w\\s/\\.,-]+)"; // Nom
    final String ogimetCountryLabel = PAYS_LABELS.get(country.toUpperCase());
    balisePatternStringDegMin += " \\(" + ogimetCountryLabel + "\\)"; // Pays
    balisePatternStringDegMinSec += " \\(" + ogimetCountryLabel + "\\)"; // Pays
    balisePatternStringDegMin += " \\| (\\d{2})-(\\d{2})([NS])"; // Latitude deg/min
    balisePatternStringDegMinSec += " \\| (\\d{2})-(\\d{2})-(\\d{2})([NS])"; // Latitude deg/min/sec
    balisePatternStringDegMin += " \\| (\\d{3})-(\\d{2})([EW])"; // Longitude deg/min
    balisePatternStringDegMinSec += " \\| (\\d{3})-(\\d{2})-(\\d{2})([EW])"; // Longitude deg/min/sec
    balisePatternStringDegMin += " \\| (\\d+) m"; // Altitude
    balisePatternStringDegMinSec += " \\| (\\d+) m"; // Altitude
    balisePatternDegMin = Pattern.compile(balisePatternStringDegMin);
    balisePatternDegMinSec = Pattern.compile(balisePatternStringDegMinSec);
  }

  @Override
  public boolean isAvailable()
  {
    return isOgimetAvailable();
  }

  /**
   * 
   * @return
   */
  private String getOgimetCountry()
  {
    if (PAYS_CODES.get(country) != null)
    {
      return country;
    }

    return getCountry();
  }

  /**
   * 
   * @return
   * @throws IOException
   */
  private String getFinalUrl() throws IOException
  {
    return URL_RELEVES.replaceAll(URL_PAYS_KEY_GROUP, URLEncoder.encode(PAYS_CODES.get(getOgimetCountry()), "UTF-8"));
  }

  /**
   * 
   * @return
   */
  private boolean isOgimetAvailable()
  {
    boolean retour = false;

    try
    {
      final URL url = new URL(getFinalUrl());
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
  public long getRelevesUpdateDate() throws IOException
  {
    return Utils.toUTC(System.currentTimeMillis());
  }

  @Override
  public boolean updateRelevesUpdateDate() throws IOException
  {
    return true;
  }

  @Override
  public boolean updateReleves() throws IOException
  {
    // Initialisations
    updatedReleves.clear();

    // Lecture des lignes
    getLignesReleves();

    // Analyse
    parseSynops(lignes);

    return !updatedReleves.isEmpty();
  }

  /**
   * 
   * @throws IOException
   */
  private void getLignesReleves() throws IOException
  {
    getLignesReleves(getFinalUrl(), lignes, false);
  }

  /**
   * 
   * @param finalUrl
   * @param inLignes
   * @param includeDate
   * @throws IOException
   */
  private static void getLignesReleves(final String finalUrl, final List<String> inLignes, final boolean includeDate) throws IOException
  {
    // Lecture des donnees
    final StringBuilder buffer = new StringBuilder(LINES_BUFFER_SIZE);
    Utils.readData(new URL(finalUrl), UTF_8, buffer);

    // Initialisations
    BufferedReader reader = null;

    try
    {
      // Initialisations
      reader = new BufferedReader(new StringReader(buffer.toString()));
      inLignes.clear();

      String ligne = reader.readLine();
      while (ligne != null)
      {
        // Filtrage
        if ((ligne.trim().length() > 0) && !ligne.trim().startsWith(CARACTERE_COMMENTAIRE) && !ligne.trim().startsWith(CARACTERE_DEBUT_HTML))
        {
          final Matcher matcher = OGIMET_SYNOP_PATTERN.matcher(ligne);
          if (matcher.matches())
          {
            inLignes.add(matcher.group(includeDate ? 0 : 2));
          }
          else if (ligne.trim().endsWith(CARACTERE_EGAL) || ligne.startsWith(_19_ESPACES))
          {
            final int size = inLignes.size();
            final boolean empty = (inLignes.size() == 0);
            final int index = (empty ? 0 : (size - 1));
            final String complete = (empty ? STRING_VIDE : inLignes.get(index)) + ligne;
            if (empty)
            {
              inLignes.add(complete);
            }
            else
            {
              inLignes.set(index, complete);
            }
          }
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

  @Override
  public Releve newReleve()
  {
    return new Releve();
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
    return true;
  }

  @Override
  public Collection<Releve> getHistoriqueBalise(final String baliseId, final int duree, final int peremption) throws IOException
  {
    // Date de debut
    final long startTs = System.currentTimeMillis() - (duree * 86400000) - (peremption * 60000);
    final Date startDate = new Date(startTs);
    Utils.toUTC(startDate);

    // Date de fin : maintenant
    final Date endDate = new Date();
    Utils.toUTC(endDate);

    // Champs debut
    final String startYear = HISTO_ANNEE_FORMAT.format(startDate);
    final String startMonth = HISTO_MOIS_FORMAT.format(startDate);
    final String startDay = HISTO_JOUR_FORMAT.format(startDate);
    final String startHour = HISTO_HEURE_FORMAT.format(startDate);

    // Champs fin
    final String endYear = HISTO_ANNEE_FORMAT.format(endDate);
    final String endMonth = HISTO_MOIS_FORMAT.format(endDate);
    final String endDay = HISTO_JOUR_FORMAT.format(endDate);
    final String endHour = HISTO_HEURE_FORMAT.format(endDate);

    // Elaboration URL
    final String finalUrl = URL_HISTO.replaceAll(URL_STATION_KEY_GROUP, baliseId).replaceAll(URL_ANNEE_DEB_KEY_GROUP, startYear).replaceAll(URL_ANNEE_FIN_KEY_GROUP, endYear).replaceAll(URL_MOIS_DEB_KEY_GROUP, startMonth)
        .replaceAll(URL_MOIS_FIN_KEY_GROUP, endMonth).replaceAll(URL_JOUR_DEB_KEY_GROUP, startDay).replaceAll(URL_JOUR_FIN_KEY_GROUP, endDay).replaceAll(URL_HEURE_DEB_KEY_GROUP, startHour).replaceAll(URL_HEURE_FIN_KEY_GROUP, endHour);

    // Lecture donnees
    final List<String> lignesHisto = new ArrayList<String>();
    getLignesReleves(finalUrl, lignesHisto, true);

    // Analyse donnees
    final List<Releve> releves = new ArrayList<Releve>();
    Releve prevReleve = null;
    for (final String ligne : lignesHisto)
    {
      final Matcher matcher = OGIMET_SYNOP_PATTERN.matcher(ligne);
      if (matcher.matches())
      {
        try
        {
          //  Analyse date
          final Date date = OGIMET_DATE_FORMAT.parse(matcher.group(1));

          // Analyse SYNOP
          final boolean releveOk = parseSynop(matcher.group(2), date);
          if (releveOk)
          {
            // Calcul tendances
            if (prevReleve != null)
            {
              // Date
              releve.dateRelevePrecedent = prevReleve.date;

              // Vent moyen
              if (!Double.isNaN(releve.ventMoyen) && !Double.isNaN(prevReleve.ventMoyen))
              {
                releve.ventMoyenTendance = releve.ventMoyen - prevReleve.ventMoyen;
              }
            }

            // Ajout releve
            final Releve nouveau = newReleve();
            nouveau.copyFrom(releve);
            releves.add(nouveau);

            // Next
            prevReleve = nouveau;
          }
        }
        catch (final ParseException pe)
        {
          System.err.println("Analyse date SYNOP impossible : '" + ligne + "' (" + pe.getMessage() + ")");
          //pe.printStackTrace(System.err);
        }
      }
    }

    // Fin
    return releves;
  }

  @Override
  public Balise newBalise()
  {
    return new Balise();
  }

  @Override
  public boolean updateBalisesUpdateDate() throws IOException
  {
    return true;
  }

  @Override
  public long getBalisesUpdateDate() throws IOException
  {
    return Utils.toUTC(System.currentTimeMillis());
  }

  @Override
  public boolean updateBalises() throws IOException
  {
    // Analyse
    readAndParseBalises();

    // Tout est OK
    setBalisesMap(newBalises);

    return true;
  }

  /**
   * 
   * @throws IOException
   */
  private void readAndParseBalises() throws IOException
  {
    // Date deb : maintenant moins 1 jour pour etre a peu pres sur d'avoir des donnees
    final Date dateDeb = new Date(System.currentTimeMillis() - (1 * 24 * 3600 * 1000));
    Utils.toUTC(dateDeb);

    // Champs deb
    final String yearDeb = HISTO_ANNEE_FORMAT.format(dateDeb);
    final String monthDeb = HISTO_MOIS_FORMAT.format(dateDeb);
    final String dayDeb = HISTO_JOUR_FORMAT.format(dateDeb);
    final String hourDeb = HISTO_HEURE_FORMAT.format(dateDeb);

    // Date fin : maintenant
    final Date dateFin = new Date(System.currentTimeMillis());
    Utils.toUTC(dateFin);

    // Champs fin
    final String yearFin = HISTO_ANNEE_FORMAT.format(dateFin);
    final String monthFin = HISTO_MOIS_FORMAT.format(dateFin);
    final String dayFin = HISTO_JOUR_FORMAT.format(dateFin);
    final String hourFin = HISTO_HEURE_FORMAT.format(dateFin);

    // Elaboration URL
    final String ogimetCountry = PAYS_CODES.get(country.toUpperCase());
    final String finalUrl = URL_BALISES.replaceAll(URL_PAYS_KEY_GROUP, ogimetCountry).replaceAll(URL_ANNEE_DEB_KEY_GROUP, yearDeb).replaceAll(URL_ANNEE_FIN_KEY_GROUP, yearFin).replaceAll(URL_MOIS_DEB_KEY_GROUP, monthDeb)
        .replaceAll(URL_MOIS_FIN_KEY_GROUP, monthFin).replaceAll(URL_JOUR_DEB_KEY_GROUP, dayDeb).replaceAll(URL_JOUR_FIN_KEY_GROUP, dayFin).replaceAll(URL_HEURE_DEB_KEY_GROUP, hourDeb).replaceAll(URL_HEURE_FIN_KEY_GROUP, hourFin);

    // Lecture des donnees
    final StringBuilder buffer = new StringBuilder(Utils.READ_BUFFER_SIZE + 10);
    Utils.readData(new URL(finalUrl), UTF_8, buffer);

    // Initialisations
    BufferedReader reader = null;
    newBalises.clear();

    try
    {
      // Initialisations
      reader = new BufferedReader(new StringReader(buffer.toString()));
      lignesBalises.clear();

      String ligne = reader.readLine();
      while (ligne != null)
      {
        // Filtrage
        final Balise balise;
        final Matcher matcherDegMin = balisePatternDegMin.matcher(ligne);
        if (matcherDegMin.matches())
        {
          balise = parseBalise(ligne, matcherDegMin, false);
        }
        else
        {
          final Matcher matcherDegMinSec = balisePatternDegMinSec.matcher(ligne);
          if (matcherDegMinSec.matches())
          {
            balise = parseBalise(ligne, matcherDegMinSec, true);
          }
          else
          {
            balise = null;
          }
        }

        // Ajout balise
        if (balise != null)
        {
          newBalises.put(balise.id, balise);
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
   * @param line
   * @param matcher
   * @param hasSecCoords
   * @return
   */
  private static Balise parseBalise(final String line, final Matcher matcher, final boolean hasSecCoords)
  {
    // OK !
    final Balise balise = new Balise();
    balise.active = Utils.BOOLEAN_TRUE;
    int group = 1;

    // ID
    balise.id = matcher.group(group++);

    // Nom
    balise.nom = matcher.group(group++);

    // Latitude
    try
    {
      final String latitudeDegString = matcher.group(group++);
      final String latitudeMinString = matcher.group(group++);
      final String latitudeSecString = (hasSecCoords ? matcher.group(group++) : null);
      final String latitudeSign = matcher.group(group++);
      final double degres = Double.parseDouble(latitudeDegString);
      final double minutes = Double.parseDouble(latitudeMinString);
      final double secondes = (hasSecCoords ? Double.parseDouble(latitudeSecString) : 0);
      final int signe = (LATITUDE_NORD.equalsIgnoreCase(latitudeSign) ? 1 : -1);
      balise.latitude = signe * (degres + minutes / 60 + secondes / 3600);
    }
    catch (final NumberFormatException nfe)
    {
      System.err.println("Invalid latitude for line : " + line);
      return null;
    }

    // Longitude
    try
    {
      final String longitudeDegString = matcher.group(group++);
      final String longitudeMinString = matcher.group(group++);
      final String longitudeSecString = (hasSecCoords ? matcher.group(group++) : null);
      final String longitudeSign = matcher.group(group++);
      final double degres = Double.parseDouble(longitudeDegString);
      final double minutes = Double.parseDouble(longitudeMinString);
      final double secondes = (hasSecCoords ? Double.parseDouble(longitudeSecString) : 0);
      final int signe = (LONGITUDE_EST.equalsIgnoreCase(longitudeSign) ? 1 : -1);
      balise.longitude = signe * (degres + minutes / 60 + secondes / 3600);
    }
    catch (final NumberFormatException nfe)
    {
      System.err.println("Invalid longitude for line : " + line);
      return null;
    }

    // Altitude
    try
    {
      final String altitudeString = matcher.group(group++);
      balise.altitude = Integer.parseInt(altitudeString, group++);
    }
    catch (final NumberFormatException nfe)
    {
      System.err.println("Invalid altitude for line : " + line);
    }

    return balise;
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
      final OgimetSynopProvider provider = new OgimetSynopProvider("synop", "fr", null);

      // Balises
      //final boolean updated = provider.updateBalises();
      //System.out.println("updated : " + updated);
      //System.out.println("balises : " + provider.getBalises());

      // Releves
      final boolean updated = provider.updateReleves();
      System.out.println("updated : " + updated);
      System.out.println("releves : " + provider.getReleves());

      // Historique
      //final Collection<Releve> releves = provider.getHistoriqueBalise("07002", 2, 30);
      //System.out.println("releves : " + releves);
    }
    catch (Throwable th)
    {
      th.printStackTrace();
    }
  }
  */
}
