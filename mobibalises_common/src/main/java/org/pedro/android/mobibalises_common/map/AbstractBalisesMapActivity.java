package org.pedro.android.mobibalises_common.map;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.pedro.android.map.MapActivity;
import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.BalisesExceptionHandler;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.analytics.AnalyticsService;
import org.pedro.android.mobibalises_common.location.LocationService;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.provider.SpotProviderUtils;
import org.pedro.android.mobibalises_common.search.AbstractSearchActivity;
import org.pedro.android.mobibalises_common.search.SearchDatabaseHelper;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService.BaliseProviderMode;
import org.pedro.android.mobibalises_common.service.BaliseProviderInfos;
import org.pedro.android.mobibalises_common.service.BaliseProviderListener;
import org.pedro.android.mobibalises_common.service.IProvidersService;
import org.pedro.android.mobibalises_common.service.ProvidersServiceBinder;
import org.pedro.android.mobibalises_common.service.SpotProviderListener;
import org.pedro.android.mobibalises_common.service.WebcamProviderListener;
import org.pedro.android.mobibalises_common.view.BaliseDrawable;
import org.pedro.android.mobibalises_common.view.BaliseDrawableHelper;
import org.pedro.android.mobibalises_common.webcam.WebcamDatabaseHelper;
import org.pedro.android.mobibalises_common.webcam.WebcamDatabaseHelper.WebcamRow;
import org.pedro.android.os.ActivityAsyncTask;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Utils;
import org.pedro.map.BuiltInTileProvider;
import org.pedro.map.GeoPoint;
import org.pedro.map.MapController;
import org.pedro.map.Overlay;
import org.pedro.map.OverlayItem;
import org.pedro.map.Point;
import org.pedro.map.TileProvider;
import org.pedro.map.TileProviderHelper;
import org.pedro.map.tileprovider.IGNMapTileProvider;
import org.pedro.map.tileprovider.IGNSatelliteTileProvider;
import org.pedro.spots.Spot;
import org.pedro.spots.SpotProvider;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractBalisesMapActivity extends MapActivity implements SharedPreferences.OnSharedPreferenceChangeListener, BaliseProviderListener, SpotProviderListener, ItemsTapListener, BaliseTapListener, SpotTapListener,
    WebcamTapListener, MapController.MapControllerListener, WebcamProviderListener
{
  public static final String         ACTION_ITEM                     = "org.pedro.mobibalises.ITEM";
  public static final String         ACTION_ITEM_TYPE                = "type";
  public static final String         ACTION_ITEM_PROVIDER            = "provider";
  public static final String         ACTION_ITEM_ID                  = "id";
  public static final String         ACTION_ITEM_LATITUDE            = "lat";
  public static final String         ACTION_ITEM_LONGITUDE           = "lng";

  private static final String        FORMAT_CONTEXT_MENU_TITLE       = "{0} ({1})";
  private static final String        FORMAT_CONTEXT_MENU_ITEMS_TITLE = "{0} : {1}";

  public static final String         EXTRA_LATITUDE                  = AbstractBalisesMapActivity.class.getName() + ".EXTRA_LATITUDE";
  public static final String         EXTRA_LONGITUDE                 = AbstractBalisesMapActivity.class.getName() + ".EXTRA_LONGITUDE";
  public static final String         EXTRA_ZOOM                      = AbstractBalisesMapActivity.class.getName() + ".EXTRA_ZOOM";
  public static final String         EXTRA_BALISE_ID                 = AbstractBalisesMapActivity.class.getName() + ".EXTRA_BALISE_ID";
  public static final String         EXTRA_BALISE_PROVIDER_ID        = AbstractBalisesMapActivity.class.getName() + ".EXTRA_BALISE_PROVIDER_ID";

  private static final int           HANDLER_ACTION_BAR_HIDER        = 0;
  private static final long          ACTION_BAR_HIDE_TIMEOUT         = MapView.ZOOM_CONTROLS_HIDE_TIMEOUT;

  private static final String        CUSTOM_MAPS_KEY                 = "custom_maps";
  private static final File          CUSTOM_MAPS_FILE                = new File(ActivityCommons.MOBIBALISES_EXTERNAL_STORAGE_PATH, "custom_maps.properties");

  private static final String        INTENT_FILE_DATA_SCHEME         = "file";
  private static final String        BING_API_KEY                    = "Aq_ZKLo-P5FKgQjX9aXzH6ZKcA2mNVfi3UXXC9n_TzvxSwZGfRSEkxmpIi2ZpQ-C";

  private static final String        GOOGLE_API_KEY                  = "AIzaSyDWMfbmMuk9xwenJ6xx3lqV-6s7pOwS7GA";

  //private static final String        IGN_DEV_API_KEY                 = "4ha20fdfovhv4t1o3gpk06kx";
  protected static final String      IGN_FREE_API_KEY                = "e6a2vpjfnlhpem51ohdc0pdd";

  private static final int           ITEMS_MENU_GROUP_ID             = 123456;

  private ProvidersServiceConnection providersServiceConnection;
  protected IProvidersService        providersService;
  boolean                            updateSpotProvidersInProgress   = false;
  private final Object               updateSpotProvidersLock         = new Object();
  private boolean                    firstOnResume                   = true;

  // MapView
  MapView                            mapView;
  String                             currentTileProviderKey;
  final Object                       mapInitLock                     = new Object();
  boolean                            mapInitialized;

  protected ItemsOverlay             itemsOverlay;

  private float                      scalingFactor;

  protected SharedPreferences        sharedPreferences;

  protected Resources                resources;

  private boolean                    backKeyDouble                   = true;

  protected boolean                  currentBaliseWindDatasDisplayed;
  protected boolean                  currentBaliseWeatherDatasDisplayed;
  boolean                            currentSiteDatasDisplayed;
  protected boolean                  currentWebcamDatasDisplayed;

  enum ItemType
  {
    BALISE, SPOT, WEBCAM, LISTE_ITEMS, LISTE_ITEMS_LONG
  }

  private ItemType                    contextMenuItemType;

  protected BaliseProvider            contextMenuBaliseProvider;
  protected String                    contextMenuBaliseProviderKey;
  protected String                    contextMenuIdBalise;

  protected SpotItem                  contextMenuSpotItem;

  protected WebcamItem                contextMenuWebcamItem;

  protected List<OverlayItem<Canvas>> contextMenuTapItems;

  protected boolean                   preferencesLaunched   = false;

  private boolean                     saveCenterOnQuit      = true;
  private String                      startBaliseId         = null;
  private String                      startBaliseProviderId = null;
  private boolean                     startBaliseDone       = true;

  // Localisation
  protected BalisesLocationListener   locationListener;
  protected LocationOverlay           locationOverlay;
  protected boolean                   doLocationAtStartup;

  // Message
  private ProviderMessageOverlay      messageOverlay;

  // Gestion des erreurs
  private BalisesExceptionHandler     exceptionHandler;

  // Ecoute pour le positionnement sur un item
  private ItemBroadcastReceiver       itemReceiver          = new ItemBroadcastReceiver(this);

  // Ecoute de la SDCard
  private SDCardBroadcastReceiver     sdcardReceiver        = new SDCardBroadcastReceiver(this);

  // Gestion des collisions
  private ManageBalisesProchesHandler manageBalisesProchesHandler;

  // Helper pour les fournisseurs de tuile
  private final TileProviderHelper    tileProviderHelper    = new TileProviderHelper();

  // Gestion ActionBar
  private ActionBarHideHandler        actionBarHideHandler;
  private boolean                     hasPermanentMenuKey;
  private boolean                     isHoneyComb;
  private Menu                        actionBarMenu;

  // Gestion splashscreen
  private boolean                     showSplash;
  private long                        createTimestamp;

  // Gestion taches de fond
  private List<AsyncTask<?, ?, ?>>    asyncTasks            = new ArrayList<AsyncTask<?, ?, ?>>();

  /**
   * 
   * @author pedro.m
   */
  private static class ActionBarHideHandler extends Handler
  {
    private AbstractBalisesMapActivity mapActivity;

    /**
     * 
     * @param mapActivity
     */
    ActionBarHideHandler(final AbstractBalisesMapActivity mapActivity)
    {
      super(Looper.getMainLooper());
      this.mapActivity = mapActivity;
    }

    @Override
    public void handleMessage(final Message msg)
    {
      mapActivity.hideActionBar();
    }

    /**
     * 
     */
    public void removeMessages()
    {
      removeMessages(0);
    }

    /**
     * 
     */
    void onShutdown()
    {
      removeMessages();
      mapActivity = null;
    }
  }

  /**
   * 
   */
  private void initActionBarHideHandler()
  {
    actionBarHideHandler = new ActionBarHideHandler(this);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ItemBroadcastReceiver extends BroadcastReceiver
  {
    AbstractBalisesMapActivity mapActivity;

    /**
     * 
     * @param mapActivity
     */
    ItemBroadcastReceiver(final AbstractBalisesMapActivity mapActivity)
    {
      super();
      this.mapActivity = mapActivity;
    }

    @Override
    public void onReceive(final Context inContext, final Intent intent)
    {
      // Initialisations
      Log.d(mapActivity.getClass().getSimpleName(), "ItemBroadcastReceiver.onReceive() : " + intent);

      // Analyse Intent
      final int type = intent.getIntExtra(ACTION_ITEM_TYPE, -1);
      final String provider = intent.getStringExtra(ACTION_ITEM_PROVIDER);
      final String id = intent.getStringExtra(ACTION_ITEM_ID);
      final double latitude = intent.getDoubleExtra(ACTION_ITEM_LATITUDE, Double.NaN);
      final double longitude = intent.getDoubleExtra(ACTION_ITEM_LONGITUDE, Double.NaN);
      Log.d(mapActivity.getClass().getSimpleName(), "type=" + type + ", provider=" + provider + ", id=" + id);

      // Action
      if (type == 1)
      {
        onReceiveBalise(provider, id);
      }
      else if (type == 2)
      {
        onReceiveSpot(provider, id, Double.valueOf(latitude), Double.valueOf(longitude));
      }
      else if (type == 3)
      {
        onReceiveWebcam(provider, id);
      }
    }

    /**
     * 
     * @param providerKey
     * @param id
     */
    private void onReceiveBalise(final String providerKey, final String id)
    {
      // Activation de la couche balise si besoin
      final Runnable layerConfigRun = new Runnable()
      {
        @Override
        public void run()
        {
          if (!mapActivity.currentBaliseWindDatasDisplayed && !mapActivity.currentBaliseWeatherDatasDisplayed)
          {
            mapActivity.currentBaliseWindDatasDisplayed = true;
            mapActivity.currentBaliseWeatherDatasDisplayed = Boolean.parseBoolean(mapActivity.resources.getString(R.string.config_map_layers_weather_default));
            final SharedPreferences.Editor editor = mapActivity.sharedPreferences.edit();
            editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES, mapActivity.currentBaliseWindDatasDisplayed);
            editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WEATHER_BALISES, mapActivity.currentBaliseWeatherDatasDisplayed);
            ActivityCommons.commitPreferences(editor);
            mapActivity.onMapLayersChanged(true, false);
          }
        }
      };

      // Activation du provider si besoin
      final Runnable providerConfigRun = new Runnable()
      {
        @Override
        public void run()
        {
          final BaliseProvider provider = mapActivity.providersService.getBaliseProvider(providerKey);

          if (provider == null)
          {
            // Activation
            final String prefKey = AbstractBalisesPreferencesActivity.formatBaliseProviderPreferenceKey(providerKey);
            final Editor editor = mapActivity.sharedPreferences.edit();
            editor.putBoolean(prefKey, true);
            final String country = BaliseProviderUtils.getBaliseProviderCountryCode(providerKey);
            final String countryKey = AbstractBalisesPreferencesActivity.formatBaliseProviderCountryPreferenceKey(country, false);
            editor.putBoolean(countryKey, true);
            ActivityCommons.commitPreferences(editor);

            // Notification du service
            mapActivity.providersService.updateBaliseProviders(false);
          }
        }
      };

      // Activations/reglages en tache de fond et focus sur l'item a la fin
      final AsyncTask<Object, Void, Void> task = new AsyncTask<Object, Void, Void>()
      {
        @Override
        protected Void doInBackground(final Object... args)
        {
          layerConfigRun.run();
          providerConfigRun.run();

          return null;
        }

        @Override
        protected void onPostExecute(final Void result)
        {
          final BaliseProvider provider = mapActivity.providersService.getBaliseProvider(providerKey);
          boolean found = false;
          if (provider != null)
          {
            final Balise balise = provider.getBaliseById(id);
            if ((balise != null) && !Double.isNaN(balise.latitude) && !Double.isNaN(balise.longitude))
            {
              final GeoPoint center = new GeoPoint(balise.latitude, balise.longitude);
              final BaliseItem item = new BaliseItem(providerKey, id, null, null);
              mapActivity.itemsOverlay.onTap(item);
              mapActivity.itemsOverlay.requestRedraw();
              mapActivity.mapView.getController().animateTo(center);
              found = true;
            }
          }

          if (!found)
          {
            // Balise non trouvee
            Toast.makeText(mapActivity, mapActivity.getResources().getString(R.string.message_balise_not_found), Toast.LENGTH_SHORT).show();
          }
        }
      };
      mapActivity.executeAsyncTask(task);
    }

    /**
     * 
     * @author pedro.m
     */
    private static interface SpotConfigRunnable
    {
      /**
       * 
       * @return
       */
      public SpotItem run();
    }

    /**
     * 
     * @param inProviderKey
     * @param id
     * @param latitude
     * @param longitude
     */
    private void onReceiveSpot(final String inProviderKey, final String id, final Double latitude, final Double longitude)
    {
      // Initialisations
      final String siteProvidersBaseKey = mapActivity.getResources().getString(R.string.site_providers_base_key);
      final String fullProviderKey = siteProvidersBaseKey + Strings.CHAR_POINT + inProviderKey;
      final String providerKey = SpotProviderUtils.getSpotProviderKey(fullProviderKey);

      // Activation de la couche spots si besoin
      final Runnable layerConfigRun = new Runnable()
      {
        @Override
        public void run()
        {
          if (!mapActivity.currentSiteDatasDisplayed)
          {
            mapActivity.currentSiteDatasDisplayed = true;
            final SharedPreferences.Editor editor = mapActivity.sharedPreferences.edit();
            editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES, mapActivity.currentSiteDatasDisplayed);
            ActivityCommons.commitPreferences(editor);
            mapActivity.onMapLayersChanged(false, false);
          }
        }
      };

      // Activation du provider si besoin
      final SpotConfigRunnable providerConfigRun = new SpotConfigRunnable()
      {
        @Override
        public SpotItem run()
        {
          // Activation
          final Editor editor = mapActivity.sharedPreferences.edit();
          editor.putBoolean(fullProviderKey, true);
          editor.putBoolean(providerKey, true);
          ActivityCommons.commitPreferences(editor);

          // Notification du service
          mapActivity.providersService.updateSpotProviders();

          // Options de filtre d'affichage
          final SpotItem item;
          final SpotProvider provider = mapActivity.providersService.getSpotProvider(providerKey);
          if ((provider != null) && (latitude != null) && (longitude != null))
          {
            final Spot spot = new Spot();
            spot.id = id;
            spot.latitude = latitude;
            spot.longitude = longitude;
            item = new SpotItem(fullProviderKey, spot, new GeoPoint(latitude.doubleValue(), longitude.doubleValue()), null);
          }
          else
          {
            item = null;
          }

          return item;
        }
      };

      // Activations/reglages en tache de fond et focus sur l'item a la fin
      final AsyncTask<Object, Void, SpotItem> task = new AsyncTask<Object, Void, SpotItem>()
      {
        @Override
        protected SpotItem doInBackground(final Object... args)
        {
          // Config
          layerConfigRun.run();
          final SpotItem item = providerConfigRun.run();

          // Recuperation du spot
          return item;
        }

        @Override
        protected void onPostExecute(final SpotItem spotItem)
        {
          // Centrage sur le spot
          if (spotItem != null)
          {
            mapActivity.itemsOverlay.onTap(spotItem);
            mapActivity.itemsOverlay.requestRedraw();
            mapActivity.mapView.getController().animateTo(spotItem.getPoint());
          }
          else
          {
            // Spot non trouve
            Toast.makeText(mapActivity, mapActivity.getResources().getString(R.string.message_spot_not_found), Toast.LENGTH_SHORT).show();
          }
        }
      };
      mapActivity.executeAsyncTask(task);
    }

    /**
     * 
     * @param inProviderKey
     * @param id
     */
    private void onReceiveWebcam(final String inProviderKey, final String id)
    {
      // Activation de la couche webcams si besoin
      final Runnable layerConfigRun = new Runnable()
      {
        @Override
        public void run()
        {
          if (!mapActivity.currentWebcamDatasDisplayed)
          {
            mapActivity.currentWebcamDatasDisplayed = true;
            final SharedPreferences.Editor editor = mapActivity.sharedPreferences.edit();
            editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WEBCAMS, true);
            ActivityCommons.commitPreferences(editor);
            mapActivity.onMapLayersChanged(false, true);
          }
        }
      };

      // Activations/reglages en tache de fond et focus sur l'item a la fin
      final AsyncTask<Object, Void, WebcamItem> task = new AsyncTask<Object, Void, WebcamItem>()
      {
        @Override
        protected WebcamItem doInBackground(final Object... args)
        {
          // Config
          layerConfigRun.run();

          // Recherche en base
          final WebcamDatabaseHelper helper = WebcamDatabaseHelper.newInstance(mapActivity.getApplicationContext());
          SQLiteDatabase database = null;
          WebcamRow row;
          try
          {
            database = helper.getReadableDatabase();
            row = helper.selectWebcam(database, inProviderKey, id);
          }
          finally
          {
            if (database != null)
            {
              database.close();
            }
          }

          final WebcamItem item;
          if (row != null)
          {
            final WebcamDrawable webcamDrawable = new WebcamDrawable(row, mapActivity.getMapView(), mapActivity.getResources().getString(R.string.menu_context_map_webcam_webmap));
            webcamDrawable.validateDrawable();
            item = new WebcamItem(row, new GeoPoint(row.latitude, row.longitude), webcamDrawable);
          }
          else
          {
            item = null;
          }

          // Recuperation du spot
          return item;
        }

        @Override
        protected void onPostExecute(final WebcamItem webcamItem)
        {
          // Centrage sur le spot
          if (webcamItem != null)
          {
            mapActivity.itemsOverlay.onTap(webcamItem);
            mapActivity.itemsOverlay.requestRedraw();
            mapActivity.mapView.getController().animateTo(webcamItem.getPoint());
          }
          else
          {
            // Webcam non trouvee
            Toast.makeText(mapActivity, mapActivity.getResources().getString(R.string.message_webcam_not_found), Toast.LENGTH_SHORT).show();
          }
        }
      };
      mapActivity.executeAsyncTask(task);
    }

    /**
     * 
     */
    void onShutdown()
    {
      mapActivity = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class SDCardBroadcastReceiver extends BroadcastReceiver
  {
    private AbstractBalisesMapActivity mapActivity;

    /**
     * 
     * @param mapActivity
     */
    SDCardBroadcastReceiver(final AbstractBalisesMapActivity mapActivity)
    {
      super();
      this.mapActivity = mapActivity;
    }

    @Override
    public void onReceive(final Context inContext, final Intent intent)
    {
      mapActivity.refreshCacheAvailability();
      mapActivity.refreshCustomTileProvidersConfiguration(true);
    }

    /**
     * 
     */
    void onShutdown()
    {
      mapActivity = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class BaliseAndSpotProvidersUpdateAsyncTask extends ActivityAsyncTask<Object, Void, Map<String, IOException>>
  {
    private final boolean doBalises;
    private final int     dialogId;

    /**
     * 
     * @param activity
     * @param doBalises
     * @param dialogId
     */
    BaliseAndSpotProvidersUpdateAsyncTask(final AbstractBalisesMapActivity activity, final boolean doBalises, final int dialogId)
    {
      super(activity);
      this.doBalises = doBalises;
      this.dialogId = dialogId;
    }

    @Override
    protected void onPreExecute()
    {
      if (dialogId > 0)
      {
        ActivityCommons.progressDialog(activity, dialogId, true, false, null);
      }
    }

    @Override
    protected Map<String, IOException> doInBackground(final Object... args)
    {
      // Initialisations
      final AbstractBalisesMapActivity mapActivity = (AbstractBalisesMapActivity)activity;

      // MAJ via le service
      if (doBalises)
      {
        mapActivity.providersService.updateBaliseProviders(false);
      }
      return mapActivity.providersService.updateSpotProviders();
    }

    @Override
    public void onPostExecute(final Map<String, IOException> result)
    {
      // Initialisations
      final AbstractBalisesMapActivity mapActivity = (AbstractBalisesMapActivity)activity;

      // Fin
      if (dialogId > 0)
      {
        ActivityCommons.cancelProgressDialog(dialogId);
      }
      if (!doBalises)
      {
        mapActivity.updateSpotProvidersInProgress = false;
      }
      mapActivity.showSpotErrors(result);

      // Super
      super.onPostExecute(result);
    }
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    // Splash screen
    createTimestamp = System.currentTimeMillis();

    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onCreate()");
    super.onCreate(savedInstanceState);

    // Avant tout !
    exceptionHandler = ActivityCommons.initExceptionHandler(getApplicationContext());
    mapInitialized = false;

    // Splash screen
    showSplash = ActivityCommons.manageSplash(this, R.layout.splash_map, R.layout.map);

    // Initialisations communes
    initCommons();

    // Initialisations de base
    firstOnResume = true;
    resources = getApplicationContext().getResources();
    sharedPreferences = ActivityCommons.getSharedPreferences(getApplicationContext());

    // Initialisations
    init();

    // Barre de navigation
    if (isHoneyComb)
    {
      getActionBar().hide();
    }

    // RAZ recherche
    AbstractSearchActivity.setLastQuery(null);

    // Effacement splash screen
    if (showSplash)
    {
      ActivityCommons.hideSplash(this, createTimestamp);
      showSplash = false;
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onCreate()");
  }

  /**
   * 
   */
  protected void initCommons()
  {
    ActivityCommons.init(getApplicationContext());
    ActivityCommons.initFromActivity();
  }

  /**
   * 
   */
  void init()
  {
    // Gestion des versions Android
    hasPermanentMenuKey = ActivityCommons.hasPermanentMenuKey(getApplicationContext());
    isHoneyComb = ActivityCommons.isHoneyComb();

    // Init Graphique
    initGraphics();

    // Init MapView
    mapView = (MapView)findViewById(R.id.mapview);
    final AsyncTask<Object, Void, Void> initMaptask = new AsyncTask<Object, Void, Void>()
    {
      @Override
      protected Void doInBackground(final Object... params)
      {
        // Initialisation
        if (!isCancelled())
        {
          initMapView();
        }

        // Configuration
        if (!isCancelled())
        {
          configureMapView();
        }

        // Deblocage des Threads qui attendent
        synchronized (mapInitLock)
        {
          mapInitialized = true;
          mapInitLock.notifyAll();
        }

        // Fin
        if (isCancelled())
        {
          Log.d(AbstractBalisesMapActivity.this.getClass().getSimpleName(), "Map init & configuration cancelled !");
        }
        else
        {
          Log.d(AbstractBalisesMapActivity.this.getClass().getSimpleName(), "Map init & configuration done");
        }
        return null;
      }
    };
    executeAsyncTask(initMaptask);

    // Preferences
    configurePreferences();

    // Localisation
    initLocation();

    // Initialisation de la gestion des balises proches
    initManageBalisesProchesHandler();

    // Initialisation de la gestion de la barre d'action
    initActionBarHideHandler();

    // Demarrage du service
    initProvidersService();

    // A l'ecoute pour le positionnement
    registerItemListener();

    // A l'ecoute de la SDCard
    registerSDCardListener();
  }

  /**
   * 
   */
  private void registerItemListener()
  {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_ITEM);
    registerReceiver(itemReceiver, filter);
  }

  /**
   * 
   */
  private void registerSDCardListener()
  {
    final IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
    filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    filter.addDataScheme(INTENT_FILE_DATA_SCHEME);
    registerReceiver(sdcardReceiver, filter);
  }

  /**
   * 
   */
  private void unregisterItemListener()
  {
    unregisterReceiver(itemReceiver);
    itemReceiver.onShutdown();
    itemReceiver = null;
  }

  /**
   * 
   */
  private void unregisterSDCardListener()
  {
    unregisterReceiver(sdcardReceiver);
    sdcardReceiver.onShutdown();
    sdcardReceiver = null;
  }

  /**
   * 
   */
  void refreshCacheAvailability()
  {
    if ((mapView != null) && (mapView.getTileCache() != null))
    {
      mapView.getTileCache().refreshAvailability();
    }
  }

  /**
   * 
   */
  private void configurePreferences()
  {
    // Preferences partagees
    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_map_centering_key));
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_error_reports_key));
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_wakelock_key));
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_double_backkey_key));
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_data_sites_orientations_zoom_level_key));
    onSharedPreferenceChanged(sharedPreferences, AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES);
    onSharedPreferenceChanged(sharedPreferences, AbstractBalisesPreferencesActivity.CONFIG_DATA_WEATHER_BALISES);
    onSharedPreferenceChanged(sharedPreferences, AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES);
    onSharedPreferenceChanged(sharedPreferences, AbstractBalisesPreferencesActivity.CONFIG_DATA_WEBCAMS);
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_unit_speed_key));
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_unit_altitude_key));
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_unit_distance_key));
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_unit_temperature_key));

    // Marquage de l'activite de demarrage
    ActivityCommons.saveStartActivity(resources.getString(R.string.intent_map_action));
  }

  /**
   * 
   * @author pedro.m
   */
  private static final class ManageBalisesProchesHandler extends Handler
  {
    private AbstractBalisesMapActivity balisesMapActivity;

    /**
     * 
     * @param balisesMapActivity
     */
    ManageBalisesProchesHandler(final AbstractBalisesMapActivity balisesMapActivity)
    {
      super(Looper.getMainLooper());
      this.balisesMapActivity = balisesMapActivity;
    }

    @Override
    public void handleMessage(final Message msg)
    {
      balisesMapActivity.itemsOverlay.manageBalisesProches(balisesMapActivity.getApplicationContext(), balisesMapActivity.sharedPreferences);
    }

    /**
     * 
     */
    public void removeMessages()
    {
      removeMessages(0);
    }

    /**
     * 
     */
    void onShutdown()
    {
      removeMessages();
      balisesMapActivity = null;
    }
  }

  /**
   * 
   */
  private void initManageBalisesProchesHandler()
  {
    manageBalisesProchesHandler = new ManageBalisesProchesHandler(this);
  }

  /**
   * 
   */
  void initMapView()
  {
    // Initialisations
    registerForContextMenu(mapView);

    // Overlay pour le(s) message(s)
    messageOverlay = new ProviderMessageOverlay(this);

    // Overlay pour la position courante
    locationOverlay = getNewLocationOverlay(mapView);

    // Overlay pour les balises et spots
    itemsOverlay = new ItemsOverlay(getApplicationContext(), sharedPreferences, mapView, scalingFactor);
    itemsOverlay.addItemsTapListener(this);
    itemsOverlay.addWebcamLongTapListener(this);
    itemsOverlay.start();

    // Ajout des couches
    try
    {
      final List<Overlay<Bitmap, Canvas>> overlays = mapView.getOverlays(true);
      overlays.add(messageOverlay);
      overlays.add(locationOverlay);
      overlays.add(itemsOverlay);
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      mapView.unlockWriteOverlays();
    }

    // Pour forcer l'ajustement de la taille de la fenetre qui peut avoir change APRES l'instanciation
    // du Overlay ci dessus (=> la taille de la fenetre est alors a zero) et AVANT l'ajout dans la liste des overlays
    // dans le TileManager.
    // Ce qui fait que le TileManager.onMapDisplayerSizeChanged qui eventuellement intervient entre ces 2 moments
    // ne prend pas en compte l'overlay puisqu'il n'est pas encore dans la liste
    itemsOverlay.onMapDisplayerSizeChanged();

    // Listener du controller
    mapView.getController().addMapControllerListener(this);
  }

  /**
   * 
   * @param inMapView
   * @return
   */
  @SuppressWarnings("static-method")
  protected LocationOverlay getNewLocationOverlay(final MapView inMapView)
  {
    return new LocationOverlay(inMapView);
  }

  @Override
  public MapView getMapView()
  {
    return mapView;
  }

  /**
   * 
   */
  void configureMapView()
  {
    // Debug ou bien
    mapView.setIsApiKeyDebug(ActivityCommons.isDebug(getApplicationContext()));

    // Clef Bing
    mapView.setBingApiKey(BING_API_KEY);

    // Clef google maps static
    mapView.setGoogleApiKey(GOOGLE_API_KEY);

    // Clef IGN
    mapView.setIgnApiKey(getIgnApiKey());

    // Init configuration
    final Intent intent = getIntent();

    // Centre
    final int intentLatitude = intent.getIntExtra(EXTRA_LATITUDE, Integer.MAX_VALUE);
    final int intentLongitude = intent.getIntExtra(EXTRA_LONGITUDE, Integer.MAX_VALUE);
    final int latitude;
    final int longitude;
    if ((intentLatitude != Integer.MAX_VALUE) && (intentLongitude != Integer.MAX_VALUE))
    {
      // Position donnee en parametres
      latitude = intentLatitude;
      longitude = intentLongitude;
      saveCenterOnQuit = false;
    }
    else if (sharedPreferences.contains(AbstractBalisesPreferencesActivity.CONFIG_CENTER_LAT) && sharedPreferences.contains(AbstractBalisesPreferencesActivity.CONFIG_CENTER_LON))
    {
      // Position sauvegardee dans les preferences
      latitude = sharedPreferences.getInt(AbstractBalisesPreferencesActivity.CONFIG_CENTER_LAT, -1);
      longitude = sharedPreferences.getInt(AbstractBalisesPreferencesActivity.CONFIG_CENTER_LON, -1);
    }
    else
    {
      // Sinon : premier lancement, tentative de geolocalisation
      final Location location = LocationService.requestSingleLocation(getApplicationContext());
      if (location != null)
      {
        latitude = (int)Math.round(1000000 * location.getLatitude());
        longitude = (int)Math.round(1000000 * location.getLongitude());
      }
      else
      {
        // Saint-Hilaire (centre du monde) par defaut, et demande de localisation
        if (LocationService.isLocationEnabled(getApplicationContext()))
        {
          doLocationAtStartup = true;
        }
        latitude = 45307316;
        longitude = 5887016;
      }
    }
    mapView.getController().setCenter(new GeoPoint(latitude, longitude));

    // Zoom
    final int intentZoom = intent.getIntExtra(EXTRA_ZOOM, Integer.MAX_VALUE);
    final int zoom;
    if (intentZoom != Integer.MAX_VALUE)
    {
      zoom = intentZoom;
    }
    else
    {
      zoom = sharedPreferences.getInt(AbstractBalisesPreferencesActivity.CONFIG_ZOOM, 9);
    }
    mapView.getController().setZoom(zoom);
    SpotDrawable.onZoomLevelChanged(mapView.getController().getZoom());

    // Balise
    startBaliseProviderId = intent.getStringExtra(EXTRA_BALISE_PROVIDER_ID);
    startBaliseId = intent.getStringExtra(EXTRA_BALISE_ID);
    if (!Utils.isStringVide(startBaliseId) && !Utils.isStringVide(startBaliseProviderId))
    {
      startBaliseDone = false;
    }

    // Type de carte
    currentTileProviderKey = sharedPreferences.getString(AbstractBalisesPreferencesActivity.CONFIG_TILE_PROVIDER_KEY, BuiltInTileProvider.OSM_MAPNIK.getKey());
    changeTileProvider();

    // Fin du "mouvement"
    mapView.getController().onMoveInputFinished();
  }

  /**
   * 
   * @return
   */
  String getIgnApiKey()
  {
    /* Debug ?
    if (ActivityCommons.isDebug(getApplicationContext()))
    {
      return IGN_DEV_API_KEY;
    }*/

    // Clef perso de l'utilisateur
    final String personalIgnApiKey = getPersonalIgnApiKey();
    if (personalIgnApiKey != null)
    {
      return personalIgnApiKey;
    }

    return getProdIgnApiKey();
  }

  /**
   * 
   * @return
   */
  String getPersonalIgnApiKey()
  {
    final boolean useOwn = sharedPreferences.getBoolean(resources.getString(R.string.config_map_ign_use_own_key_key), Boolean.parseBoolean(resources.getString(R.string.config_map_ign_use_own_key_default)));
    if (!useOwn)
    {
      return null;
    }

    return sharedPreferences.getString(resources.getString(R.string.config_map_ign_own_key_key), "");
  }

  /**
   * 
   * @return
   */
  protected abstract String getProdIgnApiKey();

  /**
   * 
   * @author pedro.m
   */
  protected static class BalisesLocationListener implements LocationListener
  {
    private AbstractBalisesMapActivity mapActivity;
    private final Point                oldCenterPoint = new Point();
    private final Point                newCenterPoint = new Point();

    /**
     * 
     * @param mapActivity
     */
    protected BalisesLocationListener(final AbstractBalisesMapActivity mapActivity)
    {
      this.mapActivity = mapActivity;
    }

    /**
     * 
     * @param location
     */
    private void changeLocation(final Location location)
    {
      // Mise a jour de la position de la carte
      final int latitude = (int)Math.round(1000000 * location.getLatitude());
      final int longitude = (int)Math.round(1000000 * location.getLongitude());
      final GeoPoint point = new GeoPoint(latitude, longitude);

      // Calcul de la distance en pixels entre les centres
      mapActivity.mapView.getProjection().toPixels(mapActivity.mapView.getController().getCenter(), oldCenterPoint);
      mapActivity.mapView.getProjection().toPixels(point, newCenterPoint);
      final int deltaX = newCenterPoint.x - oldCenterPoint.x;
      final int deltaY = newCenterPoint.y - oldCenterPoint.y;
      final double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

      // Reglage du centre, en animant ou non suivant la distance (en pixels)
      if (distance < 5 * Math.max(mapActivity.mapView.getPixelWidth(), mapActivity.mapView.getPixelHeight()))
      {
        mapActivity.mapView.getController().animateTo(point);
      }
      else
      {
        mapActivity.mapView.getController().setCenter(point);
      }

      // Point sur la carte
      if (!mapActivity.doLocationAtStartup)
      {
        // MAJ de la precision, par defaut, pas d'imprecision (GPS notamment)
        mapActivity.locationOverlay.setPrecision(-1);

        // Pour le fournisseur par reseau, imprecision geree
        if (LocationManager.NETWORK_PROVIDER.equals(location.getProvider()))
        {
          mapActivity.locationOverlay.setPrecision(location.getAccuracy());
        }

        // MAJ du marqueur de position
        mapActivity.locationOverlay.setPosition(point);
      }
      else
      {
        mapActivity.doLocationAtStartup = false;
      }
    }

    @Override
    public void onLocationChanged(final Location location)
    {
      // Mise a jour de la position de la carte
      changeLocation(location);

      // On cache la boite de dialogue de progression
      ActivityCommons.cancelProgressDialog(ActivityCommons.PROGRESS_DIALOG_LOCATION);
    }

    @Override
    public void onStatusChanged(final String provider, final int status, final Bundle extras)
    {
      //Nothing
    }

    @Override
    public void onProviderEnabled(final String provider)
    {
      //Nothing
    }

    @Override
    public void onProviderDisabled(final String provider)
    {
      //Nothing
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Divers
      mapActivity = null;
    }
  }

  /**
   * 
   */
  private void initLocation()
  {
    // Listener
    locationListener = new BalisesLocationListener(this);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ProvidersServiceConnection implements ServiceConnection
  {
    // WeakReference pour ne pas avoir a mettre a null en sortie
    final WeakReference<AbstractBalisesMapActivity> mapActivity;

    /**
     * 
     * @param mapActivity
     */
    ProvidersServiceConnection(final AbstractBalisesMapActivity mapActivity)
    {
      this.mapActivity = new WeakReference<AbstractBalisesMapActivity>(mapActivity);
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder inBinder)
    {
      // Recuperation du service
      final IProvidersService innerProvidersService = ((ProvidersServiceBinder)inBinder).getService();
      mapActivity.get().providersService = innerProvidersService;
      mapActivity.get().onServiceBinded();

      // Deja fini ?
      if (mapActivity.get().isFinishing())
      {
        return;
      }

      // Initialisations diverses necessitant que la carte soit initialisee
      // => tache en arriere plan
      final AsyncTask<Object, Void, Void> mapTask = new AsyncTask<Object, Void, Void>()
      {
        @Override
        protected Void doInBackground(final Object... params)
        {
          mapActivity.get().waitForMapInitialized();
          if (!isCancelled())
          {
            mapActivity.get().onServiceConnectedAndMapInitialized();
          }
          return null;
        }

        @Override
        protected void onPostExecute(final Void param)
        {
          // Custom sur le Thread UI
          mapActivity.get().onServiceConnected();
        }
      };
      mapActivity.get().executeAsyncTask(mapTask);
    }

    @Override
    public void onServiceDisconnected(final ComponentName name)
    {
      privateOnServiceDisconnected();
    }

    /**
     * 
     * @param name
     */
    void privateOnServiceDisconnected()
    {
      // Il peut arriver que cette methode soit appelee (via onDestroy()) avant onServiceConnected !
      if (mapActivity.get().providersService != null)
      {
        // Listeners
        mapActivity.get().providersService.removeBaliseProviderListener(mapActivity.get(), true);
        mapActivity.get().providersService.removeSpotProviderListener(mapActivity.get());
        mapActivity.get().providersService.removeWebcamProviderListener(mapActivity.get());
      }
    }
  }

  /**
   * 
   */
  private void initProvidersService()
  {
    // Creation connection
    providersServiceConnection = new ProvidersServiceConnection(this);

    // Connexion au service (et demarrage si besoin)
    final Intent providersServiceIntent = new Intent(getApplicationContext(), getProvidersServiceClass());
    providersServiceIntent.putExtra(AbstractProvidersService.STARTED_FROM_ACTIVITY, true);
    startService(providersServiceIntent);
    bindService(providersServiceIntent, providersServiceConnection, Context.BIND_AUTO_CREATE + Context.BIND_NOT_FOREGROUND);
  }

  /**
   * 
   * @return
   */
  protected abstract Class<? extends AbstractProvidersService> getProvidersServiceClass();

  /**
   * 
   */
  protected void onServiceBinded()
  {
    //Nothing
  }

  /**
   * 
   */
  protected void onServiceConnected()
  {
    // Localisation demandee
    if (doLocationAtStartup)
    {
      providersService.getLocationService().requestSingleLocation(locationListener, false, true);
    }
  }

  @Override
  protected void onStart()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onStart()");
    super.onStart();

    // Affichage des nouveautes de la version
    doAfterMapInitialized(new Runnable()
    {
      @Override
      public void run()
      {
        ActivityCommons.displayStartupMessage(AbstractBalisesMapActivity.this, mapView.getTileCache().getCurrentSize());
      }
    });

    // Affichage du message FFVL
    final boolean showFfvlAtStartup = sharedPreferences.getBoolean(resources.getString(R.string.config_ffvl_message_key), Boolean.parseBoolean(resources.getString(R.string.config_ffvl_message_default)));
    if (!preferencesLaunched && showFfvlAtStartup)
    {
      ActivityCommons.checkForFfvlMessage(this, AbstractProvidersService.FFVL_KEY, false, false);
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onStart()");
  }

  @Override
  protected void onResume()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onResume()");
    super.onResume();

    // Wakelock
    ActivityCommons.acquireWakeLock();

    // A l'ecoute de la SDCard
    refreshCacheAvailability();

    // Acquisition GPS si retour des parametres GPS (et GPS active)
    if (ActivityCommons.locationSettingsLaunched)
    {
      ActivityCommons.locationSettingsLaunched = false;
      onLocationSettingsReturn();
    }

    // MAJ des spots
    if ((providersService != null) && !firstOnResume)
    {
      updateSpotProviders(false);
    }

    // MAJ de la taille de cache
    doAfterMapInitialized(new Runnable()
    {
      @Override
      public void run()
      {
        if (mapView != null)
        {
          final int maximumSizeMega = sharedPreferences.getInt(resources.getString(R.string.config_advanced_map_cache_size_key), Integer.parseInt(resources.getString(R.string.config_advanced_map_cache_size_default), 10));
          final long maximumSizeBytes = maximumSizeMega * 1024 * 1024;
          mapView.getTileCache().setMaximumSize(maximumSizeBytes);
        }
      }
    });

    // Retour des preferences
    if (preferencesLaunched)
    {
      preferencesLaunched = false;
      ActivityCommons.updateUnitPreferences(getApplicationContext(), null, null, null, null);
      itemsOverlay.updatePreferences(getApplicationContext(), sharedPreferences);
    }

    // Flag
    firstOnResume = false;

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onResume()");
  }

  /**
   * 
   */
  protected void onLocationSettingsReturn()
  {
    final boolean useGps = sharedPreferences.getBoolean(resources.getString(R.string.config_map_use_gps_key), Boolean.parseBoolean(resources.getString(R.string.config_map_use_gps_default)));
    ActivityCommons.startLocation(this, providersService, locationListener, useGps, useGps, true, ActivityCommons.PROGRESS_DIALOG_LOCATION, true);
  }

  /**
   * 
   */
  void updateSpotProviders(final boolean force)
  {
    // Pour etre sur de ne lancer qu'une seule fois en meme temps
    synchronized (updateSpotProvidersLock)
    {
      if (updateSpotProvidersInProgress)
      {
        return;
      }

      updateSpotProvidersInProgress = true;
    }

    if (force || providersService.haveSpotProvidersChanged())
    {
      final int dialogId = (force ? 0 : ActivityCommons.PROGRESS_DIALOG_SPOTS);
      final BaliseAndSpotProvidersUpdateAsyncTask task = new BaliseAndSpotProvidersUpdateAsyncTask(this, false, dialogId);
      executeAsyncTask(task);
    }
    else
    {
      updateSpotProvidersInProgress = false;
    }
  }

  @Override
  protected void onPause()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onPause()");
    super.onPause();

    // Wakelock
    ActivityCommons.releaseWakeLock();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onPause()");
  }

  @Override
  protected void onStop()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onStop()");
    super.onStop();

    // MAJ de la config
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    if (saveCenterOnQuit && (currentTileProviderKey != null))
    {
      // Le test sur la nullite de currentTileProviderKey permet de ne pas sauver des preferences par defaut lorsque
      // l'appli n'a pas ete initialisee completement (pour cause de rotations repetees de l'ecran notamment)
      editor.putInt(AbstractBalisesPreferencesActivity.CONFIG_ZOOM, mapView.getController().getZoom());
      editor.putInt(AbstractBalisesPreferencesActivity.CONFIG_CENTER_LAT, mapView.getController().getCenter().getLatitudeE6());
      editor.putInt(AbstractBalisesPreferencesActivity.CONFIG_CENTER_LON, mapView.getController().getCenter().getLongitudeE6());
      editor.putString(AbstractBalisesPreferencesActivity.CONFIG_TILE_PROVIDER_KEY, currentTileProviderKey);
    }
    ActivityCommons.commitPreferences(editor);

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onStop()");
  }

  @Override
  protected void onDestroy()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onDestroy()");

    // Fin de taches de fond
    cancelAsyncTasks(true);

    // Fermeture des dialogues
    ActivityCommons.closeDialogs();

    // Notification au service
    providersServiceConnection.privateOnServiceDisconnected();

    // Localisation
    ActivityCommons.endLocation(providersService, locationListener);
    locationListener.onShutdown();
    locationListener = null;

    // Analytics
    ActivityCommons.trackEvent(providersService, AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_STOP, null);

    // Deconnexion et arret du service
    unbindService(providersServiceConnection);

    // A l'ecoute du positionnement d'item
    unregisterItemListener();

    // A l'ecoute de la SDCard
    unregisterSDCardListener();

    // Gestion de la touche back
    ActivityCommons.finishBackKeyManager();

    // Liberation des overlays
    itemsOverlay = null;
    locationOverlay = null;
    messageOverlay = null;

    // Helper tuiles
    tileProviderHelper.shutdown();

    // Handlers
    manageBalisesProchesHandler.onShutdown();
    manageBalisesProchesHandler = null;
    actionBarHideHandler.onShutdown();
    actionBarHideHandler = null;

    // Super ! Pour liberer la MapView
    super.onDestroy();

    // Liberation vues
    ActivityCommons.unbindDrawables(findViewById(android.R.id.content));
    mapView = null;

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onDestroy()");
  }

  /**
   * 
   * @param spotErrors
   */
  void showSpotErrors(final Map<String, IOException> spotErrors)
  {
    if ((spotErrors != null) && (spotErrors.size() > 0))
    {
      // Initialisations
      final StringBuilder message = new StringBuilder(128);
      boolean first = true;

      // En-tete
      message.append(resources.getString(R.string.message_spot_error_start));

      // Pour chaque erreur
      for (final String key : spotErrors.keySet())
      {
        if (!first)
        {
          message.append(Strings.CHAR_NEWLINE);
        }
        final String providerKey = SpotProviderUtils.getSpotProviderKey(key);
        final String providerName = SpotProviderUtils.getSpotProviderName(providerKey, resources);
        final String countryKey = SpotProviderUtils.getSpotProviderCountryKey(key);
        final String providerCountryName = SpotProviderUtils.getCountryName(countryKey);
        message.append(resources.getString(R.string.message_spot_error, providerName, providerCountryName));
        first = false;
      }

      // Pied
      message.append(resources.getString(R.string.message_spot_error_end));

      // Boite de dialogue
      ActivityCommons.alertDialog(this, ActivityCommons.ALERT_DIALOG_SPOT_ERROR, -1, resources.getString(R.string.message_spot_error_title), message.toString(), null, true, null, 0);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu)
  {
    // Initialisations
    final MenuInflater inflater = getMenuInflater();

    if (isHoneyComb)
    {
      // Creation
      inflater.inflate(R.menu.menu_map_action_bar, menu);

      // Sauvegarde
      actionBarMenu = menu;
    }
    else
    {
      // Creation
      inflater.inflate(R.menu.menu_map, menu);
    }

    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(final Menu menu)
  {
    // On grise le menu d'ajout des balises a un label si les balises ne sont pas affichees
    final MenuItem addItem = menu.findItem(R.id.item_map_add_to_label);
    addItem.setEnabled(currentBaliseWindDatasDisplayed || currentBaliseWeatherDatasDisplayed);

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item)
  {
    // Plus d'options
    if (item.getItemId() == R.id.item_map_more)
    {
      // Annulation de l'effacement
      actionBarHideHandler.removeMessages();

      return true;
    }

    // Effacement de la barre d'action qq soit l'item selectionne
    if (isHoneyComb)
    {
      hideActionBar();
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
  {
    switch (contextMenuItemType)
    {
      case BALISE:
        onCreateBaliseContextMenu(menu, view, menuInfo);
        break;
      case SPOT:
        onCreateSpotContextMenu(menu, view, menuInfo);
        break;
      case WEBCAM:
        onCreateWebcamContextMenu(menu, view, menuInfo);
        break;
      case LISTE_ITEMS:
      case LISTE_ITEMS_LONG:
        onCreateListeItemsContextMenu(menu, view, menuInfo);
        break;
    }
  }

  /**
   * 
   * @param menu
   * @param view
   * @param menuInfo
   */
  @SuppressWarnings("unused")
  protected void onCreateBaliseContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
  {
    // Initialisations
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_context_balise, menu);

    // Titre de la boite de menu
    if (contextMenuBaliseProvider.getBaliseById(contextMenuIdBalise) != null)
    {
      menu.setHeaderTitle(MessageFormat.format(FORMAT_CONTEXT_MENU_TITLE, contextMenuBaliseProvider.getBaliseById(contextMenuIdBalise).nom, contextMenuBaliseProvider.getName()));
    }

    // Lien detail
    if (contextMenuBaliseProvider.getBaliseDetailUrl(contextMenuIdBalise) == null)
    {
      final MenuItem detailItem = menu.findItem(R.id.item_context_balise_lien_detail_web);
      detailItem.setEnabled(false);
    }

    // Lien historique
    if (contextMenuBaliseProvider.getBaliseHistoriqueUrl(contextMenuIdBalise) == null)
    {
      final MenuItem historiqueItem = menu.findItem(R.id.item_context_balise_lien_historique_web);
      historiqueItem.setEnabled(false);
    }
  }

  /**
   * 
   * @param menu
   * @param view
   * @param menuInfo
   */
  @SuppressWarnings("unused")
  private void onCreateSpotContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
  {
    // Initialisations
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_context_spot, menu);

    // Recuperation du provider
    final String providerKey = SpotProviderUtils.getSpotProviderKey(contextMenuSpotItem.providerKey);
    final SpotProvider contextMenuSpotProvider = providersService.getSpotProvider(providerKey);

    // Titre de la boite de menu
    if (contextMenuSpotItem.spotName != null)
    {
      menu.setHeaderTitle(contextMenuSpotItem.spotName);
    }

    // Lien infos
    if (!contextMenuSpotItem.spotHasInfos)
    {
      final MenuItem infosItem = menu.findItem(R.id.item_context_spot_infos);
      infosItem.setEnabled(false);
    }

    // Lien detail web
    final String detailLink = contextMenuSpotProvider.getLienDetail(contextMenuSpotItem.spotId, contextMenuSpotItem.spotOtherInfos);
    if ((detailLink == null) || (detailLink.trim().length() == 0))
    {
      final MenuItem detailWebItem = menu.findItem(R.id.item_context_spot_lien_detail);
      detailWebItem.setEnabled(false);
    }
  }

  /**
   * 
   * @param menu
   * @param view
   * @param menuInfo
   */
  @SuppressWarnings("unused")
  private void onCreateWebcamContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
  {
    // Initialisations
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_context_webcam, menu);

    // Titre de la boite de menu
    menu.setHeaderTitle(contextMenuWebcamItem.row.nom);

    // Lien image
    final MenuItem imageItem = menu.findItem(R.id.item_context_webcam_image);
    imageItem.setEnabled(!Utils.isStringVide(contextMenuWebcamItem.row.urlImage));

    // Lien web
    final MenuItem webItem = menu.findItem(R.id.item_context_webcam_web);
    webItem.setEnabled(!Utils.isStringVide(contextMenuWebcamItem.row.urlPage));
  }

  @Override
  public boolean onContextItemSelected(final MenuItem menuItem)
  {
    // Est-ce un item du menu groupe d'items ?
    if (menuItem.getGroupId() == ITEMS_MENU_GROUP_ID)
    {
      final OverlayItem<Canvas> item = contextMenuTapItems.get(menuItem.getItemId() - 1);
      if (contextMenuItemType == ItemType.LISTE_ITEMS)
      {
        itemsOverlay.onTap(item);
        itemsOverlay.requestRedraw();
      }
      else if (contextMenuItemType == ItemType.LISTE_ITEMS_LONG)
      {
        // On passe par un handler avec un petit délai pour laisser le temps au menu contextuel de groupe d'items de se fermer
        // (sinon le nouveau menu contextuel ne s'affiche pas)
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
          @Override
          public void run()
          {
            itemsOverlay.onLongTap(item);
          }
        }, 100);
      }
      return true;
    }

    return super.onContextItemSelected(menuItem);
  }

  /**
   * 
   * @param dialog
   * @param spotItem
   */
  void populateInfosSpotDialog(final Dialog dialog, final SpotItem spotItem)
  {
    // Recuperation du provider
    final String providerKey = SpotProviderUtils.getSpotProviderKey(spotItem.providerKey);
    final String countryKey = SpotProviderUtils.getSpotProviderCountryKey(spotItem.providerKey);
    final SpotProvider contextMenuSpotProvider = providersService.getSpotProvider(providerKey);

    // Recuperation du spot
    List<Spot> spots = null;
    try
    {
      spots = providersService.getSpots(providerKey, countryKey);
    }
    catch (final IOException ioe)
    {
      // Remontee de l'exception
      exceptionHandler.traceException(Thread.currentThread(), ioe, false);
    }
    final Spot contextMenuSpot = SpotProviderUtils.findSpot(spots, spotItem.spotId);

    // Definition des indices
    final int[] indices = new int[contextMenuSpot.infos.size()];
    int i = 0;
    int indice = 0;
    for (final String key : contextMenuSpotProvider.getInfoKeys())
    {
      if (contextMenuSpot.infos.get(key) != null)
      {
        indices[indice] = i;
        indice++;
      }

      // Next
      i++;
    }

    // Recuperation des titres des infos
    final String infosIdName = SpotProviderUtils.getInfosIdName(providerKey);
    final int infosId = resources.getIdentifier(infosIdName, Strings.RESOURCES_ARRAY, getPackageName());
    final String[] infosTitles = resources.getStringArray(infosId);

    // Titre
    dialog.setTitle(contextMenuSpot.nom);

    // Vue
    final Gallery gallery = (Gallery)dialog.findViewById(R.id.infos_spot_gallery);
    gallery.setMinimumWidth(getWindowManager().getDefaultDisplay().getWidth());
    gallery.setMinimumHeight(getWindowManager().getDefaultDisplay().getHeight());
    gallery.setAdapter(new BaseAdapter()
    {
      @Override
      public int getCount()
      {
        return contextMenuSpot.infos.size();
      }

      @Override
      public Object getItem(final int index)
      {
        return Integer.valueOf(index);
      }

      @Override
      public long getItemId(final int index)
      {
        return index;
      }

      @Override
      public View getView(final int index, final View view, final ViewGroup viewGroup)
      {
        // Initialisations
        final LayoutParams layoutParams = new Gallery.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        final int padding = 10;

        // Layout
        final LinearLayout layout = new LinearLayout(AbstractBalisesMapActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(layoutParams);

        // Titre
        final TextView titleView = new TextView(AbstractBalisesMapActivity.this);
        final String flecheGauche = (index == 0 ? Strings.VIDE : "<  ");
        final String flecheDroite = (index == indices.length - 1 ? Strings.VIDE : "  >");
        titleView.setText(flecheGauche + infosTitles[indices[index]] + flecheDroite);
        titleView.setLayoutParams(layoutParams);
        titleView.setPadding(padding, padding, padding, 0);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(resources.getColor(android.R.color.primary_text_light));
        titleView.setBackgroundColor(resources.getColor(android.R.color.background_light));
        layout.addView(titleView);

        // Texte
        final TextView tv = new TextView(AbstractBalisesMapActivity.this);
        tv.setLayoutParams(layoutParams);
        tv.setAutoLinkMask(Linkify.WEB_URLS + Linkify.EMAIL_ADDRESSES);
        tv.setTextColor(resources.getColor(android.R.color.primary_text_light));
        tv.setBackgroundColor(resources.getColor(android.R.color.background_light));
        final String key = contextMenuSpotProvider.getInfoKeys()[indices[index]];
        tv.setText(Html.fromHtml(translateInfos(spotItem.providerKey, contextMenuSpot.infos.get(key))));

        // Scroll
        final ScrollView sv = new ScrollView(AbstractBalisesMapActivity.this);
        sv.setPadding(padding, padding, padding, padding);
        sv.setLayoutParams(layoutParams);
        sv.setVerticalScrollBarEnabled(true);
        sv.setHorizontalScrollBarEnabled(false);
        sv.setBackgroundColor(resources.getColor(android.R.color.background_light));
        sv.addView(tv);
        layout.addView(sv);

        return layout;
      }
    });
  }

  /**
   * 
   * @param providerKey
   * @param sourceInfos
   * @return
   */
  String translateInfos(final String providerKey, final String sourceInfos)
  {
    // Initialisations
    final String providerSimpleKey = SpotProviderUtils.getSpotProviderKey(providerKey).replace('.', '_');
    final String packageName = getPackageName();
    String target = sourceInfos;

    final List<String> infosKeys = getInfosKeywords(sourceInfos);
    for (final String infosKey : infosKeys)
    {
      final String resKey = providerSimpleKey + "_infosKey_" + infosKey;
      final int resId = resources.getIdentifier(resKey, Strings.RESOURCES_STRING, packageName);
      if (resId > 0)
      {
        final String res = resources.getString(resId);
        target = target.replaceAll("\\{" + infosKey + "\\}", res);
      }
    }

    // Caracteres speciaux
    target = target.replaceAll("\n", "<br/>");

    return target;
  }

  /**
   * 
   * @param source
   * @return
   */
  private static List<String> getInfosKeywords(final String source)
  {
    // Initialisations
    final List<String> keywords = new ArrayList<String>();

    int begin = source.indexOf('{');
    while (begin >= 0)
    {
      final int end = source.indexOf('}', begin + 1);
      if (end > 0)
      {
        keywords.add(source.substring(begin + 1, end));
      }
      else
      {
        break;
      }

      // Next
      begin = source.indexOf('{', begin + 1);
    }

    return keywords;
  }

  /**
   * 
   * @author pedro.m
   */
  private static final class InfosSpotDialogHandler extends Handler
  {
    private final WeakReference<Dialog> infosSpotDialog;

    /**
     * 
     * @param infosSpotDialog
     */
    InfosSpotDialogHandler(final Dialog infosSpotDialog)
    {
      super(Looper.getMainLooper());
      this.infosSpotDialog = new WeakReference<Dialog>(infosSpotDialog);
    }

    @Override
    public void handleMessage(final Message msg)
    {
      // Effacement du dialogue de progression
      ActivityCommons.cancelProgressDialog(ActivityCommons.PROGRESS_DIALOG_SPOT_INFOS);

      // Affichage du dialogue d'infos
      infosSpotDialog.get().show();
    }
  }

  /**
   * 
   * @param spotItem
   */
  protected void infosSpot(final SpotItem spotItem)
  {
    // Statistiques
    ActivityCommons.trackEvent(providersService, AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_SPOT_INFORMATIONS_CLICKED, spotItem.getName());

    // Creation et affichage du dialogue d'attente
    ActivityCommons.progressDialog(this, ActivityCommons.PROGRESS_DIALOG_SPOT_INFOS, true, false, null);

    // Creation du dialogue d'infos
    final Dialog infosSpotDialog = new Dialog(this);
    infosSpotDialog.setContentView(R.layout.infos_spot);

    // Handler de gestion des dialogues
    final Handler handler = new InfosSpotDialogHandler(infosSpotDialog);

    // Population en arriere plan
    final Timer timer = new Timer();
    timer.schedule(new TimerTask()
    {
      @Override
      public void run()
      {
        // Recuperation des infos
        populateInfosSpotDialog(infosSpotDialog, spotItem);

        // Gestion de l'affichage
        handler.sendEmptyMessage(0);
      }
    }, 0);
  }

  @Override
  public boolean onKeyDown(final int keyCode, final KeyEvent event)
  {
    // Gestion de la touche "back" quand l'ActionBar est visible
    if (isHoneyComb)
    {
      if ((keyCode == KeyEvent.KEYCODE_BACK) && getActionBar().isShowing())
      {
        hideActionBar();
        return true;
      }
    }

    // Gestion de la touche "back"
    if (backKeyDouble && (keyCode == KeyEvent.KEYCODE_BACK))
    {
      // Sortie de l'appli geree sur "keyUp" sur la touche back
      ActivityCommons.manageDoubleBackKeyDown();
      return true;
    }

    // Gestion de la touche "menu"
    if (isHoneyComb && (keyCode == KeyEvent.KEYCODE_MENU))
    {
      if (getActionBar().isShowing())
      {
        hideActionBar();
      }
      else
      {
        showActionBar(false);
      }

      return true;
    }

    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(final int keyCode, final KeyEvent event)
  {
    // Gestion de la touche "back"
    if ((keyCode == KeyEvent.KEYCODE_BACK) && (mapView != null))
    {
      // Gestion des couches
      try
      {
        for (final Overlay<?, ?> overlay : mapView.getOverlays(false))
        {
          if (overlay.onKeyPressed(keyCode))
          {
            ActivityCommons.hideBackKeyToast();
            return true;
          }
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        mapView.unlockReadOverlays();
      }

      // Si on ne gere pas le double appui
      if (!backKeyDouble)
      {
        return super.onKeyUp(keyCode, event);
      }

      // Gestion du timer pour quitter
      ActivityCommons.manageDoubleBackKeyUp(this);

      return true;
    }

    return super.onKeyUp(keyCode, event);
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences inSharedPreferences, final String key)
  {
    // Centrage
    if (resources.getString(R.string.config_map_centering_key).equals(key))
    {
      // Valeur par defaut
      boolean value = Boolean.parseBoolean(resources.getString(R.string.config_map_centering_default));

      // Pour gerer les anciennes versions pour lesquelles c'etait un String
      try
      {
        value = inSharedPreferences.getBoolean(key, value);
      }
      catch (final ClassCastException cce)
      {
        // On vient certainement d'une version precedente avec cette propriete qui etait de type String => conversion en Boolean
        final Editor editor = inSharedPreferences.edit();
        editor.putBoolean(key, value);
        ActivityCommons.commitPreferences(editor);
      }
      finally
      {
        InfosDrawable.setMAP_CENTERING(value);
      }
    }

    // Rapports d'erreur
    else if (resources.getString(R.string.config_error_reports_key).equals(key))
    {
      final boolean checked = inSharedPreferences.getBoolean(key, Boolean.parseBoolean(resources.getString(R.string.config_error_reports_default)));
      exceptionHandler.setSendReport(checked);
    }

    // WakeLock
    else if (resources.getString(R.string.config_wakelock_key).equals(key) || resources.getString(R.string.config_wakelock_flight_mode_key).equals(key) || resources.getString(R.string.config_wakelockbright_key).equals(key))
    {
      ActivityCommons.manageWakeLockConfig(getApplicationContext(), isFlightMode());
    }

    // Double back
    else if (resources.getString(R.string.config_double_backkey_key).equals(key))
    {
      backKeyDouble = inSharedPreferences.getBoolean(key, Boolean.parseBoolean(resources.getString(R.string.config_double_backkey_default)));
    }

    // Niveau de zoom pour les orientations des sites
    else if (resources.getString(R.string.config_data_sites_orientations_zoom_level_key).equals(key))
    {
      SpotDrawable.setVISU_ORIENTATIONS_MIN_ZOOM(inSharedPreferences.getInt(key, Integer.parseInt(resources.getString(R.string.config_data_sites_orientations_zoom_level_default), 10)));
    }

    // Balise wind data flag
    else if (AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES.equals(key))
    {
      currentBaliseWindDatasDisplayed = inSharedPreferences.getBoolean(key, AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES_DEFAULT);
    }

    // Balise weather data flag
    else if (AbstractBalisesPreferencesActivity.CONFIG_DATA_WEATHER_BALISES.equals(key))
    {
      currentBaliseWeatherDatasDisplayed = inSharedPreferences.getBoolean(key, Boolean.parseBoolean(resources.getString(R.string.config_map_layers_weather_default)));
    }

    // Sites data flag
    else if (AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES.equals(key))
    {
      currentSiteDatasDisplayed = inSharedPreferences.getBoolean(key, AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES_DEFAULT);
    }

    // Webcams data flag
    else if (AbstractBalisesPreferencesActivity.CONFIG_DATA_WEBCAMS.equals(key))
    {
      currentWebcamDatasDisplayed = inSharedPreferences.getBoolean(key, AbstractBalisesPreferencesActivity.CONFIG_DATA_WEBCAMS_DEFAULT);
    }
  }

  /**
   * 
   * @return
   */
  @SuppressWarnings("static-method")
  protected boolean isFlightMode()
  {
    return false;
  }

  /**
   * 
   */
  protected void location()
  {
    final boolean useGps = sharedPreferences.getBoolean(resources.getString(R.string.config_map_use_gps_key), Boolean.parseBoolean(resources.getString(R.string.config_map_use_gps_default)));
    if (!ActivityCommons.startLocation(this, providersService, locationListener, useGps, useGps, true, ActivityCommons.PROGRESS_DIALOG_LOCATION, true))
    {
      // GPS et GSM non actifs, question pour redirection vers les parametres de localisation
      ActivityCommons.locationSettingsDialog(this, null);
    }
  }

  /**
   * 
   * @param index
   * @return
   */
  private String getCustomTileProviderName(final int index)
  {
    return tileProviderHelper.getCustomTileProviderName(index);
  }

  /**
   * 
   * @param index
   * @return
   */
  String getCustomTileProviderKey(final int index)
  {
    return tileProviderHelper.getCustomTileProviderKey(index);
  }

  /**
   * 
   * @return
   */
  private int getCustomTileProvidersCount()
  {
    return tileProviderHelper.getCustomTileProvidersCount();
  }

  /**
   * 
   * @param force
   */
  void refreshCustomTileProvidersConfiguration(final boolean force)
  {
    try
    {
      final boolean sdcardAvailable = CUSTOM_MAPS_FILE.exists() && CUSTOM_MAPS_FILE.isFile() && CUSTOM_MAPS_FILE.canRead();
      if (sdcardAvailable)
      {
        Log.d(getClass().getSimpleName(), "Reading custom maps configuration from " + CUSTOM_MAPS_FILE.toURL());
        final Map<String, String> properties = tileProviderHelper.readConfiguration(CUSTOM_MAPS_FILE.toURL());

        // Sauvegarde en local
        TileProviderHelper.saveConfiguration(openFileOutput(CUSTOM_MAPS_KEY, Context.MODE_PRIVATE), properties);
      }
      else
      {
        Log.d(getClass().getSimpleName(), "Reading custom maps configuration from context");
        tileProviderHelper.readConfiguration(getFileStreamPath(CUSTOM_MAPS_KEY).toURL());
      }

      // Rafraichissement du provider
      if (force)
      {
        changeTileProvider();
      }
    }
    catch (final IOException ioe)
    {
      Log.w(getClass().getSimpleName(), ioe.getMessage());
    }
  }

  /**
   * 
   */
  protected void mapType()
  {
    // Initialisations
    final int standardTileProvidersCount = BuiltInTileProvider.values().length;
    final int customTileProvidersCount = getCustomTileProvidersCount();
    final String[] items = new String[standardTileProvidersCount + customTileProvidersCount];
    int selectedItem = -1;

    // Recherche des noms des cartes et du type de carte actuellement selectionne
    for (int i = 0; i < items.length; i++)
    {
      // Providers standard
      if (i < standardTileProvidersCount)
      {
        items[i] = resources.getStringArray(R.array.menu_maptype_names)[i];
        if (BuiltInTileProvider.values()[i].getKey().equals(currentTileProviderKey))
        {
          selectedItem = i;
        }
      }
      // Providers Custom
      else
      {
        final String customTileProviderKey = getCustomTileProviderKey(i - standardTileProvidersCount);
        items[i] = getCustomTileProviderName(i - standardTileProvidersCount);
        if (customTileProviderKey.equals(currentTileProviderKey))
        {
          selectedItem = i;
        }
      }
    }

    // Liste de choix
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(resources.getString(R.string.menu_maptype));
    builder.setSingleChoiceItems(items, selectedItem, new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int item)
      {
        // Initialisations
        boolean ok = true;
        final String tileProviderKey;
        if ((item >= standardTileProvidersCount) && (customTileProvidersCount > 0))
        {
          tileProviderKey = getCustomTileProviderKey(item - standardTileProvidersCount);
        }
        else
        {
          tileProviderKey = BuiltInTileProvider.values()[item].getKey();
        }

        // Si IGN : indispo en version FULL
        if (IGNSatelliteTileProvider.KEY.equals(tileProviderKey) || IGNMapTileProvider.KEY.equals(tileProviderKey))
        {
          final String customIgnKey = getPersonalIgnApiKey();
          final boolean customOk = (customIgnKey == null) || !Utils.isStringVide(customIgnKey);
          ok = (getIgnApiKey() != null) && customOk;
          if (!ok)
          {
            dialog.dismiss();
            final String message = resources.getString(!customOk ? R.string.message_ign_key_invalid : R.string.message_ign_unavailable);
            ActivityCommons.alertDialog(AbstractBalisesMapActivity.this, ActivityCommons.ALERT_DIALOG_IGN, R.drawable.icon, resources.getString(R.string.app_name), message, null, true, null, Linkify.ALL);
          }
        }

        // Si ok
        if (ok)
        {
          dialog.dismiss();
          currentTileProviderKey = tileProviderKey;
          changeTileProvider();
        }
      }
    });

    // Affichage dialog
    final AlertDialog alertDialog = builder.create();
    alertDialog.show();
    ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_MAP_TYPE, alertDialog, null);
  }

  /**
   * 
   * @param baliseLayerChanged
   * @param webcamLayerChanged
   */
  void onMapLayersChanged(final boolean baliseLayerChanged, final boolean webcamLayerChanged)
  {
    // Notification des preferences
    BaliseDrawable.updatePreferences(sharedPreferences, AbstractBalisesMapActivity.this.getApplicationContext());

    // Notification du service pour les balises
    if (baliseLayerChanged)
    {
      if (currentBaliseWindDatasDisplayed || currentBaliseWeatherDatasDisplayed)
      {
        providersService.addBaliseProviderListener(AbstractBalisesMapActivity.this, false);
      }
      else
      {
        providersService.removeBaliseProviderListener(AbstractBalisesMapActivity.this, false);
        removeBaliseProviders();
      }
      providersService.notifyNeededBaliseProvidersChanged(false);
    }

    // Webcams
    if (webcamLayerChanged)
    {
      if (currentWebcamDatasDisplayed)
      {
        providersService.addWebcamProviderListener(AbstractBalisesMapActivity.this);
      }
      else
      {
        providersService.removeWebcamProviderListener(AbstractBalisesMapActivity.this);
      }
    }

    // Forcage du redessin
    itemsOverlay.onLayersChanged(currentBaliseWindDatasDisplayed || currentBaliseWeatherDatasDisplayed, currentSiteDatasDisplayed, currentWebcamDatasDisplayed);
    itemsOverlay.requestRedraw();

    // MAJ des balises et spots en tache de fond
    updateBaliseAndSpotProviders();
  }

  /**
   * 
   */
  protected final void mapLayers()
  {
    // Initialisations
    final String[] noms = resources.getStringArray(R.array.menu_mapdatas_names);
    final String[] items = new String[noms.length];
    final boolean[] checkedItems = new boolean[noms.length];

    // Noms
    items[0] = noms[0];
    items[1] = noms[1];
    items[2] = noms[2];
    items[3] = noms[3];

    // Cochages
    checkedItems[0] = currentBaliseWindDatasDisplayed;
    checkedItems[1] = currentBaliseWeatherDatasDisplayed;
    checkedItems[2] = currentSiteDatasDisplayed;
    checkedItems[3] = currentWebcamDatasDisplayed;

    // Liste de choix
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(resources.getString(R.string.menu_mapdatas));
    builder.setCancelable(true);
    builder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int item, final boolean checked)
      {
        // Port pour action sur click
        if (onMapLayerClicked((AlertDialog)dialog, item, checked))
        {
          checkedItems[item] = checked;
        }
      }
    });

    // Bouton OK
    builder.setPositiveButton(resources.getString(R.string.button_ok), new OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int wich)
      {
        // Verification du changement ou pas
        final boolean baliseLayersChanged = (currentBaliseWindDatasDisplayed != checkedItems[0]) || (currentBaliseWeatherDatasDisplayed != checkedItems[1]);
        final boolean baliseLayerChanged = (currentBaliseWindDatasDisplayed || currentBaliseWeatherDatasDisplayed) != (checkedItems[0] || checkedItems[1]);
        final boolean webcamLayerChanged = (currentWebcamDatasDisplayed != checkedItems[3]);
        if (!baliseLayersChanged && (currentSiteDatasDisplayed == checkedItems[2]) && !webcamLayerChanged)
        {
          // Pas de changement => on quitte sans rien faire
          dialog.dismiss();
          return;
        }

        // Sauvegarde
        currentBaliseWindDatasDisplayed = checkedItems[0];
        currentBaliseWeatherDatasDisplayed = checkedItems[1];
        currentSiteDatasDisplayed = checkedItems[2];
        currentWebcamDatasDisplayed = checkedItems[3];

        // Sauvegarde des preferences
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES, currentBaliseWindDatasDisplayed);
        editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WEATHER_BALISES, currentBaliseWeatherDatasDisplayed);
        editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES, currentSiteDatasDisplayed);
        editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WEBCAMS, currentWebcamDatasDisplayed);
        ActivityCommons.commitPreferences(editor);

        // Notifications
        onMapLayersChanged(baliseLayerChanged, webcamLayerChanged);
        if (webcamLayerChanged)
        {
          providersService.updateWebcamProviders();
        }

        // Fermeture dialogue
        dialog.dismiss();
      }
    });

    // Bouton pour la config des sources de donnees balises
    builder.setNeutralButton(resources.getString(R.string.button_balises_sources), new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int which)
      {
        // Rien, sert juste a ne pas passer null (car dans ce cas ca ne fonctionne pas sur certaines versions (en 1.6 L8 par exemple))
      }
    });

    // Bouton pour la config des sources de donnees sites
    builder.setNegativeButton(resources.getString(R.string.button_sites_sources), new DialogInterface.OnClickListener()
    {
      @Override
      public void onClick(final DialogInterface dialog, final int which)
      {
        // Rien, sert juste a ne pas passer null (car dans ce cas ca ne fonctionne pas sur certaines versions (en 1.6 L8 par exemple))
      }
    });

    // Dialogue
    final AlertDialog alertDialog = builder.create();
    alertDialog.show();
    ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_MAP_LAYERS, alertDialog, null);

    // Bouton pour la config des sources de donnees balises
    final Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
    final long tileCacheSize = mapView.getTileCache().getCurrentSize();
    neutralButton.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(final View view)
      {
        preferencesLaunched = true;
        ActivityCommons.preferences(AbstractBalisesMapActivity.this, tileCacheSize, mapView.getController().getZoom(), AbstractBalisesPreferencesActivity.INTENT_MODE_BALISES_SOURCES);
      }
    });

    // Bouton pour la config des sources de donnees sites
    final Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
    negativeButton.setOnClickListener(new View.OnClickListener()
    {
      @Override
      public void onClick(final View view)
      {
        preferencesLaunched = true;
        ActivityCommons.preferences(AbstractBalisesMapActivity.this, tileCacheSize, mapView.getController().getZoom(), AbstractBalisesPreferencesActivity.INTENT_MODE_SITES_SOURCES);
      }
    });

    // Precochage des options
    final ListView listView = alertDialog.getListView();
    listView.setItemChecked(0, currentBaliseWindDatasDisplayed);
    listView.setItemChecked(1, currentBaliseWeatherDatasDisplayed);
    listView.setItemChecked(2, currentSiteDatasDisplayed);
    listView.setItemChecked(3, currentWebcamDatasDisplayed);
    listView.invalidate();
  }

  /**
   * 
   */
  void updateBaliseAndSpotProviders()
  {
    final BaliseAndSpotProvidersUpdateAsyncTask task = new BaliseAndSpotProvidersUpdateAsyncTask(this, true, ActivityCommons.PROGRESS_DIALOG_MAP_LAYERS);
    executeAsyncTask(task);
  }

  /**
   * 
   * @param dialog
   * @param item
   * @param checked
   */
  @SuppressWarnings("unused")
  protected boolean onMapLayerClicked(final AlertDialog dialog, final int item, final boolean checked)
  {
    if ((item == 2) && checked)
    {
      final int spotProvidersCount = SpotProviderUtils.countSpotProvidersChecked(getApplicationContext(), resources, sharedPreferences);
      if (spotProvidersCount <= 0)
      {
        Toast.makeText(getApplicationContext(), resources.getString(R.string.message_no_spot_provider_checked), Toast.LENGTH_LONG).show();
      }
    }

    return true;
  }

  /**
   * 
   */
  protected void dataInfos()
  {
    ActivityCommons.dataInfos(this, providersService);
  }

  /**
   * 
   */
  protected void changeTileProvider()
  {
    try
    {
      // Providers standard
      for (final BuiltInTileProvider tileProvider : BuiltInTileProvider.values())
      {
        if (tileProvider.getKey().equals(currentTileProviderKey))
        {
          mapView.setTileProvider(tileProvider);
          return;
        }
      }

      // Providers custom
      final TileProvider tileProvider = tileProviderHelper.getTileProvider(currentTileProviderKey, mapView.getTileProvidersParams());
      mapView.setTileProvider(tileProvider);
    }
    catch (final IOException ioe)
    {
      Log.e(getClass().getSimpleName(), ioe.getMessage(), ioe);
      throw new RuntimeException(ioe);
    }
  }

  /**
   * 
   */
  protected void preferences()
  {
    // Initialisations
    preferencesLaunched = true;

    // Lancement des preferences
    ActivityCommons.preferences(this, mapView.getTileCache().getCurrentSize(), mapView.getController().getZoom(), AbstractBalisesPreferencesActivity.INTENT_MODE_NORMAL);
  }

  /**
   * 
   */
  protected void lienDetailBalise()
  {
    if (contextMenuBaliseProvider.getBaliseById(contextMenuIdBalise) != null)
    {
      // Statistiques
      ActivityCommons.trackEvent(providersService, AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_BALISE_WEB_DETAIL_CLICKED, contextMenuBaliseProviderKey + "." + contextMenuIdBalise);

      // Ouverture du lien
      ActivityCommons.goToUrl(this, contextMenuBaliseProvider.getBaliseDetailUrl(contextMenuIdBalise));
    }
  }

  /**
   * 
   */
  protected void lienHistoriqueBalise()
  {
    if (contextMenuBaliseProvider.getBaliseById(contextMenuIdBalise) != null)
    {
      // Statistiques
      ActivityCommons.trackEvent(providersService, AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_BALISE_WEB_HISTO_CLICKED, contextMenuBaliseProviderKey + "." + contextMenuIdBalise);

      // Ouverture du lien
      ActivityCommons.goToUrl(this, contextMenuBaliseProvider.getBaliseHistoriqueUrl(contextMenuIdBalise));
    }
  }

  /**
   * 
   * @return
   */
  protected void initGraphics()
  {
    // Gestion de la densite de pixels de l'ecran
    final Context context = getApplicationContext();
    final DisplayMetrics metrics = ActivityCommons.getDisplayMetrics(context);
    scalingFactor = metrics.density;

    // Initialisations
    BaliseDrawableHelper.initialize(context);
    BaliseDrawable.initGraphics(context);
    SpotDrawable.initGraphics(context);
    WebcamDrawable.initGraphics(context);
  }

  /**
   * 
   * @param key
   * @param provider
   */
  private void addBaliseProvider(final String key, final BaliseProvider provider)
  {
    // Ajout du provider
    itemsOverlay.addBaliseProvider(provider, key);

    // Listener longTap
    itemsOverlay.addBaliseLongTapListener(this);

    // Suppression des items existants
    itemsOverlay.clearBalises(key);

    // Ajout d'un item pour chaque balise
    addBalisesToOverlay(provider, key);

    // Repaint + validation
    itemsOverlay.requestRedraw();
  }

  /**
   * 
   * @param provider
   * @param providerKey
   */
  private void addBalisesToOverlay(final BaliseProvider provider, final String providerKey)
  {
    // Liste des balises
    final Collection<Balise> balises = provider.getBalises();
    final List<Balise> balisesToAdd = new ArrayList<Balise>();

    // Pour chaque balise
    final boolean providerMatches = providerKey.equals(startBaliseProviderId);
    for (final Balise balise : balises)
    {
      if ((balise.latitude != Integer.MIN_VALUE) && (balise.longitude != Integer.MIN_VALUE))
      {
        // Balise à ajouter
        balisesToAdd.add(balise);

        // Balise donnee au demarrage : on affiche son detail (comme si on avait clique dessus)
        if (!startBaliseDone && providerMatches && balise.id.equals(startBaliseId))
        {
          final GeoPoint point = new GeoPoint(balise.latitude, balise.longitude);
          mapView.getController().setCenter(point);
          mapView.getController().setZoom(10);
          final BaliseItem baliseItem = new BaliseItem(providerKey, balise.id, point, null);
          itemsOverlay.onTap(baliseItem);
          startBaliseDone = true;
        }
      }
    }

    // Ajout en bloc
    itemsOverlay.addBalises(providerKey, balisesToAdd);

    // Si balise de depart non trouvee et que c'est le provider courant...
    if (!startBaliseDone && providerMatches)
    {
      // ... on considere qu'il ne faut plus chercher
      startBaliseDone = true;
    }
  }

  /**
   * 
   * @param key
   * @param infos
   */
  private void removeBaliseProvider(final String key, final BaliseProviderInfos infos)
  {
    // Gestion du message
    messageOverlay.removeProviderName(key, infos.getName(), null, null, false);
    repaint();

    // Retrait du provider de la couche
    itemsOverlay.removeBaliseProvider(key);

    // Repaint + validation
    itemsOverlay.requestRedraw();
  }

  /**
   * 
   */
  void removeBaliseProviders()
  {
    // Retrait de tous les providers de balises
    itemsOverlay.removeBaliseProviders();

    // Repaint + validation
    itemsOverlay.requestRedraw();
  }

  /**
   * 
   */
  private void repaint()
  {
    mapView.postInvalidate();
  }

  @Override
  public void onBalisesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    if (standardMode)
    {
      // Initialisations
      Log.d(getClass().getSimpleName(), ">>> onBalisesUpdate(" + key + ") : " + provider.getBalises().size());

      // Suppression des items existants
      itemsOverlay.clearBalises(key);

      // Recreation des items
      addBalisesToOverlay(provider, key);

      // Rafraichissement
      itemsOverlay.requestRedraw();
    }
  }

  @Override
  public void onRelevesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    // MAJ des balises
    /* *** Fait dans onBaliseProviderUpdateEnded
    ItemsOverlay.invalidateBalisesDrawables(key);
    ItemsOverlay.requestRedrawStatic();
    ItemsOverlay.validateBalisesDrawables();
    */
  }

  @Override
  public void onBaliseProviderAdded(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    // Log
    Log.d(getClass().getSimpleName(), ">>> onBaliseProviderAdded(" + key + ", ...)");

    // Ajout du provider
    if (standardMode)
    {
      addBaliseProvider(key, provider);
    }

    // Log
    Log.d(getClass().getSimpleName(), "<<< onBaliseProviderAdded(" + key + ", ...)");
  }

  @Override
  public void onBaliseProviderRemoved(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes)
  {
    // Retrait du provider
    if (wasStandardMode)
    {
      removeBaliseProvider(key, infos);
    }
  }

  @Override
  public void onBaliseProviderUpdateStarted(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
  {
    if (standardMode)
    {
      // Affichage du message
      messageOverlay.addProviderName(infos.getName());
      repaint();
    }
  }

  @Override
  public void onBaliseProviderUpdateEnded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
  {
    if (standardMode)
    {
      // Affichage du message
      messageOverlay.removeProviderName(key, infos.getName(), infos.getBalisesException(), infos.getRelevesException(), infos.isPaused());
      repaint();

      // MAJ des balises
      itemsOverlay.invalidateBalisesDrawables(key);
      itemsOverlay.requestRedraw();
    }
  }

  @Override
  public void onBaliseProvidersChanged()
  {
    // Gestion des balises proches
    callManageBalisesProches(0);
  }

  @Override
  public void onBaliseProviderModesAdded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardModeAdded, final List<BaliseProviderMode> addedModes,
      final List<BaliseProviderMode> oldActiveModes)
  {
    if (standardModeAdded)
    {
      onBaliseProviderAdded(key, provider, true, infos.getActiveModes());
    }
  }

  @Override
  public void onBaliseProviderModesRemoved(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardModeRemoved, final List<BaliseProviderMode> removedModes,
      final List<BaliseProviderMode> oldActiveModes)
  {
    if (standardModeRemoved)
    {
      onBaliseProviderRemoved(key, provider, infos, true, oldActiveModes);
    }
  }

  @Override
  public void onBaliseTap(final BaliseProvider provider, final String providerKey, final String idBalise)
  {
    // On cache le message de confirmation de sortie
    ActivityCommons.hideBackKeyToast();
  }

  @Override
  public void onBaliseLongTap(final BaliseProvider provider, final String providerKey, final String idBalise)
  {
    // Initialisations
    contextMenuItemType = ItemType.BALISE;
    contextMenuBaliseProvider = provider;
    contextMenuBaliseProviderKey = providerKey;
    contextMenuIdBalise = idBalise;

    // Affichage du menu
    mapView.showContextMenu();
  }

  /**
   * 
   */
  protected void baliseTooltip()
  {
    itemsOverlay.showTooltip(contextMenuBaliseProviderKey, contextMenuIdBalise);
    itemsOverlay.requestRedraw();
  }

  @Override
  public void onCacheNotAvailable(final String cachePath)
  {
    Toast.makeText(getApplicationContext(), String.format(resources.getString(R.string.message_cache_not_available), cachePath), Toast.LENGTH_SHORT).show();
  }

  /**
   * 
   * @param key
   * @param countryKey
   * @param spots
   */
  @SuppressWarnings("unused")
  private void addSpotProvider(final String key, final String countryKey, final List<Spot> spots)
  {
    // Listener longTap
    itemsOverlay.addSpotLongTapListener(this);

    // Ajout d'un item pour chaque balise
    addSpotsToOverlay(countryKey, spots);

    // Repaint + validation
    itemsOverlay.requestRedraw();
  }

  /**
   * 
   * @param countryKey
   * @param spots
   */
  private void addSpotsToOverlay(final String countryKey, final List<Spot> spots)
  {
    // Initialisations
    final List<Spot> spotsToAdd = new ArrayList<Spot>();

    // Pour chaque spot
    for (final Spot spot : spots)
    {
      if ((spot.latitude != null) && (spot.longitude != null))
      {
        spotsToAdd.add(spot);
      }
    }

    // Ajout en bloc
    itemsOverlay.addSpots(countryKey, spotsToAdd);
  }

  /**
   * 
   * @param countryKey
   */
  private void removeSpotProvider(final String countryKey)
  {
    // Retrait du provider de la couche
    itemsOverlay.removeSpotProvider(countryKey);

    // Repaint + validation
    itemsOverlay.requestRedraw();
  }

  @Override
  public void onSpotProviderAdded(final SpotProvider provider, final String key, final String country, final String countryKey, final List<Spot> spots, final long updateTs)
  {
    // Ajout du provider
    addSpotProvider(key, countryKey, spots);
  }

  @Override
  public void onSpotProviderRemoved(final SpotProvider provider, final String key, final String country, final String countryKey)
  {
    // Retrait du provider
    removeSpotProvider(countryKey);
  }

  @Override
  public void onSpotTap(final SpotItem spotItem)
  {
    // On cache le message de confirmation de sortie
    ActivityCommons.hideBackKeyToast();
  }

  @Override
  public void onSpotLongTap(final SpotItem spotItem)
  {
    // Initialisations
    contextMenuItemType = ItemType.SPOT;
    contextMenuSpotItem = spotItem;

    // Affichage du menu
    mapView.showContextMenu();
  }

  @Override
  public void onSpotInfoLinkTap(final SpotItem spotItem)
  {
    infosSpot(spotItem);
  }

  /**
   * 
   */
  protected void lienDetailSpot()
  {
    // Statistiques
    ActivityCommons.trackEvent(providersService, AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_SPOT_WEB_DETAIL_CLICKED, contextMenuSpotItem.getName());

    // Recuperation du provider
    final String providerKey = SpotProviderUtils.getSpotProviderKey(contextMenuSpotItem.providerKey);
    final SpotProvider contextMenuSpotProvider = providersService.getSpotProvider(providerKey);

    // Ouverture du lien
    final String detailLink = contextMenuSpotProvider.getLienDetail(contextMenuSpotItem.spotId, contextMenuSpotItem.spotOtherInfos);
    ActivityCommons.goToUrl(this, detailLink);
  }

  /**
   * 
   */
  protected void navigateToSpot()
  {
    // Statistiques
    ActivityCommons.trackEvent(providersService, AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_SPOT_NAVIGATE_TO_CLICKED, contextMenuSpotItem.getName());

    // Naivgation
    final Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setData(Uri.parse("geo:" + contextMenuSpotItem.getPoint().getLatitude() + "," + contextMenuSpotItem.getPoint().getLongitude() + "?q=" + contextMenuSpotItem.getPoint().getLatitude() + ","
        + contextMenuSpotItem.getPoint().getLongitude() + "(" + contextMenuSpotItem.spotName + ")"));
    startActivity(intent);
  }

  @Override
  public void onCenterChanged(final GeoPoint oldCenter, final GeoPoint newCenter)
  {
    //Nothing
  }

  @Override
  public void onZoomChanged(final int oldZoom, final int newZoom)
  {
    SpotDrawable.onZoomLevelChanged(newZoom);
  }

  @Override
  public boolean isBaliseProviderNeeded(final String key, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    // Le Service n'a lance que les Threads pour les providers actifs
    // => la reponse est forcement "vrai" (enfin si les balises sont activees pour la carte)
    // (on ne cherche PAS a ne mettre a jour que les providers qui ont des balises visibles a l'ecran)
    return standardMode && (currentBaliseWindDatasDisplayed || currentBaliseWeatherDatasDisplayed);
  }

  /**
   * 
   * @param delay
   */
  protected void callManageBalisesProches(final long delay)
  {
    manageBalisesProchesHandler.removeMessages(0);
    manageBalisesProchesHandler.sendEmptyMessageDelayed(0, delay);
  }

  @Override
  public boolean isGraphicsLightMode()
  {
    final SharedPreferences sharedPrefs = ActivityCommons.getSharedPreferences(getApplicationContext());
    return sharedPrefs.getBoolean(getResources().getString(R.string.config_advanced_light_mode_key), Boolean.parseBoolean(getResources().getString(R.string.config_advanced_light_mode_default)));
  }

  @Override
  public void onSingleTap(final boolean consumedByOverlays)
  {
    if (isHoneyComb)
    {
      if (getActionBar().isShowing())
      {
        hideActionBar();
      }
      //TODO PMU 201610 else if (!consumedByOverlays && !hasPermanentMenuKey)
      else if (!consumedByOverlays)
      {
        showActionBar(true);
      }
    }
  }

  /**
   * 
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  void hideActionBar()
  {
    getActionBar().hide();
  }

  /**
   * 
   */
  protected void hideActionBarDelayed()
  {
    actionBarHideHandler.removeMessages(HANDLER_ACTION_BAR_HIDER);
    actionBarHideHandler.sendEmptyMessageDelayed(HANDLER_ACTION_BAR_HIDER, ACTION_BAR_HIDE_TIMEOUT);
  }

  /**
   * 
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  void showActionBar(final boolean hideDelayed)
  {
    onPrepareOptionsMenu(actionBarMenu);
    getActionBar().show();
    if (hideDelayed)
    {
      hideActionBarDelayed();
    }
  }

  /**
   * 
   */
  void waitForMapInitialized()
  {
    synchronized (mapInitLock)
    {
      try
      {
        while (!mapInitialized)
        {
          mapInitLock.wait();
        }
      }
      catch (final InterruptedException ie)
      {
        Log.w(getClass().getSimpleName(), ie);
      }
    }
  }

  /**
   * 
   * @param runnable
   */
  protected void doAfterMapInitialized(final Runnable runnable)
  {
    final AsyncTask<Object, Void, Void> task = new AsyncTask<Object, Void, Void>()
    {
      @Override
      protected Void doInBackground(final Object... args)
      {
        waitForMapInitialized();
        if (!isCancelled())
        {
          runnable.run();
        }
        return null;
      }
    };
    executeAsyncTask(task);
  }

  /**
   * 
   * @param task
   * @param args
   */
  protected void executeAsyncTask(final AsyncTask<Object, ?, ?> task, final Object... args)
  {
    synchronized (asyncTasks)
    {
      asyncTasks.add(task);
      task.execute(args);
    }
  }

  /**
   * 
   * @param mayInterruptIfRunning
   */
  protected void cancelAsyncTasks(final boolean mayInterruptIfRunning)
  {
    synchronized (asyncTasks)
    {
      for (final AsyncTask<?, ?, ?> task : asyncTasks)
      {
        task.cancel(mayInterruptIfRunning);
      }
      asyncTasks.clear();
    }
  }

  /**
   * 
   */
  void onServiceConnectedAndMapInitialized()
  {
    // Fin ?
    if (isFinishing())
    {
      return;
    }

    // Tuiles perso
    refreshCustomTileProvidersConfiguration(false);

    // Analytics
    itemsOverlay.setAnalyticsService(providersService.getAnalyticsService());

    // Listeners
    providersService.addSpotProviderListener(this);
    if (currentBaliseWindDatasDisplayed || currentBaliseWeatherDatasDisplayed)
    {
      providersService.addBaliseProviderListener(this, false);
      providersService.notifyNeededBaliseProvidersChanged(false);
    }
    if (currentWebcamDatasDisplayed)
    {
      providersService.addWebcamProviderListener(this);
    }

    // Spot providers
    providersService.setForceDownloadSpotProviders(true);
    updateSpotProviders(true);

    // Google Analytics
    ActivityCommons.trackEvent(providersService, AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_START, null);
  }

  @Override
  public void onNewIntent(final Intent intent)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onNewIntent()");
    super.onNewIntent(intent);

    final String baliseProviderId = intent.getStringExtra(EXTRA_BALISE_PROVIDER_ID);
    final String baliseId = intent.getStringExtra(EXTRA_BALISE_ID);
    if ((baliseProviderId != null) && (baliseId != null))
    {
      final Intent innerIntent = new Intent(AbstractBalisesMapActivity.ACTION_ITEM);
      innerIntent.putExtra(AbstractBalisesMapActivity.ACTION_ITEM_TYPE, Integer.valueOf(SearchDatabaseHelper.ITEM_TYPE_BALISE, 10));
      innerIntent.putExtra(AbstractBalisesMapActivity.ACTION_ITEM_PROVIDER, baliseProviderId);
      innerIntent.putExtra(AbstractBalisesMapActivity.ACTION_ITEM_ID, baliseId);
      sendBroadcast(innerIntent);
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onNewIntent()");
  }

  /**
   * 
   */
  protected void search()
  {
    startSearch(AbstractSearchActivity.getLastQuery(), false, null, false);
  }

  @Override
  public void onItemsTap(final List<OverlayItem<Canvas>> tapItems)
  {
    // Initialisations
    contextMenuItemType = ItemType.LISTE_ITEMS;
    contextMenuTapItems = tapItems;

    // Affichage du menu
    mapView.showContextMenu();
  }

  @Override
  public void onItemsLongTap(final List<OverlayItem<Canvas>> tapItems)
  {
    // Initialisations
    contextMenuItemType = ItemType.LISTE_ITEMS_LONG;
    contextMenuTapItems = tapItems;

    // Affichage du menu
    mapView.showContextMenu();
  }

  /**
   * 
   * @param menu
   * @param view
   * @param menuInfo
   */
  @SuppressWarnings("unused")
  protected void onCreateListeItemsContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
  {
    // Initialisations
    menu.clear();

    // Pour chaque item
    int i = 1;
    for (final OverlayItem<Canvas> item : contextMenuTapItems)
    {
      final int itemTypeId;
      final String itemName;
      if (BaliseItem.class.isAssignableFrom(item.getClass()))
      {
        itemTypeId = R.string.menu_context_map_liste_items_type_balise;
        final BaliseItem baliseItem = (BaliseItem)item;
        final BaliseProvider provider = providersService.getBaliseProvider(baliseItem.providerKey);
        if (provider != null)
        {
          itemName = MessageFormat.format(FORMAT_CONTEXT_MENU_TITLE, provider.getBaliseById(baliseItem.baliseId).nom, provider.getName());
        }
        else
        {
          itemName = MessageFormat.format(FORMAT_CONTEXT_MENU_TITLE, baliseItem.getName(), baliseItem.providerKey);
        }
      }
      else if (SpotItem.class.isAssignableFrom(item.getClass()))
      {
        itemTypeId = R.string.menu_context_map_liste_items_type_spot;
        final SpotItem spotItem = (SpotItem)item;
        itemName = spotItem.spotName;
      }
      else if (WebcamItem.class.isAssignableFrom(item.getClass()))
      {
        itemTypeId = R.string.menu_context_map_liste_items_type_webcam;
        final WebcamItem webcamItem = (WebcamItem)item;
        itemName = webcamItem.row.nom;
      }
      else
      {
        itemTypeId = R.string.menu_context_map_liste_items_type_unknown;
        itemName = item.getName();
      }

      // MenuItem
      final String itemType = getResources().getString(itemTypeId);
      final String itemTitle = MessageFormat.format(FORMAT_CONTEXT_MENU_ITEMS_TITLE, itemType, itemName);
      menu.add(ITEMS_MENU_GROUP_ID, i, i, itemTitle);

      // Next
      i++;
    }
  }

  @Override
  public void onWebcamTap(final WebcamItem webcamItem)
  {
    // On cache le message de confirmation de sortie
    ActivityCommons.hideBackKeyToast();
  }

  @Override
  public void onWebcamLongTap(final WebcamItem webcamItem)
  {
    // Initialisations
    contextMenuItemType = ItemType.WEBCAM;
    contextMenuWebcamItem = webcamItem;

    // Affichage du menu
    mapView.showContextMenu();
  }

  @Override
  public void onWebcamInfoLinkTap(final WebcamItem webcamItem)
  {
    if ((webcamItem.row != null) && !Utils.isStringVide(webcamItem.row.id))
    {
      final String url = "http://www.mobibalises.net/webcams/index.php?id=" + webcamItem.row.id;
      ActivityCommons.goToUrl(this, url);
    }
  }

  @Override
  public void onWebcamImageTap(final WebcamItem webcamItem)
  {
    if ((webcamItem.row != null) && !Utils.isStringVide(webcamItem.row.provider) && !Utils.isStringVide(webcamItem.row.id))
    {
      final File file = ItemsOverlay.WebcamDownloadThread.getWebcamCacheFile(webcamItem.row.provider, webcamItem.row.id);
      if ((file != null) && file.exists() && file.isFile() && file.canRead())
      {
        final Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "image/*");
        startActivity(intent);
      }
    }
  }

  @Override
  public void onWebcamsUpdate(final String key)
  {
    itemsOverlay.onWebcamsUpdate(key);
  }
}
