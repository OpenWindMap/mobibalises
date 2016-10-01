package org.pedro.map.tileprovider;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.pedro.map.Tile;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractURLTileProvider extends AbstractTileProvider
{
  private static final String USER_AGENT       = "User-Agent";
  private static final String REFERER          = "Referer";
  private static final int    READ_BUFFER_SIZE = 1024;
  private static final int    CONNECT_TIMEOUT  = 5000;

  /**
   * 
   * @param key
   */
  public AbstractURLTileProvider(final String key)
  {
    super(key);
  }

  @Override
  public int readData(final Tile tile, final byte[] buffer) throws IOException
  {
    // Initialisations
    URLConnection cnx = null;
    InputStream is = null;
    int offset = 0;

    try
    {
      // Connexion
      cnx = getTileURL(tile).openConnection();
      cnx.setConnectTimeout(CONNECT_TIMEOUT);
      final String userAgent = getUserAgent();
      if (userAgent != null)
      {
        cnx.setRequestProperty(USER_AGENT, userAgent);
      }
      final String referer = getReferer();
      if (referer != null)
      {
        cnx.setRequestProperty(REFERER, referer);
      }
      cnx.connect();

      // InputStream
      is = new BufferedInputStream(cnx.getInputStream());

      // Lecture
      if (buffer != null)
      {
        int read = is.read(buffer, 0, READ_BUFFER_SIZE);
        while ((read > 0))
        {
          offset += read;
          read = is.read(buffer, offset, READ_BUFFER_SIZE);
        }
      }

      // Fin
      return offset;
    }
    catch (final Throwable th)
    {
      final IOException ioe = new IOException(th.getMessage());
      ioe.setStackTrace(th.getStackTrace());
      throw ioe;
    }
    finally
    {
      if (is != null)
      {
        is.close();
      }
    }
  }

  @Override
  public void shutdown()
  {
    //Rien
  }

  /**
   * 
   * @param tile
   * @return
   * @throws IOException
   */
  public abstract URL getTileURL(final Tile tile) throws IOException;

  /**
   * 
   * @return
   */
  @SuppressWarnings("static-method")
  public String getUserAgent()
  {
    return null;
  }

  /**
   *
   * @return
   */
  public String getReferer()
  {
    return null;
  }
}
