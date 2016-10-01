package org.pedro.map;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pedro.utils.FileTimestampUtils;
import org.pedro.utils.IOUtils;
import org.pedro.utils.ThreadUtils;

/**
 * 
 * @author pedro.m
 */
public final class TileFileCache extends Thread implements TileCache
{
  private static final char                       TIRET            = '-';
  private static final char                       POINT            = '.';
  private static final String                     EXTENSION        = "png";
  private static final Pattern                    FILE_PATTERN     = Pattern.compile("(\\w+)-(\\d+)_(\\d+)_(\\d+)\\.\\w+");

  private final File                              directory;
  private long                                    maximumSize      = -1;
  private final Object                            maximumSizeLock  = new Object();

  private boolean                                 available;

  private final SimpleFileFilter                  simpleFileFilter = new SimpleFileFilter();

  private final Map<String, Map<Tile, FileEntry>> currentFiles     = new HashMap<String, Map<Tile, FileEntry>>();
  private long                                    currentSize;
  private final Object                            cacheLock        = new Object();

  private final Key                               oldestKey        = new Key();

  /**
   * 
   * @author pedro.m
   */
  private static class FileEntry
  {
    final File file;
    long       timestamp;

    /**
     * 
     * @param file
     * @param timestamp
     */
    FileEntry(final File file, final long timestamp)
    {
      this.file = file;
      this.timestamp = timestamp;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class Key
  {
    String providerKey;
    Tile   tile;

    /**
     * 
     */
    Key()
    {
      super();
    }
  }

  /**
   * 
   * @param directory
   * @param maximumSizeMo
   */
  public TileFileCache(final String directory)
  {
    super(TileFileCache.class.getName());
    this.directory = new File(directory);

    // Creation du repertoire si besoin
    if (!this.directory.exists())
    {
      this.directory.mkdirs();
    }

    // Test de la disponibilite
    refreshAvailability();
  }

  /**
   * 
   * @author pedro.m
   */
  private static class SimpleFileFilter implements FileFilter
  {
    /**
     * 
     */
    SimpleFileFilter()
    {
      super();
    }

    @Override
    public boolean accept(final File file)
    {
      return file.isFile() && !file.isDirectory() && file.canWrite() && file.canRead();
    }
  }

  @Override
  public void run()
  {
    // Boucle d'attente
    while (!isInterrupted())
    {
      synchronized (this)
      {
        try
        {
          wait();
        }
        catch (final InterruptedException ie)
        {
          Thread.currentThread().interrupt();
          return;
        }
      }

      // Nettoyage du cache
      if (!isInterrupted())
      {
        doClean();
      }
    }
  }

  /**
   * 
   */
  private void doClean()
  {
    // Taille inferieure a zero = pas de limite
    synchronized (maximumSizeLock)
    {
      if (maximumSize < 0)
      {
        return;
      }
    }

    synchronized (cacheLock)
    {
      // Suppression des derniers fichiers
      while ((currentSize > maximumSize) && (currentFiles.size() > 0))
      {
        // Determination de la plus vieille entree
        computeOldestKey();

        // Initialisations
        final File file = get(oldestKey.providerKey, oldestKey.tile).file;
        final long fileLength = file.length();

        // Suppression physique du fichier
        synchronized (file)
        {
          FileTimestampUtils.deleteTimestampFile(file);
          file.delete();
        }

        // Retrait de la liste
        remove(oldestKey.providerKey, oldestKey.tile);
        currentSize -= fileLength;
      }
    }
  }

  /**
   * 
   * @return
   */
  private void computeOldestKey()
  {
    // Initialisations
    long oldestTimestamp = Long.MAX_VALUE;

    // Parcours des entrees
    for (final Map.Entry<String, Map<Tile, FileEntry>> tileEntries : currentFiles.entrySet())
    {
      for (final Map.Entry<Tile, FileEntry> tileEntry : tileEntries.getValue().entrySet())
      {
        if (tileEntry.getValue().timestamp < oldestTimestamp)
        {
          oldestTimestamp = tileEntry.getValue().timestamp;
          oldestKey.providerKey = tileEntries.getKey();
          oldestKey.tile = tileEntry.getKey();
        }
      }
    }
  }

  /**
   * 
   */
  private void initCurrentFiles()
  {
    // Calcul de la taille et mise en List
    synchronized (cacheLock)
    {
      // Initialisations
      currentSize = 0;
      clearCurrentFiles();
      final File[] files = directory.listFiles(simpleFileFilter);

      // Pour chaque fichier
      for (final File file : files)
      {
        final Matcher matcher = FILE_PATTERN.matcher(file.getName());
        if (matcher.matches())
        {
          // Initialisations
          final String providerKey = matcher.group(1);
          final Tile tile = new Tile(Long.parseLong(matcher.group(3), 10), Long.parseLong(matcher.group(4), 10), Integer.parseInt(matcher.group(2), 10));

          // Enregistrement
          put(providerKey, tile, file, FileTimestampUtils.lastModified(file));
          currentSize += file.length();
        }
      }
    }
  }

  /**
   * 
   */
  private void clean()
  {
    synchronized (maximumSizeLock)
    {
      if (maximumSize >= 0)
      {
        synchronized (this)
        {
          notify();
        }
      }
    }
  }

  /**
   * 
   */
  private void checkAvailable()
  {
    if (!available)
    {
      throw new RuntimeException("Cache not available !");
    }
  }

  /**
   * 
   */
  private void clearCurrentFiles()
  {
    for (final Map<Tile, FileEntry> providerMaps : currentFiles.values())
    {
      providerMaps.clear();
    }
    currentFiles.clear();
  }

  @Override
  public void shutdown()
  {
    // MAJ de la date d'acces de fichier
    synchronized (cacheLock)
    {
      for (final Map<Tile, FileEntry> fileEntries : currentFiles.values())
      {
        for (final FileEntry entry : fileEntries.values())
        {
          FileTimestampUtils.setLastModified(entry.file, entry.timestamp);
        }
      }
    }

    // Fin du Thread
    interrupt();
    ThreadUtils.join(this);

    // Vidage de la Map
    clearCurrentFiles();
  }

  @Override
  public int retrieveTileFromCache(final String providerKey, final Tile tile, final byte[] buffer) throws IOException
  {
    // Initialisations
    FileEntry entry;

    synchronized (cacheLock)
    {
      // Verification prealable
      if (!available)
      {
        return -1;
      }

      // Initialisations
      entry = get(providerKey, tile);

      // Si inexistant
      if (entry == null)
      {
        return -1;
      }

      // MAJ de la date d'access
      entry.timestamp = System.currentTimeMillis();
    }

    // Lecture
    synchronized (entry.file)
    {
      return IOUtils.readFile(entry.file, buffer);
    }
  }

  @Override
  public boolean isTileAvailable(final String providerKey, final Tile tile)
  {
    // Verification prealable
    if (!available)
    {
      return false;
    }

    // Recherche
    synchronized (cacheLock)
    {
      return containsKey(providerKey, tile);
    }
  }

  @Override
  public void refreshAvailability()
  {
    synchronized (cacheLock)
    {
      // Initialisations
      final boolean oldAvailable = available;
      available = directory.exists() && directory.isDirectory() && directory.canWrite();

      // Initialialisation
      if (!oldAvailable && available)
      {
        initCurrentFiles();
      }
    }
  }

  @Override
  public void storeToCache(final String providerKey, final Tile tile, final byte[] buffer, final int length) throws IOException
  {
    // Verification prealable
    checkAvailable();

    // Taille a zero : on n'ecrit pas
    synchronized (maximumSizeLock)
    {
      if (maximumSize == 0)
      {
        return;
      }
    }

    // Ecriture
    final File file = getFile(providerKey, tile);
    synchronized (file)
    {
      IOUtils.writeFile(file, buffer, length);
    }

    // MAJ compteur
    synchronized (cacheLock)
    {
      put(providerKey, tile, file, System.currentTimeMillis());
      currentSize += file.length();
    }

    // Purge
    clean();
  }

  /**
   * 
   * @param providerKey
   * @param tile
   * @param file
   * @param timestamp
   */
  private void put(final String providerKey, final Tile tile, final File file, final long timestamp)
  {
    // Initialisations
    Map<Tile, FileEntry> providerFiles = currentFiles.get(providerKey);

    // Provider inconnu
    if (providerFiles == null)
    {
      providerFiles = new HashMap<Tile, FileEntry>();
      currentFiles.put(providerKey, providerFiles);
    }

    // Creation d'une tuile locale (car celle passee en parametre provient d'un pool
    // et sera reutilisee et modifiee en parallele par les autres Threads)
    final Tile localTile = new Tile(tile);

    // Enregistrement
    providerFiles.put(localTile, new FileEntry(file, timestamp));
  }

  /**
   * 
   * @param providerKey
   * @param tile
   */
  private void remove(final String providerKey, final Tile tile)
  {
    // Initialisations
    final Map<Tile, FileEntry> providerFiles = currentFiles.get(providerKey);

    // Provider inconnu
    if (providerFiles == null)
    {
      return;
    }

    // Effacement
    providerFiles.remove(tile);
  }

  /**
   * 
   * @param providerKey
   * @param tile
   * @return
   */
  private FileEntry get(final String providerKey, final Tile tile)
  {
    // Entrees du provider
    final Map<Tile, FileEntry> fileEntries = currentFiles.get(providerKey);

    // Si absent
    if (fileEntries == null)
    {
      return null;
    }

    return fileEntries.get(tile);
  }

  /**
   * 
   * @param providerKey
   * @param tile
   * @return
   */
  private boolean containsKey(final String providerKey, final Tile tile)
  {
    // Entrees du provider
    final Map<Tile, FileEntry> fileEntries = currentFiles.get(providerKey);

    // Si absent
    if (fileEntries == null)
    {
      return false;
    }

    return fileEntries.containsKey(tile);
  }

  /**
   * 
   * @param providerKey
   * @param tile
   * @return
   */
  private static String getKey(final String providerKey, final Tile tile)
  {
    final StringBuilder builder = new StringBuilder(64);
    builder.append(providerKey);
    builder.append(TIRET);
    builder.append(tile.getKey());
    builder.append(POINT);
    builder.append(EXTENSION);

    return builder.toString();
  }

  /**
   * 
   * @param providerKey
   * @param tile
   * @return
   */
  private File getFile(final String providerKey, final Tile tile)
  {
    return new File(directory, getKey(providerKey, tile));
  }

  @Override
  public boolean isAvailable()
  {
    return available;
  }

  @Override
  public long getCurrentSize()
  {
    return currentSize;
  }

  @Override
  public void setMaximumSize(final long maximumSizeBytes)
  {
    synchronized (maximumSizeLock)
    {
      if (maximumSize != maximumSizeBytes)
      {
        maximumSize = maximumSizeBytes;
        clean();
      }
    }
  }
}
