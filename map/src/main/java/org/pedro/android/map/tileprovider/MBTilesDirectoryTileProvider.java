package org.pedro.android.map.tileprovider;

import java.io.File;

import org.pedro.map.tileprovider.StackCompositeTileProvider;

/**
 * 
 * @author pedro.m
 */
public class MBTilesDirectoryTileProvider extends StackCompositeTileProvider
{
  private static final String EXTENSION = ".mbtiles";

  /**
   * 
   * @param key
   */
  public MBTilesDirectoryTileProvider(final String key, final String dirname)
  {
    super(key);
    init(new File(dirname));
  }

  /**
   * 
   */
  private void init(final File dir)
  {
    final File[] files = dir.listFiles();
    if (files != null)
    {
      for (final File file : files)
      {
        // Fichier illisible
        if (!file.canRead())
        {
          continue;
        }

        // Repertoire
        if (file.isDirectory())
        {
          init(file);
          continue;
        }

        // Fichier mbtiles
        if (file.isFile() && file.getName().endsWith(EXTENSION))
        {
          // Fichier mbtiles
          final String fileKey = file.getName().substring(0, EXTENSION.length());
          final MBTilesTileProvider tileProvider = new MBTilesTileProvider(fileKey, file.getAbsolutePath());
          addTileProvider(tileProvider);
        }
      }
    }
  }
}
