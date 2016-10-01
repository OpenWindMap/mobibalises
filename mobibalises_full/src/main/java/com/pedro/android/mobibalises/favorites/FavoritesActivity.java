package com.pedro.android.mobibalises.favorites;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.BalisesExceptionHandler;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.analytics.AnalyticsService;
import org.pedro.android.mobibalises_common.map.AbstractBalisesMapActivity;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService;
import org.pedro.android.mobibalises_common.service.AbstractProvidersService.BaliseProviderMode;
import org.pedro.android.mobibalises_common.service.BaliseProviderInfos;
import org.pedro.android.mobibalises_common.service.BaliseProviderListener;
import org.pedro.android.mobibalises_common.service.ProvidersServiceBinder;
import org.pedro.android.mobibalises_common.view.BaliseDrawable;
import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.android.mobibalises_common.view.WindIconInfos;
import org.pedro.android.widget.DragDropListView;
import org.pedro.android.widget.DragDropListView.DragDropListener;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.utils.ThreadUtils;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.pedro.android.mobibalises.FullActivityCommons;
import com.pedro.android.mobibalises.FullActivityCommons.FavoriteLabelChooserListener;
import com.pedro.android.mobibalises.FullActivityCommons.FavoritesChooserListener;
import com.pedro.android.mobibalises.FullActivityCommons.FavoritesLabelsChooserListener;
import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.alarm.AlarmUtils;
import com.pedro.android.mobibalises.alarm.AlarmsFragmentActivity;
import com.pedro.android.mobibalises.favorites.FavoritesService.LabelListener;
import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.service.MobibalisesLicenseChecker;
import com.pedro.android.mobibalises.service.ProvidersService;
import com.pedro.android.mobibalises.view.AdditionalWindIconInfos;
import com.pedro.android.mobibalises.view.FullDrawingCommons;
import com.pedro.android.mobibalises.view.WeatherIconInfos;
import com.pedro.android.mobibalises.voice.VoiceService;
import com.pedro.android.mobibalises.widget.BalisesWidgets;

/**
 * 
 * @author pedro.m
 */
public class FavoritesActivity extends ActionBarActivity implements SharedPreferences.OnSharedPreferenceChangeListener, BaliseProviderListener, FavoriteLabelChooserListener, OnItemClickListener, DragDropListener, LabelListener,
    FavoritesLabelsChooserListener, FavoritesChooserListener, LicenseCheckerCallback, ActivityCommons.LocationSettingsDialogListener
{
  private static final String            FORMAT_CONTEXT_MENU_TITLE         = "{0} ({1})";

  private static final String            HTML_DATE_PREFIX                  = "<b>";
  private static final String            HTML_DATE_SUFFIX                  = "</b>";
  private static final String            HTML_DISTANCE_PREFIX              = "&nbsp;&nbsp;";
  private static final String            HTML_BEARING_PREFIX               = "&nbsp;&nbsp;";

  private static final String            DISTANCE_FORMAT_INF10             = "[{0,number,0.0} {1}]";
  private static final String            DISTANCE_FORMAT_SUP10             = "[{0,number,0} {1}]";

  private static final String            BEARING_FORMAT                    = "({0,number,0}{1})";

  private static final String            STRING_TROIS_TIRETS               = "---";

  private static DateFormat              DATE_FORMAT;
  private static DateFormat              HEURE_FORMAT;
  private static String                  LABEL_INACTIVE;
  private static String                  LABEL_AUCUN_RELEVE;
  private static String                  MESSAGE_FORMAT_NOM;
  private static String                  MESSAGE_FORMAT_DIR_INST;
  private static String                  MESSAGE_FORMAT_HEURE_VENT_MAX;
  private static DateFormat              HEURE_VENT_MAX_FORMAT;
  private static String                  MESSAGE_FORMAT_VENT;
  private static NumberFormat            DECIMAL_FORMAT_ENTIER;
  private static NumberFormat            DECIMAL_FORMAT_1_DECIMALE;
  private static String[]                LABELS_PLUIES;

  // Contexte et resources
  Resources                              resources;
  SharedPreferences                      sharedPreferences;

  // Gestion des erreurs
  private BalisesExceptionHandler        exceptionHandler;

  // Service
  private ProvidersServiceConnection     providersServiceConnection;
  IFullProvidersService                  fullProvidersService;

  // Preferences
  private boolean                        backKeyDouble                     = true;
  private boolean                        preferencesLaunched               = false;

  // Gestion du message
  private StatusMessageHandler           statusMessageHandler;

  // Gestion du titre
  private CurrentDisplayedLabelHandler   currentDisplayedLabelHandler;

  // Liste des favoris
  boolean                                proximityMode;
  String                                 currentChoosenLabel;
  private boolean                        flightModeAsked;
  BalisesFavoritesAdapter                balisesFavoritesAdapter;
  final List<IconView>                   balisesFavoritesViews             = new ArrayList<IconView>();
  private final List<String>             neededProviders                   = new ArrayList<String>();

  // Gestion du refraichissement
  private RefreshMessageHandler          refreshMessageHandler;
  private static final int               REFRESH_BALISE_PROVIDER_ADDED     = 1;
  private static final int               REFRESH_BALISE_PROVIDER_REMOVED   = 2;
  private static final int               REFRESH_BALISES_UPDATE            = 3;
  private static final int               REFRESH_RELEVES_UPDATE            = 4;
  private static final int               REFRESH_UPDATE_ENDED              = 5;

  protected static final long            FLIGHT_MODE_LOCATION_MIN_TIME     = 120000;
  protected static final float           FLIGHT_MODE_LOCATION_MIN_DISTANCE = 500;

  private static final int               PROGRESS_DIALOG_LOCATION          = 502;
  private static final int               PROGRESS_DIALOG_CHOOSE_FAVORITES  = 503;

  // Localisation
  FavoritesLocationListener              locationListener;
  private LocationHandler                locationHandler;

  // Menu contextuel
  private BaliseFavorite                 contextMenuBalise                 = null;
  private BaliseProvider                 contextMenuBaliseProvider         = null;

  // Affichage, limites et couleurs
  private boolean                        displayInactive;
  private Integer                        moyWindLimit                      = null;
  private Integer                        maxWindLimit                      = null;
  private int                            overLimitColor;
  private int                            underLimitColor;
  private int                            deltaPeremption;
  private long                           deltaPeremptionMs;

  // Mode d'affichage
  int                                    displayMode;
  private int                            compactDisplayMode;
  private int                            mediumDisplayMode;
  private int                            fullDisplayMode;

  // ListView
  private ListView                       listView;

  // Footer
  View                                   footerView;

  // Flag de lancement d'une autre activite
  private boolean                        otherActivityLaunched;

  // License
  private MobibalisesLicenseChecker      licenseChecker;

  // Gestion du dialogue de choix des favoris pour un label
  private ChooseFavoritesForLabelHandler chooseFavoritesForLabelHandler;

  // Liste des Threads
  final List<Thread>                     threads                           = new ArrayList<Thread>();

  // Barre d'action
  private Menu                           optionsMenu;
  MenuItem                               actionBarStatusItem;

  // Gestion splashscreen
  boolean                                showSplash;
  long                                   createTimestamp;

  /**
   * 
   * @author pedro.m
   */
  private static class IconView
  {
    final View                    view;
    final WindIconInfos           windInfos;
    final AdditionalWindIconInfos additionalWindInfos;
    final WeatherIconInfos        weatherInfos;

    /**
     * 
     * @param view
     */
    IconView(final View view)
    {
      this.view = view;
      windInfos = new WindIconInfos();
      additionalWindInfos = new AdditionalWindIconInfos();
      weatherInfos = new WeatherIconInfos();
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class BalisesFavoritesAdapter extends AbstractBalisesFavoritesAdapter
  {
    private final WeakReference<FavoritesActivity> favoritesActivity;

    /**
     * 
     * @param favoritesActivity
     */
    public BalisesFavoritesAdapter(final FavoritesActivity favoritesActivity)
    {
      super(favoritesActivity, R.layout.favorite_item);
      this.favoritesActivity = new WeakReference<FavoritesActivity>(favoritesActivity);
    }

    @Override
    public View getView(final int position, final View view, final ViewGroup parent)
    {
      return favoritesActivity.get().balisesFavoritesViews.get(position).view;
    }

    @Override
    public void publishResults(final List<BaliseFavorite> results)
    {
      favoritesActivity.get().changeBalisesFavorites(results);
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class FavoritesLocationListener extends AbstractFavoritesLocationListener
  {
    private final WeakReference<FavoritesActivity> favoritesActivity;

    /**
     * 
     * @param favoritesActivity
     */
    FavoritesLocationListener(final FavoritesActivity favoritesActivity)
    {
      super();
      this.favoritesActivity = new WeakReference<FavoritesActivity>(favoritesActivity);
    }

    /**
     * 
     * @param location
     */
    private void changeLocation(final Location location)
    {
      // Recuperation des balises proches
      final List<BaliseFavorite> proches = getProximityBalises(favoritesActivity.get().getApplicationContext(), location, favoritesActivity.get().fullProvidersService);

      // Sauvegarde label courant
      final boolean flightMode = FullActivityCommons.isFlightMode(favoritesActivity.get().fullProvidersService);
      favoritesActivity.get().setCurrentDisplayedLabel(favoritesActivity.get().resources.getString(flightMode ? R.string.label_flight_mode : R.string.label_proximity));

      // MAJ des balises de la liste
      favoritesActivity.get().changeNonFilteredBalisesFavorites(proches);
    }

    /**
     * 
     */
    void startFlightMode()
    {
      // Label
      favoritesActivity.get().setCurrentDisplayedLabel(favoritesActivity.get().resources.getString(R.string.label_flight_mode));

      // MAJ des balises de la liste
      favoritesActivity.get().changeNonFilteredBalisesFavorites(new ArrayList<BaliseFavorite>());

      // On n'affiche pas le bouton d'ajout de balises
      favoritesActivity.get().footerView.setVisibility(View.GONE);
    }

    @Override
    public void onLocationChanged(final Location location)
    {
      // Scroll interdit sur la liste
      favoritesActivity.get().getFavoritesListView().setDraggable(false);

      // On n'affiche pas le bouton d'ajout de balises
      favoritesActivity.get().footerView.setVisibility(View.GONE);

      // Mise a jour de la position de la carte
      changeLocation(location);

      // On cache la boite de dialogue de progression
      hideLocationProgressDialog();
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
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onCreate()");
    super.onCreate(savedInstanceState);

    // Avant tout !
    exceptionHandler = ActivityCommons.initExceptionHandler(getApplicationContext());

    // Initialisations communes
    initCommons();

    // Initialisations de base
    resources = getResources();

    // Splash screen
    createTimestamp = System.currentTimeMillis();
    showSplash = ActivityCommons.manageSplash(this, R.layout.splash_favorites, R.layout.favorites);

    // Initialisation statique
    staticInit(getApplicationContext());

    // FooterView
    final DragDropListView<BaliseFavorite> innerListView = getFavoritesListView();
    footerView = getLayoutInflater().inflate(R.layout.favorites_footer, null);
    innerListView.addFooterView(footerView);

    // Adapteur
    balisesFavoritesAdapter = new BalisesFavoritesAdapter(this);
    setListAdapter(balisesFavoritesAdapter);

    // EmptyView
    innerListView.setDragDropListener(this);
    innerListView.setOnItemClickListener(this);
    final View emptyView = findViewById(R.id.favorites_empty_view);
    innerListView.setEmptyView(emptyView);
    registerForContextMenu(innerListView);

    // Barre d'action
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    // Initialisations
    init();

    // License
    if (FullActivityCommons.needsLicenseCheck(getApplicationContext()))
    {
      licenseChecker = FullActivityCommons.initLicenseChecker(getApplicationContext(), this);
      licenseChecker.checkAccess();
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
  }

  /**
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  DragDropListView<BaliseFavorite> getFavoritesListView()
  {
    return (DragDropListView<BaliseFavorite>)getListView();
  }

  /**
   * 
   * @param context
   */
  private static void staticInit(final Context context)
  {
    // Initialisations
    final Resources resources = context.getResources();

    // Formats date/heure
    DATE_FORMAT = android.text.format.DateFormat.getDateFormat(context);
    HEURE_FORMAT = android.text.format.DateFormat.getTimeFormat(context);
    HEURE_VENT_MAX_FORMAT = android.text.format.DateFormat.getTimeFormat(context);

    // Labels et messages
    LABEL_INACTIVE = resources.getString(R.string.label_favorite_inactive);
    LABEL_AUCUN_RELEVE = resources.getString(R.string.label_favorite_no_data);
    MESSAGE_FORMAT_NOM = resources.getString(R.string.message_format_favorite_name);
    MESSAGE_FORMAT_DIR_INST = resources.getString(R.string.message_format_favorite_instant_direction);
    MESSAGE_FORMAT_HEURE_VENT_MAX = resources.getString(R.string.message_format_favorite_max_wind_hour);
    MESSAGE_FORMAT_VENT = resources.getString(R.string.message_format_favorite_vent);
    DECIMAL_FORMAT_ENTIER = new DecimalFormat(resources.getString(R.string.number_format_favorite_entier));
    DECIMAL_FORMAT_1_DECIMALE = new DecimalFormat(resources.getString(R.string.number_format_favorite_1_decimale));
    LABELS_PLUIES = resources.getStringArray(R.array.label_map_rains);
  }

  /**
   * 
   * @param favorites
   */
  void changeNonFilteredBalisesFavorites(final List<BaliseFavorite> favorites)
  {
    balisesFavoritesAdapter.currentNonFilteredBalisesFavorites = favorites;
    balisesFavoritesAdapter.getFilter().filter(getListView().getTextFilter());
  }

  /**
   * 
   * @param favorites
   */
  void changeBalisesFavorites(final List<BaliseFavorite> inFavorites)
  {
    // Initialisations
    final LayoutInflater inflater = getLayoutInflater();
    balisesFavoritesAdapter.clear();

    // Copie
    final List<BaliseFavorite> favorites = new ArrayList<BaliseFavorite>();
    favorites.addAll(inFavorites);

    // Filtrage et ajout
    int balisesCount = 0;
    synchronized (favorites)
    {
      synchronized (neededProviders)
      {
        neededProviders.clear();
        for (final BaliseFavorite favorite : favorites)
        {
          if (filterBaliseFavorite(favorite))
          {
            // Adapter
            balisesFavoritesAdapter.add(favorite);

            // View
            balisesCount++;
            if (balisesCount > balisesFavoritesViews.size())
            {
              final View view = inflater.inflate(R.layout.favorite_item, getListView(), false);
              balisesFavoritesViews.add(new IconView(view));
            }

            // Liste des providers utilises
            if (!neededProviders.contains(favorite.getProviderId()))
            {
              neededProviders.add(favorite.getProviderId());
            }
          }
        }
      }
    }

    // Suppression des elements en trop
    for (int i = balisesFavoritesAdapter.getCount() - 1; i >= balisesCount; i--)
    {
      // Adapter
      final BaliseFavorite favori = balisesFavoritesAdapter.getItem(i);
      balisesFavoritesAdapter.remove(favori);

      // View
      balisesFavoritesViews.remove(i);
    }

    // Synchronisation
    synchronizeBalisesFavoritesData();

    // Notification au Service que les providers necessaires ont peut-etre change
    fullProvidersService.notifyNeededBaliseProvidersChanged(false);
  }

  /**
   * 
   * @param favorite
   * @return
   */
  private boolean filterBaliseFavorite(final BaliseFavorite favorite)
  {
    // Favorite OK ?
    if (favorite == null)
    {
      return false;
    }

    // Service OK ?
    if (fullProvidersService == null)
    {
      return false;
    }

    // Initialisations
    final BaliseProvider provider = fullProvidersService.getBaliseProvider(favorite.getProviderId());

    // Provider actif ?
    if (provider == null)
    {
      return false;
    }

    // Balise existante ?
    final Balise balise = provider.getBaliseById(favorite.getBaliseId());
    if (balise == null)
    {
      return false;
    }

    return true;
  }

  /**
   * 
   * @return
   */
  protected boolean isProximityMode()
  {
    return proximityMode;
  }

  /**
   * 
   * @param value
   * @param limite
   * @param format
   * @param tendance
   * @return
   */
  private String formatDouble(final double value, final Integer limite, final NumberFormat format, final String tendance)
  {
    return ActivityCommons.formatDouble(value, limite, format, tendance, underLimitColor, overLimitColor);
  }

  /**
   * 
   * @param itemView
   * @param viewId
   * @param favorite
   * @return
   */
  private static View retrieveViewAndTag(final View itemView, final int viewId, final BaliseFavorite favorite)
  {
    final View view = itemView.findViewById(viewId);
    view.setTag(favorite);

    return view;
  }

  /**
   * 
   */
  void synchronizeBalisesFavoritesData()
  {
    // Pour chaque favori
    for (int i = 0; i < balisesFavoritesAdapter.getCount(); i++)
    {
      // Initialisations
      final BaliseFavorite favorite = balisesFavoritesAdapter.getItem(i);
      final BaliseProvider provider = fullProvidersService.getBaliseProvider(favorite.getProviderId());
      final Balise balise = provider.getBaliseById(favorite.getBaliseId());
      final Releve releve = provider.getReleveById(favorite.getBaliseId());
      final IconView iconView = balisesFavoritesViews.get(i);
      iconView.view.setTag(favorite);

      // Synchro
      synchronizeBaliseFavoriteData(iconView, favorite, balise, releve);
    }

    // Repaint
    getListView().postInvalidate();

    // Gestion du message de statut
    manageStatusMessage();
  }

  /**
   * 
   * @param iconView
   * @param favorite
   * @param balise
   * @param releve
   */
  private void synchronizeBaliseFavoriteData(final IconView iconView, final BaliseFavorite favorite, final Balise balise, final Releve releve)
  {
    // Initialisations
    TextView textView;
    final View itemView = iconView.view;
    final boolean baliseActive = (balise != null) && !Utils.isBooleanNull(balise.active) && Utils.getBooleanValue(balise.active);
    final boolean releveValide = baliseActive && (releve != null);

    // Icone
    final FavoritesIconView drawingView = (FavoritesIconView)retrieveViewAndTag(itemView, R.id.fav_item_drawing, favorite);
    if (displayMode == compactDisplayMode)
    {
      drawingView.setVisibility(View.GONE);
    }
    else
    {
      drawingView.setBaliseDatas(iconView.windInfos, iconView.additionalWindInfos, iconView.weatherInfos, balise, releve);
      drawingView.setVisibility(View.VISIBLE);
    }

    // Nom balise
    final StringBuilder nom = new StringBuilder();
    if (balise == null)
    {
      nom.append(STRING_TROIS_TIRETS);
    }
    else
    {
      nom.append(MessageFormat.format(MESSAGE_FORMAT_NOM, balise.nom));

      // Mode complet : distance et altitude
      if (displayMode == fullDisplayMode)
      {
        // Distance
        if (favorite.getDistance() != null)
        {
          nom.append(HTML_DISTANCE_PREFIX);
          final Double finalDistance = Double.valueOf(ActivityCommons.getFinalDistance(favorite.getDistance().doubleValue()));
          final String format = (finalDistance.doubleValue() >= 10 ? DISTANCE_FORMAT_SUP10 : DISTANCE_FORMAT_INF10);
          nom.append(MessageFormat.format(format, finalDistance, ActivityCommons.getDistanceUnit()));
        }

        // Cap
        if (favorite.getBearing() != null)
        {
          nom.append(HTML_BEARING_PREFIX);
          nom.append(MessageFormat.format(BEARING_FORMAT, favorite.getBearing(), resources.getString(R.string.unit_bearing)));
        }
      }
      // Mode medium : heure de MAJ (seulement l'heure pour laisser la place a l'icone)
      else if ((displayMode == mediumDisplayMode) && releveValide && (releve.date != null))
      {
        nom.append(HTML_DISTANCE_PREFIX);
        nom.append(HEURE_FORMAT.format(new Date(Utils.fromUTC(releve.date.getTime()))));
      }
      // Mode compact : date et heure de MAJ (place dispo car icone non affichee)
      else if ((displayMode == compactDisplayMode) && releveValide && (releve.date != null))
      {
        final Date date = new Date(Utils.fromUTC(releve.date.getTime()));
        nom.append(HTML_DISTANCE_PREFIX);
        nom.append(DATE_FORMAT.format(date));
        nom.append(Strings.CHAR_SPACE);
        nom.append(HEURE_FORMAT.format(date));
      }
    }
    textView = (TextView)retrieveViewAndTag(itemView, R.id.fav_item_name, favorite);
    textView.setText(Html.fromHtml(nom.toString()));

    // Date/Heure
    textView = (TextView)retrieveViewAndTag(itemView, R.id.fav_item_date_time, favorite);
    if (balise == null)
    {
      textView.setVisibility(View.INVISIBLE);
    }
    else if (displayMode != fullDisplayMode)
    {
      textView.setVisibility(View.GONE);
    }
    else
    {
      textView.setVisibility(View.VISIBLE);
      final StringBuilder dateBuffer = new StringBuilder();

      // Date / statut du releve
      dateBuffer.append(HTML_DATE_PREFIX);
      if (releveValide && (releve.date != null))
      {
        final Date date = new Date(Utils.fromUTC(releve.date.getTime()));
        dateBuffer.append(DATE_FORMAT.format(date));
        dateBuffer.append(Strings.CHAR_SPACE);
        dateBuffer.append(HEURE_FORMAT.format(date));
      }
      else if (!Utils.isBooleanNull(balise.active) && Utils.getBooleanValue(balise.active))
      {
        dateBuffer.append(LABEL_AUCUN_RELEVE);
      }
      else
      {
        dateBuffer.append(LABEL_INACTIVE);
      }
      dateBuffer.append(HTML_DATE_SUFFIX);
      textView.setText(Html.fromHtml(dateBuffer.toString()));
    }

    // Donnees vent
    textView = (TextView)retrieveViewAndTag(itemView, R.id.fav_item_wind, favorite);
    if ((balise == null) || (releve == null)
        || ((releve.directionMoyenne == Integer.MIN_VALUE) && (releve.directionInstantanee == Integer.MIN_VALUE) && Double.isNaN(releve.ventMoyen) && Double.isNaN(releve.ventMini) && Double.isNaN(releve.ventMaxi)))
    {
      textView.setVisibility(View.INVISIBLE);
    }
    else
    {
      textView.setVisibility(View.VISIBLE);

      // Direction instantanee
      String dirInst = Strings.VIDE;
      if (DrawingCommons.isDirectionOk(releve.directionInstantanee) && (releve.directionInstantanee != releve.directionMoyenne))
      {
        dirInst = MessageFormat.format(MESSAGE_FORMAT_DIR_INST, DrawingCommons.getLabelDirectionVent(releve.directionInstantanee));
      }

      // Heure de la rafale
      String heureMax = Strings.VIDE;
      if (releve.dateHeureVentMaxi != null)
      {
        heureMax = MessageFormat.format(MESSAGE_FORMAT_HEURE_VENT_MAX, HEURE_VENT_MAX_FORMAT.format(new Date(Utils.fromUTC(releve.dateHeureVentMaxi.getTime()))));
      }

      // Direction moyenne
      final String dirMoy = DrawingCommons.isDirectionOk(releve.directionMoyenne) ? DrawingCommons.getLabelDirectionVent(releve.directionMoyenne) : STRING_TROIS_TIRETS;

      // Tendances
      final String tendanceMini = FullActivityCommons.getStringTendance(BaliseProviderUtils.getTendanceVent(deltaPeremptionMs, releve.date, releve.dateRelevePrecedent, releve.ventMini, releve.ventMiniTendance));
      final String tendanceMoyen = FullActivityCommons.getStringTendance(BaliseProviderUtils.getTendanceVent(deltaPeremptionMs, releve.date, releve.dateRelevePrecedent, releve.ventMoyen, releve.ventMoyenTendance));
      final String tendanceMaxi = FullActivityCommons.getStringTendance(BaliseProviderUtils.getTendanceVent(deltaPeremptionMs, releve.date, releve.dateRelevePrecedent, releve.ventMaxi, releve.ventMaxiTendance));

      // Vent
      textView.setText(Html.fromHtml(MessageFormat.format(MESSAGE_FORMAT_VENT, dirMoy, dirInst, formatDouble(ActivityCommons.getFinalSpeed(releve.ventMini), null, DECIMAL_FORMAT_ENTIER, tendanceMini),
          formatDouble(ActivityCommons.getFinalSpeed(releve.ventMoyen), moyWindLimit, DECIMAL_FORMAT_ENTIER, tendanceMoyen), formatDouble(ActivityCommons.getFinalSpeed(releve.ventMaxi), maxWindLimit, DECIMAL_FORMAT_ENTIER, tendanceMaxi),
          heureMax, ActivityCommons.getSpeedUnit())));
    }

    // Donnees diverses
    textView = (TextView)retrieveViewAndTag(itemView, R.id.fav_item_misc, favorite);
    // Mode complet
    if (displayMode == fullDisplayMode)
    {
      final StringBuilder misc = new StringBuilder();
      boolean first = true;
      final String separator = Strings.TAB;

      // Altitude
      if ((balise != null) && (balise.altitude != Integer.MIN_VALUE))
      {
        misc.append(first ? Strings.VIDE : separator);
        misc.append(resources.getString(R.string.label_favorite_altitude_abv));
        misc.append(Strings.CHAR_SPACE);
        misc.append(ActivityCommons.getFinalAltitude(balise.altitude));
        misc.append(ActivityCommons.getAltitudeUnit());
        first = false;
      }

      if (releve != null)
      {
        // Variation vent
        if ((releve.directionVentVariation1 != Integer.MIN_VALUE) && (releve.directionVentVariation2 != Integer.MIN_VALUE))
        {
          misc.append(first ? Strings.VIDE : separator);
          misc.append(resources.getString(R.string.label_favorite_direction_variation_abv));
          misc.append(Strings.CHAR_SPACE);
          misc.append(releve.directionVentVariation1);
          misc.append(resources.getString(R.string.unit_direction));
          misc.append(Strings.CHAR_MOINS);
          misc.append(releve.directionVentVariation2);
          misc.append(resources.getString(R.string.unit_direction));
          first = false;
        }

        // Temperature
        if (!Double.isNaN(releve.temperature))
        {
          misc.append(first ? Strings.VIDE : separator);
          misc.append(resources.getString(R.string.label_favorite_temperature_abv));
          misc.append(Strings.CHAR_SPACE);
          misc.append(formatDouble(ActivityCommons.getFinalTemperature(releve.temperature), null, DECIMAL_FORMAT_ENTIER, null));
          misc.append(ActivityCommons.getTemperatureUnit());
          first = false;
        }

        // Point de rosee
        if (!Double.isNaN(releve.pointRosee))
        {
          misc.append(first ? Strings.VIDE : separator);
          misc.append(resources.getString(R.string.label_favorite_dew_point_abv));
          misc.append(Strings.CHAR_SPACE);
          misc.append(formatDouble(ActivityCommons.getFinalTemperature(releve.pointRosee), null, DECIMAL_FORMAT_ENTIER, null));
          misc.append(ActivityCommons.getTemperatureUnit());
          first = false;
        }

        // Pluie
        if ((releve.pluie != Integer.MIN_VALUE) && (releve.pluie > 0))
        {
          misc.append(first ? Strings.VIDE : separator);
          misc.append(resources.getString(R.string.label_favorite_rain));
          misc.append(Strings.CHAR_SPACE);
          misc.append(LABELS_PLUIES[releve.pluie]);
          first = false;
        }

        // Hydrometrie
        if (!Double.isNaN(releve.hydrometrie) && (releve.hydrometrie > 0))
        {
          misc.append(first ? Strings.VIDE : separator);
          misc.append(resources.getString(R.string.label_favorite_hydrometry_abv));
          misc.append(Strings.CHAR_SPACE);
          misc.append(formatDouble(releve.hydrometrie, null, DECIMAL_FORMAT_1_DECIMALE, null));
          misc.append(resources.getString(R.string.unit_hydrometry));
          first = false;
        }

        // Nuages
        if (releve.nuages != Integer.MIN_VALUE)
        {
          misc.append(first ? Strings.VIDE : separator);
          misc.append(resources.getString(R.string.label_favorite_clouds));
          misc.append(Strings.CHAR_SPACE);
          misc.append(releve.nuages);
          misc.append(resources.getString(R.string.unit_clouds));
          if (releve.plafondNuages != Integer.MIN_VALUE)
          {
            final int altitudeBalise = ((balise == null || balise.altitude == Integer.MIN_VALUE) ? 0 : balise.altitude);
            misc.append(Strings.CHAR_SPACE);
            misc.append(MessageFormat.format(resources.getString(R.string.label_favorite_clouds_base), Integer.valueOf(ActivityCommons.getFinalAltitude(releve.plafondNuages + altitudeBalise)), ActivityCommons.getAltitudeUnit()));
          }
          if (!Utils.isBooleanNull(releve.nuagesBourgeonnants) && Utils.getBooleanValue(releve.nuagesBourgeonnants))
          {
            misc.append(Strings.CHAR_SPACE);
            misc.append(resources.getString(R.string.label_favorite_clouds_cucb));
          }
          first = false;
        }

        // Pression
        if (!Double.isNaN(releve.pression))
        {
          misc.append(first ? Strings.VIDE : separator);
          misc.append(resources.getString(R.string.label_favorite_pressure_abv));
          misc.append(Strings.CHAR_SPACE);
          misc.append(formatDouble(releve.pression, null, DECIMAL_FORMAT_ENTIER, null));
          misc.append(resources.getString(R.string.unit_pressure));
          first = false;
        }

        // Luminosite
        if (!Utils.isStringVide(releve.luminosite))
        {
          misc.append(first ? Strings.VIDE : separator);
          misc.append(resources.getString(R.string.label_favorite_luminosity_abv));
          misc.append(Strings.CHAR_SPACE);
          misc.append(releve.luminosite);
          first = false;
        }

        // Humidite
        if (releve.humidite != Integer.MIN_VALUE)
        {
          misc.append(first ? Strings.VIDE : separator);
          misc.append(resources.getString(R.string.label_favorite_humidity_abv));
          misc.append(Strings.CHAR_SPACE);
          misc.append(releve.humidite);
          misc.append(resources.getString(R.string.unit_humidity));
          first = false;
        }
      }

      // Donnees diverses
      if (misc.length() > 0)
      {
        textView.setVisibility(View.VISIBLE);
        textView.setText(Html.fromHtml(misc.toString()));
      }
      else
      {
        textView.setVisibility(View.INVISIBLE);
      }
    }
    // Mode medium et compact
    else
    {
      textView.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onStop()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onStop()");
    super.onStop();
    Log.d(getClass().getSimpleName(), "<<< onStop()");
  }

  @Override
  protected void onDestroy()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onDestroy()");
    super.onDestroy();

    // Threads divers
    ThreadUtils.join(threads, true);

    // Notification au service
    providersServiceConnection.privateOnServiceDisconnected();

    // Analytics
    ActivityCommons.trackEvent(fullProvidersService, AnalyticsService.CAT_FAVORITES, AnalyticsService.ACT_FAVORITES_STOP, null);

    // Si fin (pas d'autre activite demandee)
    if (!otherActivityLaunched)
    {
      fullProvidersService.stopSelfIfPossible();
    }

    // Deconnexion et arret du service
    unbindService(providersServiceConnection);

    // Sauvegarde preferences
    final SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(AbstractBalisesPreferencesActivity.CONFIG_FAVORITES_CURRENT_LABEL, currentChoosenLabel);
    editor.putBoolean(AbstractBalisesPreferencesActivity.CONFIG_FAVORITES_PROXIMITY_MODE, proximityMode);
    editor.putInt(AbstractBalisesPreferencesActivity.CONFIG_FAVORITES_DISPLAY_MODE, displayMode);
    ActivityCommons.commitPreferences(editor);

    // License
    if (licenseChecker != null)
    {
      licenseChecker.onDestroy();
    }

    // Gestion de la touche back
    ActivityCommons.finishBackKeyManager();

    // Handlers
    currentDisplayedLabelHandler.removeMessages();
    currentDisplayedLabelHandler = null;
    refreshMessageHandler.removeMessages();
    refreshMessageHandler = null;
    statusMessageHandler.removeMessages();
    statusMessageHandler = null;
    chooseFavoritesForLabelHandler.removeMessages();
    chooseFavoritesForLabelHandler = null;
    locationHandler.removeMessages();
    locationHandler = null;

    // Nettoyages
    cleanUp();

    // Liberation vues
    ActivityCommons.unbindDrawables(findViewById(android.R.id.content));

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  /**
   * 
   */
  private void cleanUp()
  {
    balisesFavoritesAdapter.clear();
    balisesFavoritesViews.clear();
    synchronized (neededProviders)
    {
      neededProviders.clear();
    }
  }

  /**
   * 
   */
  private void initCommons()
  {
    ActivityCommons.init(getApplicationContext());
    FullActivityCommons.init(getApplicationContext());
    FullActivityCommons.removeFlightModeNotification(getApplicationContext());
    FullActivityCommons.removeHistoryModeNotification(getApplicationContext());
    FullActivityCommons.removeSpeakBlackWidgetNotification(getApplicationContext());
    FullActivityCommons.removeAlarmModeNotification(getApplicationContext());
  }

  /**
   * 
   */
  private void init()
  {
    // Graphiques
    initGraphics();

    // Choix des favoris pour un label
    initChooseFavoritesForLabelHandler();

    // Titre
    initCurrentDisplayedLabelHandler();

    // Preferences
    configurePreferences();

    // Gestion du rafraichissement
    initRefeshFavoritesHandler();

    // Message de status
    initStatusMessageHandler();

    // Gestion de la localisation
    initLocationHandler();

    // Demarrage du service
    initProvidersService();

    // Demarrage du service de localisation
    initLocation();
  }

  /**
   * 
   */
  private void initGraphics()
  {
    // Gestion de la densite de pixels de l'ecran
    final Context context = getApplicationContext();
    DrawingCommons.initialize(context);
    FullDrawingCommons.initialize(context);

    // Couleurs
    overLimitColor = resources.getColor(R.color.map_balise_texte_ko);
    underLimitColor = resources.getColor(R.color.map_balise_texte_ok);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class RefreshMessageHandler extends Handler
  {
    private final WeakReference<FavoritesActivity> favoritesActivity;

    /**
     * 
     * @param favoritesActivity
     */
    RefreshMessageHandler(final FavoritesActivity favoritesActivity)
    {
      super(Looper.getMainLooper());
      this.favoritesActivity = new WeakReference<FavoritesActivity>(favoritesActivity);
    }

    @Override
    public void handleMessage(final Message msg)
    {
      switch (msg.what)
      {
        case REFRESH_BALISE_PROVIDER_ADDED:
        case REFRESH_BALISE_PROVIDER_REMOVED:
        case REFRESH_BALISES_UPDATE:
          favoritesActivity.get().changeNonFilteredBalisesFavorites(favoritesActivity.get().balisesFavoritesAdapter.currentNonFilteredBalisesFavorites);
          break;

        case REFRESH_RELEVES_UPDATE:
        case REFRESH_UPDATE_ENDED:
          favoritesActivity.get().synchronizeBalisesFavoritesData();
          break;
      }
    }

    /**
     * 
     */
    public void removeMessages()
    {
      removeMessages(REFRESH_BALISE_PROVIDER_ADDED);
      removeMessages(REFRESH_BALISE_PROVIDER_REMOVED);
      removeMessages(REFRESH_BALISES_UPDATE);
      removeMessages(REFRESH_RELEVES_UPDATE);
      removeMessages(REFRESH_UPDATE_ENDED);
    }
  }

  /**
   * 
   */
  private void initRefeshFavoritesHandler()
  {
    refreshMessageHandler = new RefreshMessageHandler(this);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class StatusMessageHandler extends Handler
  {
    private final WeakReference<FavoritesActivity> favoritesActivity;

    /**
     * 
     * @param favoritesActivity
     */
    StatusMessageHandler(final FavoritesActivity favoritesActivity)
    {
      super(Looper.getMainLooper());
      this.favoritesActivity = new WeakReference<FavoritesActivity>(favoritesActivity);
    }

    @Override
    public void handleMessage(final Message msg)
    {
      // Texte status
      if (favoritesActivity.get().actionBarStatusItem != null)
      {
        final String status = (String)msg.obj;
        favoritesActivity.get().actionBarStatusItem.setVisible(!Utils.isStringVide(status));
        favoritesActivity.get().actionBarStatusItem.setTitle(status);
      }

      // Couleur de fond
      final int actionBarDrawableId;
      switch (msg.arg2)
      {
        case R.color.fav_status_error:
          actionBarDrawableId = R.drawable.favorites_actionbar_error;
          break;
        case R.color.fav_status_warning:
          actionBarDrawableId = R.drawable.favorites_actionbar_warning;
          break;
        default:
          actionBarDrawableId = R.drawable.favorites_actionbar_ok;
          break;
      }
      final Drawable actionBarDrawable = favoritesActivity.get().getResources().getDrawable(actionBarDrawableId);
      favoritesActivity.get().getSupportActionBar().setBackgroundDrawable(actionBarDrawable);
    }

    /**
     * 
     */
    public void removeMessages()
    {
      removeMessages(0);
    }
  }

  /**
   * 
   */
  private void initStatusMessageHandler()
  {
    statusMessageHandler = new StatusMessageHandler(this);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ProvidersServiceConnection implements ServiceConnection
  {
    private final WeakReference<FavoritesActivity> favActivity;

    /**
     * 
     * @param favActivity
     */
    ProvidersServiceConnection(final FavoritesActivity favActivity)
    {
      this.favActivity = new WeakReference<FavoritesActivity>(favActivity);
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder inBinder)
    {
      // Recuperation du service
      favActivity.get().fullProvidersService = (IFullProvidersService)((ProvidersServiceBinder)inBinder).getService();

      // Listeners
      favActivity.get().fullProvidersService.addBaliseProviderListener(favActivity.get(), false);
      favActivity.get().fullProvidersService.notifyNeededBaliseProvidersChanged(false);
      favActivity.get().fullProvidersService.getFavoritesService().addLabelListener(favActivity.get());

      // Notification au service de synthese vocale
      favActivity.get().fullProvidersService.getVoiceService().registerVoiceClient(VoiceService.FAVORITES_VOICE_CLIENT);

      // Activite en premier plan
      favActivity.get().fullProvidersService.setActivityOnForeground(true);

      // Mode vol ?
      final boolean flightMode = FullActivityCommons.isFlightMode(favActivity.get().fullProvidersService);

      // Liste courante
      if (flightMode)
      {
        // Boite de dialogue de progression
        favActivity.get().showLocationProgressDialog();

        // Enregistrement aupres du service de localisation
        final boolean useGps = favActivity.get().sharedPreferences.getBoolean(favActivity.get().resources.getString(R.string.config_flight_mode_use_gps_key),
            Boolean.parseBoolean(favActivity.get().resources.getString(R.string.config_flight_mode_use_gps_default)));
        favActivity.get().fullProvidersService.getFullLocationService().addListener(favActivity.get().locationListener, useGps, FLIGHT_MODE_LOCATION_MIN_TIME, FLIGHT_MODE_LOCATION_MIN_DISTANCE, true);
      }
      else if (favActivity.get().proximityMode)
      {
        // Boite de dialogue de progression
        favActivity.get().showLocationProgressDialog();

        // Mode proximite
        favActivity.get().doLocationAfterServiceStarted();
      }
      else
      {
        // Label
        favActivity.get().onFavoriteLabelChoosed(favActivity.get().currentChoosenLabel);
      }

      // Effacement du splash screen
      if (favActivity.get().showSplash)
      {
        ActivityCommons.hideSplash(favActivity.get(), favActivity.get().createTimestamp);
        favActivity.get().showSplash = false;
      }

      // Google Analytics
      ActivityCommons.trackEvent(favActivity.get().fullProvidersService, AnalyticsService.CAT_FAVORITES, AnalyticsService.ACT_FAVORITES_START, null);
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
      if (favActivity.get().fullProvidersService != null)
      {
        // Localisation
        if (favActivity.get().fullProvidersService.getFullLocationService() != null)
        {
          favActivity.get().fullProvidersService.getFullLocationService().removeListener(favActivity.get().locationListener, true);
          ActivityCommons.endLocation(favActivity.get().fullProvidersService, favActivity.get().locationListener);
        }

        // Synthese vocale
        if (favActivity.get().fullProvidersService.getVoiceService() != null)
        {
          favActivity.get().fullProvidersService.getVoiceService().unregisterVoiceClient(VoiceService.FAVORITES_VOICE_CLIENT);
        }

        // Listeners
        favActivity.get().fullProvidersService.removeBaliseProviderListener(favActivity.get(), true);
        if (favActivity.get().fullProvidersService.getFavoritesService() != null)
        {
          favActivity.get().fullProvidersService.getFavoritesService().removeLabelListener(favActivity.get());
        }
      }
    }
  }

  /**
   * 
   */
  private void initProvidersService()
  {
    // Initialisation connexion
    providersServiceConnection = new ProvidersServiceConnection(this);

    // Connexion au service (et demarrage si besoin)
    final Intent providersServiceIntent = new Intent(getApplicationContext(), ProvidersService.class);
    providersServiceIntent.putExtra(AbstractProvidersService.STARTED_FROM_ACTIVITY, true);
    startService(providersServiceIntent);
    bindService(providersServiceIntent, providersServiceConnection, Context.BIND_AUTO_CREATE);
  }

  /**
   * 
   */
  private void initLocation()
  {
    // Define a listener that responds to location updates
    locationListener = new FavoritesLocationListener(this);
  }

  /**
   * 
   */
  private void configurePreferences()
  {
    // Preferences partagees
    sharedPreferences = ActivityCommons.getSharedPreferences(getApplicationContext());
    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_double_backkey_key));
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_error_reports_key));
    onSharedPreferenceChanged(sharedPreferences, resources.getString(R.string.config_wakelock_key));
    updatePreferences();

    // Label courant et ancien
    currentChoosenLabel = sharedPreferences.getString(AbstractBalisesPreferencesActivity.CONFIG_FAVORITES_CURRENT_LABEL, resources.getString(R.string.label_default_label));
    final boolean flightMode = FullActivityCommons.isFlightMode(fullProvidersService);
    proximityMode = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_FAVORITES_PROXIMITY_MODE, false);
    setCurrentDisplayedLabel(flightMode ? resources.getString(R.string.label_flight_mode) : proximityMode ? resources.getString(R.string.label_proximity) : currentChoosenLabel);

    // Config commune
    DrawingCommons.updatePreferences(resources, sharedPreferences);

    // Marquage de l'activite de demarrage
    ActivityCommons.saveStartActivity(resources.getString(R.string.intent_favorites_action));

    // Display Modes
    compactDisplayMode = Integer.parseInt(resources.getString(R.string.config_balises_list_mode_display_value_compact), 10);
    mediumDisplayMode = Integer.parseInt(resources.getString(R.string.config_balises_list_mode_display_value_medium), 10);
    fullDisplayMode = Integer.parseInt(resources.getString(R.string.config_balises_list_mode_display_value_full), 10);
    displayMode = sharedPreferences.getInt(AbstractBalisesPreferencesActivity.CONFIG_FAVORITES_DISPLAY_MODE, fullDisplayMode);
  }

  /**
   * 
   * @param label
   */
  void setCurrentDisplayedLabel(final String label)
  {
    final Message msg = new Message();
    msg.what = 0;
    msg.obj = label;
    currentDisplayedLabelHandler.sendMessage(msg);
  }

  @Override
  protected void onPause()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onPause()");
    super.onPause();

    // Si pause (pas d'autre activite demandee)
    if (!otherActivityLaunched && !preferencesLaunched)
    {
      FullActivityCommons.addFlightModeNotification(getApplicationContext(), fullProvidersService, false);
      FullActivityCommons.addHistoryModeNotification(getApplicationContext());
      FullActivityCommons.addSpeakBlackWidgetNotification(getApplicationContext());
      FullActivityCommons.addAlarmModeNotification(getApplicationContext());
      if (fullProvidersService != null)
      {
        fullProvidersService.setActivityOnForeground(false);
      }
    }

    // Wakelock
    ActivityCommons.releaseWakeLock();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onPause()");
  }

  @Override
  protected void onStart()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onStart()");
    super.onStart();

    // Affichage des nouveautes de la version
    ActivityCommons.displayStartupMessage(this, -1);

    // Affichage du message FFVL
    final boolean showFfvlAtStartup = sharedPreferences.getBoolean(resources.getString(R.string.config_ffvl_message_key), Boolean.parseBoolean(resources.getString(R.string.config_ffvl_message_default)));
    if (!preferencesLaunched && showFfvlAtStartup)
    {
      ActivityCommons.checkForFfvlMessage(this, AbstractProvidersService.FFVL_KEY, false, false);
    }

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onStart()");
  }

  @Override
  protected void onResume()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onResume()");
    super.onResume();

    // Wakelock
    ActivityCommons.acquireWakeLock();

    // Notification mode vol et historique
    FullActivityCommons.removeFlightModeNotification(getApplicationContext());
    FullActivityCommons.removeHistoryModeNotification(getApplicationContext());
    FullActivityCommons.removeSpeakBlackWidgetNotification(getApplicationContext());
    FullActivityCommons.removeAlarmModeNotification(getApplicationContext());
    if (fullProvidersService != null)
    {
      fullProvidersService.setActivityOnForeground(true);
    }

    // Acquisition position si retour des parametres de localisation (et localisation active)
    if (ActivityCommons.locationSettingsLaunched)
    {
      ActivityCommons.locationSettingsLaunched = false;

      if (fullProvidersService.getFullLocationService().isLocationEnabled())
      {
        // Mode vol ?
        if (flightModeAsked)
        {
          // Flag
          flightModeAsked = false;

          // Boite de dialogue de progression
          showLocationProgressDialog();

          // Activation du mode vol
          toggleFlightMode();

        }
        // Mode proximite
        else
        {
          if (fullProvidersService.getFullLocationService().isLocationEnabled())
          {
            // Localisation
            ActivityCommons.startLocation(this, fullProvidersService, locationListener, false, false, true, PROGRESS_DIALOG_LOCATION, true);
          }
        }
      }
      else
      {
        // Pas de localisation possible, RAZ des flags
        flightModeAsked = false;
        proximityMode = false;
      }
    }

    // Retour des preferences
    if (preferencesLaunched)
    {
      preferencesLaunched = false;
      final boolean preferencesChanged = ActivityCommons.updateUnitPreferences(getApplicationContext(), null, null, null, null) || updatePreferences();
      if (DrawingCommons.updatePreferences(resources, sharedPreferences) || preferencesChanged)
      {
        changeNonFilteredBalisesFavorites(balisesFavoritesAdapter.currentNonFilteredBalisesFavorites);
      }
    }

    // RAZ flag de lancement d'un autre activite
    otherActivityLaunched = false;

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onResume()");
  }

  /**
   * 
   * @return
   */
  private boolean updatePreferences()
  {
    // Initialisations
    boolean preferencesChanged = false;

    // Limite de peremption releve
    try
    {
      final int value = sharedPreferences.getInt(resources.getString(R.string.config_map_outofdate_key), Integer.parseInt(resources.getString(R.string.config_map_outofdate_default), 10));
      if (value != deltaPeremption)
      {
        deltaPeremption = value;
        deltaPeremptionMs = 60000L * deltaPeremption;
        preferencesChanged = true;
      }
    }
    catch (final NumberFormatException nfe)
    {
      Log.e(BaliseDrawable.class.getSimpleName(), nfe.getMessage(), nfe);
    }

    // Affichage des balises inactives
    final boolean newDisplayInactive = !sharedPreferences.getBoolean(resources.getString(R.string.config_map_balise_hide_inactive_key), Boolean.parseBoolean(resources.getString(R.string.config_map_balise_hide_inactive_default)));
    if (newDisplayInactive != displayInactive)
    {
      preferencesChanged = true;
      displayInactive = newDisplayInactive;
    }

    // Limite Vent Moyen
    final boolean moyChecked = sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_moy_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_moy_check_default)));
    if (moyChecked)
    {
      final Integer newMoyWindLimit = Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_map_limit_moy_edit_key), Integer.parseInt(resources.getString(R.string.config_map_limit_moy_edit_default), 10)));
      preferencesChanged = preferencesChanged || (moyWindLimit == null) || (moyWindLimit.intValue() != newMoyWindLimit.intValue());
      moyWindLimit = newMoyWindLimit;
    }
    else
    {
      preferencesChanged = preferencesChanged || (moyWindLimit != null);
      moyWindLimit = null;
    }

    // Limite Vent Maxi
    final boolean maxChecked = sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_max_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_max_check_default)));
    if (maxChecked)
    {
      final Integer newMaxWindLimit = Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_map_limit_max_edit_key), Integer.parseInt(resources.getString(R.string.config_map_limit_max_edit_default), 10)));
      preferencesChanged = preferencesChanged || (maxWindLimit == null) || (maxWindLimit.intValue() != newMaxWindLimit.intValue());
      maxWindLimit = newMaxWindLimit;
    }
    else
    {
      preferencesChanged = preferencesChanged || (maxWindLimit != null);
      maxWindLimit = null;
    }

    return preferencesChanged;
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences inSharedPreferences, final String key)
  {
    // Double back
    if (resources.getString(R.string.config_double_backkey_key).equals(key))
    {
      backKeyDouble = inSharedPreferences.getBoolean(key, Boolean.parseBoolean(resources.getString(R.string.config_double_backkey_default)));
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
      ActivityCommons.manageWakeLockConfig(getApplicationContext(), FullActivityCommons.isFlightMode(fullProvidersService));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu)
  {
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_favorites_action_bar, menu);

    // Item status
    actionBarStatusItem = menu.findItem(R.id.item_fav_status);

    // Sauvegarde du menu
    optionsMenu = menu;

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(final Menu menu)
  {
    // Menu non encore initialise
    if (menu == null)
    {
      return false;
    }

    // Gestion du menu mode vol
    final MenuItem flightModeItem = menu.findItem(R.id.item_fav_mode_flight);
    final boolean flightMode = FullActivityCommons.manageFlightModeMenuItem(flightModeItem, fullProvidersService);

    // Gestion du menu "a proximite"
    final MenuItem proximityItem = menu.findItem(R.id.item_fav_proximity);
    proximityItem.setEnabled(!flightMode);

    // Gestion du menu mode historique
    final MenuItem historyModeItem = menu.findItem(R.id.item_fav_mode_history);
    FullActivityCommons.manageHistoryModeMenuItem(historyModeItem, fullProvidersService);

    // Gestion du menu mode alarme
    final MenuItem alarmModeItem = menu.findItem(R.id.item_fav_mode_alarm);
    FullActivityCommons.manageAlarmModeMenuItem(alarmModeItem, fullProvidersService);

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item)
  {
    // Mode vol
    if (item.getItemId() == R.id.item_fav_mode_flight)
    {
      // Si pas deja en mode vol
      if (!FullActivityCommons.isFlightMode(fullProvidersService))
      {
        // Initialisation
        flightModeAsked = true;

        // Verification des parametres de localisation
        if (!fullProvidersService.getFullLocationService().isLocationEnabled())
        {
          // GPS et GSM non actifs, question pour redirection vers les parametres de localisation
          ActivityCommons.locationSettingsDialog(this, this);
          return true;
        }

        // Boite de dialogue de progression
        showLocationProgressDialog();
      }

      // Mode vol
      toggleFlightMode();

      // Fin
      return true;
    }

    // Localisation
    else if (item.getItemId() == R.id.item_fav_proximity)
    {
      proximityMode = true;
      location();
      return true;
    }

    // Selection mode affichage
    else if (item.getItemId() == R.id.item_fav_display_mode)
    {
      displayMode();
      return true;
    }

    // Infos donnees
    else if (item.getItemId() == R.id.item_fav_datainfos)
    {
      ActivityCommons.dataInfos(this, fullProvidersService);
      return true;
    }

    // Mode carte
    else if (item.getItemId() == R.id.item_fav_mode_map)
    {
      launchMap(null);
      return true;
    }

    // Mode historique
    else if (item.getItemId() == R.id.item_fav_mode_history)
    {
      // Mode vol
      toggleHistoryMode();

      // Fin
      return true;
    }

    // Mode alarme
    else if (item.getItemId() == R.id.item_fav_mode_alarm)
    {
      // Mode alarme
      toggleAlarmMode();

      // Fin
      return true;
    }

    // Gestion des alarmes
    else if (item.getItemId() == R.id.item_fav_alarms)
    {
      launchAlarmManagement(-1);
      return true;
    }

    // Gestion des listes
    else if (item.getItemId() == R.id.item_fav_labels)
    {
      otherActivityLaunched = true;
      final Intent intent = new Intent(getApplicationContext().getString(R.string.intent_favorites_labels_action));
      startActivity(intent);
      return true;
    }

    // Preferences
    else if (item.getItemId() == R.id.item_fav_preferences)
    {
      preferencesLaunched = true;
      otherActivityLaunched = true;
      final int zoom = sharedPreferences.getInt(AbstractBalisesPreferencesActivity.CONFIG_ZOOM, 9);
      ActivityCommons.preferences(this, -1, zoom, AbstractBalisesPreferencesActivity.INTENT_MODE_NORMAL);
      return true;
    }

    // Preferences des widgets
    else if (item.getItemId() == R.id.item_fav_widget_preferences)
    {
      FullActivityCommons.widgetPreferences(this);
      return true;
    }

    // Message FFVL
    else if (item.getItemId() == R.id.item_fav_ffvl_message)
    {
      ActivityCommons.checkForFfvlMessage(this, AbstractProvidersService.FFVL_KEY, true, true);
      return true;
    }

    // A propos
    else if (item.getItemId() == R.id.item_fav_about)
    {
      ActivityCommons.about(this);
      return true;
    }

    // Aide
    else if (item.getItemId() == R.id.item_fav_help)
    {
      ActivityCommons.goToUrl(this, ActivityCommons.HELP_URL);
      return true;
    }

    // Quoi de neuf ?
    else if (item.getItemId() == R.id.item_fav_whatsnew)
    {
      ActivityCommons.displayWhatsNewMessage(this, true);
      return true;
    }

    // Aucun
    else
    {
      return super.onOptionsItemSelected(item);
    }
  }

  /**
   * 
   * @param position
   */
  private void launchAlarmManagement(final int position)
  {
    otherActivityLaunched = true;

    // Alarmes
    final Intent intent = new Intent(getApplicationContext().getString(R.string.intent_alarms_action));
    if (position >= 0)
    {
      intent.putExtra(AlarmsFragmentActivity.PARAM_POSITION, position);
    }
    startActivity(intent);
  }

  /**
   * 
   */
  void showLocationProgressDialog()
  {
    ActivityCommons.progressDialog(FavoritesActivity.this, PROGRESS_DIALOG_LOCATION, resources.getString(R.string.app_name), resources.getString(R.string.message_location_progress), true, true, new DialogInterface.OnCancelListener()
    {
      @Override
      public void onCancel(final DialogInterface dialog)
      {
        // Fin du mode vol
        cancelFlightMode(true);
      }
    });
  }

  /**
   * 
   */
  static void hideLocationProgressDialog()
  {
    ActivityCommons.cancelProgressDialog(PROGRESS_DIALOG_LOCATION);
  }

  @Override
  public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
  {
    if (proximityMode)
    {
      onCreateProximityModeContextMenu(menu, view, menuInfo);
    }
    else
    {
      onCreateLabelModeContextMenu(menu, view, menuInfo);
    }
  }

  /**
   * 
   * @param menu
   * @param view
   * @param menuInfo
   */
  @SuppressWarnings("unused")
  private void onCreateLabelModeContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
  {
    // Initialisations
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_context_favorites_label_mode, menu);

    // Titre de la boite de menu
    if (contextMenuBalise != null)
    {
      menu.setHeaderTitle(MessageFormat.format(FORMAT_CONTEXT_MENU_TITLE, contextMenuBalise.getName(), contextMenuBaliseProvider.getName()));
    }

    // Lien detail
    if (contextMenuBaliseProvider.getBaliseDetailUrl(contextMenuBalise.getBaliseId()) == null)
    {
      final MenuItem detailItem = menu.findItem(R.id.item_context_favorites_label_lien_detail_web);
      detailItem.setEnabled(false);
    }

    // Lien historique
    if (contextMenuBaliseProvider.getBaliseHistoriqueUrl(contextMenuBalise.getBaliseId()) == null)
    {
      final MenuItem historiqueItem = menu.findItem(R.id.item_context_favorites_label_lien_historique_web);
      historiqueItem.setEnabled(false);
    }

    // Synthese vocale
    final MenuItem voiceItem = menu.findItem(R.id.item_context_favorites_label_speak);
    if (!fullProvidersService.getVoiceService().canBeAvailable())
    {
      voiceItem.setEnabled(false);
    }

    // Lien carte
    final MenuItem mapItem = menu.findItem(R.id.item_context_favorites_label_map);
    if (contextMenuBalise.getCenter() == null)
    {
      mapItem.setEnabled(false);
    }
  }

  /**
   * 
   * @param menu
   * @param view
   * @param menuInfo
   */
  @SuppressWarnings("unused")
  private void onCreateProximityModeContextMenu(final ContextMenu menu, final View view, final ContextMenu.ContextMenuInfo menuInfo)
  {
    // Initialisations
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_context_favorites_proximity_mode, menu);

    // Titre de la boite de menu
    if (contextMenuBalise != null)
    {
      menu.setHeaderTitle(MessageFormat.format(FORMAT_CONTEXT_MENU_TITLE, contextMenuBalise.getName(), contextMenuBaliseProvider.getName()));
    }

    // Lien detail
    if (contextMenuBaliseProvider.getBaliseDetailUrl(contextMenuBalise.getBaliseId()) == null)
    {
      final MenuItem detailItem = menu.findItem(R.id.item_context_favorites_proximity_lien_detail);
      detailItem.setEnabled(false);
    }

    // Lien historique
    if (contextMenuBaliseProvider.getBaliseHistoriqueUrl(contextMenuBalise.getBaliseId()) == null)
    {
      final MenuItem historiqueItem = menu.findItem(R.id.item_context_favorites_proximity_lien_historique);
      historiqueItem.setEnabled(false);
    }

    // Synthese vocale
    final MenuItem voiceItem = menu.findItem(R.id.item_context_favorites_proximity_speak);
    if (!fullProvidersService.getVoiceService().canBeAvailable())
    {
      voiceItem.setEnabled(false);
    }

    // Lien carte
    final MenuItem mapItem = menu.findItem(R.id.item_context_favorites_proximity_map);
    if (contextMenuBalise.getCenter() == null)
    {
      mapItem.setEnabled(false);
    }
  }

  @Override
  public boolean onContextItemSelected(final MenuItem item)
  {
    // Selon le menu
    if ((item.getItemId() == R.id.item_context_favorites_label_historique) || (item.getItemId() == R.id.item_context_favorites_proximity_historique))
    {
      otherActivityLaunched = true;
      FullActivityCommons.historiqueBalise(this, sharedPreferences, fullProvidersService, contextMenuBaliseProvider, contextMenuBalise.getProviderId(), contextMenuBalise.getBaliseId());
      return true;
    }

    // Detail (Web)
    else if ((item.getItemId() == R.id.item_context_favorites_label_lien_detail_web) || (item.getItemId() == R.id.item_context_favorites_proximity_lien_detail))
    {
      lienDetailBalise();
      return true;
    }

    // Historique (Web)
    else if ((item.getItemId() == R.id.item_context_favorites_label_lien_historique_web) || (item.getItemId() == R.id.item_context_favorites_proximity_lien_historique))
    {
      lienHistoriqueBalise();
      return true;
    }

    // Synthese vocale
    else if ((item.getItemId() == R.id.item_context_favorites_label_speak) || (item.getItemId() == R.id.item_context_favorites_proximity_speak))
    {
      speak();
      return true;
    }

    // Ajout d'une alarme
    else if ((item.getItemId() == R.id.item_context_favorites_label_balise_alarm) || (item.getItemId() == R.id.item_context_favorites_proximity_balise_alarm))
    {
      final int position = AlarmUtils.addNewAlarm(getApplicationContext(), fullProvidersService, contextMenuBalise.getProviderId(), contextMenuBalise.getBaliseId(), null);
      launchAlarmManagement(position);
      return true;
    }

    // Suppression du favori
    else if ((item.getItemId() == R.id.item_context_favorites_label_delete))
    {
      if (fullProvidersService.getFavoritesService().isBaliseFavorite(contextMenuBalise.getProviderId(), contextMenuBalise.getBaliseId()))
      {
        // Retrait des balises courantes
        balisesFavoritesAdapter.remove(contextMenuBalise);

        // Retrait des favoris
        fullProvidersService.getFavoritesService().removeBaliseFavorite(contextMenuBalise, currentChoosenLabel);
        fullProvidersService.getFavoritesService().saveBalisesFavorites();

        // Synchro de la vue
        synchronizeBalisesFavoritesData();

        // Widgets
        BalisesWidgets.synchronizeLabelWidgets(getApplicationContext(), fullProvidersService, currentChoosenLabel, null);
      }
      return true;
    }

    // Selection des listes
    else if ((item.getItemId() == R.id.item_context_favorites_label_favorite) || (item.getItemId() == R.id.item_context_favorites_proximity_favorite))
    {
      if (fullProvidersService.getFavoritesService().isBaliseFavorite(contextMenuBalise.getProviderId(), contextMenuBalise.getBaliseId()))
      {
        final List<String> favoriteLabels = fullProvidersService.getFavoritesService().getBaliseFavoriteLabels(contextMenuBalise.getProviderId(), contextMenuBalise.getBaliseId());
        FullActivityCommons.chooseFavoritesLabels(this, fullProvidersService, this, favoriteLabels);
      }
      else
      {
        FullActivityCommons.chooseFavoritesLabels(this, fullProvidersService, this, null);
      }
      return true;
    }

    // Visualisation sur la carte
    else if ((item.getItemId() == R.id.item_context_favorites_label_map) || (item.getItemId() == R.id.item_context_favorites_proximity_map))
    {
      launchMap(contextMenuBalise);
      return true;
    }

    // Cas non prevu...
    else
    {
      return super.onContextItemSelected(item);
    }
  }

  /**
   * 
   */
  private void lienDetailBalise()
  {
    final Balise balise = contextMenuBaliseProvider.getBaliseById(contextMenuBalise.getBaliseId());
    if (balise != null)
    {
      // Statistiques
      ActivityCommons.trackEvent(fullProvidersService, AnalyticsService.CAT_FAVORITES, AnalyticsService.ACT_FAVORITES_BALISE_WEB_DETAIL_CLICKED, contextMenuBalise.getId());

      // Ouverture du lien
      ActivityCommons.goToUrl(this, contextMenuBaliseProvider.getBaliseDetailUrl(balise.id));
    }
  }

  /**
   * 
   */
  private void lienHistoriqueBalise()
  {
    final Balise balise = contextMenuBaliseProvider.getBaliseById(contextMenuBalise.getBaliseId());
    if (balise != null)
    {
      // Statistiques
      ActivityCommons.trackEvent(fullProvidersService, AnalyticsService.CAT_FAVORITES, AnalyticsService.ACT_FAVORITES_BALISE_WEB_HISTO_CLICKED, contextMenuBalise.getId());

      // Ouverture du lien
      ActivityCommons.goToUrl(this, contextMenuBaliseProvider.getBaliseHistoriqueUrl(balise.id));
    }
  }

  /**
   * 
   */
  private void speak()
  {
    ActivityCommons.trackEvent(fullProvidersService, AnalyticsService.CAT_FAVORITES, AnalyticsService.ACT_FAVORITES_BALISE_SPEECH_CLICKED, contextMenuBalise.getId());
    fullProvidersService.getVoiceService().speakBaliseReleve(contextMenuBaliseProvider.getBaliseById(contextMenuBalise.getBaliseId()), contextMenuBaliseProvider.getReleveById(contextMenuBalise.getBaliseId()), true, this);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class CurrentDisplayedLabelHandler extends Handler
  {
    private final WeakReference<FavoritesActivity> favoritesActivity;

    /**
     * 
     * @param favoritesActivity
     */
    CurrentDisplayedLabelHandler(final FavoritesActivity favoritesActivity)
    {
      super(Looper.getMainLooper());
      this.favoritesActivity = new WeakReference<FavoritesActivity>(favoritesActivity);
    }

    @Override
    public void handleMessage(final Message msg)
    {
      // Titre
      favoritesActivity.get().getSupportActionBar().setTitle((String)msg.obj);
    }

    /**
     * 
     */
    public void removeMessages()
    {
      removeMessages(0);
    }
  }

  /**
   * 
   */
  private void initCurrentDisplayedLabelHandler()
  {
    currentDisplayedLabelHandler = new CurrentDisplayedLabelHandler(this);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ChooseFavoritesForLabelHandler extends Handler
  {
    private final WeakReference<FavoritesActivity> favoritesActivity;

    /**
     * 
     * @param favoritesActivity
     */
    ChooseFavoritesForLabelHandler(final FavoritesActivity favoritesActivity)
    {
      super(Looper.getMainLooper());
      this.favoritesActivity = new WeakReference<FavoritesActivity>(favoritesActivity);
    }

    @Override
    public void handleMessage(final Message message)
    {
      final String theLabel = (String)message.obj;
      FullActivityCommons.chooseFavoritesForLabel(favoritesActivity.get().fullProvidersService, favoritesActivity.get(), favoritesActivity.get(), theLabel, PROGRESS_DIALOG_LOCATION, PROGRESS_DIALOG_CHOOSE_FAVORITES);
    }

    /**
     * 
     */
    public void removeMessages()
    {
      removeMessages(0);
    }
  }

  /**
   * 
   */
  private void initChooseFavoritesForLabelHandler()
  {
    chooseFavoritesForLabelHandler = new ChooseFavoritesForLabelHandler(this);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class LocationHandler extends Handler
  {
    private final WeakReference<FavoritesActivity> favoritesActivity;

    /**
     * 
     * @param favoritesActivity
     */
    LocationHandler(final FavoritesActivity favoritesActivity)
    {
      super(Looper.getMainLooper());
      this.favoritesActivity = new WeakReference<FavoritesActivity>(favoritesActivity);
    }

    @Override
    public void handleMessage(final Message msg)
    {
      // Demande de localisation
      favoritesActivity.get().location();
    }

    /**
     * 
     */
    public void removeMessages()
    {
      removeMessages(0);
    }
  }

  /**
   * 
   */
  private void initLocationHandler()
  {
    // Handler
    locationHandler = new LocationHandler(this);
  }

  /**
   * 
   */
  void callLocation()
  {
    final Message msg = new Message();
    msg.what = 0;
    locationHandler.sendMessage(msg);
  }

  /**
   * 
   */
  private void toggleFlightMode()
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> toggleFlightMode()");

    // Bascule
    if (fullProvidersService != null)
    {
      fullProvidersService.setFlightMode(!fullProvidersService.isFlightMode());
    }
    final boolean flightMode = fullProvidersService.isFlightMode();

    // Localisation si mode vol
    if (flightMode)
    {
      locationListener.startFlightMode();
      final boolean useGps = sharedPreferences.getBoolean(resources.getString(R.string.config_flight_mode_use_gps_key), Boolean.parseBoolean(resources.getString(R.string.config_flight_mode_use_gps_default)));
      fullProvidersService.getFullLocationService().addListener(locationListener, useGps, FLIGHT_MODE_LOCATION_MIN_TIME, FLIGHT_MODE_LOCATION_MIN_DISTANCE, true);
    }
    // Rappel du mode precedent sinon
    else
    {
      fullProvidersService.getFullLocationService().removeListener(locationListener, true);
      if (proximityMode)
      {
        // Mode proximite
        location();
      }
      else
      {
        // Label courant
        onFavoriteLabelChoosed(currentChoosenLabel);
      }
    }

    // Gestion du wakelock
    ActivityCommons.manageWakeLockConfig(getApplicationContext(), flightMode);
    ActivityCommons.acquireWakeLock();

    // MAJ du menu
    onPrepareOptionsMenu(optionsMenu);

    // Fin
    Log.d(getClass().getSimpleName(), "<<< toggleFlightMode()");
  }

  /**
   * 
   * @param callOnFavoriteLabelChoosed
   */
  void cancelFlightMode(final boolean callOnFavoriteLabelChoosed)
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> cancelFlightMode()");

    // Bascule
    if (fullProvidersService != null)
    {
      fullProvidersService.setFlightMode(false);
      fullProvidersService.getFullLocationService().removeListener(locationListener, true);
    }

    // Rappel du mode precedent sinon
    if (proximityMode)
    {
      // Mode proximite
      location();
    }
    else if (callOnFavoriteLabelChoosed)
    {
      // Label courant
      onFavoriteLabelChoosed(currentChoosenLabel);
    }

    // Gestion du wakelock
    ActivityCommons.manageWakeLockConfig(getApplicationContext(), false);
    ActivityCommons.acquireWakeLock();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< cancelFlightMode()");
  }

  /**
   * 
   */
  void location()
  {
    if (!ActivityCommons.startLocation(this, fullProvidersService, locationListener, false, false, true, PROGRESS_DIALOG_LOCATION, true))
    {
      // GPS et GSM non actifs, question pour redirection vers les parametres de localisation
      ActivityCommons.locationSettingsDialog(this, this);
    }
  }

  /**
   * 
   */
  void doLocationAfterServiceStarted()
  {
    // Thread
    final Thread locationThread = new Thread(getClass().getName() + ".doLocationAfterServiceStarted")
    {
      @Override
      public void run()
      {
        try
        {
          // Attente de la fin de l'init du service
          fullProvidersService.waitForInit();

          // Localisation
          callLocation();
        }
        catch (final InterruptedException ie)
        {
          Thread.currentThread().interrupt();
        }
      }
    };

    // Ajout du Thread a la liste des Threads a arreter
    synchronized (threads)
    {
      threads.add(locationThread);
    }

    // Demarrage
    locationThread.start();
  }

  /**
   * 
   */
  private void displayMode()
  {
    // Initialisations
    final String[] items = resources.getStringArray(R.array.config_balises_list_mode_display_entries);
    final String[] values = resources.getStringArray(R.array.config_balises_list_mode_display_values);

    // Recherche du mode actuel
    int selectedItem = -1;
    for (int i = 0; i < values.length; i++)
    {
      final int intValue = Integer.parseInt(values[i], 10);
      if (intValue == displayMode)
      {
        selectedItem = i;
        break;
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
        displayMode = Integer.parseInt(values[item], 10);
        synchronizeBalisesFavoritesData();
        dialog.dismiss();
      }
    });

    // Affichage
    final AlertDialog alertDialog = builder.create();
    alertDialog.show();
    ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_FAVS_DISPLAY_MODE, alertDialog, null);
  }

  /**
   * 
   * @param favorite
   */
  private void launchMap(final BaliseFavorite favorite)
  {
    otherActivityLaunched = true;
    final Intent intent = new Intent(getApplicationContext().getString(R.string.intent_map_action));
    if ((favorite != null) && (favorite.getCenter() != null))
    {
      intent.putExtra(AbstractBalisesMapActivity.EXTRA_LATITUDE, favorite.getCenter().getLatitudeE6());
      intent.putExtra(AbstractBalisesMapActivity.EXTRA_LONGITUDE, favorite.getCenter().getLongitudeE6());
      intent.putExtra(AbstractBalisesMapActivity.EXTRA_BALISE_PROVIDER_ID, favorite.getProviderId());
      intent.putExtra(AbstractBalisesMapActivity.EXTRA_BALISE_ID, favorite.getBaliseId());
    }
    startActivity(intent);
    finish();
  }

  @Override
  public void onFavoriteLabelChoosed(final String label)
  {
    // Label courant, le premier de la liste par defaut
    if (!fullProvidersService.getFavoritesService().getLabels().contains(label))
    {
      currentChoosenLabel = fullProvidersService.getFavoritesService().getLabels().get(0);
    }
    else
    {
      currentChoosenLabel = label;
    }

    // Fin du mode vol
    if (FullActivityCommons.isFlightMode(fullProvidersService))
    {
      cancelFlightMode(false);
    }

    // Fin du mode proximite
    proximityMode = false;

    // Scroll autorise  sur la liste
    getFavoritesListView().setDraggable(true);

    // On n'affiche pas le bouton d'ajout de balises
    footerView.setVisibility(View.VISIBLE);

    // Affichage label courant
    setCurrentDisplayedLabel(currentChoosenLabel);

    // Recup des favoris
    final List<BaliseFavorite> favorites = fullProvidersService.getFavoritesService().getBalisesForLabel(currentChoosenLabel);
    changeNonFilteredBalisesFavorites(favorites);

    // MAJ menu
    onPrepareOptionsMenu(optionsMenu);
  }

  @Override
  public void onProximityModeChoosed()
  {
    proximityMode = true;
    location();
  }

  @Override
  public boolean onKeyDown(final int keyCode, final KeyEvent event)
  {
    // Gestion de la touche "back"
    if (backKeyDouble && (keyCode == KeyEvent.KEYCODE_BACK))
    {
      // Sortie de l'appli geree sur "keyUp" sur la touche back
      ActivityCommons.manageDoubleBackKeyDown();
      return true;
    }

    return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(final int keyCode, final KeyEvent event)
  {
    // Gestion de la touche "back"
    if (keyCode == KeyEvent.KEYCODE_BACK)
    {
      // Gestion de la synthese vocale
      if ((fullProvidersService != null) && fullProvidersService.getVoiceService().stopSpeaking(false))
      {
        return true;
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
  public void onBaliseProviderAdded(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    Log.d(getClass().getSimpleName(), ">>> onBaliseProviderAdded(...)");

    // Rafraichissement de l'affichage
    if (standardMode)
    {
      manageRefreshFavoritesMessage(REFRESH_BALISE_PROVIDER_ADDED, key, provider);
    }
  }

  @Override
  public void onBaliseProviderRemoved(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean wasStandardMode, final List<BaliseProviderMode> oldActiveModes)
  {
    Log.d(getClass().getSimpleName(), ">>> onBaliseProviderRemoved(...)");

    // Rafraichissement de l'affichage
    if (wasStandardMode)
    {
      manageRefreshFavoritesMessage(REFRESH_BALISE_PROVIDER_REMOVED, key, provider);
    }
  }

  @Override
  public void onBaliseProviderUpdateEnded(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
  {
    if (standardMode)
    {
      Log.d(getClass().getSimpleName(), ">>> onBaliseProviderUpdateEnded(...)");

      // Rafraichissement de l'affichage
      manageRefreshFavoritesMessage(REFRESH_RELEVES_UPDATE, key, provider);
    }
  }

  @Override
  public void onBaliseProviderUpdateStarted(final String key, final BaliseProvider provider, final BaliseProviderInfos infos, final boolean standardMode)
  {
    if (standardMode)
    {
      Log.d(getClass().getSimpleName(), ">>> onBaliseProviderUpdateStarted(...)");

      // MAJ du message
      manageStatusMessage();
    }
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

  /**
   * 
   * @param action
   * @param providerKey
   * @param provider
   */
  private void manageRefreshFavoritesMessage(final int action, final String providerKey, final BaliseProvider provider)
  {
    // MAJ du message
    final Message msg = new Message();
    msg.what = action;
    msg.obj = new Object[] { providerKey, provider };
    refreshMessageHandler.sendMessage(msg);
  }

  /**
   * 
   */
  private void manageStatusMessage()
  {
    synchronized (ActivityCommons.statusMessageLock)
    {
      // MAJ du message
      final Message msg = new Message();
      msg.what = 0;
      msg.obj = Utils.isStringVide(ActivityCommons.getStatusMessage()) ? Strings.VIDE : ActivityCommons.getStatusMessage();
      final ActivityCommons.ProvidersStatus status = ActivityCommons.getProvidersStatus(getApplicationContext(), neededProviders);
      final int colorId;
      switch (status)
      {
        case ERROR:
          colorId = R.color.fav_status_error;
          break;
        case OK:
          colorId = R.color.fav_status_ok;
          break;
        case WARNING:
          colorId = R.color.fav_status_warning;
          break;
        default:
          colorId = R.color.fav_status_ok;
          break;
      }
      msg.arg1 = resources.getColor(colorId);
      msg.arg2 = colorId;
      statusMessageHandler.sendMessage(msg);
    }
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  public void onClickStatusTextView(final View view)
  {
    ActivityCommons.dataInfos(this, fullProvidersService);
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  public void onClickSelectionTextView(final View view)
  {
    final boolean flightMode = FullActivityCommons.isFlightMode(fullProvidersService);
    if (flightMode)
    {
      cancelFlightMode(true);
    }
    else
    {
      FullActivityCommons.chooseFavoriteLabel(this, fullProvidersService, this, currentChoosenLabel, true, !flightMode, proximityMode, flightMode);
    }
  }

  /**
   * 
   * @param view
   */
  @SuppressWarnings("unused")
  public void onClickFooterView(final View view)
  {
    // Boite de dialogue de progression
    ActivityCommons.progressDialog(this, PROGRESS_DIALOG_CHOOSE_FAVORITES, true, false, null);

    // Appel de la fenetre de choix des favoris pour le label courant
    final Message message = new Message();
    message.what = 0;
    message.obj = currentChoosenLabel;
    chooseFavoritesForLabelHandler.sendMessageDelayed(message, 100);
  }

  @Override
  public void onItemClick(final AdapterView<?> parentView, final View view, final int position, final long id)
  {
    // Initialisations
    contextMenuBalise = (BaliseFavorite)view.getTag();

    // Balise ?
    if (contextMenuBalise != null)
    {
      Log.d(getClass().getSimpleName(), "onItemClick : " + contextMenuBalise);
      contextMenuBaliseProvider = fullProvidersService.getBaliseProvider(contextMenuBalise.getProviderId());
      getListView().showContextMenu();
      ActivityCommons.trackEvent(fullProvidersService, AnalyticsService.CAT_FAVORITES, AnalyticsService.ACT_FAVORITES_BALISE_CLICKED, contextMenuBalise.getId());
    }
    // Bouton ajout
    else if (position >= getListAdapter().getCount())
    {
      onClickFooterView(view);
    }
  }

  @Override
  public void onBalisesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> onBalisesUpdate(...)");

    /* Fait dans onBaliseProviderUpdateEnded()
    manageRefreshFavoritesMessage(REFRESH_BALISES_UPDATE, key, provider);
    */
  }

  @Override
  public void onRelevesUpdate(final String key, final BaliseProvider provider, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> onRelevesUpdate(...)");

    /* Fait dans onBaliseProviderUpdateEnded
    manageRefreshFavoritesMessage(REFRESH_RELEVES_UPDATE, key, provider);
    */
  }

  @Override
  public void onDrag(final int from)
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> onDrag(...)");

    // Synchronisation de la vue
    synchronizeBalisesFavoritesData();
  }

  @Override
  public void onDrop(final int from, final int to)
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> onDrop(...)");

    // Synchronisation de la vue
    synchronizeBalisesFavoritesData();

    // Sauvegarde dans la liste
    fullProvidersService.getFavoritesService().moveBaliseFavorite(currentChoosenLabel, from, to, neededProviders);
    fullProvidersService.getFavoritesService().saveBalisesFavorites();

    // Widgets
    BalisesWidgets.synchronizeLabelWidgets(getApplicationContext(), fullProvidersService, currentChoosenLabel, null);
  }

  @Override
  public void onDelete(final int index)
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> onDelete(...)");

    // Sauvegarde dans la liste
    fullProvidersService.getFavoritesService().removeBaliseFavorite(currentChoosenLabel, index);
    fullProvidersService.getFavoritesService().saveBalisesFavorites();

    // Widgets
    BalisesWidgets.synchronizeLabelWidgets(getApplicationContext(), fullProvidersService, currentChoosenLabel, null);
  }

  @Override
  public void onLabelRenamed(final String from, final String to)
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> onLabelRenamed(...)");

    // Renommage si label courant
    if (!FullActivityCommons.isFlightMode(fullProvidersService) && !proximityMode && !Utils.isStringVide(currentChoosenLabel) && currentChoosenLabel.equals(from))
    {
      // MAJ du titre
      setCurrentDisplayedLabel(to);

      // MAJ du message
      manageStatusMessage();
    }

    // Widgets
    BalisesWidgets.onLabelRenamed(getApplicationContext(), fullProvidersService, from, to);
  }

  @Override
  public void onLabelRemoved(final String label)
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> onLabelRemoved(...)");

    // Rafraichissement si label courant => on prend le premier label
    if (!FullActivityCommons.isFlightMode(fullProvidersService) && !proximityMode && !Utils.isStringVide(currentChoosenLabel) && currentChoosenLabel.equals(label))
    {
      onFavoriteLabelChoosed(fullProvidersService.getFavoritesService().getLabels().get(0));
    }

    // Widgets
    BalisesWidgets.onLabelRemoved(getApplicationContext(), fullProvidersService, label);
  }

  @Override
  public void onFavoritesLabelsChoosed(final List<String> labels)
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> onFavoritesLabelsChoosed(...)");

    // Sauvegarde
    FullActivityCommons.updateAndSaveBaliseFavoriteLabels(fullProvidersService, contextMenuBalise.getProviderId(), contextMenuBalise.getBaliseId(), labels);

    // On force le rafraichissement si on a choisi un label, sinon (mode proximite) rien
    if (!proximityMode)
    {
      final List<BaliseFavorite> favorites = fullProvidersService.getFavoritesService().getBalisesForLabel(currentChoosenLabel);
      changeNonFilteredBalisesFavorites(favorites);
    }

    // MAJ des widgets
    BalisesWidgets.synchronizeLabelWidgets(getApplicationContext(), fullProvidersService, null, null);
  }

  @Override
  public void onFavoritesChoosed()
  {
    // Initialisation
    Log.d(getClass().getSimpleName(), ">>> onFavoritesChoosed(...)");

    // Rafraichissement
    final List<BaliseFavorite> favorites = fullProvidersService.getFavoritesService().getBalisesForLabel(currentChoosenLabel);
    changeNonFilteredBalisesFavorites(favorites);

    // MAJ des widgets
    BalisesWidgets.synchronizeLabelWidgets(getApplicationContext(), fullProvidersService, currentChoosenLabel, null);
  }

  @Override
  public boolean isBaliseProviderNeeded(final String key, final boolean standardMode, final List<BaliseProviderMode> activeModes)
  {
    if (standardMode)
    {
      synchronized (neededProviders)
      {
        return neededProviders.contains(key);
      }
    }

    return false;
  }

  @Override
  public void allow(final int reason)
  {
    // Transmission au service
    if (fullProvidersService != null)
    {
      fullProvidersService.allow(reason);
    }
  }

  @Override
  public void dontAllow(final int reason)
  {
    // Dialogue info
    FullActivityCommons.unlicensedDialog(this, licenseChecker);
  }

  @Override
  public void applicationError(final int errorCode)
  {
    // Dialogue info
    FullActivityCommons.unlicensedDialog(this, licenseChecker);
  }

  /**
   * 
   */
  private void toggleHistoryMode()
  {
    // Bascule du mode
    FullActivityCommons.toggleHistoryMode(getApplicationContext(), fullProvidersService);

    // MAJ du menu
    onPrepareOptionsMenu(optionsMenu);
  }

  /**
   * 
   */
  private void toggleAlarmMode()
  {
    // Bascule du mode
    FullActivityCommons.toggleAlarmMode(getApplicationContext(), fullProvidersService);

    // MAJ du menu
    onPrepareOptionsMenu(optionsMenu);
  }

  /**
   * 
   * @param adapter
   */
  protected void setListAdapter(final ListAdapter adapter)
  {
    getListView().setAdapter(adapter);
  }

  /**
   * 
   * @return
   */
  protected ListView getListView()
  {
    if (listView == null)
    {
      listView = (ListView)findViewById(android.R.id.list);
    }

    return listView;
  }

  /**
   * 
   * @return
   */
  protected ListAdapter getListAdapter()
  {
    final ListAdapter adapter = getListView().getAdapter();
    if (adapter instanceof HeaderViewListAdapter)
    {
      return ((HeaderViewListAdapter)adapter).getWrappedAdapter();
    }

    return adapter;
  }

  @Override
  public boolean onSupportNavigateUp()
  {
    Log.d(getClass().getSimpleName(), "onSupportNavigateUp");

    // Choix de la liste
    final boolean flightMode = FullActivityCommons.isFlightMode(fullProvidersService);
    FullActivityCommons.chooseFavoriteLabel(this, fullProvidersService, this, currentChoosenLabel, true, !flightMode, proximityMode, flightMode);

    return true;
  }

  @Override
  public void onCancelLocationSettingsDialog()
  {
    flightModeAsked = false;
    proximityMode = false;
  }
}
