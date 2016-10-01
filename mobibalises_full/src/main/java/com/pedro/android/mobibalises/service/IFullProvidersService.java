package com.pedro.android.mobibalises.service;

import java.io.IOException;
import java.util.Collection;

import org.pedro.android.mobibalises_common.location.LocationService;
import org.pedro.android.mobibalises_common.service.IProvidersService;
import org.pedro.android.mobibalises_lgpl.alarm.BaliseAlarm;
import org.pedro.balises.Releve;

import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.pedro.android.mobibalises.favorites.FavoritesService;
import com.pedro.android.mobibalises.location.FullLocationService;
import com.pedro.android.mobibalises.voice.VoiceService;

/**
 * 
 * @author pedro.m
 */
public interface IFullProvidersService extends IProvidersService, LicenseCheckerCallback
{
  /**
   * 
   * @param onForeground
   */
  public void setActivityOnForeground(final boolean onForeground);

  @Deprecated
  @Override
  public LocationService getLocationService();

  /**
   * 
   * @return
   */
  public FullLocationService getFullLocationService();

  /**
   * 
   * @param startThreads
   */
  public void notifyBaliseProvidersThreads(final boolean startThreads);

  /**
   * 
   * @return
   */
  public boolean isFlightMode();

  /**
   * 
   * @param flightMode
   */
  public void setFlightMode(final boolean flightMode);

  /**
   * 
   * @return
   */
  public boolean isHistoryMode();

  /**
   * 
   * @param historyMode
   */
  public void setHistoryMode(final boolean historyMode);

  /**
   * 
   * @param providerKey
   * @param baliseId
   * @return
   * @throws IOException
   */
  public Collection<Releve> getHistory(final String providerKey, final String baliseId) throws IOException;

  /**
   * 
   * @param providerKey
   * @param baliseId
   * @param releves
   * @throws IOException
   */
  public void recordHistory(final String providerKey, final String baliseId, final Collection<Releve> releves) throws IOException;

  /**
   * 
   * @return
   */
  public boolean isAlarmMode();

  /**
   * 
   * @param alarmMode
   */
  public void setAlarmMode(final boolean alarmMode);

  /**
   * 
   */
  public void onAlarmsUpdated();

  /**
   * 
   * @param alarm
   */
  public void onAlarmActivationChanged(final BaliseAlarm alarm);

  /**
   * 
   * @return
   */
  public VoiceService getVoiceService();

  /**
   * 
   * @return
   */
  public FavoritesService getFavoritesService();
}
