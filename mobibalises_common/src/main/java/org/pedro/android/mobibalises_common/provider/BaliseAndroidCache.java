package org.pedro.android.mobibalises_common.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import org.pedro.balises.BaliseSaveableCache;

import android.content.Context;

/**
 * 
 * @author pedro.m
 */
public final class BaliseAndroidCache extends BaliseSaveableCache
{
  private final BaliseAndroidSDCache      sdCache;
  private final BaliseAndroidContextCache contextCache;

  /**
   * 
   * @param context
   */
  public BaliseAndroidCache(final Context context)
  {
    sdCache = new BaliseAndroidSDCache(context);
    contextCache = new BaliseAndroidContextCache(context);
  }

  /**
   * 
   * @param inFileInputStream
   * @param outFile
   * @throws IOException
   */
  private static void copyFile(final FileInputStream inFileInputStream, final File outFile) throws IOException
  {
    // Initialisations
    FileChannel inChannel = null;
    FileChannel outChannel = null;

    try
    {
      // Preparation sortie
      if (!outFile.exists())
      {
        outFile.createNewFile();
      }

      // Copie
      inChannel = inFileInputStream.getChannel();
      outChannel = new FileOutputStream(outFile, false).getChannel();
      inChannel.transferTo(0, inChannel.size(), outChannel);
    }
    finally
    {
      if (inChannel != null)
      {
        inChannel.close();
      }
      if (outChannel != null)
      {
        outChannel.close();
      }
    }
  }

  @Override
  public InputStream getCacheInputStream(final String key) throws IOException
  {
    // Si la SD est dispo
    if (sdCache.isAvailable())
    {
      // Copie si le fichier local est plus recent que le fichier de la SD
      final long contextTimestamp = contextCache.getCacheTimestamp(key);
      if (contextTimestamp > sdCache.getCacheTimestamp(key))
      {
        copyFile((FileInputStream)contextCache.getCacheInputStream(key), sdCache.getFile(key));
        sdCache.setCacheTimestamp(key, contextTimestamp);
      }

      // Effacement du fichier local
      if (contextTimestamp >= 0)
      {
        contextCache.clearCache(key);
      }

      return sdCache.getCacheInputStream(key);
    }

    // Le plus recent
    return contextCache.getCacheInputStream(key);
  }

  @Override
  public OutputStream getCacheOutputStream(final String key) throws IOException
  {
    if (sdCache.isAvailable())
    {
      // Effacement du fichier local si present
      if (contextCache.getCacheTimestamp(key) >= 0)
      {
        contextCache.clearCache(key);
      }

      return sdCache.getCacheOutputStream(key);
    }

    return contextCache.getCacheOutputStream(key);
  }

  @Override
  public boolean clearCache(final String key) throws IOException
  {
    final boolean sdCleared = sdCache.clearCache(key);
    final boolean contextCleared = contextCache.clearCache(key);

    return sdCleared || contextCleared;
  }

  @Override
  public long getCacheTimestamp(final String key)
  {
    final long sdTimestamp = sdCache.getCacheTimestamp(key);
    final long contextTimestamp = contextCache.getCacheTimestamp(key);

    return (sdTimestamp >= contextTimestamp ? sdTimestamp : contextTimestamp);
  }

  @Override
  public void setCacheTimestamp(final String key, final long stamp)
  {
    sdCache.setCacheTimestamp(key, stamp);
    contextCache.setCacheTimestamp(key, stamp);
  }

  @Override
  public void onShutdown()
  {
    contextCache.onShutdown();
  }
}
