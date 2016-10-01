package org.pedro.map.tileprovider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.pedro.map.Tile;
import org.pedro.map.TileProvider;

/**
 * 
 * @author pedro.m
 */
public class StackCompositeTileProvider extends AbstractTileProvider
{
  private int                      minZoomLevel  = Integer.MAX_VALUE;
  private int                      maxZoomLevel  = 0;
  private final List<TileProvider> tileProviders = new ArrayList<TileProvider>();

  /**
   * 
   * @param key
   */
  public StackCompositeTileProvider(final String key)
  {
    super(key);
  }

  /**
   * 
   * @param beginZoom
   * @param endZoom
   * @param tileProvider
   */
  public void addTileProvider(final TileProvider tileProvider)
  {
    // Reglage du zoom mini
    if (tileProvider.getMinZoomLevel() < minZoomLevel)
    {
      minZoomLevel = tileProvider.getMinZoomLevel();
    }

    // Reglage du zoom maxi
    if (tileProvider.getMaxZoomLevel() > maxZoomLevel)
    {
      maxZoomLevel = tileProvider.getMaxZoomLevel();
    }

    // Enregistrement du provider
    tileProviders.add(tileProvider);
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
    for (final TileProvider tileProvider : tileProviders)
    {
      final boolean hasTile = tileProvider.hasTile(tile);
      if (hasTile)
      {
        return tileProvider.readData(tile, buffer);
      }
    }

    return 0;
  }

  @Override
  public boolean needsCache(final Tile tile)
  {
    for (final TileProvider tileProvider : tileProviders)
    {
      try
      {
        final boolean hasTile = tileProvider.hasTile(tile);
        if (hasTile)
        {
          return tileProvider.needsCache(tile);
        }
      }
      catch (final IOException ioe)
      {
        // Nothing
      }
    }

    return false;
  }

  @Override
  public boolean hasTile(final Tile tile) throws IOException
  {
    for (final TileProvider tileProvider : tileProviders)
    {
      final boolean hasTile = tileProvider.hasTile(tile);
      if (hasTile)
      {
        return true;
      }
    }

    return false;
  }

  @Override
  public String getCacheKey(final Tile tile)
  {
    for (final TileProvider tileProvider : tileProviders)
    {
      try
      {
        final boolean hasTile = tileProvider.hasTile(tile);
        if (hasTile)
        {
          return tileProvider.getCacheKey(tile);
        }
      }
      catch (final IOException ioe)
      {
        // Nothing
      }
    }

    return null;
  }

  @Override
  public void shutdown()
  {
    for (final TileProvider tileProvider : tileProviders)
    {
      tileProvider.shutdown();
    }
  }
}
