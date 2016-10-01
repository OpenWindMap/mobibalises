package org.pedro.android.mobibalises_common.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractBaliseProvidersReceiver extends BroadcastReceiver
{
  @Override
  public void onReceive(final Context context, final Intent intent)
  {
    // Annulation pour les suivants
    abortBroadcast();

    // Initialisations
    final String client = intent.getStringExtra(AbstractProvidersService.BROADCAST_EXTRA_CLIENT);
    Log.d(getClass().getSimpleName(), "Received intent from " + client);

    // Demarrage du service
    final Intent providersServiceIntent = new Intent(context, getProvidersServiceClass());
    providersServiceIntent.putExtra(AbstractProvidersService.BROADCAST_EXTRA_CLIENT, client);
    context.startService(providersServiceIntent);
  }

  /**
   * 
   * @return
   */
  public abstract Class<? extends IProvidersService> getProvidersServiceClass();
}
