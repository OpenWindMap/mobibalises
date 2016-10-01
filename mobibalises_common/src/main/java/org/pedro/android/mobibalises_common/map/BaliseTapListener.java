package org.pedro.android.mobibalises_common.map;

import org.pedro.balises.BaliseProvider;

/**
 * 
 * @author pedro.m
 */
public interface BaliseTapListener
{
  /**
   * 
   * @param provider
   * @param providerKey
   * @param idBalise
   */
  public void onBaliseTap(final BaliseProvider provider, final String providerKey, final String idBalise);

  /**
   * 
   * @param provider
   * @param providerKey
   * @param idBalise
   */
  public void onBaliseLongTap(final BaliseProvider provider, final String providerKey, final String idBalise);

  /**
   * 
   * @param provider
   * @param providerKey
   * @param idBalise
   */
  public void onBaliseInfoLinkTap(final BaliseProvider provider, final String providerKey, final String idBalise);
}
