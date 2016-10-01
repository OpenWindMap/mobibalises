package org.pedro.android.map;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;

/**
 * 
 * @author pedro.m
 */
public class MultiTouchHandler extends AbstractTouchHandler
{
  private static final int          MULTITOUCH_DELAY = 250;

  float                             focusX;
  float                             focusY;
  ScaleGestureDetector              detector;
  private MultiTouchGestureListener listener;
  boolean                           handled;
  private long                      lastMultiTouchTimestamp;

  /**
   * 
   * @author pedro.m
   */
  @TargetApi(Build.VERSION_CODES.FROYO)
  private static class MultiTouchGestureListener implements OnScaleGestureListener
  {
    private MultiTouchHandler multiTouchHandler;
    private float             scaleFactor;
    private float             finalScaleFactor;

    /**
     * 
     * @param multiTouchHandler
     */
    MultiTouchGestureListener(final MultiTouchHandler multiTouchHandler)
    {
      super();
      this.multiTouchHandler = multiTouchHandler;
    }

    @Override
    public boolean onScale(final ScaleGestureDetector inDetector)
    {
      multiTouchHandler.handled = true;

      scaleFactor = multiTouchHandler.detector.getScaleFactor();
      finalScaleFactor *= scaleFactor;

      multiTouchHandler.controller.postIntermediateZoom(true, finalScaleFactor, (int)multiTouchHandler.focusX, (int)multiTouchHandler.focusY);

      return true;
    }

    @Override
    public boolean onScaleBegin(final ScaleGestureDetector inDetector)
    {
      multiTouchHandler.handled = true;
      scaleFactor = 1;
      finalScaleFactor = scaleFactor;
      multiTouchHandler.focusX = inDetector.getFocusX();
      multiTouchHandler.focusY = inDetector.getFocusY();

      return true;
    }

    @Override
    public void onScaleEnd(final ScaleGestureDetector inDetector)
    {
      multiTouchHandler.handled = true;
      multiTouchHandler.controller.postIntermediateZoom(false, finalScaleFactor, 0, 0);
      final double zoom = Math.log(finalScaleFactor) / Math.log(2);
      final int deltaZoom = (int)Math.round(zoom);
      multiTouchHandler.controller.postZoom(deltaZoom, (int)multiTouchHandler.focusX, (int)multiTouchHandler.focusY);
      multiTouchHandler.controller.onMoveInputFinished();
    }

    /**
     * 
     */
    void onShutdown()
    {
      // Divers
      multiTouchHandler = null;
    }
  }

  /**
   * 
   */
  public MultiTouchHandler()
  {
    super();
  }

  @TargetApi(Build.VERSION_CODES.FROYO)
  @Override
  public boolean initHandler(final Context context, final MapView inMapView)
  {
    boolean ok = super.initHandler(context, inMapView);

    if (ok)
    {
      // Il faut que le systeme en soit capable
      final PackageManager packageManager = mapView.getContext().getPackageManager();
      ok = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH);

      // Si capable, instanciation du detecteur
      if (ok)
      {
        listener = new MultiTouchGestureListener(this);
        detector = new ScaleGestureDetector(context, listener);
      }
    }

    return ok;
  }

  @TargetApi(Build.VERSION_CODES.FROYO)
  @Override
  public boolean handleTouchEvent(final MotionEvent event)
  {
    // Initialisations
    handled = false;

    // Gestion
    detector.onTouchEvent(event);

    // Marquage du dernier instant
    if (handled)
    {
      lastMultiTouchTimestamp = System.currentTimeMillis();
    }

    // Tempo de 250 ms apres le dernier multitouch
    return handled || (System.currentTimeMillis() - lastMultiTouchTimestamp < MULTITOUCH_DELAY);
  }

  @Override
  public void shutdownHandler()
  {
    // Super
    super.shutdownHandler();

    // Divers
    if (listener != null)
    {
      listener.onShutdown();
    }
  }
}
