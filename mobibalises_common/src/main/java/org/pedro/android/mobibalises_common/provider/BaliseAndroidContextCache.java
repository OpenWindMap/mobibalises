package org.pedro.android.mobibalises_common.provider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.pedro.balises.BaliseSaveableCache;
import org.pedro.utils.FileTimestampUtils;

import android.content.Context;

/**
 * 
 * @author pedro.m
 */
public final class BaliseAndroidContextCache extends BaliseSaveableCache
{
  private Context context;

  /**
   * 
   * @param context
   */
  public BaliseAndroidContextCache(final Context context)
  {
    this.context = context;
  }

  @Override
  public InputStream getCacheInputStream(final String key) throws IOException
  {
    return context.openFileInput(key);
  }

  @Override
  public OutputStream getCacheOutputStream(final String key) throws IOException
  {
    return context.openFileOutput(key, Context.MODE_PRIVATE);
  }

  @Override
  public boolean clearCache(final String key) throws IOException
  {
    return context.deleteFile(key);
  }

  @Override
  public long getCacheTimestamp(final String key)
  {
    if (context != null)
    {
      final File file = context.getFileStreamPath(key);

      if (file.exists() && file.isFile() && file.canRead())
      {
        return FileTimestampUtils.lastModified(file);
      }
    }

    return -1;
  }

  @Override
  public void setCacheTimestamp(final String key, final long stamp)
  {
    if (context != null)
    {
      final File file = context.getFileStreamPath(key);

      if (file.exists() && file.isFile() && file.canWrite())
      {
        FileTimestampUtils.setLastModified(file, stamp);
      }
    }
  }

  @Override
  public void onShutdown()
  {
    context = null;
  }
}
