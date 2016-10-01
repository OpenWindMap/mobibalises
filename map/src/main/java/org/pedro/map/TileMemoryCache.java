package org.pedro.map;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author pedro.m
 */
public final class TileMemoryCache<ImageData>
{
  private static final float            TILE_MEMORY_CACHE_LOAD_FACTOR = 0.6f;

  final GraphicsHelper<ImageData, ?, ?> graphicsHelper;
  int                                   capacity;
  private Map<Tile, ImageData>          map;
  private final Object                  mapLock                       = new Object();
  final LinkedList<Tile>                tilesPool                     = new LinkedList<Tile>();

  /**
   * 
   * @param graphicsHelper
   * @param capacity
   */
  public TileMemoryCache(final GraphicsHelper<ImageData, ?, ?> graphicsHelper, final int capacity)
  {
    this.graphicsHelper = graphicsHelper;
    this.capacity = capacity;

    map = createMap(capacity);
  }

  /**
   * 
   * @param size
   * @return
   */
  protected Map<Tile, ImageData> createMap(final int size)
  {
    final Map<Tile, ImageData> retour = new LinkedHashMap<Tile, ImageData>((int)(size / TILE_MEMORY_CACHE_LOAD_FACTOR), TILE_MEMORY_CACHE_LOAD_FACTOR, true)
    {
      private static final long serialVersionUID = 2852968562266451487L;

      @Override
      protected boolean removeEldestEntry(final Map.Entry<Tile, ImageData> eldest)
      {
        if (size() > capacity)
        {
          final Tile tile = eldest.getKey();
          graphicsHelper.freeImageData(eldest.getValue());
          tilesPool.addFirst(tile);
          remove(tile);
        }

        return false;
      }
    };

    return retour;
  }

  /**
   * 
   */
  public void shutdown()
  {
    synchronized (mapLock)
    {
      if (map != null)
      {
        for (final ImageData data : map.values())
        {
          graphicsHelper.freeImageData(data);
        }
        map.clear();
        map = null;
      }

      tilesPool.clear();
    }
  }

  /**
   * 
   * @param tile
   * @return
   */
  public ImageData get(final Tile tile)
  {
    synchronized (mapLock)
    {
      return (map == null ? null : map.get(tile));
    }
  }

  /**
   * 
   * @param tile
   * @param data
   */
  public void put(final Tile tile, final ImageData data)
  {
    synchronized (mapLock)
    {
      if ((capacity > 0) && (map != null))
      {
        // Tuile deja en cache
        if (map.containsKey(tile))
        {
          return;
        }

        final Tile localTile = getTile();
        localTile.copy(tile);

        map.put(localTile, data);
      }
    }
  }

  /**
   * 
   * @return
   */
  private Tile getTile()
  {
    if (tilesPool.size() > 0)
    {
      return tilesPool.removeFirst();
    }

    return new Tile();
  }

  /**
   * 
   * @param newCapacity
   */
  public void adjust(final int newCapacity)
  {
    // Capacite identique : retour
    if (capacity == newCapacity)
    {
      return;
    }

    // Creation du nouveau cache
    final Map<Tile, ImageData> newMap = createMap(newCapacity);

    synchronized (mapLock)
    {
      // Liste des tuiles a reutiliser
      final List<Tile> tilesToReuse = new ArrayList<Tile>();
      final int maxTilesToReuse = Math.min(capacity, newCapacity);
      int i = 1;
      for (final Map.Entry<Tile, ImageData> entry : map.entrySet())
      {
        if (i <= maxTilesToReuse)
        {
          tilesToReuse.add(entry.getKey());
        }
        else
        {
          tilesPool.addFirst(entry.getKey());
          graphicsHelper.freeImageData(entry.getValue());
        }

        // Next
        i++;
      }

      // Copie des tuiles d'un cache a l'autre
      for (final Tile tile : tilesToReuse)
      {
        final ImageData data = map.remove(tile);
        newMap.put(tile, data);
      }

      // Sauvegarde
      map = newMap;
      capacity = newCapacity;
    }
  }

  /**
   * 
   * @param tile
   * @return
   */
  public boolean containsKey(final Tile tile)
  {
    synchronized (mapLock)
    {
      return map.containsKey(tile);
    }
  }

  /**
   * 
   */
  public void clear()
  {
    synchronized (mapLock)
    {
      for (final Map.Entry<Tile, ImageData> entry : map.entrySet())
      {
        graphicsHelper.freeImageData(entry.getValue());
        tilesPool.addFirst(entry.getKey());
      }
      map.clear();
    }
  }
}
