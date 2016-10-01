package com.pedro.android.mobibalises.favorites;

import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.android.mobibalises_common.view.WindIconInfos;
import org.pedro.balises.Balise;
import org.pedro.balises.Releve;
import org.pedro.map.Point;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.pedro.android.mobibalises.view.AdditionalWindIconInfos;
import com.pedro.android.mobibalises.view.FullDrawingCommons;
import com.pedro.android.mobibalises.view.WeatherIconInfos;

/**
 * 
 * @author pedro.m
 */
public class FavoritesIconView extends View
{
  private WindIconInfos           windInfos;
  private AdditionalWindIconInfos additionalWindInfos;
  private WeatherIconInfos        weatherInfos;
  private Balise                  balise;
  private Releve                  releve;
  private final Object            validationLock = new Object();
  private boolean                 validated;

  private final Point             center         = new Point();

  /**
   * 
   * @param context
   */
  public FavoritesIconView(final Context context)
  {
    this(context, null);
  }

  /**
   * 
   * @param context
   * @param attrs
   */
  public FavoritesIconView(final Context context, final AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  /**
   * 
   * @param context
   * @param attrs
   * @param defStyle
   */
  public FavoritesIconView(final Context context, final AttributeSet attrs, final int defStyle)
  {
    super(context, attrs, defStyle);
  }

  /**
   * 
   * @param inWindInfos
   * @param inAdditionalWindInfos
   * @param inWeatherInfos
   * @param inBalise
   * @param inReleve
   */
  protected void setBaliseDatas(final WindIconInfos inWindInfos, final AdditionalWindIconInfos inAdditionalWindInfos, final WeatherIconInfos inWeatherInfos, final Balise inBalise, final Releve inReleve)
  {
    synchronized (validationLock)
    {
      windInfos = inWindInfos;
      additionalWindInfos = inAdditionalWindInfos;
      weatherInfos = inWeatherInfos;
      balise = inBalise;
      releve = inReleve;
      validated = false;
    }
  }

  /**
   * 
   */
  private void validate()
  {
    synchronized (validationLock)
    {
      if (validated)
      {
        return;
      }

      DrawingCommons.validateWindIconInfos(windInfos, balise, releve);
      FullDrawingCommons.validateAdditionalWindIconInfos(additionalWindInfos, balise, releve);
      FullDrawingCommons.validateWeatherIconInfos(weatherInfos, windInfos, balise, releve);
      validated = true;
    }
  }

  @Override
  protected synchronized void onSizeChanged(final int newWidth, final int newHeight, final int oldWidth, final int oldHeight)
  {
    center.set(newWidth / 2, newHeight / 2);
  }

  @Override
  public void draw(final Canvas canvas)
  {
    // Validation si besoin
    validate();

    // Dessin
    FullDrawingCommons.drawWeatherIcon(canvas, center, weatherInfos);
    final Paint fillPaint = (windInfos.isWindLimitOk() ? FullDrawingCommons.paintSecteurVariationVentVertClair : FullDrawingCommons.paintSecteurVariationVentRougeClair);
    final Paint strokePaint = (windInfos.isWindLimitOk() ? FullDrawingCommons.paintSecteurVariationVentVertStroke : FullDrawingCommons.paintSecteurVariationVentRougeStroke);
    FullDrawingCommons.drawAdditionalWindIcon(canvas, center, additionalWindInfos, fillPaint, strokePaint);
    DrawingCommons.drawWindIcon(canvas, center, windInfos);
  }
}
