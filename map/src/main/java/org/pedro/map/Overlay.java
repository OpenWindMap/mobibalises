package org.pedro.map;

/**
 * 
 * @author pedro.m
 *
 * @param <ImageData>
 * @param <Drawer>
 */
public interface Overlay<ImageData, Drawer>
{
  /**
   * 
   * @param canvas
   */
  public void draw(Drawer canvas);

  /**
   * 
   * @param deltaX
   * @param deltaY
   * @param inCenter
   * @param inZoom
   */
  public void transformationPostTranslate(final int deltaX, final int deltaY, final GeoPoint inCenter, final int inZoom);

  /**
   * 
   * @param deltaZoom
   * @param inCenter
   * @param inZoom
   */
  public void transformationPostZoom(final int deltaZoom, final GeoPoint inCenter, final int inZoom);

  /**
   * 
   * @param deltaZoom
   * @param deltaX
   * @param deltaY
   * @param inCenter
   * @param inZoom
   */
  public void transformationPostIntermediateZoom(final float deltaZoom, final int deltaX, final int deltaY, final GeoPoint inCenter, final int inZoom);

  /**
   * 
   * @param inCenter
   * @param inZoom
   */
  public void transformationResetIntermediateZoom(final GeoPoint inCenter, final int inZoom);

  /**
   * 
   */
  public void requestRedraw();

  /**
   * 
   * @param inCenter
   * @param inZoom
   */
  public void requestRedraw(final GeoPoint inCenter, final int inZoom);

  /**
   * 
   * @return
   */
  public boolean needsLateDraw();

  /**
   * 
   */
  public void onMapDisplayerSizeChanged();

  /**
   * 
   */
  public void onShutdown();

  /**
   * 
   * @param geoPoint
   * @param point
   * @return
   */
  public boolean onTap(final GeoPoint geoPoint, final Point point);

  /**
   * 
   * @param geoPoint
   * @param point
   * @return
   */
  public boolean onLongTap(final GeoPoint geoPoint, final Point point);

  /**
   * 
   * @param geoPoint
   * @param point
   * @return
   */
  public boolean onDoubleTap(final GeoPoint geoPoint, final Point point);

  /**
   * 
   * @param keyCode
   * @return
   */
  public boolean onKeyPressed(final int keyCode);

  /**
   * 
   * @param center
   * @param topLeft
   * @param bottomRight
   * @param pixelLatAngle
   * @param pixelLngAngle
   */
  public void onMoveInputFinished(final GeoPoint center, final GeoPoint topLeft, final GeoPoint bottomRight, final double pixelLatAngle, final double pixelLngAngle);
}
