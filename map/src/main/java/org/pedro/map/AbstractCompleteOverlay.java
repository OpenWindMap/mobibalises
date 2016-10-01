package org.pedro.map;

import java.util.concurrent.locks.Lock;

import org.pedro.utils.ThreadUtils;

/**
 * 
 * @author pedro.m
 *
 * @param <ImageData>
 * @param <Drawer>
 */
public abstract class AbstractCompleteOverlay<ImageData, Drawer> implements Overlay<ImageData, Drawer>
{
  protected MapDisplayer<ImageData, Drawer, ?>             mapDisplayer;
  protected final Projection                               projection;
  protected final MapController                            controller;
  protected final GraphicsHelper<ImageData, Drawer, ?>     graphicsHelper;

  // Buffers
  private final Object                                     buffersLock        = new Object();
  private ImageData                                        mainBuffer;
  private Drawer                                           mainDrawer;
  private ImageData                                        tempBuffer;
  private Drawer                                           tempDrawer;

  private int                                              oldDisplayerWidth  = -1;
  private int                                              oldDisplayerHeight = -1;

  private boolean                                          lateDraw;

  private final ImageDataTransformation<ImageData, Drawer> transformation     = new ImageDataTransformation<ImageData, Drawer>();
  private final Object                                     transformationLock = new Object();

  private final RedrawThread                               redrawThread;

  /**
   * 
   * @author pedro.m
   */
  private static class RedrawThread extends Thread
  {
    private AbstractCompleteOverlay<?, ?> overlay;
    private int                           demands = 0;

    /**
     * 
     * @param name
     */
    RedrawThread(final String name, final AbstractCompleteOverlay<?, ?> overlay)
    {
      super(name + ".RedrawThread");
      this.overlay = overlay;
    }

    @Override
    public void run()
    {
      while (!isInterrupted())
      {
        synchronized (this)
        {
          while ((demands == 0) && !isInterrupted())
          {
            try
            {
              wait();
            }
            catch (final InterruptedException e)
            {
              Thread.currentThread().interrupt();
              return;
            }
          }
        }

        if (isRedrawAsked() && !isInterrupted())
        {
          overlay.redraw();
        }
      }

      // Fin
      overlay = null;
    }

    /**
     * 
     * @return
     */
    private boolean isRedrawAsked()
    {
      synchronized (this)
      {
        final boolean retour = (demands > 0);
        demands = 0;
        return retour;
      }
    }

    /**
     * 
     */
    void requestRedraw()
    {
      synchronized (this)
      {
        demands++;
        this.notify();
      }
    }
  }

  /**
   * 
   * @param mapDisplayer
   * @param name
   */
  public AbstractCompleteOverlay(final MapDisplayer<ImageData, Drawer, ?> mapDisplayer, final String name)
  {
    // Initialisations
    this.redrawThread = new RedrawThread(name, this);
    this.mapDisplayer = mapDisplayer;
    this.projection = mapDisplayer.getProjection();
    this.controller = mapDisplayer.getController();
    this.graphicsHelper = mapDisplayer.getGraphicsHelper();

    // Buffers
    adjustBufferSizes();
  }

  @Override
  public final void requestRedraw()
  {
    redrawThread.requestRedraw();
  }

  @Override
  public final void requestRedraw(final GeoPoint inCenter, final int inZoom)
  {
    redrawThread.requestRedraw();
  }

  /**
   * 
   */
  public void start()
  {
    redrawThread.start();
  }

  @Override
  public final void draw(final Drawer canvas)
  {
    // Le fond
    synchronized (transformationLock)
    {
      transformation.drawTransformation(graphicsHelper, canvas, mainBuffer, 0, 0);
    }
  }

  @Override
  public final void transformationPostTranslate(final int deltaX, final int deltaY, final GeoPoint inCenter, final int inZoom)
  {
    synchronized (transformationLock)
    {
      transformation.postTranslate(deltaX, deltaY);
    }
  }

  @Override
  public final void transformationPostZoom(final int deltaZoom, final GeoPoint inCenter, final int inZoom)
  {
    synchronized (transformationLock)
    {
      transformation.postZoom(deltaZoom);
    }
  }

  @Override
  public final void transformationPostIntermediateZoom(final float deltaZoom, final int deltaX, final int deltaY, final GeoPoint inCenter, final int inZoom)
  {
    synchronized (transformationLock)
    {
      transformation.postIntermediateZoom(deltaZoom, deltaX, deltaY);
    }
  }

  @Override
  public final void transformationResetIntermediateZoom(final GeoPoint inCenter, final int inZoom)
  {
    synchronized (transformationLock)
    {
      transformation.resetIntermediateZoom();
    }
  }

  /**
   * 
   */
  private void swapBuffers()
  {
    // Swap
    final ImageData swapBuffer = mainBuffer;
    final Drawer swapDrawer = mainDrawer;
    mainBuffer = tempBuffer;
    mainDrawer = tempDrawer;
    tempBuffer = swapBuffer;
    tempDrawer = swapDrawer;
  }

  /**
   * 
   */
  void redraw()
  {
    synchronized (buffersLock)
    {
      if (tempBuffer != null)
      {
        // Transparent !
        graphicsHelper.setTransparent(tempBuffer);

        final Lock controllerLocker = controller.getReadLock();
        try
        {
          // Lock
          controllerLocker.lockInterruptibly();

          synchronized (transformationLock)
          {
            // Coordonnees du coin superieur gauche
            final long left = Math.round(MercatorProjection.longitudeToPixelX(controller.getCenter().getLongitude(), controller.getZoom())) - (mapDisplayer.getPixelWidth() / 2);
            final long top = Math.round(MercatorProjection.latitudeToPixelY(controller.getCenter().getLatitude(), controller.getZoom())) - (mapDisplayer.getPixelHeight() / 2);

            // Dessin
            lateDraw = drawOverlay(tempDrawer, left, top);

            // RAZ
            transformation.reset();

            // Swap
            swapBuffers();
          }
        }
        catch (final InterruptedException ie)
        {
          Thread.currentThread().interrupt();
        }
        finally
        {
          controllerLocker.unlock();
        }

        // Repaint principal
        mapDisplayer.redraw();
      }
    }
  }

  /**
   * 
   * @param canvas
   * @param left
   * @param top
   * @return
   */
  protected abstract boolean drawOverlay(Drawer canvas, long left, long top);

  @Override
  public final boolean needsLateDraw()
  {
    return lateDraw;
  }

  /**
   * 
   */
  private void freeAllImageData()
  {
    synchronized (buffersLock)
    {
      graphicsHelper.freeImageData(mainBuffer);
      mainBuffer = null;

      graphicsHelper.freeImageData(tempBuffer);
      tempBuffer = null;
    }
  }

  /**
   * 
   */
  private void adjustBufferSizes()
  {
    synchronized (buffersLock)
    {
      if ((oldDisplayerWidth != mapDisplayer.getPixelWidth()) || (oldDisplayerHeight != mapDisplayer.getPixelHeight()))
      {
        // Sauvegarde dimensions
        oldDisplayerWidth = mapDisplayer.getPixelWidth();
        oldDisplayerHeight = mapDisplayer.getPixelHeight();

        // Liberation memoire
        freeAllImageData();

        // Allocations
        if ((mapDisplayer.getPixelWidth() > 0) && (mapDisplayer.getPixelHeight() > 0))
        {
          // Main
          mainBuffer = graphicsHelper.newAlphaImageData(mapDisplayer.getPixelWidth(), mapDisplayer.getPixelHeight());
          graphicsHelper.setTransparent(mainBuffer);
          mainDrawer = graphicsHelper.getDrawer(mainBuffer);

          // Temp
          tempBuffer = graphicsHelper.newAlphaImageData(mapDisplayer.getPixelWidth(), mapDisplayer.getPixelHeight());
          graphicsHelper.setTransparent(tempBuffer);
          tempDrawer = graphicsHelper.getDrawer(tempBuffer);
        }
      }
    }
  }

  @Override
  public void onMapDisplayerSizeChanged()
  {
    adjustBufferSizes();
  }

  @Override
  public void onShutdown()
  {
    // Thread
    redrawThread.interrupt();
    ThreadUtils.join(redrawThread);

    // Liberation graphiques
    freeAllImageData();

    // Divers
    mapDisplayer = null;
  }

  @Override
  public boolean onKeyPressed(final int keyCode)
  {
    return false;
  }
}
