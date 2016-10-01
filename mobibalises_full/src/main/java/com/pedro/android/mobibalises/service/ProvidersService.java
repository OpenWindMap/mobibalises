package com.pedro.android.mobibalises.service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.analytics.AnalyticsService;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;
import org.pedro.android.mobibalises_common.service.BaliseProviderInfos;
import org.pedro.android.mobibalises_common.service.BaliseProviderListener;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm.Notification;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.utils.FileTimestampUtils;
import org.pedro.utils.ThreadUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.pedro.android.mobibalises.FullActivityCommons;
import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.alarm.AlarmUtils;
import com.pedro.android.mobibalises.favorites.BaliseFavorite;
import com.pedro.android.mobibalises.favorites.FavoritesService;
import com.pedro.android.mobibalises.location.FullLocationService;
import com.pedro.android.mobibalises.start.BalisesStartActivity;
import com.pedro.android.mobibalises.voice.VoiceService;
import com.pedro.android.mobibalises.widget.BalisesWidgets;

/**
 * 
 * @author pedro.m
 */
public final class ProvidersService extends AbstractProvidersService implements IFullProvidersService
{
  // Constantes
  public static final String                    ACTION_SHUTDOWN_TRY                     = "mobibalises.SHUTDOWN_TRY";
  public static final String                    ACTION_STARTUP_TRY                      = "mobibalises.STARTUP_TRY";
  public static final String                    ACTION_WIDGET_TAP                       = "mobibalises.WIDGET_TAP";
  public static final String                    ACTION_ALARM_TAP                        = "mobibalises.ALARM_TAP";
  public static final String                    EXTRA_FLIGHT_MODE_START                 = "EXTRA_FLIGHT_MODE_START";
  public static final String                    ACTION_FLIGHT_MODE_STOP                 = "mobibalises.FLIGHT_MODE_STOP";
  public static final String                    INTENT_ALARM_ACTION                     = "alarmAction";
  public static final int                       INTENT_ALARM_ACTION_CANCEL_NOTIFICATION = 1;
  public static final int                       INTENT_ALARM_ACTION_CANCEL_ALARM        = 2;
  public static final int                       INTENT_ALARM_ACTION_CANCEL_ALARM_MODE   = 3;
  public static final String                    INTENT_ALARM_NOTIFICATION_TAG           = "notificationTag";
  public static final String                    INTENT_ALARM_ID                         = "alarmId";
  private static final String                   INTENT_FILE_DATA_SCHEME                 = "file";
  private static final List<BaliseProviderMode> staticProviderModeAlarm                 = Arrays.asList(new BaliseProviderMode[] { BaliseProviderMode.ALARM });

  // Broadcast alarme
  private static final String                   BROADCAST_ALARM                         = "mobibalises.alarm";
  private static final String                   BROADCAST_ALARM_ALARM                   = "alarm";
  private static final String                   BROADCAST_ALARM_LEVEE                   = "raised";
  private static final String                   BROADCAST_ALARM_VERIFIEE                = "satisfied";
  private static final String                   BROADCAST_ALARM_RELEVE                  = "report";

  // Commun
  Resources                                     theResources;
  SharedPreferences                             theSharedPreferences;

  // Mode vol
  boolean                                       flightMode                              = false;
  private FlightModeThread                      flightModeThread;
  boolean                                       activityOnForeground                    = false;

  // Licence
  private MobibalisesLicenseChecker             licenseChecker;
  private final Object                          licenseLock                             = new Object();
  private boolean                               licenseOk                               = false;
  private boolean                               finishing                               = false;

  // Mode historique
  boolean                                       historyMode                             = false;
  HistoryModeThread                             historyModeThread;

  // Mode alarme
  boolean                                       alarmMode                               = false;
  private AlarmModeThread                       alarmModeThread;
  List<BaliseAlarm>                             currentAlarms                           = null;
  final Object                                  currentAlarmsLock                       = new Object();

  // Ecoute des demandes d'arret du service
  private ShutdownTryReceiver                   shutdownTryReceiver;

  // Ecoute des click sur widget
  private WidgetTapReceiver                     widgetTapReceiver;

  // Ecoute des click sur notifications alarmes
  private AlarmTapReceiver                      alarmTapReceiver;

  // Ecoute des demandes pour le mode vol
  private FlightModeControlerReceiver           flightModeControlerReceiver;

  // Ecoute des evenements SDCard
  private SDCardBroadcastReceiver               sdcardReceiver;

  // Voice Service
  private VoiceService                          voiceService;

  // Favorites Service
  FavoritesService                              favoritesService;

  /**
   * 
   * @author pedro.m
   */
  private static class SDCardBroadcastReceiver extends BroadcastReceiver
  {
    private ProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    SDCardBroadcastReceiver(final ProvidersService providersService)
    {
      super();
      this.providersService = providersService;
    }

    @Override
    public void onReceive(final Context inContext, final Intent intent)
    {
      // Initialisations
      Log.d(providersService.getClass().getSimpleName(), "SDCard event : " + intent);

      // Mode historique
      providersService.historyModeThread.refreshSDCardAvailability();
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      providersService = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ShutdownTryReceiver extends BroadcastReceiver
  {
    private ProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    ShutdownTryReceiver(final ProvidersService providersService)
    {
      this.providersService = providersService;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      // Debut
      Log.d(providersService.getClass().getSimpleName(), ">>> ShutdownTryReceiver.onReceive : " + intent);

      // Retrait de la notification de mode historique
      if (!isHistoryModeOn(context))
      {
        FullActivityCommons.removeHistoryModeNotification(context);
      }

      // Retrait de la notification de widget parlant
      if (!BalisesWidgets.existsSpeakBlackWidget(context))
      {
        FullActivityCommons.removeSpeakBlackWidgetNotification(context);
      }

      // Retrait de la notification de mode alarm
      if (!isAlarmModeOn(context))
      {
        FullActivityCommons.removeAlarmModeNotification(context);
      }

      // Gestion de la voix
      BalisesWidgets.manageVoiceClient(context, providersService);

      // Arret si possible
      providersService.stopSelfIfPossible();

      // Fin
      Log.d(providersService.getClass().getSimpleName(), "<<< ShutdownTryReceiver.onReceive");
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      providersService = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class WidgetTapReceiver extends BroadcastReceiver
  {
    private ProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    WidgetTapReceiver(final ProvidersService providersService)
    {
      this.providersService = providersService;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      // Debut
      Log.d(providersService.getClass().getSimpleName(), ">>> WidgetTapReceiver.onReceive : " + intent);

      // Initialisations
      final VoiceService voiceService = providersService.getVoiceService();

      // Arret de la synthese vocale
      if (!voiceService.stopSpeaking(false))
      {
        // Si la synthese vocale n'etait pas active : demarrage de l'appli
        final Intent startIntent = new Intent(context, BalisesStartActivity.class);
        startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        providersService.startActivity(startIntent);
      }

      // Fin
      Log.d(providersService.getClass().getSimpleName(), "<<< WidgetTapReceiver.onReceive");
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      providersService = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class AlarmTapReceiver extends BroadcastReceiver
  {
    private ProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    AlarmTapReceiver(final ProvidersService providersService)
    {
      this.providersService = providersService;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      // Debut
      Log.d(providersService.getClass().getSimpleName(), ">>> AlarmTapReceiver.onReceive : " + intent);

      try
      {
        // Initialisations
        final int action = intent.getIntExtra(INTENT_ALARM_ACTION, -1);
        final String notificationTag = intent.getStringExtra(INTENT_ALARM_NOTIFICATION_TAG);
        final String alarmId = intent.getStringExtra(INTENT_ALARM_ID);
        Log.d(providersService.getClass().getSimpleName(), "intentAlarmAction : " + action);
        Log.d(providersService.getClass().getSimpleName(), "intentAlarmNotificationTag : " + notificationTag);
        Log.d(providersService.getClass().getSimpleName(), "intentAlarmId : " + alarmId);
        final BaliseAlarm alarm = new BaliseAlarm();
        alarm.setId(alarmId);

        switch (action)
        {
          case INTENT_ALARM_ACTION_CANCEL_NOTIFICATION:
            // L'action est gérée par la classe AlarmNotificationCloseReceiver
            return;

          case INTENT_ALARM_ACTION_CANCEL_ALARM_MODE:
            // Desactivation du mode alarme
            providersService.setAlarmMode(false);
            FullActivityCommons.removeAlarmModeNotification(context);
            break;

          case INTENT_ALARM_ACTION_CANCEL_ALARM:
            // Desactivation de l'alarme
            final List<BaliseAlarm> alarms = AlarmUtils.loadAlarms(providersService.getApplicationContext());
            final int position = alarms.indexOf(alarm);
            if (position >= 0)
            {
              final BaliseAlarm finalAlarm = alarms.get(position);
              finalAlarm.active = false;
              try
              {
                AlarmUtils.saveAlarm(providersService.getApplicationContext(), finalAlarm, position, null);
                providersService.onAlarmActivationChanged(finalAlarm);
              }
              catch (final JSONException jse)
              {
                throw new RuntimeException(jse);
              }
            }
            break;

          default:
            // Cas non prévu
            return;
        }

        // Suppression de la notification
        final boolean verifiee = Boolean.parseBoolean(notificationTag);
        FullActivityCommons.manageAlarmNotification(providersService, alarm, false, verifiee, null);

        // Arret si possible
        providersService.stopSelfIfPossible();
      }
      finally
      {
        // Fin
        Log.d(providersService.getClass().getSimpleName(), "<<< AlarmTapReceiver.onReceive");
      }
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      providersService = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class FlightModeControlerReceiver extends BroadcastReceiver
  {
    private ProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    FlightModeControlerReceiver(final ProvidersService providersService)
    {
      this.providersService = providersService;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      // Debut
      Log.d(providersService.getClass().getSimpleName(), ">>> FlightModeControlerReceiver.onReceive : " + intent);

      if (ACTION_FLIGHT_MODE_STOP.equals(intent.getAction()))
      {
        providersService.setFlightMode(false);
        FullActivityCommons.removeFlightModeNotification(providersService.getApplicationContext());
        providersService.stopSelfIfPossible();
      }

      // Fin
      Log.d(providersService.getClass().getSimpleName(), "<<< FlightModeControlerReceiver.onReceive");
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      providersService = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class AlarmModeThread extends Thread implements BaliseProviderListener
  {
    private ProvidersService          providersService;
    private final Map<String, Releve> lastReleves = new HashMap<String, Releve>();
    private final List<BaliseAlarm>   alarms      = new ArrayList<BaliseAlarm>();
    private final List<AlarmModeStep> steps       = new ArrayList<AlarmModeStep>();

    /**
     * 
     * @author pedro.m
     */
    private static class AlarmModeStep
    {
      enum StepType
      {
        ACTIVATE, DEACTIVATE, RELEVES_UPDATE, CONFIG_UPDATE
      }

      final StepType           type;
      final String             providerKey;
      final Collection<Releve> releves;
      final BaliseAlarm        alarm;

      /**
       * 
       * @param type
       * @param providerKey
       * @param releves
       * @param alarm
       */
      AlarmModeStep(final StepType type, final String providerKey, final Collection<Releve> releves, final BaliseAlarm alarm)
      {
        this.type = type;
        this.providerKey = providerKey;
        this.releves = releves;
        this.alarm = alarm;
      }

      @Override
      public String toString()
      {
        return type + "/" + providerKey + "/" + (releves == null ? -1 : releves.size()) + "/" + alarm;
      }
    }

    /**
     * 
     * @param providersService
     */
    AlarmModeThread(final ProvidersService providersService)
    {
      // Super
      super(AlarmModeThread.class.getName());

      this.providersService = providersService;
    }

    @Override
    public void run()
    {
      // Initialisations
      AlarmUtils.initResources(providersService.getApplicationContext());

      // Boucle principale
      while (!isInterrupted())
      {
        synchronized (this)
        {
          if (canWait())
          {
            try
            {
              Log.d(getClass().getSimpleName(), ">>> wait");
              wait();
              Log.d(getClass().getSimpleName(), "<<< wait");
            }
            catch (final InterruptedException e)
            {
              Log.d(getClass().getSimpleName(), ">>> interrupt");
              Thread.currentThread().interrupt();
            }
          }
        }

        // Si fin
        if (isInterrupted())
        {
          break;
        }

        // Traitement
        doIt();
      }

      // Fin
      providersService.removeBaliseProviderListener(this, false);
      providersService = null;
      lastReleves.clear();
    }

    /**
     * 
     */
    private void doIt()
    {
      // Etape a traiter ?
      final AlarmModeStep step;
      synchronized (steps)
      {
        // Etape
        Log.d(getClass().getSimpleName(), "steps : " + steps);
        if (steps.size() > 0)
        {
          step = steps.remove(0);
        }
        else
        {
          step = null;
        }
      }

      // Traitement etape
      if (step != null)
      {
        switch (step.type)
        {
          case ACTIVATE:
            doActivate(step);
            break;

          case DEACTIVATE:
            doDeactivate(step);
            break;

          case RELEVES_UPDATE:
            if (providersService.alarmMode)
            {
              doRelevesStep(step);
            }
            break;

          case CONFIG_UPDATE:
            if (providersService.alarmMode)
            {
              doConfigUpdate();
            }
            break;
        }
      }
    }

    /**
     * 
     * @param step
     */
    private void doActivate(final AlarmModeStep step)
    {
      // Initialisation
      Log.d(getClass().getSimpleName(), ">>> doActivate(" + step + ")");

      try
      {
        // Lecture config
        doConfigUpdate();

        // Une seule alarme ?
        if (step.alarm != null)
        {
          doActivate(step.alarm);
          return;
        }

        // Copie de la liste des alarmes
        final List<BaliseAlarm> innerAlarms = copyAlarms();

        // Recuperation des derniers releves connus pour toutes les alarmes
        for (final BaliseAlarm alarm : innerAlarms)
        {
          doActivate(alarm);
        }
      }
      finally
      {
        // Fin
        Log.d(getClass().getSimpleName(), "<<< doActivate(" + step + ")");
      }
    }

    /**
     * 
     * @param alarm
     */
    private void doActivate(final BaliseAlarm alarm)
    {
      // Seulement si l'alarme est complete
      if (!AlarmUtils.isComplete(alarm))
      {
        return;
      }
      Log.d(getClass().getSimpleName(), "alarm : " + alarm + " (active=" + alarm.active + ")");

      // Seulement si le mode alarme est actif et si l'alarme est active
      if (!providersService.isAlarmMode() || !alarm.active)
      {
        return;
      }

      // Recherche du provider
      final BaliseProvider provider = providersService.getBaliseProvider(alarm.provider);
      Log.d(getClass().getSimpleName(), "provider : " + provider);
      if (provider == null)
      {
        return;
      }

      // Nouveau step releves
      final AlarmModeStep step = new AlarmModeStep(AlarmModeStep.StepType.RELEVES_UPDATE, alarm.provider, provider.getReleves(), alarm);
      doRelevesStep(step, alarm);

      // Recuperation du dernier releve et sauvegarde
      final Releve releve = provider.getReleveById(alarm.idBalise);
      Log.d(getClass().getSimpleName(), "releve : " + releve);
      if (releve != null)
      {
        final Releve releveCopie = new Releve();
        releveCopie.copyFrom(releve);
        lastReleves.put(alarm.idBalise, releveCopie);
      }
    }

    /**
     * 
     * @param step
     */
    private void doDeactivate(final AlarmModeStep step)
    {
      // Initialisation
      Log.d(getClass().getSimpleName(), ">>> doDeactivate(" + step + ")");

      try
      {
        // Lecture config
        doConfigUpdate();

        // Une seule alarme ?
        if (step.alarm != null)
        {
          doDeactivate(step.alarm);
          return;
        }

        // Vidage de la config
        synchronized (alarms)
        {
          alarms.clear();
        }

        // Vidage des derniers releves
        lastReleves.clear();
      }
      finally
      {
        // Fin
        Log.d(getClass().getSimpleName(), "<<< doDeactivate(" + step + ")");
      }
    }

    /**
     * 
     * @param alarm
     */
    private void doDeactivate(final BaliseAlarm alarm)
    {
      // Seulement si l'alarme est complete
      if (!AlarmUtils.isComplete(alarm))
      {
        return;
      }

      // Initialisation
      Log.d(getClass().getSimpleName(), ">>> doDeactivate(" + alarm + ")");

      // Vidage des derniers releves pour la balise
      lastReleves.remove(alarm.idBalise);

      // Fin
      Log.d(getClass().getSimpleName(), "<<< doDeactivate(" + alarm + ")");
    }

    /**
     * 
     * @return
     */
    private List<BaliseAlarm> copyAlarms()
    {
      final List<BaliseAlarm> innerAlarms = new ArrayList<BaliseAlarm>();
      synchronized (alarms)
      {
        innerAlarms.addAll(alarms);
      }

      return innerAlarms;
    }

    /**
     * 
     * @param step
     */
    private void doRelevesStep(final AlarmModeStep step)
    {
      // Initialisation
      Log.d(getClass().getSimpleName(), ">>> doRelevesStep(" + step + ")");

      try
      {
        // Si l'alarme est specifiee
        if (step.alarm != null)
        {
          doRelevesStep(step, step.alarm);
          return;
        }

        // Copie de la liste des alarmes
        final List<BaliseAlarm> innerAlarms = copyAlarms();

        // Pour chaque alarme
        for (final BaliseAlarm alarm : innerAlarms)
        {
          doRelevesStep(step, alarm);
        }
      }
      finally
      {
        // Fin
        Log.d(getClass().getSimpleName(), "<<< doRelevesStep(" + step + ")");
      }
    }

    /**
     * 
     * @param step
     * @param alarm
     */
    private void doRelevesStep(final AlarmModeStep step, final BaliseAlarm alarm)
    {
      // Seulement si l'alarme est complete
      if (!AlarmUtils.isComplete(alarm))
      {
        return;
      }

      // Initialisation
      Log.d(getClass().getSimpleName(), ">>> doRelevesStep(" + step + ", " + alarm + ")");

      try
      {
        // Mode alarme inactif ou alarme inactive ?
        if (!providersService.isAlarmMode() || !alarm.active)
        {
          Log.d(getClass().getSimpleName(), "... not active");
          return;
        }

        // Verification du provider
        if (!alarm.provider.equals(step.providerKey))
        {
          Log.d(getClass().getSimpleName(), "... not for provider");
          return;
        }

        // Recherche du releve
        final Releve newReleve = findReleve(step.releves, alarm.idBalise);
        if (newReleve == null)
        {
          Log.d(getClass().getSimpleName(), "... not for balise");
          return;
        }

        // Recherche de l'ancien releve
        final Releve oldReleve = lastReleves.get(alarm.idBalise);
        Log.d(getClass().getSimpleName(), "newReleve : " + newReleve);
        Log.d(getClass().getSimpleName(), "oldReleve : " + oldReleve);
        if ((oldReleve != null) && oldReleve.date.equals(newReleve.date))
        {
          Log.d(getClass().getSimpleName(), "... releve is not new");
          return;
        }

        // Verification de l'alarme
        final boolean[] leveeVerifiee = AlarmUtils.isLevee(alarm, oldReleve, newReleve);
        Log.d(getClass().getSimpleName(), "levee : " + leveeVerifiee[0] + ", verifiee : " + leveeVerifiee[1]);
        manageAlarm(alarm, leveeVerifiee[0], leveeVerifiee[1], newReleve);

        // Fin : copie de sauvegarde du dernier releve
        final Releve releveCopie = new Releve();
        releveCopie.copyFrom(newReleve);
        lastReleves.put(alarm.idBalise, releveCopie);
      }
      finally
      {
        // Fin
        Log.d(getClass().getSimpleName(), "<<< doRelevesStep(" + step + ", " + alarm + ")");
      }
    }

    /**
     * 
     * @param alarm
     * @param levee
     * @param verifiee
     * @param releve
     */
    private void manageAlarm(final BaliseAlarm alarm, final boolean levee, final boolean verifiee, final Releve releve)
    {
      // Notifications
      for (final Notification notification : alarm.notifications)
      {
        switch (notification)
        {
          case ANDROID:
            FullActivityCommons.manageAlarmNotification(providersService.getApplicationContext(), alarm, levee, verifiee, releve);
            break;
          case VOIX:
            manageVoiceNotification(alarm, levee, verifiee, releve);
            break;
          case BROADCAST:
            manageBroadcastNotification(alarm, levee, verifiee, releve);
            break;
        }
      }

      // Statistiques
      if (levee)
      {
        providersService.getAnalyticsService().trackEvent(AnalyticsService.CAT_ALARM_MODE, AnalyticsService.ACT_ALARM_FIRED, alarm.provider + "." + alarm.idBalise);
      }
    }

    /**
     * 
     * @param alarm
     * @param levee
     * @param verifiee
     * @param releve
     */
    private void manageBroadcastNotification(final BaliseAlarm alarm, final boolean levee, final boolean verifiee, final Releve releve)
    {
      final Intent intent = new Intent(BROADCAST_ALARM);
      intent.putExtra(BROADCAST_ALARM_ALARM, alarm);
      intent.putExtra(BROADCAST_ALARM_LEVEE, levee);
      intent.putExtra(BROADCAST_ALARM_VERIFIEE, verifiee);
      intent.putExtra(BROADCAST_ALARM_RELEVE, releve);
      providersService.sendBroadcast(intent);
      Log.d(getClass().getSimpleName(), "alarm broadcasted : " + alarm + "/" + levee + "/" + verifiee + "/" + releve);
    }

    /**
     * 
     * @param alarm
     * @param levee
     * @param verifiee
     * @param releve
     */
    private void manageVoiceNotification(final BaliseAlarm alarm, final boolean levee, final boolean verifiee, final Releve releve)
    {
      // Levee ?
      if (!levee)
      {
        return;
      }

      // Provider existant ?
      final BaliseProvider provider = providersService.getBaliseProvider(alarm.provider);
      if (provider == null)
      {
        Log.e(getClass().getSimpleName(), "Provider introuvable : " + alarm.provider);
        return;
      }

      // Balise existante ?
      final Balise balise = provider.getBaliseById(alarm.idBalise);
      if (balise == null)
      {
        Log.e(getClass().getSimpleName(), "Balise introuvable : " + alarm.idBalise);
        return;
      }

      // Elaboration du texte
      final String pattern;
      if (alarm.checkNotificationVoixPerso)
      {
        // Perso
        pattern = (verifiee ? alarm.texteVerifieeNotificationVoixPerso : alarm.texteNonVerifieeNotificationVoixPerso);
      }
      else
      {
        // Defaut
        pattern = providersService.getResources().getString(verifiee ? R.string.alarm_notification_voix_text_verifiee : R.string.alarm_notification_voix_text_non_verifiee);
      }

      // Formatage
      final String speakText = AlarmUtils.formatAlarmText(providersService.getApplicationContext(), pattern, alarm, releve);

      // Parle !
      providersService.getVoiceService().speak(speakText, false, null);
    }

    /**
     * 
     * @param releves
     * @param idBalise
     * @return
     */
    private static Releve findReleve(final Collection<Releve> releves, final String idBalise)
    {
      for (final Releve releve : releves)
      {
        if (idBalise.equals(releve.id))
        {
          return releve;
        }
      }

      return null;
    }

    /**
     * 
     */
    private void doConfigUpdate()
    {
      // Initialisation
      Log.d(getClass().getSimpleName(), ">>> doConfigUpdate()");

      // Chargement des alarmes
      final List<BaliseAlarm> loadedAlarms = AlarmUtils.loadAlarms(providersService.getApplicationContext());
      synchronized (alarms)
      {
        alarms.clear();
        alarms.addAll(loadedAlarms);
      }

      // Fin
      Log.d(getClass().getSimpleName(), "<<< doConfigUpdate()");
    }

    /**
     * 
     * @return
     */
    private boolean canWait()
    {
      // Demandes d'enregistrement
      synchronized (steps)
      {
        return (steps.size() == 0);
      }
    }

    /**
     * 
     * @param step
     */
    private void postStep(final AlarmModeStep step)
    {
      synchronized (steps)
      {
        steps.add(step);
      }
      synchronized (this)
      {
        notify();
      }
    }

    /**
     * 
     * @param fullKey
     * @return
     */
    boolean isBaliseProviderNeededForAlarm(final String fullKey)
    {
      synchronized (alarms)
      {
        for (final BaliseAlarm alarm : alarms)
        {
          if (!AlarmUtils.isComplete(alarm))
          {
            continue;
          }
          if (alarm.provider.equals(fullKey))
          {
            return true;
          }
        }
      }

      return false;
    }

    /**
     * 
     */
    void configUpdate()
    {
      final AlarmModeStep step = new AlarmModeStep(AlarmModeStep.StepType.CONFIG_UPDATE, null, null, null);
      postStep(step);
    }

    /**
     * 
     * @param alarm
     */
    void activate(final BaliseAlarm alarm)
    {
      final AlarmModeStep step = new AlarmModeStep(AlarmModeStep.StepType.ACTIVATE, null, null, alarm);
      postStep(step);
    }

    /**
     * 
     * @param alarm
     */
    void deactivate(final BaliseAlarm alarm)
    {
      final AlarmModeStep step = new AlarmModeStep(AlarmModeStep.StepType.DEACTIVATE, null, null, alarm);
      postStep(step);
    }

    /**
     * 
     * @param forceReload
     * @return
     */
    boolean almostOneActiveAlarm(final boolean forceReload)
    {
      return AlarmUtils.almostOneActiveAlarm(providersService.getApplicationContext(), (forceReload ? null : alarms));
    }

    @Override
    public void onBalisesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      //Nothing
    }

    @Override
    public void onRelevesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      final AlarmModeStep step = new AlarmModeStep(AlarmModeStep.StepType.RELEVES_UPDATE, key, provider.getUpdatedReleves(), null);
      postStep(step);
    }

    @Override
    public void onBaliseProviderAdded(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderRemoved(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes)
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderUpdateStarted(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderUpdateEnded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
    {
      //Nothing
    }

    @Override
    public void onBaliseProvidersChanged()
    {
      //Nothing
    }

    @Override
    public boolean isBaliseProviderNeeded(final String key, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      return standardMode && providersService.alarmMode && isBaliseProviderNeededForAlarm(key);
    }

    @Override
    public void onBaliseProviderModesAdded(String key, BaliseProvider provider, BaliseProviderInfos infos, boolean standardModeAdded, List<BaliseProviderMode> addedModes, List<BaliseProviderMode> oldActiveModes)
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderModesRemoved(String key, BaliseProvider provider, BaliseProviderInfos infos, boolean standardModeRemoved, List<BaliseProviderMode> removedModes, List<BaliseProviderMode> oldActiveModes)
    {
      //Nothing
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class HistoryModeThread extends Thread implements BaliseProviderListener
  {
    private static final String         HISTORY_MODE_THREAD_WAKEUP_ACTION = "mobibalises.HISTORY_MODE_THREAD_WAKEUP_ACTION";
    private static final long           DELTA_WAKEUP                      = 10 * 60000;
    private static final String         HISTORY_FILE_BASE_DIR             = "history";
    private static final String         HISTORY_FILE_NAME_FORMAT          = "{0}.{1}";
    private static final String         HISTORY_TEMP_FILE_NAME_FORMAT     = "{0}.{1}.tmp";

    private ProvidersService            providersService;

    private File                        historyFileDir;

    private BroadcastReceiver           alarmReceiver;

    private long                        lastClean                         = System.currentTimeMillis() - DELTA_WAKEUP + 2 * 60000;
    private final List<HistoryModeStep> steps                             = new ArrayList<HistoryModeStep>();

    private boolean                     sdAvailable                       = true;

    /**
     * 
     * @author pedro.m
     */
    private static class HistoryModeStep
    {
      final BaliseProvider     provider;
      final String             providerKey;
      final Collection<Releve> releves;

      /**
       * 
       * @param provider
       * @param providerKey
       * @param releves
       */
      HistoryModeStep(final BaliseProvider provider, final String providerKey, final Collection<Releve> releves)
      {
        this.provider = provider;
        this.providerKey = providerKey;
        this.releves = releves;
      }

      @Override
      public String toString()
      {
        return providerKey + "<" + releves.size() + ">";
      }
    }

    /**
     * 
     * @param providersService
     */
    HistoryModeThread(final ProvidersService providersService)
    {
      // Super
      super(HistoryModeThread.class.getName());
      this.providersService = providersService;

      // Receiver alarmes
      alarmReceiver = new BroadcastReceiver()
      {
        @Override
        public void onReceive(final Context context, final Intent intent)
        {
          synchronized (HistoryModeThread.this)
          {
            HistoryModeThread.this.notify();
          }
        }
      };
    }

    @Override
    public void run()
    {
      // Initialisation
      initialize();

      // Boucle
      while (!isInterrupted())
      {
        synchronized (this)
        {
          if (canWait())
          {
            try
            {
              Log.d(getClass().getSimpleName(), ">>> wait");
              wait();
              Log.d(getClass().getSimpleName(), "<<< wait");
            }
            catch (final InterruptedException e)
            {
              Log.d(getClass().getSimpleName(), ">>> interrupt");
              Thread.currentThread().interrupt();
            }
          }
        }

        // Traitement
        doIt();
      }

      // Fin
      shutdown();

      // Notification de fin accomplie
      synchronized (this)
      {
        notifyAll();
      }
    }

    /**
     * 
     */
    private void doIt()
    {
      // Carte SD dispo ?
      synchronized (this)
      {
        if (!sdAvailable)
        {
          Log.d(getClass().getSimpleName(), "SDCard not available, skipping...");
          return;
        }
      }

      // Etape a traiter ?
      final HistoryModeStep step;
      synchronized (steps)
      {
        // Etape
        Log.d(getClass().getSimpleName(), "steps : " + steps);
        if (steps.size() > 0)
        {
          step = steps.remove(0);
        }
        else
        {
          step = null;
        }
      }

      // Traitement etape
      if (step != null)
      {
        recordReleves(step.provider, step.providerKey, step.releves);
        return; // Pour donner la priorite aux steps plutot qu'au clean
      }

      // Nettoyage
      if (lastClean + DELTA_WAKEUP < System.currentTimeMillis())
      {
        doClean();
        lastClean = System.currentTimeMillis();
      }
    }

    /**
     * 
     */
    private void doClean()
    {
      // Initialisations
      Log.d(getClass().getSimpleName(), ">>> doClean()");
      final Context context = providersService.getApplicationContext();
      final Resources resources = context.getResources();
      final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
      final long timeLimit = getHistoryTimeLimit(resources, sharedPreferences);

      // Liste des fichiers
      final File[] files = historyFileDir.listFiles();
      if (files != null)
      {
        for (final File file : files)
        {
          if (FileTimestampUtils.lastModified(file) < timeLimit)
          {
            Log.d(getClass().getSimpleName(), "deleting " + file.getName());
            FileTimestampUtils.deleteTimestampFile(file);
            file.delete();
          }
        }
      }

      // Fin
      Log.d(getClass().getSimpleName(), "<<< doClean()");
    }

    /**
     * 
     * @return
     */
    private PendingIntent getAlarmIntent()
    {
      final Intent intent = new Intent(HISTORY_MODE_THREAD_WAKEUP_ACTION);
      return PendingIntent.getBroadcast(providersService.getApplicationContext(), 0, intent, 0);
    }

    /**
     * 
     */
    void postHistoryModeAlarm()
    {
      final AlarmManager alarmManager = (AlarmManager)providersService.getSystemService(Context.ALARM_SERVICE);
      final PendingIntent alarmPendingIntent = getAlarmIntent();
      alarmManager.cancel(alarmPendingIntent);
      alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), DELTA_WAKEUP, alarmPendingIntent);
    }

    /**
     * 
     */
    void removeHistoryModeAlarm()
    {
      final AlarmManager alarmManager = (AlarmManager)providersService.getSystemService(Context.ALARM_SERVICE);
      final PendingIntent alarmPendingIntent = getAlarmIntent();
      alarmManager.cancel(alarmPendingIntent);
    }

    /**
     * 
     */
    private void shutdown()
    {
      removeHistoryModeAlarm();
      providersService.getApplicationContext().unregisterReceiver(alarmReceiver);
      alarmReceiver = null;
      providersService = null;
    }

    /**
     * 
     */
    private void initialize()
    {
      // Repertoire de sauvegarde
      historyFileDir = new File(ActivityCommons.MOBIBALISES_EXTERNAL_STORAGE_PATH, HISTORY_FILE_BASE_DIR);
      historyFileDir.mkdirs();
      refreshSDCardAvailability();

      // Retrait des alarmes eventuelles
      removeHistoryModeAlarm();

      // Enregistrement de l'ecoute d'alarme de reveil
      final IntentFilter filter = new IntentFilter(HISTORY_MODE_THREAD_WAKEUP_ACTION);
      providersService.getApplicationContext().registerReceiver(alarmReceiver, filter);
    }

    /**
     * 
     * @return
     */
    private boolean canWait()
    {
      // Carte SD
      if (!sdAvailable)
      {
        return true;
      }

      // Nettoyage
      if (lastClean + DELTA_WAKEUP < System.currentTimeMillis())
      {
        return false;
      }

      // Demandes d'enregistrement
      synchronized (steps)
      {
        return (steps.size() == 0);
      }
    }

    /**
     * 
     * @param provider
     * @param key
     * @param releves
     */
    private void postStep(final BaliseProvider provider, final String key, final Collection<Releve> releves)
    {
      if (releves.size() > 0)
      {
        synchronized (steps)
        {
          final Collection<Releve> relevesCopy = new ArrayList<Releve>();
          relevesCopy.addAll(releves);
          steps.add(new HistoryModeStep(provider, key, relevesCopy));
        }

        synchronized (this)
        {
          notify();
        }
      }
    }

    /**
     * 
     * @param historyTimeLimit
     * @param date
     * @return
     */
    private static boolean isTooOld(final long historyTimeLimit, final Date date)
    {
      return (date == null ? true : date.getTime() < historyTimeLimit);
    }

    /**
     * 
     * @param resources
     * @param sharedPreferences
     * @return
     */
    private static long getHistoryTimeLimit(final Resources resources, final SharedPreferences sharedPreferences)
    {
      final long historyDuration = 24 * 3600 * 1000 * (long)sharedPreferences.getInt(resources.getString(R.string.config_history_mode_duration_key), Integer.parseInt(resources.getString(R.string.config_history_mode_duration_default), 10));

      return Utils.toUTC(System.currentTimeMillis()) - historyDuration;
    }

    /**
     * 
     * @param provider
     * @param providerKey
     * @param baliseId
     * @param historyTimeLimit
     * @throws IOException
     */
    private void cleanRecord(final BaliseProvider provider, final String providerKey, final String baliseId, final long historyTimeLimit)
    {
      // Fichier
      final File recordFile = new File(historyFileDir, MessageFormat.format(HISTORY_FILE_NAME_FORMAT, providerKey, baliseId));

      // Existe ?
      if (!recordFile.exists() || !recordFile.isFile() || !recordFile.canRead())
      {
        return;
      }

      // Lecture du premier enregistrement
      FileInputStream fis = null;
      DataInputStream dis = null;
      boolean doClean = false;
      final Releve releve = provider.newReleve();
      try
      {
        fis = new FileInputStream(recordFile);
        dis = new DataInputStream(fis);

        releve.loadSaveable(dis);
        doClean = isTooOld(historyTimeLimit, releve.date);
        Log.d(getClass().getSimpleName(), "File " + recordFile.getName() + " needs cleaning : " + doClean);
      }
      catch (final IOException ioe)
      {
        Log.d(getClass().getSimpleName(), "File " + recordFile.getName() + " is empty or in error, deleting it");
        doClean = false;
        FileTimestampUtils.deleteTimestampFile(recordFile);
        recordFile.delete();
        try
        {
          if (dis != null)
          {
            dis.close();
            dis = null;
          }
          if (fis != null)
          {
            fis.close();
            fis = null;
          }
        }
        catch (final IOException ioe2)
        {
          Log.w(getClass().getSimpleName(), ioe2.getMessage(), ioe2);
        }
      }

      // Nettoyage si besoin
      if (doClean && (fis != null) && (dis != null))
      {
        // Initialisations
        final File recordTempFile = new File(historyFileDir, MessageFormat.format(HISTORY_TEMP_FILE_NAME_FORMAT, providerKey, baliseId));
        Log.d(getClass().getSimpleName(), "cleaning to recordTempFile : " + recordTempFile.getName());
        FileOutputStream fos = null;
        DataOutputStream dos = null;
        boolean ok = false;
        Date lastDate = null;

        try
        {
          // Fichier de sortie temporaire
          fos = new FileOutputStream(recordTempFile, false);
          dos = new DataOutputStream(fos);

          // Copie des releves non trop vieux
          boolean endOfFile = false;
          while (!endOfFile)
          {
            // Ecriture si date ok
            if (!isTooOld(historyTimeLimit, releve.date))
            {
              lastDate = releve.date;
              releve.saveSaveable(dos);
            }
            else
            {
              Log.d(getClass().getSimpleName(), "cleaning releve of " + releve.date + " for " + recordFile.getName());
            }

            // Next
            try
            {
              releve.loadSaveable(dis);
            }
            catch (final EOFException oefe)
            {
              endOfFile = true;
            }
          }

          // Fin
          ok = true;
        }
        catch (final IOException ioe)
        {
          Log.w(getClass().getSimpleName(), ioe.getMessage(), ioe);
        }
        finally
        {
          try
          {
            // Fermetures
            dis.close();
            fis.close();
            if (dos != null)
            {
              dos.close();
            }
            if (fos != null)
            {
              fos.close();
            }

            // Suppression du fichier original et remplacement par le fichier temporaire epure
            if (ok)
            {
              FileTimestampUtils.deleteTimestampFile(recordFile);
              if (!recordFile.delete())
              {
                throw new IOException("Impossible de supprimer le fichier " + recordFile.getName());
              }
              FileTimestampUtils.renameTimestampFileTo(recordTempFile, recordFile);
              if (!recordTempFile.renameTo(recordFile))
              {
                throw new IOException("Impossible de renommer le fichier " + recordTempFile.getName() + " en " + recordFile.getName());
              }
            }
            else
            {
              FileTimestampUtils.deleteTimestampFile(recordFile);
              if (!recordTempFile.delete())
              {
                throw new IOException("Impossible de supprimer le fichier temporaire : " + recordTempFile.getName());
              }
            }

            // MAJ du timestamp du fichier nettoye a la date du dernier enregistrement
            if (lastDate != null)
            {
              Log.d(getClass().getSimpleName(), "adjusting timestamp to " + lastDate + " for " + recordFile.getName());
              if (!FileTimestampUtils.setLastModified(recordFile, lastDate.getTime()))
              {
                throw new IOException("Impossible de modifier le timestamp du fichier : " + recordFile.getName());
              }
            }
          }
          catch (final IOException ioe)
          {
            Log.w(getClass().getSimpleName(), ioe.getMessage(), ioe);
          }
        }
      }
    }

    /**
     * 
     * @param providerKey
     * @param releve
     * @throws IOException
     */
    private void recordReleve(final String providerKey, final Releve releve) throws IOException
    {
      // Date
      if (releve.date == null)
      {
        Log.d(getClass().getSimpleName(), "No date for releve : " + releve);
        return;
      }

      // Fichier
      final File recordFile = new File(historyFileDir, MessageFormat.format(HISTORY_FILE_NAME_FORMAT, providerKey, releve.id));

      // Deja enregistre ce releve ?
      if (releve.date.getTime() <= FileTimestampUtils.lastModified(recordFile))
      {
        Log.d(getClass().getSimpleName(), recordFile.getName() + " : releve of " + releve.date + " already recorded");
        return;
      }

      // Flux
      FileOutputStream fos = null;
      DataOutputStream dos = null;

      try
      {
        // Flux
        fos = new FileOutputStream(recordFile, true);
        dos = new DataOutputStream(fos);

        // Enregistrement
        releve.saveSaveable(dos);
        Log.d(getClass().getSimpleName(), recordFile.getName() + " : releve of " + releve.date + " saved");
      }
      finally
      {
        // Fermetures
        if (dos != null)
        {
          dos.close();
        }
        if (fos != null)
        {
          fos.close();
        }

        // Timestamp du fichier
        FileTimestampUtils.setLastModified(recordFile, releve.date.getTime());
      }
    }

    /**
     * 
     * @param provider
     * @param key
     * @param releves
     */
    private void recordReleves(final BaliseProvider provider, final String key, final Collection<Releve> releves)
    {
      // Initialisations
      final Context context = providersService.getApplicationContext();
      final Resources resources = context.getResources();
      final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);

      // Mode historique actif ?
      if (!isHistoryModeOn(resources, sharedPreferences))
      {
        return;
      }

      // Limite historique
      final long historyTimeLimit = getHistoryTimeLimit(resources, sharedPreferences);

      // Enregistrement
      for (final Releve releve : releves)
      {
        try
        {
          cleanRecord(provider, key, releve.id, historyTimeLimit);
          if (!isTooOld(historyTimeLimit, releve.date))
          {
            recordReleve(key, releve);
          }
        }
        catch (final IOException ioe)
        {
          Log.w(getClass().getSimpleName(), ioe.getMessage(), ioe);
        }
      }
    }

    /**
     * 
     * @param provider
     * @param providerKey
     * @param baliseId
     * @return
     * @throws IOException
     */
    Collection<Releve> getHistory(final BaliseProvider provider, final String providerKey, final String baliseId) throws IOException
    {
      // Fichier
      final File recordFile = new File(historyFileDir, MessageFormat.format(HISTORY_FILE_NAME_FORMAT, providerKey, baliseId));

      // Existe ?
      if (!recordFile.exists() || !recordFile.isFile() || !recordFile.canRead())
      {
        return null;
      }

      // Lecture
      FileInputStream fis = null;
      DataInputStream dis = null;
      try
      {
        // Initialisations
        fis = new FileInputStream(recordFile);
        dis = new DataInputStream(fis);
        final List<Releve> releves = new ArrayList<Releve>();

        // Lecture
        boolean end = false;
        while (!end)
        {
          try
          {
            final Releve releve = provider.newReleve();
            releve.loadSaveable(dis);
            releves.add(releve);
          }
          catch (final EOFException eofe)
          {
            end = true;
          }
        }

        return releves;
      }
      finally
      {
        try
        {
          if (dis != null)
          {
            dis.close();
            dis = null;
          }
          if (fis != null)
          {
            fis.close();
            fis = null;
          }
        }
        catch (final IOException ioe)
        {
          Log.w(getClass().getSimpleName(), ioe.getMessage(), ioe);
        }
      }
    }

    /**
     * 
     * @param providerKey
     * @param baliseId
     * @param releves
     * @throws IOException
     */
    void recordHistory(final String providerKey, final String baliseId, final Collection<Releve> releves) throws IOException
    {
      // Fichier
      final File recordFile = new File(historyFileDir, MessageFormat.format(HISTORY_FILE_NAME_FORMAT, providerKey, baliseId));
      long lastModified = 0;

      // Flux
      FileOutputStream fos = null;
      DataOutputStream dos = null;

      try
      {
        // Flux
        fos = new FileOutputStream(recordFile, false);
        dos = new DataOutputStream(fos);

        // Enregistrement
        for (final Releve releve : releves)
        {
          releve.saveSaveable(dos);
          lastModified = releve.date.getTime();
        }
      }
      finally
      {
        // Fermetures
        if (dos != null)
        {
          dos.close();
        }
        if (fos != null)
        {
          fos.close();
        }

        // Timestamp du fichier
        FileTimestampUtils.setLastModified(recordFile, lastModified);
      }
    }

    /**
     * 
     */
    void refreshSDCardAvailability()
    {
      synchronized (this)
      {
        sdAvailable = historyFileDir.exists() && historyFileDir.isDirectory() && historyFileDir.canWrite();
        Log.d(getClass().getSimpleName(), "SDCard is now " + (sdAvailable ? "available" : "unavailable"));
        notify();
      }
    }

    @Override
    public void onBalisesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      Log.d(getClass().getSimpleName(), "onBalisesUpdate(" + key + ", ...)");
    }

    @Override
    public void onRelevesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      // Initialisations
      if (standardMode)
      {
        Log.d(getClass().getSimpleName(), ">>> onRelevesUpdate(" + key + ", ...)");
        postStep(provider, key, provider.getUpdatedReleves());
        Log.d(getClass().getSimpleName(), "<<< onRelevesUpdate(" + key + ", ...)");
      }
    }

    @Override
    public void onBaliseProviderAdded(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      Log.d(getClass().getSimpleName(), ">>> onBaliseProviderAdded(" + key + ", ...)");
      if (standardMode)
      {
        postStep(provider, key, provider.getReleves());
      }
      Log.d(getClass().getSimpleName(), "<<< onBaliseProviderAdded(" + key + ", ...)");
    }

    @Override
    public void onBaliseProviderRemoved(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes)
    {
      // Nothing
    }

    @Override
    public void onBaliseProviderUpdateStarted(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
    {
      // Nothing
    }

    @Override
    public void onBaliseProviderUpdateEnded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
    {
      // Nothing
    }

    @Override
    public boolean isBaliseProviderNeeded(final String key, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      return standardMode;
    }

    @Override
    public void onBaliseProvidersChanged()
    {
      // Nothing
    }

    @Override
    public void onBaliseProviderModesAdded(String key, BaliseProvider provider, BaliseProviderInfos infos, boolean standardModeAdded, List<BaliseProviderMode> addedModes, List<BaliseProviderMode> oldActiveModes)
    {
      if (standardModeAdded)
      {
        onBaliseProviderAdded(key, provider, true, infos.getActiveModes());
      }
    }

    @Override
    public void onBaliseProviderModesRemoved(String key, BaliseProvider provider, BaliseProviderInfos infos, boolean standardModeRemoved, List<BaliseProviderMode> removedModes, List<BaliseProviderMode> oldActiveModes)
    {
      //Nothing
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class FlightModeThread extends Thread implements BaliseProviderListener, LocationListener
  {
    // Constantes
    private static final int           FLIGHT_MODE_LOCATION_MIN_TIME     = 120000;
    private static final float         FLIGHT_MODE_LOCATION_MIN_DISTANCE = 500;

    // Liste des balises a proximite
    private final List<BaliseFavorite> proches                           = new ArrayList<BaliseFavorite>();
    private final List<BaliseFavorite> nouvellesProches                  = new ArrayList<BaliseFavorite>();
    private boolean                    firstOnLocationChanged            = true;

    // Donnees mises a jour
    private final List<String>         updatedProviderKeys               = new ArrayList<String>();

    private ProvidersService           providersService;

    /**
     * 
     * @param providersService
     */
    FlightModeThread(final ProvidersService providersService)
    {
      // Super
      super(FlightModeThread.class.getName());
      this.providersService = providersService;

      // Ajout des listeners
      providersService.addBaliseProviderListener(this, false);
    }

    @Override
    public void run()
    {
      while (!isInterrupted())
      {
        synchronized (this)
        {
          if (canWait())
          {
            try
            {
              Log.d(getClass().getSimpleName(), ">>> wait");
              wait();
              Log.d(getClass().getSimpleName(), "<<< wait");
            }
            catch (final InterruptedException e)
            {
              Log.d(getClass().getSimpleName(), ">>> interrupt");
              Thread.currentThread().interrupt();
            }
          }
        }

        // Si fin
        if (isInterrupted())
        {
          break;
        }

        // Traitement du changement de position
        final boolean computeLocationChanged;
        synchronized (nouvellesProches)
        {
          computeLocationChanged = (nouvellesProches.size() > 0);
        }
        if (computeLocationChanged)
        {
          Log.d(getClass().getSimpleName(), ">>> computing location changed for new balises");
          computeLocationChanged();
          Log.d(getClass().getSimpleName(), "<<< computing location changed for new balises");
        }

        // Traitement des mises a jour
        final String updatedProviderKey;
        synchronized (updatedProviderKeys)
        {
          updatedProviderKey = (updatedProviderKeys.size() > 0 ? updatedProviderKeys.remove(0) : null);
        }
        if (updatedProviderKey != null)
        {
          Log.d(getClass().getSimpleName(), ">>> computing relevesUpdate for " + updatedProviderKey);
          computeRelevesUpdate(updatedProviderKey);
          Log.d(getClass().getSimpleName(), "<<< computing relevesUpdate for " + updatedProviderKey);
        }
      }

      // Fin
      providersService.removeBaliseProviderListener(this, false);
      providersService = null;
    }

    /**
     * 
     * @return
     */
    private boolean canWait()
    {
      synchronized (updatedProviderKeys)
      {
        synchronized (nouvellesProches)
        {
          return !providersService.flightMode || ((updatedProviderKeys.size() == 0) && (nouvellesProches.size() == 0));
        }
      }
    }

    /**
     * 
     * @param updatedProviderKey
     */
    private void computeRelevesUpdate(final String updatedProviderKey)
    {
      // Seulement si necessaire (pas d'activity ou activity demandeuse)
      final boolean speakOnForeground = providersService.theSharedPreferences.getBoolean(providersService.theResources.getString(R.string.config_flight_mode_foreground_speech_key),
          Boolean.parseBoolean(providersService.theResources.getString(R.string.config_flight_mode_foreground_speech_default)));
      if (providersService.activityOnForeground && !speakOnForeground)
      {
        Log.d(getClass().getSimpleName(), "Activity on foreground and no speak on foreground configured, returning...");
        return;
      }

      // Recuperation des balises proches
      final List<BaliseFavorite> finalProches = new ArrayList<BaliseFavorite>();
      synchronized (proches)
      {
        finalProches.addAll(proches);
      }

      // Parmi les releves effectivement mis a jour
      final BaliseProvider updatedProvider = providersService.getBaliseProvider(updatedProviderKey);
      for (final Releve releve : updatedProvider.getUpdatedReleves())
      {
        // Si le releve fait partie des balises proches : synthese vocale
        final BaliseFavorite baliseReleve = new BaliseFavorite(updatedProviderKey, releve.id, providersService.favoritesService);
        final int index = finalProches.indexOf(baliseReleve);
        if (index > 0)
        {
          final BaliseFavorite baliseProche = finalProches.get(index);
          final Balise balise = updatedProvider.getBaliseById(releve.id);
          Log.d(getClass().getSimpleName(), "Flight mode speaking for updated " + updatedProviderKey + "." + releve.id + " : " + balise.nom + " / " + releve.date + " / " + baliseProche.getDistance() + "km");
          providersService.getVoiceService().speakBaliseReleve(balise, releve, false, null);
          providersService.getAnalyticsService().trackEvent(AnalyticsService.CAT_FLIGHT_MODE, AnalyticsService.ACT_FLIGHT_MODE_SPEECH, baliseReleve.getId());
        }
      }
    }

    /**
     * 
     */
    private void computeLocationChanged()
    {
      // Recuperation des nouvelles balises proches
      final List<BaliseFavorite> finalNouvellesProches = new ArrayList<BaliseFavorite>();
      synchronized (nouvellesProches)
      {
        finalNouvellesProches.addAll(nouvellesProches);
        nouvellesProches.clear();
      }

      // Seulement si necessaire (pas d'activity ou activity demandeuse)
      final boolean speakOnForeground = providersService.theSharedPreferences.getBoolean(providersService.theResources.getString(R.string.config_flight_mode_foreground_speech_key),
          Boolean.parseBoolean(providersService.theResources.getString(R.string.config_flight_mode_foreground_speech_default)));
      if (providersService.activityOnForeground && !speakOnForeground)
      {
        Log.d(getClass().getSimpleName(), "Activity on foreground and no speak on foreground configured, returning...");
        return;
      }

      // Pour chaque nouvelle balise
      for (final BaliseFavorite favorite : finalNouvellesProches)
      {
        final BaliseProvider provider = providersService.getBaliseProvider(favorite.getProviderId());
        final Balise balise = provider.getBaliseById(favorite.getBaliseId());
        final Releve releve = provider.getReleveById(favorite.getBaliseId());
        Log.d(getClass().getSimpleName(), "Flight mode speaking for new one " + favorite + " " + (balise == null ? "?" : balise.nom));
        providersService.getVoiceService().speakBaliseReleve(balise, releve, false, null);
        providersService.getAnalyticsService().trackEvent(AnalyticsService.CAT_FLIGHT_MODE, AnalyticsService.ACT_FLIGHT_MODE_SPEECH, favorite.getId());
      }
    }

    @Override
    public void onBalisesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      //Nothing
    }

    @Override
    public void onRelevesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      // Standard ?
      if (!standardMode)
      {
        return;
      }

      // Mode vol ?
      if (!providersService.flightMode)
      {
        return;
      }

      // Sauvegarde du provider
      synchronized (updatedProviderKeys)
      {
        if (!updatedProviderKeys.contains(key))
        {
          updatedProviderKeys.add(key);
        }
      }

      // Notification
      synchronized (this)
      {
        notify();
      }
    }

    @Override
    public void onBaliseProviderAdded(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderRemoved(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes)
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderUpdateStarted(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderUpdateEnded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
    {
      //Nothing
    }

    @Override
    public void onBaliseProvidersChanged()
    {
      //Nothing
    }

    @Override
    public boolean isBaliseProviderNeeded(final String key, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      return standardMode && providersService.flightMode;
    }

    @Override
    public void onLocationChanged(final Location location)
    {
      // Recuperation des balises proches
      final List<BaliseFavorite> inProches = FullLocationService.getProximityBalises(providersService, location, providersService);
      Log.d(getClass().getSimpleName(), "onLocationChanged, proches = " + inProches);

      // Sauvegarde des balises proches
      synchronized (proches)
      {
        synchronized (nouvellesProches)
        {
          // Detection des nouvelles balises proches
          nouvellesProches.clear();
          for (final BaliseFavorite favorite : inProches)
          {
            if (!proches.contains(favorite))
            {
              nouvellesProches.add(favorite);
            }
          }

          // MAJ des balises proches
          proches.clear();
          proches.addAll(inProches);
        }
      }

      // Log
      Log.d(getClass().getSimpleName(), "onLocationChanged, nouvelles proches = " + nouvellesProches);

      // Mode vol
      if (!providersService.flightMode)
      {
        return;
      }

      // Si premiere mise a jour des positions : on ne parle pas
      if (firstOnLocationChanged)
      {
        // Log
        Log.d(getClass().getSimpleName(), "firstOnLocationChanged => don't talk");
        return;
      }

      // Notification
      synchronized (this)
      {
        notify();
      }
    }

    @Override
    public void onBaliseProviderModesAdded(String key, BaliseProvider provider, BaliseProviderInfos infos, boolean standardModeAdded, List<BaliseProviderMode> addedModes, List<BaliseProviderMode> oldActiveModes)
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderModesRemoved(String key, BaliseProvider provider, BaliseProviderInfos infos, boolean standardModeRemoved, List<BaliseProviderMode> removedModes, List<BaliseProviderMode> oldActiveModes)
    {
      //Nothing
    }

    @Override
    public void onProviderDisabled(final String provider)
    {
      //Nothing
    }

    @Override
    public void onProviderEnabled(final String provider)
    {
      //Nothing
    }

    @Override
    public void onStatusChanged(final String provider, final int status, final Bundle extras)
    {
      //Nothing
    }

    /**
     * 
     * @param newFlightmode
     */
    void onFlightModeChanged(final boolean newFlightMode)
    {
      if (newFlightMode)
      {
        firstOnLocationChanged = true;
      }
    }
  }

  @Override
  protected void initCommons()
  {
    super.initCommons();
    FullActivityCommons.init(getApplicationContext());
  }

  @Override
  protected void initLocationService()
  {
    locationService = new FullLocationService(this, getApplicationContext());
  }

  @Override
  public void onStart(final Intent intent, final int startId)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onStart(" + intent + ", ...) " + Thread.currentThread());
    super.onStart(intent, startId);

    // Synchro des widgets
    BalisesWidgets.synchronizeWidgets(this, this, null);

    // Gestion du mode historique
    final boolean histoMode = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_HISTORY_MODE, false);
    if (histoMode)
    {
      // Reglage
      setHistoryMode(histoMode);
    }

    // Notification mode historique/alarme/widget parlant si demarre depuis widget
    final boolean startedFromActivity = (intent == null ? false : intent.getBooleanExtra(STARTED_FROM_ACTIVITY, false));
    if (!startedFromActivity && !activityOnForeground)
    {
      FullActivityCommons.addHistoryModeNotification(getApplicationContext());
      FullActivityCommons.addSpeakBlackWidgetNotification(getApplicationContext());
      FullActivityCommons.addAlarmModeNotification(getApplicationContext());
    }

    // Gestion du mode vol
    if ((intent != null) && (intent.getExtras() != null))
    {
      final boolean intentFlightMode = intent.getExtras().getBoolean(EXTRA_FLIGHT_MODE_START, false);
      if (intentFlightMode)
      {
        setFlightMode(true);
        FullActivityCommons.addFlightModeNotification(getApplicationContext(), null, true);
      }
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onStart() " + Thread.currentThread());
  }

  @Override
  protected void onInitDone()
  {
    // Super
    super.onInitDone();

    // Gestion du mode alarme
    final boolean innerAlarmMode = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_ALARM_MODE, false);
    if (innerAlarmMode)
    {
      // Reglage
      setAlarmMode(innerAlarmMode);
    }

    // Notification aux widgets
    BalisesWidgets.onServiceStartCompleted(ProvidersService.this, ProvidersService.this);
  }

  /**
   * 
   */
  private void initVoice()
  {
    // Initialisations
    voiceService = new VoiceService(getApplicationContext(), theSharedPreferences);

    // Gestion des widgets
    BalisesWidgets.manageVoiceClient(getApplicationContext(), this);
  }

  /**
   * 
   */
  private void initFavorites()
  {
    favoritesService = new FavoritesService(getApplicationContext(), theResources, this);
  }

  @Override
  protected void fireBalisesUpdate(final String fullKey, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    super.fireBalisesUpdate(fullKey, provider, standardMode, activeModes);

    // Widgets
    if (standardMode)
    {
      BalisesWidgets.onBalisesUpdate(this, fullKey, provider, this);
    }
  }

  @Override
  protected void fireRelevesUpdate(final String fullKey, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    super.fireRelevesUpdate(fullKey, provider, standardMode, activeModes);

    // Widgets
    if (standardMode)
    {
      BalisesWidgets.onRelevesUpdate(this, fullKey, provider, this);
    }
  }

  @Override
  protected void fireBaliseProviderAdded(final String fullKey, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    super.fireBaliseProviderAdded(fullKey, provider, standardMode, activeModes);

    // Widgets
    if (standardMode)
    {
      BalisesWidgets.onBaliseProviderAdded(this, fullKey, provider, this);
    }
  }

  @Override
  protected void fireBaliseProviderRemoved(final String fullKey, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes)
  {
    super.fireBaliseProviderRemoved(fullKey, provider, infos, wasStandardMode, oldActiveModes);

    // Widgets
    if (wasStandardMode)
    {
      BalisesWidgets.onBaliseProviderRemoved(this, fullKey, provider, this);
    }
  }

  @Override
  protected void fireBaliseProviderUpdateStarted(final String fullKey, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
  {
    super.fireBaliseProviderUpdateStarted(fullKey, provider, infos, standardMode);

    // Widgets
    if (standardMode)
    {
      BalisesWidgets.onBaliseProviderUpdateStarted(this, fullKey, provider, infos, this);
    }
  }

  @Override
  protected void fireBaliseProviderUpdateEnded(final String fullKey, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
  {
    super.fireBaliseProviderUpdateEnded(fullKey, provider, infos, standardMode);

    // Widgets
    if (standardMode)
    {
      BalisesWidgets.onBaliseProviderUpdateEnded(this, fullKey, provider, infos, this);
    }
  }

  @Override
  protected void fireBaliseProviderModesAdded(final String fullKey, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardModeAdded, final List<BaliseProviderMode> addedModes,
      final List<BaliseProviderMode> oldActiveModes)
  {
    super.fireBaliseProviderModesAdded(fullKey, provider, infos, standardModeAdded, addedModes, oldActiveModes);

    // Widgets
    if (standardModeAdded)
    {
      BalisesWidgets.onBaliseProviderAdded(getApplicationContext(), fullKey, provider, this);
    }
  }

  @Override
  protected void fireBaliseProviderModesRemoved(final String fullKey, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardModeRemoved, final List<BaliseProviderMode> removedModes,
      final List<BaliseProviderMode> oldActiveModes)
  {
    super.fireBaliseProviderModesRemoved(fullKey, provider, infos, standardModeRemoved, removedModes, oldActiveModes);

    // Widgets
    if (standardModeRemoved)
    {
      BalisesWidgets.onBaliseProviderRemoved(getApplicationContext(), fullKey, provider, this);
    }
  }

  @Override
  public void onCreate()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onCreate()");

    // Parent
    super.onCreate();

    // Initialisations
    theResources = getApplicationContext().getResources();
    theSharedPreferences = ActivityCommons.getSharedPreferences(getApplicationContext());

    // Initialisation de la synthese vocale
    initVoice();

    // Initialisations des favoris
    initFavorites();

    // Initialisation du mode vol
    initFlightMode();

    // Initialisation du mode historique
    initHistoryMode();

    // Initialisation du mode alarme
    initAlarmMode();

    // Initialisation de l'ecoute des essais d'arret
    initShutdownTryReceiver();

    // Initialisation de l'ecoute des click sur widget
    initWidgetTapReceiver();

    // Initialisation de l'ecoute des click sur widget
    initAlarmTapReceiver();

    // Initialisation de l'ecoute du controle du mode vol
    initFlightModeControlerReceiver();

    // Alarmes demarrage/arret
    StartupReceiver.manageStartupShutdownAlarms(getApplicationContext(), true, false);

    // Ecoute SDCard
    registerSDCardListener();

    // Verification de la license
    licenseOk = !FullActivityCommons.needsLicenseCheck(this);
    if (!licenseOk)
    {
      licenseChecker = FullActivityCommons.initLicenseChecker(this, this);
      licenseChecker.checkAccess();
    }
    else
    {
      startInitThread();
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
  }

  /**
   * 
   */
  private void initFlightMode()
  {
    // Demarrage du thread
    if (flightModeThread != null)
    {
      shutdownFlightMode();
    }
    flightModeThread = new FlightModeThread(this);
    flightModeThread.start();
  }

  /**
   * 
   */
  private void initHistoryMode()
  {
    // Demarrage du Thread
    if (historyModeThread != null)
    {
      shutdownHistoryMode();
    }
    historyModeThread = new HistoryModeThread(this);
    historyModeThread.start();
  }

  /**
   * 
   */
  private void initAlarmMode()
  {
    // Demarrage du Thread
    if (alarmModeThread != null)
    {
      shutdownAlarmMode();
    }
    alarmModeThread = new AlarmModeThread(this);
    alarmModeThread.start();
  }

  /**
   * 
   */
  private void initShutdownTryReceiver()
  {
    shutdownTryReceiver = new ShutdownTryReceiver(this);
    final IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_SHUTDOWN_TRY);
    getApplicationContext().registerReceiver(shutdownTryReceiver, filter);
  }

  /**
   * 
   */
  private void initWidgetTapReceiver()
  {
    widgetTapReceiver = new WidgetTapReceiver(this);
    final IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_WIDGET_TAP);
    getApplicationContext().registerReceiver(widgetTapReceiver, filter);
  }

  /**
   * 
   */
  private void initAlarmTapReceiver()
  {
    alarmTapReceiver = new AlarmTapReceiver(this);
    final IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_ALARM_TAP);
    getApplicationContext().registerReceiver(alarmTapReceiver, filter);
  }

  /**
   *
   */
  private void initFlightModeControlerReceiver()
  {
    flightModeControlerReceiver = new FlightModeControlerReceiver(this);
    final IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_FLIGHT_MODE_STOP);
    getApplicationContext().registerReceiver(flightModeControlerReceiver, filter);
  }

  @Override
  public void onDestroy()
  {
    // Parent
    Log.d(getClass().getSimpleName(), ">>> onDestroy()");
    super.onDestroy();

    // Notification mode vol et historique
    FullActivityCommons.removeFlightModeNotification(getApplicationContext());
    FullActivityCommons.removeHistoryModeNotification(getApplicationContext());
    FullActivityCommons.removeSpeakBlackWidgetNotification(getApplicationContext());
    FullActivityCommons.removeAlarmModeNotification(getApplicationContext());

    // Speaker
    voiceService.shutdown();

    // Favoris
    favoritesService.shutdown();

    // Mode vol
    shutdownFlightMode();

    // Mode historique
    shutdownHistoryMode();

    // Mode alarme
    shutdownAlarmMode();

    // Fin de l'ecoute des demandes d'arret
    shutdownShutdownTryReceiver();

    // Fin de l'ecoute des click sur widget
    shutdownWidgetTapReceiver();

    // Fin de l'ecoute des click sur alarme
    shutdownAlarmTapReceiver();

    // Fin de l'ecoute pour le mode vol
    shutdownFlightModeControlerReceiver();

    // Fin de l'ecoute SDCard
    unregisterSDCardListener();

    // License
    if (licenseChecker != null)
    {
      licenseChecker.onDestroy();
    }

    // Fin des attentes sur license
    synchronized (licenseLock)
    {
      finishing = true;
      licenseLock.notifyAll();
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  /**
   * 
   */
  private void shutdownFlightMode()
  {
    // Fin du Thread
    flightModeThread.interrupt();
    ThreadUtils.join(flightModeThread);
    flightModeThread = null;
  }

  /**
   * 
   */
  private void shutdownHistoryMode()
  {
    // Fin du Thread
    historyModeThread.interrupt();
    ThreadUtils.join(historyModeThread);
    historyModeThread = null;
  }

  /**
   * 
   */
  private void shutdownAlarmMode()
  {
    // Fin du Thread
    removeBaliseProviderListener(alarmModeThread, false);
    alarmModeThread.interrupt();
    ThreadUtils.join(alarmModeThread);
    alarmModeThread = null;
  }

  /**
   * 
   */
  private void shutdownShutdownTryReceiver()
  {
    getApplicationContext().unregisterReceiver(shutdownTryReceiver);
    shutdownTryReceiver.onShutdown();
    shutdownTryReceiver = null;
  }

  /**
   * 
   */
  private void shutdownWidgetTapReceiver()
  {
    getApplicationContext().unregisterReceiver(widgetTapReceiver);
    widgetTapReceiver.onShutdown();
    widgetTapReceiver = null;
  }

  /**
   * 
   */
  private void shutdownAlarmTapReceiver()
  {
    getApplicationContext().unregisterReceiver(alarmTapReceiver);
    alarmTapReceiver.onShutdown();
    alarmTapReceiver = null;
  }

  /**
   * 
   */
  private void shutdownFlightModeControlerReceiver()
  {
    getApplicationContext().unregisterReceiver(flightModeControlerReceiver);
    flightModeControlerReceiver.onShutdown();
    flightModeControlerReceiver = null;
  }

  @Override
  protected void onScreenOn()
  {
    super.onScreenOn();

    // Rafraichissement des widgets
    BalisesWidgets.synchronizeWidgets(this, this, null);
  }

  @Override
  protected void onScreenChanged()
  {
    // Parent
    super.onScreenChanged();

    // Gestion du service de localisation
    getFullLocationService().setScreenOff(screenOff);

    // Gestion de la synthe vocale pour les widgets
    BalisesWidgets.manageVoiceClient(getApplicationContext(), this);
  }

  @Override
  protected void onNetworkChanged()
  {
    super.onNetworkChanged();

    // Gestion de la synthe vocale pour les widgets
    BalisesWidgets.manageVoiceClient(getApplicationContext(), this);

    // Widgets
    BalisesWidgets.synchronizeWidgets(this, this, null);
  }

  @Override
  public void allow(final int reason)
  {
    // Reponse license OK
    synchronized (licenseLock)
    {
      licenseOk = true;
      licenseLock.notifyAll();
    }

    // Demarrage init
    startInitThread();
  }

  @Override
  public void dontAllow(final int reason)
  {
    licenseKo(reason);
  }

  @Override
  public void applicationError(final int errorCode)
  {
    licenseKo(errorCode);
  }

  /**
   * 
   * @param code
   */
  @SuppressWarnings("unused")
  private void licenseKo(final int code)
  {
    // License
    synchronized (licenseLock)
    {
      licenseOk = false;
      licenseLock.notifyAll();
    }
  }

  @Override
  public void waitForInit() throws InterruptedException
  {
    // Attente normale
    super.waitForInit();

    // License
    synchronized (licenseLock)
    {
      // Attente de la verification de license
      while (!licenseOk && !finishing)
      {
        try
        {
          // Attente
          Log.d(getClass().getSimpleName(), ">>> licenseLock.wait()");
          licenseLock.wait();
          Log.d(getClass().getSimpleName(), "<<< licenseLock.wait()");

          // Fin : interruption
          if (finishing)
          {
            throw new InterruptedException();
          }
        }
        catch (final InterruptedException ie)
        {
          Log.w(getClass().getSimpleName(), "waitForInit interrupted / " + Thread.currentThread());
          Thread.currentThread().interrupt();
          throw ie;
        }
      }
    }
  }

  @Override
  public boolean isFlightMode()
  {
    return flightMode;
  }

  @Override
  public void setFlightMode(final boolean flightMode)
  {
    // Changement ?
    if (this.flightMode == flightMode)
    {
      return;
    }

    // Enregistrement du flag
    this.flightMode = flightMode;

    // Notification au service de synthese vocale
    if (flightMode)
    {
      voiceService.registerVoiceClient(VoiceService.FLIGHT_MODE_VOICE_CLIENT);
    }
    else
    {
      voiceService.unregisterVoiceClient(VoiceService.FLIGHT_MODE_VOICE_CLIENT);
    }

    // Notification au thread du mode vol
    flightModeThread.onFlightModeChanged(flightMode);

    // Arret de la synthese vocale
    if (!flightMode)
    {
      voiceService.stopSpeaking(true);
    }

    // Localisation
    if (flightMode)
    {
      final boolean useGps = sharedPreferences.getBoolean(resources.getString(R.string.config_flight_mode_use_gps_key), Boolean.parseBoolean(resources.getString(R.string.config_flight_mode_use_gps_default)));
      getFullLocationService().addListener(flightModeThread, useGps, FlightModeThread.FLIGHT_MODE_LOCATION_MIN_TIME, FlightModeThread.FLIGHT_MODE_LOCATION_MIN_DISTANCE, false);
    }
    else
    {
      getFullLocationService().removeListener(flightModeThread, false);
    }

    // Statistiques
    analyticsService.trackEvent(AnalyticsService.CAT_FLIGHT_MODE, flightMode ? AnalyticsService.ACT_FLIGHT_MODE_START : AnalyticsService.ACT_FLIGHT_MODE_STOP, null);
  }

  @Override
  protected boolean canWait(final String fullKey, final BaliseProviderInfos infos)
  {
    // Pas de reseau => endormissement autorise
    if (networkOff)
    {
      return true;
    }

    // Mode vol => endormissement interdit
    if (isFlightMode())
    {
      return false;
    }

    // Mode historique => endormissement interdit
    if (isHistoryModeOn(getApplicationContext()))
    {
      return false;
    }

    // Mode alarme => endormissement interdit
    if (isAlarmMode() && (alarmModeThread != null) && alarmModeThread.isBaliseProviderNeededForAlarm(fullKey))
    {
      return false;
    }

    // Besoin pour un widget => endormissement interdit
    if (BalisesWidgets.isBaliseProviderNeeded(this, getApplicationContext(), fullKey))
    {
      return false;
    }

    // Sinon comportement par defaut
    return super.canWait(fullKey, infos);
  }

  @Override
  public void stopSelfIfPossible()
  {
    Log.d(getClass().getSimpleName(), ">>> stopSelfIfPossible()");

    try
    {
      // En mode vol : on continue
      if (flightMode)
      {
        Log.d(getClass().getSimpleName(), "Le mode vol est actif, le service continue...");
        return;
      }

      // En mode historique : on continue
      if (isHistoryModeOn(getApplicationContext()))
      {
        Log.d(getClass().getSimpleName(), "Le mode historique est actif, le service continue...");
        return;
      }

      // En mode alarme : on continue
      if (alarmMode)
      {
        final boolean activeAlarm = alarmModeThread.almostOneActiveAlarm(true);
        if (activeAlarm)
        {
          Log.d(getClass().getSimpleName(), "Le mode alarme est actif avec au moins une alarme active, le service continue...");
          return;
        }

        Log.d(getClass().getSimpleName(), "Le mode alarme est actif, mais il n'y a aucune alarme active.");
      }

      // Au moins 1 widget : on continue
      if (BalisesWidgets.getAllAppWidgetIds(this).length > 0)
      {
        Log.d(getClass().getSimpleName(), "Des widgets sont utilises, le service continue...");
        return;
      }

      // Comportement commun adapte
      synchronized (baliseProviderListeners)
      {
        final int deltaFlightMode = (baliseProviderListeners.contains(flightModeThread) ? 1 : 0);
        final int deltaHistoryMode = (baliseProviderListeners.contains(historyModeThread) ? 1 : 0);
        final int deltaSearchEngine = (baliseProviderListeners.contains(searchEngineThread) ? 1 : 0);
        final int deltaAlarmMode = (baliseProviderListeners.contains(alarmModeThread) ? 1 : 0);
        final int listenersCount = baliseProviderListeners.size() - deltaFlightMode - deltaHistoryMode - deltaSearchEngine - deltaAlarmMode;

        if (listenersCount <= 0)
        {
          // On peut arreter le service
          Log.d(getClass().getSimpleName(), "Arret du service !");
          stopSelf();
        }
        else
        {
          // Arret impossible
          Log.d(getClass().getSimpleName(), "Des listeners sont enregistres (" + baliseProviderListeners.size() + "), le service continue...");
        }
      }
    }
    finally
    {
      Log.d(getClass().getSimpleName(), "<<< stopSelfIfPossible()");
    }
  }

  @Override
  public void setActivityOnForeground(final boolean onForeground)
  {
    activityOnForeground = onForeground;
  }

  @Override
  public FullLocationService getFullLocationService()
  {
    return (FullLocationService)locationService;
  }

  @Override
  public boolean isHistoryMode()
  {
    return historyMode;
  }

  @Override
  public void setHistoryMode(final boolean historyMode)
  {
    // Flag
    this.historyMode = historyMode;

    // Preferences
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_HISTORY_MODE, historyMode);
    ActivityCommons.commitPreferences(editor);

    // Listener
    if (historyMode)
    {
      historyModeThread.postHistoryModeAlarm();
      addBaliseProviderListener(historyModeThread, false);
    }
    else
    {
      historyModeThread.removeHistoryModeAlarm();
      removeBaliseProviderListener(historyModeThread, false);
    }

    // Statistiques
    analyticsService.trackEvent(AnalyticsService.CAT_HISTORY_MODE, historyMode ? AnalyticsService.ACT_HISTORY_MODE_START : AnalyticsService.ACT_HISTORY_MODE_STOP, null);
  }

  /**
   * 
   * @param startupCalendar
   * @param historyModeRangeStart
   * @param startupNextDay
   * @param shutdownCalendar
   * @param historyModeRangeEnd
   * @param shutdownNextDay
   */
  public static void getHistoryModeDates(final Calendar startupCalendar, final int historyModeRangeStart, final boolean startupNextDay, final Calendar shutdownCalendar, final int historyModeRangeEnd, final boolean shutdownNextDay)
  {
    // Demarrage
    startupCalendar.set(Calendar.HOUR_OF_DAY, historyModeRangeStart);
    startupCalendar.set(Calendar.AM_PM, historyModeRangeStart >= 12 ? Calendar.PM : Calendar.AM);
    startupCalendar.set(Calendar.HOUR, historyModeRangeStart >= 12 ? historyModeRangeStart - 12 : historyModeRangeStart);
    startupCalendar.set(Calendar.MINUTE, 0);
    startupCalendar.set(Calendar.SECOND, 0);
    startupCalendar.set(Calendar.MILLISECOND, 0);
    if (startupNextDay && (startupCalendar.getTimeInMillis() < System.currentTimeMillis()))
    {
      startupCalendar.add(Calendar.DAY_OF_MONTH, 1);
    }

    // Arret
    shutdownCalendar.set(Calendar.HOUR_OF_DAY, historyModeRangeEnd);
    shutdownCalendar.set(Calendar.AM_PM, historyModeRangeEnd >= 12 ? Calendar.PM : Calendar.AM);
    shutdownCalendar.set(Calendar.HOUR, historyModeRangeEnd >= 12 ? historyModeRangeEnd - 12 : historyModeRangeEnd);
    shutdownCalendar.set(Calendar.MINUTE, 0);
    shutdownCalendar.set(Calendar.SECOND, 0);
    shutdownCalendar.set(Calendar.MILLISECOND, 0);
    if (shutdownNextDay && (shutdownCalendar.getTimeInMillis() < System.currentTimeMillis()))
    {
      shutdownCalendar.add(Calendar.DAY_OF_MONTH, 1);
    }
  }

  /**
   * 
   * @param context
   * @return
   */
  public static boolean isHistoryModeOn(final Context context)
  {
    // Initialisations
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);

    return isHistoryModeOn(resources, sharedPreferences);
  }

  /**
   * 
   * @param resources
   * @param sharedPreferences
   * @return
   */
  public static boolean isHistoryModeOn(final Resources resources, final SharedPreferences sharedPreferences)
  {
    // Mode historique actif ?
    final boolean historyMode = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_HISTORY_MODE, false);
    if (!historyMode)
    {
      return false;
    }

    // Configuration mode historique
    final boolean historyModeRange = sharedPreferences.getBoolean(resources.getString(R.string.config_history_mode_range_key), Boolean.parseBoolean(resources.getString(R.string.config_history_mode_range_default)));
    if (!historyModeRange)
    {
      return true;
    }

    // Configuration plage horaire
    final int historyModeRangeStart = (!historyModeRange ? 0 : sharedPreferences.getInt(resources.getString(R.string.config_history_mode_range_start_key),
        Integer.parseInt(resources.getString(R.string.config_history_mode_range_start_default), 10)));
    final int historyModeRangeEnd = (!historyModeRange ? 0 : sharedPreferences.getInt(resources.getString(R.string.config_history_mode_range_end_key),
        Integer.parseInt(resources.getString(R.string.config_history_mode_range_end_default), 10)));
    Log.d(ProvidersService.class.getSimpleName(), "historyModeRange : " + historyModeRange + ", " + historyModeRangeStart + "/" + historyModeRangeEnd);

    // Calcul des heures
    final Calendar startupCalendar = Calendar.getInstance();
    final Calendar shutdownCalendar = Calendar.getInstance();
    getHistoryModeDates(startupCalendar, historyModeRangeStart, false, shutdownCalendar, historyModeRangeEnd, false);

    // Dans la plage horaire
    final long current = System.currentTimeMillis();
    final boolean on = (current >= startupCalendar.getTimeInMillis()) && (current <= shutdownCalendar.getTimeInMillis());
    Log.d(ProvidersService.class.getSimpleName(), "historyModeOn : " + on + " (" + startupCalendar.getTime() + "   <<<TO>>>   " + shutdownCalendar.getTime());

    return on;
  }

  @Override
  public Collection<Releve> getHistory(final String providerKey, final String baliseId) throws IOException
  {
    return historyModeThread.getHistory(getBaliseProvider(providerKey), providerKey, baliseId);
  }

  @Override
  public void recordHistory(final String providerKey, final String baliseId, final Collection<Releve> releves) throws IOException
  {
    historyModeThread.recordHistory(providerKey, baliseId, releves);
  }

  /**
   * 
   */
  private void registerSDCardListener()
  {
    sdcardReceiver = new SDCardBroadcastReceiver(this);
    final IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
    filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    filter.addDataScheme(INTENT_FILE_DATA_SCHEME);
    getApplicationContext().registerReceiver(sdcardReceiver, filter);
  }

  /**
   * 
   */
  private void unregisterSDCardListener()
  {
    getApplicationContext().unregisterReceiver(sdcardReceiver);
    sdcardReceiver.onShutdown();
    sdcardReceiver = null;
  }

  @Override
  public VoiceService getVoiceService()
  {
    return voiceService;
  }

  @Override
  public FavoritesService getFavoritesService()
  {
    return favoritesService;
  }

  @Override
  public boolean isAlarmMode()
  {
    return alarmMode;
  }

  @Override
  public void setAlarmMode(final boolean alarmMode)
  {
    // Flag
    this.alarmMode = alarmMode;

    // Preferences
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_ALARM_MODE, alarmMode);
    ActivityCommons.commitPreferences(editor);

    // Listener
    if (alarmMode)
    {
      alarmModeThread.activate(null);
      addBaliseProviderListener(alarmModeThread, false);
    }
    else
    {
      alarmModeThread.deactivate(null);
      removeBaliseProviderListener(alarmModeThread, false);
    }

    // MAJ des providers
    updateBaliseProviders(true);

    // Statistiques
    analyticsService.trackEvent(AnalyticsService.CAT_ALARM_MODE, alarmMode ? AnalyticsService.ACT_ALARM_MODE_START : AnalyticsService.ACT_ALARM_MODE_STOP, null);
  }

  @Override
  public void onAlarmActivationChanged(final BaliseAlarm alarm)
  {
    Log.d(getClass().getSimpleName(), ">>> onAlarmActivationChanged(" + alarm + ") (alarmMode=" + alarmMode + ")");
    if (alarmMode)
    {
      if (alarm.active)
      {
        alarmModeThread.activate(alarm);
      }
      else
      {
        alarmModeThread.deactivate(alarm);
      }
    }
    Log.d(getClass().getSimpleName(), "<<< onAlarmActivationChanged(" + alarm + ")");
  }

  @Override
  public void onAlarmsUpdated()
  {
    if (alarmMode)
    {
      alarmModeThread.configUpdate();
    }
  }

  /**
   * 
   * @param context
   * @return
   */
  public static boolean isAlarmModeOn(final Context context)
  {
    // Initialisations
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);

    return sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_ALARM_MODE, false);
  }

  @Override
  protected Map<String, List<BaliseProviderMode>> getBaliseProviderActiveCountries(final String key, final int index)
  {
    // Initialisations
    final Map<String, List<BaliseProviderMode>> activeCountriesModes = super.getBaliseProviderActiveCountries(key, index);

    // Les pays pour les alarmes (seulement si mode alarme actif)
    if (alarmMode)
    {
      final List<String> activeAlarmCountries = AlarmUtils.getActiveAlarmsCountries(getApplicationContext(), key);

      // Union
      for (final String country : activeAlarmCountries)
      {
        final List<BaliseProviderMode> activeModes = activeCountriesModes.get(country);
        if (activeModes == null)
        {
          activeCountriesModes.put(country, staticProviderModeAlarm);
        }
        else
        {
          activeModes.add(BaliseProviderMode.ALARM);
        }
      }
    }

    return activeCountriesModes;
  }

  @Override
  protected List<BaliseProviderMode> isBaliseProviderCountryActive(final String key, final String country, final int index)
  {
    // Initialisations
    final List<BaliseProviderMode> activeModes = new ArrayList<BaliseProviderMode>();

    // Dans les preferences
    if (BaliseProviderUtils.isBaliseProviderCountryActive(this, sharedPreferences, key, country, index, null))
    {
      activeModes.add(BaliseProviderMode.STANDARD);
    }

    // Sinon dans les alarmes
    if (alarmMode)
    {
      // Recuperation des alarmes
      final List<BaliseAlarm> alarms;
      synchronized (currentAlarmsLock)
      {
        if (currentAlarms == null)
        {
          // Chargement des alarmes
          currentAlarms = AlarmUtils.loadAlarms(getApplicationContext());

          // Et vidage dans 10s
          final AsyncTask<Void, Void, Void> resetTask = new AsyncTask<Void, Void, Void>()
          {
            @Override
            protected Void doInBackground(final Void... params)
            {
              try
              {
                // Attente de 10s
                Thread.sleep(10000);
              }
              catch (final InterruptedException ie)
              {
                // Nothing
              }
              finally
              {
                // Vidage des alarmes
                synchronized (currentAlarmsLock)
                {
                  currentAlarms = null;
                }
              }

              return null;
            }
          };
          resetTask.execute();
        }

        // Les alarmes
        alarms = currentAlarms;
      }

      // Recherche dans les alarmes
      final String fullKey = BaliseProviderUtils.getBaliseProviderFullKey(key, country);
      for (final BaliseAlarm alarm : alarms)
      {
        if (alarm.active && AlarmUtils.isComplete(alarm) && alarm.provider.equals(fullKey))
        {
          activeModes.add(BaliseProviderMode.ALARM);
          return activeModes;
        }
      }
    }

    return activeModes;
  }
}
