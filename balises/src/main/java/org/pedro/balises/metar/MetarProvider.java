package org.pedro.balises.metar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.pedro.balises.AbstractBaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;

/**
 * 
 * @author pedro.m
 */
@Deprecated
public abstract class MetarProvider extends AbstractBaliseProvider
{
  private static final String     CHAINE_EGAL                 = "=";
  private static final String     SEPARATEUR_TEMPERATURE      = "/";
  private static final Pattern    PATTERN_TEMPERATURE         = Pattern.compile("[M]{0,1}\\d+/[M]{0,1}\\d+");
  private static final String     CHAMP_AUTO                  = "AUTO";
  private static final String     REGEXP_SEPARATEURS          = "\\s+";
  private static final String     CHAMP_COR                   = "COR";
  private static final String     SPACE                       = " ";
  private static final DateFormat DATE_FORMAT                 = new SimpleDateFormat("ddHHmm'Z'");
  private static final String     VENT_VRB                    = "VRB";
  private static final String     INDICATEUR_RAFALE           = "G";
  private static final String     UNITE_VENT_NOEUD            = "KT";
  private static final String     UNITE_VENT_KMH              = "KMH";
  private static final String     UNITE_VENT_MPS              = "MPS";
  private static final char       TEMPERATURE_MINUS           = 'M';
  private static final char       PRESSION_QNH                = 'Q';
  private static final char       PRESSION_ALTIMETER          = 'A';
  private static final double     FACTEUR_PRESSION_POUCES_HPA = 0.01 * 2.54 * 10;

  /**
   * 
   * @param name
   * @param country
   */
  public MetarProvider(final String name, final String country)
  {
    super(name, country, null, 150);
  }

  /**
   * 
   * @param textMetars
   * @param newReleves
   */
  protected static void parseMetars(final List<String> textMetars, final Map<String, Releve> newReleves)
  {
    // Analyse
    for (final String metar : textMetars)
    {
      final Releve releve = parseMetar(metar, null);
      if (releve != null)
      {
        newReleves.put(releve.id, releve);
      }
    }
  }

  /**
   * 
   * @param metar
   * @param inDate
   * @return
   */
  private static Releve parseMetar(final String metar, final Date inDate)
  {
    // Initialisations
    Releve releve = null;

    // Mise en forme
    final String finalMetar = metar.replaceAll(REGEXP_SEPARATEURS, SPACE);
    //System.out.println("======================================= '" + finalMetar + "'");

    // Champs
    final String[] champs = finalMetar.split(SPACE);

    // Analyse des champs
    if ((champs.length >= 7) && (champs.length <= 15))
    {
      try
      {
        // Initialisations
        releve = new Releve();
        int indice = 0;

        // En-tete
        indice++;

        // Indicatif
        while (CHAMP_COR.equals(champs[indice]))
        {
          indice++;
        }
        final String indicatif = champs[indice];
        if (Utils.isStringVide(indicatif))
        {
          return null;
        }
        releve.setId(indicatif);
        //System.out.println("indicatif : " + indicatif);

        // Date
        if (inDate == null)
        {
          final String stringDate = champs[++indice];
          final Date date = DATE_FORMAT.parse(stringDate);
          final Calendar cal = Calendar.getInstance();
          final int year = cal.get(Calendar.YEAR);
          final int month = cal.get(Calendar.MONTH);
          cal.setTime(date);
          cal.set(Calendar.YEAR, year);
          cal.set(Calendar.MONTH, month);
          releve.date = cal.getTime();
          //System.out.println("date : " + stringDate + " => " + releve.date);
        }
        else
        {
          releve.date = inDate;
        }

        // Vent
        indice++;
        while (CHAMP_AUTO.equals(champs[indice]))
        {
          indice++;
        }
        final String vent = champs[indice];
        //System.out.println("vent : '" + vent + "'");

        // Direction du vent
        final String direction = vent.substring(0, 3);
        //System.out.println("direction : '" + direction + "'");
        if (!VENT_VRB.equalsIgnoreCase(direction))
        {
          releve.directionMoyenne = Integer.parseInt(direction, 10);
        }

        // Vitesse moyenne
        final String vitesseMoy = vent.substring(3, 5);
        //System.out.println("vitesseMoy : '" + vitesseMoy + "'");
        releve.ventMoyen = Double.parseDouble(vitesseMoy);

        // Vitesse rafale (maxi)
        int indiceUniteVitesseDebut = 5;
        if (INDICATEUR_RAFALE.equalsIgnoreCase(vent.substring(5, 6)))
        {
          final String vitesseMax = vent.substring(6, 8);
          //System.out.println("vitesseMax : '" + vitesseMax + "'");
          releve.ventMaxi = Double.parseDouble(vitesseMax);

          // Suite
          indiceUniteVitesseDebut += 3;
        }

        // Unite vent
        final String uniteVitesseVent = vent.substring(indiceUniteVitesseDebut);
        //System.out.println("uniteVitesseVent : '" + uniteVitesseVent + "'");
        float facteur = 0;
        boolean convertVitesseVent = false;
        if (UNITE_VENT_MPS.equalsIgnoreCase(uniteVitesseVent))
        {
          facteur = 3.6f;
          convertVitesseVent = true;
        }
        else if (UNITE_VENT_NOEUD.equalsIgnoreCase(uniteVitesseVent))
        {
          facteur = 1.852f;
          convertVitesseVent = true;
        }
        else if (UNITE_VENT_KMH.equalsIgnoreCase(uniteVitesseVent))
        {
          // Nothing
        }
        if (convertVitesseVent)
        {
          if (!Double.isNaN(releve.ventMoyen))
          {
            releve.ventMoyen *= facteur;
          }
          if (!Double.isNaN(releve.ventMaxi))
          {
            releve.ventMaxi *= facteur;
          }
        }

        // Temperature
        boolean isTemperatures = false;
        while (!isTemperatures)
        {
          isTemperatures = PATTERN_TEMPERATURE.matcher(champs[++indice]).matches();
        }
        final String temperatures = champs[indice];
        //System.out.println("temperatures : '" + temperatures + "'");
        String temperature = temperatures.split(SEPARATEUR_TEMPERATURE)[0];
        //System.out.println("temperature : '" + temperature + "'");
        int signe = 1;
        if (temperature.charAt(0) == TEMPERATURE_MINUS)
        {
          signe = -1;
          temperature = temperature.substring(1);
          //System.out.println("temperature : '" + temperature + "'");
        }
        releve.temperature = signe * Double.parseDouble(temperature);

        // Pression
        boolean isPression = false;
        char first = 0;
        while (!isPression)
        {
          first = champs[++indice].charAt(0);
          isPression = (first == PRESSION_QNH) || (first == PRESSION_ALTIMETER);
        }
        final String pressions = champs[indice];
        String pression = pressions.substring(1);
        if (pression.endsWith(CHAINE_EGAL))
        {
          pression = pression.substring(0, pression.length() - 1);
        }
        //System.out.println("pressions : '" + pressions + "'");
        if (first == PRESSION_QNH)
        {
          releve.pression = Double.parseDouble(pression);
        }
        else if (first == PRESSION_ALTIMETER)
        {
          final double pouces = Double.parseDouble(pression);
          releve.pression = pouces * FACTEUR_PRESSION_POUCES_HPA;
        }
      }
      catch (final Throwable th)
      {
        System.err.println("Error for METAR : '" + metar + "'");
        th.printStackTrace(System.err);
        return null;
      }
    }

    return releve;
  }

  @Override
  public String getBaliseDetailUrl(final String id)
  {
    return null;
  }

  @Override
  public String getBaliseHistoriqueUrl(final String id)
  {
    return null;
  }

  @Override
  public int getDefaultDeltaReleves()
  {
    return 30;
  }
}
