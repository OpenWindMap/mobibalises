package com.pedro.android.mobibalises.preferences;

import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;

import android.util.Log;

import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.service.ProvidersService;
import com.pedro.android.mobibalises.service.StartupReceiver;
import com.pedro.android.mobibalises.widget.BalisesWidgets;

/**
 * 
 * @author pedro.m
 */
public final class BalisesPreferencesActivity extends AbstractBalisesPreferencesActivity
{
  @Override
  protected Class<? extends AbstractProvidersService> getProvidersServiceClass()
  {
    return ProvidersService.class;
  }

  @Override
  public void onDestroy()
  {
    Log.d(getClass().getSimpleName(), ">>> onDestroy()");

    // MAJ des Widgets
    if (providersService != null)
    {
      BalisesWidgets.synchronizeWidgets(getApplicationContext(), (IFullProvidersService)providersService, null);
    }

    // MAJ des alarmes
    StartupReceiver.manageStartupShutdownAlarms(getApplicationContext(), true, true);

    // Comportement global
    super.onDestroy();

    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  @Override
  public void onStop()
  {
    Log.d(getClass().getSimpleName(), ">>> onStop()");
    super.onStop();

    // Configuration du service de synthese vocale
    ((IFullProvidersService)providersService).getVoiceService().updatePreferences(sharedPreferences);

    Log.d(getClass().getSimpleName(), "<<< onStop()");
  }
}
