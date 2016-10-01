package com.pedro.android.mobibalises.view;

import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.view.BaliseDrawable;
import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.map.Point;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * 
 * @author pedro.m
 */
public class FullBaliseDrawable extends BaliseDrawable
{
  private static boolean                GRAPHICS_INITIALIZED      = false;
  private static final Object           GRAPHICS_INITIALIZED_LOCK = new Object();

  // Infos vent supplementaires
  private final AdditionalWindIconInfos additionalWindIconInfos   = new AdditionalWindIconInfos();

  // Infos meteo
  private final WeatherIconInfos        weatherIconInfos          = new WeatherIconInfos();

  /**
   * 
   * @param idBalise
   * @param provider
   * @param mapView
   * @param touchableTitle
   */
  public FullBaliseDrawable(final String idBalise, final BaliseProvider provider, final MapView mapView, final String touchableTitle)
  {
    super(idBalise, provider, mapView, touchableTitle);
  }

  /**
   * 
   * @param context
   */
  public static void initGraphics(final Context context)
  {
    // Initialisation une seule fois !
    synchronized (GRAPHICS_INITIALIZED_LOCK)
    {
      if (GRAPHICS_INITIALIZED)
      {
        return;
      }

      // Initialisations communes
      FullDrawingCommons.initialize(context);

      // Fin
      GRAPHICS_INITIALIZED = true;
    }
  }

  @Override
  protected void validateGraphics()
  {
    // Appel du parent
    super.validateGraphics();

    // Validation
    final Balise balise = provider.getBaliseById(idBalise);
    final Releve releve = provider.getReleveById(idBalise);

    // Infos vent supplementaires
    FullDrawingCommons.validateAdditionalWindIconInfos(additionalWindIconInfos, balise, releve);

    // Icone meteo
    FullDrawingCommons.validateWeatherIconInfos(weatherIconInfos, windIconInfos, balise, releve);
  }

  @Override
  public void draw(final Canvas canvas, final Point point)
  {
    // Validation
    validateDrawable();

    // Icone meteo
    if (BaliseDrawable.drawWeatherIcon)
    {
      // Materialisation de la balise si besoin (si infos vent non affichees)
      if (!BaliseDrawable.drawWindIcon)
      {
        FullDrawingCommons.drawWeatherPoint(canvas, point);
      }

      // Icone meteo
      FullDrawingCommons.drawWeatherIcon(canvas, point, weatherIconInfos);
    }

    // Icone vent
    if (BaliseDrawable.drawWindIcon)
    {
      // Infos vent supplementaires
      final Paint fillPaint = (windIconInfos.isWindLimitOk() ? FullDrawingCommons.paintSecteurVariationVentVertFonce : FullDrawingCommons.paintSecteurVariationVentRougeFonce);
      final Paint strokePaint = (windIconInfos.isWindLimitOk() ? FullDrawingCommons.paintSecteurVariationVentVertStroke : FullDrawingCommons.paintSecteurVariationVentRougeStroke);
      FullDrawingCommons.drawAdditionalWindIcon(canvas, point, additionalWindIconInfos, fillPaint, strokePaint);

      // Infos vent standard
      DrawingCommons.drawWindIcon(canvas, point, windIconInfos);
    }

    // Les details
    if (drawDetails)
    {
      drawDetails(canvas, point);
    }
  }
}
