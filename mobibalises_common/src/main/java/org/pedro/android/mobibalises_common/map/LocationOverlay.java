package org.pedro.android.mobibalises_common.map;

import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.R;
import org.pedro.map.AbstractSimpleOverlay;
import org.pedro.map.GeoPoint;
import org.pedro.map.MercatorProjection;
import org.pedro.map.OverlayItem;
import org.pedro.map.Point;
import org.pedro.map.Rect;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * 
 * @author pedro.m
 *
 */
public class LocationOverlay extends AbstractSimpleOverlay<Bitmap, Canvas>
{
  protected static final String CURRENT                = "current";
  private static final int      HANDLER_LOCATION_HIDER = 0;
  private static final long     LOCATION_HIDE_TIMEOUT  = 120;                             // En secondes

  protected MapView             mapView;
  protected OverlayItem<Canvas> locationItem;
  private float                 precision;
  private HideDelayedHandler    locationHideDelayedHandler;
  private final Point           itemPoint              = new Point();
  private final Point           drawPoint              = new Point();
  private final Paint           paintDisqueImprecision = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint           paintCercleImprecision = new Paint(Paint.ANTI_ALIAS_FLAG);

  /**
   * 
   * @author pedro.m
   */
  private static final class HideDelayedHandler extends Handler
  {
    private LocationOverlay locationOverlay;

    /**
     * 
     * @param locationOverlay
     */
    HideDelayedHandler(final LocationOverlay locationOverlay)
    {
      super(Looper.getMainLooper());
      this.locationOverlay = locationOverlay;
    }

    @Override
    public void handleMessage(final Message msg)
    {
      locationOverlay.hideLocation();
    }

    /**
     * 
     */
    void onShutdown()
    {
      removeMessages(HANDLER_LOCATION_HIDER);
      locationOverlay = null;
    }
  }

  /**
   * 
   * @param mapView
   */
  public LocationOverlay(final MapView mapView)
  {
    super(mapView);
    this.mapView = mapView;
    init();
  }

  @Override
  public void draw(final Canvas canvas)
  {
    if ((locationItem != null) && (locationItem.getPoint() != null) && !transformation.isIntermediateZoomInProgress())
    {
      synchronized (transformationLock)
      {
        // Calcul coordonnees
        final double x = MercatorProjection.longitudeToPixelX(locationItem.getPoint().getLongitude(), zoom);
        final double y = MercatorProjection.latitudeToPixelY(locationItem.getPoint().getLatitude(), zoom);
        final double left = MercatorProjection.longitudeToPixelX(center.getLongitude(), zoom) - (mapDisplayer.getPixelWidth() / 2);
        final double top = MercatorProjection.latitudeToPixelY(center.getLatitude(), zoom) - (mapDisplayer.getPixelHeight() / 2);
        locationItem.x = Math.round(x);
        locationItem.y = Math.round(y);
        drawPoint.set((int)Math.round(x - left), (int)Math.round(y - top));

        // Precision
        if (precision > 0)
        {
          final float rayon = projection.metersToPixels(precision, center, zoom);
          if (rayon > 5)
          {
            canvas.drawCircle(drawPoint.x, drawPoint.y, rayon, paintDisqueImprecision);
            canvas.drawCircle(drawPoint.x, drawPoint.y, rayon, paintCercleImprecision);
          }
        }

        // Point de position
        locationItem.getDrawable().draw(canvas, drawPoint);
      }
    }
  }

  @Override
  public boolean needsLateDraw()
  {
    return (locationItem.getPoint() != null);
  }

  /**
   * 
   * @param point
   */
  public void setPosition(final GeoPoint point)
  {
    locationItem.setPoint(point);
    requestRedraw();
    hideLocationDelayed();
  }

  /**
   * 
   * @param precision
   */
  public void setPrecision(final float precision)
  {
    this.precision = precision;
  }

  /**
   * 
   */
  protected void hideLocationDelayed()
  {
    locationHideDelayedHandler.removeMessages(HANDLER_LOCATION_HIDER);
    locationHideDelayedHandler.sendEmptyMessageDelayed(HANDLER_LOCATION_HIDER, LOCATION_HIDE_TIMEOUT * 1000);
  }

  /**
   * 
   */
  public void hideLocation()
  {
    locationItem.setPoint(null);
    requestRedraw();
  }

  /**
   * 
   */
  private void init()
  {
    // Disque d'imprecision
    paintDisqueImprecision.setStyle(Paint.Style.FILL);
    paintDisqueImprecision.setColor(Color.rgb(50, 50, 255));
    paintDisqueImprecision.setAlpha(50);

    // Cercle d'imprecision
    paintCercleImprecision.setStyle(Paint.Style.STROKE);
    paintCercleImprecision.setStrokeWidth(1);
    paintCercleImprecision.setColor(Color.rgb(0, 100, 255));
    paintCercleImprecision.setAlpha(100);

    // Item
    locationItem = getNewLocationItem();

    // Controle de la visibilite
    locationHideDelayedHandler = new HideDelayedHandler(this);
  }

  /**
   * 
   * @return
   */
  protected OverlayItem<Canvas> getNewLocationItem()
  {
    final LocationDrawable locationDrawable = new LocationDrawable(mapView.getResources(), R.drawable.ic_maps_indicator_current_position);
    return new OverlayItem<Canvas>(CURRENT, null, locationDrawable);
  }

  /**
   * 
   * @param point
   * @return
   */
  protected boolean isItemUnder(final Point point)
  {
    // Initialisations
    Rect bounds;

    // Inspection des items
    if (locationItem.getPoint() != null)
    {
      mapDisplayer.getProjection().toPixels(locationItem.getPoint(), itemPoint);
      bounds = locationItem.getDrawable().getInteractiveBounds();
      if ((point.x >= itemPoint.x + bounds.left) && (point.x <= itemPoint.x + bounds.right) && (point.y >= itemPoint.y + bounds.top) && (point.y <= itemPoint.y + bounds.bottom))
      {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean onDoubleTap(final GeoPoint geoPoint, final Point point)
  {
    return false;
  }

  @Override
  public boolean onLongTap(final GeoPoint geoPoint, final Point point)
  {
    return false;
  }

  @Override
  public boolean onTap(final GeoPoint geoPoint, final Point point)
  {
    if (isItemUnder(point))
    {
      hideLocation();

      return true;
    }

    return false;
  }

  @Override
  public void onShutdown()
  {
    // Handlers
    locationHideDelayedHandler.onShutdown();

    // Parent
    super.onShutdown();

    // Liberation vue
    mapView = null;
  }

  @Override
  public void onMoveInputFinished(final GeoPoint inCenter, final GeoPoint topLeft, final GeoPoint bottomRight, final double pixelLatAngle, final double pixelLngAngle)
  {
    // Nothing
  }
}
