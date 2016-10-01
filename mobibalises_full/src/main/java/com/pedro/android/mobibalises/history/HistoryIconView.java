package com.pedro.android.mobibalises.history;

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
public class HistoryIconView extends View
{
  private final WindIconInfos           windInfos           = new WindIconInfos();
  private final AdditionalWindIconInfos additionalWindInfos = new AdditionalWindIconInfos();
  private final WeatherIconInfos        weatherInfos        = new WeatherIconInfos();
  private Balise                        balise;
  private Releve                        releve;
  private final Object                  validationLock      = new Object();
  private boolean                       validated;

  private final Point                   center              = new Point();

  /**
   * 
   * @param context
   */
  public HistoryIconView(final Context context)
  {
    this(context, null);
  }

  /**
   * 
   * @param context
   * @param attrs
   */
  public HistoryIconView(final Context context, final AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  /**
   * 
   * @param context
   * @param attrs
   * @param defStyle
   */
  public HistoryIconView(final Context context, final AttributeSet attrs, final int defStyle)
  {
    super(context, attrs, defStyle);
    windInfos.setDrawPeremption(false);
  }

  /**
   * 
   * @param inWindInfos
   * @param inAdditionalWindInfos
   * @param inWeatherInfos
   * @param inReleve
   */
  protected void setBaliseDatas(final Releve inReleve)
  {
    synchronized (validationLock)
    {
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

  /**
   * 
   * @param inBalise
   */
  protected void setBalise(final Balise inBalise)
  {
    balise = inBalise;
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
