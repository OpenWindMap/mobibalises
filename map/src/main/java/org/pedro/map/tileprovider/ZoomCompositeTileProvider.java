package org.pedro.map.tileprovider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.pedro.map.Tile;
import org.pedro.map.TileProvider;

/**
 * 
 * @author pedro.m
 */
public class ZoomCompositeTileProvider extends AbstractTileProvider
{
  private int                              minZoomLevel  = Integer.MAX_VALUE;
  private int                              maxZoomLevel  = 0;
  private final Map<Integer, TileProvider> tileProviders = new HashMap<Integer, TileProvider>();

  /**
   * 
   * @param key
   */
  public ZoomCompositeTileProvider(final String key)
  {
    super(key);
  }

  /**
   * 
   * @param beginZoom
   * @param endZoom
   * @param tileProvider
   */
  public void addTileProvider(final int beginZoom, final int endZoom, final TileProvider tileProvider)
  {
    // Reglage du zoom mini
    if (beginZoom < minZoomLevel)
    {
      minZoomLevel = beginZoom;
    }

    // Reglage du zoom maxi
    if (endZoom > maxZoomLevel)
    {
      maxZoomLevel = endZoom;
    }

    // Enregistrement du provider
    for (int zoom = beginZoom; zoom <= endZoom; zoom++)
    {
      tileProviders.put(Integer.valueOf(zoom), tileProvider);
    }
  }

  @Override
  public int getMinZoomLevel()
  {
    return minZoomLevel;
  }

  @Override
  public int getMaxZoomLevel()
  {
    return maxZoomLevel;
  }

  @Override
  public int readData(final Tile tile, final byte[] buffer) throws IOException
  {
    final TileProvider tileProvider = tileProviders.get(Integer.valueOf(tile.zoom));
    if (tileProvider != null)
    {
      return tileProvider.readData(tile, buffer);
    }

    return 0;
  }

  @Override
  public boolean needsCache(final Tile tile)
  {
    final TileProvider tileProvider = tileProviders.get(Integer.valueOf(tile.zoom));
    if (tileProvider != null)
    {
      return tileProvider.needsCache(tile);
    }

    return false;
  }

  @Override
  public boolean hasTile(final Tile tile) throws IOException
  {
    final TileProvider tileProvider = tileProviders.get(Integer.valueOf(tile.zoom));
    if (tileProvider != null)
    {
      return tileProvider.hasTile(tile);
    }

    return false;
  }

  @Override
  public String getCacheKey(final Tile tile)
  {
    final TileProvider tileProvider = tileProviders.get(Integer.valueOf(tile.zoom));
    if (tileProvider != null)
    {
      return tileProvider.getCacheKey(tile);
    }

    return null;
  }

  @Override
  public void shutdown()
  {
    for (final TileProvider tileProvider : tileProviders.values())
    {
      tileProvider.shutdown();
    }
  }
}
