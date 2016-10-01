package org.pedro.map.tileprovider;

import org.pedro.map.Tile;
import org.pedro.map.TileProvider;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractTileProvider implements TileProvider
{
  private final String key;

  /**
   * 
   * @param key
   * @param dataProvider
   * @param graphicsHelper
   */
  public AbstractTileProvider(final String key)
  {
    this.key = key;
  }

  @Override
  public final String getKey()
  {
    return key;
  }

  @Override
  public String getCacheKey(final Tile tile)
  {
    return key;
  }
}
