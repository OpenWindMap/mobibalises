package com.pedro.android.mobibalises.location;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.location.LocationService;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.map.GeoPoint;
import org.pedro.map.MercatorProjection;
import org.pedro.utils.ThreadUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.favorites.BaliseFavorite;
import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.widget.BalisesWidgets;
import com.pedro.android.mobibalises.widget.WidgetsLocationListener;

/**
 * 
 * @author pedro.m
 */
public class FullLocationService extends LocationService
{
  // Constantes
  private static final long                                     GPS_MIN_MIN_TIME                = 2000;
  private static final float                                    GPS_MIN_MIN_DISTANCE            = 10;
  private static final long                                     NETWORK_MIN_MIN_TIME            = 60000;
  private static final float                                    NETWORK_MIN_MIN_DISTANCE        = 100;
  private static final int                                      BUILD_VERSION_CODES_GINGERBREAD = 9;

  private static final String                                   PROVIDERS_CHANGED_ACTION        = "PROVIDERS_CHANGED_ACTION";
  private static final String                                   STRING_IS                       = " is ";
  private static final String                                   STRING_DISABLED                 = "disabled";
  private static final String                                   STRING_ENABLED                  = "enabled";
  private static final String                                   STRING_NOT_AVAILABLE            = "not available";
  private static final String                                   STRING_AVAILABLE                = "available";

  // Service
  final IFullProvidersService                                   providersService;

  // Ecran
  private boolean                                               screenOff;

  // Clients
  private final List<InternalLocationListener>                  listeners                       = new ArrayList<InternalLocationListener>();
  private final Map<LocationListener, InternalLocationListener> internalListeners               = new HashMap<LocationListener, InternalLocationListener>();
  private final LocationListener                                widgetsListener;

  // Listeners internes
  private final EnabledFullLocationListener                     enabledNetworkListener          = new EnabledFullLocationListener(this, LocationManager.NETWORK_PROVIDER);
  private final DisabledFullLocationListener                    disabledNetworkListener;
  private final EnabledFullLocationListener                     enabledGpsListener              = new EnabledFullLocationListener(this, LocationManager.GPS_PROVIDER);
  private final DisabledFullLocationListener                    disabledGpsListener;
  ProvidersBroadcastReceiver                                    providersReceiver;
  private final IntentFilter                                    providersReceiverFilter;

  // Verrou
  private final Object                                          manageLock                      = new Object();

  // Flags
  private long                                                  currentMinTime                  = Long.MAX_VALUE;
  private float                                                 currentMinDistance              = Float.MAX_VALUE;
  private boolean                                               currentNetworkRegistered        = false;
  private boolean                                               currentGpsRegistered            = false;
  private boolean                                               firstTime                       = true;

  // Derniere position
  Location                                                      lastLocation;

  // Thread de changement de position
  final OnLocationChangedThread                                 onLocationChangedThread         = new OnLocationChangedThread(this);

  /**
   * 
   * @author pedro.m
   */
  private static class ProvidersBroadcastReceiver extends BroadcastReceiver
  {
    private FullLocationService locService;

    /**
     * 
     * @param locService
     */
    ProvidersBroadcastReceiver(final FullLocationService locService)
    {
      super();
      this.locService = locService;
    }

    @Override
    public void onReceive(final Context inContext, final Intent intent)
    {
      Log.d(locService.getClass().getSimpleName(), "received provider changed broadcast");
      locService.manageListeners(false);
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      locService = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class InternalLocationListener
  {
    final LocationListener listener;
    final boolean          doFineLocation;
    final long             time;
    final float            distance;

    /**
     * 
     * @param listener
     * @param doFineLocation
     * @param time
     * @param distance
     */
    InternalLocationListener(final LocationListener listener, final boolean doFineLocation, final long time, final float distance)
    {
      this.listener = listener;
      this.doFineLocation = doFineLocation;
      this.time = time;
      this.distance = distance;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class OnLocationChangedThread extends Thread
  {
    private FullLocationService locService;
    private int                 demands = 0;
    private Location            location;

    /**
     * 
     * @param locService
     */
    OnLocationChangedThread(final FullLocationService locService)
    {
      super(FullLocationService.class.getName() + ".OnLocationChangedThread");
      this.locService = locService;
    }

    @Override
    public void run()
    {
      while (!isInterrupted())
      {
        synchronized (this)
        {
          // Boucle d'attente
          while ((demands <= 0) && !isInterrupted())
          {
            try
            {
              Log.d(getClass().getSimpleName(), ">>> wait()");
              wait();
              Log.d(getClass().getSimpleName(), "<<< wait()");
            }
            catch (final InterruptedException ie)
            {
              Thread.currentThread().interrupt();
              return;
            }
          }
        }

        // Traitement
        synchronized (this)
        {
          demands--;
        }
        doOnLocationChanged();
      }

      // Fin
      locService = null;
    }

    /**
     * 
     */
    private void doOnLocationChanged()
    {
      // Traitement ok
      try
      {
        // Attente de la fin de l'initialisation
        locService.providersService.waitForInit();

        // Clients
        locService.fullFireOnLocationChanged(location);

        // Notification des threads
        locService.providersService.notifyBaliseProvidersThreads(true);
      }
      catch (final InterruptedException ie)
      {
        Log.w(locService.getClass().getSimpleName(), "Interrupted");
        Thread.currentThread().interrupt();
      }
    }

    /**
     * 
     * @param inLocation
     */
    void requestOnLocationChanged(final Location inLocation)
    {
      synchronized (this)
      {
        demands++;
        location = inLocation;
        this.notify();
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static abstract class AbstractFullLocationListener implements LocationListener
  {
    protected FullLocationService locService;
    protected final String        provider;

    /**
     * 
     * @param locService
     * @param provider
     */
    AbstractFullLocationListener(final FullLocationService locService, final String provider)
    {
      this.locService = locService;
      this.provider = provider;
    }

    @Override
    public void onLocationChanged(final Location location)
    {
      // Initialisations
      Log.d(locService.getClass().getSimpleName(), "AbstractFullLocationListener." + provider + ".onLocationChanged : " + location);
      locService.lastLocation = location;

      // Notification du Thread de MAJ de la position
      locService.onLocationChangedThread.requestOnLocationChanged(location);
    }

    @Override
    public void onStatusChanged(final String inProvider, final int status, final Bundle extras)
    {
      Log.d(locService.getClass().getSimpleName(), "AbstractFullLocationListener.onStatusChanged : " + inProvider + ", " + status);
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      locService = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class EnabledFullLocationListener extends AbstractFullLocationListener
  {
    /**
     * 
     * @param locService
     * @param provider
     */
    EnabledFullLocationListener(final FullLocationService locService, final String provider)
    {
      super(locService, provider);
    }

    @Override
    public void onProviderEnabled(final String inProvider)
    {
      //Nothing
    }

    @Override
    public void onProviderDisabled(final String inProvider)
    {
      if (locService.providersReceiver == null)
      {
        Log.d(locService.getClass().getSimpleName(), "EnabledFullLocationListener." + provider + ".onProviderDisabled : " + inProvider + "... managing listeners");
        locService.manageListeners(false);
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class DisabledFullLocationListener extends AbstractFullLocationListener
  {
    /**
     * 
     * @param locService
     * @param provider
     */
    DisabledFullLocationListener(final FullLocationService locService, final String provider)
    {
      super(locService, provider);
    }

    @Override
    public void onProviderEnabled(final String inProvider)
    {
      Log.d(locService.getClass().getSimpleName(), "DisabledFullLocationListener." + provider + ".onProviderEnabled : " + inProvider + "... managing listeners");
      locService.manageListeners(true);
    }

    @Override
    public void onProviderDisabled(final String inProvider)
    {
      //Nothing
    }
  }

  /**
   * 
   * @param providersService
   * @param context
   */
  public FullLocationService(final IFullProvidersService providersService, final Context context)
  {
    super(context);
    this.providersService = providersService;
    this.widgetsListener = new WidgetsLocationListener(providersService);

    // Recuperation de l'action du broadcastreceiver pour le changement de provider
    // Par introspection pour compatibilite avec versions < L9
    final String action;
    if (Build.VERSION.SDK_INT >= BUILD_VERSION_CODES_GINGERBREAD)
    {
      String theAction = null;
      try
      {
        final Field actionField = LocationManager.class.getField(PROVIDERS_CHANGED_ACTION);
        theAction = (String)actionField.get(new String());
      }
      catch (final SecurityException se)
      {
        Log.w(getClass().getSimpleName(), se);
      }
      catch (final NoSuchFieldException nsfe)
      {
        Log.w(getClass().getSimpleName(), nsfe);
      }
      catch (final IllegalArgumentException iae)
      {
        Log.w(getClass().getSimpleName(), iae);
      }
      catch (final IllegalAccessException iae)
      {
        Log.w(getClass().getSimpleName(), iae);
      }
      finally
      {
        action = theAction;
      }
    }
    else
    {
      action = null;
    }

    if ((Build.VERSION.SDK_INT < BUILD_VERSION_CODES_GINGERBREAD) || (action == null))
    {
      // Passage par des listeners
      disabledNetworkListener = new DisabledFullLocationListener(this, LocationManager.NETWORK_PROVIDER);
      disabledGpsListener = new DisabledFullLocationListener(this, LocationManager.GPS_PROVIDER);
      providersReceiver = null;
      providersReceiverFilter = null;
    }
    else
    {
      // Passage par un BroadcastReceiver
      disabledNetworkListener = null;
      disabledGpsListener = null;
      providersReceiver = new ProvidersBroadcastReceiver(this);
      providersReceiverFilter = new IntentFilter();
      providersReceiverFilter.addAction(action);
    }

    // Init
    initialize();
  }

  /**
   * 
   */
  private void initialize()
  {
    // Receiver pour les providers
    if (providersReceiver != null)
    {
      context.registerReceiver(providersReceiver, providersReceiverFilter);
    }

    // Lancement d'un Thread de MAJ (pour ne pas bloquer l'UI sur le waitForInit())
    onLocationChangedThread.start();

    // Listener pour les widgets
    addListener(widgetsListener, false, WidgetsLocationListener.WIDGETS_LISTENER_TIME, WidgetsLocationListener.WIDGETS_LISTENER_DISTANCE, true);

    // Listeners internes
    manageListeners(true);
  }

  /**
   * 
   */
  public void manageListeners(final boolean doQuickLocation)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> manageListeners()");

    synchronized (manageLock)
    {
      // Initialisations
      long minTime = Long.MAX_VALUE;
      float minDistance = Float.MAX_VALUE;
      boolean networkRegister = false;
      boolean gpsRegister = false;
      Log.d(getClass().getSimpleName(), LocationManager.NETWORK_PROVIDER + STRING_IS + (networkAvailable ? STRING_AVAILABLE : STRING_NOT_AVAILABLE));
      Log.d(getClass().getSimpleName(), LocationManager.GPS_PROVIDER + STRING_IS + (gpsAvailable ? STRING_AVAILABLE : STRING_NOT_AVAILABLE));
      final boolean networkEnabled = networkAvailable && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
      final boolean gpsEnabled = gpsAvailable && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
      Log.d(getClass().getSimpleName(), LocationManager.NETWORK_PROVIDER + STRING_IS + (networkEnabled ? STRING_ENABLED : STRING_DISABLED));
      Log.d(getClass().getSimpleName(), LocationManager.GPS_PROVIDER + STRING_IS + (gpsEnabled ? STRING_ENABLED : STRING_DISABLED));

      // RAZ de la derniere position
      lastLocation = null;

      // Ecoute seulement si mode vol OU (ecran allume ET presence de widgets ayant besoin de la localisation)
      if (providersService.isFlightMode() || (!screenOff && BalisesWidgets.needsLocationUpdates((Context)providersService)))
      {
        // Verrouillage clients
        synchronized (listeners)
        {
          if (listeners.size() > 0)
          {
            // Si aucun fournisseur
            if (!networkEnabled && !gpsEnabled)
            {
              // Notification au listener des widgets que la position est inconnue
              widgetsListener.onLocationChanged(null);
            }
            else
            {
              // Pour chaque listener client
              for (final InternalLocationListener internalListener : listeners)
              {
                // Utilisation du GPS ?
                if ((internalListener.doFineLocation || !networkEnabled) && gpsEnabled)
                {
                  gpsRegister = true;
                }
                // Utilisation du reseau ?
                else if (networkEnabled)
                {
                  networkRegister = true;
                }

                // Temps mini
                if (internalListener.time < minTime)
                {
                  minTime = internalListener.time;
                }

                // Distance mini
                if (internalListener.distance < minDistance)
                {
                  minDistance = internalListener.distance;
                }
              }
            }
          }
        }

        // Priorite au GPS
        if (gpsRegister)
        {
          networkRegister = false;
        }
      }

      // Minimums finaux
      final long finalMinTime = Math.max(minTime, gpsRegister ? GPS_MIN_MIN_TIME : NETWORK_MIN_MIN_TIME);
      final float finalMinDistance = Math.max(minDistance, gpsRegister ? GPS_MIN_MIN_DISTANCE : NETWORK_MIN_MIN_DISTANCE);

      // Detection changement
      final boolean precisionChanged = (finalMinTime != currentMinTime) || (finalMinDistance != currentMinDistance);
      final boolean providersChanged = (gpsRegister != currentGpsRegistered) || (networkRegister != currentNetworkRegistered);

      // Si changement
      if (firstTime || precisionChanged || providersChanged)
      {
        // Gestion GPS
        if (gpsAvailable)
        {
          manageListener(enabledGpsListener, disabledGpsListener, LocationManager.GPS_PROVIDER, doQuickLocation, providersChanged, gpsRegister, finalMinTime, finalMinDistance);
        }

        // Gestion NETWORK
        if (networkAvailable)
        {
          manageListener(enabledNetworkListener, disabledNetworkListener, LocationManager.NETWORK_PROVIDER, doQuickLocation, providersChanged, networkRegister, finalMinTime, finalMinDistance);
        }

        // Fin
        currentGpsRegistered = gpsRegister;
        currentNetworkRegistered = networkRegister;
        currentMinTime = finalMinTime;
        currentMinDistance = finalMinDistance;
      }
      else
      {
        // Pas de changement...
        Log.d(getClass().getSimpleName(), "... no change in location settings");

        // ...mais positionnement rapide si demande (et possible)
        if (doQuickLocation)
        {
          if (gpsRegister)
          {
            doQuickLocation(LocationManager.GPS_PROVIDER);
          }
          else if (networkRegister)
          {
            doQuickLocation(LocationManager.NETWORK_PROVIDER);
          }
        }
      }

      // Fin
      firstTime = false;
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< manageListeners()");
  }

  /**
   * 
   * @param enabledListener
   * @param disabledListener
   * @param provider
   * @param doQuickLocation
   * @param providersChanged
   * @param register
   * @param minTime
   * @param minDistance
   */
  private void manageListener(final EnabledFullLocationListener enabledListener, final DisabledFullLocationListener disabledListener, final String provider, final boolean doQuickLocation, final boolean providersChanged,
      final boolean register, final long minTime, final float minDistance)
  {
    // RAZ
    if (providersChanged)
    {
      locationManager.removeUpdates(enabledListener);
      if (disabledListener != null)
      {
        locationManager.removeUpdates(disabledListener);
      }
    }

    // Enregistrement
    if (register)
    {
      // Enregistrement
      Log.d(getClass().getSimpleName(), "registering enabledListener for " + provider + " for " + minTime + "ms and " + minDistance + "m");
      locationManager.requestLocationUpdates(provider, minTime, minDistance, enabledListener);

      // Positionnement rapide si dispo
      if (doQuickLocation)
      {
        doQuickLocation(provider);
      }
    }
    else if ((providersChanged || firstTime) && (disabledListener != null))
    {
      Log.d(getClass().getSimpleName(), "registering disabledListener for " + provider);
      locationManager.requestLocationUpdates(provider, Long.MAX_VALUE, Float.MAX_VALUE, disabledListener);
    }
  }

  /**
   * 
   * @param provider
   */
  private void doQuickLocation(final String provider)
  {
    final Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
    if (lastKnownLocation != null)
    {
      // Sauvegarde
      lastLocation = lastKnownLocation;

      // Lancement d'un thread avec cette position rapide (obligatoire sinon deadlock au demarrage du service)
      Log.d(getClass().getSimpleName(), "last known location for " + provider + " : " + lastKnownLocation);
      onLocationChangedThread.requestOnLocationChanged(lastKnownLocation);
    }
  }

  @Override
  public void fireLastLocation()
  {
    onLocationChangedThread.requestOnLocationChanged(lastLocation);
  }

  /**
   * 
   * @param location
   */
  void fullFireOnLocationChanged(final Location location)
  {
    // Initialisations : obtention d'une copie de la liste des listeners
    final List<InternalLocationListener> finalListeners;
    synchronized (listeners)
    {
      if (listeners.size() == 0)
      {
        Log.d(getClass().getSimpleName(), "not firing location : no listener");
        return;
      }

      // Copie de la liste des listeners
      finalListeners = new ArrayList<InternalLocationListener>(listeners);
    }

    // Lancement d'un Thread de notification
    Log.d(FullLocationService.class.getSimpleName(), "firing location to " + finalListeners.size() + " listener(s)");
    for (final InternalLocationListener internalListener : finalListeners)
    {
      internalListener.listener.onLocationChanged(location);
    }
  }

  /**
   * 
   * @param listener
   * @param doFineLocation
   * @param time
   * @param distance
   * @param doManage
   */
  public void addListener(final LocationListener listener, final boolean doFineLocation, final long time, final float distance, final boolean doManage)
  {
    // Ajout du listener
    final boolean added;
    synchronized (manageLock)
    {
      synchronized (listeners)
      {
        if (internalListeners.get(listener) == null)
        {
          final InternalLocationListener internalListener = new InternalLocationListener(listener, doFineLocation, time, distance);
          listeners.add(internalListener);
          internalListeners.put(listener, internalListener);
          Log.d(getClass().getSimpleName(), "adding listener " + listener + " => " + listeners.size());
          added = true;
        }
        else
        {
          Log.d(getClass().getSimpleName(), "not adding listener " + listener + " : already registered");
          added = false;
        }
      }
    }

    // Gestion
    if (added && doManage)
    {
      manageListeners(true);
    }
  }

  /**
   * 
   * @param listener
   * @param doManage
   */
  public void removeListener(final LocationListener listener, final boolean doManage)
  {
    // Suppression du listener
    final boolean removed;
    synchronized (manageLock)
    {
      synchronized (listeners)
      {
        if (internalListeners.get(listener) != null)
        {
          final InternalLocationListener internalListener = internalListeners.get(listener);
          listeners.remove(internalListener);
          internalListeners.remove(listener);
          Log.d(getClass().getSimpleName(), "removing listener " + listener + " => " + listeners.size());
          removed = true;
        }
        else
        {
          Log.d(getClass().getSimpleName(), "not removing listener " + listener + " : not registered");
          removed = false;
        }
      }
    }

    // Gestion
    if (removed && doManage)
    {
      manageListeners(false);
    }
  }

  /**
   * 
   * @param displayInactive
   * @param balise
   * @param releve
   * @return
   */
  private static boolean isBaliseUsable(final boolean displayInactive, final Balise balise, final Releve releve)
  {
    if (balise == null)
    {
      return false;
    }

    final boolean baliseActive = !Utils.isBooleanNull(balise.active) && Utils.getBooleanValue(balise.active);
    final boolean releveValide = baliseActive && (releve != null) && (releve.date != null);
    return (displayInactive || baliseActive) && !Double.isNaN(balise.latitude) && !Double.isNaN(balise.longitude) && (displayInactive || releveValide);
  }

  /**
   * 
   * @param context
   * @param location
   * @param providersService
   * @return
   */
  public static List<BaliseFavorite> getProximityBalises(final Context context, final Location location, final IFullProvidersService providersService)
  {
    // Centre
    final GeoPoint locationPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

    // Initialisations
    final boolean debugMode = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
    final boolean rayonBalisesProximiteLimited = sharedPreferences.getBoolean(resources.getString(R.string.config_favorites_proximity_radius_limited_key), Boolean.parseBoolean(resources.getString(R.string.config_error_reports_default)));
    final int rayonBalisesProximite = sharedPreferences.getInt(resources.getString(R.string.config_favorites_proximity_radius_limit_key), Integer.parseInt(resources.getString(R.string.config_favorites_proximity_radius_limit_default), 10));
    final int nbBalisesProximiteMax = sharedPreferences.getInt(resources.getString(R.string.config_favorites_proximity_nb_key), Integer.parseInt(resources.getString(R.string.config_favorites_proximity_nb_default), 10));
    final boolean displayInactive = !sharedPreferences.getBoolean(resources.getString(R.string.config_map_balise_hide_inactive_key), Boolean.parseBoolean(resources.getString(R.string.config_map_balise_hide_inactive_default)));
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
    final String[] hiddens = resources.getStringArray(R.array.providers_hidden);
    final SortedSet<BaliseFavorite> balises = new TreeSet<BaliseFavorite>();

    // Pour chaque provider actif
    final GeoPoint balisePoint = new GeoPoint();
    for (int i = 0; i < keys.length; i++)
    {
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      final boolean hidden = ActivityCommons.isBaliseProviderHidden(keys[i], hiddens[i]);
      if ((debugMode || !forDebug) && !hidden)
      {
        final List<String> countries = BaliseProviderUtils.getBaliseProviderActiveCountries(context, sharedPreferences, keys[i], i);
        for (final String country : countries)
        {
          final String fullKey = BaliseProviderUtils.getBaliseProviderFullKey(keys[i], country);
          final BaliseProvider provider = providersService.getBaliseProvider(fullKey);
          if (provider != null)
          {
            for (final Balise balise : provider.getBalises())
            {
              final Releve releve = providersService.getBaliseProvider(fullKey).getReleveById(balise.id);
              if (isBaliseUsable(displayInactive, balise, releve))
              {
                balisePoint.set(balise.latitude, balise.longitude);
                final double distance = MercatorProjection.calculateDistance(locationPoint, balisePoint);
                final double bearing = MercatorProjection.calculateBearing(locationPoint, balisePoint);
                balises.add(new BaliseFavorite(fullKey, balise.id, Double.valueOf(distance), Double.valueOf(bearing), providersService.getFavoritesService()));
              }
            }
          }
        }
      }
    }

    // Filtrage de la liste
    final List<BaliseFavorite> proches = new ArrayList<BaliseFavorite>();
    for (final BaliseFavorite balise : balises)
    {
      if (!rayonBalisesProximiteLimited || (balise.getDistance().doubleValue() <= rayonBalisesProximite))
      {
        proches.add(balise);
        if (proches.size() >= nbBalisesProximiteMax)
        {
          break;
        }
      }
    }

    // Fin
    return proches;
  }

  /**
   * 
   * @param bearing
   * @param nbSecteurs
   * @return
   */
  private static int getSecteur(final double bearing, final int nbSecteurs)
  {
    final double secteur = 360 / nbSecteurs;
    final double demiSecteur = secteur / 2;

    return (int)Math.floor(((bearing + demiSecteur) % 360) / secteur);
  }

  /**
   * 
   * @param context
   * @param location
   * @param nbSecteurs
   * @param providersService
   * @return
   */
  public static BaliseFavorite[] getAroundBalises(final Context context, final Location location, final int nbSecteurs, final IFullProvidersService providersService)
  {
    // Initialisations
    final int indiceBalisePlusProche = nbSecteurs;
    final double[] distances = new double[nbSecteurs + 1];
    final BaliseFavorite[] balises = new BaliseFavorite[nbSecteurs + 1];
    for (int i = 0; i < nbSecteurs + 1; i++)
    {
      distances[i] = Double.MAX_VALUE;
      balises[i] = null;
    }

    // Centre
    final GeoPoint locationPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

    // Initialisations
    final boolean debugMode = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
    final int rayonMini = sharedPreferences.getInt(resources.getString(R.string.config_favorites_around_min_key), Integer.parseInt(resources.getString(R.string.config_favorites_around_min_default), 10));
    final int rayonMaxi = sharedPreferences.getInt(resources.getString(R.string.config_favorites_around_max_key), Integer.parseInt(resources.getString(R.string.config_favorites_around_max_default), 10));
    final boolean displayInactive = !sharedPreferences.getBoolean(resources.getString(R.string.config_map_balise_hide_inactive_key), Boolean.parseBoolean(resources.getString(R.string.config_map_balise_hide_inactive_default)));
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
    final String[] hiddens = resources.getStringArray(R.array.providers_hidden);

    // Pour chaque provider actif
    final GeoPoint balisePoint = new GeoPoint();
    for (int i = 0; i < keys.length; i++)
    {
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      final boolean hidden = ActivityCommons.isBaliseProviderHidden(keys[i], hiddens[i]);
      if ((debugMode || !forDebug) && !hidden)
      {
        final List<String> countries = BaliseProviderUtils.getBaliseProviderActiveCountries(context, sharedPreferences, keys[i], i);
        for (final String country : countries)
        {
          final String fullKey = BaliseProviderUtils.getBaliseProviderFullKey(keys[i], country);
          final BaliseProvider provider = providersService.getBaliseProvider(fullKey);
          if (provider != null)
          {
            for (final Balise balise : provider.getBalises())
            {
              final Releve releve = providersService.getBaliseProvider(fullKey).getReleveById(balise.id);
              if (isBaliseUsable(displayInactive, balise, releve))
              {
                balisePoint.set(balise.latitude, balise.longitude);
                final double distance = MercatorProjection.calculateDistance(locationPoint, balisePoint);
                final double bearing = MercatorProjection.calculateBearing(locationPoint, balisePoint);
                // Recherche par secteur
                final int secteur = getSecteur(bearing, nbSecteurs);
                if ((distance >= rayonMini) && (distance <= rayonMaxi) && (distance < distances[secteur]))
                {
                  balises[secteur] = new BaliseFavorite(fullKey, balise.id, Double.valueOf(distance), Double.valueOf(bearing), providersService.getFavoritesService());
                  distances[secteur] = distance;
                }
                // Recherche de la plus proche
                if ((distance < rayonMini) && (distance < distances[indiceBalisePlusProche]))
                {
                  balises[indiceBalisePlusProche] = new BaliseFavorite(fullKey, balise.id, Double.valueOf(distance), Double.valueOf(bearing), providersService.getFavoritesService());
                  distances[indiceBalisePlusProche] = distance;
                }
              }
            }
          }
        }
      }
    }

    // Fin
    return balises;
  }

  /**
   * 
   * @param screenOff
   */
  public void setScreenOff(final boolean screenOff)
  {
    // MAJ du flag
    synchronized (manageLock)
    {
      this.screenOff = screenOff;
    }

    // Gestion
    manageListeners(!screenOff);
  }

  @Override
  public void shutdown()
  {
    // Fin du Thread de changement de localisation
    onLocationChangedThread.interrupt();
    ThreadUtils.join(onLocationChangedThread);

    // Retrait du broadcastreceiver
    if (providersReceiver != null)
    {
      context.unregisterReceiver(providersReceiver);
      providersReceiver.onShutdown();
      providersReceiver = null;
    }

    // Parent
    super.shutdown();

    // Retrait des listeners
    locationManager.removeUpdates(enabledNetworkListener);
    enabledNetworkListener.onShutdown();
    if (disabledNetworkListener != null)
    {
      locationManager.removeUpdates(disabledNetworkListener);
      disabledNetworkListener.onShutdown();
    }
    locationManager.removeUpdates(enabledGpsListener);
    enabledGpsListener.onShutdown();
    if (disabledGpsListener != null)
    {
      locationManager.removeUpdates(disabledGpsListener);
      disabledGpsListener.onShutdown();
    }
  }
}
