package org.pedro.map;

/**
 * 
 * @author pedro.m
 */
public final class Rect
{
  public int left;
  public int top;
  public int right;
  public int bottom;

  /**
   * 
   */
  public Rect()
  {
    // Nothing
  }

  /**
   * 
   * @param left
   * @param top
   * @param right
   * @param bottom
   */
  public Rect(final int left, final int top, final int right, final int bottom)
  {
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
  }

  @Override
  public String toString()
  {
    return left + "/" + top + "|" + right + "/" + bottom;
  }

  /**
   * 
   * @param rect
   */
  public Rect(final Rect rect)
  {
    left = rect.left;
    top = rect.top;
    right = rect.right;
    bottom = rect.bottom;
  }

  /**
   * 
   * @return
   */
  public final int getWidth()
  {
    return right - left;
  }

  /**
   * 
   * @return
   */
  public final int getHeight()
  {
    return bottom - top;
  }

  /**
   * 
   * @param point
   * @return
   */
  public Point getCenter(final Point point)
  {
    final Point retour = (point == null ? new Point() : point);
    retour.set(Math.round(((float)(right - left)) / 2), Math.round(((float)(bottom - top)) / 2));

    return retour;
  }
}
