package org.pedro.android.mobibalises_common.service;

import java.util.List;

import org.pedro.android.mobibalises_common.service.AbstractProvidersService.BaliseProviderMode;
import org.pedro.balises.BaliseProvider;

/**
 * 
 * @author pedro.m
 */
public interface BaliseProviderListener
{
  /**
   * 
   * @param key
   * @param provider
   * @param standardMode
   * @param activeModes
   */
  public void onBalisesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes);

  /**
   * 
   * @param key
   * @param provider
   * @param standardMode
   * @param activeModes
   */
  public void onRelevesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes);

  /**
   * 
   * @param key
   * @param provider
   * @param standardMode
   * @param activeModes
   */
  public void onBaliseProviderAdded(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes);

  /**
   * 
   * @param key
   * @param provider
   * @param infos
   * @param wasStandardMode
   * @param oldActiveModes
   */
  public void onBaliseProviderRemoved(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes);

  /**
   * 
   * @param key
   * @param provider
   * @param infos
   * @param standardMode
   */
  public void onBaliseProviderUpdateStarted(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode);

  /**
   * 
   * @param key
   * @param provider
   * @param infos
   * @param standardMode
   */
  public void onBaliseProviderUpdateEnded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode);

  /**
   * 
   * @param key
   * @param standardMode
   * @param activeModes
   * @return
   */
  public boolean isBaliseProviderNeeded(final String key, final boolean standardMode, final List<BaliseProviderMode> activeModes);

  /**
   * 
   */
  public void onBaliseProvidersChanged();

  /**
   * 
   * @param key
   * @param provider
   * @param infos
   * @param standardModeAdded
   * @param addedModes
   * @param oldActiveModes
   */
  public void onBaliseProviderModesAdded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardModeAdded, final List<BaliseProviderMode> addedModes,
      final List<BaliseProviderMode> oldActiveModes);

  /**
   * 
   * @param key
   * @param provider
   * @param infos
   * @param standardModeRemoved
   * @param removedModes
   * @param oldActiveModes
   */
  public void onBaliseProviderModesRemoved(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardModeRemoved, final List<BaliseProviderMode> removedModes,
      final List<BaliseProviderMode> oldActiveModes);
}
