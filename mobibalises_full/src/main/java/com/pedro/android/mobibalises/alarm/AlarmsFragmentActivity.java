package com.pedro.android.mobibalises.alarm;

import org.pedro.android.mobibalises_common.IProvidersServiceActivity;
import org.pedro.android.mobibalises_common.service.IProvidersService;
import org.pedro.android.mobibalises_common.service.ProvidersServiceBinder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.service.ProvidersService;

/**
 * 
 * @author pedro.m
 */
public class AlarmsFragmentActivity extends ActionBarActivity implements IProvidersServiceActivity
{
  public static final String         PARAM_POSITION = "position";

  // Provider
  private ProvidersServiceConnection providersServiceConnection;
  IFullProvidersService              providersService;

  /**
   * 
   * @author pedro.m
   */
  private static class ProvidersServiceConnection implements ServiceConnection
  {
    private AlarmsFragmentActivity activity;

    /**
     * 
     * @param activity
     */
    ProvidersServiceConnection(final AlarmsFragmentActivity activity)
    {
      this.activity = activity;
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder inBinder)
    {
      // Recuperation du service
      activity.providersService = (IFullProvidersService)((ProvidersServiceBinder)inBinder).getService();

      // Notification
      activity.onProvidersServiceConnected();
    }

    @Override
    public void onServiceDisconnected(final ComponentName name)
    {
      activity = null;
    }
  }

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    // Super
    Log.d(getClass().getSimpleName(), ">>> onCreate()");
    super.onCreate(savedInstanceState);

    // Vue
    setContentView(R.layout.alarms_fragment);

    // Barre d'action
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // Connexion au service
    initServiceConnection();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
  }

  @Override
  public void onDestroy()
  {
    // Super
    Log.d(getClass().getSimpleName(), ">>> onDestroy()");
    super.onDestroy();

    // MAJ Service
    if (providersService != null)
    {
      providersService.updateBaliseProviders(true);
      providersService.onAlarmsUpdated();
    }

    // Fin de la connexion au service
    disconnectService();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  /**
   * 
   */
  private void initServiceConnection()
  {
    // Initialisation service
    providersServiceConnection = new ProvidersServiceConnection(this);

    // Connexion au service
    final Intent providersServiceIntent = new Intent(getApplicationContext(), ProvidersService.class);
    bindService(providersServiceIntent, providersServiceConnection, Context.BIND_AUTO_CREATE);
  }

  /**
   * 
   */
  private void disconnectService()
  {
    // Deconnexion du service
    unbindService(providersServiceConnection);
  }

  @Override
  public IProvidersService getProvidersService()
  {
    return providersService;
  }

  @Override
  public void onProvidersServiceConnected()
  {
    // Fragment principal
    final AlarmsFragment alarmsFragment = (AlarmsFragment)getSupportFragmentManager().findFragmentById(R.id.alarms_part);
    if (alarmsFragment != null)
    {
      alarmsFragment.onProvidersServiceConnected(providersService);
    }

    // Fragment secondaire
    final AlarmFragment alarmFragment = (AlarmFragment)getSupportFragmentManager().findFragmentById(R.id.alarm_part);
    if (alarmFragment != null)
    {
      alarmFragment.onProvidersServiceConnected(providersService);
    }
  }
}
