package org.pedro.android.mobibalises_common.preferences;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.provider.SpotProviderUtils;
import org.pedro.android.mobibalises_common.search.SearchDatabaseHelper;
import org.pedro.android.mobibalises_common.search.SearchDatabaseHelper.SearchItem;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;
import org.pedro.android.mobibalises_common.service.IProvidersService;
import org.pedro.android.mobibalises_common.service.ProvidersServiceBinder;
import org.pedro.android.widget.SliderPreference;
import org.pedro.balises.Utils;
import org.pedro.spots.Orientation;
import org.pedro.spots.Pratique;
import org.pedro.spots.Spot;
import org.pedro.spots.SpotProvider;
import org.pedro.spots.TypeSpot;
import org.pedro.utils.ThreadUtils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractBalisesPreferencesActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener
{
  private static final String                         STRING_UNIT_SLIDER                              = "\\[unit\\]";
  public static final String                          RESOURCES_COUNTRY_CODES_PREFIX                  = "country_codes_";
  public static final String                          RESOURCES_REGION_CODES_PREFIX                   = "regional_codes_";
  private static final String                         RESOURCES_REGION_LABELS_PREFIX                  = "regional_names_";
  public static final String                          RESOURCES_REGION_CODE_DEFAULT_PREFIX            = "regional_code_default_";
  private static final String                         STRING_SEPARATEUR_PROVIDER_LABEL                = ", ";
  private static final String                         STRING_SEPARATEUR_COUNTRY_LABEL                 = STRING_SEPARATEUR_PROVIDER_LABEL;
  private static final String                         STRING_POINT_SPACE                              = "&#8226; ";

  static DateFormat                                   FORMAT_DATE_MAJ_SPOTS;

  public static final String                          INTENT_CACHE_CURRENT_SIZE                       = ".CacheCurrentSize";
  public static final String                          INTENT_ZOOM_LEVEL                               = ".OrientationsZoomLevel";
  public static final String                          INTENT_MODE                                     = ".Mode";
  public static final int                             INTENT_MODE_NORMAL                              = 1;
  public static final int                             INTENT_MODE_BALISES_SOURCES                     = 2;
  public static final int                             INTENT_MODE_SITES_SOURCES                       = 3;

  public static final String                          CONFIG_ZOOM                                     = "config.zoom";
  public static final String                          CONFIG_CENTER_LAT                               = "config.center.lat";
  public static final String                          CONFIG_CENTER_LON                               = "config.center.lon";
  public static final String                          CONFIG_TILE_PROVIDER_KEY                        = "config.tileproviderkey";
  public static final String                          CONFIG_DATA_WIND_BALISES                        = "config.balises_wind";
  public static final boolean                         CONFIG_DATA_WIND_BALISES_DEFAULT                = true;
  public static final String                          CONFIG_DATA_WEATHER_BALISES                     = "config.balises_weather";
  public static final String                          CONFIG_DATA_SITES                               = "config.sites";
  public static final boolean                         CONFIG_DATA_SITES_DEFAULT                       = false;
  public static final String                          CONFIG_DATA_WEBCAMS                             = "config.webcams";
  public static final boolean                         CONFIG_DATA_WEBCAMS_DEFAULT                     = true;
  public static final String                          CONFIG_KEY_TYPE_SPOT                            = "config.type_spot";
  public static final String                          CONFIG_KEY_PRATIQUE                             = "config.pratique";
  public static final String                          CONFIG_KEY_ORIENTATION                          = "config_data_sites_orientation_";
  public static final String                          CONFIG_KEY_WEBCAMS_PRATIQUE                     = "config.webcams.pratique";
  public static final String                          CONFIG_LAST_VERSION_USED                        = "config.last_version_used";
  // N'est plus utilise : public static final String        CONFIG_LAST_ERROR_DISPLAYED              = "config.last_error_displayed";
  public static final String                          CONFIG_LAST_BALISES_UPDATE_DATE                 = "config.last_balises_update_date";
  public static final String                          CONFIG_LAST_BALISES_CHECK_LOCAL_DATE            = "config.last_balises_check_local_date";
  public static final String                          CONFIG_LAST_RELEVES_UPDATE_DATE                 = "config.last_releves_update_date";
  public static final String                          CONFIG_LAST_RELEVES_CHECK_LOCAL_DATE            = "config.last_releves_check_local_date";
  public static final String                          UNIT_SPEED_PREFIX                               = "unit_speed_";
  public static final String                          UNIT_SPEED_FACTOR_PREFIX                        = "unit_speed_factor_";
  public static final String                          UNIT_ALTITUDE_PREFIX                            = "unit_altitude_";
  public static final String                          UNIT_ALTITUDE_FACTOR_PREFIX                     = "unit_altitude_factor_";
  public static final String                          UNIT_DISTANCE_PREFIX                            = "unit_distance_";
  public static final String                          UNIT_DISTANCE_FACTOR_PREFIX                     = "unit_distance_factor_";
  public static final String                          UNIT_TEMPERATURE_PREFIX                         = "unit_temperature_";
  public static final String                          UNIT_TEMPERATURE_FACTOR_PREFIX                  = "unit_temperature_factor_";
  public static final String                          UNIT_TEMPERATURE_DELTA_PREFIX                   = "unit_temperature_delta_";
  public static final String                          CONFIG_LAST_FFVL_MESSAGE                        = "config.last_ffvl_message";

  public static final String                          CONFIG_START_ACTIVITY                           = "config.start_activity";

  public static final String                          CONFIG_FAVORITES_CURRENT_LABEL                  = "config.favorites_current_label";
  public static final String                          CONFIG_FAVORITES_PROXIMITY_MODE                 = "config.favorites_proximity_mode";
  public static final String                          CONFIG_FAVORITES_DISPLAY_MODE                   = "config.favorites_display_mode";

  private static final String                         CONFIG_BALISE_PROVIDERS_COUNTRY_PREFIX          = "config.balise_providers.country.";
  private static final String                         CONFIG_BALISE_PROVIDERS_REGIONAL_COUNTRY_PREFIX = "config.balise_providers.regional-country.";
  private static final String                         CONFIG_BALISE_PROVIDERS_PROVIDER_PREFIX         = "config.balise_providers.provider.";
  private static final String                         CONFIG_BALISE_PROVIDERS_CONTINENT_PREFIX        = "config.balise_providers.continent.";

  public static final String                          CONFIG_HISTORY_MODE                             = "config.historyMode";

  public static final String                          CONFIG_ALARM_MODE                               = "config.alarmMode";

  public static final int                             NB_ORIENTATIONS                                 = 8;                                              // N, NE, E, SE, S, SO, O, NO

  private PreferenceManager                           manager;
  Resources                                           resources;
  protected SharedPreferences                         sharedPreferences;

  protected IProvidersService                         providersService;
  private ProvidersServiceConnection                  providersServiceConnection;

  ProgressDialog                                      progressDialogDownload;
  boolean                                             continueDownload;
  final List<Thread>                                  spotDownloadThreads                             = new ArrayList<Thread>();

  boolean                                             forceSpotProvidersUpdate;

  private boolean[]                                   baliseProvidersFlags;
  private boolean[]                                   spotProvidersFlags;

  final Map<String, String>                           continentCountry                                = new HashMap<String, String>();

  private int                                         mode;

  private final BaliseCountryPreferenceComparator     baliseCountryPreferenceComparator               = new BaliseCountryPreferenceComparator(this);
  private final BaliseRegionPreferenceComparator      baliseRegionPreferenceComparator                = new BaliseRegionPreferenceComparator();
  private final BaliseProviderPreferenceClickListener baliseProviderPreferenceClickListener           = new BaliseProviderPreferenceClickListener(this);

  private static final String                         CONFIG_INIT_BALISE_PROVIDERS_PREFERENCES_DONE   = "config.initBaliseProvidersPreferencesDone";
  private static final Object                         initBaliseProvidersPreferencesLock              = new Object();

  /**
   * 
   * @author pedro.m
   */
  private static final class CheckBoxPreferenceHandler extends Handler
  {
    private final WeakReference<CheckBoxPreference> chkPreference;

    /**
     * 
     * @param chkPreference
     */
    CheckBoxPreferenceHandler(final CheckBoxPreference chkPreference)
    {
      super(Looper.getMainLooper());
      this.chkPreference = new WeakReference<CheckBoxPreference>(chkPreference);
    }

    @Override
    public void handleMessage(final Message msg)
    {
      chkPreference.get().setChecked(msg.arg1 == 1);
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class SpotsDownloadProgressHandler extends Handler
  {
    private final WeakReference<AbstractBalisesPreferencesActivity> prefsActivity;

    /**
     * 
     * @param prefsActivity
     */
    SpotsDownloadProgressHandler(final AbstractBalisesPreferencesActivity prefsActivity)
    {
      super(Looper.getMainLooper());
      this.prefsActivity = new WeakReference<AbstractBalisesPreferencesActivity>(prefsActivity);
    }

    @Override
    public void handleMessage(final Message msg)
    {
      if ((prefsActivity.get() != null) && !prefsActivity.get().isFinishing())
      {
        prefsActivity.get().progressDialogDownload.setMax(msg.arg2);
        if (msg.arg1 > msg.arg2)
        {
          prefsActivity.get().progressDialogDownload.dismiss();
          if ((msg.obj != null) && prefsActivity.get().continueDownload)
          {
            String message = Strings.VIDE;
            boolean first = true;
            final IOException[] exceptions = (IOException[])msg.obj;
            for (final IOException exception : exceptions)
            {
              if (exception != null)
              {
                // Message a afficher a l'ecran
                if (!first)
                {
                  message += Strings.NEWLINE;
                }
                message += STRING_POINT_SPACE + exception.getMessage();
                first = false;
              }
            }
            ActivityCommons.alertDialog(prefsActivity.get(), ActivityCommons.ALERT_DIALOG_SPOT_DOWNLOAD_ERROR, -1, prefsActivity.get().resources.getString(R.string.message_download_error_title), Html.fromHtml(Strings.toHtmlString(message))
                .toString(), null, true, null, 0);
          }
        }
        else
        {
          prefsActivity.get().progressDialogDownload.setProgress(msg.arg1);
        }
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class SpotsDownloadThread extends Thread
  {
    private final WeakReference<AbstractBalisesPreferencesActivity> prefsActivity;
    private final SpotsDownloadProgressHandler                      progressHandler;
    private final CheckBoxPreferenceHandler                         checkboxHandler;
    private final String                                            providerKey;
    private final SpotProvider                                      provider;
    private final List<String>                                      countriesToDownload;

    /**
     * 
     * @param prefsActivity
     * @param provider
     * @param providerKey
     * @param countriesToDownload
     * @param progressHandler
     * @param checkboxHandler
     */
    SpotsDownloadThread(final AbstractBalisesPreferencesActivity prefsActivity, final SpotProvider provider, final String providerKey, final List<String> countriesToDownload, final SpotsDownloadProgressHandler progressHandler,
        final CheckBoxPreferenceHandler checkboxHandler)
    {
      super(SpotsDownloadThread.class.getName());
      this.prefsActivity = new WeakReference<AbstractBalisesPreferencesActivity>(prefsActivity);
      this.provider = provider;
      this.providerKey = providerKey;
      this.countriesToDownload = countriesToDownload;
      this.progressHandler = progressHandler;
      this.checkboxHandler = checkboxHandler;
    }

    @Override
    public void run()
    {
      downloadSpots();
    }

    /**
     * 
     */
    private void downloadSpots()
    {
      // Initialisations
      prefsActivity.get().continueDownload = true;
      boolean errorHappened = false;
      final int nbCountries = countriesToDownload.size();
      final IOException[] ioExceptions = new IOException[nbCountries];

      // Progression
      final Message firstProgressMsg = new Message();
      firstProgressMsg.what = 0;
      firstProgressMsg.arg1 = 0;
      firstProgressMsg.arg2 = nbCountries;
      progressHandler.sendMessage(firstProgressMsg);

      // Pour chaque pays a telecharger
      for (int ti = 1; (ti <= nbCountries) && prefsActivity.get().continueDownload && !Thread.currentThread().isInterrupted(); ti++)
      {
        // Initialisations
        final String forCountry = countriesToDownload.get(ti - 1);
        final String forCountryKey = SpotProviderUtils.getFullSpotProviderKey(providerKey, forCountry);

        // Telechargement
        boolean downloadOk = false;
        try
        {
          final List<Spot> spots = provider.getSpots(forCountry);

          // Interruption ?
          if (Thread.currentThread().isInterrupted())
          {
            break;
          }

          if ((spots != null) && (spots.size() > 0))
          {
            prefsActivity.get().providersService.storeSpots(providerKey, forCountry, spots);
            downloadOk = true;
          }
          else
          {
            // Pas de donnees
            throw new IOException(prefsActivity.get().resources.getString(R.string.message_download_error_nodata) + Strings.CHAR_SPACE + new Locale(forCountry, forCountry).getDisplayCountry(Locale.getDefault()));
          }
        }
        catch (final IOException ioe)
        {
          errorHappened = true;
          ioExceptions[ti - 1] = ioe;
          Log.e(getClass().getSimpleName(), "Erreur telechargement : ", ioe);
        }

        // Erreur lors du telechargement ou stockage : on decoche le pays
        if (!downloadOk)
        {
          final SharedPreferences.Editor editor = prefsActivity.get().sharedPreferences.edit();
          editor.putBoolean(forCountryKey, false);
          ActivityCommons.commitPreferences(editor);
        }

        // Progression
        final Message progressMsg = new Message();
        progressMsg.what = 0;
        progressMsg.arg1 = ti;
        progressMsg.arg2 = nbCountries;
        progressHandler.sendMessage(progressMsg);
      }

      // Progression (fin)
      final Message lastProgressMsg = new Message();
      lastProgressMsg.what = 0;
      lastProgressMsg.arg1 = nbCountries + 1;
      lastProgressMsg.arg2 = nbCountries;
      if (errorHappened)
      {
        lastProgressMsg.obj = ioExceptions;
      }
      progressHandler.sendMessage(lastProgressMsg);

      // Cochage ou non du provider selon les resultats
      if (!Thread.currentThread().isInterrupted())
      {
        try
        {
          boolean providerChecked = false;
          for (final String country : provider.getAvailableCountries())
          {
            // La clef du provider de site + pays
            final String threadForCountryKey = SpotProviderUtils.getFullSpotProviderKey(providerKey, country);

            // Le provider coche ?
            providerChecked = providerChecked || prefsActivity.get().sharedPreferences.getBoolean(threadForCountryKey, false);
          }

          // Provider coche ou non
          final Message chkMsg = new Message();
          chkMsg.what = 0;
          chkMsg.arg1 = providerChecked ? 1 : 0;
          checkboxHandler.sendMessage(chkMsg);
        }
        catch (final IOException ioe)
        {
          throw new RuntimeException(ioe);
        }
      }
    }
  }

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    // Debut
    Log.d(getClass().getSimpleName(), ">>> abstract.onCreate()");
    super.onCreate(savedInstanceState);

    // Vue
    setContentView(R.layout.preferences);

    // Initialisations
    manager = getPreferenceManager();
    resources = getApplicationContext().getResources();

    // Initialisation pays
    initBaliseProvidersPreferences(getApplicationContext());

    // Mode
    mode = getIntent().getIntExtra(getPackageName() + INTENT_MODE, INTENT_MODE_NORMAL);

    // Connexion au service
    initProvidersService();

    // Preferences partagees
    manager.setSharedPreferencesName(resources.getString(R.string.preferences_shared_name));
    manager.setSharedPreferencesMode(Context.MODE_PRIVATE);
    sharedPreferences = manager.getSharedPreferences();

    // Ajout de l'ecran
    switch (mode)
    {
      case INTENT_MODE_NORMAL:
        addPreferencesFromResource(R.xml.config);
        break;
      case INTENT_MODE_BALISES_SOURCES:
        addPreferencesFromResource(R.xml.config_balises_sources);
        break;
      case INTENT_MODE_SITES_SOURCES:
        addPreferencesFromResource(R.xml.config_sites_sources);
        break;
    }

    // Configuration des providers
    if ((mode == INTENT_MODE_BALISES_SOURCES) || (mode == INTENT_MODE_NORMAL))
    {
      initProviderContinentsPreferences();
    }

    // Configuration des providers de sites
    if ((mode == INTENT_MODE_SITES_SOURCES) || (mode == INTENT_MODE_NORMAL))
    {
      initSiteProvidersPreferences();
    }

    // Configuration des webcams
    if (mode == INTENT_MODE_NORMAL)
    {
      initWebcamProvidersPreferences();
    }

    // Configuration preferences
    if (mode == INTENT_MODE_NORMAL)
    {
      initPreferences();
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onCreate()");
  }

  /**
   * 
   */
  private void initPreferences()
  {
    // Action sur touch
    final Preference touchActionPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_touch_action_key));
    onPreferenceChange(touchActionPreference, sharedPreferences.getString(resources.getString(R.string.config_map_touch_action_key), resources.getString(R.string.config_map_touch_action_default)));
    touchActionPreference.setOnPreferenceChangeListener(this);

    // Collision METAR/SYNOP
    final Preference metarSynopCollisionPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_metar_synop_collision_key));
    onPreferenceChange(metarSynopCollisionPreference, sharedPreferences.getString(resources.getString(R.string.config_map_metar_synop_collision_key), resources.getString(R.string.config_map_metar_synop_collision_default)));
    metarSynopCollisionPreference.setOnPreferenceChangeListener(this);

    // Valeur de vent affichee
    final Preference windValuePreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_wind_key));
    onPreferenceChange(windValuePreference, sharedPreferences.getString(resources.getString(R.string.config_map_wind_key), resources.getString(R.string.config_map_wind_default)));
    windValuePreference.setOnPreferenceChangeListener(this);

    // Limite de vent : checkbox moy
    final Preference limitMoyCheckPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_limit_moy_check_key));
    onPreferenceChange(limitMoyCheckPreference,
        Boolean.valueOf(sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_moy_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_moy_check_default)))));
    limitMoyCheckPreference.setOnPreferenceChangeListener(this);

    // Limite de vent : valeur moy
    final Preference limitMoyEditPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_limit_moy_edit_key));
    onPreferenceChange(limitMoyEditPreference, Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_map_limit_moy_edit_key), Integer.parseInt(resources.getString(R.string.config_map_limit_moy_edit_default), 10))));
    limitMoyEditPreference.setOnPreferenceChangeListener(this);

    // Limite de vent : operateur
    final Preference limitOperatorPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_limit_operator_key));
    onPreferenceChange(limitOperatorPreference, sharedPreferences.getString(resources.getString(R.string.config_map_limit_operator_key), resources.getString(R.string.config_map_limit_operator_default)));
    limitOperatorPreference.setOnPreferenceChangeListener(this);

    // Limite de vent : checkbox max
    final Preference limitMaxCheckPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_limit_max_check_key));
    onPreferenceChange(limitMaxCheckPreference,
        Boolean.valueOf(sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_max_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_max_check_default)))));
    limitMaxCheckPreference.setOnPreferenceChangeListener(this);

    // Limite de vent : valeur max
    final Preference limitMaxEditPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_limit_max_edit_key));
    onPreferenceChange(limitMaxEditPreference, Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_map_limit_max_edit_key), Integer.parseInt(resources.getString(R.string.config_map_limit_max_edit_default), 10))));
    limitMaxEditPreference.setOnPreferenceChangeListener(this);

    // Limite de peremption releve
    final Preference outOfDatePreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_outofdate_key));
    onPreferenceChange(outOfDatePreference, Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_map_outofdate_key), Integer.parseInt(resources.getString(R.string.config_map_outofdate_default), 10))));
    outOfDatePreference.setOnPreferenceChangeListener(this);

    // Centrage sur click
    final Preference centeringPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_centering_key));
    onPreferenceChange(centeringPreference, Boolean.valueOf(sharedPreferences.getBoolean(resources.getString(R.string.config_map_centering_key), Boolean.parseBoolean(resources.getString(R.string.config_map_centering_default)))));
    centeringPreference.setOnPreferenceChangeListener(this);

    // Clef IGN perso
    final Preference ignOwnKeyPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_ign_own_key_key));
    onPreferenceChange(ignOwnKeyPreference, sharedPreferences.getString(resources.getString(R.string.config_map_ign_own_key_key), null));
    ignOwnKeyPreference.setOnPreferenceChangeListener(this);

    // Reset BD de recherhe
    final Preference searchResetPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_search_reset_key));
    searchResetPreference.setOnPreferenceClickListener(this);

    // Unite vitesse
    final Preference speedUnitPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_unit_speed_key));
    onPreferenceChange(speedUnitPreference, sharedPreferences.getString(resources.getString(R.string.config_unit_speed_key), resources.getString(R.string.config_unit_speed_default)));
    speedUnitPreference.setOnPreferenceChangeListener(this);

    // Unite altitude
    final Preference altitudeUnitPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_unit_altitude_key));
    onPreferenceChange(altitudeUnitPreference, sharedPreferences.getString(resources.getString(R.string.config_unit_altitude_key), resources.getString(R.string.config_unit_altitude_default)));
    altitudeUnitPreference.setOnPreferenceChangeListener(this);

    // Unite distance
    final Preference distanceUnitPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_unit_distance_key));
    onPreferenceChange(distanceUnitPreference, sharedPreferences.getString(resources.getString(R.string.config_unit_distance_key), resources.getString(R.string.config_unit_distance_default)));
    distanceUnitPreference.setOnPreferenceChangeListener(this);

    // Unite temperature
    final Preference temperatureUnitPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_unit_temperature_key));
    onPreferenceChange(temperatureUnitPreference, sharedPreferences.getString(resources.getString(R.string.config_unit_temperature_key), resources.getString(R.string.config_unit_temperature_default)));
    temperatureUnitPreference.setOnPreferenceChangeListener(this);

    // Google Analytics
    final Preference gaPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_ga_key));
    onPreferenceChange(gaPreference, Boolean.valueOf(sharedPreferences.getBoolean(resources.getString(R.string.config_ga_key), Boolean.parseBoolean(resources.getString(R.string.config_ga_default)))));
    gaPreference.setOnPreferenceChangeListener(this);

    // Valeur mini du mode "atour de moi"
    final Preference aroundMinPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_favorites_around_min_key));
    onPreferenceChange(aroundMinPreference, Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_favorites_around_min_key), Integer.parseInt(resources.getString(R.string.config_favorites_around_min_default), 10))));
    aroundMinPreference.setOnPreferenceChangeListener(this);

    // Valeur maxi du mode "atour de moi"
    final Preference aroundMaxPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_favorites_around_max_key));
    onPreferenceChange(aroundMaxPreference, Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_favorites_around_max_key), Integer.parseInt(resources.getString(R.string.config_favorites_around_max_default), 10))));
    aroundMaxPreference.setOnPreferenceChangeListener(this);

    // Debut plage mode historique
    final Preference historyModeBeginPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_history_mode_range_start_key));
    onPreferenceChange(historyModeBeginPreference,
        Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_history_mode_range_start_key), Integer.parseInt(resources.getString(R.string.config_history_mode_range_start_default), 10))));
    historyModeBeginPreference.setOnPreferenceChangeListener(this);

    // Fin plage mode historique
    final Preference historyModeEndPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_history_mode_range_end_key));
    onPreferenceChange(historyModeEndPreference,
        Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_history_mode_range_end_key), Integer.parseInt(resources.getString(R.string.config_history_mode_range_end_default), 10))));
    historyModeEndPreference.setOnPreferenceChangeListener(this);
  }

  /**
   * 
   * @param Context
   */
  public static void initBaliseProvidersPreferences(final Context context)
  {
    // Initialisations
    Log.d(AbstractBalisesPreferencesActivity.class.getSimpleName(), ">>> initBaliseProvidersPreferences");
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
    FORMAT_DATE_MAJ_SPOTS = android.text.format.DateFormat.getDateFormat(context);

    // Verification si deja fait
    synchronized (initBaliseProvidersPreferencesLock)
    {
      // Deja fait ?
      if (sharedPreferences.getBoolean(CONFIG_INIT_BALISE_PROVIDERS_PREFERENCES_DONE, false))
      {
        Log.d(AbstractBalisesPreferencesActivity.class.getSimpleName(), "<<< initBaliseProvidersPreferences, already done");
        return;
      }

      // MAJ du flag
      final SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.putBoolean(CONFIG_INIT_BALISE_PROVIDERS_PREFERENCES_DONE, true);
      ActivityCommons.commitPreferences(editor);
    }

    // Initialisations
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
    final String[] hiddens = resources.getStringArray(R.array.providers_hidden);
    final boolean debugMode = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;

    // Pour chaque provider
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    for (int i = 0; i < keys.length; i++)
    {
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      final boolean hidden = ActivityCommons.isBaliseProviderHidden(keys[i], hiddens[i]);
      if ((debugMode || !forDebug) && !hidden)
      {
        final List<String> countries = BaliseProviderUtils.getBaliseProviderCountries(context, i);
        for (final String country : countries)
        {
          BaliseProviderUtils.isBaliseProviderCountryActive(context, sharedPreferences, keys[i], country, i, editor);
        }
      }
    }

    // Fin
    ActivityCommons.commitPreferences(editor);
    Log.d(AbstractBalisesPreferencesActivity.class.getSimpleName(), "<<< initBaliseProvidersPreferences, done");
  }

  /**
   * 
   */
  private void initProviderContinentsPreferences()
  {
    // Initialisations
    final PreferenceCategory providersCategory = (PreferenceCategory)getPreferenceScreen().findPreference(resources.getString(R.string.config_data_category_balises_sources_key));
    continentCountry.clear();

    // Recuperation des resources
    final String[] continentCodes = resources.getStringArray(R.array.continent_codes);
    final String[] continentLabels = resources.getStringArray(R.array.continent_labels);

    // Pour chaque continent
    for (int i = 0; i < continentCodes.length; i++)
    {
      final PreferenceScreen prefScreen = initProviderCountriesPreferences(continentCodes[i], continentLabels[i]);
      if (prefScreen != null)
      {
        // Ajout de l'ecran
        providersCategory.addPreference(prefScreen);

        // MAJ du summary (liste de providers selectionnes pour ce continent)
        updateBaliseProviderContinentPreference(prefScreen);
      }
    }
  }

  /**
   * 
   * @param prefScreen
   * @param continentCode
   * @param continentLabel
   * @return
   */
  private PreferenceScreen initProviderCountriesPreferences(final String continentCode, final String continentLabel)
  {
    // Recuperation des resources
    final int countryCodesId = resources.getIdentifier(RESOURCES_COUNTRY_CODES_PREFIX + continentCode, Strings.RESOURCES_ARRAY, getPackageName());
    final String[] countryCodes = resources.getStringArray(countryCodesId);

    // Comparateur pays (pays "local" en premier, pays coches ensuite, tries alphabetiquement par nom sinon)
    final SortedSet<Preference> countryPrefs = new TreeSet<Preference>(baliseCountryPreferenceComparator);

    // Pour chaque pays
    for (final String countryCode : countryCodes)
    {
      // Renseignement de la correspondance pays <> continent
      continentCountry.put(countryCode, continentCode);

      // Si le pays est disponible (il existe un provider)
      if (BaliseProviderUtils.isCountryAvailable(getApplicationContext(), countryCode))
      {
        // Initialisations
        final int regionCodesId = resources.getIdentifier(RESOURCES_REGION_CODES_PREFIX + countryCode.toUpperCase(), Strings.RESOURCES_ARRAY, getPackageName());
        final Locale locale = new Locale(countryCode, countryCode);
        final List<Preference> countryPreferences = initProviderCountryPreferences(countryCode, locale.getDisplayCountry(), regionCodesId, baliseProviderPreferenceClickListener);

        for (final Preference countryPref : countryPreferences)
        {
          // Case a cocher (cochee si au moins un provider selectionne pour ce pays)
          // et Summary (liste de providers selectionnes pour ce pays)
          updateBaliseProviderCountryPreference(countryPref);

          // Ajout
          countryPrefs.add(countryPref);
        }
      }
    }

    // Creation de l'ecran
    final PreferenceScreen prefScreen;
    if (countryPrefs.size() > 0)
    {
      // Initialisations
      prefScreen = getPreferenceManager().createPreferenceScreen(this);
      prefScreen.setKey(formatBaliseProviderContinentPreferenceKey(continentCode));

      // Titre
      prefScreen.setTitle(continentLabel);

      // Pour chaque pref
      for (final Preference countryPref : countryPrefs)
      {
        prefScreen.addPreference(countryPref);
      }
    }
    else
    {
      prefScreen = null;
    }

    return prefScreen;
  }

  /**
   * 
   * @param countryCode
   * @param countryLabel
   * @param regionCodesId
   * @param clickListener
   * @return
   */
  private List<Preference> initProviderCountryPreferences(final String countryCode, final String countryLabel, final int regionCodesId, final BaliseProviderPreferenceClickListener clickListener)
  {
    // Initialisations
    final List<Preference> countryPrefs = new ArrayList<Preference>();
    final String regionCode = BaliseProviderUtils.getRegionCode(countryCode);

    // Si national ou region
    if (BaliseProviderUtils.hasNationalProviders(getApplicationContext(), countryCode) || !Utils.isStringVide(regionCode))
    {
      // Checkbox
      final String key = formatBaliseProviderCountryPreferenceKey(countryCode, false);
      final CheckBoxPreference chkCountryPref = new CheckBoxPreference(this);
      final boolean active = sharedPreferences.getBoolean(key, BaliseProviderUtils.getDefault(getApplicationContext(), countryCode));
      chkCountryPref.setChecked(active);
      chkCountryPref.setOnPreferenceClickListener(clickListener);
      chkCountryPref.setTitle(countryLabel);
      chkCountryPref.setKey(key);
      countryPrefs.add(chkCountryPref);
    }

    // Si regional
    if ((regionCodesId > 0) && (BaliseProviderUtils.hasRegionalProviders(countryCode)))
    {
      // Ecran avec liste des regions
      final String key = formatBaliseProviderCountryPreferenceKey(countryCode, true);
      final Preference countryPref = initProviderCountryRegionsPreferences(countryCode, regionCodesId);
      countryPref.setTitle(countryLabel + Strings.SPACE + resources.getString(R.string.label_states));
      countryPref.setKey(key);
      countryPrefs.add(countryPref);
    }

    return countryPrefs;
  }

  /**
   * 
   * @param countryCode
   * @param regionCodesId
   * @return
   */
  private PreferenceScreen initProviderCountryRegionsPreferences(final String countryCode, final int regionCodesId)
  {
    // Recuperation des resources
    final String[] regionCodes = resources.getStringArray(regionCodesId);
    final String[] regionLabels = resources.getStringArray(resources.getIdentifier(RESOURCES_REGION_LABELS_PREFIX + countryCode.toUpperCase(), Strings.RESOURCES_ARRAY, getPackageName()));

    // Comparateur pays/region (pays "local" en premier, pays coches ensuite, tries alphabetiquement par nom sinon)
    final SortedSet<Preference> regionPrefs = new TreeSet<Preference>(baliseRegionPreferenceComparator);

    // Pour chaque pays
    int i = 0;
    for (final String regionCode : regionCodes)
    {
      // Initialisations
      final List<Preference> regionPreferences = initProviderCountryPreferences(BaliseProviderUtils.getCountryRegion(countryCode, regionCode), regionLabels[i], 0, baliseProviderPreferenceClickListener);

      for (final Preference regionPref : regionPreferences)
      {
        // Case a cocher (cochee si au moins un provider selectionne pour ce pays)
        // et Summary (liste de providers selectionnes pour cette region)
        updateBaliseProviderCountryPreference(regionPref);

        // Ajout
        regionPrefs.add(regionPref);
      }

      // Next
      i++;
    }

    // Creation de l'ecran
    final PreferenceScreen prefScreen;
    if (regionPrefs.size() > 0)
    {
      // Initialisations
      prefScreen = getPreferenceManager().createPreferenceScreen(this);

      // Pour chaque pref
      for (final Preference regionPref : regionPrefs)
      {
        prefScreen.addPreference(regionPref);
      }
    }
    else
    {
      prefScreen = null;
    }

    return prefScreen;
  }

  /**
   * 
   * @param continentCode
   * @return
   */
  private List<String> getCountryCodesCheckeds(final String continentCode)
  {
    // Recuperation des resources
    final List<String> countryCodesChecked = new ArrayList<String>();
    final int countryCodesId = resources.getIdentifier(RESOURCES_COUNTRY_CODES_PREFIX + continentCode, Strings.RESOURCES_ARRAY, getPackageName());
    final String[] countryCodes = resources.getStringArray(countryCodesId);

    // Pour chaque pays
    for (final String countryCode : countryCodes)
    {
      // Si le pays est disponible (il existe un provider) et selectionne
      if (BaliseProviderUtils.isCountryAvailable(getApplicationContext(), countryCode) && isCountryUsed(countryCode))
      {
        countryCodesChecked.add(countryCode);
      }
    }

    return countryCodesChecked;
  }

  /**
   * 
   * @param countryCode
   * @return
   */
  private List<String> getRegionLabelsCheckeds(final String countryCode)
  {
    // Recuperation des resources
    final List<String> regionLabelsChecked = new ArrayList<String>();
    final int regionCodesId = resources.getIdentifier(RESOURCES_REGION_CODES_PREFIX + countryCode.toUpperCase(), Strings.RESOURCES_ARRAY, getPackageName());
    final String[] regionCodes = resources.getStringArray(regionCodesId);
    final int regionLabelsId = resources.getIdentifier(RESOURCES_REGION_LABELS_PREFIX + countryCode.toUpperCase(), Strings.RESOURCES_ARRAY, getPackageName());
    final String[] regionLabels = resources.getStringArray(regionLabelsId);

    // Pour chaque region
    for (int i = 0; i < regionCodes.length; i++)
    {
      // Si la region est selectionnee
      final String countryRegion = BaliseProviderUtils.getCountryRegion(countryCode, regionCodes[i]);
      if (isCountryUsed(countryRegion))
      {
        regionLabelsChecked.add(regionLabels[i]);
      }
    }

    return regionLabelsChecked;
  }

  /**
   * 
   * @param countryPref
   */
  @SuppressWarnings("unchecked")
  void updateBaliseProviderCountryPreference(final Preference countryPref)
  {
    // Initialisations
    final String key = countryPref.getKey();
    final String countryRegionCode = parseBaliseProviderCountryPreferenceKey(key);
    final String countryCode = BaliseProviderUtils.getCountryCode(countryRegionCode);
    final String regionCode = BaliseProviderUtils.getRegionCode(countryRegionCode);
    final boolean isRegion = !Utils.isStringVide(regionCode);
    final int regionCodesId = (isRegion ? 0 : resources.getIdentifier(RESOURCES_REGION_CODES_PREFIX + countryCode.toUpperCase(), Strings.RESOURCES_ARRAY, getPackageName()));
    final boolean hasRegions = (regionCodesId > 0) && isBaliseProviderCountryPreferenceKeyRegional(key);

    // Construction du summary
    if (hasRegions)
    {
      // Pays avec regions
      // Recuperation des infos
      final List<String> regionCheckeds = getRegionLabelsCheckeds(countryCode);

      // Construction du summary
      final StringBuilder summary = new StringBuilder(64);
      boolean first = true;
      for (final String regionChecked : regionCheckeds)
      {
        if (!first)
        {
          summary.append(STRING_SEPARATEUR_COUNTRY_LABEL);
        }
        summary.append(regionChecked);
        first = false;
      }

      // MAJ de la preference
      if (Utils.isStringVide(summary.toString()))
      {
        countryPref.setSummary(null);
      }
      else
      {
        countryPref.setSummary(summary.toString());
      }

      // Ruse pour forcer la MAJ
      final String continentCode = continentCountry.get(countryCode);
      final Preference continentPref = getPreferenceManager().findPreference(formatBaliseProviderContinentPreferenceKey(continentCode));
      if (continentPref != null)
      {
        final CharSequence tempSummary = continentPref.getSummary();
        continentPref.setSummary(summary.toString());
        continentPref.setSummary(tempSummary);
      }
    }
    else
    {
      // Pays ou region
      final StringBuilder summary = new StringBuilder(64);
      final CheckBoxPreference chkCountryPref = (CheckBoxPreference)countryPref;
      if (chkCountryPref.isChecked())
      {
        // Recuperation des infos
        final boolean[] availableProviders = BaliseProviderUtils.getAvailableBalisesProviders(getApplicationContext(), countryCode, regionCode);
        final List<?>[] infos = BaliseProviderUtils.getBaliseProvidersCheckedForCountry(getApplicationContext(), sharedPreferences, countryRegionCode, availableProviders);
        final List<String> providerLabels = (List<String>)infos[1];
        final List<Boolean> providerCheckeds = (List<Boolean>)infos[2];

        // Pour chaque provider
        int index = 0;
        boolean first = true;
        boolean almostOne = false;
        for (final Boolean checked : providerCheckeds)
        {
          if (checked.booleanValue())
          {
            almostOne = true;
            if (!first)
            {
              summary.append(STRING_SEPARATEUR_PROVIDER_LABEL);
            }
            summary.append(providerLabels.get(index));
            first = false;
          }

          // Next
          index++;
        }

        // Summary
        if (almostOne)
        {
          chkCountryPref.setSummary(summary.toString());
        }
        else
        {
          chkCountryPref.setSummary(null);
          chkCountryPref.setChecked(false);
        }
      }
      else
      {
        // RAZ du summary
        chkCountryPref.setSummary(null);
      }

      // Ruse pour forcer la MAJ
      final String continentCode = continentCountry.get(countryCode);
      final Preference continentPref = getPreferenceManager().findPreference(formatBaliseProviderContinentPreferenceKey(continentCode));
      if (continentPref != null)
      {
        final CharSequence tempSummary = continentPref.getSummary();
        continentPref.setSummary(summary.toString());
        continentPref.setSummary(tempSummary);
      }

      // Pays de la region
      if (isRegion)
      {
        final Preference parentCountryPref = getPreferenceManager().findPreference(formatBaliseProviderCountryPreferenceKey(countryCode, true));
        if (parentCountryPref != null)
        {
          updateBaliseProviderCountryPreference(parentCountryPref);
        }
      }
    }
  }

  /**
   * 
   * @param continentPref
   */
  void updateBaliseProviderContinentPreference(final Preference continentPref)
  {
    // Recuperation des infos
    final String continentCode = parseBaliseProviderContinentPreferenceKey(continentPref.getKey());
    final List<String> countryCheckeds = getCountryCodesCheckeds(continentCode);

    // Construction du summary
    final StringBuilder summary = new StringBuilder(64);
    boolean first = true;
    for (final String countryChecked : countryCheckeds)
    {
      if (!first)
      {
        summary.append(STRING_SEPARATEUR_COUNTRY_LABEL);
      }
      summary.append(new Locale(countryChecked, countryChecked).getDisplayCountry(Locale.getDefault()));
      first = false;
    }

    // MAJ de la preference
    if (Utils.isStringVide(summary.toString()))
    {
      continentPref.setSummary(null);
    }
    else
    {
      continentPref.setSummary(summary.toString());
    }

    // Ruse pour forcer la MAJ
    final Preference tempPref = getPreferenceManager().findPreference(resources.getString(R.string.config_data_category_balises_sources_key));
    final CharSequence tempSummary = tempPref.getSummary();
    tempPref.setSummary(summary.toString());
    tempPref.setSummary(tempSummary);
  }

  /**
   * 
   * @param countryCode
   * @param regional
   * @return
   */
  public static String formatBaliseProviderCountryPreferenceKey(final String countryCode, final boolean regional)
  {
    if (regional)
    {
      return CONFIG_BALISE_PROVIDERS_REGIONAL_COUNTRY_PREFIX + countryCode;
    }

    return CONFIG_BALISE_PROVIDERS_COUNTRY_PREFIX + countryCode;
  }

  /**
   * 
   * @param key
   * @return
   */
  static String parseBaliseProviderCountryPreferenceKey(final String key)
  {
    final int position = key.indexOf(CONFIG_BALISE_PROVIDERS_COUNTRY_PREFIX);
    if (position >= 0)
    {
      return key.substring(CONFIG_BALISE_PROVIDERS_COUNTRY_PREFIX.length());
    }

    final int regionalPosition = key.indexOf(CONFIG_BALISE_PROVIDERS_REGIONAL_COUNTRY_PREFIX);
    if (regionalPosition >= 0)
    {
      return key.substring(CONFIG_BALISE_PROVIDERS_REGIONAL_COUNTRY_PREFIX.length());
    }

    return null;
  }

  /**
   * 
   * @param key
   * @return
   */
  static boolean isBaliseProviderCountryPreferenceKeyRegional(final String key)
  {
    return key.startsWith(CONFIG_BALISE_PROVIDERS_REGIONAL_COUNTRY_PREFIX);
  }

  /**
   * 
   * @param key
   * @return
   */
  private static String parseBaliseProviderContinentPreferenceKey(final String key)
  {
    final int position = key.indexOf(CONFIG_BALISE_PROVIDERS_CONTINENT_PREFIX);
    if (position < 0)
    {
      return null;
    }

    return key.substring(CONFIG_BALISE_PROVIDERS_CONTINENT_PREFIX.length());
  }

  /**
   * 
   * @param fullProviderKey
   * @return
   */
  public static String formatBaliseProviderPreferenceKey(final String fullProviderKey)
  {
    return CONFIG_BALISE_PROVIDERS_PROVIDER_PREFIX + fullProviderKey;
  }

  /**
   * 
   * @param providerKey
   * @param countryCode
   * @return
   */
  public static String formatBaliseProviderPreferenceKey(final String providerKey, final String countryCode)
  {
    return formatBaliseProviderPreferenceKey(providerKey + '_' + countryCode);
  }

  /**
   * 
   * @param continentCode
   * @return
   */
  static String formatBaliseProviderContinentPreferenceKey(final String continentCode)
  {
    return CONFIG_BALISE_PROVIDERS_CONTINENT_PREFIX + continentCode;
  }

  /**
   * 
   * @param type
   * @return
   */
  public static String formatTypeSpotPreferenceKey(final TypeSpot type)
  {
    return CONFIG_KEY_TYPE_SPOT + Strings.CHAR_POINT + type.getKey();
  }

  /**
   * 
   * @param pratique
   * @return
   */
  public static String formatPratiquePreferenceKey(final Pratique pratique)
  {
    return CONFIG_KEY_PRATIQUE + Strings.CHAR_POINT + pratique.getKey();
  }

  /**
   * 
   * @param pratique
   * @return
   */
  public static String formatWebcamPratiquePreferenceKey(final int pratique)
  {
    return CONFIG_KEY_WEBCAMS_PRATIQUE + Strings.CHAR_POINT + pratique;
  }

  /**
   * 
   * @param orientation
   * @return
   */
  public static String formatOrientationPreferenceKey(final Orientation orientation)
  {
    return CONFIG_KEY_ORIENTATION + orientation.getKey().toUpperCase();
  }

  /**
   * 
   */
  private void initSiteProvidersPreferences()
  {
    // Sources
    initSiteSourcesPreferences();

    // Types
    initTypesSpotPreferences();

    // Pratiques
    initPratiquesPreferences();
  }

  /**
   * 
   */
  private void initWebcamProvidersPreferences()
  {
    // Pratiques
    initWebcamPratiquesPreferences();
  }

  /**
   * 
   */
  private void initSiteSourcesPreferences()
  {
    // Initialisations
    final PreferenceGroup siteSources = (PreferenceGroup)getPreferenceScreen().findPreference(resources.getString(R.string.config_data_category_sites_sources));
    final SpotProviderPreferenceClickListener clickListener = new SpotProviderPreferenceClickListener(this);

    // Recuperation des resources
    final String[] keys = resources.getStringArray(R.array.site_providers_keys);
    final String[] titles = resources.getStringArray(R.array.site_providers_titles);
    final String[] summaries = resources.getStringArray(R.array.site_providers_summaries);
    final String[] forDebugs = resources.getStringArray(R.array.site_providers_forDebugs);
    final boolean debugMode = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;

    // Pour chaque provider
    for (int i = 0; i < keys.length; i++)
    {
      // On ne prend en compte le provider que si mode debug ou non provider de debug
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      if (debugMode || !forDebug)
      {
        final CheckBoxPreference checkboxPreference = new CheckBoxPreference(this);
        checkboxPreference.setKey(keys[i]);
        checkboxPreference.setTitle(titles[i]);
        if (summaries[i].trim().length() > 0)
        {
          checkboxPreference.setSummary(summaries[i]);
        }
        checkboxPreference.setOnPreferenceClickListener(clickListener);

        // Valeur
        final boolean checked = sharedPreferences.getBoolean(keys[i], false);
        checkboxPreference.setChecked(checked);

        // Ajout
        siteSources.addPreference(checkboxPreference);
      }
    }
  }

  /**
   * 
   */
  private void initTypesSpotPreferences()
  {
    // Initialisations
    final PreferenceCategory typesSpot = (PreferenceCategory)getPreferenceScreen().findPreference(resources.getString(R.string.config_data_category_sites_types));

    // Recuperation des resources
    final String[] titles = resources.getStringArray(R.array.types_spot_titles);
    final String[] defaults = resources.getStringArray(R.array.types_spot_defaults);

    // Pour chaque type de spot
    int i = 0;
    for (final TypeSpot typeSpot : TypeSpot.values())
    {
      // Clef
      final String prefKey = formatTypeSpotPreferenceKey(typeSpot);

      // Initialisations
      final CheckBoxPreference checkboxPreference = new CheckBoxPreference(this);
      checkboxPreference.setKey(prefKey);
      checkboxPreference.setTitle(titles[i]);

      // Valeur
      final boolean checked = sharedPreferences.getBoolean(prefKey, Boolean.parseBoolean(defaults[i]));
      checkboxPreference.setChecked(checked);

      // Ajout
      typesSpot.addPreference(checkboxPreference);

      // Next
      i++;
    }
  }

  /**
   * 
   */
  private void initPratiquesPreferences()
  {
    // Initialisations
    final PreferenceCategory pratiques = (PreferenceCategory)getPreferenceScreen().findPreference(resources.getString(R.string.config_data_category_sites_pratiques));

    // Recuperation des resources
    final String[] titles = resources.getStringArray(R.array.pratiques_titles);
    final String[] defaults = resources.getStringArray(R.array.pratiques_defaults);

    // Pour chaque pratique
    int i = 0;
    for (final Pratique pratique : Pratique.values())
    {
      // Clef
      final String prefKey = formatPratiquePreferenceKey(pratique);

      // Initialisations
      final CheckBoxPreference checkboxPreference = new CheckBoxPreference(this);
      checkboxPreference.setKey(prefKey);
      checkboxPreference.setTitle(titles[i]);

      // Valeur
      final boolean checked = sharedPreferences.getBoolean(prefKey, Boolean.parseBoolean(defaults[i]));
      checkboxPreference.setChecked(checked);

      // Ajout
      pratiques.addPreference(checkboxPreference);

      // Next
      i++;
    }
  }

  /**
   * 
   */
  private void initWebcamPratiquesPreferences()
  {
    // Initialisations
    final PreferenceCategory pratiques = (PreferenceCategory)getPreferenceScreen().findPreference(resources.getString(R.string.config_data_category_webcams_pratiques));

    // Recuperation des resources
    final String[] titles = resources.getStringArray(R.array.webcam_pratiques_titles);
    final String[] defaults = resources.getStringArray(R.array.webcam_pratiques_defaults);

    // Pour chaque pratique
    final int nbPratiques = titles.length;
    for (int i = 0; i < nbPratiques; i++)
    {
      // Clef
      final String prefKey = formatWebcamPratiquePreferenceKey(i);

      // Initialisations
      final CheckBoxPreference checkboxPreference = new CheckBoxPreference(this);
      checkboxPreference.setKey(prefKey);
      checkboxPreference.setTitle(titles[i]);

      // Valeur
      final boolean checked = sharedPreferences.getBoolean(prefKey, Boolean.parseBoolean(defaults[i]));
      checkboxPreference.setChecked(checked);

      // Ajout
      pratiques.addPreference(checkboxPreference);
    }
  }

  @Override
  public void onResume()
  {
    // Debut
    Log.d(getClass().getSimpleName(), ">>> onResume()");
    super.onResume();

    if (mode == INTENT_MODE_NORMAL)
    {
      // MAJ de la taille de cache utilisee si fournie, desactivation de la Preference sinon
      final long cacheCurrentSize = getIntent().getLongExtra(getPackageName() + INTENT_CACHE_CURRENT_SIZE, -1);
      final SliderPreference cacheSizePreference = (SliderPreference)getPreferenceScreen().findPreference(resources.getString(R.string.config_advanced_map_cache_size_key));
      cacheSizePreference.setEnabled(cacheCurrentSize >= 0);
      if (cacheCurrentSize >= 0)
      {
        final float cacheCurrentSizeMo = ((float)cacheCurrentSize) / 1024 / 1024;
        final String cacheSummary = MessageFormat.format(resources.getString(R.string.label_map_cache_size_summary), resources.getString(R.string.config_advanced_map_cache_size_default), Float.valueOf(cacheCurrentSizeMo));
        cacheSizePreference.setSummary(cacheSummary);
      }

      // MAJ du niveau de zoom utilise
      final int zoomLevel = getIntent().getIntExtra(getPackageName() + INTENT_ZOOM_LEVEL, -1);
      final String zoomSummary = MessageFormat.format(resources.getString(R.string.label_map_orientations_zoom_level_summary), resources.getString(R.string.config_data_sites_orientations_zoom_level_default), Integer.valueOf(zoomLevel));
      final SliderPreference zoomPreference = (SliderPreference)getPreferenceScreen().findPreference(resources.getString(R.string.config_data_sites_orientations_zoom_level_key));
      zoomPreference.setSummary(zoomSummary);
    }

    // Sauvegarde de l'etat des providers
    saveProvidersFlags();

    // RAZ
    forceSpotProvidersUpdate = false;

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onResume()");
  }

  @Override
  public void onPause()
  {
    // Debut
    Log.d(getClass().getSimpleName(), ">>> abstract.onPause()");
    super.onPause();

    // Gestion de l'etat des providers
    manageProvidersFlags();
    if (providersService != null)
    {
      providersService.setForceDownloadSpotProviders(forceSpotProvidersUpdate);
    }

    // Debut
    Log.d(getClass().getSimpleName(), "<<< abstract.onPause()");
  }

  @Override
  public void onStart()
  {
    Log.d(getClass().getSimpleName(), ">>> abstract.onStart()");
    super.onStart();
    Log.d(getClass().getSimpleName(), "<<< abstract.onStart()");
  }

  /**
   * 
   */
  private void saveProvidersFlags()
  {
    saveBaliseProvidersFlags();
    saveSpotProvidersFlags();
  }

  /**
   * 
   */
  private void saveBaliseProvidersFlags()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> saveBaliseProvidersFlags()");
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    baliseProvidersFlags = new boolean[keys.length];

    // Pour chaque provider
    for (int i = 0; i < keys.length; i++)
    {
      final List<String> countries = BaliseProviderUtils.getBaliseProviderActiveCountries(getApplicationContext(), sharedPreferences, keys[i], i);
      baliseProvidersFlags[i] = ((countries != null) && (countries.size() > 0));
    }
  }

  /**
   * 
   */
  private void saveSpotProvidersFlags()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> saveSpotProvidersFlags()");
    final String[] keys = resources.getStringArray(R.array.site_providers_keys);
    spotProvidersFlags = new boolean[keys.length];

    // Pour chaque provider
    for (int i = 0; i < keys.length; i++)
    {
      spotProvidersFlags[i] = sharedPreferences.getBoolean(keys[i], false);
    }
  }

  @Override
  public void onStop()
  {
    Log.d(getClass().getSimpleName(), ">>> abstract.onStop()");
    super.onStop();
    Log.d(getClass().getSimpleName(), "<<< abstract.onStop()");
  }

  /**
   * 
   */
  private void manageProvidersFlags()
  {
    if (mode == INTENT_MODE_NORMAL)
    {
      manageBaliseProvidersFlags();
      manageSpotProvidersFlags();
    }
  }

  /**
   * 
   */
  private void manageBaliseProvidersFlags()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> manageBaliseProvidersFlags()");
    final boolean baliseProvidersEnabled = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES, AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES_DEFAULT)
        || sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WEATHER_BALISES, Boolean.parseBoolean(resources.getString(R.string.config_map_layers_weather_default)));

    if (!baliseProvidersEnabled)
    {
      // Initialisations
      final String[] keys = resources.getStringArray(R.array.providers_keys);

      // Pour chaque provider
      int countWasActive = 0;
      int countActive = 0;
      for (int i = 0; i < keys.length; i++)
      {
        countWasActive += (baliseProvidersFlags[i] ? 1 : 0);
        final List<String> countries = BaliseProviderUtils.getBaliseProviderActiveCountries(getApplicationContext(), sharedPreferences, keys[i], i);
        final boolean active = ((countries != null) && (countries.size() > 0));
        countActive += (active ? 1 : 0);
      }

      // Activation de l'option si aucun n'etait actif et qu'il y en a un maintenant
      if ((countWasActive == 0) && (countActive > 0))
      {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES, true);
        editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WEATHER_BALISES, Boolean.parseBoolean(resources.getString(R.string.config_map_layers_weather_default)));
        ActivityCommons.commitPreferences(editor);
      }
    }
  }

  /**
   * 
   */
  private void manageSpotProvidersFlags()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> manageSpotProvidersFlags()");
    final boolean spotProvidersEnabled = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES, AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES_DEFAULT);

    if (!spotProvidersEnabled)
    {
      // Initialisations
      final String[] keys = resources.getStringArray(R.array.site_providers_keys);

      // Pour chaque provider
      int countWasActive = 0;
      int countActive = 0;
      for (int i = 0; i < keys.length; i++)
      {
        countWasActive += (spotProvidersFlags[i] ? 1 : 0);
        final boolean active = sharedPreferences.getBoolean(keys[i], false);
        countActive += (active ? 1 : 0);
      }

      // Activation de l'option si aucun n'etait actif et qu'il y en a un maintenant
      if ((countWasActive == 0) && (countActive > 0))
      {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES, true);
        ActivityCommons.commitPreferences(editor);
      }
    }
  }

  @Override
  public void onDestroy()
  {
    // Debut
    Log.d(getClass().getSimpleName(), ">>> abstract.onDestroy()");
    super.onDestroy();

    // Fin des threads divers
    ThreadUtils.join(spotDownloadThreads, true);

    // Signalement du changement de configuration
    providersServiceConnection.privateOnServiceDisconnected();

    // Deconnexion du service
    unbindService(providersServiceConnection);

    // Liberation vues
    ActivityCommons.unbindDrawables(findViewById(android.R.id.content));

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onDestroy()");
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ProvidersServiceConnection implements ServiceConnection
  {
    private final WeakReference<AbstractBalisesPreferencesActivity> prefsActivity;

    /**
     * 
     * @param prefsActivity
     */
    ProvidersServiceConnection(final AbstractBalisesPreferencesActivity prefsActivity)
    {
      this.prefsActivity = new WeakReference<AbstractBalisesPreferencesActivity>(prefsActivity);
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder inBinder)
    {
      // Recuperation du service
      prefsActivity.get().providersService = ((ProvidersServiceBinder)inBinder).getService();
    }

    @Override
    public void onServiceDisconnected(final ComponentName name)
    {
      privateOnServiceDisconnected();
    }

    /**
     * 
     */
    void privateOnServiceDisconnected()
    {
      // Il peut arriver que cette methode soit appelee (via onDestroy()) avant onServiceConnected !
      if (prefsActivity.get().providersService != null)
      {
        prefsActivity.get().providersService.updateBaliseProviders(true);
        if (prefsActivity.get().providersService.getLocationService() != null)
        {
          prefsActivity.get().providersService.getLocationService().fireLastLocation();
        }
      }
    }
  }

  /**
   * 
   */
  private void initProvidersService()
  {
    // Initialisation
    providersServiceConnection = new ProvidersServiceConnection(this);

    // Connexion au service
    final Intent providersServiceIntent = new Intent(getApplicationContext(), getProvidersServiceClass());
    bindService(providersServiceIntent, providersServiceConnection, 0);
  }

  /**
   * 
   * @return
   */
  protected abstract Class<? extends AbstractProvidersService> getProvidersServiceClass();

  @Override
  public boolean onPreferenceChange(final Preference preference, final Object newValue)
  {
    // Action sur touch
    if (resources.getString(R.string.config_map_touch_action_key).equals(preference.getKey()))
    {
      try
      {
        final String[] labels = resources.getStringArray(R.array.config_map_touch_action_entries);
        final int index = Integer.parseInt((String)newValue, 10);
        preference.setSummary(labels[index - 1]);
        return true;
      }
      catch (final NumberFormatException nfe)
      {
        return false;
      }
    }

    // Collision METAR/SYNOP
    if (resources.getString(R.string.config_map_metar_synop_collision_key).equals(preference.getKey()))
    {
      try
      {
        final String[] labels = resources.getStringArray(R.array.config_map_metar_synop_collision_entries);
        final int index = Integer.parseInt((String)newValue, 10);
        preference.setSummary(labels[index - 1]);
        return true;
      }
      catch (final NumberFormatException nfe)
      {
        return false;
      }
    }

    // Valeur de vent affichee
    else if (resources.getString(R.string.config_map_wind_key).equals(preference.getKey()))
    {
      try
      {
        final String[] labels = resources.getStringArray(R.array.config_map_wind_entries);
        final int index = Integer.parseInt((String)newValue, 10);
        preference.setSummary(labels[index - 1]);
        return true;
      }
      catch (final NumberFormatException nfe)
      {
        return false;
      }
    }

    // Limite moy check || Limite moy edit || Limite max check || Limite max edit
    else if (resources.getString(R.string.config_map_limit_moy_check_key).equals(preference.getKey()) || resources.getString(R.string.config_map_limit_max_check_key).equals(preference.getKey()))
    {
      // Initialisations
      Boolean moyCheck;
      if (resources.getString(R.string.config_map_limit_moy_check_key).equals(preference.getKey()))
      {
        moyCheck = (Boolean)newValue;
        updateSummaryLimitMoy(moyCheck, null, null);
      }
      else
      {
        moyCheck = Boolean.valueOf(sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_moy_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_moy_check_default))));
      }
      Boolean maxCheck;
      if (resources.getString(R.string.config_map_limit_max_check_key).equals(preference.getKey()))
      {
        maxCheck = (Boolean)newValue;
        updateSummaryLimitMax(maxCheck, null, null);
      }
      else
      {
        maxCheck = Boolean.valueOf(sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_max_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_max_check_default))));
      }

      // Activation / desactivation du choix de l'operateur
      final Preference operatorPreference = getPreferenceScreen().findPreference(resources.getString(R.string.config_map_limit_operator_key));
      operatorPreference.setEnabled(moyCheck.booleanValue() && maxCheck.booleanValue());

      return true;
    }

    // Operateur Limite
    else if (resources.getString(R.string.config_map_limit_operator_key).equals(preference.getKey()))
    {
      try
      {
        final String[] labels = resources.getStringArray(R.array.config_map_limit_operator_entries);
        final int index = Integer.parseInt((String)newValue, 10);
        preference.setSummary(labels[index - 1]);
        return true;
      }
      catch (final NumberFormatException nfe)
      {
        return false;
      }
    }

    // Limite de vent moyen
    else if (resources.getString(R.string.config_map_limit_moy_edit_key).equals(preference.getKey()))
    {
      updateSummaryLimitMoy(null, (Integer)newValue, null);
      return true;
    }

    // Limite de vent max
    else if (resources.getString(R.string.config_map_limit_max_edit_key).equals(preference.getKey()))
    {
      updateSummaryLimitMax(null, (Integer)newValue, null);
      return true;
    }

    // Limite de peremption releve
    else if (resources.getString(R.string.config_map_outofdate_key).equals(preference.getKey()))
    {
      return true;
    }

    // Centrage
    else if (resources.getString(R.string.config_map_centering_key).equals(preference.getKey()))
    {
      try
      {
        final String summary = resources.getString(((Boolean)newValue).booleanValue() ? R.string.label_map_centering_true : R.string.label_map_centering_false);
        preference.setSummary(summary);

        return true;
      }
      catch (final NumberFormatException nfe)
      {
        return false;
      }
    }

    // Clef IGN perso
    else if (resources.getString(R.string.config_map_ign_own_key_key).equals(preference.getKey()))
    {
      preference.setSummary((String)newValue);
      return true;
    }

    // Unite vitesse
    else if (resources.getString(R.string.config_unit_speed_key).equals(preference.getKey()))
    {
      // Summary
      final String value = (String)newValue;
      final int resId = resources.getIdentifier(UNIT_SPEED_PREFIX + value, Strings.RESOURCES_STRING, getPackageName());
      final String speedUnit = resources.getString(resId);
      preference.setSummary(speedUnit);

      // Sliders
      final SliderPreference moySliderPref = (SliderPreference)getPreferenceScreen().findPreference(resources.getString(R.string.config_map_limit_moy_edit_key));
      final SliderPreference maxSliderPref = (SliderPreference)getPreferenceScreen().findPreference(resources.getString(R.string.config_map_limit_max_edit_key));

      // Valeurs en unite de base
      final int moyMinValue = (int)Math.round(ActivityCommons.getInitialSpeed(moySliderPref.getMinValue()));
      final int moyMaxValue = (int)Math.round(ActivityCommons.getInitialSpeed(moySliderPref.getMaxValue()));
      final int moyValue = (int)Math.round(ActivityCommons.getInitialSpeed(moySliderPref.getValue()));
      final int maxMinValue = (int)Math.round(ActivityCommons.getInitialSpeed(maxSliderPref.getMinValue()));
      final int maxMaxValue = (int)Math.round(ActivityCommons.getInitialSpeed(maxSliderPref.getMaxValue()));
      final int maxValue = (int)Math.round(ActivityCommons.getInitialSpeed(maxSliderPref.getValue()));

      // MAJ communes
      ActivityCommons.updateUnitPreferences(getApplicationContext(), value, null, null, null);

      // Valeurs en nouvelle unite
      moySliderPref.setMinValue((int)Math.round(ActivityCommons.getFinalSpeed(moyMinValue)));
      moySliderPref.setMaxValue((int)Math.round(ActivityCommons.getFinalSpeed(moyMaxValue)));
      moySliderPref.setValue((int)Math.round(ActivityCommons.getFinalSpeed(moyValue)));
      maxSliderPref.setMinValue((int)Math.round(ActivityCommons.getFinalSpeed(maxMinValue)));
      maxSliderPref.setMaxValue((int)Math.round(ActivityCommons.getFinalSpeed(maxMaxValue)));
      maxSliderPref.setValue((int)Math.round(ActivityCommons.getFinalSpeed(maxValue)));

      // Summaries
      updateSummaryLimitMoy(null, null, speedUnit);
      updateSummaryLimitMax(null, null, speedUnit);

      // Labels
      moySliderPref.setTitle(resources.getString(R.string.label_map_windslimits_moy_value).replaceAll(STRING_UNIT_SLIDER, speedUnit));
      maxSliderPref.setTitle(resources.getString(R.string.label_map_windslimits_max_value).replaceAll(STRING_UNIT_SLIDER, speedUnit));

      return true;
    }

    // Unite altitude
    else if (resources.getString(R.string.config_unit_altitude_key).equals(preference.getKey()))
    {
      final String value = (String)newValue;
      final int resId = resources.getIdentifier(UNIT_ALTITUDE_PREFIX + value, Strings.RESOURCES_STRING, getPackageName());
      preference.setSummary(resources.getString(resId));

      // MAJ communes
      ActivityCommons.updateUnitPreferences(getApplicationContext(), null, value, null, null);

      return true;
    }

    // Unite distance
    else if (resources.getString(R.string.config_unit_distance_key).equals(preference.getKey()))
    {
      final String value = (String)newValue;
      final int resId = resources.getIdentifier(UNIT_DISTANCE_PREFIX + value, Strings.RESOURCES_STRING, getPackageName());
      preference.setSummary(resources.getString(resId));

      // MAJ communes
      ActivityCommons.updateUnitPreferences(getApplicationContext(), null, null, value, null);

      return true;
    }

    // Unite temperature
    else if (resources.getString(R.string.config_unit_temperature_key).equals(preference.getKey()))
    {
      final String value = (String)newValue;
      final int resId = resources.getIdentifier(UNIT_TEMPERATURE_PREFIX + value, Strings.RESOURCES_STRING, getPackageName());
      preference.setSummary(resources.getString(resId));

      // MAJ communes
      ActivityCommons.updateUnitPreferences(getApplicationContext(), null, null, null, value);

      return true;
    }

    // Google Analytics
    else if (resources.getString(R.string.config_ga_key).equals(preference.getKey()))
    {
      final String summary = resources.getString(((Boolean)newValue).booleanValue() ? R.string.label_ga_true : R.string.label_ga_false);
      preference.setSummary(summary);

      return true;
    }

    // Limite mini en mode "autour de moi"
    else if (resources.getString(R.string.config_favorites_around_min_key).equals(preference.getKey()))
    {
      final SliderPreference aroundMaxPreference = (SliderPreference)getPreferenceScreen().findPreference(resources.getString(R.string.config_favorites_around_max_key));
      final int minValue = ((Integer)newValue).intValue();
      final int maxValue = aroundMaxPreference.getValue();
      if (minValue >= maxValue)
      {
        aroundMaxPreference.setValue(minValue + 1);
      }
      return true;
    }

    // Limite maxi en mode "autour de moi"
    else if (resources.getString(R.string.config_favorites_around_max_key).equals(preference.getKey()))
    {
      final SliderPreference aroundMinPreference = (SliderPreference)getPreferenceScreen().findPreference(resources.getString(R.string.config_favorites_around_min_key));
      final int maxValue = ((Integer)newValue).intValue();
      final int minValue = aroundMinPreference.getValue();
      if (maxValue <= minValue)
      {
        aroundMinPreference.setValue(maxValue - 1);
      }
      return true;
    }

    // Debut de la plage horaire du mode historique
    else if (resources.getString(R.string.config_history_mode_range_start_key).equals(preference.getKey()))
    {
      final SliderPreference endPreference = (SliderPreference)getPreferenceScreen().findPreference(resources.getString(R.string.config_history_mode_range_end_key));
      final int startValue = ((Integer)newValue).intValue();
      final int endValue = endPreference.getValue();
      if (startValue >= endValue)
      {
        endPreference.setValue(startValue + 1);
      }
      return true;
    }

    // Fin de la plage horaire du mode historique
    else if (resources.getString(R.string.config_history_mode_range_end_key).equals(preference.getKey()))
    {
      final SliderPreference startPreference = (SliderPreference)getPreferenceScreen().findPreference(resources.getString(R.string.config_history_mode_range_start_key));
      final int endValue = ((Integer)newValue).intValue();
      final int startValue = startPreference.getValue();
      if (endValue <= startValue)
      {
        startPreference.setValue(endValue - 1);
      }
      return true;
    }

    return false;
  }

  /**
   * 
   * @author pedro.m
   */
  private static class SpotProviderPreferenceClickListener implements Preference.OnPreferenceClickListener
  {
    final WeakReference<AbstractBalisesPreferencesActivity> prefsActivity;

    /**
     * 
     * @param prefsActivity
     */
    SpotProviderPreferenceClickListener(final AbstractBalisesPreferencesActivity prefsActivity)
    {
      super();
      this.prefsActivity = new WeakReference<AbstractBalisesPreferencesActivity>(prefsActivity);
    }

    @Override
    public boolean onPreferenceClick(final Preference preference)
    {
      // Initialisation
      final String[] keys = prefsActivity.get().resources.getStringArray(R.array.site_providers_keys);
      final String[] titles = prefsActivity.get().resources.getStringArray(R.array.site_providers_titles);

      // Parcours de clefs de chaque provider de site
      for (int i = 0; i < keys.length; i++)
      {
        // Oui, on a clique sur un provider de sites
        if (keys[i].equals(preference.getKey()))
        {
          // Initialisation
          final CheckBoxPreference chkPreference = (CheckBoxPreference)preference;

          // Cochage
          if (chkPreference.isChecked())
          {
            try
            {
              // Initialisations
              final int fi = i;
              final List<String> countriesToDownload = new ArrayList<String>();

              // Provider
              final SpotProvider provider = prefsActivity.get().providersService.getSpotProvider(keys[i]);

              // La liste des pays dispos
              final String[] countries = new String[provider.getAvailableCountries().size()];
              final boolean[] countriesChecked = new boolean[provider.getAvailableCountries().size()];

              // Pour chaque pays
              int j = 0;
              for (final String country : provider.getAvailableCountries())
              {
                // La clef du provider de site + pays
                final String countryKey = SpotProviderUtils.getFullSpotProviderKey(keys[fi], country);

                // Le nom du pays
                countries[j] = SpotProviderUtils.getCountryName(country);

                // La date de MAJ
                final Date dateMaj = prefsActivity.get().providersService.getLastSpotsUpdate(keys[fi], country);
                if (dateMaj != null)
                {
                  countries[j] += " (" + FORMAT_DATE_MAJ_SPOTS.format(dateMaj) + ")";
                }

                // Pays deja choisi ?
                countriesChecked[j] = prefsActivity.get().sharedPreferences.getBoolean(countryKey, false);

                // Next
                j++;
              }

              // Liste de choix
              final AlertDialog.Builder builder = new AlertDialog.Builder(prefsActivity.get());
              builder.setTitle(titles[i]);
              builder.setCancelable(false);

              // Bouton annuler : on decoche le provider
              builder.setNegativeButton(prefsActivity.get().resources.getString(R.string.button_cancel), new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(final DialogInterface dialog, final int wich)
                {
                  chkPreference.setChecked(false);
                }
              });

              // Bouton forcer MAJ
              builder.setNeutralButton(prefsActivity.get().resources.getString(R.string.button_update), new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(final DialogInterface dialog, final int which)
                {
                  try
                  {
                    // Forcage de la MAJ
                    prefsActivity.get().forceSpotProvidersUpdate = true;

                    for (final String country : provider.getAvailableCountries())
                    {
                      // La clef du provider de site + pays
                      final String clickCountryKey = SpotProviderUtils.getFullSpotProviderKey(keys[fi], country);

                      // Coche ?
                      final boolean isCountryChecked = prefsActivity.get().sharedPreferences.getBoolean(clickCountryKey, false);

                      // Ajout dans la liste de telechargements
                      if (isCountryChecked)
                      {
                        countriesToDownload.add(country);
                      }
                    }
                  }
                  catch (final IOException ioe)
                  {
                    throw new RuntimeException(ioe);
                  }
                }
              });

              // Bouton OK : on determine la liste de spots a telecharger
              builder.setPositiveButton(prefsActivity.get().resources.getString(R.string.button_ok), new DialogInterface.OnClickListener()
              {
                @Override
                public void onClick(final DialogInterface dialog, final int which)
                {
                  try
                  {
                    for (final String country : provider.getAvailableCountries())
                    {
                      // La clef du provider de site + pays
                      final String clickCountryKey = SpotProviderUtils.getFullSpotProviderKey(keys[fi], country);

                      // Coche ?
                      final boolean isCountryChecked = prefsActivity.get().sharedPreferences.getBoolean(clickCountryKey, false);

                      // Ajout dans la liste de telechargements
                      final Date clickDateMaj = prefsActivity.get().providersService.getLastSpotsUpdate(keys[fi], country);
                      if (isCountryChecked && (clickDateMaj == null))
                      {
                        countriesToDownload.add(country);
                      }
                    }
                  }
                  catch (final IOException ioe)
                  {
                    throw new RuntimeException(ioe);
                  }
                }
              });
              builder.setMultiChoiceItems(countries, countriesChecked, new DialogInterface.OnMultiChoiceClickListener()
              {
                @Override
                public void onClick(final DialogInterface dialog, final int which, final boolean isChecked)
                {
                  try
                  {
                    // Initialisations
                    final String country = provider.getAvailableCountries().get(which);
                    final String clickCountryKey = SpotProviderUtils.getFullSpotProviderKey(keys[fi], country);

                    // Sauvegarde dans les preferences
                    final SharedPreferences.Editor editor = prefsActivity.get().sharedPreferences.edit();
                    editor.putBoolean(clickCountryKey, isChecked);
                    ActivityCommons.commitPreferences(editor);
                  }
                  catch (final IOException ioe)
                  {
                    throw new RuntimeException(ioe);
                  }
                }
              });

              // Affichage de la liste de choix des pays pour le provider
              final AlertDialog alertDialog = builder.create();
              final OnDismissListener dismissListener = new DialogInterface.OnDismissListener()
              {
                @Override
                public void onDismiss(final DialogInterface dialog)
                {
                  if (!chkPreference.isChecked())
                  {
                    return;
                  }

                  final int nbCountries = countriesToDownload.size();
                  if (nbCountries > 0)
                  {
                    // Affichage de la boite de dialogue de telechargement
                    prefsActivity.get().spotsDownloadProgress();
                    final SpotsDownloadProgressHandler progressHandler = new SpotsDownloadProgressHandler(prefsActivity.get());
                    final CheckBoxPreferenceHandler checkboxHandler = new CheckBoxPreferenceHandler(chkPreference);

                    // Thread de telechargement
                    final SpotsDownloadThread spotsDownloadThread = new SpotsDownloadThread(prefsActivity.get(), provider, keys[fi], countriesToDownload, progressHandler, checkboxHandler);
                    spotsDownloadThread.start();
                    prefsActivity.get().spotDownloadThreads.add(spotsDownloadThread);
                  }
                  else
                  {
                    // Cochage ou non du provider selon les resultats
                    try
                    {
                      boolean providerChecked = false;
                      for (final String country : provider.getAvailableCountries())
                      {
                        // La clef du provider de site + pays
                        final String forCountryKey = SpotProviderUtils.getFullSpotProviderKey(keys[fi], country);

                        // Le provider coche ?
                        providerChecked = providerChecked || prefsActivity.get().sharedPreferences.getBoolean(forCountryKey, false);
                      }

                      // Provider coche ou non
                      chkPreference.setChecked(providerChecked);
                    }
                    catch (final IOException ioe)
                    {
                      throw new RuntimeException(ioe);
                    }
                  }
                }
              };
              alertDialog.show();
              ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_PREFS_SPOTS_COUNTRY_CHOICE, alertDialog, dismissListener);

              return true;
            }
            catch (final IOException ioe)
            {
              throw new RuntimeException(ioe);
            }
          }
        }
      }

      return false;
    }
  }

  /**
   * 
   * @param countryRegionCode
   * @param hasRegions
   * @return
   */
  boolean isCountryUsed(final String countryRegionCode)
  {
    // Initialisations
    final String countryCode = BaliseProviderUtils.getCountryCode(countryRegionCode);
    final String regionCode = BaliseProviderUtils.getRegionCode(countryRegionCode);
    final int regionCodesId = resources.getIdentifier(RESOURCES_REGION_CODES_PREFIX + countryCode.toUpperCase(), Strings.RESOURCES_ARRAY, getPackageName());

    // Region ou pays sans region
    if (!Utils.isStringVide(regionCode) || (regionCodesId <= 0))
    {
      final String prefKey = formatBaliseProviderCountryPreferenceKey(countryRegionCode, false);
      final boolean checked = sharedPreferences.getBoolean(prefKey, BaliseProviderUtils.getDefault(getApplicationContext(), countryRegionCode));
      return checked;
    }

    // Pays avec regions : niveau pays
    final String prefKey = formatBaliseProviderCountryPreferenceKey(countryRegionCode, false);
    final boolean checked = sharedPreferences.getBoolean(prefKey, BaliseProviderUtils.getDefault(getApplicationContext(), countryRegionCode));
    if (checked)
    {
      return true;
    }

    // Pays avec regions : region par region
    final String[] regions = resources.getStringArray(regionCodesId);
    for (final String region : regions)
    {
      if (isCountryUsed(BaliseProviderUtils.getCountryRegion(countryCode, region)))
      {
        return true;
      }
    }

    return false;
  }

  /**
   * 
   * @author pedro.m
   */
  private static class BaliseCountryPreferenceComparator implements Comparator<Preference>
  {
    private final WeakReference<AbstractBalisesPreferencesActivity> prefsActivity;
    private final String                                            localCountryCode = Locale.getDefault().getCountry();

    /**
     * 
     * @param prefsActivity
     */
    BaliseCountryPreferenceComparator(final AbstractBalisesPreferencesActivity prefsActivity)
    {
      super();
      this.prefsActivity = new WeakReference<AbstractBalisesPreferencesActivity>(prefsActivity);
    }

    @Override
    public int compare(final Preference pref1, final Preference pref2)
    {
      // Pays d'origine
      final String countryCode1 = parseBaliseProviderCountryPreferenceKey(pref1.getKey());
      if (countryCode1.equalsIgnoreCase(localCountryCode))
      {
        return Integer.MIN_VALUE;
      }
      final String countryCode2 = parseBaliseProviderCountryPreferenceKey(pref2.getKey());
      if (countryCode2.equalsIgnoreCase(localCountryCode))
      {
        return Integer.MAX_VALUE;
      }

      // Utilisation ?
      final boolean used1 = prefsActivity.get().isCountryUsed(countryCode1);
      final boolean used2 = prefsActivity.get().isCountryUsed(countryCode2);

      // Pays utilises
      if (used1 && !used2)
      {
        return Integer.MIN_VALUE;
      }
      else if (!used1 && used2)
      {
        return Integer.MAX_VALUE;
      }

      // Par nom
      return pref1.getTitle().toString().compareTo(pref2.getTitle().toString());
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class BaliseRegionPreferenceComparator implements Comparator<Preference>
  {
    /**
     * 
     */
    BaliseRegionPreferenceComparator()
    {
      // Nothing
    }

    @Override
    public int compare(final Preference pref1, final Preference pref2)
    {
      // Utilisation ?
      final boolean used1 = ((CheckBoxPreference)pref1).isChecked();
      final boolean used2 = ((CheckBoxPreference)pref2).isChecked();

      // Pays utilises
      if (used1 && !used2)
      {
        return Integer.MIN_VALUE;
      }
      else if (!used1 && used2)
      {
        return Integer.MAX_VALUE;
      }

      // Par nom
      return pref1.getTitle().toString().compareTo(pref2.getTitle().toString());
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class BaliseProviderPreferenceClickListener implements Preference.OnPreferenceClickListener
  {
    final WeakReference<AbstractBalisesPreferencesActivity> prefsActivity;

    /**
     * 
     * @param prefsActivity
     */
    BaliseProviderPreferenceClickListener(final AbstractBalisesPreferencesActivity prefsActivity)
    {
      super();
      this.prefsActivity = new WeakReference<AbstractBalisesPreferencesActivity>(prefsActivity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean onPreferenceClick(final Preference preference)
    {
      // Initialisations
      final boolean regional = isBaliseProviderCountryPreferenceKeyRegional(preference.getKey());
      final String countryRegionCode = parseBaliseProviderCountryPreferenceKey(preference.getKey());
      final String countryCode = BaliseProviderUtils.getCountryCode(countryRegionCode);
      final String regionCode = BaliseProviderUtils.getRegionCode(countryRegionCode);
      final CheckBoxPreference chkPref = (CheckBoxPreference)preference;

      // Si decochage
      if (!chkPref.isChecked())
      {
        chkPref.setSummary(null);

        // Si region => MAJ du pays
        if (!Utils.isStringVide(regionCode))
        {
          prefsActivity.get().updateBaliseProviderCountryPreference(prefsActivity.get().getPreferenceManager().findPreference(formatBaliseProviderCountryPreferenceKey(countryCode, true)));
        }

        // MAJ du continent
        final String continentCode = prefsActivity.get().continentCountry.get(countryCode);
        final Preference continentPref = prefsActivity.get().getPreferenceManager().findPreference(formatBaliseProviderContinentPreferenceKey(continentCode));
        prefsActivity.get().updateBaliseProviderContinentPreference(continentPref);

        return true;
      }

      // Initialisations
      final boolean[] availableProviders = BaliseProviderUtils.getAvailableBalisesProviders(prefsActivity.get(), countryCode, regionCode);
      final List<?>[] infos = BaliseProviderUtils.getBaliseProvidersCheckedForCountry(prefsActivity.get(), prefsActivity.get().sharedPreferences, countryRegionCode, availableProviders);
      final List<String> providerKeys = (List<String>)infos[0];
      final List<String> providerLabels = (List<String>)infos[1];
      final String[] providers = providerLabels.toArray(new String[0]);
      final List<Boolean> providerCheckeds = (List<Boolean>)infos[2];

      // Liste des providers actifs pour le pays
      final boolean[] checkeds = new boolean[providerCheckeds.size()];
      for (int i = 0; i < checkeds.length; i++)
      {
        checkeds[i] = providerCheckeds.get(i).booleanValue();
      }

      // Dialogue
      final AlertDialog.Builder builder = new AlertDialog.Builder(prefsActivity.get());
      builder.setTitle(preference.getTitle());
      builder.setIcon(R.drawable.icon);
      builder.setCancelable(false);

      // Choix multiples
      builder.setMultiChoiceItems(providers, checkeds, new DialogInterface.OnMultiChoiceClickListener()
      {
        @Override
        public void onClick(final DialogInterface dialog, final int which, final boolean isChecked)
        {
          final String prefKey = formatBaliseProviderPreferenceKey(providerKeys.get(which), countryRegionCode);
          final Editor editor = prefsActivity.get().sharedPreferences.edit();
          editor.putBoolean(prefKey, isChecked);
          ActivityCommons.commitPreferences(editor);
        }
      });

      // Bouton OK
      builder.setPositiveButton(prefsActivity.get().resources.getString(R.string.button_ok), new DialogInterface.OnClickListener()
      {
        @Override
        public void onClick(final DialogInterface dialog, final int which)
        {
          final Preference countryPreference = prefsActivity.get().getPreferenceManager().findPreference(formatBaliseProviderCountryPreferenceKey(countryRegionCode, regional));
          prefsActivity.get().updateBaliseProviderCountryPreference(countryPreference);
          final String continentCode = prefsActivity.get().continentCountry.get(countryCode);
          final Preference continentPref = prefsActivity.get().getPreferenceManager().findPreference(formatBaliseProviderContinentPreferenceKey(continentCode));
          prefsActivity.get().updateBaliseProviderContinentPreference(continentPref);
          dialog.dismiss();
        }
      });

      // Affichage de la liste des choix des providers pour le pays
      final AlertDialog alertDialog = builder.create();
      alertDialog.show();
      ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_PREFS_BALISE_PROVIDER_CHOICE, alertDialog, null);

      return true;
    }
  }

  /**
   * 
   */
  void spotsDownloadProgress()
  {
    // Dismiss de securite
    if (progressDialogDownload != null)
    {
      progressDialogDownload.dismiss();
    }

    progressDialogDownload = new ProgressDialog(this);
    progressDialogDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialogDownload.setTitle(resources.getString(R.string.label_spots_download));
    progressDialogDownload.setMessage(resources.getString(R.string.label_spots_download_progress));
    progressDialogDownload.setCancelable(false);
    progressDialogDownload.setProgress(0);
    progressDialogDownload.setButton(resources.getString(R.string.button_cancel), new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int wich)
      {
        continueDownload = false;
        ThreadUtils.interrupt(spotDownloadThreads);
      }
    });

    progressDialogDownload.show();
    ActivityCommons.registerProgressDialog(ActivityCommons.PROGRESS_DIALOG_SPOTS_DOWNLOAD, progressDialogDownload, null);
  }

  /**
   * 
   * @param inChecked
   * @param inValue
   * @param unit
   */
  private void updateSummaryLimitMoy(final Boolean inChecked, final Integer inValue, final String unit)
  {
    // Valeur de la case a cocher
    Boolean checked = inChecked;
    if (checked == null)
    {
      // Si non fournie, recherche en sharedPreferences
      checked = Boolean.valueOf(sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_moy_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_moy_check_default))));
    }

    // Valeur de la limite
    Integer value = inValue;
    if (value == null)
    {
      // Si non fournie, recherche en sharedPreferences
      value = Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_map_limit_moy_edit_key), Integer.parseInt(resources.getString(R.string.config_map_limit_moy_edit_default), 10)));
    }

    // Mise a jour du resume
    updateSummaryLimit(getPreferenceScreen().findPreference(resources.getString(R.string.config_map_limit_moy_check_key)), checked, value, unit);
  }

  /**
   * 
   * @param inChecked
   * @param inValue
   * @param unit
   */
  private void updateSummaryLimitMax(final Boolean inChecked, final Integer inValue, final String unit)
  {
    // Valeur de la case a cocher
    Boolean checked = inChecked;
    if (checked == null)
    {
      // Si non fournie, recherche en sharedPreferences
      checked = Boolean.valueOf(sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_max_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_max_check_default))));
    }

    // Valeur de la limite
    Integer value = inValue;
    if (value == null)
    {
      // Si non fournie, recherche en sharedPreferences
      value = Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_map_limit_max_edit_key), Integer.parseInt(resources.getString(R.string.config_map_limit_max_edit_default), 10)));
    }

    // Mise a jour du resume
    updateSummaryLimit(getPreferenceScreen().findPreference(resources.getString(R.string.config_map_limit_max_check_key)), checked, value, unit);
  }

  /**
   * 
   * @param preference
   * @param checked
   * @param value
   * @param unit
   */
  private void updateSummaryLimit(final Preference preference, final Boolean checked, final Integer value, final String unit)
  {
    if ((checked != null) && checked.booleanValue())
    {
      final String finalUnit = (unit == null ? ActivityCommons.getSpeedUnit() : unit);
      preference.setSummary(resources.getString(R.string.label_map_windslimits_summary_checked) + Strings.CHAR_SPACE + value + finalUnit);
    }
    else
    {
      preference.setSummary(resources.getString(R.string.label_map_windslimits_summary_unchecked));
    }
  }

  @Override
  public boolean onPreferenceClick(final Preference preference)
  {
    if (resources.getString(R.string.config_map_search_reset_key).equals(preference.getKey()))
    {
      onResetSearchClicked();
    }

    return false;
  }

  /**
   * 
   */
  private void onResetSearchClicked()
  {
    // Liste des providers enregistres
    final SearchDatabaseHelper helper = new SearchDatabaseHelper(getApplicationContext());
    final SQLiteDatabase database = helper.getReadableDatabase();
    try
    {
      final List<SearchItem> providerItems = helper.searchProviders(database);

      // Aucun provider
      final String title;
      final String message;
      final int okButtonId;
      final int cancelButtonId;
      final String[] items;
      final boolean[] checkedItems;
      final OnMultiChoiceClickListener checkedListener;
      final boolean alreadyEmpty = (providerItems.size() <= 0);
      if (!alreadyEmpty)
      {
        // Initialisations
        final String balisesPrefix = getResources().getString(R.string.label_map_search_reset_balises_prefix);
        final String spotsPrefix = getResources().getString(R.string.label_map_search_reset_spots_prefix);
        title = getResources().getString(R.string.message_config_search_delete_title);
        message = null;
        okButtonId = R.string.button_ok;
        cancelButtonId = R.string.button_cancel;

        // Liste des items
        final int itemsSize = providerItems.size();
        items = new String[itemsSize];
        checkedItems = new boolean[itemsSize];
        int i = 0;
        for (final SearchItem item : providerItems)
        {
          items[i] = (SearchDatabaseHelper.ITEM_TYPE_BALISE.equals(item.type) ? balisesPrefix : spotsPrefix) + " : " + item.provider;
          checkedItems[i] = false;
          i++;
        }

        // Listener
        checkedListener = new OnMultiChoiceClickListener()
        {
          @Override
          public void onClick(final DialogInterface dialog, final int which, final boolean isChecked)
          {
            checkedItems[which] = isChecked;
          }
        };
      }
      else
      {
        // Plus de donnees => proposition de vidage/raz de la structure entiere
        title = getResources().getString(R.string.app_name);
        message = getResources().getString(R.string.message_config_search_reset);
        okButtonId = R.string.button_yes;
        cancelButtonId = R.string.button_no;
        items = null;
        checkedItems = null;
        checkedListener = null;
      }

      // Dialogue
      ActivityCommons.confirmDialog(this, ActivityCommons.CONFIRM_DIALOG_SEARCH_RESET, title, message, new OnClickListener()
      {
        @Override
        public void onClick(final DialogInterface dialog, final int which)
        {
          // Initialisations
          final SQLiteDatabase writeDatabase = helper.getWritableDatabase();

          try
          {
            // Base deja vide => raz de la structure
            if (alreadyEmpty)
            {
              // Vidage
              writeDatabase.beginTransaction();
              try
              {
                helper.clean(writeDatabase);
                writeDatabase.setTransactionSuccessful();
              }
              finally
              {
                writeDatabase.endTransaction();
              }

              // Message
              Toast.makeText(AbstractBalisesPreferencesActivity.this, R.string.message_config_search_reset_done, Toast.LENGTH_SHORT).show();
            }

            // Base non vide => suppression pour les items choisis
            else if (checkedItems != null)
            {
              boolean deleted = false;
              int i = 0;
              writeDatabase.beginTransaction();
              try
              {
                for (final SearchItem item : providerItems)
                {
                  if (checkedItems[i])
                  {
                    // Suppression
                    helper.deleteForProvider(writeDatabase, item.type, item.provider);

                    // Marquage
                    deleted = true;
                  }

                  // Next
                  i++;
                }

                // Fin
                writeDatabase.setTransactionSuccessful();
              }
              finally
              {
                writeDatabase.endTransaction();
              }

              // Message
              Toast.makeText(AbstractBalisesPreferencesActivity.this, (deleted ? R.string.message_config_search_delete_done : R.string.message_config_search_delete_nothing), Toast.LENGTH_SHORT).show();
            }
          }
          finally
          {
            database.close();
          }
        }
      }, null, true, null, okButtonId, cancelButtonId, items, checkedItems, checkedListener);
    }
    finally
    {
      database.close();
    }
  }
}
