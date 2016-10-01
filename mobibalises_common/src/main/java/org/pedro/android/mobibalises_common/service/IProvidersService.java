package org.pedro.android.mobibalises_common.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pedro.android.mobibalises_common.analytics.AnalyticsService;
import org.pedro.android.mobibalises_common.location.LocationService;
import org.pedro.balises.BaliseProvider;
import org.pedro.spots.Spot;
import org.pedro.spots.SpotProvider;

/**
 * 
 * @author pedro.m
 */
public interface IProvidersService
{
  /**
   * 
   * @param async
   */
  public void updateBaliseProviders(final boolean async);

  /**
   * 
   * @return
   */
  public Set<String> getActiveBaliseProviders();

  /**
   * 
   * @param key
   * @return
   */
  public BaliseProvider getBaliseProvider(final String key);

  /**
   * 
   * @param key
   * @return
   */
  public BaliseProviderInfos getBaliseProviderInfos(final String key);

  /**
   * 
   * @param listener
   * @param waitForNotify
   */
  public void addBaliseProviderListener(final BaliseProviderListener listener, final boolean waitForNotify);

  /**
   * 
   * @param listener
   * @param waitForNotifyEnd
   */
  public void removeBaliseProviderListener(final BaliseProviderListener listener, final boolean waitForNotifyEnd);

  /**
   * 
   * @param key
   * @return
   */
  public SpotProvider getSpotProvider(final String key);

  /**
   * 
   * @param key
   * @param country
   * @return
   * @throws IOException
   */
  public Date getLastSpotsUpdate(final String key, final String country) throws IOException;

  /**
   * 
   * @param key
   * @param country
   * @return
   * @throws IOException
   */
  public List<Spot> getSpots(final String key, final String country) throws IOException;

  /**
   * 
   * @param key
   * @param country
   * @param spots
   * @throws IOException
   */
  public void storeSpots(final String key, final String country, final List<Spot> spots) throws IOException;

  /**
   * 
   * @return
   */
  public Map<String, IOException> updateSpotProviders();

  /**
   * 
   * @return
   */
  public boolean haveSpotProvidersChanged();

  /**
   * 
   * @param forceUpdate
   */
  public void setForceDownloadSpotProviders(final boolean forceUpdate);

  /**
   * 
   * @param listener
   */
  public void addSpotProviderListener(final SpotProviderListener listener);

  /**
   * 
   * @param listener
   */
  public void removeSpotProviderListener(final SpotProviderListener listener);

  /**
   * 
   * @throws InterruptedException
   */
  public void waitForInit() throws InterruptedException;

  /**
   * 
   * @param isCallerWidget
   */
  public void notifyNeededBaliseProvidersChanged(final boolean isCallerWidget);

  /**
   * 
   */
  public void stopSelfIfPossible();

  /**
   * 
   * @return
   */
  public LocationService getLocationService();

  /**
   * 
   * @return
   */
  public AnalyticsService getAnalyticsService();

  /**
   * 
   * @return
   */
  public boolean isNetworkOff();

  /**
   * 
   * @return
   */
  public boolean isScreenOff();

  /**
   * 
   */
  public void updateWebcamProviders();

  /**
   * 
   * @param listener
   */
  public void addWebcamProviderListener(final WebcamProviderListener listener);

  /**
   * 
   * @param listener
   */
  public void removeWebcamProviderListener(final WebcamProviderListener listener);

  /**
   * 
   * @param key
   * @return
   */
  public WebcamProviderInfos getWebcamProviderInfos(final String key);
}
