package com.pedro.android.mobibalises.alarm;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.service.BaliseProviderInfos;
import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm.Notification;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm.PlageHoraire;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm.PlageVitesse;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.misc.Sector;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.util.Log;

import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.service.IFullProvidersService;

/**
 * 
 * @author pedro.m
 */
public abstract class AlarmUtils
{
  public static final String PREFS_NB_ALARMS    = "config.alarms.nb";
  public static final String PREFS_ALARM_PREFIX = "config.alarms.alarm_";

  /**
   * 
   * @param context
   */
  public static void initResources(final Context context)
  {
    // Initialisations communes
    DrawingCommons.initialize(context);
  }

  /**
   * 
   * @param context
   * @param key
   * @return
   */
  public static List<String> getActiveAlarmsCountries(final Context context, final String key)
  {
    // Initialisations
    final List<BaliseAlarm> alarms = loadAlarms(context);
    final List<String> countries = new ArrayList<String>();

    // Pour chaque alarme
    for (final BaliseAlarm alarm : alarms)
    {
      if (alarm.active && (alarm.provider != null))
      {
        final String simpleKey = BaliseProviderUtils.getBaliseProviderSimpleKey(alarm.provider);
        if (simpleKey.equals(key))
        {
          final String countryKey = BaliseProviderUtils.getBaliseProviderCountryCode(alarm.provider);
          if (!countries.contains(countryKey))
          {
            countries.add(countryKey);
          }
        }
      }
    }

    return countries;
  }

  /**
   * 
   * @param context
   * @return
   */
  public static int getAlarmsCount(final Context context)
  {
    // Initialisations
    final SharedPreferences preferences = ActivityCommons.getSharedPreferences(context);

    // Nombre d'alarmes
    return preferences.getInt(PREFS_NB_ALARMS, 0);
  }

  /**
   * 
   * @param context
   * @return
   */
  public static List<BaliseAlarm> loadAlarms(final Context context)
  {
    // Initialisations
    final List<BaliseAlarm> alarms = new ArrayList<BaliseAlarm>();
    final SharedPreferences preferences = ActivityCommons.getSharedPreferences(context);

    // Nombre d'alarmes
    final int nbAlarmes = preferences.getInt(PREFS_NB_ALARMS, 0);

    // Alarmes
    for (int i = 0; i < nbAlarmes; i++)
    {
      try
      {
        final BaliseAlarm alarm = new BaliseAlarm();
        alarm.fromJSON(new JSONObject(preferences.getString(PREFS_ALARM_PREFIX + i, null)));
        alarms.add(alarm);
      }
      catch (final JSONException jse)
      {
        throw new RuntimeException(jse);
      }
    }

    // Fin
    return alarms;
  }

  /**
   * 
   * @param context
   * @param alarms
   * @return
   */
  public static boolean almostOneActiveAlarm(final Context context, final List<BaliseAlarm> alarms)
  {
    // Initialisations
    final List<BaliseAlarm> finalAlarms;
    if (alarms == null)
    {
      finalAlarms = loadAlarms(context);
    }
    else
    {
      finalAlarms = alarms;
    }

    // Au moins une alarme active ?
    for (final BaliseAlarm alarm : finalAlarms)
    {
      if (alarm.active)
      {
        return true;
      }
    }

    return false;
  }

  /**
   * 
   * @param context
   * @param alarm
   * @param position
   * @param inEditor
   * @throws JSONException
   */
  public static void saveAlarm(final Context context, final BaliseAlarm alarm, final int position, final Editor inEditor) throws JSONException
  {
    // Initialisations
    final Editor editor;
    if (inEditor != null)
    {
      editor = inEditor;
    }
    else
    {
      final SharedPreferences preferences = ActivityCommons.getSharedPreferences(context);
      editor = preferences.edit();
    }

    // Sauvegarde
    try
    {
      editor.putString(AlarmUtils.PREFS_ALARM_PREFIX + position, alarm.toJSON().toString());
    }
    finally
    {
      // Fin
      if (inEditor == null)
      {
        editor.commit();
      }
    }
  }

  /**
   * 
   * @param context
   * @param alarm
   * @param inEditor
   * @throws JSONException
   */
  public static int addAlarm(final Context context, final BaliseAlarm alarm, final Editor inEditor) throws JSONException
  {
    // Initialisations
    final SharedPreferences preferences = ActivityCommons.getSharedPreferences(context);
    final Editor editor;
    if (inEditor != null)
    {
      editor = inEditor;
    }
    else
    {
      editor = preferences.edit();
    }

    // Sauvegarde
    try
    {
      final int position = preferences.getInt(PREFS_NB_ALARMS, 0);
      editor.putString(AlarmUtils.PREFS_ALARM_PREFIX + position, alarm.toJSON().toString());
      editor.putInt(PREFS_NB_ALARMS, position + 1);

      return position;
    }
    finally
    {
      // Fin
      if (inEditor == null)
      {
        editor.commit();
      }
    }
  }

  /**
   * 
   * @param providersService
   * @param providerKey
   * @return
   */
  public static String getProviderName(final IFullProvidersService providersService, final String providerKey)
  {
    final BaliseProviderInfos infos = providersService.getBaliseProviderInfos(providerKey);
    if (infos == null)
    {
      return null;
    }

    return BaliseProviderUtils.getBaliseProviderSimpleName(infos.getName()) + " (" + BaliseProviderUtils.getBaliseProviderCountryCode(providerKey).toLowerCase() + ")";
  }

  /**
   * 
   * @param providersService
   * @param providerKey
   * @return
   */
  public static String getBaliseName(final IFullProvidersService providersService, final String providerKey, final String baliseId)
  {
    final BaliseProvider provider = providersService.getBaliseProvider(providerKey);
    if (provider == null)
    {
      return null;
    }

    final Balise balise = provider.getBaliseById(baliseId);
    if (balise == null)
    {
      return null;
    }

    return balise.nom;
  }

  /**
   * 
   * @param context
   * @param providerKey
   * @param baliseId
   * @param inEditor
   */
  public static int addNewAlarm(final Context context, final IFullProvidersService providersService, final String providerKey, final String baliseId, final Editor inEditor)
  {
    try
    {
      // Nouvelle Alarme
      final BaliseAlarm alarm = new BaliseAlarm();
      alarm.setId(String.valueOf(System.currentTimeMillis()));
      alarm.provider = providerKey;
      alarm.nomProvider = getProviderName(providersService, providerKey);
      alarm.idBalise = baliseId;
      alarm.nomBalise = getBaliseName(providersService, providerKey, baliseId);
      alarm.notifications.add(Notification.ANDROID);

      // Ajout
      return addAlarm(context, alarm, inEditor);
    }
    catch (final JSONException jse)
    {
      throw new RuntimeException(jse);
    }
  }

  /**
   * 
   * @param alarm
   * @param releve
   * @return
   */
  private static boolean isSecteurOk(final BaliseAlarm alarm, final Releve releve)
  {
    // Aucun secteur defini => ok
    if (!alarm.checkSecteurs || alarm.secteurs.isEmpty())
    {
      return true;
    }

    // Pas de direction dans le Releve => ko
    if (releve.directionMoyenne == Integer.MIN_VALUE)
    {
      return false;
    }

    // Direction connue et au moins 1 secteur defini => test
    for (final Sector secteur : alarm.secteurs)
    {
      if ((releve.directionMoyenne >= secteur.getStartAngle()) && (releve.directionMoyenne <= secteur.getEndAngle()))
      {
        return true;
      }
    }

    // Aucun secteur ne matche
    return false;
  }

  /**
   * 
   * @param alarm
   * @param releve
   * @return
   */
  private static boolean isVitessesOk(final BaliseAlarm alarm, final Releve releve)
  {
    final boolean miniOk = isVitesseOk(releve.ventMini, alarm.checkVitesseMini, alarm.plagesVitesseMini);
    final boolean moyOk = miniOk && isVitesseOk(releve.ventMoyen, alarm.checkVitesseMoy, alarm.plagesVitesseMoy);
    final boolean maxiOk = moyOk && isVitesseOk(releve.ventMaxi, alarm.checkVitesseMaxi, alarm.plagesVitesseMaxi);

    return maxiOk;
  }

  /**
   * 
   * @param vitesse
   * @param checkVitesse
   * @param plagesVitesse
   * @return
   */
  private static boolean isVitesseOk(final double vitesse, final boolean checkVitesse, final List<PlageVitesse> plagesVitesse)
  {
    // Check ?
    if (!checkVitesse)
    {
      return true;
    }

    // Vitesse donnee ?
    if (Double.isNaN(vitesse))
    {
      // Non => on ne peut pas tester
      return false;
    }

    // Pas de plage de vitesse
    if (plagesVitesse.size() == 0)
    {
      return true;
    }

    // Pour chaque plage de vitesse
    for (final PlageVitesse plage : plagesVitesse)
    {
      // Des qu'une plage est OK => la vitesse est OK (un "OU" entre les plages)
      if ((vitesse >= plage.vitesseMini) && (vitesse <= plage.vitesseMaxi))
      {
        return true;
      }
    }

    return false;
  }

  /**
   * 
   * @param alarm
   * @param releve
   * @return
   */
  private static boolean isVerifiee(final BaliseAlarm alarm, final Releve releve)
  {
    // Plages horaires
    if (!isPlagesHorairesOk(alarm, releve))
    {
      return false;
    }

    // Direction
    if (!isSecteurOk(alarm, releve))
    {
      return false;
    }

    // Vitesses
    if (!isVitessesOk(alarm, releve))
    {
      return false;
    }

    return true;
  }

  /**
   * 
   * @param alarm
   * @param releve
   * @return
   */
  private static boolean isPlagesHorairesOk(final BaliseAlarm alarm, final Releve releve)
  {
    if (!alarm.checkPlagesHoraires)
    {
      return true;
    }

    // Pour chaque plage
    final long now = System.currentTimeMillis();
    for (final PlageHoraire plage : alarm.plagesHoraires)
    {
      if (isPlageHoraireOk(plage, releve, now))
      {
        return true;
      }
    }

    return false;
  }

  /**
   * 
   * @param plage
   * @param releve
   * @return
   */
  @SuppressWarnings("unused")
  private static boolean isPlageHoraireOk(final PlageHoraire plage, final Releve releve, final long now)
  {
    // Debut
    final Calendar debut = Calendar.getInstance();
    debut.set(Calendar.HOUR_OF_DAY, plage.heuresDebut);
    debut.set(Calendar.MINUTE, plage.minutesDebut);
    debut.set(Calendar.SECOND, 0);
    debut.set(Calendar.MILLISECOND, 0);

    // Fin
    final Calendar fin = Calendar.getInstance();
    fin.set(Calendar.HOUR_OF_DAY, plage.heuresFin);
    fin.set(Calendar.MINUTE, plage.minutesFin);
    fin.set(Calendar.SECOND, 0);
    fin.set(Calendar.MILLISECOND, 0);

    return (now >= debut.getTimeInMillis()) && (now <= fin.getTimeInMillis());
  }

  /**
   * 
   * @param alarm
   * @param old
   * @param current
   * @return
   */
  public static boolean[] isLevee(final BaliseAlarm alarm, final Releve old, final Releve current)
  {
    // Alarme inactive
    if (!alarm.active)
    {
      return new boolean[] { false, false };
    }

    // Ancien releve
    final boolean verifieeAncienne = (old == null ? false : isVerifiee(alarm, old));

    // Releve courant
    final boolean verifieeCourante = isVerifiee(alarm, current);

    final boolean levee;
    switch (alarm.activation)
    {
      case CHANGE_ETAT:
        levee = (old != null) && (verifieeAncienne != verifieeCourante);
        break;
      case DEVIENT_INVALIDE:
        levee = (old != null) && verifieeAncienne && !verifieeCourante;
        break;
      case DEVIENT_VALIDE:
        levee = (old != null) && !verifieeAncienne && verifieeCourante;
        break;
      case VALIDE:
        levee = verifieeCourante;
        break;
      case INVALIDE:
        levee = !verifieeCourante;
        break;
      default:
        throw new RuntimeException("Activation inconnue !");
    }

    Log.d(AlarmUtils.class.getSimpleName(), "activation=" + alarm.activation + ", verifieeAncienne=" + verifieeAncienne + (old == null ? "(null) " : "") + ", verifieeCourante=" + verifieeCourante + ", levee=" + levee);

    return new boolean[] { levee, verifieeCourante };
  }

  /**
   * 
   * {0} : provider (nom)
   * {1} : balise (nom)
   * {2} : date relevé (format local)
   * {3} : heure relevé (format local)
   * {4} : direction moyenne (°)
   * {5} : direction moyenne (texte, ex : NNE)
   * {6} : vent mini (dans l'unité de l'utilisateur)
   * {7} : vent moyen (dans l'unité de l'utilisateur)
   * {8} : vent maxi (dans l'unité de l'utilisateur)
   * {9} : unité vent (kmh/mph/nds)
   *
   * @param context
   * @param pattern
   * @param alarm
   * @param releve
   * @param dateHeureAlarme
   * @return
   */
  public static String formatAlarmText(final Context context, final String pattern, final BaliseAlarm alarm, final Releve releve)
  {
    // Initialisations
    final Resources resources = context.getResources();

    // Provider
    final String nomProvider = (alarm.nomProvider == null ? resources.getString(R.string.alarm_notification_android_text_default_provider) : alarm.nomProvider);
    if (nomProvider == null)
    {
      return pattern;
    }

    // Balise
    final String nomBalise = (alarm.nomBalise == null ? resources.getString(R.string.alarm_notification_android_text_default_balise) : alarm.nomBalise);
    if (nomBalise == null)
    {
      return MessageFormat.format(pattern, nomProvider);
    }

    // Releve
    if (releve == null)
    {
      return MessageFormat.format(pattern, nomProvider, nomBalise);
    }

    // Date releve
    final Date date = (releve.date == null ? null : new Date(Utils.fromUTC(releve.date.getTime())));
    final String txtDate = (date == null ? "?" : DateFormat.getDateInstance(DateFormat.SHORT).format(date));

    // Heure releve
    final String txtHeure = (date == null ? "?" : DateFormat.getTimeInstance(DateFormat.SHORT).format(date));

    // Direction (°)
    final Integer directionMoyenne = (Integer.MIN_VALUE == releve.directionMoyenne ? null : Integer.valueOf(releve.directionMoyenne));

    // Direction (texte)
    final String txtDirectionMoyenne = (Integer.MIN_VALUE == releve.directionMoyenne ? "?" : DrawingCommons.getLabelDirectionVent(releve.directionMoyenne));

    // Vent mini
    final Double ventMini = (Double.isNaN(releve.ventMini) ? null : Double.valueOf(ActivityCommons.getFinalSpeed(releve.ventMini)));

    // Vent moyen
    final Double ventMoyen = (Double.isNaN(releve.ventMoyen) ? null : Double.valueOf(ActivityCommons.getFinalSpeed(releve.ventMoyen)));

    // Vent maxi
    final Double ventMaxi = (Double.isNaN(releve.ventMaxi) ? null : Double.valueOf(ActivityCommons.getFinalSpeed(releve.ventMaxi)));

    // Unité vent
    final String speedUnit = ActivityCommons.getSpeedUnit();

    // Formatage
    final String retour = MessageFormat.format(pattern, nomProvider, nomBalise, txtDate, txtHeure, directionMoyenne, txtDirectionMoyenne, ventMini, ventMoyen, ventMaxi, speedUnit);
    Log.d(AlarmUtils.class.getSimpleName(), "formatAlarmText : " + retour);
    return retour;
  }

  /**
   * 
   * @param alarm
   * @return
   */
  public static boolean isComplete(final BaliseAlarm alarm)
  {
    return (alarm != null) && !Utils.isStringVide(alarm.provider) && !Utils.isStringVide(alarm.idBalise);
  }
}
