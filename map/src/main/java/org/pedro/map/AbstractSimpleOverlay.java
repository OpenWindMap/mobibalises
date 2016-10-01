package org.pedro.map;

import java.util.concurrent.locks.Lock;

/**
 * 
 * @author pedro.m
 *
 * @param <ImageData>
 * @param <Drawer>
 */
public abstract class AbstractSimpleOverlay<ImageData, Drawer> implements Overlay<ImageData, Drawer>
{
  protected MapDisplayer<ImageData, Drawer, ?>               mapDisplayer;
  protected final Projection                                 projection;
  private final MapController                                controller;
  protected final GraphicsHelper<ImageData, Drawer, ?>       graphicsHelper;

  protected final GeoPoint                                   center             = new GeoPoint();
  protected int                                              zoom;
  protected final ImageDataTransformation<ImageData, Drawer> transformation     = new ImageDataTransformation<ImageData, Drawer>();

  protected final Object                                     transformationLock = new Object();

  /**
   * 
   * @param mapDisplayer
   */
  public AbstractSimpleOverlay(final MapDisplayer<ImageData, Drawer, ?> mapDisplayer)
  {
    // Initialisations
    this.mapDisplayer = mapDisplayer;
    this.projection = mapDisplayer.getProjection();
    this.controller = mapDisplayer.getController();
    this.graphicsHelper = mapDisplayer.getGraphicsHelper();
  }

  @Override
  public final void transformationPostTranslate(final int deltaX, final int deltaY, final GeoPoint inCenter, final int inZoom)
  {
    synchronized (transformationLock)
    {
      // Centre et zoom
      center.copy(inCenter);
      zoom = inZoom;

      // Transformation
      transformation.postTranslate(deltaX, deltaY);
    }
  }

  @Override
  public final void transformationPostZoom(final int deltaZoom, final GeoPoint inCenter, final int inZoom)
  {
    synchronized (transformationLock)
    {
      // Centre et zoom
      center.copy(inCenter);
      zoom = inZoom;

      // Transformation
      transformation.postZoom(deltaZoom);
    }
  }

  @Override
  public final void transformationPostIntermediateZoom(final float deltaZoom, final int deltaX, final int deltaY, final GeoPoint inCenter, final int inZoom)
  {
    synchronized (transformationLock)
    {
      // Centre et zoom
      center.copy(inCenter);
      zoom = inZoom;

      // Transformation
      transformation.postIntermediateZoom(deltaZoom, deltaX, deltaY);
    }
  }

  @Override
  public final void transformationResetIntermediateZoom(final GeoPoint inCenter, final int inZoom)
  {
    synchronized (transformationLock)
    {
      // Centre et zoom
      center.copy(inCenter);
      zoom = inZoom;

      // Transformation
      transformation.resetIntermediateZoom();
    }
  }

  @Override
  public final void requestRedraw()
  {
    final Lock controllerLocker = controller.getReadLock();
    try
    {
      // Lock
      controllerLocker.lockInterruptibly();

      synchronized (transformationLock)
      {
        // RAZ
        transformation.reset();
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

    // Repaint
    mapDisplayer.redraw();
  }

  @Override
  public final void requestRedraw(final GeoPoint inCenter, final int inZoom)
  {
    synchronized (transformationLock)
    {
      // Centre et zoom
      center.copy(inCenter);
      zoom = inZoom;

      // RAZ
      transformation.reset();
    }

    // Repaint
    mapDisplayer.redraw();
  }

  @Override
  public final void onMapDisplayerSizeChanged()
  {
    // Nothing
  }

  @Override
  public void onShutdown()
  {
    // Divers
    mapDisplayer = null;
  }

  @Override
  public final boolean onKeyPressed(final int keyCode)
  {
    return false;
  }
}
