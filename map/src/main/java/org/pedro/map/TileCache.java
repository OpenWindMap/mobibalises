package org.pedro.map;

import java.io.IOException;

/**
 * 
 * @author pedro.m
 */
public interface TileCache
{
  /**
   * 
   * @author pedro.m
   *
   */
  public interface CacheNotAvailableListener
  {
    /**
     * 
     */
    public void onCacheNotAvailable(String cachePath);
  }

  /**
   * 
   * @param providerKey
   * @param tile
   * @param buffer
   * @return
   * @throws IOException
   */
  public int retrieveTileFromCache(String providerKey, Tile tile, byte[] buffer) throws IOException;

  /**
   * 
   * @param providerKey
   * @param tile
   * @return
   */
  public boolean isTileAvailable(String providerKey, Tile tile);

  /**
   * 
   * @param providerKey
   * @param tile
   * @param buffer
   * @param length
   * @throws IOException
   */
  public void storeToCache(String providerKey, Tile tile, byte[] buffer, int length) throws IOException;

  /**
   * 
   */
  public void start();

  /**
   * 
   */
  public void shutdown();

  /**
   * 
   * @return
   */
  public boolean isAvailable();

  /**
   * 
   */
  public void refreshAvailability();

  /**
   * 
   * @return
   */
  public long getCurrentSize();

  /**
   * 
   * @param maximumSizeBytes
   */
  public void setMaximumSize(long maximumSizeBytes);
}
