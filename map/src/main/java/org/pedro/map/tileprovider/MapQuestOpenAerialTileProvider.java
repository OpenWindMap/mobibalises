package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class MapQuestOpenAerialTileProvider extends MapQuestTileProvider
{
  public static final String KEY = "mapquestaerial";

  /**
   * 
   */
  public MapQuestOpenAerialTileProvider()
  {
    super(KEY, "sat");
  }

  @Override
  public int getMaxZoomLevel()
  {
    return 11;
  }
}
