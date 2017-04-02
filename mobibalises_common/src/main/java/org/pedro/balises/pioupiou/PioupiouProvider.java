package org.pedro.balises.pioupiou;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pedro.balises.AbstractBaliseProvider;
import org.pedro.balises.Balise;
import org.pedro.balises.HistoryBaliseProvider;
import org.pedro.balises.MobibalisesHistoryBaliseProviderHelper;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;

import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public class PioupiouProvider extends AbstractBaliseProvider implements HistoryBaliseProvider
{
  //private static final String       URL_BALISES = "file:C:/pmu/perso/_workspaces/mobibalises/vps01_web/data.mobibalises.net/htdocs/pioupiou/balises.json";
  //private static final String       URL_BALISES = "http://api.pioupiou.fr/v1/live-with-meta/all";
  private static final String       URL_BALISES              = "http://data.mobibalises.net/pioupiou/balises.mb.json";

  //private static final String                URL_RELEVES             = "file:C:/Temp/relevemeteo-20110721.xml";
  //private static final String     URL_RELEVES = "http://api.pioupiou.fr/v1/live/all";
  private static final String       URL_RELEVES              = "http://data.mobibalises.net/pioupiou/releves.mb.json";

  private static final String       URL_BALISE_ID_KEY        = "baliseId";
  private static final String       URL_BALISE_ID_KEY_GROUP  = "\\{" + URL_BALISE_ID_KEY + "\\}";
  private static final String       URL_DUREE_KEY            = "duree";
  private static final String       URL_DUREE_KEY_GROUP      = "\\{" + URL_DUREE_KEY + "\\}";
  private static final String       URL_PEREMPTION_KEY       = "peremption";
  private static final String       URL_PEREMPTION_KEY_GROUP = "\\{" + URL_PEREMPTION_KEY + "\\}";

  //private static final String URL_HISTO                   = "file:D:/download/ffvl-histo-{" + URL_BALISE_ID_KEY + "}.gz";
  private static final String       URL_HISTO                = "http://data.mobibalises.net/pioupiou/pioupiou-histo.php?baliseId={" + URL_BALISE_ID_KEY + "}&duree={" + URL_DUREE_KEY + "}&peremption={" + URL_PEREMPTION_KEY + "}";

  private static final List<String> COUNTRIES                = new ArrayList<String>();

  private static final DateFormat   UTC_FORMAT               = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

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
   */
  public PioupiouProvider(final String name, final String country)
  {
    // Initialisation
    super(name, country, null, 150);
  }

  @Override
  public boolean isAvailable()
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
    // Initialisations
    final StringBuilder buffer = new StringBuilder(Utils.READ_BUFFER_SIZE + 10);

    // Lecture
    Utils.readData(new URL(URL_BALISES), "UTF-8", buffer);
    final Map<String, Balise> newBalises = parseBalisesMap(buffer.toString());

    // Tout est OK
    setBalisesMap(newBalises);

    return true;
  }

  /**
   * 
   * @param source
   * @return
   * @throws IOException
   */
  private Map<String, Balise> parseBalisesMap(final String source) throws IOException
  {
    try
    {
      // Initialisations
      final Map<String, Balise> map = new HashMap<String, Balise>();
      final JSONObject json = new JSONObject(source);
      final JSONArray data = json.getJSONArray("data");

      // Pour chaque balise
      final int length = data.length();
      for (int i = 0; i < length; i++)
      {
        // Initialisations
        final JSONObject jsonBalise = (JSONObject)data.get(i);
        final JSONObject location = jsonBalise.getJSONObject("location");

        // Vérification de la localisation
        if (location.isNull("date")) {
          continue;
        }

        // Analyse
        final Balise balise = newBalise();
        balise.id = jsonBalise.getString("id");
        final JSONObject meta = jsonBalise.getJSONObject("meta");
        final JSONObject status = jsonBalise.getJSONObject("status");
        balise.nom = meta.getString("name");
        balise.description = meta.getString("description");
        balise.latitude = location.getDouble("latitude");
        balise.longitude = location.getDouble("longitude");
        balise.altitude = location.optInt("altitude", Integer.MIN_VALUE);
        balise.active = 1;

        // Ajout
        map.put(balise.id, balise);
        Log.d(this.getClass().getSimpleName(), "balise analysée : " + balise);
      }

      return map;
    }
    catch (final JSONException jse)
    {
      final IOException ioe = new IOException(jse.getMessage());
      ioe.setStackTrace(jse.getStackTrace());
      throw ioe;
    }
  }

  @Override
  public String getBaliseDetailUrl(final String id)
  {
    return "http://www.pioupiou.fr/" + id;
  }

  @Override
  public String getBaliseHistoriqueUrl(final String id)
  {
    return null;
  }

  @Override
  public boolean updateRelevesUpdateDate() throws IOException
  {
    return true;
  }

  @Override
  public long getRelevesUpdateDate() throws IOException
  {
    return Utils.toUTC(System.currentTimeMillis());
  }

  @Override
  public boolean updateReleves() throws IOException
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder(Utils.READ_BUFFER_SIZE + 10);
    updatedReleves.clear();

    // Lecture
    Utils.readData(new URL(URL_RELEVES), "UTF-8", buffer);
    parseReleves(buffer.toString());

    return !updatedReleves.isEmpty();
  }

  /**
   *
   * @param source
   * @throws IOException
   */
  protected void parseReleves(final String source) throws IOException
  {
    try
    {
      // Initialisations
      final JSONObject json = new JSONObject(source);
      final JSONArray data = json.getJSONArray("data");

      // Pour chaque relevé
      final int length = data.length();
      for (int i = 0; i < length; i++)
      {
        // Initialisations
        final JSONObject jsonBalise = (JSONObject)data.get(i);
        //final JSONObject status = jsonBalise.getJSONObject("status");

        // Vérification de la localisation
        //if (!"on".equalsIgnoreCase(status.getString("state")))
        //{
        //  continue;
        //}

        // Analyse
        final Releve releve = new Releve();
        final JSONObject measurements = jsonBalise.getJSONObject("measurements");
        releve.id = jsonBalise.getString("id");
        if (measurements.isNull("date")) {
          continue;
        }
        final String dateString = measurements.getString("date").replaceAll("Z", "+0000");
        releve.date = UTC_FORMAT.parse(dateString);
        Utils.toUTC(releve.date);
        releve.pression = measurements.optDouble("pressure", Double.NaN);
        releve.directionMoyenne = (int)Math.round(measurements.optDouble("wind_heading", Double.NaN));
        releve.ventMoyen = measurements.optDouble("wind_speed_avg", Double.NaN);
        releve.ventMaxi = measurements.optDouble("wind_speed_max", Double.NaN);
        releve.ventMini = measurements.optDouble("wind_speed_min", Double.NaN);
        final long datePrec = measurements.optLong("date_prec", Long.MIN_VALUE);
        releve.dateRelevePrecedent = (datePrec != Long.MIN_VALUE ? new Date(datePrec * 1000) : null);
        if (releve.dateRelevePrecedent != null)
        {
          Utils.toUTC(releve.dateRelevePrecedent);
        }
        releve.ventMoyenTendance = measurements.optDouble("tvmoy", Double.NaN);
        releve.ventMaxiTendance = measurements.optDouble("tvmax", Double.NaN);
        releve.ventMiniTendance = measurements.optDouble("tvmin", Double.NaN);

        // Ajout
        Log.d(this.getClass().getSimpleName(), "relevé analysé : " + releve);
        onReleveParsed(releve);
      }
    }
    catch (final JSONException jse)
    {
      final IOException ioe = new IOException(jse.getMessage());
      ioe.setStackTrace(jse.getStackTrace());
      throw ioe;
    }
    catch (final ParseException pe)
    {
      final IOException ioe = new IOException(pe.getMessage());
      ioe.setStackTrace(pe.getStackTrace());
      throw ioe;
    }
  }

  @Override
  public Balise newBalise()
  {
    return new Balise();
  }

  @Override
  public Releve newReleve()
  {
    return new Releve();
  }

  @Override
  public String filterExceptionMessage(final String message)
  {
    return message;
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

  @Override
  public Collection<Releve> getHistoriqueBalise(String baliseId, int duree, int peremption) throws IOException
  {
    // Elaboration URL
    final String finalUrl = URL_HISTO.replaceAll(URL_BALISE_ID_KEY_GROUP, baliseId).replaceAll(URL_DUREE_KEY_GROUP, "" + duree).replaceAll(URL_PEREMPTION_KEY_GROUP, "" + peremption);

    // Lecture donnees
    final List<Releve> releves = new ArrayList<Releve>();
    BufferedInputStream bis = null;
    GZIPInputStream gzis = null;
    InputStreamReader isr = null;
    BufferedReader br = null;
    try
    {
      // Initialisations
      bis = new BufferedInputStream(new URL(finalUrl).openStream());
      gzis = new GZIPInputStream(bis);
      isr = new InputStreamReader(gzis);
      br = new BufferedReader(isr);

      // Lecture
      String line = br.readLine();
      while ((line != null) && (line.trim().length() > 0))
      {
        // Analyse
        final Releve releve = MobibalisesHistoryBaliseProviderHelper.parseLigneReleve(line, baliseId);
        if (releve != null)
        {
          releves.add(releve);
        }

        // Next
        line = br.readLine();
      }
    }
    finally
    {
      if (br != null)
      {
        br.close();
      }
      if (isr != null)
      {
        isr.close();
      }
      if (gzis != null)
      {
        gzis.close();
      }
      if (bis != null)
      {
        bis.close();
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
      PioupiouProvider provider = new PioupiouProvider("pioupiou", "FR");

      boolean balisesUpdateDateUpdated = provider.updateBalisesUpdateDate();
      System.out.println("balisesUpdateDateUpdated : " + balisesUpdateDateUpdated);
      System.out.println("MAJ Balises : " + new Date(provider.getBalisesUpdateDate()));

      boolean relevesUpdateDateUpdated = provider.updateRelevesUpdateDate();
      System.out.println("relevesUpdateDateUpdated : " + relevesUpdateDateUpdated);
      System.out.println("MAJ Releves : " + new Date(provider.getRelevesUpdateDate()));

      provider.updateBalises();
      //provider.updateReleves();
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
  }
  */
}
