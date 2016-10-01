package org.pedro.android.mobibalises_common.provider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.balises.BaliseSaveableCache;
import org.pedro.utils.FileTimestampUtils;

/**
 * 
 * @author pedro.m
 */
public final class BaliseAndroidSDCache extends BaliseSaveableCache
{
  private static final File STORAGE_PATH = new File(ActivityCommons.MOBIBALISES_EXTERNAL_STORAGE_PATH, "balises");

  /**
   * 
   */
  public BaliseAndroidSDCache()
  {
    STORAGE_PATH.mkdirs();
  }

  /**
   * 
   * @return
   */
  public static boolean isAvailable()
  {
    return (STORAGE_PATH.exists() && STORAGE_PATH.isDirectory() && STORAGE_PATH.canWrite());
  }

  /**
   * 
   * @param key
   * @return
   */
  protected static File getFile(final String key)
  {
    return new File(STORAGE_PATH, key);
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
