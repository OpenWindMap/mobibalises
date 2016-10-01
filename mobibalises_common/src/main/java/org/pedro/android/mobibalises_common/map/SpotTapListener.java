package org.pedro.android.mobibalises_common.map;

/**
 * 
 * @author pedro.m
 */
public interface SpotTapListener
{
  /**
   * 
   * @param spotItem
   */
  public void onSpotTap(final SpotItem spotItem);

  /**
   * 
   * @param spotItem
   */
  public void onSpotLongTap(final SpotItem spotItem);

  /**
   * 
   * @param spotItem
   */
  public void onSpotInfoLinkTap(final SpotItem spotItem);
}
