package com.pedro.android.mobibalises.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public class FlightModeStartReceiver extends BroadcastReceiver
{
  @Override
  public void onReceive(final Context context, final Intent intent)
  {
    // Debut
    Log.d(getClass().getSimpleName(), ">>> onReceive : " + intent);

    // Demarrage du service
    Log.d(getClass().getSimpleName(), "starting service...");
    final Intent providersServiceIntent = new Intent(context, ProvidersService.class);
    providersServiceIntent.putExtra(ProvidersService.EXTRA_FLIGHT_MODE_START, true);
    context.startService(providersServiceIntent);

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onReceive");
  }
}
