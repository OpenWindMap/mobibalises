package com.pedro.android.mobibalises.widget;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.analytics.AnalyticsService;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.service.BaliseProviderInfos;
import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.android.mobibalises_common.view.WindIconInfos;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.map.Point;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.pedro.android.mobibalises.FullActivityCommons;
import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.favorites.BaliseFavorite;
import com.pedro.android.mobibalises.preferences.AbstractBalisesWidgetPreferencesActivity;
import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.service.ProvidersService;
import com.pedro.android.mobibalises.view.AdditionalWindIconInfos;
import com.pedro.android.mobibalises.view.FullDrawingCommons;
import com.pedro.android.mobibalises.view.WeatherIconInfos;
import com.pedro.android.mobibalises.voice.VoiceService;

/**
 * 
 * @author pedro.m
 */
public abstract class BalisesWidgets extends AppWidgetProvider
{
  private static final int    NB_BALISES_11           = 1;
  private static final int    NB_BALISES_21           = 2;
  private static final int    NB_BALISES_22           = 4;
  private static final int    NB_BALISES_33           = 9;
  private static final int    NB_BALISES_41           = 4;
  private static final int    NB_BALISES_42           = 8;
  private static final int    NB_BALISES_43           = 12;
  private static final int    NB_BALISES_44           = 16;

  private static final String PREFS_PROXIMITY_SIZE    = "PREFS_PROXIMITY_SIZE";
  private static final String PREFS_PROXIMITY_ITEM    = "PREFS_PROXIMITY_ITEM_";

  private static final String PREFS_AROUND_SIZE       = "PREFS_AROUND_SIZE";
  private static final String PREFS_AROUND_ITEM       = "PREFS_AROUND_ITEM_";
  private static final String PREFS_AROUND_NO_ITEM    = "PREFS_AROUND_NO_ITEM";

  private static final String ID_MESSAGE_FORMAT       = "<small>#{0}</small>";
  private static final String DATES_MESSAGE_FORMAT    = "<font color=\"{0}\"><small>{1}</small></font>";
  private static final String METHOD_SET_IMAGE_BITMAP = "setImageBitmap";
  private static final String ID_WIDGET_ICON          = ":id/widget_icon_";
  private static final String ID_WIDGET_TEXT          = ":id/widget_text_";
  private static final String TITLE_MESSAGE_FORMAT    = "<font color=\"#9999FF\"><b>{0}</b></font>";

  private static final String MESSAGE_UNKNOWN_DATE    = "???";
  private static String       MESSAGE_UPDATE_IN_PROGRESS;
  private static String       MESSAGE_UPDATE_IN_PROGRESS_SHORT;
  private static String       MESSAGE_NO_LOCALISATION;
  private static String       MESSAGE_NO_LOCALISATION_SHORT;

  private static float        scalingFactor           = 1;
  private static final Point  center                  = new Point();
  private static int          iconWidth;
  private static boolean      debugMode;

  private static boolean      licensed                = true;

  @Override
  public void onEnabled(final Context context)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), ">>> onEnabled()");
    super.onEnabled(context);

    // Statistiques
    final AnalyticsService analyticsService = new AnalyticsService(context);
    analyticsService.trackEvent(AnalyticsService.CAT_WIDGETS, AnalyticsService.ACT_WIDGET_ENABLED, getWidgetTypeName());
  }

  @Override
  public void onDeleted(final Context context, final int[] appWidgetsIds)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), ">>> onDeleted()");
    super.onDeleted(context, appWidgetsIds);

    // Notification au service
    final Intent intent = new Intent(ProvidersService.ACTION_SHUTDOWN_TRY);
    context.sendBroadcast(intent);
  }

  @Override
  public void onDisabled(final Context context)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), ">>> onDisabled()");
    super.onDisabled(context);

    // Notification au service
    final Intent intent = new Intent(ProvidersService.ACTION_SHUTDOWN_TRY);
    context.sendBroadcast(intent);

    // Statistiques
    final AnalyticsService analyticsService = new AnalyticsService(context);
    analyticsService.trackEvent(AnalyticsService.CAT_WIDGETS, AnalyticsService.ACT_WIDGET_DISABLED, getWidgetTypeName());
  }

  @Override
  public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), ">>> onUpdate()");
    super.onUpdate(context, appWidgetManager, appWidgetIds);
  }

  /**
   * 
   * @param context
   */
  private static void init(final Context context)
  {
    // Communs
    ActivityCommons.init(context);
    FullActivityCommons.init(context);

    // Initialisations graphiques
    final DisplayMetrics metrics = ActivityCommons.getDisplayMetrics(context);
    scalingFactor = metrics.density;
    DrawingCommons.initialize(context);
    FullDrawingCommons.initialize(context);

    // Calculs graphiques
    iconWidth = (int)Math.ceil((2 * DrawingCommons.WIND_ICON_FLECHE_MAX + 1) * scalingFactor);
    final int half = Math.round(iconWidth / 2);
    center.set(half, half);

    // Resources
    final Resources resources = context.getResources();
    MESSAGE_UPDATE_IN_PROGRESS = resources.getString(R.string.message_widget_update_in_progress);
    MESSAGE_UPDATE_IN_PROGRESS = resources.getString(R.string.message_widget_update_in_progress_short);
    MESSAGE_NO_LOCALISATION = resources.getString(R.string.message_widget_no_localisation);
    MESSAGE_NO_LOCALISATION = resources.getString(R.string.message_widget_no_localisation_short);
    debugMode = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
  }

  /**
   * 
   * @param context
   * @param appWidgetId
   * @param providersService
   * @param resources
   * @param sharedPreferences
   * @param providerKey
   */
  private static void synchronizeWidget(final Context context, final int appWidgetId, final IFullProvidersService providersService, final Resources resources, final SharedPreferences sharedPreferences, final String providerKey)
  {
    // Initialisations
    final int widgetType = getWidgetType(resources, sharedPreferences, appWidgetId);

    // Recuperation de la liste
    final List<BaliseFavorite> nonFilteredFavorites = getBalisesList(providersService, context, appWidgetId, resources, sharedPreferences);
    final List<BaliseFavorite> favorites;
    if ((widgetType == AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_33) && isAroundWidget(appWidgetId, resources, sharedPreferences))
    {
      favorites = nonFilteredFavorites;
    }
    else
    {
      favorites = filterActiveBalises(nonFilteredFavorites, resources, sharedPreferences);
    }

    // Selon le mode
    final String message;
    if (licensed)
    {
      if (isProximityWidget(appWidgetId, resources, sharedPreferences))
      {
        // Synchro des balises de proximite
        message = (providersService == null ? getMessageUpdateInProgress(resources, sharedPreferences, appWidgetId) : (favorites == null ? getMessageNoLocalization(resources, sharedPreferences, appWidgetId) : null));
      }
      else
      {
        // Synchro des balises favorites
        message = (providersService == null ? getMessageUpdateInProgress(resources, sharedPreferences, appWidgetId) : null);
      }
    }
    else
    {
      message = resources.getString(R.string.message_unlicensed_widget);
    }

    // Synchro
    if (!Utils.isStringVide(message))
    {
      Log.w(BalisesWidgets.class.getSimpleName(), "Widget message : " + message + " (providersService : " + providersService + ", favorites : " + favorites + ")");
    }
    final List<String> displayedIds = synchronizeWidget(context, appWidgetId, providersService, favorites, message, false);

    // Voix
    final String speakAllKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_speak_all_key), appWidgetId);
    final boolean speakAll = sharedPreferences.getBoolean(speakAllKey, Boolean.parseBoolean(resources.getString(R.string.config_widget_speak_all_default)));
    Log.d(BalisesWidgets.class.getSimpleName(), "Check speaking for " + providerKey + " (speakAll=" + speakAll + ")");
    if ((providersService != null) && (favorites != null) && (providerKey != null) && isSpeakingWidget(appWidgetId, providersService.isScreenOff(), resources, sharedPreferences))
    {
      // Pour chaque balise de la liste
      for (final BaliseFavorite favorite : favorites)
      {
        // Balise du provider a l'origine de la mise a jour ?
        if (!providerKey.equals(favorite.getProviderId()))
        {
          // Non => on ignore la balise favorite
          continue;
        }

        // Liste des releves mis a jour pour le provider
        final Collection<Releve> updatedReleves = providersService.getBaliseProvider(favorite.getProviderId()).getUpdatedReleves();

        // La balise a-t-elle ete mise a jour ?
        Releve updatedReleve = null;
        Balise updatedBalise = null;
        for (final Releve releve : updatedReleves)
        {
          if (releve.id.equals(favorite.getBaliseId()) && (speakAll || displayedIds.contains(releve.id)))
          {
            updatedBalise = providersService.getBaliseProvider(favorite.getProviderId()).getBaliseById(favorite.getBaliseId());
            updatedReleve = releve;
            break;
          }
        }

        // Si balise mise a jour => voix
        if (updatedReleve != null)
        {
          Log.d(BalisesWidgets.class.getSimpleName(), "Speaking for " + favorite.getProviderId() + "." + updatedReleve.id);
          providersService.getVoiceService().speakBaliseReleve(updatedBalise, updatedReleve, false, null);
        }
      }
    }
  }

  /**
   * 
   * @param nonFilteredFavorites
   * @param resources
   * @param sharedPreferences
   * @return
   */
  private static List<BaliseFavorite> filterActiveBalises(final List<BaliseFavorite> nonFilteredFavorites, final Resources resources, final SharedPreferences sharedPreferences)
  {
    // Initialisations
    final List<BaliseFavorite> filtered = new ArrayList<BaliseFavorite>();
    final boolean displayInactive = !sharedPreferences.getBoolean(resources.getString(R.string.config_map_balise_hide_inactive_key), Boolean.parseBoolean(resources.getString(R.string.config_map_balise_hide_inactive_default)));

    // Pour chaque balise
    if (nonFilteredFavorites != null)
    {
      for (final BaliseFavorite favorite : nonFilteredFavorites)
      {
        if ((favorite != null) && (displayInactive || favorite.isDrawable()))
        {
          filtered.add(favorite);
        }
      }
    }

    return filtered;
  }

  /**
   * 
   * @param resources
   * @param sharedPreferences
   * @param appWidgetId
   * @return
   */
  private static CommonWidgetDrawingInfos getDrawingInfos(final Resources resources, final SharedPreferences sharedPreferences, final int appWidgetId)
  {
    // Initialisations
    final CommonWidgetDrawingInfos drawingInfos = new CommonWidgetDrawingInfos();

    // Affichage ou non de la meteo
    final String displayWeatherKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_display_weather_key), appWidgetId);
    drawingInfos.displayWeather = sharedPreferences.getBoolean(displayWeatherKey, Boolean.parseBoolean(resources.getString(R.string.config_widget_display_weather_default)));

    return drawingInfos;
  }

  /**
   * 
   * @param context
   * @param appWidgetId
   * @param providersService
   * @param favorites
   * @param doInit
   */
  private static List<String> synchronizeWidget(final Context context, final int appWidgetId, final IFullProvidersService providersService, final List<BaliseFavorite> favorites, final String message, final boolean doInit)
  {
    // Initialisations
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
    if (doInit)
    {
      init(context);
      DrawingCommons.updatePreferences(resources, sharedPreferences);
    }

    // Creation vues
    final RemoteViews views = new RemoteViews(context.getPackageName(), getWidgetLayoutId(resources, sharedPreferences, appWidgetId));

    // Gestion du titre
    final int widgetType = getWidgetType(resources, sharedPreferences, appWidgetId);
    switch (widgetType)
    {
      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_11:
        BalisesWidget11.manageTextviews(context, resources, sharedPreferences, appWidgetId, views);
        break;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_21:
        BalisesWidget21.manageTextviews(context, resources, sharedPreferences, appWidgetId, views);
        break;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_22:
        BalisesWidget22.manageTextviews(context, resources, sharedPreferences, appWidgetId, views);
        break;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_33:
        BalisesWidget33.manageHeader(context, resources, sharedPreferences, appWidgetId, views);
        break;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_41:
        BalisesWidget41.manageTextviews(context, resources, sharedPreferences, appWidgetId, views);
        break;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_42:
        BalisesWidget42.manageTextviews(context, resources, sharedPreferences, appWidgetId, views);
        break;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_43:
        BalisesWidget43.manageTextviews(context, resources, sharedPreferences, appWidgetId, views);
        break;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_44:
        BalisesWidget44.manageTextviews(context, resources, sharedPreferences, appWidgetId, views);
        break;
    }

    // Gestion de l'ID du widget
    final String idText = MessageFormat.format(ID_MESSAGE_FORMAT, Integer.valueOf(appWidgetId));
    views.setTextViewText(R.id.widget_id, Html.fromHtml(idText));

    // Gestion du message
    views.setTextViewText(R.id.widget_message, (message == null ? Strings.VIDE : message));

    // Infos communes
    final CommonWidgetDrawingInfos drawingInfos = getDrawingInfos(resources, sharedPreferences, appWidgetId);

    // Affichage des 8 balises
    final List<String> displayedIds;
    if (licensed)
    {
      switch (widgetType)
      {
        case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_11:
        case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_21:
        case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_22:
        case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_41:
        case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_42:
        case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_43:
        case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_44:
          displayedIds = synchronizeBalisesStandardList(context, appWidgetId, providersService, favorites, views, drawingInfos);
          break;

        case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_33:
          if (isAroundWidget(appWidgetId, resources, sharedPreferences))
          {
            displayedIds = synchronizeBalisesExactList(context, appWidgetId, providersService, favorites, views, drawingInfos);
          }
          else
          {
            displayedIds = synchronizeBalisesStandardList(context, appWidgetId, providersService, favorites, views, drawingInfos);
          }
          break;
        default:
          displayedIds = null;
          break;
      }
    }
    else
    {
      // Pas de license : pas de balise
      displayedIds = synchronizeBalisesExactList(context, appWidgetId, providersService, null, views, drawingInfos);
    }

    // Gestion des dates et du statut
    final ActivityCommons.ProvidersStatus providersStatus = ActivityCommons.getProvidersStatus(context, getNeededProviders(favorites));
    final int colorId;
    switch (providersStatus)
    {
      case ERROR:
        colorId = R.color.widget_status_error;
        break;
      case OK:
        colorId = R.color.widget_status_ok;
        break;
      case WARNING:
        colorId = R.color.widget_status_warning;
        break;
      default:
        colorId = R.color.widget_status_ok;
        break;
    }
    final int color = resources.getColor(colorId);
    final String colorString = Strings.toHexColor(color);
    final long lastUpdateDate = getLastUpdateDate(context, appWidgetId, providersService);
    final DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(context);
    final String dates = MessageFormat.format(DATES_MESSAGE_FORMAT, colorString, (lastUpdateDate > 0 ? dateFormat.format(new Date(lastUpdateDate)) : MESSAGE_UNKNOWN_DATE));
    views.setTextViewText(R.id.widget_dates, Html.fromHtml(dates));

    // Notification du widget
    Log.d(BalisesWidgets.class.getSimpleName(), "updating widget #" + appWidgetId);
    AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, views);

    return displayedIds;
  }

  /**
   * 
   * @param context
   * @return
   */
  private static PendingIntent getMainPendingIntent(final Context context)
  {
    if (licensed)
    {
      // License OK : click = arret de la synthse vocale ou lancement de Mobibalises (decision geree dans le Service)
      final Intent intent = new Intent(ProvidersService.ACTION_WIDGET_TAP);
      return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    // Pas de license : click = play store
    final Intent goToMarket = new Intent(Intent.ACTION_VIEW, ActivityCommons.MARKET_URI);
    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    return PendingIntent.getActivity(context, 0, goToMarket, 0);
  }

  /**
   * 
   * @param context
   * @param appWidgetId
   * @param providersService
   * @param favorites
   * @param views
   * @param drawingInfos
   * @return
   */
  private static List<String> synchronizeBalisesStandardList(final Context context, final int appWidgetId, final IFullProvidersService providersService, final List<BaliseFavorite> favorites, final RemoteViews views,
      final CommonWidgetDrawingInfos drawingInfos)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), ">>> synchronizeBalisesStandardList(...)");
    final List<String> displayedIds = new ArrayList<String>();
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);

    // Affichage des N balises
    int favoritesIndex = 0;
    for (int i = 1; i <= getNbBalises(resources, sharedPreferences, appWidgetId); i++)
    {
      // Recuperation balise et releve
      BaliseFavorite favorite = null;
      Balise balise = null;
      Releve releve = null;
      if ((favorites != null) && (providersService != null))
      {
        for (int fi = favoritesIndex; fi < favorites.size(); fi++)
        {
          favorite = favorites.get(fi);
          final BaliseProvider provider = providersService.getBaliseProvider(favorite.getProviderId());
          if (provider != null)
          {
            // Balise et releve
            balise = provider.getBaliseById(favorite.getBaliseId());
            releve = provider.getReleveById(favorite.getBaliseId());

            // Suivante
            favoritesIndex = fi + 1;
            break;
          }
        }
      }

      // Synchro
      synchronizeBalise(context, views, i, balise, releve, getMainPendingIntent(context), drawingInfos);
      if (releve != null)
      {
        displayedIds.add(releve.id);
      }
    }

    return displayedIds;
  }

  /**
   * 
   * @param context
   * @param appWidgetId
   * @param providersService
   * @param favorites
   * @param views
   * @param drawingInfos
   */
  private static List<String> synchronizeBalisesExactList(final Context context, final int appWidgetId, final IFullProvidersService providersService, final List<BaliseFavorite> favorites, final RemoteViews views,
      final CommonWidgetDrawingInfos drawingInfos)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), ">>> synchronizeBalisesExactList(...)");
    final List<String> displayedIds = new ArrayList<String>();
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);

    // Affichage des N balises
    for (int i = 1; i <= getNbBalises(resources, sharedPreferences, appWidgetId); i++)
    {
      // Recuperation balise et releve
      BaliseFavorite favorite = null;
      Balise balise = null;
      Releve releve = null;
      if ((favorites != null) && (providersService != null))
      {
        favorite = (i <= favorites.size() ? favorites.get(i - 1) : null);
        if (favorite != null)
        {
          final BaliseProvider provider = providersService.getBaliseProvider(favorite.getProviderId());
          if (provider != null)
          {
            // Balise et releve
            balise = provider.getBaliseById(favorite.getBaliseId());
            releve = provider.getReleveById(favorite.getBaliseId());
          }
        }
      }

      // Synchro
      synchronizeBalise(context, views, i, balise, releve, getMainPendingIntent(context), drawingInfos);
      if (releve != null)
      {
        displayedIds.add(releve.id);
      }
    }

    return displayedIds;
  }

  /**
   * 
   * @param context
   * @param views
   * @param preferencesActivityClass
   */
  protected static void manageTitleClick(final Context context, final RemoteViews views, final Class<? extends AbstractBalisesWidgetPreferencesActivity> preferencesActivityClass)
  {
    // Gestion du click sur le libelle
    final Intent prefsIntent = new Intent(context, preferencesActivityClass);
    prefsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    final PendingIntent prefsPendingIntent = PendingIntent.getActivity(context, 0, prefsIntent, 0);
    views.setOnClickPendingIntent(R.id.widget_title, prefsPendingIntent);
  }

  /**
   * 
   * @param context
   * @param resources
   * @param sharedPreferences
   * @param appWidgetId
   * @param views
   * @param preferencesActivityClass
   */
  protected static void manageStandardHeader(final Context context, final Resources resources, final SharedPreferences sharedPreferences, final int appWidgetId, final RemoteViews views,
      final Class<? extends AbstractBalisesWidgetPreferencesActivity> preferencesActivityClass)
  {
    // Gestion de l'en-tete
    final String displayHeaderKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_display_header_key), appWidgetId);
    final boolean displayHeader = sharedPreferences.getBoolean(displayHeaderKey, Boolean.parseBoolean(resources.getString(R.string.config_widget_display_header_default)));
    views.setViewVisibility(R.id.widget_id, displayHeader ? View.VISIBLE : View.GONE);
    views.setViewVisibility(R.id.widget_dates, displayHeader ? View.VISIBLE : View.GONE);

    // Gestion du titre
    final String displayTitleKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_display_title_key), appWidgetId);
    final boolean displayTitle = sharedPreferences.getBoolean(displayTitleKey, Boolean.parseBoolean(resources.getString(R.string.config_widget_display_title_default)));
    views.setViewVisibility(R.id.widget_title, displayTitle ? View.VISIBLE : View.GONE);
    if (displayTitle)
    {
      // Texte
      final String proximityValue = resources.getString(R.string.config_widget_label_value_proximity);
      final String proximityDisplay = resources.getString(R.string.label_proximity);
      final String aroundValue = resources.getString(R.string.config_widget_label_value_around);
      final String aroundDisplay = resources.getString(R.string.label_around);
      final String labelKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_label_key), appWidgetId);
      final String labelValue = sharedPreferences.getString(labelKey, resources.getString(R.string.config_widget_label_value_proximity));
      final String label = (proximityValue.equals(labelValue) ? proximityDisplay : aroundValue.equals(labelValue) ? aroundDisplay : labelValue);
      final String title = MessageFormat.format(TITLE_MESSAGE_FORMAT, label);
      views.setTextViewText(R.id.widget_title, Html.fromHtml(title));

      // Gestion des clicks
      manageTitleClick(context, views, preferencesActivityClass);
    }
  }

  /**
   * 
   * @param favorites
   * @return
   */
  private static List<String> getNeededProviders(final List<BaliseFavorite> favorites)
  {
    // Verification
    if (favorites == null)
    {
      return null;
    }

    // Initialisations
    final List<String> neededProviders = new ArrayList<String>();
    for (final BaliseFavorite favorite : favorites)
    {
      if ((favorite != null) && !neededProviders.contains(favorite.getProviderId()))
      {
        neededProviders.add(favorite.getProviderId());
      }
    }

    return neededProviders;
  }

  /**
   * 
   * @param context
   * @param appWidgetId
   * @param providersService
   * @return
   */
  private static long getLastUpdateDate(final Context context, final int appWidgetId, final IFullProvidersService providersService)
  {
    // Verification
    if (providersService == null)
    {
      return -1;
    }

    // Recuperation des resources
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
    final String[] keys = resources.getStringArray(R.array.providers_keys);
    final String[] forDebugs = resources.getStringArray(R.array.providers_forDebugs);
    final String[] hiddens = resources.getStringArray(R.array.providers_hidden);

    // Pour chaque provider
    long lastUpdate = -1;
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
          final boolean needed = isBaliseProviderNeeded(providersService, context, fullKey, appWidgetId, resources, sharedPreferences, false);
          if (needed)
          {
            final BaliseProviderInfos infos = providersService.getBaliseProviderInfos(fullKey);
            if ((infos != null) && (infos.getLastRelevesUpdateLocalDate() > lastUpdate))
            {
              lastUpdate = infos.getLastRelevesUpdateLocalDate();
            }
          }
        }
      }
    }

    // Fin
    return lastUpdate;
  }

  /**
   * 
   * @param context
   * @param views
   * @param index
   * @param balise
   * @param releve
   * @param mainPendingIntent
   * @param drawingInfos
   */
  private static void synchronizeBalise(final Context context, final RemoteViews views, final int index, final Balise balise, final Releve releve, final PendingIntent mainPendingIntent, final CommonWidgetDrawingInfos drawingInfos)
  {
    // Le nom de la balise
    final int textId = context.getResources().getIdentifier(context.getPackageName() + ID_WIDGET_TEXT + index, null, null);
    views.setTextViewText(textId, balise == null ? Strings.VIDE : balise.nom);
    views.setOnClickPendingIntent(textId, mainPendingIntent);

    // Icones meteo et vent
    final Bitmap iconBitmap = Bitmap.createBitmap(iconWidth, iconWidth, Bitmap.Config.ARGB_4444);
    final Canvas iconCanvas = new Canvas(iconBitmap);
    if (balise != null)
    {
      // Validation
      final WindIconInfos windInfos = new WindIconInfos();
      DrawingCommons.validateWindIconInfos(windInfos, balise, releve);
      final AdditionalWindIconInfos additionalWindInfos = new AdditionalWindIconInfos();
      FullDrawingCommons.validateAdditionalWindIconInfos(additionalWindInfos, balise, releve);
      final WeatherIconInfos weatherInfos = (drawingInfos.displayWeather ? new WeatherIconInfos() : null);
      if (drawingInfos.displayWeather)
      {
        FullDrawingCommons.validateWeatherIconInfos(weatherInfos, windInfos, balise, releve);
      }

      // Dessin
      if (drawingInfos.displayWeather)
      {
        FullDrawingCommons.drawWeatherIcon(iconCanvas, center, weatherInfos);
      }
      final Paint fillPaint = (windInfos.isWindLimitOk() ? FullDrawingCommons.paintSecteurVariationVentVertClair : FullDrawingCommons.paintSecteurVariationVentRougeClair);
      final Paint strokePaint = (windInfos.isWindLimitOk() ? FullDrawingCommons.paintSecteurVariationVentVertStroke : FullDrawingCommons.paintSecteurVariationVentRougeStroke);
      FullDrawingCommons.drawAdditionalWindIcon(iconCanvas, center, additionalWindInfos, fillPaint, strokePaint);
      DrawingCommons.drawWindIcon(iconCanvas, center, windInfos);
    }

    // Widgets icones
    final int iconImageId = context.getResources().getIdentifier(context.getPackageName() + ID_WIDGET_ICON + index, null, null);
    views.setBitmap(iconImageId, METHOD_SET_IMAGE_BITMAP, iconBitmap);
    views.setOnClickPendingIntent(iconImageId, mainPendingIntent);
  }

  /**
   * 
   * @param context
   * @param providersService
   * @param providerKey
   */
  public static void synchronizeWidgets(final Context context, final IFullProvidersService providersService, final String providerKey)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), ">>> synchronizeWidgets(..., " + providersService + ")");

    // Verification
    if (providersService == null)
    {
      return;
    }

    // Initialisations
    final int[] appWidgetIds = getAllAppWidgetIds(context);

    if ((appWidgetIds != null) && (appWidgetIds.length > 0))
    {
      // Initialisations
      init(context);
      final Resources resources = context.getResources();
      final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
      DrawingCommons.updatePreferences(resources, sharedPreferences);

      // Pour chaque widget
      for (final int appWidgetId : appWidgetIds)
      {
        // Synchro
        synchronizeWidget(context, appWidgetId, providersService, resources, sharedPreferences, providerKey);
      }
    }
  }

  /**
   * 
   * @param context
   * @return
   */
  public static int[] getAllAppWidgetIds(final Context context)
  {
    // Initialisations
    final int[] appWidgetIds11 = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, BalisesWidget11.class));
    final int[] appWidgetIds21 = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, BalisesWidget21.class));
    final int[] appWidgetIds22 = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, BalisesWidget22.class));
    final int[] appWidgetIds33 = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, BalisesWidget33.class));
    final int[] appWidgetIds41 = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, BalisesWidget41.class));
    final int[] appWidgetIds42 = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, BalisesWidget42.class));
    final int[] appWidgetIds43 = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, BalisesWidget43.class));
    final int[] appWidgetIds44 = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, BalisesWidget44.class));
    final int size = appWidgetIds11.length + appWidgetIds21.length + +appWidgetIds22.length + appWidgetIds33.length + appWidgetIds41.length + appWidgetIds42.length + appWidgetIds43.length + appWidgetIds44.length;
    final int[] appWidgetIds = new int[size];
    int index = 0;

    // 1x1
    for (int i = 0; i < appWidgetIds11.length; i++, index++)
    {
      appWidgetIds[index] = appWidgetIds11[i];
    }

    // 2x1
    for (int i = 0; i < appWidgetIds21.length; i++, index++)
    {
      appWidgetIds[index] = appWidgetIds21[i];
    }

    // 2x2
    for (int i = 0; i < appWidgetIds22.length; i++, index++)
    {
      appWidgetIds[index] = appWidgetIds22[i];
    }

    // 3x3
    for (int i = 0; i < appWidgetIds33.length; i++, index++)
    {
      appWidgetIds[index] = appWidgetIds33[i];
    }

    // 4x1
    for (int i = 0; i < appWidgetIds41.length; i++, index++)
    {
      appWidgetIds[index] = appWidgetIds41[i];
    }

    // 4x2
    for (int i = 0; i < appWidgetIds42.length; i++, index++)
    {
      appWidgetIds[index] = appWidgetIds42[i];
    }

    // 4x3
    for (int i = 0; i < appWidgetIds43.length; i++, index++)
    {
      appWidgetIds[index] = appWidgetIds43[i];
    }

    // 4x4
    for (int i = 0; i < appWidgetIds44.length; i++, index++)
    {
      appWidgetIds[index] = appWidgetIds44[i];
    }

    // Tri par widget id
    Arrays.sort(appWidgetIds);

    return appWidgetIds;
  }

  /**
   * 
   * @param context
   * @param providersService
   */
  private static void synchronizeProximityOrAroundWidgets(final Context context, final IFullProvidersService providersService)
  {
    synchronizeLabelOrProximityOrAroundWidgets(context, providersService, true, null, null);
  }

  /**
   * 
   * @param context
   * @param providersService
   * @param label
   * @param providerKey
   */
  public static void synchronizeLabelWidgets(final Context context, final IFullProvidersService providersService, final String label, final String providerKey)
  {
    synchronizeLabelOrProximityOrAroundWidgets(context, providersService, false, label, providerKey);
  }

  /**
   * 
   * @param context
   * @param providersService
   * @param locationMode
   * @param label
   * @param providerKey
   */
  private static void synchronizeLabelOrProximityOrAroundWidgets(final Context context, final IFullProvidersService providersService, final boolean locationMode, final String label, final String providerKey)
  {
    // Pour chaque widget
    final int[] appWidgetIds = getAllAppWidgetIds(context);

    if ((appWidgetIds != null) && (appWidgetIds.length > 0))
    {
      // Initialisations
      boolean initialized = false;
      final Resources resources = context.getResources();
      final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
      final String listeProximityValue = resources.getString(R.string.config_widget_label_value_proximity);
      final String listeAroundValue = resources.getString(R.string.config_widget_label_value_around);

      // Pour chaque widget
      for (final int appWidgetId : appWidgetIds)
      {
        final String listeKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_label_key), appWidgetId);
        final String listeValue = sharedPreferences.getString(listeKey, listeProximityValue);
        final boolean proximityWidget = (listeValue.equals(listeProximityValue));
        final boolean aroundWidget = (listeValue.equals(listeAroundValue));

        if ((locationMode && (proximityWidget || aroundWidget)) || (!locationMode && !proximityWidget && !aroundWidget && (Utils.isStringVide(label) || label.equals(listeValue))))
        {
          if (!initialized)
          {
            init(context);
            DrawingCommons.updatePreferences(resources, sharedPreferences);
            initialized = true;
          }

          synchronizeWidget(context, appWidgetId, providersService, resources, sharedPreferences, providerKey);
        }
      }
    }
  }

  /**
   * 
   * @param context
   * @param proches
   * @param providersService
   */
  public static void onLocationChanged(final Context context, final List<BaliseFavorite> proches, final BaliseFavorite[] autour, final IFullProvidersService providersService)
  {
    // Sauvegarde des balises proches dans les prefs
    setProximityBalises(context, proches);

    // Sauvegarde des balises autour dans les prefs
    setAroundBalises(context, autour);

    // Synchro
    synchronizeProximityOrAroundWidgets(context, providersService);
  }

  /**
   * 
   * @param context
   * @param proches
   */
  private static void setProximityBalises(final Context context, final List<BaliseFavorite> proches)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), "saving proximity balises : " + proches);
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
    final SharedPreferences.Editor editor = sharedPreferences.edit();

    // Sauvegarde du nombre de balises
    final int nbBalises = (proches == null ? -1 : proches.size());
    editor.putInt(PREFS_PROXIMITY_SIZE, nbBalises);

    // Sauvegarde des balises
    if (proches != null)
    {
      for (int i = 0; i < nbBalises; i++)
      {
        editor.putString(PREFS_PROXIMITY_ITEM + i, proches.get(i).toString());
      }
    }

    // Fin
    ActivityCommons.commitPreferences(editor);
  }

  /**
   * 
   * @param context
   * @param proches
   */
  private static void setAroundBalises(final Context context, final BaliseFavorite[] autour)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), "saving around balises : " + (autour == null ? null : Arrays.asList(autour)));
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
    final SharedPreferences.Editor editor = sharedPreferences.edit();

    // Sauvegarde du nombre de balises
    final int nbBalises = (autour == null ? -1 : autour.length);
    editor.putInt(PREFS_AROUND_SIZE, nbBalises);

    // Sauvegarde des balises
    if (autour != null)
    {
      for (int i = 0; i < nbBalises; i++)
      {
        editor.putString(PREFS_AROUND_ITEM + i, autour[i] == null ? PREFS_AROUND_NO_ITEM : autour[i].toString());
      }
    }

    // Fin
    ActivityCommons.commitPreferences(editor);
  }

  /**
   * 
   * @param providersService
   * @param context
   * @return
   */
  private static List<BaliseFavorite> getProximityBalises(final IFullProvidersService providersService, final Context context)
  {
    // Initialisations
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);

    // Recuperation du nombre de balises
    final int nbBalises = sharedPreferences.getInt(PREFS_PROXIMITY_SIZE, 0);

    // Si taille = -1 => pas de source de localisation
    if (nbBalises == -1)
    {
      return null;
    }

    // Chargement des balises
    final List<BaliseFavorite> proches = new ArrayList<BaliseFavorite>();
    for (int i = 0; i < nbBalises; i++)
    {
      final String stringFavorite = sharedPreferences.getString(PREFS_PROXIMITY_ITEM + i, Strings.VIDE);
      if (!Utils.isStringVide(stringFavorite))
      {
        final BaliseFavorite favorite = BaliseFavorite.parseBalise(stringFavorite, providersService.getFavoritesService());
        if (favorite != null)
        {
          proches.add(favorite);
        }
      }
    }

    // Fin
    Log.d(BalisesWidgets.class.getSimpleName(), "loading proximity balises : " + proches);
    return proches;
  }

  /**
   * 
   * @param providersService
   * @param context
   * @return
   */
  private static List<BaliseFavorite> getAroundBalises(final IFullProvidersService providersService, final Context context)
  {
    // Initialisations
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);

    // Recuperation du nombre de balises
    final int nbBalises = sharedPreferences.getInt(PREFS_AROUND_SIZE, 0);

    // Si taille = -1 => pas de source de localisation
    if (nbBalises == -1)
    {
      return null;
    }

    // Initialisation des balises
    final List<BaliseFavorite> autour = new ArrayList<BaliseFavorite>(nbBalises);
    final BaliseFavorite vide = new BaliseFavorite(null, null, providersService.getFavoritesService());
    for (int i = 0; i < nbBalises; i++)
    {
      // Remplissage de la liste
      autour.add(vide);
      autour.set(i, null);
    }

    // Chargement des balises
    for (int i = 0; i < nbBalises; i++)
    {
      // Recherche dans les preferences
      final String stringFavorite = sharedPreferences.getString(PREFS_AROUND_ITEM + i, Strings.VIDE);
      if (!Utils.isStringVide(stringFavorite) && !PREFS_AROUND_NO_ITEM.equals(stringFavorite))
      {
        final BaliseFavorite favorite = BaliseFavorite.parseBalise(stringFavorite, providersService.getFavoritesService());
        if (favorite != null)
        {
          autour.set(secteurToNumBalise(i), favorite);
        }
      }
    }

    // Fin
    Log.d(BalisesWidgets.class.getSimpleName(), "loading around balises : " + autour);
    return autour;
  }

  /**
   * 
   * @param secteur
   * @return
   */
  private static int secteurToNumBalise(final int secteur)
  {
    switch (secteur)
    {
      case 0:
        return 1;
      case 1:
        return 2;
      case 2:
        return 5;
      case 3:
        return 8;
      case 4:
        return 7;
      case 5:
        return 6;
      case 6:
        return 3;
      case 7:
        return 0;
      case 8:
        return 4;
    }

    return -1;
  }

  /**
   * 
   * @param appWidgetId
   * @param resources
   * @param sharedPreferences
   * @return
   */
  private static boolean isProximityWidget(final int appWidgetId, final Resources resources, final SharedPreferences sharedPreferences)
  {
    // Recuperation de la liste
    final String listeKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_label_key), appWidgetId);
    final String listeProximityValue = resources.getString(R.string.config_widget_label_value_proximity);
    final String listeValue = sharedPreferences.getString(listeKey, listeProximityValue);

    return (listeValue.equals(listeProximityValue));
  }

  /**
   * 
   * @param appWidgetId
   * @param resources
   * @param sharedPreferences
   * @return
   */
  protected static boolean isAroundWidget(final int appWidgetId, final Resources resources, final SharedPreferences sharedPreferences)
  {
    // Recuperation de la liste
    final String listeKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_label_key), appWidgetId);
    final String listeAroundValue = resources.getString(R.string.config_widget_label_value_around);
    final String listeValue = sharedPreferences.getString(listeKey, listeAroundValue);

    return (listeValue.equals(listeAroundValue));
  }

  /**
   * 
   * @param context
   * @return
   */
  public static boolean needsLocationUpdates(final Context context)
  {
    // Pour chaque widget
    final int[] appWidgetIds = getAllAppWidgetIds(context);

    if ((appWidgetIds != null) && (appWidgetIds.length > 0))
    {
      // Initialisations
      final Resources resources = context.getResources();
      final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);

      // Pour chaque widget
      for (final int appWidgetId : appWidgetIds)
      {
        if (isProximityWidget(appWidgetId, resources, sharedPreferences) || isAroundWidget(appWidgetId, resources, sharedPreferences))
        {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 
   * @param providersService
   * @param context
   * @param key
   * @return
   */
  public static boolean isBaliseProviderNeeded(final IFullProvidersService providersService, final Context context, final String key)
  {
    // Verification de la liste des balises de chaque widget
    final int[] appWidgetIds = getAllAppWidgetIds(context);

    if ((appWidgetIds != null) && (appWidgetIds.length > 0))
    {
      // Initialisation
      final Resources resources = context.getResources();
      final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
      Log.d(BalisesWidgets.class.getSimpleName(), "isBaliseProviderNeeded()");

      // Pour chaque widget
      for (final int appWidgetId : appWidgetIds)
      {
        // Verification des balises du widget
        if (isBaliseProviderNeeded(providersService, context, key, appWidgetId, resources, sharedPreferences, true))
        {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 
   * @param providersService
   * @param context
   * @param key
   * @param appWidgetId
   * @param resources
   * @param sharedPreferences
   * @param checkForSpeakAsked
   * @return
   */
  private static boolean isBaliseProviderNeeded(final IFullProvidersService providersService, final Context context, final String key, final int appWidgetId, final Resources resources, final SharedPreferences sharedPreferences,
      final boolean checkForSpeakAsked)
  {
    // Recuperation de la liste des balises
    final List<BaliseFavorite> favorites = getBalisesList(providersService, context, appWidgetId, resources, sharedPreferences);

    // Parcours de la liste
    if (favorites != null)
    {
      final int favoritesSize = favorites.size();
      final boolean speakingWidget = isSpeakingWidget(appWidgetId, providersService.isScreenOff(), resources, sharedPreferences);
      final boolean checkForSpeak = checkForSpeakAsked && speakingWidget;

      // Ecran eteint et widget non parlant => pas besoin
      if (providersService.isScreenOff() && !checkForSpeak)
      {
        return false;
      }

      // Nombre de balises a prendre en compte : toute la liste si widget parlant, seulement les balises affichees sinon
      final int nbBalises = (checkForSpeak ? favoritesSize : Math.min(getNbBalises(resources, sharedPreferences, appWidgetId), favoritesSize));
      for (int i = 0; i < nbBalises; i++)
      {
        final BaliseFavorite favorite = favorites.get(i);
        if ((favorite != null) && favorite.getProviderId().equals(key))
        {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * 
   * @param appWidgetId
   * @param screenOff
   * @param resources
   * @param sharedPreferences
   * @return
   */
  private static boolean isSpeakingWidget(final int appWidgetId, final boolean screenOff, final Resources resources, final SharedPreferences sharedPreferences)
  {
    // Initialisations
    final String speakKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_speak_key), appWidgetId);
    final boolean speaking = sharedPreferences.getBoolean(speakKey, Boolean.parseBoolean(resources.getString(R.string.config_widget_speak_default)));
    final String speakBlackKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_speak_black_key), appWidgetId);
    final boolean speakingBlack = sharedPreferences.getBoolean(speakBlackKey, Boolean.parseBoolean(resources.getString(R.string.config_widget_speak_black_default)));

    // Le widget est parlant si configure comme tel (parlant ET (parlant meme si ecran noir OU ecran allume))
    final boolean speakingWidget = speaking && (speakingBlack || !screenOff);
    Log.d(BalisesWidgets.class.getSimpleName(), "isSpeakingWidget(" + appWidgetId + ") : screenOff=" + screenOff + " => " + speakingWidget);

    // Fin
    return speakingWidget;
  }

  /**
   * 
   * @param context
   * @return
   */
  public static boolean existsSpeakBlackWidget(final Context context)
  {
    // Initialisations
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);

    for (final int appWidgetId : getAllAppWidgetIds(context))
    {
      final String speakKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_speak_key), appWidgetId);
      final boolean speaking = sharedPreferences.getBoolean(speakKey, Boolean.parseBoolean(resources.getString(R.string.config_widget_speak_default)));
      final String speakBlackKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_speak_black_key), appWidgetId);
      final boolean speakingBlack = sharedPreferences.getBoolean(speakBlackKey, Boolean.parseBoolean(resources.getString(R.string.config_widget_speak_black_default)));

      if (speaking && speakingBlack)
      {
        return true;
      }
    }

    return false;
  }

  /**
   * 
   * @param context
   * @param providersService
   */
  public static void manageVoiceClient(final Context context, final IFullProvidersService providersService)
  {
    // Verification
    if (providersService == null)
    {
      return;
    }

    // Initialisations
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);

    if (!providersService.isNetworkOff())
    {
      final int[] appWidgetIds = getAllAppWidgetIds(context);
      for (final int appWidgetId : appWidgetIds)
      {
        // Log
        Log.d(BalisesWidgets.class.getSimpleName(), "manageVoiceClient() : appWidgetId=" + appWidgetId);

        // Si le widget est parlant
        if (isSpeakingWidget(appWidgetId, providersService.isScreenOff(), resources, sharedPreferences))
        {
          // Il existe au moins 1 widget parlant => register
          providersService.getVoiceService().registerVoiceClient(VoiceService.WIDGETS_VOICE_CLIENT);
          return;
        }
      }
    }

    // Aucun widget parlant => unregister
    providersService.getVoiceService().unregisterVoiceClient(VoiceService.WIDGETS_VOICE_CLIENT);
  }

  /**
   * 
   * @param providersService
   * @param context
   * @param appWidgetId
   * @param resources
   * @param sharedPreferences
   * @return
   */
  private static List<BaliseFavorite> getBalisesList(final IFullProvidersService providersService, final Context context, final int appWidgetId, final Resources resources, final SharedPreferences sharedPreferences)
  {
    // Recuperation de la liste des balises de proximite
    if (isProximityWidget(appWidgetId, resources, sharedPreferences))
    {
      // Recuperation de la liste des balises
      return getProximityBalises(providersService, context);
    }

    // Recuperation de la liste des balises de proximite
    if (isAroundWidget(appWidgetId, resources, sharedPreferences))
    {
      // Recuperation de la liste des balises
      return getAroundBalises(providersService, context);
    }

    // Recuperation du nom de la liste
    final String listeKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_label_key), appWidgetId);
    final String listeProximityValue = resources.getString(R.string.config_widget_label_value_proximity);
    final String listeValue = sharedPreferences.getString(listeKey, listeProximityValue);

    // Mode libelle
    return providersService.getFavoritesService().getBalisesForLabel(listeValue);
  }

  /**
   * 
   * @param resources
   * @param sharedPreferences
   * @param appWidgetId
   * @return
   */
  private static int getWidgetType(final Resources resources, final SharedPreferences sharedPreferences, final int appWidgetId)
  {
    return sharedPreferences.getInt(AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_type_key), appWidgetId), -1);
  }

  /**
   * 
   * @param resources
   * @param sharedPreferences
   * @param appWidgetId
   * @return
   */
  private static int getNbBalises(final Resources resources, final SharedPreferences sharedPreferences, final int appWidgetId)
  {
    final int widgetType = getWidgetType(resources, sharedPreferences, appWidgetId);

    switch (widgetType)
    {
      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_11:
        return NB_BALISES_11;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_21:
        return NB_BALISES_21;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_22:
        return NB_BALISES_22;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_33:
        return NB_BALISES_33;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_41:
        return NB_BALISES_41;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_42:
        return NB_BALISES_42;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_43:
        return NB_BALISES_43;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_44:
        return NB_BALISES_44;

      default:
        return -1;
    }
  }

  /**
   * 
   * @param resources
   * @param sharedPreferences
   * @param appWidgetId
   * @return
   */
  private static String getMessageUpdateInProgress(final Resources resources, final SharedPreferences sharedPreferences, final int appWidgetId)
  {
    final int widgetType = getWidgetType(resources, sharedPreferences, appWidgetId);

    switch (widgetType)
    {
      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_11:
        return MESSAGE_UPDATE_IN_PROGRESS_SHORT;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_21:
        return MESSAGE_UPDATE_IN_PROGRESS_SHORT;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_22:
        return MESSAGE_UPDATE_IN_PROGRESS_SHORT;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_33:
        return MESSAGE_UPDATE_IN_PROGRESS_SHORT;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_41:
        return MESSAGE_UPDATE_IN_PROGRESS;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_42:
        return MESSAGE_UPDATE_IN_PROGRESS;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_43:
        return MESSAGE_UPDATE_IN_PROGRESS;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_44:
        return MESSAGE_UPDATE_IN_PROGRESS;

      default:
        return Strings.VIDE;
    }
  }

  /**
   * 
   * @param resources
   * @param sharedPreferences
   * @param appWidgetId
   * @return
   */
  private static String getMessageNoLocalization(final Resources resources, final SharedPreferences sharedPreferences, final int appWidgetId)
  {
    final int widgetType = getWidgetType(resources, sharedPreferences, appWidgetId);

    switch (widgetType)
    {
      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_11:
        return MESSAGE_NO_LOCALISATION_SHORT;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_21:
        return MESSAGE_NO_LOCALISATION_SHORT;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_22:
        return MESSAGE_NO_LOCALISATION_SHORT;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_33:
        return MESSAGE_NO_LOCALISATION_SHORT;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_41:
        return MESSAGE_NO_LOCALISATION_SHORT;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_42:
        return MESSAGE_NO_LOCALISATION;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_43:
        return MESSAGE_NO_LOCALISATION;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_44:
        return MESSAGE_NO_LOCALISATION;

      default:
        return Strings.VIDE;
    }
  }

  /**
   * 
   * @param resources
   * @param sharedPreferences
   * @param appWidgetId
   * @return
   */
  private static int getWidgetLayoutId(final Resources resources, final SharedPreferences sharedPreferences, final int appWidgetId)
  {
    final int widgetType = getWidgetType(resources, sharedPreferences, appWidgetId);

    switch (widgetType)
    {
      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_11:
        return R.layout.balises_widget_11_layout;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_21:
        return R.layout.balises_widget_21_layout;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_22:
        return R.layout.balises_widget_22_layout;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_33:
        return R.layout.balises_widget_33_layout;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_41:
        return R.layout.balises_widget_41_layout;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_42:
        return R.layout.balises_widget_42_layout;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_43:
        return R.layout.balises_widget_43_layout;

      case AbstractBalisesWidgetPreferencesActivity.WIDGET_TYPE_44:
        return R.layout.balises_widget_44_layout;

      default:
        return -1;
    }
  }

  /**
   * 
   * @param context
   * @param key
   * @param provider
   * @param providersService
   */
  @SuppressWarnings("unused")
  public static void onBaliseProviderAdded(final Context context, final String key, final BaliseProvider provider, final IFullProvidersService providersService)
  {
    // Initialisation
    Log.d(BalisesWidgets.class.getSimpleName(), "onBaliseProviderAdded()");

    // Synchro
    synchronizeWidgets(context, providersService, key);
  }

  /**
   * 
   * @param context
   * @param key
   * @param provider
   * @param providersService
   */
  @SuppressWarnings("unused")
  public static void onBaliseProviderRemoved(final Context context, final String key, final BaliseProvider provider, final IFullProvidersService providersService)
  {
    // Initialisation
    Log.d(BalisesWidgets.class.getSimpleName(), "onBaliseProviderRemoved()");

    // Synchro
    synchronizeWidgets(context, providersService, key);
  }

  /**
   * 
   * @param context
   * @param key
   * @param provider
   * @param infos
   * @param providersService
   */
  @SuppressWarnings("unused")
  public static void onBaliseProviderUpdateEnded(final Context context, final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final IFullProvidersService providersService)
  {
    // Initialisation
    Log.d(BalisesWidgets.class.getSimpleName(), "onBaliseProviderUpdateEnded()");

    // Synchro
    synchronizeWidgets(context, providersService, key);
  }

  /**
   * 
   * @param context
   * @param key
   * @param provider
   * @param infos
   * @param providersService
   */
  @SuppressWarnings("unused")
  public static void onBaliseProviderUpdateStarted(final Context context, final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final IFullProvidersService providersService)
  {
    // Initialisation
    Log.d(BalisesWidgets.class.getSimpleName(), "onBaliseProviderUpdateStarted()");
  }

  /**
   * 
   * @param context
   * @param key
   * @param provider
   * @param providersService
   */
  @SuppressWarnings("unused")
  public static void onBalisesUpdate(final Context context, final String key, final BaliseProvider provider, final IFullProvidersService providersService)
  {
    // Initialisation
    Log.d(BalisesWidgets.class.getSimpleName(), "onBalisesUpdate()");

    // MAJ faite dans onBaliseProviderUpdateEnded
  }

  /**
   * 
   * @param context
   * @param key
   * @param provider
   * @param providersService
   */
  @SuppressWarnings("unused")
  public static void onRelevesUpdate(final Context context, final String key, final BaliseProvider provider, final IFullProvidersService providersService)
  {
    // Initialisation
    Log.d(BalisesWidgets.class.getSimpleName(), "onRelevesUpdate()");

    // MAJ faite dans onBaliseProviderUpdateEnded
  }

  /**
   * 
   * @param context
   * @param providersService
   */
  @SuppressWarnings("unused")
  public static void onServiceStartCompleted(final Context context, final IFullProvidersService providersService)
  {
    // Initialisation
    Log.d(BalisesWidgets.class.getSimpleName(), "onServiceStartCompleted()");

    // Demarrage des Threads si besoin
    providersService.notifyNeededBaliseProvidersChanged(true);
  }

  /**
   * 
   * @param context
   * @param providersService
   * @param label
   */
  public static void onLabelRemoved(final Context context, final IFullProvidersService providersService, final String label)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), ">>> onLabelRemoved(..., " + providersService + ", " + label + ")");

    // Pour chaque widget
    final int[] appWidgetIds = getAllAppWidgetIds(context);

    if ((appWidgetIds != null) && (appWidgetIds.length > 0))
    {
      // Initialisations
      boolean initialized = false;
      final Resources resources = context.getResources();
      final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
      final String listeProximityValue = resources.getString(R.string.config_widget_label_value_proximity);
      final String listeAroundValue = resources.getString(R.string.config_widget_label_value_around);

      // Pour chaque widget
      for (final int appWidgetId : appWidgetIds)
      {
        final String listeKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_label_key), appWidgetId);
        final String listeValue = sharedPreferences.getString(listeKey, listeProximityValue);
        final boolean proximityWidget = (listeValue.equals(listeProximityValue));
        final boolean aroundWidget = (listeValue.equals(listeAroundValue));

        if (!proximityWidget && !aroundWidget && label.equals(listeValue))
        {
          if (!initialized)
          {
            init(context);
            DrawingCommons.updatePreferences(resources, sharedPreferences);
            initialized = true;
          }

          // Passage sur le premier label
          final Editor editor = sharedPreferences.edit();
          editor.putString(listeKey, providersService.getFavoritesService().getLabels().get(0));
          ActivityCommons.commitPreferences(editor);

          // MAJ du widget
          synchronizeWidget(context, appWidgetId, providersService, resources, sharedPreferences, null);
        }
      }
    }
  }

  /**
   * 
   * @param context
   * @param providersService
   * @param from
   * @param to
   */
  public static void onLabelRenamed(final Context context, final IFullProvidersService providersService, final String from, final String to)
  {
    // Initialisations
    Log.d(BalisesWidgets.class.getSimpleName(), ">>> onLabelRenamed(..., " + providersService + ", " + from + ", " + to + ")");

    // Pour chaque widget
    final int[] appWidgetIds = getAllAppWidgetIds(context);

    if ((appWidgetIds != null) && (appWidgetIds.length > 0))
    {
      // Initialisations
      boolean initialized = false;
      final Resources resources = context.getResources();
      final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(context);
      final String listeProximityValue = resources.getString(R.string.config_widget_label_value_proximity);
      final String listeAroundValue = resources.getString(R.string.config_widget_label_value_around);

      // Pour chaque widget
      for (final int appWidgetId : appWidgetIds)
      {
        final String listeKey = AbstractBalisesWidgetPreferencesActivity.getKeyForWidget(resources.getString(R.string.config_widget_label_key), appWidgetId);
        final String listeValue = sharedPreferences.getString(listeKey, listeProximityValue);
        final boolean proximityWidget = (listeValue.equals(listeProximityValue));
        final boolean aroundWidget = (listeValue.equals(listeAroundValue));

        if (!proximityWidget && !aroundWidget && from.equals(listeValue))
        {
          if (!initialized)
          {
            init(context);
            DrawingCommons.updatePreferences(resources, sharedPreferences);
            initialized = true;
          }

          // Passage sur le premier label
          final Editor editor = sharedPreferences.edit();
          editor.putString(listeKey, to);
          ActivityCommons.commitPreferences(editor);

          // MAJ du widget
          synchronizeWidget(context, appWidgetId, providersService, resources, sharedPreferences, null);
        }
      }
    }
  }

  /**
   * 
   * @param inLicensed
   */
  public static void setLicensed(final boolean inLicensed)
  {
    licensed = inLicensed;
  }

  /**
   * 
   * @return
   */
  protected abstract String getWidgetTypeName();
}
