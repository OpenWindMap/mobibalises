package org.pedro.map;

/**
 * 
 * @author pedro.m
 */
public interface Projection
{
  /**
   * 
   * @param meters
   * @return
   */
  public float metersToPixels(final float meters);

  /**
   * 
   * @param meters
   * @param center
   * @param zoom
   * @return
   */
  public float metersToPixels(final float meters, final GeoPoint center, final int zoom);

  /**
   * 
   * @param x
   * @param y
   * @param point
   * @return
   */
  public GeoPoint fromPixels(final int x, final int y, final GeoPoint point);

  /**
   * 
   * @param geoPoint
   * @param point
   * @return
   */
  public Point toPixels(final GeoPoint geoPoint, final Point point);

  /**
   * 
   * @param latitude
   * @param longitude
   * @param point
   * @return
   */
  public Point toPixels(final double latitude, final double longitude, final Point point);
}
