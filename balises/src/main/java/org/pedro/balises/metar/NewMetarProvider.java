package org.pedro.balises.metar;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
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
public abstract class NewMetarProvider extends AbstractBaliseProvider
{
  private static final String       STRING_MESSAGE_METAR_INCORRECT    = "METAR incorrect : ";

  private static final String       CHAMP_METAR                       = "METAR";
  private static final String       CHAMP_SPECI                       = "SPECI";
  private static final String       CHAMP_NIL                         = "NIL";
  private static final String       CHAMP_AUTO                        = "AUTO";
  private static final String       CHAMP_CCA                         = "CCA";
  private static final String       CHAMP_CCB                         = "CCB";
  private static final String       CHAMP_COR                         = "COR";

  private static final Pattern      PATTERN_INDICATIF                 = Pattern.compile("\\w{4}");
  private static final Pattern      PATTERN_INCONNU                   = Pattern.compile("/+");
  private static final Pattern      PATTERN_VARIABILITE_VENT          = Pattern.compile("(\\d{3})V(\\d{3})");
  private static final Pattern      PATTERN_VISIBILITE_METRES         = Pattern.compile("\\d{4}(N|NE|E|SE|S|SW|W|NW)?");
  private static final Pattern      PATTERN_VISIBILITE_STATUS_MILES   = Pattern.compile("\\d+SM");
  private static final Pattern      PATTERN_VISIBILITE_FRACTION_SM    = Pattern.compile("\\d+/\\d+SM");
  private static final Pattern      PATTERN_VISI_PISTE                = Pattern.compile("R\\w+/\\w+");
  private static final Pattern      PATTERN_TEMPS_SIGNIFICATIF        = Pattern.compile("(\\+|-)?([A-Z]{2,})");
  private static final Pattern      PATTERN_NUAGES                    = Pattern.compile("([A-Z]{3}|///)(\\d{3}|///)?(///)?([A-Z]{2,3})?");
  private static final Pattern      PATTERN_TEMPERATURE               = Pattern.compile("([M]{0,1}\\d+)/([M]{0,1}\\d+)");
  private static final Pattern      PATTERN_PRESSION                  = Pattern.compile("([QA]{1})(\\d{3,4})");

  private static final Pattern      DATE_FORMAT                       = Pattern.compile("(\\d{2})(\\d{2})(\\d{2})Z");

  private static final String       CHAMP_VENT_VRB                    = "VRB";
  private static final String       CHAMP_VENT_DIRECTION_INCONNUE     = "///";
  private static final String       CHAMP_VENT_VITESSE_INCONNUE       = "//";
  private static final String       CHAMP_VENT_INDICATEUR_RAFALE      = "G";
  private static final String       CHAMP_VENT_UNITE_VENT_NOEUD       = "KT";
  private static final String       CHAMP_VENT_UNITE_VENT_KMH         = "KMH";
  private static final String       CHAMP_VENT_UNITE_VENT_MPS         = "MPS";

  private static final String       CHAMP_VISIBILITE_CAVOK            = "CAVOK";
  private static final String       CHAMP_VISIBILITE_NSC              = "NSC";
  private static final String       CHAMP_VISIBILITE_SKC              = "SKC";

  private static final String       CHAMP_TEMPS_AVERSE                = "SH";
  private static final String       CHAMP_TEMPS_ORAGE                 = "TS";
  private static final String       CHAMP_TEMPS_PLUIE                 = "RA";
  private static final String       CHAMP_TEMPS_BRUINE                = "DZ";
  private static final String       CHAMP_TEMPS_GRELE                 = "GR";
  private static final String       CHAMP_TEMPS_QUALIFICATIF_PLUS     = "+";
  private static final String       CHAMP_TEMPS_QUALIFICATIF_MOINS    = "-";
  private static final String[]     CHAMPS_TEMPS_SIGNIFICATIF         = { CHAMP_TEMPS_AVERSE, CHAMP_TEMPS_ORAGE, CHAMP_TEMPS_PLUIE, CHAMP_TEMPS_BRUINE, CHAMP_TEMPS_GRELE, "VC", "MI", "PR", "DR", "BL", "FZ", "RE", "BC", "XX", "SN", "PL",
      "GS", "SG", "IC", "UP", "BR", "FG", "HZ", "FU", "SA", "DU", "VA" };
  private static final List<String> CHAMPS_TEMPS_SIGNIFICATIF_LIST    = Arrays.asList(CHAMPS_TEMPS_SIGNIFICATIF);

  private static final String       CHAMP_NUAGES_ALTITUDE_INCONNUE    = "///";
  private static final String       CHAMP_NUAGES_FEW                  = "FEW";
  private static final String       CHAMP_NUAGES_SCT                  = "SCT";
  private static final String       CHAMP_NUAGES_BKN                  = "BKN";
  private static final String       CHAMP_NUAGES_OVC                  = "OVC";
  private static final String       CHAMP_NUAGES_CB                   = "CB";
  private static final String       CHAMP_NUAGES_TCU                  = "TCU";
  private static final int          CHAMP_NUAGES_AUCUN                = 0;

  private static final char         CHAMP_TEMPERATURE_MINUS           = 'M';

  private static final String       CHAMP_PRESSION_QNH                = "Q";
  private static final String       CHAMP_PRESSION_ALTIMETER          = "A";
  private static final double       CHAMP_PRESSION_FACTEUR_POUCES_HPA = 0.01 * 2.54 * 10;

  private static final String       REGEXP_SEPARATEURS                = "\\s+";
  private static final String       REGEXP_FIN_LIGNE                  = "^([^=]+)(=?)$";
  private static final String       REMPLACEMENT_FIN_LIGNE            = "$1 $2";
  private static final String       SPACE                             = " ";

  protected final Releve            releve                            = new Releve();

  /**
   * 
   * @param name
   * @param country
   * @param region
   */
  public NewMetarProvider(final String name, final String country, final String region)
  {
    super(name, country, region, 150);
  }

  /**
   * 
   * @param textMetars
   */
  protected void parseMetars(final List<String> textMetars)
  {
    // Analyse
    for (final String metar : textMetars)
    {
      final boolean ok = parseMetar(metar, null);
      if (ok)
      {
        onReleveParsed(releve);
      }
    }
  }

  /**
   * 
   * @param chaine
   * @param longueur
   * @return
   */
  private static List<String> splitFixed(final String chaine, final int longueur)
  {
    if (Utils.isStringVide(chaine))
    {
      return null;
    }

    if (chaine.length() % longueur != 0)
    {
      return null;
    }

    final int longueurChaine = chaine.length();
    final List<String> retour = new ArrayList<String>();
    for (int index = 0; index < longueurChaine; index += longueur)
    {
      retour.add(chaine.substring(index, index + longueur));
    }

    return retour;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @return
   */
  @SuppressWarnings("unused")
  private static boolean checkChamp(final String metar, final String[] champs, final int indice)
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
   * @param metar
   * @param champs
   * @param indice
   * @return
   */
  @SuppressWarnings("unused")
  private static boolean checkInconnu(final String metar, final String[] champs, final int indice)
  {
    if (PATTERN_INCONNU.matcher(champs[indice]).matches())
    {
      return false;
    }

    return true;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private static int parseChampMetar(final String metar, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }

    if (!CHAMP_METAR.equals(champs[indice]) && !CHAMP_SPECI.equals(champs[indice]))
    {
      return 0;
    }

    return 1;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private static int parseChampCor(final String metar, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }

    return CHAMP_COR.equals(champs[indice]) ? 1 : 0;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @param releve
   * @return
   * @throws ParseException
   */
  private static int parseChampIndicatif(final String metar, final String[] champs, final int indice, final Releve releve) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }

    // Format indicatif
    final Matcher matcher = PATTERN_INDICATIF.matcher(champs[indice]);
    if (!matcher.matches())
    {
      throw new ParseException("INDICATIF incorrect : " + metar, indice);
    }

    // OK
    releve.setId(champs[indice]);
    return 1;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private static int parseChampNilAutoCcaCcb(final String metar, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }

    // NIL
    if (CHAMP_NIL.equals(champs[indice]))
    {
      throw new ParseException("METAR manquant (NIL) : " + metar, indice);
    }

    // AUTO
    if (CHAMP_AUTO.equals(champs[indice]))
    {
      return 1;
    }

    // CCA (premiere correction)
    if (CHAMP_CCA.equals(champs[indice]))
    {
      return 1;
    }

    // CCB (seconde correction)
    if (CHAMP_CCB.equals(champs[indice]))
    {
      return 1;
    }

    return 0;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @param releve
   * @param inDate
   * @return
   * @throws ParseException
   */
  private static int parseChampDateHeure(final String metar, final String[] champs, final int indice, final Releve releve, final Date inDate) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }
    if (!checkInconnu(metar, champs, indice))
    {
      return 1;
    }

    // Date
    if (inDate == null)
    {
      final Matcher matcher = DATE_FORMAT.matcher(champs[indice]);
      if (!matcher.matches())
      {
        throw new ParseException("Date incorrecte : " + champs[indice], 0);
      }
      final int day = Integer.parseInt(matcher.group(1), 10);
      final int hours = Integer.parseInt(matcher.group(2), 10);
      final int minutes = Integer.parseInt(matcher.group(3), 10);

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
      cal.set(Calendar.MINUTE, minutes);
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

    return 1;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @param releve
   * @return
   * @throws ParseException
   */
  private static int parseChampVent(final String metar, final String[] champs, final int indice, final Releve releve) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }
    if (!checkInconnu(metar, champs, indice))
    {
      return 1;
    }

    // Direction du vent
    final String vent = champs[indice];
    final String direction = vent.substring(0, 3);
    if (!CHAMP_VENT_VRB.equals(direction) && !CHAMP_VENT_DIRECTION_INCONNUE.equals(direction))
    {
      releve.directionMoyenne = Integer.parseInt(direction, 10);
    }

    // Vitesse moyenne
    final String vitesseMoy = vent.substring(3, 5);
    if (!CHAMP_VENT_VITESSE_INCONNUE.equals(vitesseMoy))
    {
      releve.ventMoyen = Double.parseDouble(vitesseMoy);
    }

    // Vitesse rafale (maxi)
    int indiceUniteVitesseDebut = 5;
    if (CHAMP_VENT_INDICATEUR_RAFALE.equals(vent.substring(5, 6)))
    {
      final String vitesseMax = vent.substring(6, 8);
      if (!CHAMP_VENT_VITESSE_INCONNUE.equals(vitesseMax))
      {
        releve.ventMaxi = Double.parseDouble(vitesseMax);
      }

      // Suite
      indiceUniteVitesseDebut += 3;
    }

    // Unite vent
    final String uniteVitesseVent = vent.substring(indiceUniteVitesseDebut);
    float facteur = 0;
    boolean convertVitesseVent = false;
    if (CHAMP_VENT_UNITE_VENT_MPS.equals(uniteVitesseVent))
    {
      facteur = 3.6f;
      convertVitesseVent = true;
    }
    else if (CHAMP_VENT_UNITE_VENT_NOEUD.equals(uniteVitesseVent))
    {
      facteur = 1.852f;
      convertVitesseVent = true;
    }
    else if (CHAMP_VENT_UNITE_VENT_KMH.equals(uniteVitesseVent))
    {
      // Nothing
    }

    // Conversion si besoin
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

    return 1;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @param releve
   * @return
   * @throws ParseException
   */
  private static int parseChampVariabiliteVent(final String metar, final String[] champs, final int indice, final Releve releve) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }
    if (!checkInconnu(metar, champs, indice))
    {
      return 1;
    }

    // Expression reguliere
    final Matcher matcher = PATTERN_VARIABILITE_VENT.matcher(champs[indice]);
    if (!matcher.matches())
    {
      return 0;
    }

    // Variabilite
    releve.directionVentVariation1 = Integer.parseInt(matcher.group(1), 10);
    releve.directionVentVariation2 = Integer.parseInt(matcher.group(2), 10);

    return 1;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private static int parseChampVisibilite(final String metar, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }
    if (!checkInconnu(metar, champs, indice))
    {
      return 1;
    }

    // CAVOK, NSC, SKC
    if (CHAMP_VISIBILITE_CAVOK.equals(champs[indice]) || CHAMP_VISIBILITE_NSC.equals(champs[indice]) || CHAMP_VISIBILITE_SKC.equals(champs[indice]))
    {
      return 1;
    }

    // Visibilite metres
    final Matcher metresMatcher = PATTERN_VISIBILITE_METRES.matcher(champs[indice]);
    if (metresMatcher.matches())
    {
      return 1;
    }

    // Visibilite status miles
    final Matcher smMatcher = PATTERN_VISIBILITE_STATUS_MILES.matcher(champs[indice]);
    if (smMatcher.matches())
    {
      return 1;
    }

    // Visibilite fraction status miles
    final Matcher fractionSmMatcher = PATTERN_VISIBILITE_FRACTION_SM.matcher(champs[indice]);
    if (fractionSmMatcher.matches())
    {
      return 1;
    }

    return 0;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @return
   * @throws ParseException
   */
  private static int parseChampVisibilitePiste(final String metar, final String[] champs, final int indice) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }
    if (!checkInconnu(metar, champs, indice))
    {
      return 1;
    }

    final Matcher matcher = PATTERN_VISI_PISTE.matcher(champs[indice]);
    if (matcher.matches())
    {
      return 1;
    }

    return 0;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @param releve
   * @return
   * @throws ParseException
   */
  private static int parseChampTempsSignificatif(final String metar, final String[] champs, final int indice, final Releve releve) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }
    if (!checkInconnu(metar, champs, indice))
    {
      return 1;
    }

    // Expression reguliere
    final Matcher matcher = PATTERN_TEMPS_SIGNIFICATIF.matcher(champs[indice]);
    if (!matcher.matches())
    {
      return 0;
    }

    // Analyse regexp
    final int groupCount = matcher.groupCount();
    final String qualificatif = (groupCount == 2 ? matcher.group(1) : null);
    final String phenomenesString = matcher.group(groupCount == 1 ? 1 : 2);
    final List<String> phenomenes = splitFixed(phenomenesString, 2);

    // Verification taille
    if ((phenomenes == null) || (phenomenes.size() == 0))
    {
      return 0;
    }

    // Verification contenu
    for (final String phenomene : phenomenes)
    {
      if (!CHAMPS_TEMPS_SIGNIFICATIF_LIST.contains(phenomene))
      {
        return 0;
      }
    }

    // Analyse contenu
    if (phenomenes.contains(CHAMP_TEMPS_ORAGE) || phenomenes.contains(CHAMP_TEMPS_GRELE))
    {
      releve.pluie = Releve.PLUIE_TRES_FORTE;
    }
    else if (phenomenes.contains(CHAMP_TEMPS_PLUIE) || phenomenes.contains(CHAMP_TEMPS_AVERSE))
    {
      if (CHAMP_TEMPS_QUALIFICATIF_PLUS.equals(qualificatif))
      {
        releve.pluie = Releve.PLUIE_FORTE;
      }
      else if (CHAMP_TEMPS_QUALIFICATIF_MOINS.equals(qualificatif))
      {
        releve.pluie = Releve.PLUIE_FAIBLE;
      }
      else
      {
        releve.pluie = Releve.PLUIE_MOYENNE;
      }
    }
    else if (phenomenes.contains(CHAMP_TEMPS_BRUINE))
    {
      releve.pluie = Releve.PLUIE_FAIBLE;
    }
    else
    {
      releve.pluie = Releve.PLUIE_AUCUNE;
    }

    return 1;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @param releve
   * @return
   * @throws ParseException
   */
  private static int parseChampNuages(final String metar, final String[] champs, final int indice, final Releve releve) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }
    if (!checkInconnu(metar, champs, indice))
    {
      return 1;
    }

    // Expression reguliere
    final Matcher matcher = PATTERN_NUAGES.matcher(champs[indice]);
    if (!matcher.matches())
    {
      return 0;
    }

    // Extraction des groupes
    final List<String> groups = new ArrayList<String>();
    final int groupCount = matcher.groupCount();
    for (int i = 1; i <= groupCount; i++)
    {
      groups.add(matcher.group(i));
    }

    // Huitieme
    final String huitiemeString = groups.get(0);
    final int huitieme;
    if (CHAMP_NUAGES_FEW.equals(huitiemeString))
    {
      huitieme = 2;
    }
    else if (CHAMP_NUAGES_SCT.equals(huitiemeString))
    {
      huitieme = 4;
    }
    else if (CHAMP_NUAGES_BKN.equals(huitiemeString))
    {
      huitieme = 6;
    }
    else if (CHAMP_NUAGES_OVC.equals(huitiemeString))
    {
      huitieme = 8;
    }
    else
    {
      huitieme = 0;
    }

    // Huitieme pire que celui deja enregistre ?
    final int huitiemeCourant = (releve.nuages == Integer.MIN_VALUE ? 0 : releve.nuages);
    if (huitieme > huitiemeCourant)
    {
      // Nuages
      releve.nuages = huitieme;
    }

    // Plafond mini
    if ((huitieme > 0) && !CHAMP_NUAGES_ALTITUDE_INCONNUE.equals(groups.get(1)))
    {
      final int plafond = Math.round(30.48f * Integer.parseInt(groups.get(1), 10));
      final int plafondCourant = (releve.plafondNuages == Integer.MIN_VALUE ? Integer.MAX_VALUE : releve.plafondNuages);
      if (plafond < plafondCourant)
      {
        releve.plafondNuages = plafond;
      }
    }

    // Nuages bourgeonnants
    if (groups.contains(CHAMP_NUAGES_CB) || groups.contains(CHAMP_NUAGES_TCU))
    {
      releve.nuagesBourgeonnants = Utils.BOOLEAN_TRUE;
    }

    return 1;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @param releve
   * @return
   * @throws ParseException
   */
  private static int parseChampTemperatures(final String metar, final String[] champs, final int indice, final Releve releve) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }
    if (!checkInconnu(metar, champs, indice))
    {
      return 1;
    }

    // Expression reguliere
    final Matcher matcher = PATTERN_TEMPERATURE.matcher(champs[indice]);
    if (!matcher.matches())
    {
      return 0;
    }

    // Temperature
    String temperatureChaine = matcher.group(1);
    int signeTemperature = 1;
    if (temperatureChaine.charAt(0) == CHAMP_TEMPERATURE_MINUS)
    {
      signeTemperature = -1;
      temperatureChaine = temperatureChaine.substring(1);
    }
    releve.temperature = signeTemperature * Double.parseDouble(temperatureChaine);

    // Point de rosee
    String roseeChaine = matcher.group(2);
    int signeRosee = 1;
    if (roseeChaine.charAt(0) == CHAMP_TEMPERATURE_MINUS)
    {
      signeRosee = -1;
      roseeChaine = roseeChaine.substring(1);
    }
    releve.pointRosee = signeRosee * Double.parseDouble(roseeChaine);

    return 1;
  }

  /**
   * 
   * @param metar
   * @param champs
   * @param indice
   * @param releve
   * @return
   * @throws ParseException
   */
  private static int parseChampPression(final String metar, final String[] champs, final int indice, final Releve releve) throws ParseException
  {
    // Verifications de base
    if (!checkChamp(metar, champs, indice))
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }
    if (!checkInconnu(metar, champs, indice))
    {
      return 1;
    }

    // Expression reguliere
    final Matcher matcher = PATTERN_PRESSION.matcher(champs[indice]);
    if (!matcher.matches())
    {
      return 0;
    }

    // Pression
    final String typePression = matcher.group(1);
    final double pression = Double.parseDouble(matcher.group(2));
    if (CHAMP_PRESSION_QNH.equals(typePression))
    {
      releve.pression = pression;
    }
    else if (CHAMP_PRESSION_ALTIMETER.equals(typePression))
    {
      releve.pression = pression * CHAMP_PRESSION_FACTEUR_POUCES_HPA;
    }
    else
    {
      throw new ParseException(STRING_MESSAGE_METAR_INCORRECT + metar, indice);
    }

    return 1;
  }

  /**
   * 
   * @param metar
   * @param inDate
   * @return
   */
  protected boolean parseMetar(final String metar, final Date inDate)
  {
    // Mise en forme
    final String finalMetar = metar.replaceFirst(REGEXP_FIN_LIGNE, REMPLACEMENT_FIN_LIGNE).replaceAll(REGEXP_SEPARATEURS, SPACE);

    // Champs
    final String[] champs = finalMetar.trim().toUpperCase().split(SPACE);

    // Analyse des champs
    try
    {
      // Initialisations
      int indice = 0;

      // METAR
      int deltaIndice = parseChampMetar(metar, champs, indice);
      if (deltaIndice == 0)
      {
        // Pas un METAR
        return false;
      }
      indice += deltaIndice;

      // Champ "COR" (utilite inconnue)
      indice += parseChampCor(metar, champs, indice);

      // Indicatif
      releve.clear();
      releve.nuages = CHAMP_NUAGES_AUCUN;
      indice += parseChampIndicatif(metar, champs, indice, releve);

      // NIL, AUTO, CCA ou CCB
      indice += parseChampNilAutoCcaCcb(metar, champs, indice);
      indice += parseChampNilAutoCcaCcb(metar, champs, indice);

      // Date
      indice += parseChampDateHeure(metar, champs, indice, releve, inDate);

      // NIL, AUTO, CCA ou CCB
      indice += parseChampNilAutoCcaCcb(metar, champs, indice);

      // Vent
      indice += parseChampVent(metar, champs, indice, releve);

      // Variabilite vent
      indice += parseChampVariabiliteVent(metar, champs, indice, releve);

      // Visibilite
      deltaIndice = parseChampVisibilite(metar, champs, indice);
      while (deltaIndice > 0)
      {
        indice += deltaIndice;
        deltaIndice = parseChampVisibilite(metar, champs, indice);
      }

      // Portee visuelle sur piste
      deltaIndice = parseChampVisibilitePiste(metar, champs, indice);
      while (deltaIndice > 0)
      {
        indice += deltaIndice;
        deltaIndice = parseChampVisibilitePiste(metar, champs, indice);
      }

      // Temps significatif
      indice += parseChampTempsSignificatif(metar, champs, indice, releve);

      // Nuages
      deltaIndice = parseChampNuages(metar, champs, indice, releve);
      while (deltaIndice > 0)
      {
        indice += deltaIndice;
        deltaIndice = parseChampNuages(metar, champs, indice, releve);
      }

      // Temperatures
      indice += parseChampTemperatures(metar, champs, indice, releve);

      // Pression
      indice += parseChampPression(metar, champs, indice, releve);

      // La fin : peu importe
    }
    catch (final Throwable th)
    {
      System.err.println("Analyse METAR impossible : '" + metar + "' (" + th.getMessage() + ")");
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
    return 30;
  }

  /**
   * 
   * @param args
  public static void main(final String[] args)
  {
    try
    {
      final NewMetarProvider provider = new OgimetMetarProvider("metar", "fr", null);

      provider.parseMetar("METAR LFAQ 282330Z AUTO 26012KT 9999 BKN010 OVC019 15/14 Q1013=", null);
      System.out.println("releve : " + provider.releve);

      System.out.println("\n\n");
      provider.parseMetar("METAR LFAQ 010800Z AUTO 26012KT 9999 BKN010 OVC019 15/14 Q1013=", null);
      System.out.println("releve : " + provider.releve);
    }
    catch (final Throwable th)
    {
      th.printStackTrace();
    }
  }
  */
}
