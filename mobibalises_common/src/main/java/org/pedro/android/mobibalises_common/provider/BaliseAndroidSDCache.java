package org.pedro.android.mobibalises_common.provider;

import android.content.Context;

import org.pedro.balises.BaliseSaveableCache;
import org.pedro.utils.FileTimestampUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 * @author pedro.m
 */
public final class BaliseAndroidSDCache extends BaliseSaveableCache
{
  private final File storagePath;

  /**
   * 
   */
  public BaliseAndroidSDCache(final Context context)
  {
    storagePath = new File(context.getExternalFilesDir(null), "balises");
    storagePath.mkdirs();
  }

  /**
   * 
   * @return
   */
  public boolean isAvailable()
  {
    return (storagePath.exists() && storagePath.isDirectory() && storagePath.canWrite());
  }

  /**
   * 
   * @param key
   * @return
   */
  protected File getFile(final String key)
  {
    return new File(storagePath, key);
  }

  @Override
  public InputStream getCacheInputStream(final String key) throws IOException
  {
    return new FileInputStream(getFile(key));
  }

  @Override
  public OutputStream getCacheOutputStream(final String key) throws IOException
  {
    return new FileOutputStream(getFile(key), false);
  }

  @Override
  public boolean clearCache(final String key) throws IOException
  {
    final File file = getFile(key);
    return FileTimestampUtils.deleteTimestampFile(file) && file.delete();
  }

  @Override
  public long getCacheTimestamp(final String key)
  {
    final File file = getFile(key);

    if (file.exists() && file.isFile() && file.canRead())
    {
      return FileTimestampUtils.lastModified(file);
    }

    return -1;
  }

  @Override
  public void setCacheTimestamp(final String key, final long stamp)
  {
    final File file = getFile(key);

    if (file.exists() && file.isFile() && file.canWrite())
    {
      FileTimestampUtils.setLastModified(file, stamp);
    }
  }

  @Override
  public void onShutdown()
  {
    // Nothing
  }
}
