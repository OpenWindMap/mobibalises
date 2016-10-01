package org.pedro.android.map;

import org.pedro.map.MapController;
import org.pedro.map.Projection;

import android.content.Context;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractTouchHandler implements TouchHandler
{
  protected MapView       mapView;
  protected MapController controller;
  protected Projection    projection;

  /**
   * 
   */
  protected AbstractTouchHandler()
  {
    super();
  }

  @Override
  public boolean initHandler(final Context context, final MapView inMapView)
  {
    this.mapView = inMapView;
    this.controller = inMapView.getController();
    this.projection = inMapView.getProjection();

    return true;
  }

  @Override
  public void shutdownHandler()
  {
    // Divers
    mapView = null;
  }
}
