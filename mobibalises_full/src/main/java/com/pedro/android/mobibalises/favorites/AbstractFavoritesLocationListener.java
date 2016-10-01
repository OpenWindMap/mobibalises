package com.pedro.android.mobibalises.favorites;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.balises.Balise;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.map.GeoPoint;
import org.pedro.map.MercatorProjection;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;

import com.pedro.android.mobibalises.service.IFullProvidersService;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractFavoritesLocationListener implements LocationListener
{
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
  protected static List<BaliseFavorite> getProximityBalises(final Context context, final Location location, final IFullProvidersService providersService)
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
          for (final Balise balise : providersService.getBaliseProvider(fullKey).getBalises())
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
  protected static BaliseFavorite[] getAroundBalises(final Context context, final Location location, final int nbSecteurs, final IFullProvidersService providersService)
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
          for (final Balise balise : providersService.getBaliseProvider(fullKey).getBalises())
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

    // Fin
    return balises;
  }
}
