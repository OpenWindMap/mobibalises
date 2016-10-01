package org.pedro.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

/**
 * 
 * @author pedro.m
 *
 */
public abstract class IOUtils
{
  /**
   * 
   * @param file
   * @param buffer
   * @return
   * @throws IOException
   */
  public static int readFile(final File file, final byte[] buffer) throws IOException
  {
    // Initialisations
    FileOutputStream fos = null;
    FileLock lock = null;
    FileInputStream fis = null;
    BufferedInputStream bis = null;

    try
    {
      fos = new FileOutputStream(file, true);
      lock = fos.getChannel().lock();
      fis = new FileInputStream(file);
      bis = new BufferedInputStream(fis);
      return bis.read(buffer);
    }
    finally
    {
      if (bis != null)
      {
        bis.close();
      }
      if (fis != null)
      {
        fis.close();
      }
      if (lock != null)
      {
        lock.release();
      }
      if (fos != null)
      {
        fos.close();
      }
    }
  }

  /**
   * 
   * @param file
   * @param buffer
   * @throws IOException
   */
  public static void writeFile(final File file, final byte[] buffer, final int length) throws IOException
  {
    FileLock lock = null;
    FileOutputStream fos = null;
    BufferedOutputStream bos = null;

    try
    {
      fos = new FileOutputStream(file, false);
      lock = fos.getChannel().lock();
      bos = new BufferedOutputStream(fos);
      bos.write(buffer, 0, length);
    }
    finally
    {
      if (lock != null)
      {
        lock.release();
      }
      if (bos != null)
      {
        bos.close();
      }
      if (fos != null)
      {
        fos.close();
      }
    }
  }

  /**
   * 
   * @param url
   */
  public static void checkNotMainThread(final String url)
  {
    final String currentThreadName = Thread.currentThread().getName();
    if ("main".equals(currentThreadName))
    {
      System.out.println("### Not OK for : " + url);
      final Exception ex = new RuntimeException("Main Thread for url : " + url);
      ex.printStackTrace(System.err);
    }
    else
    {
      System.out.println("OK " + currentThreadName + " for : " + url);
    }
  }
}
