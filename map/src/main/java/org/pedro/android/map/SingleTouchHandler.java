package org.pedro.android.map;

import org.pedro.map.FadeOutMover;
import org.pedro.map.GeoPoint;
import org.pedro.map.Overlay;
import org.pedro.map.Point;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

/**
 * 
 * @author pedro.m
 */
public class SingleTouchHandler extends AbstractTouchHandler
{
  private static final float         FLING_LOW  = 50;
  private static final float         FLING_HIGH = 750;

  boolean                            locked     = false;

  FadeOutMover                       fadeOutMover;

  private final GestureDetector      detector;
  private SingleTouchGestureListener listener;

  /**
   * 
   * @author pedro.m
   */
  private static class SingleTouchGestureListener implements OnGestureListener, OnDoubleTapListener
  {
    private SingleTouchHandler singleTouchHandler;
    private final Point        tapPoint    = new Point();
    private final GeoPoint     tapGeoPoint = new GeoPoint();

    /**
     * 
     * @param singleTouchHandler
     */
    SingleTouchGestureListener(final SingleTouchHandler singleTouchHandler)
    {
      super();
      this.singleTouchHandler = singleTouchHandler;
    }

    @Override
    public boolean onDown(final MotionEvent event)
    {
      // Verrou ?
      if (singleTouchHandler.locked)
      {
        return true;
      }

      // Arret du mouvement courant
      singleTouchHandler.controller.stopMove();

      return true;
    }

    @Override
    public boolean onFling(final MotionEvent event1, final MotionEvent event2, final float deltaX, final float deltaY)
    {
      // Verrou ?
      if (singleTouchHandler.locked)
      {
        return true;
      }

      // Arret du mouvement courant
      singleTouchHandler.controller.stopMove();

      // Limitation basse
      if ((Math.abs(deltaX) > FLING_LOW) || (Math.abs(deltaY) > FLING_LOW))
      {
        // Limitation haute
        final float initialX = Math.max(Math.min(deltaX, FLING_HIGH), -FLING_HIGH);
        final float initialY = Math.max(Math.min(deltaY, FLING_HIGH), -FLING_HIGH);

        // Mouvement !
        singleTouchHandler.fadeOutMover.initialize(initialX, initialY);
        singleTouchHandler.controller.setMover(singleTouchHandler.fadeOutMover);
      }

      return true;
    }

    @Override
    public void onLongPress(final MotionEvent event)
    {
      // Verrou ?
      if (singleTouchHandler.locked)
      {
        return;
      }

      // Gestion du message de disponibilite du cache
      singleTouchHandler.mapView.manageCacheAvailabilityMessage();

      // Overlays
      tapPoint.set(Math.round(event.getX()), Math.round(event.getY()));
      singleTouchHandler.projection.fromPixels(tapPoint.x, tapPoint.y, tapGeoPoint);
      try
      {
        for (final Overlay<?, ?> overlay : singleTouchHandler.mapView.getOverlays(false))
        {
          if (overlay.onLongTap(tapGeoPoint, tapPoint))
          {
            break;
          }
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        singleTouchHandler.mapView.unlockReadOverlays();
      }
    }

    @Override
    public boolean onScroll(final MotionEvent event1, final MotionEvent event2, final float deltaX, final float deltaY)
    {
      // Verrou ?
      if (singleTouchHandler.locked)
      {
        return true;
      }

      // Scroll
      singleTouchHandler.controller.scrollBy(Math.round(-deltaX), Math.round(-deltaY));

      return true;
    }

    @Override
    public void onShowPress(final MotionEvent event)
    {
      // Nothing
    }

    @Override
    public boolean onSingleTapUp(final MotionEvent event)
    {
      return true;
    }

    @Override
    public boolean onDoubleTap(final MotionEvent event)
    {
      // Verrou ?
      if (singleTouchHandler.locked)
      {
        return true;
      }

      // Gestion du message de disponibilite du cache
      singleTouchHandler.mapView.manageCacheAvailabilityMessage();

      // Overlays
      tapPoint.set(Math.round(event.getX()), Math.round(event.getY()));
      singleTouchHandler.projection.fromPixels(tapPoint.x, tapPoint.y, tapGeoPoint);
      try
      {
        for (final Overlay<?, ?> overlay : singleTouchHandler.mapView.getOverlays(false))
        {
          if (overlay.onDoubleTap(tapGeoPoint, tapPoint))
          {
            break;
          }
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        singleTouchHandler.mapView.unlockReadOverlays();
      }

      // Carte principale : zoom sur point
      singleTouchHandler.controller.zoomIn(tapGeoPoint);
      singleTouchHandler.mapView.manageZoomControlsChange();

      return true;
    }

    @Override
    public boolean onDoubleTapEvent(final MotionEvent event)
    {
      return true;
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent event)
    {
      // Verrou ou non pret ?
      if (singleTouchHandler.locked || (singleTouchHandler.mapView == null))
      {
        return true;
      }

      // Gestion du message de disponibilite du cache
      singleTouchHandler.mapView.manageCacheAvailabilityMessage();

      // Overlays
      boolean consumedByOverlays = false;
      tapPoint.set(Math.round(event.getX()), Math.round(event.getY()));
      singleTouchHandler.projection.fromPixels(tapPoint.x, tapPoint.y, tapGeoPoint);
      try
      {
        for (final Overlay<?, ?> overlay : singleTouchHandler.mapView.getOverlays(false))
        {
          if (overlay.onTap(tapGeoPoint, tapPoint))
          {
            consumedByOverlays = true;
            break;
          }
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        singleTouchHandler.mapView.unlockReadOverlays();
      }

      // Transmission a la View
      singleTouchHandler.mapView.onSingleTap(consumedByOverlays);

      return true;
    }

    /**
     * 
     */
    void onShutdown()
    {
      singleTouchHandler = null;
    }
  }

  /**
   * 
   */
  public SingleTouchHandler()
  {
    super();
    listener = new SingleTouchGestureListener(this);
    detector = new GestureDetector(listener);
  }

  /**
   * 
   */
  private void initMovers()
  {
    fadeOutMover = new FadeOutMover(controller);
    fadeOutMover.start();
  }

  @Override
  public boolean initHandler(final Context context, final MapView inMapView)
  {
    final boolean ok = super.initHandler(context, inMapView);

    if (ok)
    {
      initMovers();
    }

    return ok;
  }

  @Override
  public void shutdownHandler()
  {
    // Super
    super.shutdownHandler();

    // Mover
    if (fadeOutMover != null)
    {
      fadeOutMover.shutdown();
    }

    // Divers
    listener.onShutdown();
  }

  @Override
  public boolean handleTouchEvent(final MotionEvent event)
  {
    return detector.onTouchEvent(event);
  }

  /**
   * 
   * @param lock
   */
  public void lock(final boolean lock)
  {
    this.locked = lock;
  }
}
