package org.pedro.map.tileprovider;

/**
 * 
 * @author pedro.m
 */
public final class HikingEuropeTileProvider extends MRITileProvider
{
  public static final String  KEY   = "hiking_europe";

  private static final String LAYER = "hiking";

  /**
   * 
   */
  public HikingEuropeTileProvider()
  {
    super(KEY);
  }

  @Override
  protected String getLayer()
  {
    return LAYER;
  }
}
