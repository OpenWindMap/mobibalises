package org.pedro.android.mobibalises.service;

import org.pedro.android.mobibalises_common.service.AbstractBaliseProvidersReceiver;
import org.pedro.android.mobibalises_common.service.IProvidersService;

/**
 * 
 * @author pedro.m
 */
public class BaliseProvidersReceiver extends AbstractBaliseProvidersReceiver
{
  @Override
  public Class<? extends IProvidersService> getProvidersServiceClass()
  {
    return ProvidersService.class;
  }
}
