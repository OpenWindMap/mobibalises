package com.pedro.android.mobibalises.map;

import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.map.AbstractBalisesMapActivity;
import org.pedro.android.mobibalises_common.map.LocationDrawable;
import org.pedro.android.mobibalises_common.map.LocationOverlay;
import org.pedro.map.GeoPoint;
import org.pedro.map.MapDrawable;
import org.pedro.map.OverlayItem;
import org.pedro.map.Point;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;

import com.pedro.android.mobibalises.R;

/**
 * 
 * @author pedro.m
 */
public final class FullLocationOverlay extends LocationOverlay
{
  private static final float              FLIGHT_MODE_CANCELER_ICON_SIZE_PERCENT = 0.75f;
  private static final long               FLIGHT_MODE_CANCELER_TIMEOUT           = 30000;

  boolean                                 flightMode                             = false;

  private final int                       flightModeCancelerTouchSize;
  boolean                                 drawFlightModeCanceler                 = false;
  private FlightModeCancelListener        flightModeCancelListener               = null;
  private final Drawable                  flightModeCancelerDrawable;
  private final Rect                      flightModeCancelerBounds               = new Rect();
  private final Rect                      flightModeCancelerDrawableBounds       = new Rect();
  private final Paint                     flightModeCancelerBackgroundPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF                     flightModeCancelerOval                 = new RectF();
  private final FlightModeCancelerHandler flightModeCancelerHandler;

  /**
   * 
   * @author pedro.m
   */
  public interface FlightModeCancelListener
  {
    public void onFlightModeCanceled();
  }

  /**
   * 
   * @author pedro.m
   */
  private static class FlightModeCancelerHandler extends Handler
  {
    private FullLocationOverlay fullLocationOverlay;

    /**
     * 
     * @param favoritesActivity
     */
    FlightModeCancelerHandler(final FullLocationOverlay fullLocationOverlay)
    {
      super(Looper.getMainLooper());
      this.fullLocationOverlay = fullLocationOverlay;
    }

    @Override
    public void handleMessage(final Message msg)
    {
      fullLocationOverlay.drawFlightModeCanceler = false;
      fullLocationOverlay.requestRedraw();
    }

    /**
     * 
     */
    void onShutdown()
    {
      removeMessages(0);
      fullLocationOverlay = null;
    }
  }

  /**
   * 
   * @param mapView
   * @param mapActivity
   */
  public FullLocationOverlay(final MapView mapView, final AbstractBalisesMapActivity mapActivity)
  {
    super(mapView);

    // Initialisations communes
    final DisplayMetrics metrics = ActivityCommons.getDisplayMetrics(mapActivity.getApplicationContext());

    // Taille de la zone de desactivation du mode vol
    flightModeCancelerTouchSize = Math.round(60 * metrics.density);

    // Zone de desactivation du mode vol
    flightModeCancelerBounds.top = 0;
    flightModeCancelerBounds.bottom = flightModeCancelerTouchSize;

    // Quart de disque
    flightModeCancelerBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    flightModeCancelerBackgroundPaint.setColor(Color.BLUE);
    flightModeCancelerBackgroundPaint.setAlpha(95);

    // Icone
    flightModeCancelerDrawableBounds.top = 0;
    flightModeCancelerDrawableBounds.bottom = Math.round(flightModeCancelerTouchSize * FLIGHT_MODE_CANCELER_ICON_SIZE_PERCENT);
    final Resources resources = mapActivity.getResources();
    flightModeCancelerDrawable = resources.getDrawable(R.drawable.ic_menu_mode_vol_on);

    // Controle de la visibilite de la zone d'annulation du mode vol
    flightModeCancelerHandler = new FlightModeCancelerHandler(this);
  }

  @Override
  protected OverlayItem<Canvas> getNewLocationItem()
  {
    final LocationDrawable locationDrawable = new LocationDrawable(mapView.getResources(), R.drawable.ic_maps_indicator_current_position);
    final LocationDrawable flightModeDrawable = new LocationDrawable(mapView.getResources(), R.drawable.ic_maps_indicator_current_position_red);

    return new OverlayItem<Canvas>(CURRENT, null, locationDrawable)
    {
      @Override
      public final MapDrawable<Canvas> getDrawable()
      {
        return (flightMode ? flightModeDrawable : locationDrawable);
      }
    };
  }

  @Override
  public void setPosition(final GeoPoint point)
  {
    locationItem.setPoint(point);
    requestRedraw();
    if (!flightMode)
    {
      hideLocationDelayed();
    }
  }

  @Override
  public boolean onTap(final GeoPoint geoPoint, final Point point)
  {
    // Gestion de l'annulation du mode vol
    if (flightMode && drawFlightModeCanceler && (flightModeCancelListener != null) && (point.x >= flightModeCancelerBounds.left) && (point.x <= flightModeCancelerBounds.right) && (point.y <= flightModeCancelerBounds.bottom))
    {
      flightMode = false;
      flightModeCancelListener.onFlightModeCanceled();
      return true;
    }

    // Gestion de l'icone de positionnement
    if (!flightMode && isItemUnder(point))
    {
      hideLocation();
      return true;
    }

    return false;
  }

  /**
   * 
   * @param flightMode
   */
  public void setFlightMode(final boolean flightMode, final FlightModeCancelListener inFlightModeCancelListener)
  {
    this.flightMode = flightMode;
    this.flightModeCancelListener = inFlightModeCancelListener;
    flightModeCancelerHandler.removeMessages(0);
    if (flightMode)
    {
      drawFlightModeCanceler = true;
      flightModeCancelerHandler.sendEmptyMessageDelayed(0, FLIGHT_MODE_CANCELER_TIMEOUT);
    }
    else
    {
      hideLocation();
    }
  }

  @Override
  public void draw(final Canvas canvas)
  {
    // Icone mode vol
    if (flightMode && drawFlightModeCanceler)
    {
      // Zone
      flightModeCancelerBounds.left = mapView.getWidth() - flightModeCancelerTouchSize;
      flightModeCancelerBounds.right = mapView.getWidth();

      // Quart de disque rouge semi-transparent
      flightModeCancelerOval.left = mapView.getWidth() - flightModeCancelerTouchSize;
      flightModeCancelerOval.right = mapView.getWidth() + flightModeCancelerTouchSize;
      flightModeCancelerOval.top = -flightModeCancelerTouchSize;
      flightModeCancelerOval.bottom = flightModeCancelerTouchSize;
      canvas.drawArc(flightModeCancelerOval, 90, 180, true, flightModeCancelerBackgroundPaint);

      // Icone
      flightModeCancelerDrawableBounds.left = mapView.getWidth() - Math.round(flightModeCancelerTouchSize * FLIGHT_MODE_CANCELER_ICON_SIZE_PERCENT);
      flightModeCancelerDrawableBounds.right = mapView.getWidth();
      flightModeCancelerDrawable.setBounds(flightModeCancelerDrawableBounds);
      flightModeCancelerDrawable.draw(canvas);
    }

    // Parent
    super.draw(canvas);
  }

  @Override
  public void onShutdown()
  {
    // Handlers
    flightModeCancelerHandler.onShutdown();

    // Parent
    super.onShutdown();
  }
}
