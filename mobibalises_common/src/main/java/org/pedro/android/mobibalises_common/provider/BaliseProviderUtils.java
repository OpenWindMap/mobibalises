package org.pedro.android.mobibalises_common.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.balises.BidonProvider;
import org.pedro.balises.Utils;
import org.pedro.balises.ffvl.FfvlProvider;
import org.pedro.balises.metar.AviationWeatherListMetarProvider;
import org.pedro.balises.metar.OgimetMetarProvider;
import org.pedro.balises.pioupiou.PioupiouProvider;
import org.pedro.balises.romma.XmlRommaProvider;
import org.pedro.balises.synop.OgimetSynopProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;

/**
 * 
 * @author pedro.m
 */
public abstract class BaliseProviderUtils
{
  // Limites pour les tendances de vent
  private static final double DELTA_STABLE_LIMITE          = 2;
  private static final double DELTA_POURCENT_STABLE_LIMITE = 0.05;
  private static final double DELTA_FAIBLE_LIMITE          = 5;
  private static final double DELTA_POURCENT_FAIBLE_LIMITE = 0.20;

  /**
   * 
   * @author pedro.m
   */
  public static enum TendanceVent
  {
    INCONNUE, STABLE, FAIBLE_HAUSSE, FORTE_HAUSSE, FAIBLE_BAISSE, FORTE_BAISSE
  }

  /**
   * 
   * @param countryRegion
   * @return
   */
  public static String getCountryCode(final String countryRegion)
  {
    if ((countryRegion.length() == 5) && (countryRegion.charAt(2) == Strings.CHAR_MOINS))
    {
      return countryRegion.substring(0, 2);
    }

    return countryRegion;
  }

  /**
   * 
   * @param countryRegion
   * @return
   */
  public static String getRegionCode(final String countryRegion)
  {
    if ((countryRegion.length() == 5) && (countryRegion.charAt(2) == Strings.CHAR_MOINS))
    {
      return countryRegion.substring(3, 5);
    }

    return null;
  }

  /**
   * 
   * @param country
   * @param region
   * @return
   */
  public static String getCountryRegion(final String country, final String region)
  {
    // Initialisations
    final StringBuilder builder = new StringBuilder();

    // Pays
    builder.append(country);

    // Region
    if (!Utils.isStringVide(region))
    {
      builder.append(Strings.CHAR_MOINS);
      builder.append(region);
    }

    // Fin
    return builder.toString();
  }

  /**
   * 
   * @param key
   * @param country
   * @return
   */
  public static String getBaliseProviderFullKey(final String key, final String country)
  {
    return new StringBuilder().append(key).append(Strings.CHAR_UNDERSCORE).append(country).toString();
  }

  /**
   * 
   * @param fullName
   * @return
   */
  public static String getBaliseProviderSimpleName(final String fullName)
  {
    final int index = fullName.lastIndexOf(Strings.CHAR_POINT);
    if (index < 0)
    {
      return fullName;
    }

    return fullName.substring(0, index);
  }

  /**
   * 
   * @param fullKey
   * @return
   */
  public static String getBaliseProviderCountryCode(final String fullKey)
  {
    final int index = fullKey.lastIndexOf(Strings.CHAR_UNDERSCORE);
    if (index < 0)
    {
      return null;
    }

    return fullKey.substring(index + 1);
  }

  /**
   * 
   * @param fullKey
   * @return
   */
  public static String getBaliseProviderSimpleKey(final String fullKey)
  {
    final int index = fullKey.lastIndexOf(Strings.CHAR_UNDERSCORE);
    if (index < 0)
    {
      return fullKey;
    }

    return fullKey.substring(0, index);
  }

  /**
   * 
   * @param context
   * @param countryCode
   * @return
   */
  public static boolean isCountryAvailable(final Context context, final String countryCode)
  {
    // METAR
    if (OgimetMetarProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // SYNOP
    if (OgimetSynopProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // FFVL + FFVL-MobiBalises
    if (FfvlProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // PiouPiou
    if (PioupiouProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // ROMMA + ROMMA-MobiBalises
    if (XmlRommaProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // Bidon (debug)
    final boolean debugMode = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    if (debugMode && BidonProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // Sinon...
    return false;
  }

  /**
   * 
   * @param context
   * @param countryCode
   * @param regionCode
   * @return
   */
  public static boolean[] getAvailableBalisesProviders(final Context context, final String countryCode, final String regionCode)
  {
    // Initialisations
    final Resources resources = context.getResources();
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
    final String[] hiddens = resources.getStringArray(R.array.providers_hidden);
    final boolean debugMode = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    final boolean[] availables = new boolean[keys.length];

    // Pour chaque provider
    for (int i = 0; i < keys.length; i++)
    {
      // On ne prend en compte le provider que si mode debug ou non provider de debug
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      final boolean hidden = ActivityCommons.isBaliseProviderHidden(keys[i], hiddens[i]);
      if ((debugMode || !forDebug) && !hidden)
      {
        // FFVL-MobiBalises
        if ((i == 0) && FfvlProvider.getAvailableCountries().contains(countryCode))
        {
          availables[i] = true;
        }
        // Pioupiou-MobiBalises
        else if ((i == 1) && PioupiouProvider.getAvailableCountries().contains(countryCode))
        {
          availables[i] = true;
        }
        // METAR
        else if ((i == 2) && (!Utils.isStringVide(regionCode) || !AviationWeatherListMetarProvider.isRegional(countryCode)) && OgimetMetarProvider.getAvailableCountries().contains(countryCode))
        {

          availables[i] = true;
        }
        // ROMMA-MobiBalises
        else if ((i == 3) && XmlRommaProvider.getAvailableCountries().contains(countryCode))
        {
          availables[i] = true;
        }
        // SYNOP
        else if ((i == 4) && Utils.isStringVide(regionCode) && OgimetSynopProvider.getAvailableCountries().contains(countryCode))
        {
          availables[i] = true;
        }
        // FFVL (direct)
        else if ((i == 5) && FfvlProvider.getAvailableCountries().contains(countryCode))
        {
          availables[i] = true;
        }
        // ROMMA (direct)
        else if ((i == 6) && XmlRommaProvider.getAvailableCountries().contains(countryCode))
        {
          availables[i] = true;
        }
        // Bidon
        else if ((i == 7) && BidonProvider.getAvailableCountries().contains(countryCode))
        {
          availables[i] = true;
        }
      }
    }

    return availables;
  }

  /**
   * 
   * @param context
   * @param countryRegionCode
   * @param index
   * @return
   */
  private static boolean getDefault(final Context context, final String countryRegionCode, final int index)
  {
    final String[] defaults = context.getResources().getStringArray(R.array.providers_defaults);

    return Boolean.parseBoolean(defaults[index]) && getDefault(context, countryRegionCode);
  }

  /**
   * 
   * @param context
   * @param countryRegionCode
   * @return
   */
  public static boolean getDefault(final Context context, final String countryRegionCode)
  {
    // Initialisations
    final String countryCode = getCountryCode(countryRegionCode);
    final String regionCode = getRegionCode(countryRegionCode);
    final String currentCountryCode = Locale.getDefault().getCountry();
    final String defaultCountry = (isCountryAvailable(context, currentCountryCode) ? currentCountryCode : Locale.FRANCE.getCountry());

    // Pays
    if (Utils.isStringVide(regionCode))
    {
      return defaultCountry.equals(countryCode);
    }

    // Region
    final int defaultRegionId = context.getResources().getIdentifier(new StringBuilder(AbstractBalisesPreferencesActivity.RESOURCES_REGION_CODE_DEFAULT_PREFIX).append(countryCode.toUpperCase()).toString(), Strings.RESOURCES_STRING,
        context.getPackageName());
    final String defaultRegion = context.getResources().getString(defaultRegionId);
    return (defaultCountry.equals(countryCode) && defaultRegion.equals(regionCode));
  }

  /**
   * 
   * @param context
   * @param countryCode
   * @param availableProviders
   */
  public static List<?>[] getBaliseProvidersCheckedForCountry(final Context context, final SharedPreferences sharedPreferences, final String countryCode, final boolean[] availableProviders)
  {
    // Initialisations
    final Resources resources = context.getResources();
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    final String[] titles = resources.getStringArray(R.array.providers_titles);

    // Pour chaque provider disponible
    final List<String> providerLabels = new ArrayList<String>();
    final List<String> providerKeys = new ArrayList<String>();
    final List<Boolean> providerCheckeds = new ArrayList<Boolean>();
    for (int i = 0; i < availableProviders.length; i++)
    {
      if (availableProviders[i])
      {
        // Clefs et labels
        providerKeys.add(keys[i]);
        providerLabels.add(titles[i]);

        // Cochage ou non
        final String prefKey = AbstractBalisesPreferencesActivity.formatBaliseProviderPreferenceKey(keys[i], countryCode);
        providerCheckeds.add(Boolean.valueOf(sharedPreferences.getBoolean(prefKey, getDefault(context, countryCode, i))));
      }
    }

    return new List<?>[] { providerKeys, providerLabels, providerCheckeds };
  }

  /**
   * 
   * @param context
   * @param index
   * @return
   */
  public static List<String> getBaliseProviderCountries(final Context context, final int index)
  {
    // Liste des pays dispos
    final List<String> countries;
    final boolean hasRegions;
    switch (index)
    {
      case 0: // FFVL-MobiBalises
        countries = FfvlProvider.getAvailableCountries();
        hasRegions = false;
        break;
      case 1: // PiouPiou-MobiBalises
        countries = PioupiouProvider.getAvailableCountries();
        hasRegions = false;
        break;
      case 2: // METAR
        countries = OgimetMetarProvider.getAvailableCountries();
        hasRegions = true;
        break;
      case 3: // ROMMA-MobiBalises
        countries = XmlRommaProvider.getAvailableCountries();
        hasRegions = false;
        break;
      case 4: // SYNOP
        countries = OgimetSynopProvider.getAvailableCountries();
        hasRegions = false;
        break;
      case 5: // FFVL
        countries = FfvlProvider.getAvailableCountries();
        hasRegions = false;
        break;
      case 6: // ROMMA
        countries = XmlRommaProvider.getAvailableCountries();
        hasRegions = false;
        break;
      case 7: // Bidon
        countries = BidonProvider.getAvailableCountries();
        hasRegions = false;
        break;
      default:
        throw new RuntimeException();
    }

    // Avec les regions
    final List<String> countriesRegions = new ArrayList<String>();
    for (final String country : countries)
    {
      final int regionCodesId = context.getResources().getIdentifier(new StringBuilder(AbstractBalisesPreferencesActivity.RESOURCES_REGION_CODES_PREFIX).append(country.toUpperCase()).toString(), Strings.RESOURCES_ARRAY,
          context.getPackageName());
      if (hasRegions && (regionCodesId > 0))
      {
        final String[] regionCodes = context.getResources().getStringArray(regionCodesId);
        for (final String region : regionCodes)
        {
          countriesRegions.add(getCountryRegion(country, region));
        }
      }
      else
      {
        countriesRegions.add(country);
      }
    }

    return countriesRegions;
  }

  /**
   * 
   * @param context
   * @param sharedPreferences
   * @param key
   * @param index
   * @return
   */
  public static List<String> getBaliseProviderActiveCountries(final Context context, final SharedPreferences sharedPreferences, final String key, final int index)
  {
    // Liste des pays dispos
    final List<String> countries = getBaliseProviderCountries(context, index);

    // Pour chaque pays dispo
    final List<String> activeCountries = new ArrayList<String>();
    for (final String country : countries)
    {
      if (isBaliseProviderCountryActive(context, sharedPreferences, key, country, index, null))
      {
        activeCountries.add(country);
      }
    }

    return activeCountries;
  }

  /**
   * 
   * @param context
   * @param sharedPreferences
   * @param key
   * @param country
   * @param index
   * @param editor
   * @return
   */
  public static boolean isBaliseProviderCountryActive(final Context context, final SharedPreferences sharedPreferences, final String key, final String country, final int index, final SharedPreferences.Editor editor)
  {
    // Pays actif ?
    final String countryPref = AbstractBalisesPreferencesActivity.formatBaliseProviderCountryPreferenceKey(country, false);
    final boolean countryActive = sharedPreferences.getBoolean(countryPref, getDefault(context, country));

    // Provider actif ?
    final String providerPrefKey = AbstractBalisesPreferencesActivity.formatBaliseProviderPreferenceKey(key, country);
    final boolean countryProviderActive = sharedPreferences.getBoolean(providerPrefKey, getDefault(context, country, index));

    // Sauvegarde si demande
    if (editor != null)
    {
      editor.putBoolean(countryPref, countryActive);
      editor.putBoolean(providerPrefKey, countryProviderActive);
    }

    // Si pays inactif
    if (!countryActive)
    {
      return false;
    }

    // Sinon provider actif ?
    return countryProviderActive;
  }

  /**
   * 
   * @param deltaPeremptionMs
   * @param date
   * @param datePrecedent
   * @param valeur
   * @param delta
   * @return
   */
  public static TendanceVent getTendanceVent(final long deltaPeremptionMs, final Date date, final Date datePrecedent, final double valeur, final double delta)
  {
    // Tendance incalculable ou perimee
    if (Double.isNaN(delta) || Double.isNaN(valeur) || (date == null) || (datePrecedent == null) || (date.getTime() - datePrecedent.getTime() > deltaPeremptionMs))
    {
      return TendanceVent.INCONNUE;
    }

    // Variation %
    final double valeurPrecedente = valeur - delta;
    final double deltaPourcent;
    if (valeurPrecedente == 0)
    {
      deltaPourcent = Double.MAX_VALUE;
    }
    else
    {
      deltaPourcent = delta / valeurPrecedente;
    }
    final double absDelta = Math.abs(delta);
    final double absDeltaPourcent = Math.abs(deltaPourcent);

    // Elaboration
    if ((absDelta <= DELTA_STABLE_LIMITE) || (absDeltaPourcent <= DELTA_POURCENT_STABLE_LIMITE))
    {
      return TendanceVent.STABLE;
    }
    else if ((absDelta <= DELTA_FAIBLE_LIMITE) || (absDeltaPourcent <= DELTA_POURCENT_FAIBLE_LIMITE))
    {
      return (delta > 0 ? TendanceVent.FAIBLE_HAUSSE : TendanceVent.FAIBLE_BAISSE);
    }
    else
    {
      return (delta > 0 ? TendanceVent.FORTE_HAUSSE : TendanceVent.FORTE_BAISSE);
    }
  }

  /**
   * 
   * @param countryCode
   * @return
   */
  public static boolean hasRegionalProviders(final String countryCode)
  {
    // METAR
    if (OgimetMetarProvider.getAvailableCountries().contains(countryCode) && AviationWeatherListMetarProvider.isRegional(countryCode))
    {
      return true;
    }

    // Sinon...
    return false;
  }

  /**
   * 
   * @param context
   * @param countryCode
   * @return
   */
  public static boolean hasNationalProviders(final Context context, final String countryCode)
  {
    // METAR
    if (OgimetMetarProvider.getAvailableCountries().contains(countryCode) && !AviationWeatherListMetarProvider.isRegional(countryCode))
    {
      return true;
    }

    // SYNOP
    if (OgimetSynopProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // FFVL + FFVL-MobiBalises
    if (FfvlProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // Pioupiou-MobiBalises
    if (PioupiouProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // ROMMA + ROMMA-MobiBalises
    if (XmlRommaProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // Bidon (debug)
    final boolean debugMode = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    if (debugMode && BidonProvider.getAvailableCountries().contains(countryCode))
    {
      return true;
    }

    // Sinon...
    return false;
  }

  /**
   * 
   * @param context
   * @param inCountryCode
   * @return
   */
  public static String getCountryContinentCode(final Context context, final String inCountryCode)
  {
    // Initialisations
    final Resources resources = context.getResources();

    // Pour chaque continent
    final String[] continentCodes = resources.getStringArray(R.array.continent_codes);
    for (final String continentCode : continentCodes)
    {
      final int countryCodesId = resources.getIdentifier(AbstractBalisesPreferencesActivity.RESOURCES_COUNTRY_CODES_PREFIX + continentCode, Strings.RESOURCES_ARRAY, context.getPackageName());
      final String[] countryCodes = resources.getStringArray(countryCodesId);

      // Pour chaque pays
      for (final String countryCode : countryCodes)
      {
        if (countryCode.equals(inCountryCode))
        {
          return continentCode;
        }
      }
    }

    return null;
  }
}
