package org.pedro.map;

import java.io.IOException;
import java.net.URL;

/**
 * 
 * @author pedro.m
 *
 * @param <ImageData>
 * @param <Drawer>
 * @param <ColorData>
 */
public interface GraphicsHelper<ImageData, Drawer, ColorData>
{
  /**
   * 
   * @param canvas
   * @param width
   * @param height
   */
  public void fillImageData(final Drawer canvas, final ColorData color, final int width, final int height);

  /**
   * 
   * @param canvas
   * @param color
   * @param left
   * @param top
   * @param width
   * @param height
   */
  public void fillImageData(final Drawer canvas, final ColorData color, final int left, final int top, final int width, final int height);

  /**
   * 
   * @param image
   */
  public void setTransparent(final ImageData image);

  /**
   * 
   * @param canvas
   * @param image
   * @param point
   */
  public void drawImageData(final Drawer canvas, final ImageData image, final int x, final int y);

  /**
   * 
   * @param canvas
   * @param text
   * @param x
   * @param y
   * @param color
   */
  public void drawText(final Drawer canvas, final String text, final int x, final int y, final ColorData color);

  /**
   * 
   * @param canvas
   * @param left
   * @param top
   * @param right
   * @param bottom
   * @param color
   */
  public void drawLine(final Drawer canvas, final int left, final int top, final int right, final int bottom, final ColorData color);

  /**
   * 
   * @param canvas
   * @param image
   * @param zoom
   * @param dx
   * @param dy
   */
  public void intermediateZoom(final Drawer canvas, final ImageData image, final float zoom, final int dx, final int dy);

  /**
   * 
   * @param canvas
   * @param image
   * @param zoom
   * @param x
   * @param y
   */
  public void zoomIn(final Drawer canvas, final ImageData image, final int zoom, final int x, final int y);

  /**
   * 
   * @param canvas
   * @param image
   * @param zoom
   * @param x
   * @param y
   */
  public void zoomOut(final Drawer canvas, final ImageData image, final int zoom, final int x, final int y);

  /**
   * 
   * @param width
   * @param height
   * @return
   */
  public ImageData newAlphaImageData(final int width, final int height);

  /**
   * 
   * @param imageData
   */
  public void freeImageData(final ImageData imageData);

  /**
   * 
   * @param image
   * @return
   */
  public Drawer getDrawer(final ImageData image);

  /**
   * 
   * @param red
   * @param green
   * @param blue
   * @return
   */
  public ColorData newColor(final int red, final int green, final int blue);

  /**
   * 
   * @param red
   * @param green
   * @param blue
   * @param alpha
   * @return
   */
  public ColorData newColor(final int red, final int green, final int blue, final int alpha);

  /**
   * 
   * @param url
   * @param width
   * @param height
   * @return
   * @throws IOException
   */
  public ImageData getTileImageData(final URL url, final int width, final int height) throws IOException;

  /**
   * 
   * @param buffer
   * @param length
   * @return
   * @throws IOException
   */
  public ImageData getTileImageData(final byte[] buffer, final int length) throws IOException;
  
  /**
   * 
   */
  public void onShutdown();
}
