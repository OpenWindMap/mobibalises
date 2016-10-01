package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class MapQuestOSMTileProvider extends MapQuestTileProvider
{
  public static final String KEY = "mapquestmap";

  /**
   * 
   */
  public MapQuestOSMTileProvider()
  {
    super(KEY, "map");
  }

  @Override
  public int getMaxZoomLevel()
  {
    return 19;
  }
}
