package org.pedro.android.mobibalises.preferences;

import org.pedro.android.mobibalises.service.ProvidersService;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;

/**
 * 
 * @author pedro.m
 */
public final class BalisesPreferencesActivity extends AbstractBalisesPreferencesActivity
{
  @Override
  protected Class<? extends AbstractProvidersService> getProvidersServiceClass()
  {
    return ProvidersService.class;
  }
}
