package org.pedro.map.tileprovider;

import java.io.IOException;

import org.pedro.map.Tile;

/**
 * 
 * @author pedro.m
 */
public abstract class NetworkTileProvider extends AbstractURLTileProvider
{
  /**
   * 
   * @param key
   */
  public NetworkTileProvider(final String key)
  {
    super(key);
  }

  @Override
  public boolean needsCache(final Tile tile)
  {
    return true;
  }

  @Override
  public boolean hasTile(final Tile tile) throws IOException
  {
    return true;
  }
}
