package org.pedro.android.mobibalises_common.provider;

import java.util.List;
import java.util.Locale;

import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.spots.Spot;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;

/**
 * 
 * @author pedro.m
 */
public abstract class SpotProviderUtils
{
  private static final String SUFFIXE_INFOS = "_infos";

  /**
   * 
   * @param fullKey
   * @return
   */
  public static String getSpotProviderCountryKey(final String fullKey)
  {
    final int lastPoint = fullKey.lastIndexOf(Strings.CHAR_POINT);
    if (lastPoint > 0)
    {
      return fullKey.substring(lastPoint + 1);
    }

    return null;
  }

  /**
   * 
   * @param fullKey
   * @return
   */
  public static String getSpotProviderKey(final String fullKey)
  {
    final int lastPoint = fullKey.lastIndexOf(Strings.CHAR_POINT);
    if (lastPoint > 0)
    {
      return fullKey.substring(0, lastPoint);
    }

    return null;
  }

  /**
   * 
   * @param fullKey
   * @param resources
   * @return
   */
  public static String getSpotProviderName(final String fullKey, final Resources resources)
  {
    // Initialisation
    final String[] keys = resources.getStringArray(R.array.site_providers_keys);
    final String[] titles = resources.getStringArray(R.array.site_providers_titles);

    // Parcours de clefs de chaque provider de site
    for (int i = 0; i < keys.length; i++)
    {
      if (keys[i].equals(fullKey))
      {
        return titles[i];
      }
    }

    return null;
  }

  /**
   * 
   * @param countryCode
   * @return
   */
  public static String getCountryName(final String countryCode)
  {
    return new Locale(countryCode, countryCode).getDisplayCountry(Locale.getDefault());
  }

  /**
   * 
   * @param providerKey
   * @return
   */
  public static String getInfosIdName(final String providerKey)
  {
    return new StringBuilder(providerKey.replace(Strings.CHAR_POINT, Strings.CHAR_UNDERSCORE)).append(SUFFIXE_INFOS).toString();
  }

  /**
   * 
   * @param providerKey
   * @param countryKey
   * @return
   */
  public static String getFullSpotProviderKey(final String providerKey, final String countryKey)
  {
    return new StringBuilder(providerKey).append(Strings.CHAR_POINT).append(countryKey).toString();
  }

  /**
   * 
   * @param spots
   * @param id
   * @return
   */
  public static Spot findSpot(final List<Spot> spots, final String id)
  {
    for (final Spot spot : spots)
    {
      if (id.equals(spot.id))
      {
        return spot;
      }
    }

    return null;
  }

  /**
   * 
   * @param context
   * @param resources
   * @param sharedPreferences
   * @return
   */
  public static int countSpotProvidersChecked(final Context context, final Resources resources, final SharedPreferences sharedPreferences)
  {
    // Initialisations
    final String[] keys = resources.getStringArray(R.array.site_providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.site_providers_forDebugs);
    final boolean debugMode = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    int count = 0;

    // Pour chaque provider
    for (int i = 0; i < keys.length; i++)
    {
      // On ne prend en compte le provider que si mode debug ou non provider de debug
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      if (debugMode || !forDebug)
      {
        final boolean checked = sharedPreferences.getBoolean(keys[i], false);
        if (checked)
        {
          count++;
        }
      }
    }

    return count;
  }
}
