package com.pedro.android.mobibalises.service;

import java.util.Calendar;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

import com.pedro.android.mobibalises.widget.BalisesWidgets;

/**
 * 
 * @author pedro.m
 */
public class StartupReceiver extends BroadcastReceiver
{
  // Constantes
  private static final long INTERVALLE_JOUR_MILLIS = 24 * 3600 * 1000;

  @Override
  public void onReceive(final Context context, final Intent intent)
  {
    // Debut
    Log.d(getClass().getSimpleName(), ">>> onReceive : " + intent);

    // Presence de widgets ?
    final int[] widgetsIds = BalisesWidgets.getAllAppWidgetIds(context);
    final boolean widgets = (widgetsIds != null) && (widgetsIds.length > 0);
    Log.d(getClass().getSimpleName(), "widgets : " + widgets);

    // Mode historique ?
    final boolean historyMode = ProvidersService.isHistoryModeOn(context);
    Log.d(getClass().getSimpleName(), "historyMode : " + historyMode);

    // Mode alarme ?
    final boolean alarmMode = ProvidersService.isAlarmModeOn(context);
    Log.d(getClass().getSimpleName(), "alarmMode : " + alarmMode);

    // Demarrage du service si besoin
    if (widgets || historyMode || alarmMode)
    {
      Log.d(getClass().getSimpleName(), "starting service...");
      final Intent providersServiceIntent = new Intent(context, ProvidersService.class);
      context.startService(providersServiceIntent);
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onReceive");
  }

  /**
   * 
   * @param context
   * @param startupTomorrow
   * @param shutdownTomorrow
   */
  public static void manageStartupShutdownAlarms(final Context context, final boolean startupTomorrow, final boolean shutdownTomorrow)
  {
    //TODO : gestion du mode alarme !

    // Initialisations
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
    final AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

    // Configuration mode historique
    final boolean historyModeRange = sharedPreferences.getBoolean(resources.getString(R.string.config_history_mode_range_key), Boolean.parseBoolean(resources.getString(R.string.config_history_mode_range_default)));
    final int historyModeRangeStart = (!historyModeRange ? 0 : sharedPreferences.getInt(resources.getString(R.string.config_history_mode_range_start_key),
        Integer.parseInt(resources.getString(R.string.config_history_mode_range_start_default), 10)));
    final int historyModeRangeEnd = (!historyModeRange ? 0 : sharedPreferences.getInt(resources.getString(R.string.config_history_mode_range_end_key),
        Integer.parseInt(resources.getString(R.string.config_history_mode_range_end_default), 10)));
    Log.d(StartupReceiver.class.getSimpleName(), "historyModeRange : " + historyModeRange + ", " + historyModeRangeStart + "/" + historyModeRangeEnd);

    // Intent de demarrage
    final Intent startupIntent = new Intent(ProvidersService.ACTION_STARTUP_TRY);
    final PendingIntent startupPendingIntent = PendingIntent.getBroadcast(context, 0, startupIntent, 0);

    // Intent d'arret
    final Intent shutdownIntent = new Intent(ProvidersService.ACTION_SHUTDOWN_TRY);
    final PendingIntent shutdownPendingIntent = PendingIntent.getBroadcast(context, 0, shutdownIntent, 0);

    // Annulation des alarmes precedentes
    alarmManager.cancel(startupPendingIntent);
    alarmManager.cancel(shutdownPendingIntent);

    // Mise en place des alarmes si besoin
    if (historyModeRange)
    {
      // Calcul dates
      final Calendar startupCalendar = Calendar.getInstance();
      final Calendar shutdownCalendar = Calendar.getInstance();
      ProvidersService.getHistoryModeDates(startupCalendar, historyModeRangeStart, startupTomorrow, shutdownCalendar, historyModeRangeEnd, shutdownTomorrow);

      // Alarme demarrage
      Log.d(StartupReceiver.class.getSimpleName(), "startup alarm date : " + startupCalendar.getTime());
      alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, startupCalendar.getTimeInMillis(), INTERVALLE_JOUR_MILLIS, startupPendingIntent);

      // Alarme arret
      Log.d(StartupReceiver.class.getSimpleName(), "shutdown alarm date : " + shutdownCalendar.getTime());
      alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, shutdownCalendar.getTimeInMillis(), INTERVALLE_JOUR_MILLIS, shutdownPendingIntent);
    }
  }
}
