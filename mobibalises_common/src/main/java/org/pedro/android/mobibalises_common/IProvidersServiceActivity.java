package org.pedro.android.mobibalises_common;

import org.pedro.android.mobibalises_common.service.IProvidersService;

/**
 * 
 * @author pedro.m
 */
public interface IProvidersServiceActivity
{
  /**
   * 
   * @return
   */
  public IProvidersService getProvidersService();

  /**
   * 
   */
  public void onProvidersServiceConnected();
}
