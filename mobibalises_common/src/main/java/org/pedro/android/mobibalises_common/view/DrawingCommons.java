package org.pedro.android.mobibalises_common.view;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils.TendanceVent;
import org.pedro.balises.Balise;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.map.Point;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public abstract class DrawingCommons
{
  // Texte
  public static float         TEXT_SIZE               = 17;

  // Points cardinaux
  private static float        DELTA_POINTS_CARDINAUX;
  private static float        DECALAGE_POINTS_CARDINAUX;
  private static String[]     LABELS_POINTS_CARDINAUX;

  // Icone vents
  public static final int     WIND_ICON_FLECHE_MAX    = 33;
  private static Path         WIND_ICON_PATH_FLECHE;
  private static Paint        WIND_ICON_PAINT_FLECHE_CERCLE_CONTOUR;
  private static Paint        WIND_ICON_PAINT_FLECHE_REMPLISSAGE;
  private static Paint        WIND_ICON_PAINT_CERCLE_EXTERIEUR_REMPLISSAGE_ACTIF;
  private static Paint        WIND_ICON_PAINT_CERCLE_EXTERIEUR_REMPLISSAGE_INACTIF;
  private static Paint        WIND_ICON_PAINT_CERCLE_INTERIEUR_REMPLISSAGE_ACTIF;
  private static Paint        WIND_ICON_PAINT_CERCLE_INTERIEUR_REMPLISSAGE_INACTIF;
  private static Paint        WIND_ICON_PAINT_CERCLE_INTERIEUR_REMPLISSAGE_PERIMEE;
  private static Paint        WIND_ICON_PAINT_VALEUR_OK;
  private static Paint        WIND_ICON_PAINT_VALEUR_KO;
  private static float        WIND_ICON_Y_TEXT;
  private static float        WIND_ICON_RAYON         = 18.6f;
  public static float         WIND_ICON_RAYON_CONTOUR;
  private static float        WIND_ICON_PETIT_RAYON   = 13;
  private static Path         WIND_ICON_PATH_TENDANCE_VENT;
  private static Paint        WIND_ICON_PAINT_TENDANCE_VENT_FAIBLE;
  private static Paint        WIND_ICON_PAINT_TENDANCE_VENT_FORTE;
  private static Paint        WIND_ICON_PAINT_TENDANCE_VENT_STABLE;
  private static float        WIND_ICON_DELTA_Y_VALEUR_TENDANCE;
  private static float        WIND_ICON_TENDANCE_STABLE_DELTA_X;
  private static float        WIND_ICON_TENDANCE_STABLE_DELTA_Y;

  // Icone meteo
  public static final float   RAYON_SPOT_WEATHER      = 8;

  // Limites
  private static int          WIND_LIMIT_MOY;
  private static int          WIND_LIMIT_MAX;
  private static int          WIND_LIMIT_OPERATOR;
  private static int          WIND_LIMIT_OPERATOR_AND = 1;
  private static int          WIND_LIMIT_OPERATOR_OR;
  private static int          WIND_VALUE_DISPLAYED;
  private static int          WIND_VALUE_DISPLAYED_MOY;
  private static int          WIND_VALUE_DISPLAYED_MAX;
  private static int          WIND_VALUE_DISPLAYED_MIN;
  private static long         DELTA_PEREMPTION;
  public static long          DELTA_PEREMPTION_MS;
  private static long         DELTA_PEREMPTION_MAX;
  private static long         DELTA_PEREMPTION_MAX_MS;
  private static String       TEXT_NON_AVAILABLE;

  // Divers
  private static boolean      INITIALIZED;
  private static final Object INITIALIZED_LOCK        = new Object();

  /**
   * 
   * @param context
   */
  public static void initialize(final Context context)
  {
    // Initilisation une seule fois !
    synchronized (INITIALIZED_LOCK)
    {
      if (INITIALIZED)
      {
        return;
      }

      // Initialisations
      final Resources resources = context.getResources();

      // ScalingFactor
      final DisplayMetrics metrics = ActivityCommons.getDisplayMetrics(context);
      final float scalingFactor = metrics.density;

      // Labels
      LABELS_POINTS_CARDINAUX = resources.getStringArray(R.array.label_map_directions);

      // Pour la transformation de la direction en texte
      DELTA_POINTS_CARDINAUX = 360f / LABELS_POINTS_CARDINAUX.length;
      DECALAGE_POINTS_CARDINAUX = DELTA_POINTS_CARDINAUX / 2;

      // Dimensions
      TEXT_SIZE *= scalingFactor;
      WIND_ICON_RAYON *= scalingFactor;
      WIND_ICON_RAYON_CONTOUR = WIND_ICON_RAYON + 0.5f;
      WIND_ICON_PETIT_RAYON *= scalingFactor;

      // Paint
      final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setTextSize(TEXT_SIZE);

      // Fleche
      WIND_ICON_PATH_FLECHE = new Path();
      WIND_ICON_PATH_FLECHE.moveTo(-7, 0);
      WIND_ICON_PATH_FLECHE.lineTo(-7, 20);
      WIND_ICON_PATH_FLECHE.lineTo(-14, 20);
      WIND_ICON_PATH_FLECHE.lineTo(0, WIND_ICON_FLECHE_MAX);
      WIND_ICON_PATH_FLECHE.lineTo(14, 20);
      WIND_ICON_PATH_FLECHE.lineTo(7, 20);
      WIND_ICON_PATH_FLECHE.lineTo(7, 0);
      WIND_ICON_PATH_FLECHE.lineTo(-7, 0);
      WIND_ICON_PATH_FLECHE.setLastPoint(-7, 0);

      // Fleche avec facteur d'etirement
      final Matrix matrix = new Matrix();
      matrix.postScale(scalingFactor, scalingFactor);
      WIND_ICON_PATH_FLECHE.transform(matrix);

      // Interieur Fleche
      paint.setColor(resources.getColor(R.color.map_balise_exterieur_active));
      paint.setStyle(Paint.Style.FILL);
      WIND_ICON_PAINT_FLECHE_REMPLISSAGE = new Paint(paint);

      // Contour Fleche et cercle exterieur
      paint.setColor(resources.getColor(R.color.map_balise_contour));
      paint.setStyle(Paint.Style.STROKE);
      WIND_ICON_PAINT_FLECHE_CERCLE_CONTOUR = new Paint(paint);

      // Contour Fleche et cercle exterieur
      paint.setColor(resources.getColor(R.color.map_balise_contour));
      paint.setStyle(Paint.Style.STROKE);
      WIND_ICON_PAINT_FLECHE_CERCLE_CONTOUR = new Paint(paint);

      // Remplissage Cercle Exterieur si balise active
      paint.setColor(resources.getColor(R.color.map_balise_exterieur_active));
      paint.setStyle(Paint.Style.FILL);
      WIND_ICON_PAINT_CERCLE_EXTERIEUR_REMPLISSAGE_ACTIF = new Paint(paint);

      // Remplissage Cercle Exterieur si balise inactive
      paint.setColor(resources.getColor(R.color.map_balise_exterieur_inactive));
      paint.setStyle(Paint.Style.FILL);
      WIND_ICON_PAINT_CERCLE_EXTERIEUR_REMPLISSAGE_INACTIF = new Paint(paint);

      // Remplissage Cercle Interieur si balise active
      paint.setColor(resources.getColor(R.color.map_balise_interieur_active));
      paint.setStyle(Paint.Style.FILL);
      WIND_ICON_PAINT_CERCLE_INTERIEUR_REMPLISSAGE_ACTIF = new Paint(paint);

      // Remplissage Cercle Interieur si balise active mais perimee
      paint.setColor(resources.getColor(R.color.map_balise_interieur_perimee));
      paint.setStyle(Paint.Style.FILL);
      WIND_ICON_PAINT_CERCLE_INTERIEUR_REMPLISSAGE_PERIMEE = new Paint(paint);

      // Remplissage Cercle Interieur si balise inactive
      paint.setColor(resources.getColor(R.color.map_balise_interieur_inactive));
      paint.setStyle(Paint.Style.FILL);
      WIND_ICON_PAINT_CERCLE_INTERIEUR_REMPLISSAGE_INACTIF = new Paint(paint);

      // Valeur
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setTypeface(Typeface.DEFAULT_BOLD);
      // Si inferieure a la limite
      paint.setColor(resources.getColor(R.color.map_balise_texte_ok));
      WIND_ICON_PAINT_VALEUR_OK = new Paint(paint);
      // Si superieure a la limite
      paint.setColor(resources.getColor(R.color.map_balise_texte_ko));
      WIND_ICON_PAINT_VALEUR_KO = new Paint(paint);

      // Decalage Y du texte
      final Rect textBounds = new Rect();
      paint.getTextBounds(Strings.ZERO, 0, 1, textBounds);
      WIND_ICON_Y_TEXT = (textBounds.bottom - textBounds.top) / 2;

      // Texte lorsque donnee non disponible
      TEXT_NON_AVAILABLE = resources.getString(R.string.label_non_available);

      // Valeur de vent affichee
      WIND_VALUE_DISPLAYED_MOY = Integer.parseInt(resources.getString(R.string.config_map_wind_value_moy), 10);
      WIND_VALUE_DISPLAYED_MAX = Integer.parseInt(resources.getString(R.string.config_map_wind_value_max), 10);
      WIND_VALUE_DISPLAYED_MIN = Integer.parseInt(resources.getString(R.string.config_map_wind_value_min), 10);

      // Operateurs
      WIND_LIMIT_OPERATOR_AND = Integer.parseInt(resources.getString(R.string.config_map_limit_operator_value_and), 10);
      WIND_LIMIT_OPERATOR_OR = Integer.parseInt(resources.getString(R.string.config_map_limit_operator_value_or), 10);

      // Tendance vent
      {
        final float angle = 50;
        final float angleRad = (float)Math.toRadians(angle);
        final float rayon = WIND_ICON_PETIT_RAYON * 0.6f;
        WIND_ICON_PATH_TENDANCE_VENT = new Path();
        final float xp = (float)Math.cos(angleRad) * rayon;
        final float yp = (float)Math.sin(angleRad) * rayon;
        WIND_ICON_DELTA_Y_VALEUR_TENDANCE = 4 * scalingFactor;

        // Path
        WIND_ICON_PATH_TENDANCE_VENT.moveTo(xp, -yp);
        WIND_ICON_PATH_TENDANCE_VENT.lineTo(WIND_ICON_PETIT_RAYON, 0);
        WIND_ICON_PATH_TENDANCE_VENT.lineTo(xp, yp);
        WIND_ICON_PATH_TENDANCE_VENT.lineTo(xp, -yp);
        WIND_ICON_PATH_TENDANCE_VENT.setLastPoint(xp, -yp);

        // Paints
        WIND_ICON_PAINT_TENDANCE_VENT_FAIBLE = new Paint(Paint.ANTI_ALIAS_FLAG);
        WIND_ICON_PAINT_TENDANCE_VENT_FAIBLE.setStyle(Paint.Style.FILL);
        WIND_ICON_PAINT_TENDANCE_VENT_FAIBLE.setColor(resources.getColor(R.color.map_balise_tendance_vent_faible));
        WIND_ICON_PAINT_TENDANCE_VENT_FORTE = new Paint(WIND_ICON_PAINT_TENDANCE_VENT_FAIBLE);
        WIND_ICON_PAINT_TENDANCE_VENT_FORTE.setColor(resources.getColor(R.color.map_balise_tendance_vent_forte));
        WIND_ICON_PAINT_TENDANCE_VENT_STABLE = new Paint(WIND_ICON_PAINT_TENDANCE_VENT_FAIBLE);
        WIND_ICON_PAINT_TENDANCE_VENT_STABLE.setColor(resources.getColor(R.color.map_balise_tendance_vent_stable));
      }

      // Tendance vent stable
      WIND_ICON_TENDANCE_STABLE_DELTA_X = 5 * scalingFactor;
      WIND_ICON_TENDANCE_STABLE_DELTA_Y = TEXT_SIZE / 2 + (1 * scalingFactor);

      // Fin
      INITIALIZED = true;
    }
  }

  /**
   * 
   * @param resources
   * @param sharedPreferences
   * @return
   */
  public static boolean updatePreferences(final Resources resources, final SharedPreferences sharedPreferences)
  {
    // Initialisations
    boolean preferencesChanged = false;

    // Valeur de vent affichee
    try
    {
      final String sValue = sharedPreferences.getString(resources.getString(R.string.config_map_wind_key), resources.getString(R.string.config_map_wind_default));
      final int value = Integer.parseInt(sValue, 10);
      if (value != WIND_VALUE_DISPLAYED)
      {
        WIND_VALUE_DISPLAYED = value;
        preferencesChanged = true;
      }
    }
    catch (final NumberFormatException nfe)
    {
      Log.e(BaliseDrawable.class.getSimpleName(), nfe.getMessage(), nfe);
    }

    // Limite Vent Moyen
    final boolean moyChecked = sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_moy_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_moy_check_default)));
    int moyWindLimit = sharedPreferences.getInt(resources.getString(R.string.config_map_limit_moy_edit_key), Integer.parseInt(resources.getString(R.string.config_map_limit_moy_edit_default), 10));
    moyWindLimit = (moyChecked ? moyWindLimit : -1);
    if (moyWindLimit != WIND_LIMIT_MOY)
    {
      WIND_LIMIT_MOY = moyWindLimit;
      preferencesChanged = true;
    }

    // Limite Vent Operator
    final int operator = Integer.parseInt(sharedPreferences.getString(resources.getString(R.string.config_map_limit_operator_key), resources.getString(R.string.config_map_limit_operator_default)), 10);
    if (operator != WIND_LIMIT_OPERATOR)
    {
      WIND_LIMIT_OPERATOR = operator;
      preferencesChanged = true;
    }

    // Limite Vent Maxi
    final boolean maxChecked = sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_max_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_max_check_default)));
    int maxWindLimit = sharedPreferences.getInt(resources.getString(R.string.config_map_limit_max_edit_key), Integer.parseInt(resources.getString(R.string.config_map_limit_max_edit_default), 10));
    maxWindLimit = (maxChecked ? maxWindLimit : -1);
    if (maxWindLimit != WIND_LIMIT_MAX)
    {
      WIND_LIMIT_MAX = maxWindLimit;
      preferencesChanged = true;
    }

    // Limite de peremption releve
    try
    {
      final int value = sharedPreferences.getInt(resources.getString(R.string.config_map_outofdate_key), Integer.parseInt(resources.getString(R.string.config_map_outofdate_default), 10));
      if (value != DELTA_PEREMPTION)
      {
        DELTA_PEREMPTION = value;
        DELTA_PEREMPTION_MS = 60000L * DELTA_PEREMPTION;
        preferencesChanged = true;
      }
    }
    catch (final NumberFormatException nfe)
    {
      Log.e(BaliseDrawable.class.getSimpleName(), nfe.getMessage(), nfe);
    }

    // Limite de peremption vent maxi
    try
    {
      final int value = sharedPreferences.getInt(resources.getString(R.string.config_map_outofdate_max_wind_key), Integer.parseInt(resources.getString(R.string.config_map_outofdate_max_wind_default), 10));
      if (value != DELTA_PEREMPTION_MAX)
      {
        DELTA_PEREMPTION_MAX = value;
        DELTA_PEREMPTION_MAX_MS = 60000L * DELTA_PEREMPTION_MAX;
        preferencesChanged = true;
      }
    }
    catch (final NumberFormatException nfe)
    {
      Log.e(BaliseDrawable.class.getSimpleName(), nfe.getMessage(), nfe);
    }

    // Fin
    return preferencesChanged;
  }

  /**
   * 
   * @param direction
   * @return
   */
  public static boolean isDirectionOk(final int direction)
  {
    return (direction != Integer.MIN_VALUE) && (direction >= 0) && (direction <= 360);
  }

  /**
   * 
   * @param direction
   * @return
   */
  private static int getIndicePointsCardinaux(final int direction)
  {
    int finalDirection = direction;
    while (finalDirection < 0)
    {
      finalDirection += 360;
    }
    return ((int)Math.floor((finalDirection + DECALAGE_POINTS_CARDINAUX) / DELTA_POINTS_CARDINAUX)) % LABELS_POINTS_CARDINAUX.length;
  }

  /**
   * 
   * @param value
   * @return
   */
  public static String getLabelDirectionVent(final int value)
  {
    return LABELS_POINTS_CARDINAUX[getIndicePointsCardinaux(value)];
  }

  /**
   * 
   * @param releve
   * @return
   */
  private static boolean isWindLimitOk(final Releve releve)
  {
    // Initialisations
    boolean ok;

    // Vent moyen
    boolean moyenOk = true;
    if (WIND_LIMIT_MOY >= 0)
    {
      moyenOk = Double.isNaN(releve.ventMoyen) || (ActivityCommons.getFinalSpeed(releve.ventMoyen) <= WIND_LIMIT_MOY);
    }

    // Vent maxi
    boolean maxiOk = true;
    if (WIND_LIMIT_MAX >= 0)
    {
      // Vitesse
      maxiOk = Double.isNaN(releve.ventMaxi) || (ActivityCommons.getFinalSpeed(releve.ventMaxi) <= WIND_LIMIT_MAX);

      // Peremption du vent maxi ?
      if (!Double.isNaN(releve.ventMaxi) && !maxiOk && (releve.dateHeureVentMaxi != null))
      {
        // Si l'heure du vent maxi est > au delai de peremption par rapport a l'heure du releve, le maxi devient ok
        maxiOk = (releve.date.getTime() - releve.dateHeureVentMaxi.getTime() > DELTA_PEREMPTION_MAX_MS);
      }
    }

    // Operateur
    if ((WIND_LIMIT_MOY >= 0) && (WIND_LIMIT_MAX >= 0))
    {
      if ((WIND_LIMIT_OPERATOR != WIND_LIMIT_OPERATOR_AND) && (WIND_LIMIT_OPERATOR != WIND_LIMIT_OPERATOR_OR))
      {
        // Cas normalement impossible de l'operateur non renseigne. Mais qui arrive (je ne sais pas pourquoi)
        // => Et par defaut
        WIND_LIMIT_OPERATOR = WIND_LIMIT_OPERATOR_AND;
      }

      // Selon l'operateur
      if (WIND_LIMIT_OPERATOR == WIND_LIMIT_OPERATOR_AND)
      {
        ok = moyenOk && maxiOk;
      }
      else if (WIND_LIMIT_OPERATOR == WIND_LIMIT_OPERATOR_OR)
      {
        ok = moyenOk || maxiOk;
      }
      else
      {
        // Cas impossible normalement
        throw new RuntimeException("Situation inconfortable...");
      }
    }
    else if (WIND_LIMIT_MOY >= 0)
    {
      ok = moyenOk;
    }
    else if (WIND_LIMIT_MAX >= 0)
    {
      ok = maxiOk;
    }
    else
    {
      // Aucune regle => tout vert
      ok = true;
    }

    return ok;
  }

  /**
   * 
   * @param infos
   * @param balise
   * @param releve
   */
  public static void validateWindIconInfos(final WindIconInfos infos, final Balise balise, final Releve releve)
  {
    // Validation
    infos.baliseActive = (balise != null) && !Utils.isBooleanNull(balise.active) && Utils.getBooleanValue(balise.active);
    infos.releveValide = infos.baliseActive && (releve != null);
    infos.directionValide = infos.releveValide && (releve != null) && DrawingCommons.isDirectionOk(releve.directionMoyenne);

    // La fleche
    if (infos.directionValide)
    {
      // Rotation
      infos.pathFleche.set(WIND_ICON_PATH_FLECHE);
      infos.matrix.reset();
      if ((releve != null) && (releve.directionMoyenne != Integer.MIN_VALUE))
      {
        infos.matrix.postRotate(releve.directionMoyenne);
        infos.pathFleche.transform(infos.matrix);
      }
    }

    // Le cercle exterieur
    infos.paintCercleExterieurRemplissage = (infos.baliseActive ? WIND_ICON_PAINT_CERCLE_EXTERIEUR_REMPLISSAGE_ACTIF : WIND_ICON_PAINT_CERCLE_EXTERIEUR_REMPLISSAGE_INACTIF);

    // Le remplissage
    if (infos.baliseActive && infos.releveValide && (releve != null) && (releve.date != null))
    {
      final long currentTime = infos.drawPeremption ? Utils.toUTC(System.currentTimeMillis()) : releve.date.getTime();
      infos.paintInterieur = ((currentTime - releve.date.getTime()) > DELTA_PEREMPTION_MS ? WIND_ICON_PAINT_CERCLE_INTERIEUR_REMPLISSAGE_PERIMEE : WIND_ICON_PAINT_CERCLE_INTERIEUR_REMPLISSAGE_ACTIF);
    }
    else
    {
      infos.paintInterieur = WIND_ICON_PAINT_CERCLE_INTERIEUR_REMPLISSAGE_INACTIF;
    }

    // La valeur de vent et la tendance
    if (infos.releveValide && (releve != null))
    {
      // Limite
      infos.windLimitOk = isWindLimitOk(releve);
      infos.paintValeur = (infos.windLimitOk ? WIND_ICON_PAINT_VALEUR_OK : WIND_ICON_PAINT_VALEUR_KO);

      // Valeur
      final double value;
      final double valueTendance;
      if (WIND_VALUE_DISPLAYED == WIND_VALUE_DISPLAYED_MOY)
      {
        value = releve.ventMoyen;
        valueTendance = releve.ventMoyenTendance;
      }
      else if (WIND_VALUE_DISPLAYED == WIND_VALUE_DISPLAYED_MAX)
      {
        value = releve.ventMaxi;
        valueTendance = releve.ventMaxiTendance;
      }
      else if (WIND_VALUE_DISPLAYED == WIND_VALUE_DISPLAYED_MIN)
      {
        value = releve.ventMini;
        valueTendance = releve.ventMiniTendance;
      }
      else
      {
        value = Double.NaN;
        valueTendance = Double.NaN;
      }
      infos.texteValeur = (Double.isNaN(value) ? TEXT_NON_AVAILABLE : Long.toString(Math.round(ActivityCommons.getFinalSpeed(value))));

      // Tendance
      validateTendance(infos, releve, value, valueTendance);
    }

    // Pour palier au bug inexplicable de la perte de l'alignement au centre
    if (infos.paintValeur != null)
    {
      infos.paintValeur.setTextAlign(Align.CENTER);
    }
  }

  /**
   * 
   * @param infos
   * @param releve
   * @param value
   * @param valueTendance
   * @return
   */
  public static TendanceVent validateTendance(final WindIconInfos infos, final Releve releve, final double value, final double valueTendance)
  {
    // Tendance
    infos.tendanceVent = BaliseProviderUtils.getTendanceVent(DELTA_PEREMPTION_MS, releve.date, releve.dateRelevePrecedent, value, valueTendance);
    final float rotation;
    switch (infos.tendanceVent)
    {
      case FAIBLE_BAISSE:
        infos.paintTendanceVent = WIND_ICON_PAINT_TENDANCE_VENT_FAIBLE;
        infos.deltaYValeur = -WIND_ICON_DELTA_Y_VALEUR_TENDANCE;
        rotation = 90;
        break;
      case FAIBLE_HAUSSE:
        infos.paintTendanceVent = WIND_ICON_PAINT_TENDANCE_VENT_FAIBLE;
        infos.deltaYValeur = WIND_ICON_DELTA_Y_VALEUR_TENDANCE;
        rotation = -90;
        break;
      case FORTE_HAUSSE:
        infos.paintTendanceVent = WIND_ICON_PAINT_TENDANCE_VENT_FORTE;
        infos.deltaYValeur = WIND_ICON_DELTA_Y_VALEUR_TENDANCE;
        rotation = -90;
        break;
      case FORTE_BAISSE:
        infos.paintTendanceVent = WIND_ICON_PAINT_TENDANCE_VENT_FORTE;
        infos.deltaYValeur = -WIND_ICON_DELTA_Y_VALEUR_TENDANCE;
        rotation = 90;
        break;
      default:
        infos.paintTendanceVent = WIND_ICON_PAINT_TENDANCE_VENT_STABLE;
        infos.deltaYValeur = 0;
        rotation = 0;
        break;
    }
    if (infos.paintTendanceVent != null)
    {
      infos.pathTendanceVent.set(WIND_ICON_PATH_TENDANCE_VENT);
      infos.matrix.reset();
      infos.matrix.postRotate(rotation);
      infos.pathTendanceVent.transform(infos.matrix);
    }

    return infos.tendanceVent;
  }

  /**
   * 
   * @param canvas
   * @param point
   * @param infos
   * @param rotation
   */
  private static void drawTendanceVentStable(final Canvas canvas, final Point point, final WindIconInfos infos)
  {
    final float x1 = point.x - WIND_ICON_TENDANCE_STABLE_DELTA_X;
    final float x2 = point.x + WIND_ICON_TENDANCE_STABLE_DELTA_X;
    final float y1 = point.y - WIND_ICON_TENDANCE_STABLE_DELTA_Y;
    final float y2 = point.y + WIND_ICON_TENDANCE_STABLE_DELTA_Y;
    canvas.drawLine(x1, y1, x2, y1, infos.paintTendanceVent);
    canvas.drawLine(x1, y2, x2, y2, infos.paintTendanceVent);
  }

  /**
   * 
   * @param canvas
   * @param point
   * @param infos
   * @param rotation
   */
  private static void drawTendanceVent(final Canvas canvas, final Point point, final WindIconInfos infos)
  {
    infos.pathTendanceVent.offset(point.x, point.y);

    canvas.drawPath(infos.pathTendanceVent, infos.paintTendanceVent);

    infos.pathTendanceVent.offset(-point.x, -point.y);
  }

  /**
   * 
   * @param canvas
   * @param point
   * @param infos
   */
  public static void drawTendanceVentIfNeeded(final Canvas canvas, final Point point, final WindIconInfos infos)
  {
    if ((infos.tendanceVent != null) && (infos.tendanceVent != TendanceVent.INCONNUE) && (infos.tendanceVent != TendanceVent.STABLE))
    {
      drawTendanceVent(canvas, point, infos);
    }
  }

  /**
   * 
   * @param canvas
   * @param point
   * @param infos
   */
  public static void drawWindIcon(final Canvas canvas, final Point point, final WindIconInfos infos)
  {
    // Le contour du cercle exterieur
    canvas.drawCircle(point.x, point.y, WIND_ICON_RAYON_CONTOUR, WIND_ICON_PAINT_FLECHE_CERCLE_CONTOUR);

    // La fleche
    if (infos.directionValide)
    {
      // Deplacement de la fleche
      infos.pathFleche.offset(point.x, point.y);

      // Remplissage
      canvas.drawPath(infos.pathFleche, WIND_ICON_PAINT_FLECHE_REMPLISSAGE);

      // Contour
      canvas.drawPath(infos.pathFleche, WIND_ICON_PAINT_FLECHE_CERCLE_CONTOUR);

      // Deplacement de la fleche
      infos.pathFleche.offset(-point.x, -point.y);
    }

    // Le cercle exterieur
    canvas.drawCircle(point.x, point.y, WIND_ICON_RAYON, infos.paintCercleExterieurRemplissage);

    // Le remplissage
    canvas.drawCircle(point.x, point.y, WIND_ICON_PETIT_RAYON, infos.paintInterieur);

    // La tendance
    if ((infos.tendanceVent != null) && (infos.tendanceVent != TendanceVent.INCONNUE))
    {
      if (infos.tendanceVent == TendanceVent.STABLE)
      {
        drawTendanceVentStable(canvas, point, infos);
      }
      else
      {
        drawTendanceVent(canvas, point, infos);
      }
    }

    // La valeur de vent (mini, moyen ou maxi selon configuration)
    if (infos.releveValide)
    {
      canvas.drawText(infos.texteValeur, point.x, point.y + infos.deltaYValeur + WIND_ICON_Y_TEXT, infos.paintValeur);
    }
  }
}
