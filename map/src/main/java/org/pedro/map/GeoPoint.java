package org.pedro.map;

/**
 * 
 * @author pedro.m
 */
public final class GeoPoint
{
  private static final double LATITUDE_MAX     = 85.05113;
  private static final double LATITUDE_MIN     = -85.05113;
  private static final double LONGITUDE_MAX    = 180;
  private static final double LONGITUDE_MIN    = -180;

  private static final int    LATITUDE_MAX_E6  = toInt(LATITUDE_MAX);
  private static final int    LATITUDE_MIN_E6  = toInt(LATITUDE_MIN);
  private static final int    LONGITUDE_MAX_E6 = toInt(LONGITUDE_MAX);
  private static final int    LONGITUDE_MIN_E6 = toInt(LONGITUDE_MIN);

  private static final int    E6               = 1000000;

  private double              longitude;
  private double              latitude;
  private int                 longitudeE6;
  private int                 latitudeE6;
  private int                 hashCode;

  /**
   * 
   * @param value
   * @return
   */
  private static int toInt(final double value)
  {
    return (int)Math.round(value * E6);
  }

  /**
   * 
   * @param value
   * @return
   */
  private static double toDouble(final int value)
  {
    return ((double)value) / E6;
  }

  /**
   * 
   */
  public GeoPoint()
  {
    calculateHashCode();
  }

  /**
   * 
   * @param latitude
   * @param longitude
   */
  public GeoPoint(final double latitude, final double longitude)
  {
    set(latitude, longitude);
  }

  /**
   * 
   * @param latitudeE6
   * @param longitudeE6
   */
  public GeoPoint(final int latitudeE6, final int longitudeE6)
  {
    set(latitudeE6, longitudeE6);
  }

  @Override
  public String toString()
  {
    return latitude + "N - " + longitude + "E";
  }

  /**
   * 
   * @param latitude
   * @param longitude
   */
  public void set(final double latitude, final double longitude)
  {
    this.longitude = clipLongitude(longitude);
    this.latitude = clipLatitude(latitude);
    this.longitudeE6 = toInt(this.longitude);
    this.latitudeE6 = toInt(this.latitude);
    calculateHashCode();
  }

  /**
   * 
   * @param latitudeE6
   * @param longitudeE6
   */
  public void set(final int latitudeE6, final int longitudeE6)
  {
    this.longitudeE6 = clipLongitude(longitudeE6);
    this.latitudeE6 = clipLatitude(latitudeE6);
    this.longitude = toDouble(this.longitudeE6);
    this.latitude = toDouble(this.latitudeE6);
    calculateHashCode();
  }

  /**
   * 
   * @param another
   */
  public void copy(final GeoPoint another)
  {
    longitude = another.longitude;
    latitude = another.latitude;
    longitudeE6 = another.longitudeE6;
    latitudeE6 = another.latitudeE6;
    hashCode = another.hashCode;
  }

  /**
   * 
   * @param longitude
   * @return
   */
  private static int clipLongitude(final int longitude)
  {
    return Math.min(Math.max(longitude, LONGITUDE_MIN_E6), LONGITUDE_MAX_E6);
  }

  /**
   * 
   * @param latitude
   * @return
   */
  private static int clipLatitude(final int latitude)
  {
    return Math.min(Math.max(latitude, LATITUDE_MIN_E6), LATITUDE_MAX_E6);
  }

  /**
   * 
   * @param longitude
   * @return
   */
  private static double clipLongitude(final double longitude)
  {
    return Math.min(Math.max(longitude, LONGITUDE_MIN), LONGITUDE_MAX);
  }

  /**
   * 
   * @param latitude
   * @return
   */
  private static double clipLatitude(final double latitude)
  {
    return Math.min(Math.max(latitude, LATITUDE_MIN), LATITUDE_MAX);
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

    if (!GeoPoint.class.isAssignableFrom(object.getClass()))
    {
      return false;
    }

    final GeoPoint point = (GeoPoint)object;
    return (latitudeE6 == point.latitudeE6) && (longitudeE6 == point.longitudeE6);
  }

  /**
   * 
   */
  private void calculateHashCode()
  {
    hashCode = 217 + latitudeE6;
    hashCode = hashCode * 31 + longitudeE6;
  }

  @Override
  public int hashCode()
  {
    return hashCode;
  }

  /**
   * @return the longitude
   */
  public double getLongitude()
  {
    return longitude;
  }

  /**
   * @return the latitude
   */
  public double getLatitude()
  {
    return latitude;
  }

  /**
   * @return the longitudeE6
   */
  public int getLongitudeE6()
  {
    return longitudeE6;
  }

  /**
   * @return the latitudeE6
   */
  public int getLatitudeE6()
  {
    return latitudeE6;
  }
}
