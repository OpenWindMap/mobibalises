package org.pedro.android.mobibalises_common.map;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.map.MapDrawable;
import org.pedro.map.Point;
import org.pedro.spots.Orientation;
import org.pedro.spots.Pratique;
import org.pedro.spots.Spot;
import org.pedro.spots.TypeSpot;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.FloatMath;

/**
 * 
 * @author pedro.m
 */
public final class SpotDrawable extends InfosDrawable implements MapDrawable<Canvas>, InvalidableDrawable
{
  private static final String               SUFFIXE_DEFAULT                  = "_default";

  private static final float                RAYON_SPOT_MINI                  = 4;
  public static final float                 RAYON_SPOT_MAXI                  = 10;
  private static final int                  ZOOM_LEVEL_SPOT_MINI             = 5;
  private static final int                  ZOOM_LEVEL_SPOT_MAXI             = 10;

  private static float                      RAYON_DISQUE_ORIENTATIONS        = 45;
  public static float                       RAYON_SPOT                       = RAYON_SPOT_MAXI;

  private static final Object               GRAPHICS_INITIALIZED_LOCK        = new Object();
  private static boolean                    GRAPHICS_INITIALIZED             = false;
  private static float                      RAYON_TOUCH;
  private static final org.pedro.map.Rect   DISPLAY_BOUNDS                   = new org.pedro.map.Rect();
  private static final org.pedro.map.Rect   INTERACTIVE_BOUNDS               = new org.pedro.map.Rect();

  // =========================== Deco
  private static Paint                      PAINT_DECO_DISQUE_REMPLISSAGE;
  private static final int                  ALPHA_DECO_DISQUE_ORIENTATION    = 70;
  private static Paint                      PAINT_DECO_SPOT_REMPLISSAGE;
  private static Paint                      PAINT_DECO_SPOT_CONTOUR;

  // =========================== Atterro
  private static Paint                      PAINT_ATTERRO_DISQUE_REMPLISSAGE;
  private static final int                  ALPHA_ATTERRO_DISQUE_ORIENTATION = 70;
  private static Paint                      PAINT_ATTERRO_SPOT_REMPLISSAGE;
  private static Paint                      PAINT_ATTERRO_SPOT_CONTOUR;

  // =========================== Spot
  private static Path                       PATH_DISQUE;
  private static Map<Orientation, Path>     PATHS_DISQUE;
  private static Paint                      PAINT_SPOT_SPOT_REMPLISSAGE;
  private static Paint                      PAINT_SPOT_SPOT_CONTOUR;

  // =========================== INFOS
  private static String                     LABEL_ALTITUDE;
  private static String[]                   LABELS_POINTS_CARDINAUX;
  private static final String               SEPARATEUR_ORIENTATIONS          = ",";
  private static String                     LABEL_ORIENTATIONS;
  private static String                     LABEL_ORIENTATIONS_TOUTES;
  private static String[]                   LABELS_PRATIQUES_VALEURS;
  private static final String               SEPARATEUR_PRATIQUES             = "+";
  private static String                     LABEL_PRATIQUES;

  private static final String               MESSAGE_FORMAT_ITEM_TITLE        = "{0} :";
  private static String                     MESSAGE_FORMAT_ALTITUDE;
  private static String                     MESSAGE_FORMAT_ORIENTATIONS;
  private static String                     MESSAGE_FORMAT_PRATIQUES;

  private static int                        VISU_ORIENTATIONS_MIN_ZOOM       = 10;
  private static final boolean              VISU_ALTITUDE                    = true;
  private static final boolean              VISU_ORIENTATIONS                = true;
  private static final boolean              VISU_PRATIQUES                   = true;

  private static float                      SCALING_FACTOR;

  private final static Map<String, Boolean> selectedPractices                = new HashMap<String, Boolean>();
  private final static Map<String, Boolean> selectedSpotTypes                = new HashMap<String, Boolean>();
  private final static Map<String, Boolean> selectedOrientations             = new HashMap<String, Boolean>();

  private final String                      nom;
  private final TypeSpot                    typeSpot;
  private final List<Pratique>              pratiques;
  private final List<Orientation>           orientations;
  private final Integer                     altitude;

  private boolean                           graphicsValidated                = false;
  private final Object                      graphicsValidationLock           = new Object();
  private List<Path>                        disquesOrientations;
  private boolean                           displayable                      = false;

  // Unites
  public static String                      unitAltitude                     = null;

  /**
   * 
   * @param original
   * @param matrix
   * @return
   */
  private static Path nextOrientation(final Path original, final Matrix matrix)
  {
    final Path next = new Path(original);
    matrix.postRotate(22.5f);
    next.transform(matrix);

    return next;
  }

  /**
   * 
   * @param context
   */
  protected static void initGraphics(final Context context)
  {
    // Initilisation une seule fois !
    synchronized (GRAPHICS_INITIALIZED_LOCK)
    {
      if (GRAPHICS_INITIALIZED)
      {
        return;
      }

      // ScalingFactor
      final DisplayMetrics metrics = ActivityCommons.getDisplayMetrics(context);
      final float scalingFactor = metrics.density;

      // Initialisations
      final Resources resources = context.getResources();
      SCALING_FACTOR = scalingFactor;
      RAYON_DISQUE_ORIENTATIONS *= scalingFactor;
      RAYON_SPOT *= scalingFactor;
      RAYON_TOUCH = RAYON_SPOT * 1.5f;
      final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setTextSize(DrawingCommons.TEXT_SIZE);

      // Bounds
      DISPLAY_BOUNDS.right = (int)Math.ceil(RAYON_DISQUE_ORIENTATIONS);
      DISPLAY_BOUNDS.left = -DISPLAY_BOUNDS.right;
      DISPLAY_BOUNDS.bottom = DISPLAY_BOUNDS.right;
      DISPLAY_BOUNDS.top = -DISPLAY_BOUNDS.bottom;

      // DISQUE Deco
      PATH_DISQUE = new Path();
      PATH_DISQUE.moveTo(0, -RAYON_SPOT_MAXI * scalingFactor);
      PATH_DISQUE.lineTo(0, -RAYON_DISQUE_ORIENTATIONS);
      PATH_DISQUE.arcTo(new RectF(-RAYON_DISQUE_ORIENTATIONS, -RAYON_DISQUE_ORIENTATIONS, RAYON_DISQUE_ORIENTATIONS, RAYON_DISQUE_ORIENTATIONS), -90, 46);
      final float angle = (float)(46 * Math.PI / 180);
      PATH_DISQUE.lineTo(RAYON_SPOT_MAXI * scalingFactor * (float)Math.sin(angle), -RAYON_SPOT_MAXI * scalingFactor * (float)Math.cos(angle));
      PATH_DISQUE.arcTo(new RectF(-RAYON_SPOT_MAXI * scalingFactor, -RAYON_SPOT_MAXI * scalingFactor, RAYON_SPOT_MAXI * scalingFactor, RAYON_SPOT_MAXI * scalingFactor), -44, -46);
      PATH_DISQUE.setLastPoint(0, -RAYON_SPOT_MAXI * scalingFactor);
      final Matrix matrix = new Matrix();
      matrix.postRotate(-0.5f);

      // DISQUES des differentes orientations
      PATHS_DISQUE = new HashMap<Orientation, Path>();
      PATHS_DISQUE.put(Orientation.NNE, PATH_DISQUE);
      // Autres orientations
      matrix.setRotate(0);
      PATHS_DISQUE.put(Orientation.NE, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.ENE, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.E, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.ESE, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.SE, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.SSE, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.S, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.SSO, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.SO, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.OSO, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.O, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.ONO, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.NO, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.NNO, nextOrientation(PATH_DISQUE, matrix));
      PATHS_DISQUE.put(Orientation.N, nextOrientation(PATH_DISQUE, matrix));

      // Interieur disque
      paint.setColor(resources.getColor(R.color.map_spot_deco_disque));
      paint.setAlpha(ALPHA_DECO_DISQUE_ORIENTATION);
      paint.setStyle(Paint.Style.FILL);
      PAINT_DECO_DISQUE_REMPLISSAGE = new Paint(paint);
      paint.setColor(resources.getColor(R.color.map_spot_atterro_disque));
      paint.setAlpha(ALPHA_ATTERRO_DISQUE_ORIENTATION);
      PAINT_ATTERRO_DISQUE_REMPLISSAGE = new Paint(paint);

      // Spot
      paint.setColor(resources.getColor(R.color.map_spot_deco_interieur));
      PAINT_DECO_SPOT_REMPLISSAGE = new Paint(paint);
      paint.setColor(resources.getColor(R.color.map_spot_atterro_interieur));
      PAINT_ATTERRO_SPOT_REMPLISSAGE = new Paint(paint);
      paint.setColor(resources.getColor(R.color.map_spot_spot_interieur));
      PAINT_SPOT_SPOT_REMPLISSAGE = new Paint(paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(2);
      paint.setColor(resources.getColor(R.color.map_spot_deco_exterieur));
      PAINT_DECO_SPOT_CONTOUR = new Paint(paint);
      paint.setColor(resources.getColor(R.color.map_spot_atterro_exterieur));
      PAINT_ATTERRO_SPOT_CONTOUR = new Paint(paint);
      paint.setColor(resources.getColor(R.color.map_spot_spot_exterieur));
      PAINT_SPOT_SPOT_CONTOUR = new Paint(paint);

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
    // Libelles
    LABEL_ALTITUDE = resources.getString(R.string.label_map_altitude);
    LABELS_POINTS_CARDINAUX = resources.getStringArray(R.array.label_map_directions);
    LABEL_ORIENTATIONS = resources.getString(R.string.label_map_orientations);
    LABEL_ORIENTATIONS_TOUTES = resources.getString(R.string.label_map_orientations_toutes);
    LABELS_PRATIQUES_VALEURS = resources.getStringArray(R.array.label_map_pratiques_valeurs_abv);
    LABEL_PRATIQUES = resources.getString(R.string.label_map_pratiques);

    // Formats messages
    MESSAGE_FORMAT_ALTITUDE = resources.getString(R.string.message_format_altitude);
    MESSAGE_FORMAT_ORIENTATIONS = resources.getString(R.string.message_format_orientations);
    MESSAGE_FORMAT_PRATIQUES = resources.getString(R.string.message_format_pratiques);

    // Parent
    InfosDrawable.initInfosGraphics(resources, scalingFactor);
  }

  /**
   * 
   * @param zoomLevel
   */
  public static void onZoomLevelChanged(final int zoomLevel)
  {
    calculRayonSpots(zoomLevel);
  }

  /**
   * 
   * @param zoomLevel
   */
  private static void calculRayonSpots(final int zoomLevel)
  {
    final float pourcentageZoom = (float)(zoomLevel - ZOOM_LEVEL_SPOT_MINI) / (float)(ZOOM_LEVEL_SPOT_MAXI - ZOOM_LEVEL_SPOT_MINI);
    RAYON_SPOT = RAYON_SPOT_MINI + pourcentageZoom * (RAYON_SPOT_MAXI - RAYON_SPOT_MINI);

    RAYON_SPOT = Math.min(RAYON_SPOT, RAYON_SPOT_MAXI);
    RAYON_SPOT = Math.max(RAYON_SPOT, RAYON_SPOT_MINI);

    RAYON_SPOT *= SCALING_FACTOR;
    RAYON_TOUCH = RAYON_SPOT * 1.5f;

    INTERACTIVE_BOUNDS.right = (int)Math.ceil(RAYON_TOUCH);
    INTERACTIVE_BOUNDS.left = -INTERACTIVE_BOUNDS.right;
    INTERACTIVE_BOUNDS.bottom = INTERACTIVE_BOUNDS.right;
    INTERACTIVE_BOUNDS.top = -INTERACTIVE_BOUNDS.bottom;
  }

  /**
   * 
   * @param spot
   * @param mapView
   * @param touchableTitle
   */
  public SpotDrawable(final Spot spot, final MapView mapView, final String touchableTitle)
  {
    super(mapView, touchableTitle);
    nom = spot.nom;
    typeSpot = spot.type;
    pratiques = spot.pratiques;
    orientations = spot.orientations;
    altitude = spot.altitude;
  }

  /**
   * 
   */
  private void validateGraphics()
  {
    switch (typeSpot)
    {
      case DECOLLAGE:
        validateGraphicsDeco();
        break;
      case ATTERRISSAGE:
        validateGraphicsAtterro();
        break;
      default:
        break;
    }

    // Infos
    validateInfosGraphics();

    // Affichage
    validateDisplayability();
  }

  /**
   * 
   */
  private void validateGraphicsDeco()
  {
    validateDisquesOrientations(false);
  }

  /**
   * 
   */
  private void validateGraphicsAtterro()
  {
    validateDisquesOrientations(true);
  }

  /**
   * 
   */
  private void validateDisquesOrientations(final boolean inverse)
  {
    disquesOrientations = new ArrayList<Path>();
    if ((orientations != null) && (orientations.size() > 0) && (orientations.size() < Orientation.values().length))
    {
      for (final Orientation orientation : orientations)
      {
        if (inverse)
        {
          disquesOrientations.add(PATHS_DISQUE.get(orientation.getOpposee()));
        }
        else
        {
          disquesOrientations.add(PATHS_DISQUE.get(orientation));
        }
      }
    }
  }

  /**
   * 
   */
  private void validateInfosGraphics()
  {
    // Elaboration du texte
    infos.clear();

    // Nom
    infos.add(new DrawableInfo(nom, null));

    // Altitude
    if (VISU_ALTITUDE && (altitude != null))
    {
      final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_ALTITUDE);
      final String value = MessageFormat.format(MESSAGE_FORMAT_ALTITUDE, Integer.valueOf(ActivityCommons.getFinalAltitude(altitude.intValue())), ActivityCommons.getAltitudeUnit());
      infos.add(new DrawableInfo(title, value));
    }

    // Orientations
    if (VISU_ORIENTATIONS && (orientations != null) && (orientations.size() > 0))
    {
      final StringBuilder txtOrientations = new StringBuilder();
      String separateur = Strings.VIDE;
      if (orientations.size() < Orientation.values().length)
      {
        int i = 0;
        for (final Orientation orientation : Orientation.values())
        {
          if (orientations.contains(orientation))
          {
            txtOrientations.append(separateur);
            txtOrientations.append(LABELS_POINTS_CARDINAUX[i]);

            separateur = SEPARATEUR_ORIENTATIONS;
          }

          // Next
          i++;
        }
      }
      else
      {
        txtOrientations.append(LABEL_ORIENTATIONS_TOUTES);
      }

      final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_ORIENTATIONS);
      final String value = MessageFormat.format(MESSAGE_FORMAT_ORIENTATIONS, txtOrientations.toString());
      infos.add(new DrawableInfo(title, value));
    }

    // Pratiques
    if (VISU_PRATIQUES && (pratiques != null) && (pratiques.size() > 0))
    {
      final StringBuilder txtPratiques = new StringBuilder();
      String separateur = Strings.VIDE;
      int i = 0;
      for (final Pratique pratique : Pratique.values())
      {
        if (pratiques.contains(pratique))
        {
          txtPratiques.append(separateur);
          txtPratiques.append(LABELS_PRATIQUES_VALEURS[i]);

          separateur = SEPARATEUR_PRATIQUES;
        }

        // Next
        i++;
      }

      final String title = MessageFormat.format(MESSAGE_FORMAT_ITEM_TITLE, LABEL_PRATIQUES);
      final String value = MessageFormat.format(MESSAGE_FORMAT_PRATIQUES, txtPratiques.toString());
      infos.add(new DrawableInfo(title, value));
    }

    // Bounds du texte
    getBoxBounds(boxBounds, infos, true);
  }

  /**
   * 
   */
  private void validateDisplayability()
  {
    // Type
    final boolean typeDisplayed = (selectedSpotTypes.get(typeSpot.getKey()) != null);

    // Pratique
    boolean pratiqueDisplayed = false;
    if (typeDisplayed)
    {
      if ((pratiques == null) || (pratiques.size() == 0))
      {
        pratiqueDisplayed = true;
      }
      else
      {
        for (final Pratique pratique : pratiques)
        {
          if (selectedPractices.get(pratique.getKey()) != null)
          {
            pratiqueDisplayed = true;
            break;
          }
        }
      }
    }

    // Orientation
    boolean orientationDisplayed = false;
    if (typeDisplayed && pratiqueDisplayed)
    {
      if (orientations.size() == 0)
      {
        // Aucune info d'orientation => on affiche
        orientationDisplayed = true;
      }
      else if (orientations.size() == Orientation.values().length)
      {
        // Toutes les orientations => on affiche
        orientationDisplayed = true;
      }
      else if (selectedOrientations.size() == 0)
      {
        // Aucun critere d'orientation => on affiche quand meme
        orientationDisplayed = true;
      }
      else if (selectedOrientations.size() == AbstractBalisesPreferencesActivity.NB_ORIENTATIONS)
      {
        // Tous les criteres d'orientation => on affiche
        orientationDisplayed = true;
      }
      else
      {
        for (final Orientation orientationSpot : orientations)
        {
          for (final String orientationPreferencesKey : selectedOrientations.keySet())
          {
            if (orientationSpot.isAlmostTheSame(orientationPreferencesKey))
            {
              orientationDisplayed = true;
              break;
            }
          }

          if (orientationDisplayed)
          {
            break;
          }
        }
      }
    }

    // MAJ des flags
    displayable = pratiqueDisplayed && typeDisplayed && orientationDisplayed;
  }

  @Override
  public void draw(final Canvas canvas, final Point point)
  {
    // Validation
    validateDrawable();

    // Selon le type
    switch (typeSpot)
    {
      case DECOLLAGE:
        drawDeco(canvas, point);
        break;
      case ATTERRISSAGE:
        drawAtterro(canvas, point);
        break;
      case SPOT:
        drawSpot(canvas, point);
        break;
    }

    // Les details
    if (drawDetails)
    {
      drawDetails(canvas, point);
    }
  }

  /**
   * 
   * @param canvas
   * @param point
   */
  private void drawDeco(final Canvas canvas, final Point point)
  {
    // Les disques d'orientations
    if (mapView.get().getController().getZoom() >= VISU_ORIENTATIONS_MIN_ZOOM)
    {
      drawDisquesOrientations(canvas, point, PAINT_DECO_DISQUE_REMPLISSAGE);
    }

    // Le spot
    canvas.drawCircle(point.x, point.y, RAYON_SPOT, PAINT_DECO_SPOT_REMPLISSAGE);
    canvas.drawCircle(point.x, point.y, RAYON_SPOT, PAINT_DECO_SPOT_CONTOUR);
  }

  /**
   * 
   * @param canvas
   * @param point
   */
  private void drawAtterro(final Canvas canvas, final Point point)
  {
    // Les disques d'orientations
    if (mapView.get().getController().getZoom() >= VISU_ORIENTATIONS_MIN_ZOOM)
    {
      drawDisquesOrientations(canvas, point, PAINT_ATTERRO_DISQUE_REMPLISSAGE);
    }

    // Le spot
    canvas.drawCircle(point.x, point.y, RAYON_SPOT, PAINT_ATTERRO_SPOT_REMPLISSAGE);
    canvas.drawCircle(point.x, point.y, RAYON_SPOT, PAINT_ATTERRO_SPOT_CONTOUR);
  }

  /**
   * 
   * @param canvas
   * @param point
   */
  private void drawDisquesOrientations(final Canvas canvas, final Point point, final Paint paint)
  {
    // Les disques des directions
    if ((disquesOrientations != null) && (disquesOrientations.size() > 0))
    {
      for (final Path disque : disquesOrientations)
      {
        disque.offset(point.x, point.y);
        canvas.drawPath(disque, paint);
        disque.offset(-point.x, -point.y);
      }
    }
  }

  /**
   * 
   * @param canvas
   * @param point
   */
  private static void drawSpot(final Canvas canvas, final Point point)
  {
    // Le spot
    canvas.drawCircle(point.x, point.y, RAYON_SPOT, PAINT_SPOT_SPOT_REMPLISSAGE);
    canvas.drawCircle(point.x, point.y, RAYON_SPOT, PAINT_SPOT_SPOT_CONTOUR);
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
   * @return the vISU_ORIENTATIONS_MIN_ZOOM
   */
  public static int getVISU_ORIENTATIONS_MIN_ZOOM()
  {
    return VISU_ORIENTATIONS_MIN_ZOOM;
  }

  /**
   * @param vISUORIENTATIONSMINZOOM the vISU_ORIENTATIONS_MIN_ZOOM to set
   */
  public static void setVISU_ORIENTATIONS_MIN_ZOOM(final int vISUORIENTATIONSMINZOOM)
  {
    VISU_ORIENTATIONS_MIN_ZOOM = vISUORIENTATIONSMINZOOM;
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
  public boolean isDrawable()
  {
    // Validation si besoin
    validateDrawable();

    return displayable;
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

    // Pratiques
    final String[] practicesDefaults = resources.getStringArray(R.array.pratiques_defaults);
    boolean practicesChanged = false;
    int i = 0;
    for (final Pratique pratique : Pratique.values())
    {
      final String key = AbstractBalisesPreferencesActivity.CONFIG_KEY_PRATIQUE + Strings.CHAR_POINT + pratique.getKey();
      final boolean value = sharedPreferences.getBoolean(key, Boolean.parseBoolean(practicesDefaults[i]));
      final Boolean oldBooleanValue = selectedPractices.get(pratique.getKey());
      final boolean oldValue = (oldBooleanValue == null ? false : oldBooleanValue.booleanValue());
      if (value != oldValue)
      {
        practicesChanged = true;
      }
      if (value)
      {
        selectedPractices.put(pratique.getKey(), Boolean.TRUE);
      }
      else
      {
        selectedPractices.remove(pratique.getKey());
      }

      // Next
      i++;
    }

    // Types spot
    final String[] typesDefaults = resources.getStringArray(R.array.types_spot_defaults);
    boolean spotTypesChanged = false;
    i = 0;
    for (final TypeSpot typeSpot : TypeSpot.values())
    {
      final String key = AbstractBalisesPreferencesActivity.CONFIG_KEY_TYPE_SPOT + Strings.CHAR_POINT + typeSpot.getKey();
      final boolean value = sharedPreferences.getBoolean(key, Boolean.parseBoolean(typesDefaults[i]));
      final Boolean oldBooleanValue = selectedSpotTypes.get(typeSpot.getKey());
      final boolean oldValue = (oldBooleanValue == null ? false : oldBooleanValue.booleanValue());
      if (value != oldValue)
      {
        spotTypesChanged = true;
      }
      if (value)
      {
        selectedSpotTypes.put(typeSpot.getKey(), Boolean.TRUE);
      }
      else
      {
        selectedSpotTypes.remove(typeSpot.getKey());
      }

      // Next
      i++;
    }

    // Orientations
    boolean orientationsChanged = false;
    for (final Orientation orientation : Orientation.values())
    {
      // On ne prend que les orientations majeures et moyennes (8 points cardinaux, N, NE, E, SE, ...)
      if (orientation.getLevel() > 2)
      {
        continue;
      }

      final String key = AbstractBalisesPreferencesActivity.formatOrientationPreferenceKey(orientation);
      final String defaultKey = key + SUFFIXE_DEFAULT;
      try
      {
        final boolean value = sharedPreferences.getBoolean(key, Boolean.parseBoolean(resources.getString(resources.getIdentifier(defaultKey, Strings.RESOURCES_STRING, context.getPackageName()))));
        final Boolean oldBooleanValue = selectedOrientations.get(orientation.getKey());
        final boolean oldValue = (oldBooleanValue == null ? false : oldBooleanValue.booleanValue());
        if (value != oldValue)
        {
          orientationsChanged = true;
        }
        if (value)
        {
          selectedOrientations.put(orientation.getKey(), Boolean.TRUE);
        }
        else
        {
          selectedOrientations.remove(orientation.getKey());
        }
      }
      catch (final Resources.NotFoundException rnfe)
      {
        // Nothing : c'est le cas pour les orientations intermediaires (NNE, ENE, ...)
      }
    }

    // Unite altitude
    final String unitAltitudeNew = sharedPreferences.getString(resources.getString(R.string.config_unit_altitude_key), resources.getString(R.string.config_unit_altitude_default));
    boolean altitudeUnitChanged = false;
    if (!unitAltitudeNew.equals(unitAltitude))
    {
      unitAltitude = unitAltitudeNew;
      altitudeUnitChanged = true;
    }

    return practicesChanged || spotTypesChanged || orientationsChanged || altitudeUnitChanged;
  }

  /**
   * @return
   */
  public List<Pratique> getPratiques()
  {
    return pratiques;
  }

  /**
   * @return
   */
  public List<Orientation> getOrientations()
  {
    return orientations;
  }

  /**
   * @return
   */
  public TypeSpot getTypeSpot()
  {
    return typeSpot;
  }
}
