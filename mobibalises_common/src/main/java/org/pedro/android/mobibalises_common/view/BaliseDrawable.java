package org.pedro.android.mobibalises_common.view;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.map.InfosDrawable;
import org.pedro.android.mobibalises_common.map.InvalidableDrawable;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils.TendanceVent;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.map.MapDrawable;
import org.pedro.map.Point;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public class BaliseDrawable extends InfosDrawable implements MapDrawable<Canvas>, InvalidableDrawable
{
  private static final long               DELTA_BALISE_DRAWABLE     = 3L * 3600000L; // 3 heures : limite au delà de laquelle les balises ne sont plus affichées
  private static boolean                  GRAPHICS_INITIALIZED      = false;
  private static final Object             GRAPHICS_INITIALIZED_LOCK = new Object();

  // Tendances
  private static final String             DELTA_STABLE              = " =";
  private static final String             DELTA_FAIBLE_HAUSSE       = "\u25B2";
  private static final String             DELTA_FORTE_HAUSSE        = "\u25B2";
  private static final String             DELTA_FAIBLE_BAISSE       = "\u25BC";
  public static final String              DELTA_FORTE_BAISSE        = "\u25BC";

  // =========================== BALISE
  public static final org.pedro.map.Rect  DISPLAY_BOUNDS            = new org.pedro.map.Rect();
  private static final org.pedro.map.Rect INTERACTIVE_BOUNDS        = new org.pedro.map.Rect();
  private static boolean                  TOOLTIP_DISPLAYED_ON_TAP;

  // =========================== INFOS
  private static String                   LABEL_UNITE_DIRECTION;
  private static String                   LABEL_UNITE_HYDROMETRIE;
  private static String                   LABEL_UNITE_PRESSION;
  private static String                   LABEL_UNITE_HUMIDITE;
  private static String                   LABEL_INACTIVE;
  private static String                   LABEL_AUCUN_RELEVE;
  private static String                   LABEL_DIRECTION_MOYENNE;
  private static String                   LABEL_DIRECTION_INSTANTANEE;
  private static String                   LABEL_OSCILLATION_DIRECTION;
  private static String                   LABEL_VENT_MOYEN;
  private static String                   LABEL_VENT_MINI;
  private static String                   LABEL_VENT_MAXI;
  private static String                   LABEL_TEMPERATURE;
  private static String                   LABEL_POINT_ROSEE;
  private static String                   LABEL_HYDROMETRIE;
  private static String[]                 LABELS_PLUIES;
  private static String                   LABEL_NUAGES;
  private static String                   LABEL_PRESSION;
  private static String                   LABEL_LUMINOSITE;
  private static String                   LABEL_HUMIDITE;
  private static String                   LABEL_ALTITUDE;

  private static final String             MESSAGE_FORMAT_ITEM_TITLE = "{0} :";
  private static String                   MESSAGE_FORMAT_DIRECTION;
  private static String                   MESSAGE_FORMAT_OSCILLATION_DIRECTION;
  private static String                   MESSAGE_FORMAT_VITESSE;
  private static String                   MESSAGE_FORMAT_HEURE_VITESSE_MAX;
  private static String                   MESSAGE_FORMAT_VITESSE_HORODATEE;
  private static String                   MESSAGE_FORMAT_TEMPERATURE;
  private static String                   MESSAGE_FORMAT_PLUIE;
  private static String                   MESSAGE_FORMAT_HYDROMETRIE;
  private static String                   MESSAGE_FORMAT_PLUIE_HYDROMETRIE;
  private static String                   MESSAGE_FORMAT_NUAGES_HUITIEME;
  private static String                   MESSAGE_FORMAT_NUAGES_PLAFOND;
  private static String                   MESSAGE_FORMAT_NUAGES_BOURGEONS;
  private static String                   MESSAGE_FORMAT_NUAGES;
  private static String                   MESSAGE_FORMAT_PRESSION;
  private static String                   MESSAGE_FORMAT_LUMINOSITE;
  private static String                   MESSAGE_FORMAT_HUMIDITE;
  private static String                   MESSAGE_FORMAT_ALTITUDE;

  private static DateFormat               DATE_FORMAT;
  private static DateFormat               HEURE_FORMAT;

  private static boolean                  VISU_INACTIVE;
  private static boolean                  VISU_DATE;
  private static boolean                  VISU_DIRECTION_MOYENNE;
  private static boolean                  VISU_DIRECTION_INSTANTANEE;
  private static boolean                  VISU_OSCILLATION_DIRECTION;
  private static boolean                  VISU_VENT_MOYEN;
  private static boolean                  VISU_VENT_MINI;
  private static boolean                  VISU_VENT_MAXI;
  private static boolean                  VISU_TEMPERATURE;
  private static boolean                  VISU_POINT_ROSEE;
  private static boolean                  VISU_HYDROMETRIE;
  private static boolean                  VISU_NUAGES;
  private static boolean                  VISU_PRESSION;
  private static boolean                  VISU_LUMINOSITE;
  private static boolean                  VISU_HUMIDITE;
  private static boolean                  VISU_ALTITUDE;

  // =========================== Balise
  protected final String                  idBalise;
  protected final BaliseProvider          provider;

  private boolean                         graphicsValidated         = false;
  private final Object                    graphicsValidationLock    = new Object();
  private boolean                         collide                   = false;

  private boolean                         drawable;
  protected final WindIconInfos           windIconInfos             = new WindIconInfos();

  public static boolean                   drawWindIcon              = false;
  public static boolean                   drawWeatherIcon           = false;

  // Unites
  public static String                    unitSpeed                 = null;
  public static String                    unitAltitude              = null;
  public static String                    unitDistance              = null;
  public static String                    unitTemperature           = null;

  /**
   * 
   * @param context
   */
  public static void initGraphics(final Context context)
  {
    // Initialisation une seule fois !
    synchronized (GRAPHICS_INITIALIZED_LOCK)
    {
      // Formats de date
      DATE_FORMAT = android.text.format.DateFormat.getDateFormat(context);
      HEURE_FORMAT = android.text.format.DateFormat.getTimeFormat(context);

      if (GRAPHICS_INITIALIZED)
      {
        return;
      }

      // ScalingFactor
      final DisplayMetrics metrics = ActivityCommons.getDisplayMetrics(context);
      final float scalingFactor = metrics.density;

      // Initialisations communes
      final Resources resources = context.getResources();
      DrawingCommons.initialize(context);

      // Bounds
      DISPLAY_BOUNDS.right = (int)Math.ceil(DrawingCommons.WIND_ICON_FLECHE_MAX * scalingFactor);
      DISPLAY_BOUNDS.left = -DISPLAY_BOUNDS.right;
      DISPLAY_BOUNDS.bottom = DISPLAY_BOUNDS.right;
      DISPLAY_BOUNDS.top = -DISPLAY_BOUNDS.bottom;
      INTERACTIVE_BOUNDS.right = (int)Math.ceil(DrawingCommons.WIND_ICON_RAYON_CONTOUR);
      INTERACTIVE_BOUNDS.left = -INTERACTIVE_BOUNDS.right;
      INTERACTIVE_BOUNDS.bottom = INTERACTIVE_BOUNDS.right;
      INTERACTIVE_BOUNDS.top = -INTERACTIVE_BOUNDS.bottom;

      // Infos
      initInfosGraphics(resources, scalingFactor);

      // Fin
      GRAPHICS_INITIALIZED = true;
    }
  }

  /**
   * 
   * @param resources
   * @param scalingFactor
   */
  protected static void initInfosGraphics(final Resources resources, final float scalingFactor)
  {
    // Parent
    InfosDrawable.initInfosGraphics(resources, scalingFactor);

    // Initialisations
    EPAISSEUR_FLECHE_BOX *= scalingFactor;

    // Libelles
    LABEL_UNITE_DIRECTION = resources.getString(R.string.unit_direction);
    LABEL_UNITE_HYDROMETRIE = resources.getString(R.string.unit_hydrometry);
    LABEL_UNITE_PRESSION = resources.getString(R.string.unit_pressure);
    LABEL_UNITE_HUMIDITE = resources.getString(R.string.unit_humidity);
    LABEL_INACTIVE = resources.getString(R.string.label_map_inactive);
    LABEL_AUCUN_RELEVE = resources.getString(R.string.label_map_no_data);
    LABEL_DIRECTION_MOYENNE = resources.getString(R.string.label_map_average_direction_abv);
    LABEL_DIRECTION_INSTANTANEE = resources.getString(R.string.label_map_instant_direction_abv);
    LABEL_OSCILLATION_DIRECTION = resources.getString(R.string.label_map_variation_direction_abv);
    LABEL_VENT_MOYEN = resources.getString(R.string.label_map_average_wind_abv);
    LABEL_VENT_MINI = resources.getString(R.string.label_map_min_wind_abv);
    LABEL_VENT_MAXI = resources.getString(R.string.label_map_max_wind_abv);
    LABEL_TEMPERATURE = resources.getString(R.string.label_map_temperature_abv);
    LABEL_POINT_ROSEE = resources.getString(R.string.label_map_dew_point_abv);
    LABEL_HYDROMETRIE = resources.getString(R.string.label_map_hydrometry_abv);
    LABEL_NUAGES = resources.getString(R.string.label_map_clouds_abv);
    LABELS_PLUIES = resources.getStringArray(R.array.label_map_rains);
    LABEL_PRESSION = resources.getString(R.string.label_map_pressure_abv);
    LABEL_LUMINOSITE = resources.getString(R.string.label_map_luminosity_abv);
    LABEL_HUMIDITE = resources.getString(R.string.label_map_humidity_abv);
    LABEL_ALTITUDE = resources.getString(R.string.label_map_altitude_abv);

    // Formats messages
    MESSAGE_FORMAT_DIRECTION = resources.getString(R.string.message_format_direction);
    MESSAGE_FORMAT_OSCILLATION_DIRECTION = resources.getString(R.string.message_format_oscillation_direction);
    MESSAGE_FORMAT_VITESSE = resources.getString(R.string.message_format_vitesse);
    MESSAGE_FORMAT_HEURE_VITESSE_MAX = resources.getString(R.string.message_format_heure_vitesse_max);
    MESSAGE_FORMAT_VITESSE_HORODATEE = resources.getString(R.string.message_format_vitesse_horodatee);
    MESSAGE_FORMAT_TEMPERATURE = resources.getString(R.string.message_format_temperature);
    MESSAGE_FORMAT_PLUIE = resources.getString(R.string.message_format_pluie);
    MESSAGE_FORMAT_HYDROMETRIE = resources.getString(R.string.message_format_hydrometrie);
    MESSAGE_FORMAT_PLUIE_HYDROMETRIE = resources.getString(R.string.message_format_pluie_hydrometrie);
    MESSAGE_FORMAT_NUAGES_HUITIEME = resources.getString(R.string.message_format_nuages_huitieme);
    MESSAGE_FORMAT_NUAGES_PLAFOND = resources.getString(R.string.message_format_nuages_plafond);
    MESSAGE_FORMAT_NUAGES_BOURGEONS = resources.getString(R.string.message_format_nuages_bourgeons);
    MESSAGE_FORMAT_NUAGES = resources.getString(R.string.message_format_nuages);
    MESSAGE_FORMAT_PRESSION = resources.getString(R.string.message_format_pression);
    MESSAGE_FORMAT_LUMINOSITE = resources.getString(R.string.message_format_luminosite);
    MESSAGE_FORMAT_HUMIDITE = resources.getString(R.string.message_format_humidite);
    MESSAGE_FORMAT_ALTITUDE = resources.getString(R.string.message_format_altitude);
  }

  /**
   * 
   * @param sharedPreferences
   * @param context
   * @return
   */
  public static boolean updatePreferences(final SharedPreferences sharedPreferences, final Context context)
  {
    // Initialisations
    final Resources resources = context.getResources();
    boolean preferencesChanged = false;

    // Action sur touch
    final String touchAction = sharedPreferences.getString(resources.getString(R.string.config_map_touch_action_key), resources.getString(R.string.config_map_touch_action_default));
    final String tooltipTouchAction = resources.getString(R.string.config_map_touch_action_tooltip);
    final String bothTouchAction = resources.getString(R.string.config_map_touch_action_both);
    TOOLTIP_DISPLAYED_ON_TAP = (tooltipTouchAction.equals(touchAction) || bothTouchAction.equals(touchAction));

    // Communs
    if (DrawingCommons.updatePreferences(resources, sharedPreferences))
    {
      preferencesChanged = true;
    }

    // Visualisable si inactive
    final boolean visuInactive = !sharedPreferences.getBoolean(resources.getString(R.string.config_map_balise_hide_inactive_key), Boolean.parseBoolean(resources.getString(R.string.config_map_balise_hide_inactive_default)));
    if (visuInactive != VISU_INACTIVE)
    {
      VISU_INACTIVE = visuInactive;
      preferencesChanged = true;
    }

    // Altitude
    final boolean visuAltitude = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_altitude_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_altitude_default)));
    if (visuAltitude != VISU_ALTITUDE)
    {
      VISU_ALTITUDE = visuAltitude;
      preferencesChanged = true;
    }

    // Date et heure
    final boolean visuDate = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_date_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_date_default)));
    if (visuDate != VISU_DATE)
    {
      VISU_DATE = visuDate;
      preferencesChanged = true;
    }

    // Direction moyenne
    final boolean visuDirectionMoyenne = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_averagedir_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_averagedir_default)));
    if (visuDirectionMoyenne != VISU_DIRECTION_MOYENNE)
    {
      VISU_DIRECTION_MOYENNE = visuDirectionMoyenne;
      preferencesChanged = true;
    }

    // Direction instantanee
    final boolean visuDirectionInstantanee = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_instantdir_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_instantdir_default)));
    if (visuDirectionInstantanee != VISU_DIRECTION_INSTANTANEE)
    {
      VISU_DIRECTION_INSTANTANEE = visuDirectionInstantanee;
      preferencesChanged = true;
    }

    // Oscillation direction
    final boolean visuOscillationDirection = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_variationdir_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_variationdir_default)));
    if (visuOscillationDirection != VISU_OSCILLATION_DIRECTION)
    {
      VISU_OSCILLATION_DIRECTION = visuOscillationDirection;
      preferencesChanged = true;
    }

    // Vent moyen
    final boolean visuVentMoyen = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_averagewind_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_averagewind_default)));
    if (visuVentMoyen != VISU_VENT_MOYEN)
    {
      VISU_VENT_MOYEN = visuVentMoyen;
      preferencesChanged = true;
    }

    // Vent mini
    final boolean visuVentMini = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_minwind_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_minwind_default)));
    if (visuVentMini != VISU_VENT_MINI)
    {
      VISU_VENT_MINI = visuVentMini;
      preferencesChanged = true;
    }

    // Vent maxi
    final boolean visuVentMaxi = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_maxwind_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_maxwind_default)));
    if (visuVentMaxi != VISU_VENT_MAXI)
    {
      VISU_VENT_MAXI = visuVentMaxi;
      preferencesChanged = true;
    }

    // Temperature
    final boolean visuTemperature = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_temperature_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_temperature_default)));
    if (visuTemperature != VISU_TEMPERATURE)
    {
      VISU_TEMPERATURE = visuTemperature;
      preferencesChanged = true;
    }

    // Point de rosee
    final boolean visuPointRosee = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_dew_point_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_dew_point_default)));
    if (visuPointRosee != VISU_POINT_ROSEE)
    {
      VISU_POINT_ROSEE = visuPointRosee;
      preferencesChanged = true;
    }

    // Hydrometrie
    final boolean visuHydrometrie = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_hydrometry_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_hydrometry_default)));
    if (visuHydrometrie != VISU_HYDROMETRIE)
    {
      VISU_HYDROMETRIE = visuHydrometrie;
      preferencesChanged = true;
    }

    // Nuages
    final boolean visuNuages = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_clouds_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_clouds_default)));
    if (visuNuages != VISU_NUAGES)
    {
      VISU_NUAGES = visuNuages;
      preferencesChanged = true;
    }

    // Pression
    final boolean visuPression = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_pressure_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_pressure_default)));
    if (visuPression != VISU_PRESSION)
    {
      VISU_PRESSION = visuPression;
      preferencesChanged = true;
    }

    // Luminosite
    final boolean visuLuminosite = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_luminosity_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_luminosity_default)));
    if (visuLuminosite != VISU_LUMINOSITE)
    {
      VISU_LUMINOSITE = visuLuminosite;
      preferencesChanged = true;
    }

    // Humidite
    final boolean visuHumidite = sharedPreferences.getBoolean(resources.getString(R.string.config_map_tooltip_humidity_key), Boolean.parseBoolean(resources.getString(R.string.config_map_tooltip_humidity_default)));
    if (visuHumidite != VISU_HUMIDITE)
    {
      VISU_HUMIDITE = visuHumidite;
      preferencesChanged = true;
    }

    // Visualisation icone vent
    final boolean visuWindIcon = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES, AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES_DEFAULT);
    if (visuWindIcon != drawWindIcon)
    {
      drawWindIcon = visuWindIcon;
      preferencesChanged = true;
    }

    // Visualisation icone meteo
    final boolean visuWeatherIcon = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WEATHER_BALISES, Boolean.parseBoolean(resources.getString(R.string.config_map_layers_weather_default)));
    if (visuWeatherIcon != drawWeatherIcon)
    {
      drawWeatherIcon = visuWeatherIcon;
      preferencesChanged = true;
    }

    // Unite vitesse
    final String unitSpeedNew = sharedPreferences.getString(resources.getString(R.string.config_unit_speed_key), resources.getString(R.string.config_unit_speed_default));
    if (!unitSpeedNew.equals(unitSpeed))
    {
      unitSpeed = unitSpeedNew;
      preferencesChanged = true;
    }

    // Unite altitude
    final String unitAltitudeNew = sharedPreferences.getString(resources.getString(R.string.config_unit_altitude_key), resources.getString(R.string.config_unit_altitude_default));
    if (!unitAltitudeNew.equals(unitAltitude))
    {
      unitAltitude = unitAltitudeNew;
      preferencesChanged = true;
    }

    // Unite distance
    final String unitDistanceNew = sharedPreferences.getString(resources.getString(R.string.config_unit_distance_key), resources.getString(R.string.config_unit_distance_default));
    if (!unitDistanceNew.equals(unitDistance))
    {
      unitDistance = unitDistanceNew;
      preferencesChanged = true;
    }

    // Unite temperature
    final String unitTemperatureNew = sharedPreferences.getString(resources.getString(R.string.config_unit_temperature_key), resources.getString(R.string.config_unit_temperature_default));
    if (!unitTemperatureNew.equals(unitTemperature))
    {
      unitTemperature = unitTemperatureNew;
      preferencesChanged = true;
    }

    // Fin
    return preferencesChanged;
  }

  /**
   * 
   * @return
   */
  public static boolean isTooltipDisplayedOnTap()
  {
    return TOOLTIP_DISPLAYED_ON_TAP;
  }

  /**
   * 
   * @param idBalise
   * @param provider
   * @param mapView
   * @param touchableTitle
   */
  protected BaliseDrawable(final String idBalise, final BaliseProvider provider, final MapView mapView, final String touchableTitle)
  {
    super(mapView, touchableTitle);
    this.idBalise = idBalise;
    this.provider = provider;
  }

  /**
   * 
   */
  protected void validateGraphics()
  {
    // Validation
    final Balise balise = provider.getBaliseById(idBalise);
    final Releve releve = provider.getReleveById(idBalise);

    // Icone
    DrawingCommons.validateWindIconInfos(windIconInfos, balise, releve);

    // Affichable ?
    drawable = (windIconInfos.isBaliseActive() && windIconInfos.isReleveValide() && (releve != null) && (releve.date != null));

    // Ne pas afficher les balises trop vieilles
    if (drawable)
    {
      Log.d(getClass().getSimpleName(), "releve : " + (drawable ? releve.date : "null")); //TODO
      final long delta = System.currentTimeMillis() - releve.date.getTime();
      if (delta > DELTA_BALISE_DRAWABLE)
      {
        Log.d(getClass().getSimpleName(), "trop vieille"); //TODO
        drawable = false;
      }
    }

    // Infos
    validateInfosGraphics(balise, releve);
  }

  /**
   * 
   * @param tendance
   * @return
   */
  private static String formatTendanceVent(final TendanceVent tendance)
  {
    switch (tendance)
    {
      case STABLE:
        return DELTA_STABLE;
      case FAIBLE_HAUSSE:
        return DELTA_FAIBLE_HAUSSE;
      case FORTE_HAUSSE:
        return DELTA_FORTE_HAUSSE;
      case FAIBLE_BAISSE:
        return DELTA_FAIBLE_BAISSE;
      case FORTE_BAISSE:
        return DELTA_FORTE_BAISSE;
      case INCONNUE:
      default:
        return Strings.VIDE;
    }
  }

  /**
   * 
   * @param tendance
   * @return
   */
  private static Paint getPaintTendanceVent(final TendanceVent tendance)
  {
    switch (tendance)
    {
      case FAIBLE_HAUSSE:
      case FAIBLE_BAISSE:
        return PAINT_TENDANCE_FAIBLE;
      case FORTE_HAUSSE:
      case FORTE_BAISSE:
        return PAINT_TENDANCE_FORTE;
      case STABLE:
      case INCONNUE:
      default:
        return PAINT_INFOS_TEXTE_LEFT;
    }
  }

  /**
   * 
   * @param balise
   * @param releve
   */
  private void validateInfosGraphics(final Balise balise, final Releve releve)
  {
    // Elaboration du texte
    infos.clear();

    if (balise != null)
    {
      // Nom
      final StringBuilder nom = new StringBuilder();
      nom.append(balise.nom);
      if (collide)
      {
        nom.append(Strings.SPACE);
        nom.append(Strings.CHAR_PARENTHESE_DEB);
        nom.append(provider.getName());
        nom.append(Strings.CHAR_PARENTHESE_FIN);
      }
      infos.add(new DrawableInfo(nom.toString(), null));
      PAINT_INFOS_TEXTE_TITRE_LEFT.getTextBounds(nom.toString(), 0, nom.length(), titleBounds);

      // Date si le releve est valide
      if ((releve != null) && VISU_DATE)
      {
        if (windIconInfos.isReleveValide() && (releve.date != null))
        {
          final Date date = new Date(Utils.fromUTC(releve.date.getTime()));
          infos.add(new DrawableInfo(DATE_FORMAT.format(date) + Strings.SPACE + HEURE_FORMAT.format(date), true, true));
        }
        else if (!Utils.isBooleanNull(balise.active) && Utils.getBooleanValue(balise.active))
        {
          infos.add(new DrawableInfo(LABEL_AUCUN_RELEVE, null));
        }
        else
        {
          infos.add(new DrawableInfo(LABEL_INACTIVE, null));
        }
      }

      // Altitude
      if (VISU_ALTITUDE && (balise.altitude != Integer.MIN_VALUE))
      {
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_ALTITUDE);
        final String value = MessageFormat.format(MESSAGE_FORMAT_ALTITUDE, Integer.valueOf(ActivityCommons.getFinalAltitude(balise.altitude)), ActivityCommons.getAltitudeUnit());
        infos.add(new DrawableInfo(title, value));
      }
    }

    // Autres donnees du releve
    if ((balise != null) && (releve != null) && windIconInfos.isReleveValide())
    {
      if (VISU_DIRECTION_MOYENNE && windIconInfos.isDirectionValide())
      {
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_DIRECTION_MOYENNE);
        final String value = MessageFormat.format(MESSAGE_FORMAT_DIRECTION, DrawingCommons.getLabelDirectionVent(releve.directionMoyenne), Integer.valueOf(releve.directionMoyenne), LABEL_UNITE_DIRECTION);
        infos.add(new DrawableInfo(title, value));
      }
      if (VISU_DIRECTION_INSTANTANEE && DrawingCommons.isDirectionOk(releve.directionInstantanee))
      {
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_DIRECTION_INSTANTANEE);
        final String value = MessageFormat.format(MESSAGE_FORMAT_DIRECTION, DrawingCommons.getLabelDirectionVent(releve.directionInstantanee), Integer.valueOf(releve.directionInstantanee), LABEL_UNITE_DIRECTION);
        infos.add(new DrawableInfo(title, value));
      }
      if (VISU_OSCILLATION_DIRECTION && DrawingCommons.isDirectionOk(releve.directionVentVariation1) && DrawingCommons.isDirectionOk(releve.directionVentVariation2))
      {
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_OSCILLATION_DIRECTION);
        final String value = MessageFormat.format(MESSAGE_FORMAT_OSCILLATION_DIRECTION, Integer.valueOf(releve.directionVentVariation1), LABEL_UNITE_DIRECTION, Integer.valueOf(releve.directionVentVariation2), LABEL_UNITE_DIRECTION);
        infos.add(new DrawableInfo(title, value));
      }
      if (VISU_VENT_MINI && !Double.isNaN(releve.ventMini))
      {
        final TendanceVent tendance = BaliseProviderUtils.getTendanceVent(DrawingCommons.DELTA_PEREMPTION_MS, releve.date, releve.dateRelevePrecedent, releve.ventMini, releve.ventMiniTendance);
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_VENT_MINI);
        final String value = MessageFormat.format(MESSAGE_FORMAT_VITESSE, Double.valueOf(ActivityCommons.getFinalSpeed(releve.ventMini)), ActivityCommons.getSpeedUnit());
        infos.add(new DrawableInfo(title, value, formatTendanceVent(tendance), getPaintTendanceVent(tendance)));
      }
      if (VISU_VENT_MOYEN && !Double.isNaN(releve.ventMoyen))
      {
        final TendanceVent tendance = BaliseProviderUtils.getTendanceVent(DrawingCommons.DELTA_PEREMPTION_MS, releve.date, releve.dateRelevePrecedent, releve.ventMoyen, releve.ventMoyenTendance);
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_VENT_MOYEN);
        final String value = MessageFormat.format(MESSAGE_FORMAT_VITESSE, Double.valueOf(ActivityCommons.getFinalSpeed(releve.ventMoyen)), ActivityCommons.getSpeedUnit());
        infos.add(new DrawableInfo(title, value, formatTendanceVent(tendance), getPaintTendanceVent(tendance)));
      }
      if (VISU_VENT_MAXI && !Double.isNaN(releve.ventMaxi))
      {
        final StringBuilder heureMax = new StringBuilder();
        if (releve.dateHeureVentMaxi != null)
        {
          heureMax.append(Strings.SPACE);
          heureMax.append(MessageFormat.format(MESSAGE_FORMAT_HEURE_VITESSE_MAX, HEURE_FORMAT.format(new Date(Utils.fromUTC(releve.dateHeureVentMaxi.getTime())))));
        }
        final TendanceVent tendance = BaliseProviderUtils.getTendanceVent(DrawingCommons.DELTA_PEREMPTION_MS, releve.date, releve.dateRelevePrecedent, releve.ventMaxi, releve.ventMaxiTendance);
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_VENT_MAXI);
        final String value = MessageFormat.format(MESSAGE_FORMAT_VITESSE_HORODATEE, Double.valueOf(ActivityCommons.getFinalSpeed(releve.ventMaxi)), ActivityCommons.getSpeedUnit(), heureMax.toString());
        infos.add(new DrawableInfo(title, value, formatTendanceVent(tendance), getPaintTendanceVent(tendance)));
      }
      if (VISU_TEMPERATURE && !Double.isNaN(releve.temperature))
      {
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_TEMPERATURE);
        final String value = MessageFormat.format(MESSAGE_FORMAT_TEMPERATURE, Double.valueOf(ActivityCommons.getFinalTemperature(releve.temperature)), ActivityCommons.getTemperatureUnit());
        infos.add(new DrawableInfo(title, value));
      }
      if (VISU_POINT_ROSEE && !Double.isNaN(releve.pointRosee))
      {
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_POINT_ROSEE);
        final String value = MessageFormat.format(MESSAGE_FORMAT_TEMPERATURE, Double.valueOf(ActivityCommons.getFinalTemperature(releve.pointRosee)), ActivityCommons.getTemperatureUnit());
        infos.add(new DrawableInfo(title, value));
      }
      if (VISU_HYDROMETRIE && ((!Double.isNaN(releve.hydrometrie) && (releve.hydrometrie > 0)) || ((releve.pluie != Integer.MIN_VALUE) && (releve.pluie > 0))))
      {
        if (!Double.isNaN(releve.hydrometrie) && (releve.hydrometrie > 0) && (releve.pluie != Integer.MIN_VALUE) && (releve.pluie > 0))
        {
          final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_HYDROMETRIE);
          final String value = MessageFormat.format(MESSAGE_FORMAT_PLUIE_HYDROMETRIE, LABELS_PLUIES[releve.pluie], Double.valueOf(releve.hydrometrie), LABEL_UNITE_HYDROMETRIE);
          infos.add(new DrawableInfo(title, value));
        }
        else if (!Double.isNaN(releve.hydrometrie) && (releve.hydrometrie > 0))
        {
          final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_HYDROMETRIE);
          final String value = MessageFormat.format(MESSAGE_FORMAT_HYDROMETRIE, Double.valueOf(releve.hydrometrie), LABEL_UNITE_HYDROMETRIE);
          infos.add(new DrawableInfo(title, value));
        }
        else
        {
          final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_HYDROMETRIE);
          final String value = MessageFormat.format(MESSAGE_FORMAT_PLUIE, LABELS_PLUIES[releve.pluie]);
          infos.add(new DrawableInfo(title, value));
        }
      }
      if (VISU_NUAGES && (((releve.nuages != Integer.MIN_VALUE) && (releve.nuages > 0)) || !Utils.isBooleanNull(releve.nuagesBourgeonnants)))
      {
        // Huitieme et plafond
        String huitieme = Strings.VIDE;
        String plafond = Strings.VIDE;
        if (releve.nuages != Integer.MIN_VALUE)
        {
          huitieme = MessageFormat.format(MESSAGE_FORMAT_NUAGES_HUITIEME, Integer.valueOf(releve.nuages));
          if ((releve.plafondNuages != Integer.MIN_VALUE) && (releve.nuages > 0))
          {
            final int altitudeBalise = (balise.altitude == Integer.MIN_VALUE ? 0 : balise.altitude);
            plafond = MessageFormat.format(MESSAGE_FORMAT_NUAGES_PLAFOND, Integer.valueOf(ActivityCommons.getFinalAltitude(releve.plafondNuages + altitudeBalise)).toString(), ActivityCommons.getAltitudeUnit());
          }
        }

        // Cumulus/Cunimb
        String bourgeons = Strings.VIDE;
        if (!Utils.isBooleanNull(releve.nuagesBourgeonnants) && Utils.getBooleanValue(releve.nuagesBourgeonnants))
        {
          bourgeons = MESSAGE_FORMAT_NUAGES_BOURGEONS;
        }

        // Nuages
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_NUAGES);
        final String value = MessageFormat.format(MESSAGE_FORMAT_NUAGES, huitieme, plafond, bourgeons);
        infos.add(new DrawableInfo(title, value));
      }
      if (VISU_PRESSION && !Double.isNaN(releve.pression))
      {
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_PRESSION);
        final String value = MessageFormat.format(MESSAGE_FORMAT_PRESSION, Double.valueOf(releve.pression), LABEL_UNITE_PRESSION);
        infos.add(new DrawableInfo(title, value));
      }
      if (VISU_LUMINOSITE && (releve.luminosite != null))
      {
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_LUMINOSITE);
        final String value = MessageFormat.format(MESSAGE_FORMAT_LUMINOSITE, releve.luminosite);
        infos.add(new DrawableInfo(title, value));
      }
      if (VISU_HUMIDITE && (releve.humidite != Integer.MIN_VALUE))
      {
        final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_HUMIDITE);
        final String value = MessageFormat.format(MESSAGE_FORMAT_HUMIDITE, Integer.valueOf(releve.humidite), LABEL_UNITE_HUMIDITE);
        infos.add(new DrawableInfo(title, value));
      }
    }

    // Bounds du texte
    getBoxBounds(boxBounds, infos, false);
  }

  @Override
  public void draw(final Canvas canvas, final Point point)
  {
    // Validation
    validateDrawable();

    // Icone
    if (drawWindIcon)
    {
      DrawingCommons.drawWindIcon(canvas, point, windIconInfos);
    }

    // Les details
    if (drawDetails)
    {
      drawDetails(canvas, point);
    }
  }

  @Override
  public void invalidateDrawable()
  {
    synchronized (graphicsValidationLock)
    {
      graphicsValidated = false;
    }
  }

  @Override
  public void validateDrawable()
  {
    synchronized (graphicsValidationLock)
    {
      if (!graphicsValidated)
      {
        validateGraphics();
        graphicsValidated = true;
      }
    }

    // Pour palier au bug inexplicable de la perte de l'alignement au centre
    if (windIconInfos.paintValeur != null)
    {
      windIconInfos.paintValeur.setTextAlign(Align.CENTER);
    }
  }

  @Override
  public boolean isDrawable()
  {
    // Si visualisation des balises inactives : toujours vrai
    if (VISU_INACTIVE)
    {
      return true;
    }

    // Validation si besoin
    validateDrawable();

    return drawable;
  }

  @Override
  public org.pedro.map.Rect getDisplayBounds()
  {
    return DISPLAY_BOUNDS;
  }

  @Override
  public org.pedro.map.Rect getInteractiveBounds()
  {
    return INTERACTIVE_BOUNDS;
  }

  /**
   * 
   */
  public void setCollide(final boolean collide)
  {
    this.collide = collide;
  }
}
