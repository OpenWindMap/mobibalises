package org.pedro.map;

import java.util.concurrent.locks.Lock;

/**
 * 
 * @author pedro.m
 */
public interface MapController
{
  /**
   * 
   * @author pedro.m
   */
  public interface MapControllerListener
  {
    /**
     * 
     * @param oldZoom
     * @param newZoom
     */
    public void onZoomChanged(final int oldZoom, final int newZoom);

    /**
     * 
     * @param oldCenter
     * @param newCenter
     */
    public void onCenterChanged(final GeoPoint oldCenter, final GeoPoint newCenter);
  }

  /**
   * 
   * @param listener
   */
  public void addMapControllerListener(final MapControllerListener listener);

  /**
   * 
   * @param listener
   */
  public void removeMapControllerListener(final MapControllerListener listener);

  /**
   * 
   * @return
   */
  public GeoPoint getCenter();

  /**
   * 
   * @param center
   */
  public void setCenter(final GeoPoint center);

  /**
   * 
   * @param center
   */
  public void animateTo(final GeoPoint center);

  /**
   * 
   * @param deltaX
   * @param deltaY
   */
  public void scrollBy(final int deltaX, final int deltaY);

  /**
   * 
   * @return
   */
  public int getZoom();

  /**
   * 
   * @return
   */
  public int getMaxZoom();

  /**
   * 
   * @return
   */
  public int getMinZoom();

  /**
   * 
   * @param zoom
   */
  public void setZoom(final int zoom);

  /**
   * 
   */
  public void zoomIn();

  /**
   * 
   * @param center
   */
  public void zoomIn(final GeoPoint center);

  /**
   * 
   */
  public void zoomOut();

  /**
   * 
   * @param doZoom
   * @param zoom
   * @param deltaX
   * @param deltaY
   */
  public void postIntermediateZoom(final boolean doZoom, final float zoom, final int deltaX, final int deltaY);

  /**
   * 
   * @param deltaZoom
   * @param centerX
   * @param centerY
   */
  public void postZoom(final int deltaZoom, final int centerX, final int centerY);

  /**
   * 
   */
  public void stopMove();

  /**
   * 
   * @param mover
   */
  public void setMover(final MapMover mover);

  /**
   * 
   * @return
   */
  public Lock getReadLock();

  /**
   * 
   */
  public void onMoveInputFinished();
}
