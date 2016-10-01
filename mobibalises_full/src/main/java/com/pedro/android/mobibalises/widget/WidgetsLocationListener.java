package com.pedro.android.mobibalises.widget;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import com.pedro.android.mobibalises.favorites.BaliseFavorite;
import com.pedro.android.mobibalises.location.FullLocationService;
import com.pedro.android.mobibalises.service.IFullProvidersService;

/**
 * 
 * @author pedro.m
 */
public class WidgetsLocationListener implements LocationListener
{
  // Constantes
  public static final int             WIDGETS_LISTENER_TIME     = 300000;
  public static final int             WIDGETS_LISTENER_DISTANCE = 1000;

  // Service
  private final IFullProvidersService providersService;

  /**
   * 
   * @param providersService
   */
  public WidgetsLocationListener(final IFullProvidersService providersService)
  {
    this.providersService = providersService;
  }

  @Override
  public void onLocationChanged(final Location location)
  {
    final List<BaliseFavorite> proches = (location == null ? null : FullLocationService.getProximityBalises((Context)providersService, location, providersService));
    final BaliseFavorite[] autour = (location == null ? null : FullLocationService.getAroundBalises((Context)providersService, location, 8, providersService));
    Log.d(getClass().getSimpleName(), "widgetsListener.onLocationChanged : proches=" + proches + ", autour=" + (autour == null ? null : Arrays.asList(autour)) + " (" + location + ")");
    BalisesWidgets.onLocationChanged((Context)providersService, proches, autour, providersService);
  }

  @Override
  public void onProviderDisabled(final String provider)
  {
    //Nothing
  }

  @Override
  public void onProviderEnabled(final String provider)
  {
    //Nothing
  }

  @Override
  public void onStatusChanged(final String provider, final int status, final Bundle extras)
  {
    //Nothing
  }
}
