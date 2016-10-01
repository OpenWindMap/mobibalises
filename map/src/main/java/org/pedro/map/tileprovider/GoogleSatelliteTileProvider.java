package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class GoogleSatelliteTileProvider extends GoogleTileProvider
{
  public static final String  KEY       = "google_satellite";
  private static final String SATELLITE = "satellite";

  /**
   * 
   * @param apiKey
   */
  public GoogleSatelliteTileProvider(final String apiKey)
  {
    super(KEY, apiKey);
  }

  @Override
  public int getMaxZoomLevel()
  {
    return 17;
  }

  @Override
  public String getMapType()
  {
    return SATELLITE;
  }
}
