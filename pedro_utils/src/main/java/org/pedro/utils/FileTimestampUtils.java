package org.pedro.utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 
 * @author pedro.m
 */
public abstract class FileTimestampUtils
{
  /**
   * 
   * @param file
   * @return
   */
  private static File getTimestampFile(final File file)
  {
    return new File(file.getParent(), file.getName() + ".ts");
  }

  /**
   * 
   * @param file
   * @return
   */
  public static boolean deleteTimestampFile(final File file)
  {
    final File tsFile = getTimestampFile(file);
    if (tsFile.isFile() && tsFile.exists() && tsFile.canWrite())
    {
      return tsFile.delete();
    }

    return false;
  }

  /**
   * 
   * @param source
   * @param dest
   * @return
   */
  public static boolean renameTimestampFileTo(final File source, final File dest)
  {
    final File tsSource = getTimestampFile(source);
    final File tsDest = getTimestampFile(dest);

    if (tsSource.isFile() && tsSource.exists())
    {
      return tsSource.renameTo(tsDest);
    }

    return false;
  }

  /**
   * 
   * @param file
   * @param timestamp
   * @return
   */
  public static boolean setLastModified(final File file, final long timestamp)
  {
    // Essai via API standard
    final boolean modified = file.setLastModified(timestamp);
    if (modified)
    {
      // OK
      return true;
    }

    // ... API standard ne fonctionne pas => creation d'un fichier supplementaire
    // Initialisations
    final File tsFile = getTimestampFile(file);
    FileOutputStream fos = null;
    DataOutputStream dos = null;

    try
    {
      // Ouvertures
      fos = new FileOutputStream(tsFile, false);
      dos = new DataOutputStream(fos);

      // Ecriture
      dos.writeLong(timestamp);

      return true;
    }
    catch (final IOException ioe)
    {
      ioe.printStackTrace();
      return false;
    }
    finally
    {
      try
      {
        // Fermetures
        if (dos != null)
        {
          dos.close();
        }
        if (fos != null)
        {
          fos.close();
        }
      }
      catch (final IOException ioe)
      {
        ioe.printStackTrace();
      }
    }
  }

  /**
   * 
   * @param file
   * @return
   */
  public static long lastModified(final File file)
  {
    // Initialisations
    final File tsFile = getTimestampFile(file);

    // API standard ?
    if (!tsFile.isFile() || !tsFile.exists() || !tsFile.canRead())
    {
      return file.lastModified();
    }

    // API non standard : par fichier supplementaire
    FileInputStream fis = null;
    DataInputStream dis = null;

    try
    {
      // Ouvertures
      fis = new FileInputStream(tsFile);
      dis = new DataInputStream(fis);

      // Ecriture
      return dis.readLong();
    }
    catch (final IOException ioe)
    {
      ioe.printStackTrace();
      return 0;
    }
    finally
    {
      try
      {
        // Fermetures
        if (dis != null)
        {
          dis.close();
        }
        if (fis != null)
        {
          fis.close();
        }
      }
      catch (final IOException ioe)
      {
        ioe.printStackTrace();
      }
    }
  }
}
