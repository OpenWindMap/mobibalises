package org.pedro.map;

/**
 * 
 * @author pedro.m
 *
 * @param <Drawer>
 */
public interface MapDrawable<Drawer>
{
  /**
   * 
   * @param canvas
   * @param point
   */
  public void draw(Drawer canvas, Point point);

  /**
   * 
   * @return
   */
  public Rect getDisplayBounds();

  /**
   * 
   * @return
   */
  public Rect getInteractiveBounds();
}
