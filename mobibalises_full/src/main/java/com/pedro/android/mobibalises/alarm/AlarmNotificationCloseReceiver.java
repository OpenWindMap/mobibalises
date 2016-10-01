package com.pedro.android.mobibalises.alarm;

import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.pedro.android.mobibalises.FullActivityCommons;
import com.pedro.android.mobibalises.service.ProvidersService;

/**
 * 
 * @author pedro.m
 */
public class AlarmNotificationCloseReceiver extends BroadcastReceiver
{
  @Override
  public void onReceive(final Context context, final Intent intent)
  {
    // Debut
    Log.d(getClass().getSimpleName(), ">>> onReceive : " + intent);

    // Initialisations
    final int action = intent.getIntExtra(ProvidersService.INTENT_ALARM_ACTION, -1);
    final String notificationTag = intent.getStringExtra(ProvidersService.INTENT_ALARM_NOTIFICATION_TAG);
    final String alarmId = intent.getStringExtra(ProvidersService.INTENT_ALARM_ID);
    Log.d(getClass().getSimpleName(), "intentAlarmAction : " + action);
    Log.d(getClass().getSimpleName(), "intentNotificationTag : " + notificationTag);
    Log.d(getClass().getSimpleName(), "intentAlarmId : " + alarmId);
    final BaliseAlarm alarm = new BaliseAlarm();
    alarm.setId(alarmId);

    if (action == ProvidersService.INTENT_ALARM_ACTION_CANCEL_NOTIFICATION)
    {
      // Suppression de la notification
      Log.d(getClass().getSimpleName(), "Suppression de la notification #" + alarmId + " pour l'alarme " + alarm);
      final boolean verifiee = Boolean.parseBoolean(notificationTag);
      FullActivityCommons.manageAlarmNotification(context, alarm, false, verifiee, null);
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onReceive");
  }
}
