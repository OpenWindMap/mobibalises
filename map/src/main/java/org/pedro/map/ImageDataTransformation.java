package org.pedro.map;

/**
 * 
 * @author pedro.m
 */
public final class ImageDataTransformation<ImageData, Drawer>
{
  private boolean doIntermediateZoom = false;
  private float   intermediateZoom;
  private int     zoom;
  private int     deltaX;
  private int     deltaY;

  /**
   * 
   * @param graphicsHelper
   * @param canvas
   * @param image
   * @param dx
   * @param dy
   */
  public void drawTransformation(final GraphicsHelper<ImageData, Drawer, ?> graphicsHelper, final Drawer canvas, final ImageData image, final int dx, final int dy)
  {
    if (doIntermediateZoom)
    {
      graphicsHelper.intermediateZoom(canvas, image, intermediateZoom, deltaX, deltaY);
      return;
    }

    // Identite
    if (isIdentity())
    {
      graphicsHelper.drawImageData(canvas, image, dx, dy);
    }
    else
    {
      // Puis le zoom
      if (zoom > 0)
      {
        graphicsHelper.zoomIn(canvas, image, zoom, deltaX + dx, deltaY + dy);
      }
      else if (zoom < 0)
      {
        graphicsHelper.zoomOut(canvas, image, -zoom, deltaX + dx, deltaY + dy);
      }
      else if ((deltaX != 0) || (deltaY != 0))
      {
        graphicsHelper.drawImageData(canvas, image, deltaX + dx, deltaY + dy);
      }
    }
  }

  /**
   * 
   */
  public void reset()
  {
    doIntermediateZoom = false;
    intermediateZoom = 1;
    zoom = 0;
    deltaX = 0;
    deltaY = 0;
  }

  /**
   * 
   * @return
   */
  public boolean isIdentity()
  {
    return (zoom == 0) && (deltaX == 0) && (deltaY == 0);
  }

  /**
   * 
   * @param deltaZoom
   */
  public void postZoom(final int deltaZoom)
  {
    if (deltaZoom > 0)
    {
      deltaX *= Math.pow(2, deltaZoom);
      deltaY *= Math.pow(2, deltaZoom);
    }
    else if (deltaZoom < 0)
    {
      deltaX /= Math.pow(2, -deltaZoom);
      deltaY /= Math.pow(2, -deltaZoom);
    }

    this.zoom += deltaZoom;
  }

  /**
   * 
   * @param inZoom
   */
  public void postIntermediateZoom(final float inZoom, final int dx, final int dy)
  {
    doIntermediateZoom = true;
    this.intermediateZoom = inZoom;
    this.deltaX = dx;
    this.deltaY = dy;
  }

  /**
   * 
   */
  public void resetIntermediateZoom()
  {
    doIntermediateZoom = false;
    this.intermediateZoom = 0;
    this.deltaX = 0;
    this.deltaY = 0;
  }

  /**
   * 
   * @return
   */
  public boolean isIntermediateZoomInProgress()
  {
    return doIntermediateZoom;
  }

  /**
   * 
   * @param inDeltaX
   * @param inDeltaY
   */
  public void postTranslate(final int inDeltaX, final int inDeltaY)
  {
    deltaX += inDeltaX;
    deltaY += inDeltaY;
  }

  /**
   * @return the zoom
   */
  public int getZoom()
  {
    return zoom;
  }

  /**
   * @return the deltaX
   */
  public int getDeltaX()
  {
    return deltaX;
  }

  /**
   * @return the deltaY
   */
  public int getDeltaY()
  {
    return deltaY;
  }
}
