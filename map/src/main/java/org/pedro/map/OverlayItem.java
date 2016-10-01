package org.pedro.map;

/**
 * 
 * @author pedro.m
 *
 * @param <Drawer>
 */
public class OverlayItem<Drawer>
{
  private final String              name;
  public GeoPoint                   point;
  private final MapDrawable<Drawer> drawable;
  public long                       x = -1;
  public long                       y = -1;
  public boolean                    visible;

  private int                       hashCode;

  /**
   * 
   * @param name
   * @param point
   * @param drawable
   */
  public OverlayItem(final String name, final GeoPoint point, final MapDrawable<Drawer> drawable)
  {
    this.name = name;
    this.point = point;
    this.drawable = drawable;

    calculateHashCode();
  }

  /**
   * 
   * @param b
   * @return
   */
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

    if (!OverlayItem.class.isAssignableFrom(object.getClass()))
    {
      return false;
    }

    final OverlayItem<?> item = (OverlayItem<?>)object;

    return name.equals(item.name);
  }

  /**
   * 
   */
  private void calculateHashCode()
  {
    hashCode = name.hashCode();
  }

  @Override
  public int hashCode()
  {
    return hashCode;
  }

  /**
   * @return the drawable
   */
  public MapDrawable<Drawer> getDrawable()
  {
    return drawable;
  }

  /**
   * @return the name
   */
  public final String getName()
  {
    return name;
  }

  /**
   * @return the point
   */
  public final GeoPoint getPoint()
  {
    return point;
  }

  /**
   * @param point the point to set
   */
  public final void setPoint(final GeoPoint point)
  {
    this.point = point;
    x = -1;
    y = -1;
  }

  /**
   * 
   */
  public void recycle()
  {
    //Nothing
  }
}
