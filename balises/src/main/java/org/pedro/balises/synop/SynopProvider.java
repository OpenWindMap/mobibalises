package org.pedro.balises.synop;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pedro.balises.AbstractBaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;

/**
 * 
 * @author pedro.m
 */
public abstract class SynopProvider extends AbstractBaliseProvider
{

  private static final String  STRING_MESSAGE_SYNOP_INCORRECT = "SYNOP incorrect : ";

  private static final String  CHAMP_SYNOP                    = "AAXX";
  private static final String  CHAMP_333                      = "333";
  private static final String  HYDROMETRIE_990                = "990";
  private static final String  STRING_SLASH                   = "/";

  private static final Pattern PATTERN_INCONNU                = Pattern.compile("/+");
  private static final Pattern PATTERN_DATE_HEURE_TYPE_VENT   = Pattern.compile("(\\d{2})(\\d{2})(\\d)");
  private static final Pattern PATTERN_INDICATIF              = Pattern.compile("\\d{5}");
  private static final Pattern PATTERN_PLAFOND_VISIBILITE     = Pattern.compile("\\d\\d([\\d/])([\\d/]{2})");
  private static final Pattern PATTERN_NUAGES_VENT            = Pattern.compile("([\\d/])(\\d{2})(\\d{2})");
  private static final Pattern PATTERN_VENT_FORT              = Pattern.compile("00(\\d{3})");
  private static final Pattern PATTERN_TEMPERATURE            = Pattern.compile("1([01])(\\d{3})");
  private static final Pattern PATTERN_POINT_ROSEE            = Pattern.compile("2([019])(\\d{3})");
  private static final Pattern PATTERN_PRESSION               = Pattern.compile("3((\\d{3})([\\d/]))");
  private static final Pattern PATTERN_PLUIE                  = Pattern.compile("6(\\d{3})([\\d/])");

  private static final int     TYPE_VENT_MS_ESTIMATED         = 0;
  private static final int     TYPE_VENT_MS_ANEMOMETRE        = 1;
  private static final int     TYPE_VENT_KN_ESTIMATED         = 2;
  private static final int     TYPE_VENT_KN_ANEMOMETRE        = 3;
  private static final int     TYPE_VENT_KN_ANEMOMETRE_4      = 4;

  private static final int[]   PLAFONDS                       = new int[] { 25, 75, 150, 250, 450, 800, 1250, 1750, 2250, 2500 };

  private static final String  REGEXP_SEPARATEURS             = "\\s+";
  private static final String  REGEXP_FIN_LIGNE               = "^([^=]+)(=?)$";
  private static final String  REMPLACEMENT_FIN_LIGNE         = "$1 $2";
  private static final String  SPACE                          = " ";

  protected final Releve       releve                         = new Releve();
  protected int                typeVent;

  /**
   * 
   * @param name
   * @param country
   * @param region
   */
  public SynopProvider(final String name, final String country, final String region)
  {
    super(name, country, region, 150);
  }

  /**
   * 
   * @param textSynops
   */
  protected void parseSynops(final List<String> textSynops)
  {
    // Analyse
    for (final String synop : textSynops)
    {
      final boolean ok = parseSynop(synop, null);
      if (ok)
      {
        onReleveParsed(releve);
      }
    }
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   */
  @SuppressWarnings("unused")
  private static boolean checkChamp(final String synop, final String[] champs, final int indice)
  {
    // Nombre de champs
    if (indice >= champs.length)
    {
      return false;
    }

    return true;
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   */
  @SuppressWarnings("unused")
  private static boolean checkInconnu(final String synop, final String[] champs, final int indice)
  {
    if (PATTERN_INCONNU.matcher(champs[indice]).matches())
    {
      return false;
    }

    return true;
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private static int parseChampSynop(final String synop, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(synop, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_SYNOP_INCORRECT + synop, indice);
    }

    if (!CHAMP_SYNOP.equals(champs[indice]))
    {
      return 0;
    }

    return 1;
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @param releve
   * @return
   * @throws ParseException
   */
  private static int parseChampIndicatif(final String synop, final String[] champs, final int indice, final Releve releve) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(synop, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_SYNOP_INCORRECT + synop, indice);
    }

    // Format indicatif
    final Matcher matcher = PATTERN_INDICATIF.matcher(champs[indice]);
    if (!matcher.matches())
    {
      throw new ParseException("INDICATIF incorrect : " + synop, indice);
    }

    // OK
    releve.setId(champs[indice]);
    return 1;
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @param inDate
   * @return
   * @throws ParseException
   */
  private int parseChampDateHeureTypeVent(final String synop, final String[] champs, final int indice, final Date inDate) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(synop, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_SYNOP_INCORRECT + synop, indice);
    }
    if (!checkInconnu(synop, champs, indice))
    {
      return 1;
    }

    // Verif
    final Matcher matcher = PATTERN_DATE_HEURE_TYPE_VENT.matcher(champs[indice]);
    if (!matcher.matches())
    {
      throw new ParseException("Date/heure/typeVent incorrect : " + champs[indice], 0);
    }

    // Date/heure
    if (inDate == null)
    {
      final int day = Integer.parseInt(matcher.group(1), 10);
      final int hours = Integer.parseInt(matcher.group(2), 10);

      // Now
      final Calendar now = Calendar.getInstance();
      now.setTimeInMillis(Utils.toUTC(now.getTimeInMillis()));
      final int nowMonth = now.get(Calendar.MONTH);
      final int nowYear = now.get(Calendar.YEAR);

      // Date du releve
      final Calendar cal = Calendar.getInstance();
      cal.set(Calendar.YEAR, nowYear);
      cal.set(Calendar.MONTH, nowMonth);
      cal.set(Calendar.DAY_OF_MONTH, day);
      cal.set(Calendar.HOUR_OF_DAY, hours);
      cal.set(Calendar.MINUTE, 0);
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);

      // Date du releve dans le futur ?
      final int month;
      final int year;
      if (cal.after(now))
      {
        // Oui
        if (nowMonth == 0)
        {
          // Decalage au mois precedent (decembre de l'annee precedente)
          month = 11;
          year = nowYear - 1;
        }
        else
        {
          // Decalage au mois precedent
          month = nowMonth - 1;
          year = nowYear;
        }
      }
      else
      {
        month = nowMonth;
        year = nowYear;
      }

      // Rereglage avec les decalages
      cal.set(Calendar.YEAR, year);
      cal.set(Calendar.MONTH, month);
      cal.set(Calendar.DAY_OF_MONTH, day);

      // Enregistrement dans le releve
      releve.date = cal.getTime();
    }
    else
    {
      releve.date = inDate;
    }

    // Type vent
    typeVent = Integer.parseInt(matcher.group(3), 10);

    return 1;
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private int parseChampPlafondVisibilite(final String synop, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(synop, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_SYNOP_INCORRECT + synop, indice);
    }
    if (!checkInconnu(synop, champs, indice))
    {
      return 1;
    }

    // Verif
    final Matcher matcher = PATTERN_PLAFOND_VISIBILITE.matcher(champs[indice]);
    if (!matcher.matches())
    {
      throw new ParseException("Plafond/visibilite incorrect : " + champs[indice], 0);
    }

    // Plafond
    final String plafString = matcher.group(1);
    if (STRING_SLASH.equals(plafString))
    {
      releve.plafondNuages = Integer.MIN_VALUE;
    }
    else
    {
      releve.plafondNuages = PLAFONDS[Integer.parseInt(plafString, 10)];
    }

    return 1;
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private int parseChampNuagesVent(final String synop, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(synop, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_SYNOP_INCORRECT + synop, indice);
    }
    if (!checkInconnu(synop, champs, indice))
    {
      return 1;
    }

    // Verif
    final Matcher matcher = PATTERN_NUAGES_VENT.matcher(champs[indice]);
    if (!matcher.matches())
    {
      throw new ParseException("Plafond/visibilite incorrect : " + champs[indice], 0);
    }

    // Nuages
    final String nuagesString = matcher.group(1);
    if (STRING_SLASH.equals(nuagesString))
    {
      releve.nuages = Integer.MIN_VALUE;
    }
    else
    {
      releve.nuages = Integer.parseInt(nuagesString, 10);
    }

    // Direction vent
    final String dirString = matcher.group(2);
    releve.directionMoyenne = 10 * Integer.parseInt(dirString, 10);

    // Vitesse vent
    final String vitString = matcher.group(3);
    releve.ventMoyen = getVent(Integer.parseInt(vitString, 10));

    return 1;
  }

  /**
   * 
   * @param value
   * @return
   */
  private double getVent(final int value)
  {
    switch (typeVent)
    {
      case TYPE_VENT_MS_ESTIMATED:
      case TYPE_VENT_MS_ANEMOMETRE:
        return value * 3.6;
      case TYPE_VENT_KN_ESTIMATED:
      case TYPE_VENT_KN_ANEMOMETRE:
      case TYPE_VENT_KN_ANEMOMETRE_4:
      default:
        return value * 1.852;
    }

  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private int parseChampVentFort(final String synop, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(synop, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_SYNOP_INCORRECT + synop, indice);
    }
    if (!checkInconnu(synop, champs, indice))
    {
      return 1;
    }

    // Verif
    final Matcher matcher = PATTERN_VENT_FORT.matcher(champs[indice]);
    if (!matcher.matches())
    {
      return 0;
    }

    // Vitesse vent
    final String vitString = matcher.group(1);
    releve.ventMoyen = getVent(Integer.parseInt(vitString, 10));

    return 1;
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private int parseChampTemperature(final String synop, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(synop, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_SYNOP_INCORRECT + synop, indice);
    }
    if (!checkInconnu(synop, champs, indice))
    {
      return 1;
    }

    // Verif
    final Matcher matcher = PATTERN_TEMPERATURE.matcher(champs[indice]);
    if (!matcher.matches())
    {
      return 0;
    }

    // Signe et valeur
    releve.temperature = parseTemperature(matcher);

    return 1;
  }

  /**
   * 
   * @param matcher
   * @return
   */
  private static double parseTemperature(final Matcher matcher)
  {
    // Signe
    final String sigString = matcher.group(1);
    final double signe = "0".equals(sigString) ? 1 : -1;

    // Valeur
    final String tmpString = matcher.group(2);
    return signe * Double.parseDouble(tmpString) * 0.1;
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private int parseChampPointRoseeHumidite(final String synop, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(synop, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_SYNOP_INCORRECT + synop, indice);
    }
    if (!checkInconnu(synop, champs, indice))
    {
      return 1;
    }

    // Verif
    final Matcher matcher = PATTERN_POINT_ROSEE.matcher(champs[indice]);
    if (!matcher.matches())
    {
      return 0;
    }

    // Signe
    final String sigString = matcher.group(1);

    // Si 9 => humidite
    if ("9".equals(sigString))
    {
      releve.humidite = Integer.parseInt(matcher.group(2), 10);
    }
    // Sinon point de rosee
    else
    {
      releve.pointRosee = parseTemperature(matcher);
    }

    return 1;
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private int parseChampPression(final String synop, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(synop, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_SYNOP_INCORRECT + synop, indice);
    }
    if (!checkInconnu(synop, champs, indice))
    {
      return 1;
    }

    // Verif
    final Matcher matcher = PATTERN_PRESSION.matcher(champs[indice]);
    if (!matcher.matches())
    {
      return 0;
    }

    // Pression
    final String champ1 = matcher.group(1);
    final String champ2 = matcher.group(2);
    final String champ3 = matcher.group(3);
    final double facteur = (STRING_SLASH.equals(champ3) ? 1 : 0.1);
    final String strValue = (STRING_SLASH.equals(champ3) ? champ2 : champ1);
    final double value = facteur * Double.parseDouble(strValue);
    releve.pression = (value > 500 ? value : 1000 + value);

    return 1;
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  @SuppressWarnings("unused")
  private static int moveAfter333(final String synop, final String[] champs, final int indice) throws ParseException
  {
    int i = indice;
    while ((i < champs.length) && !CHAMP_333.equals(champs[i]))
    {
      i++;
    }

    return (i < champs.length ? i + 1 : -1);
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  @SuppressWarnings("unused")
  private static int moveToPattern(final String synop, final String[] champs, final int indice, final Pattern pattern) throws ParseException
  {
    int i = indice;
    while ((i < champs.length) && !pattern.matcher(champs[i]).matches())
    {
      i++;
    }

    return (i < champs.length ? i : -1);
  }

  /**
   * 
   * @param synop
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private int parseChampPluie(final String synop, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(synop, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_SYNOP_INCORRECT + synop, indice);
    }
    if (!checkInconnu(synop, champs, indice))
    {
      return 1;
    }

    // Verif
    final Matcher matcher = PATTERN_PLUIE.matcher(champs[indice]);
    if (!matcher.matches())
    {
      return 0;
    }

    // Hydrometrie
    final String strHydro = matcher.group(1);
    releve.hydrometrie = Double.parseDouble(strHydro);
    if (HYDROMETRIE_990.equals(strHydro))
    {
      // Traces
      releve.hydrometrie = 0.01;
    }
    else if (releve.hydrometrie >= 991)
    {
      releve.hydrometrie = (releve.hydrometrie - 990) * 0.1;
    }

    // Duree
    final double duree;
    final String strDuree = matcher.group(2);
    if (STRING_SLASH.equals(strDuree))
    {
      duree = 24;
    }
    else
    {
      final int intDuree = Integer.parseInt(strDuree, 10);
      switch (intDuree)
      {
        case 1:
        case 2:
        case 3:
        case 4:
          duree = 3 * intDuree;
          break;
        case 5:
        case 6:
        case 7:
          duree = intDuree - 4;
          break;
        case 8:
          duree = 9;
          break;
        case 9:
          duree = 24;
          break;
        default:
          duree = 1;
          break;
      }
    }
    releve.hydrometrie = releve.hydrometrie / duree;

    // Calcul pluie
    releve.calculatePluieFromHydrometrie();

    return 1;
  }

  /**
   * 
   * @param synop
   * @param inDate
   * @return
   */
  protected boolean parseSynop(final String synop, final Date inDate)
  {
    // Mise en forme
    final String finalSynop = synop.replaceFirst(REGEXP_FIN_LIGNE, REMPLACEMENT_FIN_LIGNE).replaceAll(REGEXP_SEPARATEURS, SPACE);

    // Champs
    final String[] champs = finalSynop.trim().toUpperCase().split(SPACE);

    // Analyse des champs
    try
    {
      // Initialisations
      int indice = 0;

      // SYNOP
      int deltaIndice = parseChampSynop(finalSynop, champs, indice);
      if (deltaIndice == 0)
      {
        // Pas un SYNOP
        return false;
      }
      indice += deltaIndice;

      // Init
      releve.clear();

      // Date/heure
      indice += parseChampDateHeureTypeVent(finalSynop, champs, indice, inDate);

      // Indicatif
      indice += parseChampIndicatif(finalSynop, champs, indice, releve);

      // Plafond
      indice += parseChampPlafondVisibilite(finalSynop, champs, indice);

      // Nuages/vent
      indice += parseChampNuagesVent(finalSynop, champs, indice);

      // Vitesse vent si > 100
      indice += parseChampVentFort(finalSynop, champs, indice);

      // Temperature
      indice += parseChampTemperature(finalSynop, champs, indice);

      // Point de rosee
      indice += parseChampPointRoseeHumidite(finalSynop, champs, indice);

      // Pression
      indice += parseChampPression(finalSynop, champs, indice);

      // Groupe climatologie ("333")
      indice = moveAfter333(finalSynop, champs, indice);
      if (indice == -1)
      {
        return true;
      }

      // Pluie
      indice = moveToPattern(finalSynop, champs, indice, PATTERN_PLUIE);
      if (indice == -1)
      {
        return true;
      }
      indice += parseChampPluie(finalSynop, champs, indice);

      // La fin : peu importe
    }
    catch (final Throwable th)
    {
      System.err.println("Analyse SYNOP impossible : '" + finalSynop + "' (" + th.getMessage() + ")");
      //th.printStackTrace(System.err);
      return false;
    }

    return true;
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
    return 60;
  }

  /**
   * 
   * @param args
  public static void main(final String[] args)
  {
    try
    {
      final SynopProvider provider = new OgimetSynopProvider("metar", "fr");

      provider.parseSynop("AAXX 28094 07002 22556 82910 10128 20123 30044 40125 51004 885 333 60007 88620 90710 91117 92426 555 60005=", null);
      System.out.println("releve : " + provider.releve);

      System.out.println("\n\n");
      provider.parseSynop("AAXX 27094 07002 22556 82910 10128 20123 30044 40125 51004 885 333 60007 88620 90710 91117 92426 555 60005=", null);
      System.out.println("releve : " + provider.releve);
    }
    catch (final Throwable th)
    {
      th.printStackTrace();
    }
  } */
}
