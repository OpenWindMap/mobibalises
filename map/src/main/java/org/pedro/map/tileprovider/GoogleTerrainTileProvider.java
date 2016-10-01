package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class GoogleTerrainTileProvider extends GoogleTileProvider
{
  public static final String  KEY     = "google_terrain";
  private static final String TERRAIN = "terrain";

  /**
   * 
   * @param apiKey
   */
  public GoogleTerrainTileProvider(final String apiKey)
  {
    super(KEY, apiKey);
  }

  @Override
  public int getMaxZoomLevel()
  {
    return 15;
  }

  @Override
  public String getMapType()
  {
    return TERRAIN;
  }
}
