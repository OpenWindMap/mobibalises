package org.pedro.map;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractMapController implements MapController
{
  private final List<MapControllerListener> listeners = new ArrayList<MapControllerListener>();

  @Override
  public final void addMapControllerListener(final MapControllerListener listener)
  {
    listeners.add(listener);
  }

  @Override
  public final void removeMapControllerListener(final MapControllerListener listener)
  {
    listeners.remove(listener);
  }

  /**
   * 
   * @param oldZoom
   * @param newZoom
   */
  protected final void fireZoomChanged(final int oldZoom, final int newZoom)
  {
    for (final MapControllerListener listener : listeners)
    {
      listener.onZoomChanged(oldZoom, newZoom);
    }
  }

  /**
   * 
   * @param oldCenter
   * @param newCenter
   */
  protected final void fireCenterChanged(final GeoPoint oldCenter, final GeoPoint newCenter)
  {
    for (final MapControllerListener listener : listeners)
    {
      listener.onCenterChanged(oldCenter, newCenter);
    }
  }
}
