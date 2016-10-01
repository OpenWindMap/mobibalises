package org.pedro.balises.metar;

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

import org.pedro.balises.HistoryBaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;

/**
 * 
 * @author pedro.m
 */
public final class OgimetMetarProvider extends AviationWeatherListMetarProvider implements HistoryBaliseProvider
{
  // Environ la taille des donnees pour la france...
  private static final int                 LINES_BUFFER_SIZE       = 4 * 1024;

  private static final String              UTF_8                   = "UTF-8";

  private static final String              STRING_VIDE             = "";
  private static final String              _14_ESPACES             = "              ";
  private static final String              CARACTERE_EGAL          = "=";
  private static final String              CARACTERE_DEBUT_HTML    = "<";
  private static final String              CARACTERE_COMMENTAIRE   = "#";
  private static final char                CARACTERE_PIPE          = '|';

  private static final Pattern             OGIMET_METAR_PATTERN    = Pattern.compile("^(\\d{12})\\s(.*)$");

  private static final String              URL_PAYS_KEY            = "paysKey";
  private static final String              URL_PAYS_KEY_GROUP      = "\\{" + URL_PAYS_KEY + "\\}";
  //private static final String              URL_RELEVES           = "file:D:/_workspaces/perso/mobibalises/balises/res/ultimos_metars.txt";
  private static final String              URL_RELEVES             = "http://www.ogimet.com/ultimos_metars.php?lang=en&estado={" + URL_PAYS_KEY + "}&fmt=txt&iord=no&Send=Send";

  private static final DateFormat          OGIMET_DATE_FORMAT      = new SimpleDateFormat("yyyyMMddHHmm");
  private static final DateFormat          HISTO_ANNEE_FORMAT      = new SimpleDateFormat("yyyy");
  private static final DateFormat          HISTO_MOIS_FORMAT       = new SimpleDateFormat("MM");
  private static final DateFormat          HISTO_JOUR_FORMAT       = new SimpleDateFormat("dd");
  private static final DateFormat          HISTO_HEURE_FORMAT      = new SimpleDateFormat("HH");
  private static final String              URL_STATION_KEY         = "stationKey";
  private static final String              URL_STATION_KEY_GROUP   = "\\{" + URL_STATION_KEY + "\\}";
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
  //private static final String              URL_HISTO               = "file:D:/download/histo_metars.htm";
  private static final String              URL_HISTO               = "http://www.ogimet.com/display_metars.php?lang=en&lugar={" + URL_STATION_KEY + "}&tipo=SA&ord=DIR&nil=NO&fmt=txt&ano={" + URL_ANNEE_DEB_KEY + "}&mes={" + URL_MOIS_DEB_KEY
                                                                       + "}&day={" + URL_JOUR_DEB_KEY + "}&hora={" + URL_HEURE_DEB_KEY + "}&anof={" + URL_ANNEE_FIN_KEY + "}&mesf={" + URL_MOIS_FIN_KEY + "}&dayf={" + URL_JOUR_FIN_KEY
                                                                       + "}&horaf={" + URL_HEURE_FIN_KEY + "}&minf=59&send=send";

  private static final Map<String, String> PAYS                    = new HashMap<String, String>();

  private static final List<String>        COUNTRIES               = new ArrayList<String>();

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
      final String label = value.substring(0, value.indexOf(CARACTERE_PIPE));
      addCountry(key, label);
    }
  }

  /**
   * 
   * @param country
   * @param ogimetCode
   */
  private static void addCountry(final String country, final String ogimetCode)
  {
    COUNTRIES.add(country);
    PAYS.put(country, ogimetCode);
  }

  /**
   * 
   * @param name
   * @param country
   * @param region
   */
  public OgimetMetarProvider(final String name, final String country, final String region)
  {
    super(name, country, region);
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
    if (PAYS.get(country) != null)
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
    return URL_RELEVES.replaceAll(URL_PAYS_KEY_GROUP, URLEncoder.encode(PAYS.get(getOgimetCountry()), "UTF-8"));
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
    parseMetars(lignes);

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
          final Matcher matcher = OGIMET_METAR_PATTERN.matcher(ligne);
          if (matcher.matches())
          {
            inLignes.add(matcher.group(includeDate ? 0 : 2));
          }
          else if (ligne.trim().endsWith(CARACTERE_EGAL) || ligne.startsWith(_14_ESPACES))
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
      final Matcher matcher = OGIMET_METAR_PATTERN.matcher(ligne);
      if (matcher.matches())
      {
        try
        {
          //  Analyse date
          final Date date = OGIMET_DATE_FORMAT.parse(matcher.group(1));

          // Analyse METAR
          final boolean releveOk = parseMetar(matcher.group(2), date);
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

              // Vent maxi
              if (!Double.isNaN(releve.ventMaxi) && !Double.isNaN(prevReleve.ventMaxi))
              {
                releve.ventMaxiTendance = releve.ventMaxi - prevReleve.ventMaxi;
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
          System.err.println("Analyse date METAR impossible : '" + ligne + "' (" + pe.getMessage() + ")");
          //pe.printStackTrace(System.err);
        }
      }
    }

    // Fin
    return releves;
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
      final OgimetMetarProvider provider = new OgimetMetarProvider("metar", "fr", null);

      final Collection<Releve> releves = provider.getHistoriqueBalise("LFLY", 1, 30);
      System.out.println("releves : " + releves);
    }
    catch (Throwable th)
    {
      th.printStackTrace();
    }
  }
  */
}
