package org.pedro.android.mobibalises_common.service;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.analytics.AnalyticsService;
import org.pedro.android.mobibalises_common.location.LocationService;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.provider.BaliseAndroidCache;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.provider.SpotProviderUtils;
import org.pedro.android.mobibalises_common.search.SearchDatabaseHelper;
import org.pedro.android.mobibalises_common.webcam.WebcamDatabaseHelper;
import org.pedro.android.webcams.JSONAbleWebcam;
import org.pedro.android.webcams.MobibalisesWebcamProvider;
import org.pedro.balises.AbstractBaliseProvider;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseCache;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.BalisesCorrector;
import org.pedro.balises.BidonProvider;
import org.pedro.balises.CachedProvider;
import org.pedro.balises.Utils;
import org.pedro.balises.ffvl.FfvlMobibalisesProvider;
import org.pedro.balises.ffvl.FfvlProvider;
import org.pedro.balises.metar.OgimetMetarProvider;
import org.pedro.balises.pioupiou.PioupiouProvider;
import org.pedro.balises.romma.XmlMobibalisesRommaProvider;
import org.pedro.balises.romma.XmlRommaProvider;
import org.pedro.balises.synop.OgimetSynopProvider;
import org.pedro.saveable.Saveable;
import org.pedro.spots.Spot;
import org.pedro.spots.SpotProvider;
import org.pedro.spots.dhv.DhvSpotProvider;
import org.pedro.spots.ffvl.FfvlSpotProvider;
import org.pedro.spots.pge.PgeAfriqueSpotProvider;
import org.pedro.spots.pge.PgeAmeriqueNordSpotProvider;
import org.pedro.spots.pge.PgeAmeriqueSudSpotProvider;
import org.pedro.spots.pge.PgeAsieSpotProvider;
import org.pedro.spots.pge.PgeEuropeEstSpotProvider;
import org.pedro.spots.pge.PgeEuropeOuestSpotProvider;
import org.pedro.spots.pge.PgeMoyenOrientSpotProvider;
import org.pedro.spots.pge.PgeOceanieSpotProvider;
import org.pedro.utils.FileTimestampUtils;
import org.pedro.utils.ThreadUtils;
import org.pedro.webcams.Webcam;
import org.pedro.webcams.WebcamProvider;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractProvidersService extends Service implements IProvidersService
{
  // Constantes
  private static final String                  INTENT_FILE_DATA_SCHEME                   = "file";
  public static final String                   STARTED_FROM_ACTIVITY                     = "mobibalises.providersService.startedFromActivity";
  protected static final String                BROADCAST_EXTRA_CLIENT                    = "client";
  private static final String                  BROADCAST_STOP                            = "mobibalises.stop";
  private static final String                  BROADCAST_BALISES_UPDATE_BALISES          = "balises";
  private static final String                  BROADCAST_BALISES_UPDATE_KEY              = "key";
  private static final String                  BROADCAST_BALISES_UPDATE                  = "mobibalises.balisesUpdate";
  private static final String                  BROADCAST_RELEVES_UPDATE_RELEVES          = "releves";
  private static final String                  BROADCAST_RELEVES_UPDATE_KEY              = "key";
  private static final String                  BROADCAST_RELEVES_UPDATE                  = "mobibalises.relevesUpdate";

  private static final String                  FORMAT_INTERNATIONAL_BALISE_PROVIDER_NAME = "{0}.{1}";

  private File                                 spotsPath;

  private static final long                    ADJUSTABLE_SLEEP_PERIOD_MILLIS            = 1 * 60000;
  private static final long                    MINIMUM_SLEEP_PERIOD_MILLIS               = 30 * 1000;

  public static final String                   FFVL_KEY                                  = "4D6F626942616C69736573";
  private static final String                  FFVL_CORRECTOR_URL                        = "http://data.mobibalises.net/data/ffvl.properties";

  private static final String                  ROMMA_KEY                                 = "f37e12e32e189a412f4";

  private static final String                  MOBIBALISES_WEBCAMS_KEY                   = "7kQtUPSlOoXOTjOKtvn89g6lhvBYfinAtI2sxxO2ddc=";

  private ProvidersServiceBinder               binder;

  protected Resources                          resources;
  protected SharedPreferences                  sharedPreferences;
  boolean                                      debugMode;

  // Balise providers
  private static final String                  BALISE_PROVIDER_THREAD_WAKEUP_ACTION      = "BALISE_PROVIDER_THREAD_WAKEUP_ACTION_";
  protected final List<BaliseProviderListener> baliseProviderListeners                   = new ArrayList<BaliseProviderListener>();
  final Map<String, CachedProvider>            baliseProviders                           = new HashMap<String, CachedProvider>();
  final Object                                 baliseProvidersLock                       = new Object();
  final Map<String, BaliseProviderInfos>       baliseProvidersInfos                      = new HashMap<String, BaliseProviderInfos>();
  private BaliseCache                          baliseCache;

  public enum BaliseProviderMode
  {
    STANDARD, ALARM
  }

  private static final List<BaliseProviderMode>   staticProviderModeStandard            = Arrays.asList(new BaliseProviderMode[] { BaliseProviderMode.STANDARD });
  private static final List<BaliseProviderMode>   staticProviderModeEmpty               = new ArrayList<BaliseProviderMode>();

  // Spots providers
  private final List<SpotProviderListener>        spotProviderListeners                 = new ArrayList<SpotProviderListener>();
  private final Map<String, SpotProvider>         activeSpotProviders                   = new HashMap<String, SpotProvider>();
  private final Map<String, SpotProvider>         spotProviders                         = new HashMap<String, SpotProvider>();
  private boolean                                 forceSpotProvidersUpdate              = false;

  // Webcam providers
  private static final String                     WEBCAM_PROVIDERS_THREAD_WAKEUP_ACTION = "WEBCAM_PROVIDERS_THREAD_WAKEUP_ACTION";
  protected final List<WebcamProviderListener>    webcamProviderListeners               = new ArrayList<WebcamProviderListener>();

  // Thread d'initialisation
  Thread                                          initThread;
  boolean                                         initDone                              = false;
  final Object                                    initLock                              = new Object();

  // Gestion de l'ecran
  private ScreenReceiver                          screenReceiver;
  protected boolean                               screenOff                             = false;

  // Gestion de la localisation
  protected LocationService                       locationService;

  // Gestion du reseau
  private NetworkReceiver                         networkReceiver;
  protected boolean                               networkOff                            = false;

  // Gestion du broadcast
  boolean                                         broadcastDatas                        = false;
  final List<String>                              broadcastClients                      = new ArrayList<String>();
  BroadcasterListener                             broadcasterListener;
  private BaliseProvidersStopReceiver             stopReceiver;

  // Ecoute de la SDCard
  private SDCardBroadcastReceiver                 sdcardReceiver;

  // Analytics
  protected AnalyticsService                      analyticsService;

  // Search engine
  protected SearchEngineThread                    searchEngineThread;

  // Webcams
  WebcamProvidersThread                           webcamProvidersThread;
  private WebcamProvidersThreadReceiver           webcamProvidersThreadReceiver;

  // Threads divers
  final Map<BaliseProviderListener, List<Thread>> notifyThreads                         = new HashMap<BaliseProviderListener, List<Thread>>();
  final List<Thread>                              threads                               = new ArrayList<Thread>();

  /**
   * 
   * @author pedro.m
   */
  private static class SearchEngineThread extends Thread implements BaliseProviderListener, SpotProviderListener
  {
    private AbstractProvidersService     providersService;
    private final SearchDatabaseHelper   searchDbHelper;
    private final List<SearchEngineStep> steps = new ArrayList<SearchEngineStep>();

    /**
     * 
     * @author pedro.m
     */
    private static class SearchEngineStep
    {
      final String             providerKey;
      final Collection<Balise> balises;
      final Collection<Spot>   spots;
      final long               maj;
      final boolean            force;

      /**
       * 
       * @param providerKey
       * @param maj
       * @param balises
       * @param spots
       * @param force
       */
      SearchEngineStep(final String providerKey, final long maj, final Collection<Balise> balises, final Collection<Spot> spots, final boolean force)
      {
        this.providerKey = providerKey;
        this.balises = (balises == null ? null : new ArrayList<Balise>(balises)); // Copie pour eviter les ConcurrentModificationException
        this.spots = (spots == null ? null : new ArrayList<Spot>(spots)); // Copie pour eviter les ConcurrentModificationException
        this.maj = maj;
        this.force = force;
      }

      @Override
      public String toString()
      {
        final int balisesSize = (balises == null ? 0 : balises.size());
        final int spotsSize = (spots == null ? 0 : spots.size());
        return providerKey + "<" + maj + "/" + balisesSize + "/" + spotsSize + "/" + force + ">";
      }
    }

    /**
     * 
     * @param providersService
     */
    SearchEngineThread(final AbstractProvidersService providersService)
    {
      // Super
      super(SearchEngineThread.class.getName());
      this.providersService = providersService;
      this.searchDbHelper = new SearchDatabaseHelper(providersService.getApplicationContext());
    }

    @Override
    public void run()
    {
      // Initialisation
      initialize();

      // Boucle
      while (!isInterrupted())
      {
        synchronized (this)
        {
          if (canWait())
          {
            try
            {
              Log.d(getClass().getSimpleName(), ">>> wait");
              wait();
              Log.d(getClass().getSimpleName(), "<<< wait");
            }
            catch (final InterruptedException e)
            {
              Log.d(getClass().getSimpleName(), ">>> interrupt");
              Thread.currentThread().interrupt();
            }
          }
        }

        // Traitement
        if (!isInterrupted())
        {
          doIt();
        }
      }

      // Fin
      shutdown();

      // Notification de fin accomplie
      synchronized (this)
      {
        notifyAll();
      }
    }

    /**
     * 
     */
    private void doIt()
    {
      // Etape a traiter ?
      final SearchEngineStep step;
      synchronized (steps)
      {
        // Etape
        Log.d(getClass().getSimpleName(), "steps : " + steps);
        if (steps.size() > 0)
        {
          step = steps.remove(0);
        }
        else
        {
          step = null;
        }
      }

      // Traitement etape
      if (step != null)
      {
        // Balises
        if (step.balises != null)
        {
          doBalisesStep(step);
        }

        //Spots
        if (step.spots != null)
        {
          doSpotsStep(step);
        }
      }
    }

    /**
     * 
     * @param step
     */
    private void doSpotsStep(final SearchEngineStep step)
    {
      final SQLiteDatabase database = searchDbHelper.getWritableDatabase();
      try
      {
        final long ts = searchDbHelper.getSpotProviderTimestamp(database, step.providerKey);
        Log.d(getClass().getSimpleName(), "doSpotsStep : " + step + " // tsDb=" + ts);
        if (step.force || (ts == Long.MIN_VALUE) || (step.maj > ts))
        {
          database.beginTransaction();
          try
          {
            searchDbHelper.deleteSpotsForProvider(database, step.providerKey);
            searchDbHelper.insertSpotsForProvider(database, step.providerKey, step.spots, step.maj);
            database.setTransactionSuccessful();
          }
          finally
          {
            database.endTransaction();
          }
        }
      }
      finally
      {
        database.close();
      }
    }

    /**
     * 
     * @param step
     */
    private void doBalisesStep(final SearchEngineStep step)
    {
      final SQLiteDatabase database = searchDbHelper.getWritableDatabase();
      try
      {
        final long ts = searchDbHelper.getBaliseProviderTimestamp(database, step.providerKey);
        Log.d(getClass().getSimpleName(), "doBalisesStep : " + step + " // tsDb=" + ts);
        if (step.force || (ts == Long.MIN_VALUE))
        {
          database.beginTransaction();
          try
          {
            searchDbHelper.deleteBalisesForProvider(database, step.providerKey);
            searchDbHelper.insertBalisesForProvider(database, step.providerKey, step.balises, step.maj);
            database.setTransactionSuccessful();
          }
          finally
          {
            database.endTransaction();
          }
        }
      }
      finally
      {
        database.close();
      }
    }

    /**
     * 
     */
    private void shutdown()
    {
      providersService = null;
    }

    /**
     * 
     */
    private void initialize()
    {
      // Nothing
    }

    /**
     * 
     * @return
     */
    private boolean canWait()
    {
      // Demandes d'enregistrement
      synchronized (steps)
      {
        return (steps.size() == 0);
      }
    }

    /**
     * 
     * @param step
     */
    private void postStep(final SearchEngineStep step)
    {
      synchronized (steps)
      {
        steps.add(step);
      }
      synchronized (this)
      {
        notify();
      }
    }

    @Override
    public void onBalisesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      if (standardMode)
      {
        try
        {
          final SearchEngineStep step = new SearchEngineStep(key, provider.getBalisesUpdateDate(), provider.getBalises(), null, true);
          postStep(step);
        }
        catch (final IOException ioe)
        {
          Log.e(getClass().getSimpleName(), ioe.getMessage(), ioe);
        }
      }
    }

    @Override
    public void onRelevesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      // Nothing
    }

    @Override
    public void onBaliseProviderAdded(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      if (standardMode)
      {
        try
        {
          final SearchEngineStep step = new SearchEngineStep(key, provider.getBalisesUpdateDate(), provider.getBalises(), null, false);
          postStep(step);
        }
        catch (final IOException ioe)
        {
          Log.e(getClass().getSimpleName(), ioe.getMessage(), ioe);
        }
      }
    }

    @Override
    public void onBaliseProviderRemoved(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes)
    {
      // Nothing
    }

    @Override
    public void onBaliseProviderUpdateStarted(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
    {
      // Nothing
    }

    @Override
    public void onBaliseProviderUpdateEnded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
    {
      // Nothing
    }

    @Override
    public boolean isBaliseProviderNeeded(final String key, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      return false;
    }

    @Override
    public void onBaliseProvidersChanged()
    {
      // Nothing
    }

    @Override
    public void onBaliseProviderModesAdded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardModeAdded, final List<BaliseProviderMode> addedModes,
        final List<BaliseProviderMode> oldActiveModes)
    {
      if (standardModeAdded)
      {
        onBaliseProviderAdded(key, provider, true, infos.activeModes);
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
    public void onSpotProviderAdded(final SpotProvider provider, final String key, final String country, final String countryKey, final List<Spot> spots, final long updateTs)
    {
      try
      {
        if (providersService == null)
        {
          return;
        }

        final String siteProvidersBaseKey = providersService.getResources().getString(R.string.site_providers_base_key);
        final String providerKey = countryKey.substring(siteProvidersBaseKey.length() + 1);
        final SearchEngineStep step = new SearchEngineStep(providerKey, updateTs, null, provider.getSpots(country), false);
        postStep(step);
      }
      catch (final IOException ioe)
      {
        Log.e(getClass().getSimpleName(), ioe.getMessage(), ioe);
      }
    }

    @Override
    public void onSpotProviderRemoved(final SpotProvider provider, final String key, final String country, final String countryKey)
    {
      // Nothing
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class SDCardBroadcastReceiver extends BroadcastReceiver
  {
    private IProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    SDCardBroadcastReceiver(final IProvidersService providersService)
    {
      super();
      this.providersService = providersService;
    }

    @Override
    public void onReceive(final Context inContext, final Intent intent)
    {
      providersService.updateBaliseProviders(true);
      providersService.updateWebcamProviders();
    }

    /**
     * 
     */
    void onShutdown()
    {
      providersService = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class BaliseProvidersStopReceiver extends BroadcastReceiver
  {
    private AbstractProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    BaliseProvidersStopReceiver(final AbstractProvidersService providersService)
    {
      super();
      this.providersService = providersService;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      // Initialisation
      final String client = intent.getStringExtra(BROADCAST_EXTRA_CLIENT);
      Log.d(AbstractProvidersService.class.getSimpleName(), "Received stop intent from " + client);

      // Arret
      synchronized (providersService.broadcastClients)
      {
        final boolean removed = providersService.broadcastClients.remove(client);
        Log.d(AbstractProvidersService.class.getSimpleName(), "Broadcast client removed (" + removed + "): " + client + ", remaining " + providersService.broadcastClients.size());
        if (removed && (providersService.broadcastClients.size() == 0))
        {
          Log.d(AbstractBaliseProvider.class.getSimpleName(), "Removing broadcasterListener and trying to stop");
          providersService.removeBaliseProviderListener(providersService.broadcasterListener, true);
          providersService.stopSelfIfPossible();
        }
      }
    }

    /**
     * 
     */
    void onShutdown()
    {
      providersService = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class BroadcasterListener implements BaliseProviderListener
  {
    private AbstractProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    BroadcasterListener(final AbstractProvidersService providersService)
    {
      super();
      this.providersService = providersService;
    }

    @Override
    public void onBalisesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      if (standardMode)
      {
        broadcastBalises(key, provider);
      }
    }

    /**
     * 
     * @param key
     * @param provider
     */
    private void broadcastBalises(final String key, final BaliseProvider provider)
    {
      // Envoi du broadcast si necessaire
      if (providersService.broadcastDatas)
      {
        final Intent intent = new Intent(BROADCAST_BALISES_UPDATE);
        intent.putExtra(BROADCAST_BALISES_UPDATE_KEY, key);
        intent.putExtra(BROADCAST_BALISES_UPDATE_BALISES, provider.getBalises().toArray());
        providersService.sendBroadcast(intent);
        Log.d(AbstractProvidersService.class.getSimpleName(), "balisesUpdate broadcasted for " + key);
      }
    }

    @Override
    public void onRelevesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      if (standardMode)
      {
        broadcastReleves(key, provider);
      }
    }

    /**
     * 
     * @param key
     * @param provider
     */
    private void broadcastReleves(final String key, final BaliseProvider provider)
    {
      // Envoi du broadcast si necessaire
      if (providersService.broadcastDatas)
      {
        final Intent intent = new Intent(BROADCAST_RELEVES_UPDATE);
        intent.putExtra(BROADCAST_RELEVES_UPDATE_KEY, key);
        intent.putExtra(BROADCAST_RELEVES_UPDATE_RELEVES, provider.getReleves().toArray());
        providersService.sendBroadcast(intent);
        Log.d(AbstractProvidersService.class.getSimpleName(), "relevesUpdate broadcasted for " + key);
      }
    }

    @Override
    public void onBaliseProviderAdded(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      if (standardMode)
      {
        broadcastBalises(key, provider);
        broadcastReleves(key, provider);
      }
    }

    @Override
    public void onBaliseProviderRemoved(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes)
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderUpdateStarted(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderUpdateEnded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
    {
      //Nothing
    }

    @Override
    public boolean isBaliseProviderNeeded(final String key, final boolean standardMode, final List<BaliseProviderMode> activeModes)
    {
      return standardMode && providersService.broadcastDatas;
    }

    @Override
    public void onBaliseProvidersChanged()
    {
      //Nothing
    }

    @Override
    public void onBaliseProviderModesAdded(String key, BaliseProvider provider, BaliseProviderInfos infos, boolean standardModeAdded, List<BaliseProviderMode> addedModes, final List<BaliseProviderMode> oldActiveModes)
    {
      if (standardModeAdded)
      {
        onBaliseProviderAdded(key, provider, true, infos.activeModes);
      }
    }

    @Override
    public void onBaliseProviderModesRemoved(String key, BaliseProvider provider, BaliseProviderInfos infos, boolean standardModeRemoved, List<BaliseProviderMode> removedModes, final List<BaliseProviderMode> oldActiveModes)
    {
      // Nothing
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      providersService = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  protected static class BaliseProviderThread extends Thread
  {
    private AbstractProvidersService  providersService;
    private final String              fullKey;
    private final CachedProvider      provider;
    private final BaliseProviderInfos infos;
    private final boolean             relevesAdjustable;
    private final long                sleepPeriod;
    private final Object              sleepLock = new Object();
    private boolean                   sleeping  = false;
    private final String              logTag;

    /**
     * 
     * @param providersService
     * @param fullKey
     * @param provider
     * @param infos
     */
    BaliseProviderThread(final AbstractProvidersService providersService, final String fullKey, final CachedProvider provider, final BaliseProviderInfos infos)
    {
      super(BaliseProviderThread.class.getName() + Strings.CHAR_POINT + fullKey);
      this.providersService = providersService;
      this.fullKey = fullKey;
      this.provider = provider;
      this.infos = infos;
      relevesAdjustable = infos.isAdjustRelevesUpdate();
      sleepPeriod = infos.getRelevesUpdatePeriod();
      this.logTag = "BaliseProviderThread." + fullKey;
    }

    /**
     * 
     * @return
     */
    private boolean isSleeping()
    {
      synchronized (sleepLock)
      {
        return sleeping;
      }
    }

    /**
     * 
     * @param delay
     */
    private void postAlarm(final long delay)
    {
      final AlarmManager alarmManager = (AlarmManager)providersService.getSystemService(Context.ALARM_SERVICE);
      final Intent intent = new Intent(BALISE_PROVIDER_THREAD_WAKEUP_ACTION + fullKey);
      final PendingIntent pendingIntent = PendingIntent.getBroadcast(providersService.getApplicationContext(), 0, intent, 0);
      alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pendingIntent);
    }

    /**
     * 
     * @param delay
     */
    private void gotoSleep(final long delay)
    {
      // Statut
      synchronized (sleepLock)
      {
        sleeping = true;
      }

      // Mise en place d'une alarme
      postAlarm(delay);

      try
      {
        // Tant que le Thread doit dormir
        while (!isInterrupted() && isSleeping())
        {
          synchronized (this)
          {
            Log.d(logTag, "Provider thread is now waiting/sleeping");
            wait();
            Log.d(logTag, "Provider thread is trying to wake up");
          }
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
    }

    /**
     * 
     */
    void wakeUp()
    {
      synchronized (sleepLock)
      {
        // Statut
        sleeping = false;

        // Notification
        synchronized (this)
        {
          notify();
        }
      }
    }

    @Override
    public void run()
    {
      while (!isInterrupted())
      {
        // Besoin du provider ?
        while (!isInterrupted() && providersService.canWait(fullKey, infos))
        {
          try
          {
            // Info
            synchronized (infos.updateFireLock)
            {
              infos.setPaused(true);
            }

            // Attente
            synchronized (this)
            {
              Log.d(logTag, ">>> wait");
              wait();
              Log.d(logTag, "<<< wait");
            }

            // Info
            synchronized (infos.updateFireLock)
            {
              infos.setPaused(false);

              // Le Thread s'est endormi un certain temps => si algo ajuste, il faut considerer qu'il n'est plus ajuste
              if (infos.isAdjustRelevesUpdate())
              {
                infos.setRelevesAdjusted(false);
                infos.setPreviousRelevesUpdateDate(-1);
              }
            }
          }
          catch (final InterruptedException ie)
          {
            Log.d(logTag, ">>> interrupt");
            Thread.currentThread().interrupt();
          }
        }

        if (!isInterrupted())
        {
          // Fin de boucle d'attente
          final long debut = System.currentTimeMillis();

          // MAJ des donnees
          update();

          // Calcul duree endormissement
          final long delta = System.currentTimeMillis() - debut;
          final long sleep;
          if ((infos.getBalisesException() != null) || (infos.getRelevesException() != null) || (relevesAdjustable && !infos.isRelevesAdjusted()))
          {
            sleep = Math.max(MINIMUM_SLEEP_PERIOD_MILLIS, ADJUSTABLE_SLEEP_PERIOD_MILLIS - delta);
          }
          else
          {
            sleep = Math.max(MINIMUM_SLEEP_PERIOD_MILLIS, sleepPeriod - delta);
          }

          // Dodo
          try
          {
            // Statut
            synchronized (infos.updateFireLock)
            {
              infos.setSleeping(true);
              infos.setNextWakeUp(System.currentTimeMillis() + sleep);
            }

            // Dodo
            Log.d(logTag, "Provider thread is now sleeping for " + sleep + "ms (" + (int)Math.floor(sleep / 60000) + "min" + ((sleep / 1000) % 60) + "s)");
            gotoSleep(sleep);
            Log.d(logTag, "Provider thread is waking up");
          }
          finally
          {
            // Statut
            synchronized (infos.updateFireLock)
            {
              infos.setSleeping(false);
              infos.setNextWakeUp(-1);
            }
          }
        }
      }

      // Fin
      providersService = null;
    }

    /**
     * 
     */
    private void update()
    {
      // Signalement
      synchronized (providersService.baliseProvidersLock)
      {
        synchronized (infos.baliseProviderThreadLock)
        {
          synchronized (infos.updateFireLock)
          {
            infos.setUpdateInProgress(true);
            final boolean standardMode = infos.activeModes.contains(BaliseProviderMode.STANDARD);
            providersService.fireBaliseProviderUpdateStarted(fullKey, provider, infos, standardMode);
          }
        }
      }

      // Balises
      final boolean balisesOk = providersService.goBalises(fullKey, provider, infos);

      // Releves (seulement si balises MAJ ou balises deja recuperees)
      if (balisesOk || ((provider.getBalises() != null) && (provider.getBalises().size() > 0)))
      {
        if (relevesAdjustable)
        {
          providersService.goRelevesAdjust(fullKey, provider, infos);
        }
        else
        {
          providersService.goReleves(fullKey, provider, infos);
        }
      }

      // Signalement
      synchronized (providersService.baliseProvidersLock)
      {
        synchronized (infos.baliseProviderThreadLock)
        {
          synchronized (infos.updateFireLock)
          {
            infos.setUpdateInProgress(false);
            final boolean standardMode = infos.activeModes.contains(BaliseProviderMode.STANDARD);
            providersService.fireBaliseProviderUpdateEnded(fullKey, provider, infos, standardMode);
          }
        }
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class WebcamProvidersThread extends Thread
  {
    private static final String                               NEXT_WEBCAMS_WAKEUP                   = "webcams.nextWakeUp";
    private static final long                                 WEBCAMS_SLEEP_PERIOD_MILLIS           = 24 * 3600 * 1000;                                     // 24 heures
    private static final long                                 WEBCAMS_EXCEPTION_SLEEP_PERIOD_MILLIS = 5 * 60 * 1000;                                        // 5 minutes

    private AbstractProvidersService                          providersService;
    private final Map<String, WebcamProvider<JSONAbleWebcam>> webcamProviders                       = new HashMap<String, WebcamProvider<JSONAbleWebcam>>();
    final Map<String, WebcamProviderInfos>                    webcamProvidersInfos                  = new HashMap<String, WebcamProviderInfos>();

    /**
     * 
     * @param providersService
     */
    WebcamProvidersThread(final AbstractProvidersService providersService)
    {
      // Initialisations
      super(WebcamProvidersThread.class.getName());
      this.providersService = providersService;
      final Resources resources = providersService.getResources();

      // Providers
      final String[] keys = resources.getStringArray(R.array.webcam_providers_keys);
      for (int i = 0; i < keys.length; i++)
      {
        final WebcamProvider<JSONAbleWebcam> provider;
        switch (i)
        {
          case 0:
            provider = new MobibalisesWebcamProvider(MOBIBALISES_WEBCAMS_KEY);
            break;
          default:
            throw new RuntimeException("Fournisseur de webcam inconnu");
        }
        webcamProviders.put(keys[i], provider);
        webcamProvidersInfos.put(keys[i], new WebcamProviderInfos());
      }
    }

    /**
     * 
     * @param delay
     */
    private void postAlarm(final long delay)
    {
      // Heure de reveil
      final long wakeUpTs = System.currentTimeMillis() + delay;

      // Alarme
      final AlarmManager alarmManager = (AlarmManager)providersService.getSystemService(Context.ALARM_SERVICE);
      final Intent intent = new Intent(WEBCAM_PROVIDERS_THREAD_WAKEUP_ACTION);
      final PendingIntent pendingIntent = PendingIntent.getBroadcast(providersService.getApplicationContext(), 0, intent, 0);
      alarmManager.set(AlarmManager.RTC_WAKEUP, wakeUpTs, pendingIntent);

      // Sauvegarde dans les preferences
      final SharedPreferences preferences = ActivityCommons.getSharedPreferences(providersService.getApplicationContext());
      final SharedPreferences.Editor editor = preferences.edit();
      editor.putLong(NEXT_WEBCAMS_WAKEUP, wakeUpTs);
      ActivityCommons.commitPreferences(editor);
      Log.d(getClass().getSimpleName(), "Webcam providers thread is now sleeping for " + delay + "ms (" + (int)Math.floor(delay / 3600000) + "h" + (int)Math.floor((delay / 60000) % 60) + "min" + ((delay / 1000) % 60) + "s)");
    }

    /**
     * 
     */
    void wakeUp()
    {
      // Notification
      synchronized (this)
      {
        notify();
      }
    }

    @Override
    public void run()
    {
      // MAJ des infos providers
      SQLiteDatabase database = null;
      try
      {
        // Initialisations
        final WebcamDatabaseHelper helper = WebcamDatabaseHelper.newInstance(providersService.getApplicationContext());
        database = helper.getReadableDatabase();

        // Pour chaque provider
        for (final Entry<String, WebcamProviderInfos> entry : webcamProvidersInfos.entrySet())
        {
          final String key = entry.getKey();
          final WebcamProviderInfos infos = entry.getValue();
          final long[] lastCheckUpdate = WebcamDatabaseHelper.getProviderLastCheckAndUpdate(database, key);
          infos.setLastWebcamsCheckLocalDate(lastCheckUpdate == null ? -1 : lastCheckUpdate[0]);
          infos.setLastWebcamsUpdateLocalDate(lastCheckUpdate == null ? -1 : lastCheckUpdate[1]);
        }
      }
      finally
      {
        if (database != null)
        {
          database.close();
        }
      }

      while (!isInterrupted())
      {
        // Besoin du provider ?
        while (!isInterrupted() && canWait())
        {
          try
          {
            // Attente
            synchronized (this)
            {
              Log.d(getClass().getSimpleName(), ">>> wait");
              wait();
              Log.d(getClass().getSimpleName(), "<<< wait");
            }
          }
          catch (final InterruptedException ie)
          {
            Log.d(getClass().getSimpleName(), ">>> interrupt");
            Thread.currentThread().interrupt();
          }
        }

        if (!isInterrupted())
        {
          // Fin de boucle d'attente
          final long debut = System.currentTimeMillis();

          // MAJ des donnees
          boolean ok;
          try
          {
            // MAJ
            ok = update();
          }
          catch (final Throwable th)
          {
            ok = false;
          }

          // Endormissement normal
          final long sleep;
          if (ok)
          {
            final long delta = System.currentTimeMillis() - debut;
            sleep = WEBCAMS_SLEEP_PERIOD_MILLIS - delta;
          }
          else
          {
            // Endormissement reduit (en cas d'erreur)
            final long delta = System.currentTimeMillis() - debut;
            sleep = WEBCAMS_EXCEPTION_SLEEP_PERIOD_MILLIS - delta;
          }

          // MAJ statut
          final long nextWakeUp = System.currentTimeMillis() + sleep;
          for (final WebcamProviderInfos infos : webcamProvidersInfos.values())
          {
            infos.setNextWakeUp(nextWakeUp);
          }

          // Dodo
          postAlarm(sleep);
        }
      }

      // Fin
      providersService = null;
    }

    /**
     * 
     * @return
     */
    private boolean canWait()
    {
      // Initialisations
      final SharedPreferences preferences = ActivityCommons.getSharedPreferences(providersService.getApplicationContext());
      final long nextWakeUp = preferences.getLong(NEXT_WEBCAMS_WAKEUP, -1);

      // Webcams affichees ?
      final boolean webcamsDisplayed = preferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WEBCAMS, AbstractBalisesPreferencesActivity.CONFIG_DATA_WEBCAMS_DEFAULT);
      Log.d(getClass().getSimpleName(), "webcamsDisplayed : " + webcamsDisplayed);
      // MAJ statuts
      for (final WebcamProviderInfos infos : webcamProvidersInfos.values())
      {
        infos.setPaused(!webcamsDisplayed);
        infos.setSleeping(webcamsDisplayed);
        infos.setNextWakeUp(nextWakeUp);
      }
      // On peut faire dodo
      if (!webcamsDisplayed)
      {
        return true;
      }

      // Base de données vierge ?
      /* TODO ?
      SQLiteDatabase database = null;
      try
      {
        final WebcamDatabaseHelper helper = WebcamDatabaseHelper.newInstance(providersService.getApplicationContext());
        database = helper.getReadableDatabase();
        final int nbProvidersUpdated = WebcamDatabaseHelper.getNbProvidersUpdated(database);
        if (nbProvidersUpdated == 0)
        {
          // Base vierge, il faut recharger tout
          Log.d(getClass().getSimpleName(), "webcams database is empty, force wakeup");
          return false;
        }
      }
      finally
      {
        if (database != null)
        {
          database.close();
        }
      }
      */

      // Le prochain reveil est-il passe ?
      Log.d(getClass().getSimpleName(), "nextWakeUp : " + nextWakeUp + " (" + (nextWakeUp - System.currentTimeMillis()) + ")");
      if (System.currentTimeMillis() < nextWakeUp)
      {
        // Non, on peut faire dodo
        return true;
      }

      // Réseau disponible ?
      Log.d(getClass().getSimpleName(), "networkOff : " + providersService.networkOff);
      if (providersService.networkOff)
      {
        // Non, on peut faire dodo
        return true;
      }

      // Ecran allumé ?
      Log.d(getClass().getSimpleName(), "screenOff : " + providersService.screenOff);
      if (providersService.screenOff)
      {
        // Oui, on peut faire dodo
        return true;
      }

      // Carte SD indisponible ?
      final File webcamDbDir = new File(WebcamDatabaseHelper.getDatabaseName(providersService.getApplicationContext())).getParentFile();
      final boolean sdAvailable = webcamDbDir.exists() && webcamDbDir.isDirectory() && webcamDbDir.canWrite();
      Log.d(getClass().getSimpleName(), "sdAvailable : " + sdAvailable);
      if (!sdAvailable)
      {
        // Oui (indispo), on peut faire dodo
        return true;
      }

      // Reveil necessaire !
      return false;
    }

    /**
     * 
     * @return
     */
    private boolean update()
    {
      Log.d(getClass().getSimpleName(), ">>> update()");
      boolean ok = true;
      for (final Map.Entry<String, WebcamProvider<JSONAbleWebcam>> entry : webcamProviders.entrySet())
      {
        ok = ok && updateProvider(entry.getKey(), entry.getValue());
      }
      Log.d(getClass().getSimpleName(), "<<< update() : " + ok);
      return ok;
    }

    /**
     * 
     * @param key
     * @param provider
     * @return
     */
    private boolean updateProvider(final String key, final WebcamProvider<JSONAbleWebcam> provider)
    {
      // Initialisations
      Log.d(getClass().getSimpleName(), ">>> updateProvider(" + key + ", ...)");
      final WebcamProviderInfos infos = webcamProvidersInfos.get(key);

      // Statuts
      infos.setSleeping(false);
      infos.setUpdateInProgress(true);

      // Ouverture de la base de données
      SQLiteDatabase database = null;

      try
      {
        // Ouverture de la base de données
        final WebcamDatabaseHelper helper = WebcamDatabaseHelper.newInstance(providersService.getApplicationContext());
        database = helper.getWritableDatabase();

        // Dernier timestamp de mise a jour
        final long[] lastCheckUpdate = WebcamDatabaseHelper.getProviderLastCheckAndUpdate(database, key);
        final long lastUpdate = (lastCheckUpdate == null ? -1 : lastCheckUpdate[1]);
        Log.d(getClass().getSimpleName(), "lastUpdate for " + key + " : " + lastUpdate);

        // Recup des webcams
        final Collection<JSONAbleWebcam> webcams = provider.getWebcams(lastUpdate);
        Log.d(getClass().getSimpleName(), webcams.size() + " webcams recuperees");

        // Debut de transaction
        database.beginTransaction();

        // Insertion/MAJ en base
        for (final Webcam webcam : webcams)
        {
          if (Webcam.ETAT_VALIDE.equals(webcam.etat))
          {
            WebcamDatabaseHelper.updateWebcam(database, key, webcam);
          }
          else if (Webcam.ETAT_INVALIDE.equals(webcam.etat))
          {
            WebcamDatabaseHelper.deleteWebcam(database, key, webcam);
          }
        }

        // MAJ des timestamps
        final long now = System.currentTimeMillis();
        WebcamDatabaseHelper.updateProviderLastCheck(database, key, now);
        infos.setLastWebcamsCheckLocalDate(now);
        if (webcams.size() > 0)
        {
          WebcamDatabaseHelper.updateProviderLastUpdate(database, key, now);
          infos.setLastWebcamsUpdateLocalDate(now);
        }

        // Transaction OK
        database.setTransactionSuccessful();

        // Notification
        providersService.fireWebcamsUpdate(key);

        // Fin
        return true;
      }
      catch (final IOException ioe)
      {
        infos.setWebcamsException(ioe);
        Log.e(getClass().getSimpleName(), ioe.getMessage(), ioe);
        return false;
      }
      finally
      {
        // Fermeture base de données
        if (database != null)
        {
          database.endTransaction();
          database.close();
        }

        // MAJ statuts
        infos.setSleeping(true);
        infos.setUpdateInProgress(false);

        // Fin
        Log.d(getClass().getSimpleName(), "<<< updateProvider(" + key + ", ...)");
      }
    }
  }

  @Override
  public IBinder onBind(final Intent intent)
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> onBind() : " + intent);

    return binder;
  }

  @Override
  public void onCreate()
  {
    // Parent
    Log.d(getClass().getSimpleName(), ">>> abstract.onCreate()");
    super.onCreate();

    spotsPath = new File(getApplicationContext().getExternalFilesDir(null), "spots");

    // Initialisations
    initCommons();
    resources = getApplicationContext().getResources();
    sharedPreferences = ActivityCommons.getSharedPreferences(getApplicationContext());
    debugMode = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    binder = new ProvidersServiceBinder(this);
    baliseCache = new BaliseAndroidCache(getApplicationContext());

    // Analytics
    initAnalytics();

    // Gestion des preferences providers
    AbstractBalisesPreferencesActivity.initBaliseProvidersPreferences(this);

    // Initialisation du search engine
    initSearchEngine();

    // Initialisation des webcams
    initWebcamProviders();

    // Gestion du broadcast
    initStopReceiver();

    // Gestion du reseau
    initNetworkReceiver();

    // Gestion de l'ecran
    initScreenReceiver();

    // Gestion de la carte SD
    registerSDCardListener();

    // Gestion de la localisation
    initLocationService();

    // Google analytics
    analyticsService.trackEvent(AnalyticsService.CAT_SERVICE, AnalyticsService.ACT_SERVICE_START, null);

    Log.d(getClass().getSimpleName(), "<<< abstract.onCreate()");
  }

  @Override
  public void onStart(final Intent intent, final int startId)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> abstract.onStart() " + Thread.currentThread());
    super.onStart(intent, startId);

    // Demarrage pour broadcast ?
    if (intent != null)
    {
      final String broadcastClient = intent.getStringExtra(BROADCAST_EXTRA_CLIENT);
      if (broadcastClient != null)
      {
        // Initialisation
        Log.d(getClass().getSimpleName(), "Received start broadcast from " + broadcastClient);

        // Enregistrement
        broadcastDatas = true;
        synchronized (broadcastClients)
        {
          if (!broadcastClients.contains(broadcastClient))
          {
            broadcastClients.add(broadcastClient);
          }
        }

        // Ajout du listener interne
        addBaliseProviderListener(broadcasterListener, false);
      }
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onStart() " + Thread.currentThread());
  }

  /**
   * 
   */
  protected void initCommons()
  {
    ActivityCommons.init(getApplicationContext());
    broadcasterListener = new BroadcasterListener(this);
  }

  /**
   * 
   */
  private void initAnalytics()
  {
    analyticsService = new AnalyticsService(getApplicationContext());
  }

  /**
   * 
   * @author pedro.m
   */
  private static class InitThread extends Thread
  {
    private AbstractProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    InitThread(final AbstractProvidersService providersService)
    {
      super(providersService.getClass().getName() + ".InitThread");
      this.providersService = providersService;
    }

    @Override
    public void run()
    {
      // Initialisation
      Log.d(getName(), ">>> run()");
      synchronized (providersService.initLock)
      {
        Log.d(getName(), "lock acquired");
        if (providersService.initDone)
        {
          return;
        }

        providersService.initBaliseProviders();
        if (!isInterrupted())
        {
          providersService.initDone = true;
        }
        providersService.initLock.notifyAll();
      }
      Log.d(getName(), "lock released");

      // Fin
      if (!isInterrupted())
      {
        providersService.onInitDone();
      }
      providersService = null;
      Log.d(getName(), "<<< run()");
    }
  }

  /**
   * 
   */
  protected void startInitThread()
  {
    synchronized (initLock)
    {
      if (!initDone)
      {
        if (initThread != null)
        {
          initThread.interrupt();
        }
        initThread = new InitThread(this);
        initThread.start();
      }
    }
  }

  /**
   * 
   */
  protected void onInitDone()
  {
    //Nothing
  }

  @Override
  public void waitForInit() throws InterruptedException
  {
    // Attente de la fin eventuelle du thread d'initialisation
    // On pourrait utiliser initThread.join(), sauf que dans le cas du service
    // de la version "full", initThread peut n'etre appele qu'apres la verification
    // de licence, donc join() quitterait tout de suite, meme si le thread d'init n'a
    // pas ete lance.
    // Or on veut attendre que le thread d'init ait ete lance et soit termine =>
    // mecanisme "custom" ci-dessous
    synchronized (initLock)
    {
      while (!initDone && !Thread.currentThread().isInterrupted())
      {
        try
        {
          Log.d(getClass().getSimpleName(), ">>> waitForInit.wait");
          initLock.wait();
          Log.d(getClass().getSimpleName(), ">>> waitForInit.wait");
        }
        catch (final InterruptedException ie)
        {
          Log.w(getClass().getSimpleName(), "waitForInit interrupted !");
          Thread.currentThread().interrupt();
          throw ie;
        }
      }
    }
  }

  /**
   * 
   * @param listener
   * @throws InterruptedException
   */
  void privateNotifyBaliseProviderListener(final BaliseProviderListener listener) throws InterruptedException
  {
    // Log
    Log.d(getClass().getSimpleName(), ">>> privateNotifyBaliseProviderListener(" + listener + ")");

    // Attente de la fin eventuelle du thread d'initialisation
    try
    {
      waitForInit();
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
      throw ie;
    }

    // Notifications des providers existants
    // Recuperation des resources
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
    final String[] hiddens = resources.getStringArray(R.array.providers_hidden);

    // Pour chaque provider
    for (int i = 0; i < keys.length; i++)
    {
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      final boolean hidden = ActivityCommons.isBaliseProviderHidden(keys[i], hiddens[i]);
      if ((debugMode || !forDebug) && !hidden)
      {
        final List<String> countries = BaliseProviderUtils.getBaliseProviderActiveCountries(AbstractProvidersService.this, sharedPreferences, keys[i], i);
        for (final String country : countries)
        {
          final String fullKey = BaliseProviderUtils.getBaliseProviderFullKey(keys[i], country);
          final BaliseProvider provider;
          final BaliseProviderInfos infos;
          synchronized (baliseProvidersLock)
          {
            // Interruption ?
            if (Thread.currentThread().isInterrupted())
            {
              return;
            }

            // Provider actif ?
            provider = baliseProviders.get(fullKey);
            if (provider != null)
            {
              // Infos
              infos = baliseProvidersInfos.get(fullKey);

              // Notification
              listener.onBaliseProviderAdded(fullKey, provider, infos.activeModes.contains(BaliseProviderMode.STANDARD), infos.activeModes);

              // Thread
              synchronized (infos.baliseProviderThreadLock)
              {
                // Gestion du Thread
                final BaliseProviderThread providerThread = infos.getBaliseProviderThread();
                if (!providerThread.isAlive())
                {
                  // Demarrage du Thread
                  providerThread.start();
                }
                else
                {
                  // Thread deja demarre : notification MAJ en cours puis attente de la fin de MAJ 
                  synchronized (infos.updateFireLock)
                  {
                    if (infos.isUpdateInProgress())
                    {
                      // MAJ commune
                      ActivityCommons.addStatusMessageProviderName(infos.getName());

                      // Listener
                      final boolean standardMode = infos.activeModes.contains(BaliseProviderMode.STANDARD);
                      listener.onBaliseProviderUpdateStarted(fullKey, provider, infos, standardMode);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Notification listeners
    fireBaliseProvidersChanged();

    // Log
    Log.d(getClass().getSimpleName(), "<<< privateNotifyBaliseProviderListener(" + listener + ")");
  }

  /**
   * 
   * @author pedro.m
   */
  private static class NotifyThread extends Thread
  {
    private AbstractProvidersService providersService;
    BaliseProviderListener           listener;

    /**
     * 
     * @param providersService
     * @param listener
     */
    NotifyThread(final AbstractProvidersService providersService, final BaliseProviderListener listener)
    {
      super(providersService.getClass().getName() + ".NotifyThread");
      this.providersService = providersService;
      this.listener = listener;
    }

    @Override
    public void run()
    {
      // Notification
      try
      {
        providersService.privateNotifyBaliseProviderListener(listener);
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }

      // Fin
      listener = null;
      providersService = null;
    }
  }

  /**
   * 
   * @param listener
   * @param waitForNotify
   */
  private void notifyBaliseProviderListener(final BaliseProviderListener listener, final boolean waitForNotify)
  {
    if (waitForNotify)
    {
      // Attente demandee : appel direct
      try
      {
        privateNotifyBaliseProviderListener(listener);
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
        return;
      }
    }
    else
    {
      // Pas d'attente demandee : appel via un thread
      final Thread notifyThread = new NotifyThread(this, listener);

      // Ajout du Thread dans la liste des threads a attendre en cas de fin
      synchronized (threads)
      {
        threads.add(notifyThread);
      }

      // Ajout du Thread dans la liste des threads a attendre en cas de retrait de listener de balises
      synchronized (notifyThreads)
      {
        final List<Thread> threadList;
        if (notifyThreads.containsKey(listener))
        {
          threadList = notifyThreads.get(listener);
        }
        else
        {
          threadList = new ArrayList<Thread>();
          notifyThreads.put(listener, threadList);
        }
        threadList.add(notifyThread);
      }

      // Demarrage
      notifyThread.start();
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ScreenReceiver extends BroadcastReceiver
  {
    private AbstractProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    ScreenReceiver(final AbstractProvidersService providersService)
    {
      this.providersService = providersService;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
      {
        providersService.screenOff = true;
        Log.d(providersService.getClass().getSimpleName(), "Screen is now off");
      }
      else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
      {
        providersService.screenOff = false;
        Log.d(providersService.getClass().getSimpleName(), "Screen is now on");
      }

      // Event
      providersService.onScreenChanged();

      // Si ecran on
      if (!providersService.screenOff)
      {
        // Event
        providersService.onScreenOn();

        // Reveil eventuel des Threads
        providersService.notifyBaliseProvidersThreads(false);
        providersService.webcamProvidersThread.wakeUp();
      }
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      providersService = null;
    }
  }

  /**
   * 
   */
  private void initScreenReceiver()
  {
    // Creation de l'espion
    screenReceiver = new ScreenReceiver(this);

    // Enregistrement
    final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
    filter.addAction(Intent.ACTION_SCREEN_OFF);
    registerReceiver(screenReceiver, filter);
  }

  /**
   * 
   */
  private void registerSDCardListener()
  {
    sdcardReceiver = new SDCardBroadcastReceiver(this);
    final IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
    filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
    filter.addDataScheme(INTENT_FILE_DATA_SCHEME);
    registerReceiver(sdcardReceiver, filter);
  }

  /**
   * 
   */
  protected void initLocationService()
  {
    locationService = new LocationService(getApplicationContext());
  }

  /**
   * 
   */
  protected void onScreenOn()
  {
    //Nothing
  }

  /**
   * 
   * @param screenOn
   */
  protected void onScreenChanged()
  {
    //Nothing
  }

  /**
   * 
   * @author pedro.m
   */
  protected static class BaliseProviderThreadReceiver extends BroadcastReceiver
  {
    private AbstractProvidersService providersService;
    private final String             fullKey;

    /**
     * 
     * @param providersService
     * @param fullKey
     */
    BaliseProviderThreadReceiver(final AbstractProvidersService providersService, final String fullKey)
    {
      this.providersService = providersService;
      this.fullKey = fullKey;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      Log.d(AbstractProvidersService.class.getSimpleName(), ">>> onReceive() BaliseProviderThread WakeUp for '" + fullKey + "'");
      final BaliseProviderInfos infos;
      synchronized (providersService.baliseProvidersLock)
      {
        infos = providersService.baliseProvidersInfos.get(fullKey);
        synchronized (infos.baliseProviderThreadLock)
        {
          final BaliseProviderThread thread = infos.getBaliseProviderThread();
          if ((thread != null) && thread.isAlive())
          {
            thread.wakeUp();
          }
        }
      }
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      providersService = null;
    }
  }

  /**
   * 
   * @param fullKey
   * @return
   */
  private BaliseProviderThreadReceiver initBaliseProviderThreadReceiver(final String fullKey)
  {
    // Creation de l'espion
    final BaliseProviderThreadReceiver receiver = new BaliseProviderThreadReceiver(this, fullKey);

    // Enregistrement
    final IntentFilter filter = new IntentFilter(BALISE_PROVIDER_THREAD_WAKEUP_ACTION + fullKey);
    registerReceiver(receiver, filter);

    return receiver;
  }

  /**
   * 
   */
  private void initSearchEngine()
  {
    // Demarrage du Thread
    if (searchEngineThread != null)
    {
      shutdownSearchEngine();
    }
    searchEngineThread = new SearchEngineThread(this);
    searchEngineThread.start();
    addBaliseProviderListener(searchEngineThread, false);
    addSpotProviderListener(searchEngineThread);
  }

  /**
   * 
   */
  private void shutdownSearchEngine()
  {
    // Fin du Thread
    removeBaliseProviderListener(searchEngineThread, false);
    searchEngineThread.interrupt();
    ThreadUtils.join(searchEngineThread);
    searchEngineThread = null;
  }

  /**
   * 
   * @author pedro.m
   */
  private static class WebcamProvidersThreadReceiver extends BroadcastReceiver
  {
    private AbstractProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    WebcamProvidersThreadReceiver(final AbstractProvidersService providersService)
    {
      this.providersService = providersService;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      Log.d(AbstractProvidersService.class.getSimpleName(), ">>> onReceive() WebcamProvidersThread WakeUp");
      providersService.webcamProvidersThread.wakeUp();
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      providersService = null;
    }
  }

  /**
   * 
   */
  private void initWebcamProviders()
  {
    // Demarrage du Thread
    if (webcamProvidersThread != null)
    {
      return;
    }
    webcamProvidersThread = new WebcamProvidersThread(this);
    webcamProvidersThread.start();

    // Initialisation du Receiver
    webcamProvidersThreadReceiver = new WebcamProvidersThreadReceiver(this);
    final IntentFilter filter = new IntentFilter(WEBCAM_PROVIDERS_THREAD_WAKEUP_ACTION);
    registerReceiver(webcamProvidersThreadReceiver, filter);
  }

  /**
   * 
   */
  private void shutdownWebcamProviders()
  {
    // Fin du Receiver
    unregisterReceiver(webcamProvidersThreadReceiver);
    webcamProvidersThreadReceiver.onShutdown();
    webcamProvidersThreadReceiver = null;

    // Fin du Thread
    webcamProvidersThread.interrupt();
    ThreadUtils.join(webcamProvidersThread);
    webcamProvidersThread = null;
  }

  /**
   * 
   * @param connectivityManager
   * @return
   */
  public static boolean isNetworkOff(final ConnectivityManager connectivityManager)
  {
    final NetworkInfo info = connectivityManager.getActiveNetworkInfo();
    if (info == null)
    {
      return true;
    }

    return !info.isAvailable() || !info.isConnectedOrConnecting();
  }

  /**
   * 
   */
  private void initStopReceiver()
  {
    stopReceiver = new BaliseProvidersStopReceiver(this);
    final IntentFilter filter = new IntentFilter(BROADCAST_STOP);
    registerReceiver(stopReceiver, filter);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class NetworkReceiver extends BroadcastReceiver
  {
    private AbstractProvidersService providersService;

    /**
     * 
     * @param providersService
     */
    NetworkReceiver(final AbstractProvidersService providersService)
    {
      this.providersService = providersService;
    }

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
      // Initialisations
      final ConnectivityManager connectivityManager = (ConnectivityManager)providersService.getSystemService(Context.CONNECTIVITY_SERVICE);

      // Etat du reseau
      final boolean netOff = isNetworkOff(connectivityManager);
      final boolean changed = (providersService.networkOff != netOff);
      final boolean wentOn = (providersService.networkOff && !netOff);
      providersService.networkOff = netOff;
      ActivityCommons.manageNetworkStatus(providersService.networkOff);
      Log.d(providersService.getClass().getSimpleName(), "NetworkOff : " + providersService.networkOff);

      // Reveil eventuel des Threads si le reseau est revenu
      if (wentOn)
      {
        providersService.notifyBaliseProvidersThreads(false);
        providersService.webcamProvidersThread.wakeUp();
      }

      // Rafraichissement des widgets si changement
      if (changed)
      {
        providersService.onNetworkChanged();
      }
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Fin
      providersService = null;
    }
  }

  /**
   * 
   */
  private void initNetworkReceiver()
  {
    // Recuperation de l'etat actuel
    final ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
    networkOff = isNetworkOff(connectivityManager);
    ActivityCommons.manageNetworkStatus(networkOff);
    Log.d(getClass().getSimpleName(), "NetworkOff : " + networkOff);

    // Creation de l'espion
    networkReceiver = new NetworkReceiver(this);

    // Enregistrement
    final IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
    registerReceiver(networkReceiver, filter);
  }

  /**
   * 
   */
  protected void onNetworkChanged()
  {
    //Nothing
  }

  /**
   * 
   */
  private void shutdownScreenReceiver()
  {
    if (screenReceiver != null)
    {
      unregisterReceiver(screenReceiver);
      screenReceiver.onShutdown();
      screenReceiver = null;
    }
  }

  /**
   * 
   */
  private void unregisterSDCardListener()
  {
    if (sdcardReceiver != null)
    {
      unregisterReceiver(sdcardReceiver);
      sdcardReceiver.onShutdown();
      sdcardReceiver = null;
    }
  }

  /**
   * 
   */
  private void shutdownLocationService()
  {
    if (locationService != null)
    {
      locationService.shutdown();
      locationService = null;
    }
  }

  /**
   * 
   */
  private void shutdownStopReceiver()
  {
    if (stopReceiver != null)
    {
      unregisterReceiver(stopReceiver);
      stopReceiver.onShutdown();
      stopReceiver = null;
    }
  }

  /**
   * 
   */
  private void shutdownNetworkReceiver()
  {
    if (networkReceiver != null)
    {
      unregisterReceiver(networkReceiver);
      networkReceiver.onShutdown();
      networkReceiver = null;
    }
  }

  /**
   * 
   */
  private void shutdownBaliseProviderThreads()
  {
    // Fin des Threads par provider
    synchronized (baliseProvidersLock)
    {
      for (final BaliseProviderInfos infos : baliseProvidersInfos.values())
      {
        synchronized (infos.baliseProviderThreadLock)
        {
          // Thread
          if ((infos.getBaliseProviderThread() != null) && (infos.getBaliseProviderThread().isAlive()))
          {
            infos.getBaliseProviderThread().interrupt();
            ThreadUtils.join(infos.getBaliseProviderThread());
            infos.setBaliseProviderThread(null);
          }

          // Receiver
          if (infos.baliseProviderThreadReceiver != null)
          {
            unregisterReceiver(infos.baliseProviderThreadReceiver);
            infos.baliseProviderThreadReceiver.onShutdown();
            infos.baliseProviderThreadReceiver = null;
          }
        }
      }
    }
  }

  /**
   * 
   */
  void initBaliseProviders()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> initBaliseProviders()");

    // Recuperation des resources
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
    final String[] hiddens = resources.getStringArray(R.array.providers_hidden);

    // Pour chaque provider
    for (int i = 0; i < keys.length; i++)
    {
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      final boolean hidden = ActivityCommons.isBaliseProviderHidden(keys[i], hiddens[i]);
      if ((debugMode || !forDebug) && !hidden)
      {
        final Map<String, List<BaliseProviderMode>> activeCountriesModes = getBaliseProviderActiveCountries(keys[i], i);
        for (final Map.Entry<String, List<BaliseProviderMode>> entry : activeCountriesModes.entrySet())
        {
          synchronized (baliseProvidersLock)
          {
            initBaliseProvider(keys[i], entry.getKey(), i, false, entry.getValue());
          }
        }
      }
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< initBaliseProviders()");
  }

  /**
   * 
   * @param key
   * @param index
   * @return
   */
  protected Map<String, List<BaliseProviderMode>> getBaliseProviderActiveCountries(final String key, final int index)
  {
    final List<String> countries = BaliseProviderUtils.getBaliseProviderActiveCountries(this, sharedPreferences, key, index);
    final Map<String, List<BaliseProviderMode>> activeCountriesModes = new HashMap<String, List<BaliseProviderMode>>();
    for (final String country : countries)
    {
      activeCountriesModes.put(country, staticProviderModeStandard);
    }

    return activeCountriesModes;
  }

  /**
   * 
   * @param providerName
   * @param providerKey
   * @param index
   * @param country
   * @return
   */
  private CachedProvider createCachedBaliseProvider(final String providerName, final String providerKey, final int index, final String country)
  {
    // Initialisations
    final AbstractBaliseProvider provider;
    final BalisesCorrector corrector;
    final String countryCode = BaliseProviderUtils.getCountryCode(country);
    final String regionCode = BaliseProviderUtils.getRegionCode(country);

    switch (index)
    {
      case 0: // FFVL-MobiBalises
        provider = new FfvlMobibalisesProvider(providerName, countryCode, FFVL_KEY, true);
        corrector = new BalisesCorrector(FFVL_CORRECTOR_URL);
        break;
      case 1: // PiouPiou-MobiBalises
        provider = new PioupiouProvider(providerName, countryCode);
        corrector = null;
        break;
      case 2: // METAR
        provider = new OgimetMetarProvider(providerName, countryCode, regionCode);
        corrector = null;
        break;
      case 3: // ROMMA-MobiBalises
        provider = new XmlMobibalisesRommaProvider(providerName, countryCode, ROMMA_KEY, true);
        corrector = null;
        break;
      case 4: // SYNOP
        provider = new OgimetSynopProvider(providerName, countryCode);
        corrector = null;
        break;
      case 5: // FFVL
        provider = new FfvlProvider(providerName, countryCode, FFVL_KEY, true);
        corrector = new BalisesCorrector(FFVL_CORRECTOR_URL);
        break;
      case 6: // ROMMA
        provider = new XmlRommaProvider(providerName, countryCode, ROMMA_KEY, true);
        corrector = null;
        break;
      case 7: // Bidon
        provider = new BidonProvider(providerName);
        corrector = null;
        break;
      default:
        throw new RuntimeException();
    }

    // CachedProvider
    return new CachedProvider(BaliseProviderUtils.getBaliseProviderFullKey(providerKey, country), provider, corrector, baliseCache);
  }

  /**
   * 
   * @param key
   * @param country
   * @param index
   * @param fireListeners
   * @param activeModes
   */
  private void initBaliseProvider(final String key, final String country, final int index, final boolean fireListeners, final List<BaliseProviderMode> activeModes)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> initBaliseProvider(" + key + ", " + country + ")");
    final String[] balisesDelays = resources.getStringArray(R.array.providers_balisesDelays);
    final String[] relevesDelays = resources.getStringArray(R.array.providers_relevesDelays);
    final String[] relevesAdjust = resources.getStringArray(R.array.providers_relevesAdjust);
    final String[] availabilities = resources.getStringArray(R.array.providers_availability);
    final String[] titles = resources.getStringArray(R.array.providers_titles);
    final String fullKey = BaliseProviderUtils.getBaliseProviderFullKey(key, country);

    // Creation
    final BaliseProviderInfos infos = new BaliseProviderInfos();
    final CachedProvider provider = createCachedBaliseProvider(titles[index], key, index, country);
    if (provider.isMultiCountries())
    {
      infos.setName(MessageFormat.format(FORMAT_INTERNATIONAL_BALISE_PROVIDER_NAME, titles[index], country.toLowerCase()));
    }
    else
    {
      infos.setName(titles[index]);
    }

    // Delais
    final long delaiBalises = Long.parseLong(balisesDelays[index], 10); // Minutes
    final long delaiReleves = Long.parseLong(relevesDelays[index], 10); // Minutes

    // Indisponibilite
    if (!Utils.isStringVide(availabilities[index]))
    {
      final String[] champs = availabilities[index].split(Strings.REGEXP_PIPE);
      if ((champs != null) && (champs.length == 3))
      {
        infos.timeZone = TimeZone.getTimeZone(champs[0]);
        infos.availabilityBegin = Utils.hmToMillis(champs[1]);
        infos.availabilityEnd = Utils.hmToMillis(champs[2]);
        infos.availabilityManaged = ((infos.timeZone != null) && (infos.availabilityBegin >= 0) && (infos.availabilityEnd >= 0));
      }
    }

    // Modes
    infos.activeModes = activeModes;

    // Infos balises
    infos.setBalisesUpdatePeriod(delaiBalises * 60000); // Millisecondes
    try
    {
      final long baliseCacheTimestamp = baliseCache.getCacheTimestamp(provider.getBalisesCacheKey());
      if (baliseCacheTimestamp > 0)
      {
        provider.restoreBalises();
        infos.setLastBalisesUpdateLocalDate(baliseCacheTimestamp);
        infos.setLastBalisesCheckLocalDate(sharedPreferences.getLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_BALISES_CHECK_LOCAL_DATE + Strings.CHAR_POINT + fullKey, baliseCacheTimestamp));
        infos.setLastBalisesUpdateDate(sharedPreferences.getLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_BALISES_UPDATE_DATE + Strings.CHAR_POINT + fullKey, -1));
        Log.d(getClass().getSimpleName(), "balises deserialized for " + fullKey + " : lbuld=" + new Date(infos.getLastBalisesUpdateLocalDate()) + ", lbud=" + new Date(infos.getLastBalisesUpdateDate()) + ", balises : "
            + provider.getBalises().size());
      }
    }
    catch (final IOException ioe)
    {
      Log.w(getClass().getSimpleName(), ioe);
      try
      {
        baliseCache.clearCache(provider.getBalisesCacheKey());
      }
      catch (final IOException ioe2)
      {
        throw new RuntimeException(ioe2);
      }
    }

    // Infos releves
    infos.setRelevesUpdatePeriod(delaiReleves * 60000); // Millisecondes
    infos.setAdjustRelevesUpdate(Boolean.parseBoolean(relevesAdjust[index]));
    try
    {
      final long releveCacheTimestamp = baliseCache.getCacheTimestamp(provider.getRelevesCacheKey());
      if (releveCacheTimestamp > 0)
      {
        provider.restoreReleves();
        infos.setLastRelevesUpdateLocalDate(releveCacheTimestamp);
        if (!infos.isAdjustRelevesUpdate())
        {
          infos.setLastRelevesCheckLocalDate(sharedPreferences.getLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_RELEVES_CHECK_LOCAL_DATE + Strings.CHAR_POINT + fullKey, releveCacheTimestamp));
        }
        infos.setLastRelevesUpdateDate(sharedPreferences.getLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_RELEVES_UPDATE_DATE + Strings.CHAR_POINT + fullKey, -1));
        Log.d(getClass().getSimpleName(), "releves deserialized for " + fullKey + " : lruld=" + new Date(infos.getLastRelevesUpdateLocalDate()) + ", lrud=" + new Date(infos.getLastRelevesUpdateDate()) + ", releves : "
            + provider.getReleves().size());
      }
    }
    catch (final IOException ioe)
    {
      Log.w(getClass().getSimpleName(), ioe);
      try
      {
        baliseCache.clearCache(provider.getRelevesCacheKey());
      }
      catch (final IOException ioe2)
      {
        throw new RuntimeException(ioe2);
      }
    }

    // Ajout
    addBaliseProvider(fullKey, provider, infos, fireListeners);

    // Thread
    final BaliseProviderThread providerThread = new BaliseProviderThread(this, fullKey, provider, infos);
    synchronized (infos.baliseProviderThreadLock)
    {
      infos.setBaliseProviderThread(providerThread);
      infos.baliseProviderThreadReceiver = initBaliseProviderThreadReceiver(fullKey);
      if (fireListeners)
      {
        providerThread.start();
      }
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< initBaliseProvider(" + key + ", " + country + ")");
  }

  @Override
  public void onDestroy()
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> abstract.onDestroy()");
    super.onDestroy();

    // Thread d'init
    if (initThread != null)
    {
      initThread.interrupt();
      ThreadUtils.join(initThread);
    }

    // Threads divers
    ThreadUtils.join(threads, true);

    // Search engine
    shutdownSearchEngine();

    // Webcams
    shutdownWebcamProviders();

    // Gestion du broadcast
    shutdownStopReceiver();

    // Gestion du reseau
    shutdownNetworkReceiver();

    // Gestion de l'ecran
    shutdownScreenReceiver();

    // Gestion de la carte SD
    unregisterSDCardListener();

    // Gestion de la localisation
    shutdownLocationService();

    // Gestion des Threads
    shutdownBaliseProviderThreads();

    // Nettoyages
    cleanUp();

    // Google analytics
    analyticsService.trackEvent(AnalyticsService.CAT_SERVICE, AnalyticsService.ACT_SERVICE_STOP, null);
    analyticsService.shutdown();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< abstract.onDestroy()");
  }

  /**
   * 
   */
  protected void cleanUp()
  {
    synchronized (baliseProvidersLock)
    {
      baliseProviders.clear();
      baliseProvidersInfos.clear();
    }
    synchronized (baliseProviderListeners)
    {
      baliseProviderListeners.clear();
    }
    synchronized (spotProviderListeners)
    {
      spotProviderListeners.clear();
    }
    activeSpotProviders.clear();
    spotProviders.clear();
    synchronized (broadcastClients)
    {
      broadcastClients.clear();
    }
    broadcasterListener.onShutdown();
    broadcasterListener = null;
    baliseCache.onShutdown();
  }

  @Override
  public boolean onUnbind(final Intent intent)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onUnbind()");

    return super.onUnbind(intent);
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param infos
   * @return
   */
  boolean goBalises(final String fullKey, final CachedProvider provider, final BaliseProviderInfos infos)
  {
    // Initialisations
    final long localDate = System.currentTimeMillis();
    Log.d(getClass().getSimpleName(), ">>> goBalises(" + fullKey + ")");

    try
    {
      if ((infos.getBalisesException() != null) || (infos.getLastBalisesUpdateDate() < 0) || ((1100 + localDate - infos.getLastBalisesCheckLocalDate()) >= infos.getBalisesUpdatePeriod()))
      {
        // Recuperation de la date de MAJ des balises
        Log.d(getClass().getSimpleName(), "Checking balises update date for " + fullKey);
        final boolean balisesUpdateDateUpdated = provider.updateBalisesUpdateDate();
        long localEndDate = System.currentTimeMillis();

        if (balisesUpdateDateUpdated)
        {
          // Recuperation OK, on compare
          final long balisesUpdateDate = provider.getBalisesUpdateDate();
          Log.d(getClass().getSimpleName(), "Balises update date for " + fullKey + " : " + new Date(balisesUpdateDate));

          // La date de MAJ cote fournisseur a-t-elle change ?
          if (infos.getLastBalisesUpdateDate() != balisesUpdateDate)
          {
            // La date de MAJ des balises a change, on les met a jour
            Log.d(getClass().getSimpleName(), "Balises needs update for " + fullKey);
            final boolean balisesUpdated = provider.updateBalises();
            localEndDate = System.currentTimeMillis();

            if (balisesUpdated)
            {
              // Log
              Log.d(getClass().getSimpleName(), "Balises updated for provider '" + fullKey + "'");

              // Correction
              final List<String> unknownBalises = provider.correct();

              // Sauvegarde (avec le timestamp "local")
              provider.storeBalises();
              baliseCache.setCacheTimestamp(provider.getBalisesCacheKey(), localEndDate);

              // Tout est OK sur les IO : MAJ des infos
              infos.setLastBalisesUpdateDate(balisesUpdateDate);
              infos.setLastBalisesUpdateLocalDate(localEndDate);
              final SharedPreferences.Editor editor = sharedPreferences.edit();
              editor.putLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_BALISES_UPDATE_DATE + Strings.CHAR_POINT + fullKey, balisesUpdateDate);
              ActivityCommons.commitPreferences(editor);

              // Signalement
              final boolean standardMode = infos.activeModes.contains(BaliseProviderMode.STANDARD);
              fireBalisesUpdate(fullKey, provider, standardMode, infos.activeModes);
              analyticsService.trackEvent(AnalyticsService.CAT_SERVICE, AnalyticsService.ACT_SERVICE_BALISES_UPDATE, fullKey);

              // Traces des balises non correctement corrigees
              if ((unknownBalises != null) && (unknownBalises.size() > 0))
              {
                Log.w(getClass().getSimpleName(), "Balises corrigees inconnues pour provider '" + fullKey + "' : " + unknownBalises);
              }
            }
          }

          // MAJ timestamp
          infos.setLastBalisesCheckLocalDate(localEndDate);
          final SharedPreferences.Editor editor = sharedPreferences.edit();
          editor.putLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_BALISES_CHECK_LOCAL_DATE + Strings.CHAR_POINT + fullKey, localEndDate);
          ActivityCommons.commitPreferences(editor);
        }

        // MAJ
        infos.setBalisesException(null);
      }

      return true;
    }
    catch (final IOException ioe)
    {
      infos.setBalisesException(ioe);
      infos.setLastBalisesCheckLocalDate(localDate);
      Log.w(getClass().getSimpleName(), ioe);
      return false;
    }
    finally
    {
      // Fin
      Log.d(getClass().getSimpleName(), "<<< goBalises(" + fullKey + ")");
    }
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param infos
   * @return
   */
  boolean goReleves(final String fullKey, final CachedProvider provider, final BaliseProviderInfos infos)
  {
    // Initialisations
    long localEndDate = System.currentTimeMillis();
    Log.d(getClass().getSimpleName(), ">>> goReleves(" + fullKey + ")");

    try
    {
      // RAZ des releves mis a jour
      provider.getUpdatedReleves().clear();

      // Recuperation de la date de MAJ des releves
      Log.d(getClass().getSimpleName(), "Checking releves update date for " + fullKey);
      final boolean relevesUpdateDateUpdated = provider.updateRelevesUpdateDate();
      localEndDate = System.currentTimeMillis();

      if (relevesUpdateDateUpdated)
      {
        // Recuperation OK, on compare
        final long relevesUpdateDate = provider.getRelevesUpdateDate();
        Log.d(getClass().getSimpleName(), "Releves update date for " + fullKey + " : " + new Date(relevesUpdateDate));

        // La date de MAJ cote fournisseur a-t-elle change ?
        if (infos.getLastRelevesUpdateDate() != relevesUpdateDate)
        {
          // La date de MAJ des releves a change, on les met a jour
          Log.d(getClass().getSimpleName(), "Releves needs update for " + fullKey);
          final boolean relevesUpdated = provider.updateReleves();
          localEndDate = System.currentTimeMillis();

          if (relevesUpdated)
          {
            // Log
            Log.d(getClass().getSimpleName(), "Releves updated for provider '" + fullKey + "'");
            Log.d(getClass().getSimpleName(), "Releves really updated : " + provider.getUpdatedReleves().size() + "/" + provider.getReleves().size());

            // Sauvegarde (avec le timestamp "local")
            provider.storeReleves();
            baliseCache.setCacheTimestamp(provider.getRelevesCacheKey(), localEndDate);

            // Tout est OK sur les IO : MAJ des infos
            infos.setLastRelevesUpdateDate(relevesUpdateDate);
            infos.setLastRelevesUpdateLocalDate(localEndDate);
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_RELEVES_UPDATE_DATE + Strings.CHAR_POINT + fullKey, relevesUpdateDate);
            ActivityCommons.commitPreferences(editor);

            // Signalement
            final boolean standardMode = infos.activeModes.contains(BaliseProviderMode.STANDARD);
            fireRelevesUpdate(fullKey, provider, standardMode, infos.activeModes);
            analyticsService.trackEvent(AnalyticsService.CAT_SERVICE, AnalyticsService.ACT_SERVICE_RELEVES_UPDATE, fullKey);
          }
        }
      }

      // MAJ
      infos.setRelevesException(null);

      // Fin
      return true;
    }
    catch (final IOException ioe)
    {
      infos.setRelevesException(ioe);
      infos.setLastRelevesCheckLocalDate(localEndDate);
      Log.w(getClass().getSimpleName(), ioe);
      return false;
    }
    finally
    {
      // MAJ timestamp
      infos.setLastRelevesCheckLocalDate(localEndDate);
      final SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.putLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_RELEVES_CHECK_LOCAL_DATE + Strings.CHAR_POINT + fullKey, localEndDate);
      ActivityCommons.commitPreferences(editor);

      // Fin
      Log.d(getClass().getSimpleName(), "<<< goReleves(" + fullKey + ")");
    }
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param infos
   * @return
   */
  boolean goRelevesAdjust(final String fullKey, final CachedProvider provider, final BaliseProviderInfos infos)
  {
    // Initialisations
    long localEndDate = System.currentTimeMillis();
    Log.d(getClass().getSimpleName(), ">>> goRelevesAdjust(" + fullKey + ") : " + infos);

    try
    {
      // RAZ des releves mis a jour
      provider.getUpdatedReleves().clear();

      // Recuperation de la date de MAJ des releves
      Log.d(getClass().getSimpleName(), "Checking releves update date for " + fullKey);
      final boolean relevesUpdateDateUpdated = provider.updateRelevesUpdateDate();
      localEndDate = System.currentTimeMillis();

      if (relevesUpdateDateUpdated)
      {
        // Recuperation OK, on compare
        final long relevesUpdateDate = provider.getRelevesUpdateDate();
        Log.d(getClass().getSimpleName(), "Releves update date for " + fullKey + " : " + new Date(relevesUpdateDate));

        // La date de MAJ cote fournisseur a-t-elle change ?
        if (infos.getLastRelevesUpdateDate() != relevesUpdateDate)
        {
          // La date de MAJ des releves a change, on les met a jour
          Log.d(getClass().getSimpleName(), "Releves needs update for " + fullKey);
          final boolean relevesUpdated = provider.updateReleves();
          localEndDate = System.currentTimeMillis();

          if (relevesUpdated)
          {
            // Log
            Log.d(getClass().getSimpleName(), "Releves updated for provider '" + fullKey + "'");
            Log.d(getClass().getSimpleName(), "Releves really updated : " + provider.getUpdatedReleves().size() + "/" + provider.getReleves().size());

            // Sauvegarde (avec le timestamp "local")
            provider.storeReleves();
            baliseCache.setCacheTimestamp(provider.getRelevesCacheKey(), localEndDate);

            // Tout est OK : MAJ des infos
            infos.setLastRelevesUpdateDate(relevesUpdateDate);
            infos.setLastRelevesUpdateLocalDate(localEndDate);
            final SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_RELEVES_UPDATE_DATE + Strings.CHAR_POINT + fullKey, relevesUpdateDate);
            ActivityCommons.commitPreferences(editor);

            // Signalement
            final boolean standardMode = infos.activeModes.contains(BaliseProviderMode.STANDARD);
            fireRelevesUpdate(fullKey, provider, standardMode, infos.activeModes);
            analyticsService.trackEvent(AnalyticsService.CAT_SERVICE, AnalyticsService.ACT_SERVICE_RELEVES_UPDATE, fullKey);
          }

          // MAJ des infos
          if (infos.getPreviousRelevesUpdateDate() > 0)
          {
            infos.setRelevesAdjusted(true);
          }
        }

        // MAJ des infos
        infos.setPreviousRelevesUpdateDate(relevesUpdateDate);
      }

      // MAJ
      infos.setRelevesException(null);

      // Fin
      return true;
    }
    catch (final IOException ioe)
    {
      infos.setRelevesException(ioe);
      infos.setRelevesAdjusted(false);
      infos.setPreviousRelevesUpdateDate(-1);
      Log.w(getClass().getSimpleName(), ioe);
      return false;
    }
    finally
    {
      // MAJ timestamp
      infos.setLastRelevesCheckLocalDate(localEndDate);
      final SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.putLong(AbstractBalisesPreferencesActivity.CONFIG_LAST_RELEVES_CHECK_LOCAL_DATE + Strings.CHAR_POINT + fullKey, localEndDate);
      ActivityCommons.commitPreferences(editor);

      // Fin
      Log.d(getClass().getSimpleName(), "<<< goRelevesAdjust(" + fullKey + ") : " + infos);
    }
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param standardMode
   * @param activeModes
   */
  protected void fireBalisesUpdate(final String fullKey, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    // Notification des listeners
    synchronized (baliseProviderListeners)
    {
      for (final BaliseProviderListener listener : baliseProviderListeners)
      {
        listener.onBalisesUpdate(fullKey, provider, standardMode, activeModes);
      }
    }
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param standardMode
   * @param activeModes
   */
  protected void fireRelevesUpdate(final String fullKey, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    // Notification des listeners
    synchronized (baliseProviderListeners)
    {
      for (final BaliseProviderListener listener : baliseProviderListeners)
      {
        listener.onRelevesUpdate(fullKey, provider, standardMode, activeModes);
      }
    }
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param infos
   * @param standardModeAdded
   * @param addedModes
   * @param oldActiveModes
   */
  protected void fireBaliseProviderModesAdded(final String fullKey, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardModeAdded, final List<BaliseProviderMode> addedModes,
      final List<BaliseProviderMode> oldActiveModes)
  {
    // Notification des listeners
    synchronized (baliseProviderListeners)
    {
      for (final BaliseProviderListener listener : baliseProviderListeners)
      {
        listener.onBaliseProviderModesAdded(fullKey, provider, infos, standardModeAdded, addedModes, oldActiveModes);
      }
    }
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param infos
   * @param standardModeRemoved
   * @param removedModes
   * @param oldActiveModes
   */
  protected void fireBaliseProviderModesRemoved(final String fullKey, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardModeRemoved, final List<BaliseProviderMode> removedModes,
      final List<BaliseProviderMode> oldActiveModes)
  {
    // Notification des listeners
    synchronized (baliseProviderListeners)
    {
      for (final BaliseProviderListener listener : baliseProviderListeners)
      {
        listener.onBaliseProviderModesRemoved(fullKey, provider, infos, standardModeRemoved, removedModes, oldActiveModes);
      }
    }
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param infos
   * @param fireListeners
   */
  private void addBaliseProvider(final String fullKey, final CachedProvider provider, final BaliseProviderInfos infos, final boolean fireListeners)
  {
    Log.d(getClass().getSimpleName(), ">>> addBaliseProvider(" + fullKey + ", ...)");

    // Initialisations
    baliseProviders.put(fullKey, provider);
    baliseProvidersInfos.put(fullKey, infos);

    // Propagation evenement
    if (fireListeners)
    {
      fireBaliseProviderAdded(fullKey, provider, infos.activeModes.contains(BaliseProviderMode.STANDARD), infos.activeModes);
    }
  }

  /**
   * 
   * @param fullKey
   * @param wasStandardMode
   * @param oldActiveModes
   */
  private void removeBaliseProvider(final String fullKey, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> removeBaliseProvider(" + fullKey + ")");

    // Arret thread
    final BaliseProviderInfos infos = baliseProvidersInfos.get(fullKey);
    synchronized (infos.baliseProviderThreadLock)
    {
      if (infos.getBaliseProviderThread() != null)
      {
        infos.getBaliseProviderThread().interrupt();
        infos.setBaliseProviderThread(null);
      }
    }

    // Retraits
    final BaliseProvider provider = baliseProviders.remove(fullKey);

    // Propagation evenement
    fireBaliseProviderRemoved(fullKey, provider, infos, wasStandardMode, oldActiveModes);

    // Fin
    Log.d(getClass().getSimpleName(), "<<< removeBaliseProvider(" + fullKey + ")");
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param standardMode
   * @param activeModes
   */
  protected void fireBaliseProviderAdded(final String fullKey, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    Log.d(getClass().getSimpleName(), ">>> fireBaliseProviderAdded(" + fullKey + ", ...)");
    synchronized (baliseProviderListeners)
    {
      for (final BaliseProviderListener listener : baliseProviderListeners)
      {
        listener.onBaliseProviderAdded(fullKey, provider, standardMode, activeModes);
      }
    }
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param infos
   * @param wasStandardMode
   * @param oldActiveModes
   */
  protected void fireBaliseProviderRemoved(final String fullKey, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes)
  {
    Log.d(getClass().getSimpleName(), ">>> fireBaliseProviderRemoved(" + fullKey + ", ...)");
    synchronized (baliseProviderListeners)
    {
      for (final BaliseProviderListener listener : baliseProviderListeners)
      {
        listener.onBaliseProviderRemoved(fullKey, provider, infos, wasStandardMode, oldActiveModes);
      }
    }
  }

  @Override
  public void addBaliseProviderListener(final BaliseProviderListener listener, final boolean waitForNotify)
  {
    // Log
    Log.d(getClass().getSimpleName(), ">>> addBaliseProviderListener(..., " + waitForNotify + ")");

    synchronized (baliseProviderListeners)
    {
      if (!baliseProviderListeners.contains(listener))
      {
        baliseProviderListeners.add(listener);
      }
      notifyBaliseProviderListener(listener, waitForNotify);
    }

    // Log
    Log.d(getClass().getSimpleName(), "<<< addBaliseProviderListener(..., " + waitForNotify + ")");
  }

  @Override
  public void removeBaliseProviderListener(final BaliseProviderListener listener, final boolean waitForNotifyEnd)
  {
    // Retrait des listeners
    synchronized (baliseProviderListeners)
    {
      baliseProviderListeners.remove(listener);
    }

    // Attente de la fin des threads de notification si demande
    if (waitForNotifyEnd)
    {
      synchronized (notifyThreads)
      {
        final List<Thread> threadList = notifyThreads.get(listener);
        if (threadList != null)
        {
          ThreadUtils.join(threadList, true);
        }
      }
    }
  }

  @Override
  public void updateBaliseProviders(final boolean async)
  {
    if (async)
    {
      final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>()
      {
        @Override
        protected Void doInBackground(final Void... arg)
        {
          privateUpdateBaliseProviders();

          return null;
        }
      };
      task.execute();
    }
    else
    {
      privateUpdateBaliseProviders();
    }
  }

  /**
   * 
   */
  void privateUpdateBaliseProviders()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> privateUpdateBaliseProviders()");
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
    final String[] hiddens = resources.getStringArray(R.array.providers_hidden);

    // Pour chaque provider
    synchronized (baliseProvidersLock)
    {
      for (int i = 0; i < keys.length; i++)
      {
        final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
        final boolean hidden = ActivityCommons.isBaliseProviderHidden(keys[i], hiddens[i]);
        if ((debugMode || !forDebug) && !hidden)
        {
          final List<String> countries = BaliseProviderUtils.getBaliseProviderCountries(this, i);
          for (final String country : countries)
          {
            final String fullKey = BaliseProviderUtils.getBaliseProviderFullKey(keys[i], country);
            final BaliseProviderInfos infos = baliseProvidersInfos.get(fullKey);
            final List<BaliseProviderMode> oldActiveModes = (infos == null ? null : infos.activeModes);
            final boolean wasStandard = (oldActiveModes == null ? false : oldActiveModes.contains(BaliseProviderMode.STANDARD));

            final List<BaliseProviderMode> activeModes = isBaliseProviderCountryActive(keys[i], country, i);
            if (infos != null)
            {
              infos.activeModes = activeModes;
            }

            final BaliseProvider provider = baliseProviders.get(fullKey);
            if ((activeModes.size() > 0) && (provider == null))
            {
              // Ajout du provider
              initBaliseProvider(keys[i], country, i, true, activeModes);
            }
            else if ((activeModes.size() == 0) && (provider != null))
            {
              // Retrait du provider
              removeBaliseProvider(fullKey, wasStandard, oldActiveModes);
            }
            else if (provider != null)
            {
              // Le(s) mode(s) ont change ?
              if (!activeModes.equals(oldActiveModes))
              {
                // Modes ajoutes
                final List<BaliseProviderMode> addedModes = new ArrayList<BaliseProviderMode>(activeModes);
                addedModes.removeAll(oldActiveModes);
                final boolean standardModeAdded = addedModes.contains(BaliseProviderMode.STANDARD);
                fireBaliseProviderModesAdded(fullKey, provider, infos, standardModeAdded, addedModes, oldActiveModes);

                // Modes supprimes
                final List<BaliseProviderMode> removedModes = new ArrayList<BaliseProviderMode>(oldActiveModes);
                removedModes.removeAll(activeModes);
                final boolean standardModeRemoved = removedModes.contains(BaliseProviderMode.STANDARD);
                fireBaliseProviderModesRemoved(fullKey, provider, infos, standardModeRemoved, removedModes, oldActiveModes);
              }
            }
          }
        }
      }
    }

    // Notification listeners
    fireBaliseProvidersChanged();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< privateUpdateBaliseProviders()");
  }

  /**
   * 
   * @param key
   * @param country
   * @param index
   * @return
   */
  protected List<BaliseProviderMode> isBaliseProviderCountryActive(final String key, final String country, final int index)
  {
    if (BaliseProviderUtils.isBaliseProviderCountryActive(this, sharedPreferences, key, country, index, null))
    {
      return staticProviderModeStandard;
    }

    return staticProviderModeEmpty;
  }

  @Override
  public BaliseProvider getBaliseProvider(final String fullKey)
  {
    synchronized (baliseProvidersLock)
    {
      return baliseProviders.get(fullKey);
    }
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param infos
   * @param standardMode
   */
  protected void fireBaliseProviderUpdateStarted(final String fullKey, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
  {
    // MAJ commune
    ActivityCommons.addStatusMessageProviderName(infos.getName());

    // Listeners
    synchronized (baliseProviderListeners)
    {
      for (final BaliseProviderListener listener : baliseProviderListeners)
      {
        listener.onBaliseProviderUpdateStarted(fullKey, provider, infos, standardMode);
      }
    }
  }

  /**
   * 
   */
  void fireBaliseProvidersChanged()
  {
    // Listeners
    synchronized (baliseProviderListeners)
    {
      for (final BaliseProviderListener listener : baliseProviderListeners)
      {
        listener.onBaliseProvidersChanged();
      }
    }
  }

  /**
   * 
   * @param fullKey
   * @param provider
   * @param infos
   * @param standardMode
   */
  protected void fireBaliseProviderUpdateEnded(final String fullKey, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
  {
    // MAJ commune
    ActivityCommons.removeStatusMessageProviderName(fullKey, infos.getName(), infos.getBalisesException(), infos.getRelevesException(), infos.isPaused());

    // Appels listeners
    synchronized (baliseProviderListeners)
    {
      for (final BaliseProviderListener listener : baliseProviderListeners)
      {
        listener.onBaliseProviderUpdateEnded(fullKey, provider, infos, standardMode);
      }
    }
  }

  @Override
  public SpotProvider getSpotProvider(final String key)
  {
    // Sauvegarde dans le repo
    SpotProvider provider = spotProviders.get(key);

    // Creation si besoin
    if (provider == null)
    {
      // Initialisation
      final String[] keys = resources.getStringArray(R.array.site_providers_keys);
      int index = 0;

      // FFVL
      if (key.equals(keys[index++]))
      {
        provider = new FfvlSpotProvider(FFVL_KEY, true);
      }
      // DHV
      else if (key.equals(keys[index++]))
      {
        provider = new DhvSpotProvider();
      }
      // PGE Europe Ouest
      else if (key.equals(keys[index++]))
      {
        provider = new PgeEuropeOuestSpotProvider();
      }
      // PGE Europe Est
      else if (key.equals(keys[index++]))
      {
        provider = new PgeEuropeEstSpotProvider();
      }
      // PGE Amerique du Nord
      else if (key.equals(keys[index++]))
      {
        provider = new PgeAmeriqueNordSpotProvider();
      }
      // PGE Amerique du Sud
      else if (key.equals(keys[index++]))
      {
        provider = new PgeAmeriqueSudSpotProvider();
      }
      // PGE Moyen Orient
      else if (key.equals(keys[index++]))
      {
        provider = new PgeMoyenOrientSpotProvider();
      }
      // PGE Afrique
      else if (key.equals(keys[index++]))
      {
        provider = new PgeAfriqueSpotProvider();
      }
      // PGE Asie
      else if (key.equals(keys[index++]))
      {
        provider = new PgeAsieSpotProvider();
      }
      // PGE Oceanie
      else if (key.equals(keys[index++]))
      {
        provider = new PgeOceanieSpotProvider();
      }

      // Sauvegarde dans le repo
      spotProviders.put(key, provider);
    }

    return provider;
  }

  /**
   * 
   * @param key
   * @param country
   * @return
   */
  private File getSpotsFile(final String key, final String country)
  {
    return new File(spotsPath, key + Strings.CHAR_POINT + country);
  }

  @Override
  public Date getLastSpotsUpdate(final String key, final String country) throws IOException
  {
    // Initialisations
    final File spotsFile = getSpotsFile(key, country);
    Date retour = null;

    // Si le fichier existe
    if (spotsFile.isFile() && spotsFile.canRead())
    {
      retour = new Date(FileTimestampUtils.lastModified(spotsFile));
    }

    return retour;
  }

  /**
   * 
   * @param os
   * @param spots
   * @throws IOException
   */
  private static void saveSpots(final OutputStream os, final List<Spot> spots) throws IOException
  {
    DataOutputStream out = null;

    try
    {
      out = new DataOutputStream(os);
      for (final Saveable saveable : spots)
      {
        saveable.saveSaveable(out);
      }
    }
    finally
    {
      if (out != null)
      {
        out.close();
      }
    }
  }

  /**
   * 
   * @param is
   * @return
   * @throws IOException
   */
  private static Object deserializeObject(final InputStream is) throws IOException
  {
    // Initialisations
    BufferedInputStream bis = null;
    ObjectInputStream ois = null;

    try
    {
      bis = new BufferedInputStream(is, 16000);
      ois = new ObjectInputStream(bis);

      return ois.readObject();
    }
    catch (final ClassNotFoundException cnfe)
    {
      final IOException ioe = new IOException(cnfe.getMessage());
      ioe.setStackTrace(cnfe.getStackTrace());
      throw ioe;
    }
    finally
    {
      if (ois != null)
      {
        ois.close();
      }
      if (bis != null)
      {
        bis.close();
      }
      if (is != null)
      {
        is.close();
      }
    }
  }

  /**
   * 
   * @param is
   * @param provider
   * @return
   * @throws IOException
   */
  private static List<Spot> loadSpots(final InputStream is, final SpotProvider provider) throws IOException
  {
    final List<Spot> retour = new ArrayList<Spot>();
    DataInputStream in = null;

    try
    {
      in = new DataInputStream(is);
      while (in.available() > 0)
      {
        final Spot spot = provider.newSpot();
        spot.loadSaveable(in);
        retour.add(spot);
      }

      return retour;
    }
    catch (final IOException ioe)
    {
      throw ioe;
    }
    catch (final Throwable th)
    {
      final IOException ioe = new IOException(th.getMessage());
      ioe.setStackTrace(th.getStackTrace());
      throw ioe;
    }
    finally
    {
      if (in != null)
      {
        in.close();
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<Spot> getSpots(final String key, final String country) throws IOException
  {
    // Initialisations
    final File spotsFile = getSpotsFile(key, country);
    List<Spot> retour = null;

    // Lecture
    if (spotsFile.exists() && spotsFile.canRead())
    {
      try
      {
        // Nouvelle version
        retour = loadSpots(new FileInputStream(spotsFile), getSpotProvider(key));
      }
      catch (final IOException ioe)
      {
        // Plantage, essai avec l'ancienne version en conservant le timestamp
        final long oldTimestamp = FileTimestampUtils.lastModified(spotsFile);
        retour = (List<Spot>)deserializeObject(new FileInputStream(spotsFile));
        saveSpots(new FileOutputStream(spotsFile), retour);
        FileTimestampUtils.setLastModified(spotsFile, oldTimestamp);
      }
    }

    return retour;
  }

  @Override
  public void storeSpots(final String key, final String country, final List<Spot> spots) throws IOException
  {
    // Initialisations
    final File spotsFile = getSpotsFile(key, country);

    // Effacement si deja existant
    if (spotsFile.exists() && spotsFile.canWrite())
    {
      FileTimestampUtils.deleteTimestampFile(spotsFile);
      spotsFile.delete();
    }
    else
    {
      spotsFile.getParentFile().mkdirs();
    }

    // Ecriture
    saveSpots(new FileOutputStream(spotsFile), spots);
  }

  @Override
  public Map<String, IOException> updateSpotProviders()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> updateSpotProviders()");
    final String[] keys = resources.getStringArray(R.array.site_providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.site_providers_forDebugs);
    final Map<String, IOException> exceptions = new HashMap<String, IOException>();
    final boolean spotProvidersEnabled = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES, AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES_DEFAULT);

    // Pour chaque provider
    for (int i = 0; i < keys.length; i++)
    {
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      final boolean active = sharedPreferences.getBoolean(keys[i], false);

      final SpotProvider provider = getSpotProvider(keys[i]);
      try
      {
        for (final String country : provider.getAvailableCountries())
        {
          try
          {
            final String countryKey = SpotProviderUtils.getFullSpotProviderKey(keys[i], country);
            final boolean countryActive = sharedPreferences.getBoolean(countryKey, false);
            final Date countryUpdate = getLastSpotsUpdate(keys[i], country);
            if (spotProvidersEnabled && active && countryActive && (countryUpdate != null) && ((activeSpotProviders.get(countryKey) == null) || forceSpotProvidersUpdate) && (debugMode || !forDebug))
            {
              if (forceSpotProvidersUpdate && (activeSpotProviders.get(countryKey) != null))
              {
                removeSpotProvider(provider, keys[i], country, countryKey);
              }
              initSpotProvider(provider, keys[i], country, countryKey, countryUpdate.getTime());
            }
            else if ((!spotProvidersEnabled || !active || !countryActive) && (activeSpotProviders.get(countryKey) != null))
            {
              removeSpotProvider(provider, keys[i], country, countryKey);
            }
          }
          catch (final IOException ioe)
          {
            exceptions.put(SpotProviderUtils.getFullSpotProviderKey(keys[i], country), ioe);
          }
        }
      }
      catch (final IOException ioe)
      {
        Log.e(getClass().getSimpleName(), ioe.getMessage(), ioe);
      }
    }

    // Fin
    forceSpotProvidersUpdate = false;

    return exceptions;
  }

  @Override
  public boolean haveSpotProvidersChanged()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> haveSpotProvidersChanged()");
    final String[] keys = resources.getStringArray(R.array.site_providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.site_providers_forDebugs);
    final boolean spotProvidersEnabled = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES, AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES_DEFAULT);

    // Pour chaque provider
    for (int i = 0; i < keys.length; i++)
    {
      final boolean forDebug = Boolean.parseBoolean(forDebugs[i]);
      final boolean active = sharedPreferences.getBoolean(keys[i], false);

      final SpotProvider provider = getSpotProvider(keys[i]);
      try
      {
        for (final String country : provider.getAvailableCountries())
        {
          final String countryKey = SpotProviderUtils.getFullSpotProviderKey(keys[i], country);
          final boolean countryActive = sharedPreferences.getBoolean(countryKey, false);
          final boolean countryAvailable = (getLastSpotsUpdate(keys[i], country) != null);
          if (spotProvidersEnabled && active && countryActive && countryAvailable && ((activeSpotProviders.get(countryKey) == null) || forceSpotProvidersUpdate) && (debugMode || !forDebug))
          {
            return true;
          }
          else if ((!spotProvidersEnabled || !active || !countryActive) && (activeSpotProviders.get(countryKey) != null))
          {
            return true;
          }
        }
      }
      catch (final IOException ioe)
      {
        throw new RuntimeException(ioe);
      }
    }

    return false;
  }

  @Override
  public void setForceDownloadSpotProviders(final boolean forceUpdate)
  {
    forceSpotProvidersUpdate = forceUpdate;
  }

  /**
   * 
   * @param provider
   * @param key
   * @param country
   * @param countryKey
   * @param updateTs
   */
  private void initSpotProvider(final SpotProvider provider, final String key, final String country, final String countryKey, final long updateTs) throws IOException
  {
    activeSpotProviders.put(countryKey, provider);
    fireSpotProviderAdded(provider, key, country, countryKey, updateTs);
  }

  /**
   * 
   * @param provider
   * @param key
   * @param country
   * @param countryKey
   */
  private void removeSpotProvider(final SpotProvider provider, final String key, final String country, final String countryKey)
  {
    activeSpotProviders.remove(countryKey);
    fireSpotProviderRemoved(provider, key, country, countryKey);
  }

  @Override
  public void addSpotProviderListener(final SpotProviderListener listener)
  {
    synchronized (spotProviderListeners)
    {
      if (!spotProviderListeners.contains(listener))
      {
        spotProviderListeners.add(listener);
      }
    }
  }

  @Override
  public void removeSpotProviderListener(final SpotProviderListener listener)
  {
    synchronized (spotProviderListeners)
    {
      spotProviderListeners.remove(listener);
    }
  }

  /**
   * 
   * @param provider
   * @param key
   * @param country
   * @param countryKey
   * @param updateTs
   */
  private void fireSpotProviderAdded(final SpotProvider provider, final String key, final String country, final String countryKey, final long updateTs) throws IOException
  {
    synchronized (spotProviderListeners)
    {
      if (spotProviderListeners.size() > 0)
      {
        final List<Spot> spots = getSpots(key, country);
        for (final SpotProviderListener listener : spotProviderListeners)
        {
          listener.onSpotProviderAdded(provider, key, country, countryKey, spots, updateTs);
        }
      }
    }
  }

  /**
   * 
   * @param provider
   * @param key
   * @param country
   * @param countryKey
   */
  private void fireSpotProviderRemoved(final SpotProvider provider, final String key, final String country, final String countryKey)
  {
    synchronized (spotProviderListeners)
    {
      for (final SpotProviderListener listener : spotProviderListeners)
      {
        listener.onSpotProviderRemoved(provider, key, country, countryKey);
      }
    }
  }

  @Override
  public BaliseProviderInfos getBaliseProviderInfos(final String fullKey)
  {
    synchronized (baliseProvidersLock)
    {
      return baliseProvidersInfos.get(fullKey);
    }
  }

  /**
   * 
   * @param fullKey
   * @param infos
   * @return
   */
  protected boolean canWait(final String fullKey, final BaliseProviderInfos infos)
  {
    // Pas de reseau => endormissement autorise
    if (networkOff)
    {
      return true;
    }

    // Ecran eteint => endormissement autorise
    if (screenOff)
    {
      return true;
    }

    // Si indispo
    if (infos.availabilityManaged && !infos.isAvailable())
    {
      return true;
    }

    // Sinon endormissement si le provider n'est pas necessaire
    final boolean standardMode = infos.activeModes.contains(BaliseProviderMode.STANDARD);
    return !isBaliseProviderNeeded(fullKey, standardMode, infos.activeModes);
  }

  /**
   * 
   * @param fullKey
   * @param standardMode
   * @param activeModes
   * @return
   */
  protected boolean isBaliseProviderNeeded(final String fullKey, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    for (final BaliseProviderListener listener : baliseProviderListeners)
    {
      if (listener.isBaliseProviderNeeded(fullKey, standardMode, activeModes))
      {
        return true;
      }
    }

    return false;
  }

  @Override
  public void notifyNeededBaliseProvidersChanged(final boolean isCallerWidget)
  {
    notifyBaliseProvidersThreads(isCallerWidget);
  }

  /**
   * 
   * @param startThreads
   */
  public void notifyBaliseProvidersThreads(final boolean startThreads)
  {
    synchronized (baliseProvidersLock)
    {
      for (final BaliseProviderInfos infos : baliseProvidersInfos.values())
      {
        synchronized (infos.baliseProviderThreadLock)
        {
          if (infos.getBaliseProviderThread() != null)
          {
            synchronized (infos.getBaliseProviderThread())
            {
              if (startThreads && !infos.getBaliseProviderThread().isAlive())
              {
                // Appelant = widget : demarrage du thread si besoin
                infos.getBaliseProviderThread().start();
              }
              else
              {
                // Appelant n'est pas widget ou deja demarre = simple notification
                infos.getBaliseProviderThread().notify();
              }
            }
          }
        }
      }
    }
  }

  @Override
  public LocationService getLocationService()
  {
    return locationService;
  }

  @Override
  public void stopSelfIfPossible()
  {
    Log.d(getClass().getSimpleName(), ">>> abstract.stopSelfIfPossible()");
    synchronized (baliseProviderListeners)
    {
      final int deltaSearchEngine = (baliseProviderListeners.contains(searchEngineThread) ? 1 : 0);
      final int listenersCount = baliseProviderListeners.size() - deltaSearchEngine;

      if (listenersCount == 0)
      {
        // On peut arreter le service
        Log.d(getClass().getSimpleName(), "Arret du service !");
        stopSelf();
      }
      else
      {
        // Arret impossible
        Log.d(getClass().getSimpleName(), "Des listeners sont enregistres (" + baliseProviderListeners + "), le service continue...");
      }
    }
    Log.d(getClass().getSimpleName(), "<<< abstract.stopSelfIfPossible()");
  }

  @Override
  public AnalyticsService getAnalyticsService()
  {
    return analyticsService;
  }

  @Override
  public boolean isScreenOff()
  {
    return screenOff;
  }

  @Override
  public boolean isNetworkOff()
  {
    return networkOff;
  }

  @Override
  public Set<String> getActiveBaliseProviders()
  {
    return baliseProviders.keySet();
  }

  @Override
  public void updateWebcamProviders()
  {
    webcamProvidersThread.wakeUp();
  }

  @Override
  public void addWebcamProviderListener(final WebcamProviderListener listener)
  {
    // Log
    Log.d(getClass().getSimpleName(), ">>> addWebcamProviderListener(...)");

    synchronized (webcamProviderListeners)
    {
      if (!webcamProviderListeners.contains(listener))
      {
        webcamProviderListeners.add(listener);
      }
    }

    // Log
    Log.d(getClass().getSimpleName(), "<<< addWebcamProviderListener(...)");
  }

  @Override
  public void removeWebcamProviderListener(final WebcamProviderListener listener)
  {
    // Retrait des listeners
    synchronized (webcamProviderListeners)
    {
      webcamProviderListeners.remove(listener);
    }
  }

  /**
   * 
   * @param key
   */
  void fireWebcamsUpdate(final String key)
  {
    // Notification des listeners
    synchronized (webcamProviderListeners)
    {
      for (final WebcamProviderListener listener : webcamProviderListeners)
      {
        listener.onWebcamsUpdate(key);
      }
    }
  }

  @Override
  public WebcamProviderInfos getWebcamProviderInfos(final String key)
  {
    return webcamProvidersThread.webcamProvidersInfos.get(key);
  }
}
