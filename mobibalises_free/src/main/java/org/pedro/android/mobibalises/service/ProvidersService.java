package org.pedro.android.mobibalises.service;

import org.pedro.android.mobibalises.FreeActivityCommons;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;

import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public final class ProvidersService extends AbstractProvidersService
{
  @Override
  public void onCreate()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onCreate()");

    // Super
    super.onCreate();

    // Demarrage init
    startInitThread();

    // Initialisations
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
  }

  @Override
  protected void initCommons()
  {
    super.initCommons();
    FreeActivityCommons.init(getApplicationContext());
  }
}
