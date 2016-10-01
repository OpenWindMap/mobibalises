package org.pedro.android.mobibalises_common.location;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public class LocationService
{
  // Dispo des providers
  protected boolean                      networkAvailable;
  protected boolean                      gpsAvailable;

  // Localisation
  protected final LocationManager        locationManager;

  // Contexte
  protected Context                      context;

  // Listener interne
  protected final SingleLocationListener singleListener  = new SingleLocationListener(this);

  // Listeners clients
  final List<LocationListener>           singleListeners = new ArrayList<LocationListener>();

  /**
   * 
   * @author pedro.m
   */
  private static class SingleLocationListener implements LocationListener
  {
    private LocationService locationService;

    /**
     * 
     * @param locationService
     */
    SingleLocationListener(final LocationService locationService)
    {
      super();
      this.locationService = locationService;
    }

    @Override
    public void onLocationChanged(final Location location)
    {
      Log.d(locationService.getClass().getSimpleName(), "SingleLocationListener.onLocationChanged : " + location);
      synchronized (locationService.singleListeners)
      {
        locationService.locationManager.removeUpdates(locationService.singleListener);
        locationService.fireOnLocationChanged(location);
        locationService.singleListeners.clear();
      }
    }

    @Override
    public void onProviderDisabled(final String provider)
    {
      Log.d(locationService.getClass().getSimpleName(), "SingleLocationListener.onProviderDisabled : " + provider);
      locationService.locationManager.removeUpdates(locationService.singleListener);
    }

    @Override
    public void onProviderEnabled(final String provider)
    {
      Log.d(locationService.getClass().getSimpleName(), "SingleLocationListener.onProviderEnabled : " + provider);
      // Nothing
    }

    @Override
    public void onStatusChanged(final String provider, final int status, final Bundle extras)
    {
      Log.d(locationService.getClass().getSimpleName(), "SingleLocationListener.onStatusChanged : " + provider + ", " + status);
      // Nothing
    }

    /**
     * 
     */
    void onShutdown()
    {
      locationService = null;
    }
  }

  /**
   * 
   * @param context
   */
  public LocationService(final Context context)
  {
    // Contexte
    this.context = context;

    // Manager
    locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

    // Dispo des providers
    final List<String> providers = locationManager.getAllProviders();
    networkAvailable = providers.contains(LocationManager.NETWORK_PROVIDER);
    gpsAvailable = providers.contains(LocationManager.GPS_PROVIDER);
  }

  /**
   * 
   */
  public void shutdown()
  {
    // Scruteur
    locationManager.removeUpdates(singleListener);

    // Divers
    singleListener.onShutdown();
    context = null;
  }

  /**
   * 
   * @param context
   * @return
   */
  public static Location requestSingleLocation(final Context context)
  {
    // Manager
    final LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

    // Dispo des providers
    final List<String> providers = locationManager.getAllProviders();
    final boolean innerNetworkAvailable = providers.contains(LocationManager.NETWORK_PROVIDER);
    final boolean innerGpsAvailable = providers.contains(LocationManager.GPS_PROVIDER);

    // Statuts
    final boolean gpsActivated = innerGpsAvailable && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    final boolean networkActivated = innerNetworkAvailable && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

    // Positions
    final Location gpsLocation = (gpsActivated ? locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) : null);
    final Location networkLocation = (networkActivated ? locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) : null);

    // Choix
    if ((gpsLocation != null) && (networkLocation != null))
    {
      // 2 positions connues : on prend la plus recente
      if (gpsLocation.getTime() > networkLocation.getTime())
      {
        // GPS
        return gpsLocation;
      }

      // Reseau
      return networkLocation;
    }
    else if (gpsLocation != null)
    {
      // Seulement position par GPS connue
      return gpsLocation;
    }
    else if (networkLocation != null)
    {
      // Seulement position par reseau connue
      return networkLocation;
    }

    // Sinon rien du tout... ;)
    return null;
  }

  /**
   * 
   * @param listener
   * @param doFineLocation
   * @param requestLocationUpdate
   * @return
   */
  public Location requestSingleLocation(final LocationListener listener, final boolean doFineLocation, final boolean requestLocationUpdate)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> requestSingleLocation(..., " + doFineLocation + ", " + requestLocationUpdate + ")");

    // Selection du provider
    final String provider;
    if ((doFineLocation || !networkAvailable || !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) && gpsAvailable && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
    {
      // GPS si (precision demandee ou NETWORK inactif) et GPS actif
      provider = LocationManager.GPS_PROVIDER;
    }
    else if (networkAvailable && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    {
      // NETWORK si actif
      provider = LocationManager.NETWORK_PROVIDER;
    }
    else
    {
      // Sinon rien du tout...
      Log.d(getClass().getSimpleName(), "<<< requestSingleLocation() : no provider");
      return null;
    }

    // Log
    Log.d(getClass().getSimpleName(), "provider choosed for single location : " + provider);

    // Recuperation de la derniere position connue (network ou GPS peu importe)
    final Location location = requestSingleLocation(context);
    Log.d(getClass().getSimpleName(), "last known location " + location);

    // Demande precise si besoin
    if ((location == null) || requestLocationUpdate)
    {
      // Log
      Log.d(getClass().getSimpleName(), "requesting location update...");

      // Enregistrement du listener
      synchronized (singleListeners)
      {
        // Ajout du client
        final boolean added;
        if (!singleListeners.contains(listener))
        {
          added = singleListeners.add(listener);
        }
        else
        {
          added = false;
        }

        // Demande de localisation (si besoin)
        if (added && (singleListeners.size() == 1))
        {
          locationManager.requestLocationUpdates(provider, 0, 0, singleListener);
        }
      }
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< requestSingleLocation() : " + location);
    return location;
  }

  /**
   * 
   * @param listener
   */
  public void cancelSingleRequest(final LocationListener listener)
  {
    synchronized (singleListeners)
    {
      // Log
      Log.d(getClass().getSimpleName(), "canceling request for " + listener + " in " + singleListeners.size() + " listeners");

      // Retrait de la liste des listeners
      singleListeners.remove(listener);

      // Log
      Log.d(getClass().getSimpleName(), "listener " + listener + " removed, " + singleListeners.size() + " listeners remaining");
    }

    // Fin des updates si possible
    removeUpdatesIfPossible();
  }

  /**
   * 
   * @param location
   */
  void fireOnLocationChanged(final Location location)
  {
    synchronized (singleListeners)
    {
      if (singleListeners.size() > 0)
      {
        Log.d(getClass().getSimpleName(), "firing location to " + singleListeners.size() + " listener(s)");
        for (final LocationListener listener : singleListeners)
        {
          listener.onLocationChanged(location);
        }
      }
      else
      {
        Log.d(getClass().getSimpleName(), "not firing location : no listener");
      }
    }
  }

  /**
   * 
   */
  private void removeUpdatesIfPossible()
  {
    synchronized (singleListeners)
    {
      if (singleListeners.size() == 0)
      {
        Log.d(getClass().getSimpleName(), "no more listener, removing updates");
        locationManager.removeUpdates(singleListener);
      }
    }
  }

  /**
   * 
   * @param context
   * @return
   */
  public static boolean isLocationEnabled(final Context context)
  {
    // Manager
    final LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);

    // Dispo des providers
    final List<String> providers = locationManager.getAllProviders();
    final boolean innerNetworkAvailable = providers.contains(LocationManager.NETWORK_PROVIDER);
    final boolean innerGpsAvailable = providers.contains(LocationManager.GPS_PROVIDER);

    return (innerNetworkAvailable && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) || (innerGpsAvailable && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
  }

  /**
   * 
   * @return
   */
  public boolean isLocationEnabled()
  {
    return (networkAvailable && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) || (gpsAvailable && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
  }

  /**
   * 
   */
  public void fireLastLocation()
  {
    // Nothing
  }
}
