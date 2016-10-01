package org.pedro.map;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.regex.Pattern;

import org.pedro.utils.FileTimestampUtils;
import org.pedro.utils.IOUtils;
import org.pedro.utils.ReadManyWriteSingleLock;
import org.pedro.utils.ThreadUtils;

/**
 * 
 * @author pedro.m
 */
public final class TileFileCacheLight extends Thread implements TileCache
{
  private static final char     TIRET           = '-';
  private static final char     POINT           = '.';
  private static final String   EXTENSION       = "png";

  private final File            directory;
  private long                  maximumSize     = -1;
  private long                  currentSize     = 0;
  private final ReadWriteLock   maximumSizeLock = new ReadManyWriteSingleLock();
  private final Object          cleanLock       = new Object();
  private final CacheFileFilter cacheFileFilter = new CacheFileFilter();

  private boolean               available;

  final List<Thread>            threads         = new ArrayList<Thread>();

  /**
   * 
   * @author pedro.m
   */
  private static class CacheFileFilter implements FileFilter
  {
    private static final Pattern FILE_PATTERN = Pattern.compile("(\\w+)-(\\d+)_(\\d+)_(\\d+)\\.\\w+");

    /**
     * 
     */
    CacheFileFilter()
    {
      super();
    }

    @Override
    public boolean accept(final File file)
    {
      // Verification de base
      final boolean ok = file.isFile() && !file.isDirectory() && file.canWrite() && file.canRead();
      if (!ok)
      {
        return false;
      }

      // Filtre sur le nom
      return FILE_PATTERN.matcher(file.getName()).matches();
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class FileEntry
  {
    File file;
    long size;
    long timestamp;

    /**
     * 
     * @param file
     */
    FileEntry(final File file)
    {
      this.file = file;
      size = file.length();
      timestamp = FileTimestampUtils.lastModified(file);
    }
  }

  /**
   * 
   * @param directory
   * @param maximumSizeMo
   */
  public TileFileCacheLight(final String directory)
  {
    super(TileFileCacheLight.class.getName());
    this.directory = new File(directory);

    // Creation du repertoire si besoin
    if (!this.directory.exists())
    {
      this.directory.mkdirs();
    }

    // Test de la disponibilite
    refreshAvailability();
  }

  @Override
  public void run()
  {
    // Boucle d'attente
    while (!isInterrupted())
    {
      // Attente
      synchronized (this)
      {
        try
        {
          Thread.sleep(60000);
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
   * @param file
   * @param sortedFiles
   * @return
   */
  private static FileEntry addFile(final File file, final List<FileEntry> sortedFiles)
  {
    // Initialisations
    final FileEntry entry = new FileEntry(file);
    int index = -1;

    // Parcours de la liste
    int listIndex = 0;
    for (final FileEntry listEntry : sortedFiles)
    {
      // Comparaison
      if (entry.timestamp <= listEntry.timestamp)
      {
        index = listIndex;
        break;
      }

      // Next
      listIndex++;
    }

    // Ajout
    if (index < 0)
    {
      sortedFiles.add(entry);
    }
    else
    {
      sortedFiles.add(index, entry);
    }

    return entry;
  }

  /**
   * 
   */
  void doClean()
  {
    // Taille inferieure a zero = pas de limite
    final Lock maximumSizeLocker = maximumSizeLock.readLock();
    try
    {
      maximumSizeLocker.lockInterruptibly();
      if (maximumSize < 0)
      {
        return;
      }
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      maximumSizeLocker.unlock();
    }

    synchronized (cleanLock)
    {
      // Verification dispo
      refreshAvailability();
      if (!available)
      {
        return;
      }

      // Liste des fichiers
      currentSize = 0;
      final File[] files = directory.listFiles(cacheFileFilter);

      // Verification dispo
      if (files == null)
      {
        return;
      }

      // Tri
      final List<FileEntry> sortedFiles = new ArrayList<FileEntry>();
      for (final File file : files)
      {
        currentSize += addFile(file, sortedFiles).size;
      }

      // Suppression des fichiers les plus anciens si la taille est depassee
      if (currentSize > maximumSize)
      {
        // Suppression tant que la taille est superieure a la taille maxi
        final Iterator<FileEntry> iterator = sortedFiles.iterator();
        while ((currentSize > maximumSize) && iterator.hasNext())
        {
          final FileEntry entry = iterator.next();
          currentSize -= entry.size;
          FileTimestampUtils.deleteTimestampFile(entry.file);
          entry.file.delete();
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

  @Override
  public void shutdown()
  {
    // Fin du Thread
    interrupt();
    ThreadUtils.join(this);

    // Fin des threads divers
    ThreadUtils.join(threads, true);
  }

  /**
   * 
   * @param file
   * @return
   */
  private static boolean isFileReadable(final File file)
  {
    return ((file != null) && file.exists() && file.isFile() && file.canRead());
  }

  @Override
  public int retrieveTileFromCache(final String providerKey, final Tile tile, final byte[] buffer) throws IOException
  {
    // Verification prealable
    if (!available)
    {
      return -1;
    }

    // Initialisations
    final File file = getFile(providerKey, tile);

    // Si inexistant
    if (!isFileReadable(file))
    {
      return -1;
    }

    // MAJ de la date d'access
    FileTimestampUtils.setLastModified(file, System.currentTimeMillis());

    // Lecture
    return IOUtils.readFile(file, buffer);
  }

  @Override
  public boolean isTileAvailable(final String providerKey, final Tile tile)
  {
    // Verification prealable
    if (!available)
    {
      return false;
    }

    // Initialisations
    final File file = getFile(providerKey, tile);

    return isFileReadable(file);
  }

  @Override
  public void refreshAvailability()
  {
    // Initialisations
    available = directory.exists() && directory.isDirectory() && directory.canWrite();
  }

  @Override
  public void storeToCache(final String providerKey, final Tile tile, final byte[] buffer, final int length) throws IOException
  {
    // Verification prealable
    checkAvailable();

    // Taille a zero : on n'ecrit pas
    final Lock maximumSizeLocker = maximumSizeLock.readLock();
    try
    {
      maximumSizeLocker.lockInterruptibly();
      if (maximumSize == 0)
      {
        return;
      }
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      maximumSizeLocker.unlock();
    }

    // Ecriture
    final File file = getFile(providerKey, tile);
    synchronized (file)
    {
      IOUtils.writeFile(file, buffer, length);
    }
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
    final Lock maximumSizeLocker = maximumSizeLock.readLock();
    try
    {
      maximumSizeLocker.lockInterruptibly();
      final boolean doClean = ((maximumSize < 0) && (maximumSizeBytes >= 0));

      if (maximumSize != maximumSizeBytes)
      {
        maximumSize = maximumSizeBytes;
      }

      if (doClean)
      {
        // Thread de nettoyage
        final Thread cleanThread = new Thread(TileFileCacheLight.class.getName() + ".firstClean")
        {
          @Override
          public void run()
          {
            // Nettoyage
            doClean();
          }
        };

        // Ajout a la liste des threads a interrompe au shutdown
        synchronized (threads)
        {
          threads.add(cleanThread);
        }

        // Demarrage
        cleanThread.start();
      }
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      maximumSizeLocker.unlock();
    }
  }
}
