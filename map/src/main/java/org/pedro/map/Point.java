package org.pedro.map;

/**
 * 
 * @author pedro.m
 */
public class Point
{
  public int x;
  public int y;
  public int hashCode;

  /**
   * 
   */
  public Point()
  {
    // Nothing
  }

  /**
   * 
   * @param x
   * @param y
   */
  public Point(final int x, final int y)
  {
    this.x = x;
    this.y = y;
    calculateHashCode();
  }

  /**
   * 
   * @param point
   */
  public Point(final Point point)
  {
    this.x = point.x;
    this.y = point.y;
    this.hashCode = point.hashCode;
  }

  @Override
  public boolean equals(final Object object)
  {
    if (this == object)
    {
      return true;
    }

    if (object == null)
    {
      return false;
    }

    if (!Point.class.isAssignableFrom(object.getClass()))
    {
      return false;
    }

    final Point point = (Point)object;
    return (x == point.x) && (y == point.y);
  }

  @Override
  public int hashCode()
  {
    return hashCode;
  }

  /**
   * 
   * @param x
   * @param y
   * @return
   */
  @SuppressWarnings("hiding")
  public boolean equals(final int x, final int y)
  {
    return (this.x == x) && (this.y == y);
  }

  /**
   * 
   */
  private void calculateHashCode()
  {
    hashCode = 217 + y;
    hashCode = hashCode * 31 + x;
  }

  /**
   * 
   * @param another
   */
  public void copy(final Point another)
  {
    this.x = another.x;
    this.y = another.y;
    this.hashCode = another.hashCode;
  }

  @Override
  public String toString()
  {
    return x + "/" + y;
  }

  /**
   * 
   */
  public void negate()
  {
    x = -x;
    y = -y;
    calculateHashCode();
  }

  /**
   * 
   * @param dx
   * @param dy
   */
  public void offset(final int dx, final int dy)
  {
    x += dx;
    y += dy;
    calculateHashCode();
  }

  /**
   * 
   * @param x
   * @param y
   */
  @SuppressWarnings("hiding")
  public void move(final int x, final int y)
  {
    set(x, y);
  }

  /**
   * 
   * @param dx
   * @param dy
   */
  public void translate(final int dx, final int dy)
  {
    offset(dx, dy);
  }

  /**
   * 
   * @param x
   * @param y
   */
  public void set(final int x, final int y)
  {
    this.x = x;
    this.y = y;
    calculateHashCode();
  }

  /**
   * @param x the x to set
   */
  public void setX(final int x)
  {
    this.x = x;
    calculateHashCode();
  }

  /**
   * @param y the y to set
   */
  public void setY(final int y)
  {
    this.y = y;
    calculateHashCode();
  }
}
