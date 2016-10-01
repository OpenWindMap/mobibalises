package org.pedro.map;

import java.io.IOException;
import java.util.List;

/**
 * 
 * @author pedro.m
 *
 * @param <ImageData>
 * @param <Drawer>
 * @param <ColorData>
 */
public interface MapDisplayer<ImageData, Drawer, ColorData>
{
  /**
   * 
   * @return
   */
  public int getPixelWidth();

  /**
   * 
   * @return
   */
  public int getPixelHeight();

  /**
   * 
   */
  public void redraw();

  /**
   * 
   */
  public void invalidateDisplayer();

  /**
   * 
   * @return
   */
  public MapController getController();

  /**
   * 
   * @return
   */
  public Projection getProjection();

  /**
   * 
   * @param tile
   * @param point
   * @param ioe
   */
  public void onTileDataNotAvailable(final Tile tile, final Point point, final IOException ioe);

  /**
   * 
   * @param tileProvider
   */
  public void setTileProvider(final TileProvider tileProvider);

  /**
   * 
   * @param tileProvider
   * @throws IOException
   */
  public void setTileProvider(final BuiltInTileProvider tileProvider) throws IOException;

  /**
   * 
   * @return
   */
  public GraphicsHelper<ImageData, Drawer, ColorData> getGraphicsHelper();

  /**
   * 
   * @param writeLock
   * @return
   * @throws InterruptedException
   */
  public List<Overlay<ImageData, Drawer>> getOverlays(final boolean writeLock) throws InterruptedException;

  /**
   * 
   */
  public void unlockReadOverlays();

  /**
   * 
   */
  public void unlockWriteOverlays();

  /**
   * 
   * @param bingApiKey
   */
  public void setBingApiKey(final String bingApiKey);

  /**
   * 
   * @param apiKey
   * @param token
   */
  public void setCloudMadeKeys(final String apiKey, final String token);

  /**
   * 
   * @param googleApiKey
   */
  public void setGoogleApiKey(final String googleApiKey);

  /**
   * 
   * @param ignApiKey
   */
  public void setIgnApiKey(final String ignApiKey);

  /**
   * 
   * @param isApiKeyDebug
   */
  public void setIsApiKeyDebug(final boolean isApiKeyDebug);

  /**
   * 
   * @return
   */
  public Object[] getTileProvidersParams();

  /**
   * 
   * @return
   */
  public TileCache getTileCache();

  /**
   * 
   * @param tile
   * @param point
   * @param ioe
   * @param drawer
   * @param missingTileColor
   */
  public void onMissingTileException(final Tile tile, final Point point, final IOException ioe, final Drawer drawer, final ColorData missingTileColor);

  /**
   * 
   * @return
   */
  public boolean isLightMode();
}
