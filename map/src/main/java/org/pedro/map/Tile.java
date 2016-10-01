package org.pedro.map;

/**
 * 
 * @author pedro.m
 */
public final class Tile
{
  private static final char   TIRET     = '_';

  public long                 x;
  public long                 y;
  public int                  zoom;

  private String              key;
  private int                 hashCode;
  private final StringBuilder keyBuffer = new StringBuilder(32);

  /**
   * 
   */
  public Tile()
  {
    //Nothing
  }

  /**
   * 
   * @param x
   * @param y
   * @param zoom
   */
  public Tile(final long x, final long y, final int zoom)
  {
    this.x = x;
    this.y = y;
    this.zoom = zoom;

    resetKeyAndCalculateHashcode();
  }

  /**
   * 
   * @param another
   */
  public Tile(final Tile another)
  {
    this.copy(another);
  }

  /**
   * 
   * @param another
   */
  public void copy(final Tile another)
  {
    this.x = another.x;
    this.y = another.y;
    this.zoom = another.zoom;

    this.hashCode = another.hashCode;
    this.key = another.key;
  }

  @Override
  public String toString()
  {
    return x + "/" + y + "@" + zoom;
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

    if (!Tile.class.isAssignableFrom(object.getClass()))
    {
      return false;
    }

    final Tile tile = (Tile)object;
    return (zoom == tile.zoom) && (x == tile.x) && (y == tile.y);
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
   * @param zoom
   */
  public void set(final long x, final long y, final int zoom)
  {
    this.x = x;
    this.y = y;
    this.zoom = zoom;
    resetKeyAndCalculateHashcode();
  }

  /**
   * 
   */
  private void resetKeyAndCalculateHashcode()
  {
    key = null;
    hashCode = (int)(x ^ (x >>> 32));
    hashCode = 31 * hashCode + (int)(y ^ (y >>> 32));
    hashCode = 31 * hashCode + (zoom ^ (zoom >>> 32));
  }

  /**
   * 
   * @return
   */
  public String getKey()
  {
    if (key == null)
    {
      keyBuffer.setLength(0);
      keyBuffer.append(zoom);
      keyBuffer.append(TIRET);
      keyBuffer.append(x);
      keyBuffer.append(TIRET);
      keyBuffer.append(y);

      key = keyBuffer.toString();
    }

    return key;
  }

  /**
   * @param x the x to set
   */
  public void setX(final long x)
  {
    this.x = x;
    resetKeyAndCalculateHashcode();
  }

  /**
   * @param y the y to set
   */
  public void setY(final long y)
  {
    this.y = y;
    resetKeyAndCalculateHashcode();
  }

  /**
   * @param zoom the zoom to set
   */
  public void setZoom(final int zoom)
  {
    this.zoom = zoom;
    resetKeyAndCalculateHashcode();
  }
}
