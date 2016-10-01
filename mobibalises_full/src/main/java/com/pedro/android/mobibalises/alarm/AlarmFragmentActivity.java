package com.pedro.android.mobibalises.alarm;

import org.json.JSONException;
import org.pedro.android.mobibalises_common.IProvidersServiceActivity;
import org.pedro.android.mobibalises_common.service.IProvidersService;
import org.pedro.android.mobibalises_common.service.ProvidersServiceBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;

import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.service.ProvidersService;

/**
 * 
 * @author pedro.m
 */
public class AlarmFragmentActivity extends ActionBarActivity implements IProvidersServiceActivity
{
  public static final String         INTENT_ALARM_JSON = "json";

  // Fragment
  private AlarmFragment              alarmFragment;

  // Provider
  private ProvidersServiceConnection providersServiceConnection;
  IFullProvidersService              providersService;

  /**
   * 
   * @author pedro.m
   */
  private static class ProvidersServiceConnection implements ServiceConnection
  {
    private AlarmFragmentActivity activity;

    /**
     * 
     * @param activity
     */
    ProvidersServiceConnection(final AlarmFragmentActivity activity)
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

    // Fragment
    alarmFragment = new AlarmFragment();
    alarmFragment.setArguments(getIntent().getExtras());
    getSupportFragmentManager().beginTransaction().add(android.R.id.content, alarmFragment).commit();

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
    }

    // Fin de la connexion au service
    disconnectService();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item)
  {
    switch (item.getItemId())
    {
      case android.R.id.home:
        // Fin de l'activite
        finish();
        return true;

        // Aucun
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void finish()
  {
    // Resultat
    try
    {
      final Intent data = new Intent();
      data.putExtra(INTENT_ALARM_JSON, alarmFragment.alarm.toJSON().toString());
      setResult(Activity.RESULT_OK, data);
    }
    catch (final JSONException jse)
    {
      throw new RuntimeException(jse);
    }

    super.finish();
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
    alarmFragment.onProvidersServiceConnected(providersService);
  }
}
