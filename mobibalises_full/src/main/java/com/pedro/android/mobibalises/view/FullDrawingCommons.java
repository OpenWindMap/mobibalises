package com.pedro.android.mobibalises.view;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.android.mobibalises_common.view.WindIconInfos;
import org.pedro.balises.Balise;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.map.Point;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.FloatMath;

/**
 * 
 * @author pedro.m
 */
public class FullDrawingCommons
{
  // Resources
  private static Resources      resources;

  // Icone meteo
  private static int            demieLargeur;
  private static int            weatherPointRadius;
  private static final Paint    weatherPointFillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
  private static final Paint    weatherPointStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  // Divers
  private static float          scalingFactor;
  private static boolean        INITIALIZED;
  private static final Object   INITIALIZED_LOCK        = new Object();

  // Infos vent supplementaires
  public static Paint           paintSecteurVariationVentRougeClair;
  public static Paint           paintSecteurVariationVentRougeFonce;
  public static Paint           paintSecteurVariationVentRougeStroke;
  public static Paint           paintSecteurVariationVentVertClair;
  public static Paint           paintSecteurVariationVentVertFonce;
  public static Paint           paintSecteurVariationVentVertStroke;

  // Limites de pluie
  private static final double[] LIMITES_PRECIPITATIONS  = new double[] { 3.0, 6.0, 9.0, 12.0 };

  /**
   * 
   * @param context
   * @param inScalingFactor
   */
  public static void initialize(final Context context)
  {
    // Initialisation une seule fois !
    synchronized (INITIALIZED_LOCK)
    {
      if (INITIALIZED)
      {
        return;
      }

      // Resources
      resources = context.getResources();

      // Divers
      final DisplayMetrics metrics = ActivityCommons.getDisplayMetrics(context);
      scalingFactor = metrics.density;

      // Bounds de l'icone
      demieLargeur = (int)Math.ceil(DrawingCommons.WIND_ICON_FLECHE_MAX * scalingFactor);
      weatherPointRadius = Math.round(DrawingCommons.RAYON_SPOT_WEATHER * scalingFactor);

      // Paint du point de meteo
      weatherPointFillPaint.setStyle(Paint.Style.FILL);
      weatherPointFillPaint.setColor(resources.getColor(R.color.map_balise_weather_point_fill));
      weatherPointStrokePaint.setStyle(Paint.Style.STROKE);
      weatherPointStrokePaint.setStrokeWidth(2);
      weatherPointStrokePaint.setColor(resources.getColor(R.color.map_balise_weather_point_stroke));

      // Infos vent supplementaires (rouge)
      paintSecteurVariationVentRougeFonce = new Paint(Paint.ANTI_ALIAS_FLAG);
      paintSecteurVariationVentRougeFonce.setColor(resources.getColor(R.color.map_balise_variation_vent_disque_rouge_fonce));
      paintSecteurVariationVentRougeFonce.setStyle(Paint.Style.FILL);
      paintSecteurVariationVentRougeClair = new Paint(paintSecteurVariationVentRougeFonce);
      paintSecteurVariationVentRougeClair.setColor(resources.getColor(R.color.map_balise_variation_vent_disque_rouge_clair));
      paintSecteurVariationVentRougeStroke = new Paint(paintSecteurVariationVentRougeFonce);
      paintSecteurVariationVentRougeStroke.setAlpha(255);
      paintSecteurVariationVentRougeStroke.setStyle(Paint.Style.STROKE);

      // Infos vent supplementaires (vert)
      paintSecteurVariationVentVertFonce = new Paint(Paint.ANTI_ALIAS_FLAG);
      paintSecteurVariationVentVertFonce.setColor(resources.getColor(R.color.map_balise_variation_vent_disque_vert_fonce));
      paintSecteurVariationVentVertFonce.setStyle(Paint.Style.FILL);
      paintSecteurVariationVentVertClair = new Paint(paintSecteurVariationVentVertFonce);
      paintSecteurVariationVentVertClair.setColor(resources.getColor(R.color.map_balise_variation_vent_disque_vert_clair));
      paintSecteurVariationVentVertStroke = new Paint(paintSecteurVariationVentVertClair);
      paintSecteurVariationVentVertStroke.setAlpha(255);
      paintSecteurVariationVentVertStroke.setStyle(Paint.Style.STROKE);

      // Fin
      INITIALIZED = true;
    }
  }

  /**
   * 
   * @param canvas
   * @param point
   * @param weatherInfos
   */
  public static void drawWeatherIcon(final Canvas canvas, final Point point, final WeatherIconInfos weatherInfos)
  {
    // Nuage
    if (weatherInfos.nuage != null)
    {
      // Soleil / Lune
      /* TODO : soleil/lune ?
      if (weatherInfos.nuage.drawSunMoon())
      {
        final Drawable sunMoon = resources.getDrawable(weatherInfos.night ? R.drawable.ic_lune_etoiles : R.drawable.ic_soleil);
        sunMoon.setBounds(point.x - demieLargeur, point.y - demieLargeur, point.x + demieLargeur, point.y + demieLargeur);
        sunMoon.draw(canvas);
      }
      */

      // Nuage
      if (weatherInfos.nuage.getResourceId() >= 0)
      {
        final Drawable tempo = resources.getDrawable(weatherInfos.nuage.getResourceId());
        tempo.setBounds(point.x - demieLargeur, point.y - demieLargeur, point.x + demieLargeur, point.y + demieLargeur);
        tempo.draw(canvas);
      }
    }

    // Precipitations
    if ((weatherInfos.precipitation != null) && (weatherInfos.precipitation.getResourceId() >= 0))
    {
      final Drawable tempo = resources.getDrawable(weatherInfos.precipitation.getResourceId());
      tempo.setBounds(point.x - demieLargeur, point.y - demieLargeur, point.x + demieLargeur, point.y + demieLargeur);
      tempo.draw(canvas);
    }
  }

  /**
   * 
   * @param canvas
   * @param point
   */
  public static void drawWeatherPoint(final Canvas canvas, final Point point)
  {
    canvas.drawCircle(point.x, point.y, weatherPointRadius, weatherPointFillPaint);
    canvas.drawCircle(point.x, point.y, weatherPointRadius, weatherPointStrokePaint);
  }

  /**
   * 
   * @param infos
   * @param balise
   * @param releve
   */
  @SuppressWarnings("unused")
  public static void validateWeatherIconInfos(final WeatherIconInfos weatherInfos, final WindIconInfos windInfos, final Balise balise, final Releve releve)
  {
    if (windInfos.isBaliseActive() && windInfos.isReleveValide())
    {
      // Precipitations
      if ((Double.isNaN(releve.hydrometrie) || (releve.hydrometrie == 0)) && ((releve.pluie == Integer.MIN_VALUE) || (releve.pluie == 0)))
      {
        weatherInfos.precipitation = null;
      }
      else if ((releve.pluie != Integer.MIN_VALUE) && (releve.pluie > 0))
      {
        final WeatherIconInfos.Precipitation precipitations[] = WeatherIconInfos.Precipitation.values();
        weatherInfos.precipitation = precipitations[releve.pluie];
      }
      else if (!Double.isNaN(releve.hydrometrie) && (releve.hydrometrie > 0))
      {
        final WeatherIconInfos.Precipitation precipitations[] = WeatherIconInfos.Precipitation.values();
        weatherInfos.precipitation = precipitations[precipitations.length - 1];
        for (int i = 0; i < precipitations.length - 1; i++)
        {
          if (releve.hydrometrie < LIMITES_PRECIPITATIONS[i])
          {
            weatherInfos.precipitation = precipitations[i];
            break;
          }
        }
      }

      // Nuages
      if (releve.nuages == Integer.MIN_VALUE)
      {
        weatherInfos.nuage = null;
      }
      else
      {
        final boolean cbcu = (Utils.isBooleanNull(releve.nuagesBourgeonnants) ? false : Utils.getBooleanValue(releve.nuagesBourgeonnants));
        switch (releve.nuages)
        {
          case 0:
            weatherInfos.nuage = WeatherIconInfos.Nuage.CLAIR;
            break;
          case 1:
          case 2:
            weatherInfos.nuage = (cbcu ? WeatherIconInfos.Nuage.UN_DEUX_CBCU : WeatherIconInfos.Nuage.UN_DEUX);
            break;
          case 3:
          case 4:
            weatherInfos.nuage = (cbcu ? WeatherIconInfos.Nuage.TROIS_QUATRE_CBCU : WeatherIconInfos.Nuage.TROIS_QUATRE);
            break;
          case 5:
          case 6:
            weatherInfos.nuage = (cbcu ? WeatherIconInfos.Nuage.CINQ_SIX_CBCU : WeatherIconInfos.Nuage.CINQ_SIX);
            break;
          case 7:
          case 8:
            weatherInfos.nuage = (cbcu ? WeatherIconInfos.Nuage.SEPT_HUIT_CBCU : WeatherIconInfos.Nuage.SEPT_HUIT);
            break;
        }
      }

      // Jour / Nuit
      /* TODO : soleil/lune ?
      final CalculSolaire calculSolaire = new CalculSolaire();
      calculSolaire.calculLeverCoucher(releve.date, balise.latitude.doubleValue(), balise.longitude.doubleValue());
      final long releveMillis = releve.date.getTime();
      weatherInfos.night = !((releveMillis > calculSolaire.heureLever.getTime()) && (releveMillis < calculSolaire.heureCoucher.getTime()));
      */
    }
    else
    {
      // Precipitations
      weatherInfos.precipitation = null;

      // Nuages
      weatherInfos.nuage = null;

      // Jour/Nuit
      weatherInfos.night = false;
    }
  }

  /**
   * 
   * @param additionalInfos
   * @param balise
   * @param releve
   */
  public static void validateAdditionalWindIconInfos(final AdditionalWindIconInfos additionalInfos, final Balise balise, final Releve releve)
  {
    // Validation
    additionalInfos.baliseActive = (balise != null) && !Utils.isBooleanNull(balise.active) && Utils.getBooleanValue(balise.active);
    additionalInfos.releveValide = additionalInfos.baliseActive && (releve != null);
    final boolean directionVariation1Valide = additionalInfos.releveValide && (releve != null) && DrawingCommons.isDirectionOk(releve.directionVentVariation1);
    final boolean directionVariation2Valide = additionalInfos.releveValide && (releve != null) && DrawingCommons.isDirectionOk(releve.directionVentVariation2);
    additionalInfos.variationVentValide = (directionVariation1Valide && directionVariation2Valide && (releve.directionVentVariation1 != releve.directionVentVariation2));

    if (additionalInfos.variationVentValide)
    {
      // Calculs d'angles
      final float delta;
      if (releve.directionVentVariation1 > releve.directionVentVariation2)
      {
        delta = 360 - releve.directionVentVariation1 + releve.directionVentVariation2;
      }
      else
      {
        delta = releve.directionVentVariation2 - releve.directionVentVariation1;
      }

      // Secteur variations
      final float rayon = scalingFactor * DrawingCommons.WIND_ICON_FLECHE_MAX;
      additionalInfos.pathVariationVent.reset();
      additionalInfos.pathVariationVent.lineTo(0, -rayon);
      additionalInfos.pathVariationVent.arcTo(new RectF(-rayon, -rayon, rayon, rayon), -90, delta);
      additionalInfos.pathVariationVent.lineTo(0, 0);
      additionalInfos.pathVariationVent.setLastPoint(0, 0);

      // Rotation secteur
      final Matrix matrix = new Matrix();
      matrix.postRotate((releve.directionVentVariation1 + 180) % 360);
      additionalInfos.pathVariationVent.transform(matrix);
    }
  }

  /**
   * 
   * @param canvas
   * @param point
   * @param additionalWindIconInfos
   * @param paintSecteurFill
   * @param paintSecteurStroke
   */
  public static void drawAdditionalWindIcon(final Canvas canvas, final Point point, final AdditionalWindIconInfos additionalWindIconInfos, final Paint paintSecteurFill, final Paint paintSecteurStroke)
  {
    if (additionalWindIconInfos.variationVentValide)
    {
      additionalWindIconInfos.pathVariationVent.offset(point.x, point.y);
      canvas.drawPath(additionalWindIconInfos.pathVariationVent, paintSecteurFill);
      canvas.drawPath(additionalWindIconInfos.pathVariationVent, paintSecteurStroke);
      additionalWindIconInfos.pathVariationVent.offset(-point.x, -point.y);
    }
  }
}
