package org.pedro.android.mobibalises_common.service;

import java.util.List;

import org.pedro.spots.Spot;
import org.pedro.spots.SpotProvider;

/**
 * 
 * @author pedro.m
 */
public interface SpotProviderListener
{
  /**
   * 
   * @param provider
   * @param key
   * @param country
   * @param countryKey
   * @param spots
   * @param updateTs
   */
  public void onSpotProviderAdded(final SpotProvider provider, final String key, final String country, final String countryKey, final List<Spot> spots, final long updateTs);

  /**
   * 
   * @param provider
   * @param key
   * @param country
   * @param countryKey
   */
  public void onSpotProviderRemoved(final SpotProvider provider, final String key, final String country, final String countryKey);
}
