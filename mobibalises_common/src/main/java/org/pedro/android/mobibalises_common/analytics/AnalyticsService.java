package org.pedro.android.mobibalises_common.analytics;

import java.text.MessageFormat;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

/**
 * 
 * @author pedro.m
 */
public final class AnalyticsService implements SharedPreferences.OnSharedPreferenceChangeListener
{
  private static final int             DELAI                                   = 300;                                 // 5 minutes
  private static final String          GA_ID                                   = "UA-21264887-4";

  public static final String           CAT_SERVICE                             = "service";
  public static final String           CAT_MAP                                 = "map";
  public static final String           CAT_FAVORITES                           = "favorites";
  public static final String           CAT_FLIGHT_MODE                         = "flightMode";
  public static final String           CAT_HISTORY_MODE                        = "historyMode";
  public static final String           CAT_WIDGETS                             = "widgets";
  public static final String           CAT_ALARM_MODE                          = "alarmMode";

  public static final String           ACT_SERVICE_START                       = "start";
  public static final String           ACT_SERVICE_STOP                        = "stop";
  public static final String           ACT_SERVICE_BALISES_UPDATE              = "balisesUpdate";
  public static final String           ACT_SERVICE_RELEVES_UPDATE              = "relevesUpdate";

  public static final String           ACT_MAP_START                           = "start";
  public static final String           ACT_MAP_STOP                            = "stop";

  public static final String           ACT_MAP_BALISE_CLICKED                  = "baliseClicked";
  public static final String           ACT_MAP_BALISE_WEB_DETAIL_CLICKED       = "baliseWebDetailClicked";
  public static final String           ACT_MAP_BALISE_WEB_HISTO_CLICKED        = "baliseWebHistoClicked";
  public static final String           ACT_MAP_BALISE_SPEECH_CLICKED           = "baliseSpeechClicked";

  public static final String           ACT_MAP_SPOT_CLICKED                    = "spotClicked";
  public static final String           ACT_MAP_SPOT_INFORMATIONS_CLICKED       = "spotInformationsClicked";
  public static final String           ACT_MAP_SPOT_NAVIGATE_TO_CLICKED        = "spotNavigateToClicked";
  public static final String           ACT_MAP_SPOT_WEB_DETAIL_CLICKED         = "spotWebDetailClicked";

  public static final String           ACT_MAP_WEBCAM_CLICKED                  = "webcamClicked";

  public static final String           ACT_FAVORITES_START                     = "start";
  public static final String           ACT_FAVORITES_STOP                      = "stop";

  public static final String           ACT_FAVORITES_BALISE_CLICKED            = "baliseClicked";
  public static final String           ACT_FAVORITES_BALISE_WEB_DETAIL_CLICKED = "baliseWebDetailClicked";
  public static final String           ACT_FAVORITES_BALISE_WEB_HISTO_CLICKED  = "baliseWebHistoClicked";
  public static final String           ACT_FAVORITES_BALISE_SPEECH_CLICKED     = "baliseSpeechClicked";

  public static final String           ACT_FLIGHT_MODE_START                   = "start";
  public static final String           ACT_FLIGHT_MODE_STOP                    = "stop";
  public static final String           ACT_FLIGHT_MODE_SPEECH                  = "speech";

  public static final String           ACT_HISTORY_MODE_START                  = "start";
  public static final String           ACT_HISTORY_MODE_STOP                   = "stop";
  public static final String           ACT_HISTORY                             = "history";

  public static final String           ACT_WIDGET_ENABLED                      = "enabled";
  public static final String           ACT_WIDGET_DISABLED                     = "disabled";

  public static final String           ACT_ALARM_MODE_START                    = "start";
  public static final String           ACT_ALARM_MODE_STOP                     = "stop";
  public static final String           ACT_ALARM_FIRED                         = "fired";

  private static final String          MESSAGE_FORMAT_CATEGORY                 = "{0}:{1}:{2}:{3}";

  private final GoogleAnalyticsTracker tracker                                 = GoogleAnalyticsTracker.getInstance();

  private boolean                      doTracking                              = false;
  private final boolean                debugMode;
  private final boolean                forceTracking;
  private final String                 packageName;
  private final String                 versionName;
  private Context                      context;
  private final Resources              resources;
  private final SharedPreferences      sharedPreferences;
  private int                          buildNumber;

  /**
   * 
   */
  public AnalyticsService(final Context inContext)
  {
    // Initialisations
    this.context = inContext;
    this.resources = inContext.getResources();
    this.packageName = inContext.getPackageName();
    this.versionName = resources.getString(R.string.app_version);
    this.debugMode = (inContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
    this.forceTracking = false;

    // Preferences
    sharedPreferences = ActivityCommons.getSharedPreferences(inContext);
    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_ga_key));

    // Numero de build
    try
    {
      final PackageInfo packageInfo = inContext.getPackageManager().getPackageInfo(inContext.getPackageName(), 0);
      buildNumber = packageInfo.versionCode;
    }
    catch (final NameNotFoundException nnfe)
    {
      Log.w(getClass().getSimpleName(), nnfe);
    }
  }

  /**
   * 
   * @param category
   * @param action
   * @param label
   */
  public void trackEvent(final String category, final String action, final String label)
  {
    final String finalCategory = MessageFormat.format(MESSAGE_FORMAT_CATEGORY, packageName, Integer.toString(buildNumber, 10), versionName, category);
    if ((doTracking && !debugMode) || forceTracking)
    {
      Log.d(getClass().getSimpleName(), "Tracking event " + finalCategory + "/" + action + "/" + label);
      try
      {
        tracker.trackEvent(finalCategory, action, label, 0);
      }
      catch (final Throwable th)
      {
        th.printStackTrace(System.err);
      }
    }
    else
    {
      Log.d(getClass().getSimpleName(), "Skipping trackEvent(), tracking disabled for " + finalCategory + "/" + action + "/" + label);
    }
  }

  /**
   * 
   * @param active
   */
  private void setActive(final boolean activated)
  {
    // Changement de config ?
    if (activated == doTracking)
    {
      // Non => rien a faire
      return;
    }

    // Initialisations
    doTracking = activated;

    // Selon l'ordre donne
    if (doTracking)
    {
      // Activation
      tracker.start(GA_ID, DELAI, context);
    }
    else
    {
      // Desactivation
      tracker.dispatch();
      tracker.stop();
    }
  }

  /**
   * 
   */
  public void shutdown()
  {
    setActive(false);
    context = null;
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences inSharedPreferences, final String key)
  {
    if (resources.getString(R.string.config_ga_key).equals(key))
    {
      final boolean activated = inSharedPreferences.getBoolean(key, Boolean.parseBoolean(resources.getString(R.string.config_ga_default)));
      setActive(activated);
    }
  }
}
