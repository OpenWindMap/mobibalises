package org.pedro.map;

import java.io.IOException;

/**
 * 
 * @author pedro.m
 */
public interface TileProvider
{
  public static final int TILE_SIZE = 256;

  /**
   * 
   * @return
   */
  public String getKey();

  /**
   * 
   * @return
   */
  public int getMinZoomLevel();

  /**
   * 
   * @return
   */
  public int getMaxZoomLevel();

  /**
   * 
   * @param tile
   * @param buffer
   * @return
   * @throws IOException
   */
  public int readData(final Tile tile, final byte[] buffer) throws IOException;

  /**
   * 
   * @param tile
   * @return
   * @throws IOException
   */
  public boolean hasTile(final Tile tile) throws IOException;

  /**
   * 
   * @param tile
   * @return
   */
  public boolean needsCache(final Tile tile);

  /**
   * 
   * @param tile
   * @return
   */
  public String getCacheKey(final Tile tile);

  /**
   * 
   */
  public void shutdown();
}
