package org.pedro.map;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.pedro.utils.ReadManyWriteSingleLock;

/**
 * 
 * @author pedro.m
 *
 * @param <ImageData>
 * @param <Drawer>
 * @param <Item>
 */
public abstract class ItemizedOverlay<ImageData, Drawer, Item extends OverlayItem<Drawer>> extends AbstractCompleteOverlay<ImageData, Drawer>
{
  protected final ReadWriteLock itemsLock = new ReadManyWriteSingleLock();
  protected final List<Item>    items     = new ArrayList<Item>();
  protected int                 lastZoom  = -1;
  protected final Point         drawPoint = new Point();

  /**
   * 
   * @param mapDisplayer
   * @param name
   */
  public ItemizedOverlay(final MapDisplayer<ImageData, Drawer, ?> mapDisplayer, final String name)
  {
    super(mapDisplayer, name);
  }

  /**
   * 
   * @param item
   */
  public void addItem(final Item item)
  {
    final Lock itemsLocker = itemsLock.writeLock();
    try
    {
      itemsLocker.lockInterruptibly();
      items.add(item);
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      itemsLocker.unlock();
    }
  }

  /**
   * 
   */
  public void clearItems()
  {
    final Lock itemsLocker = itemsLock.writeLock();
    try
    {
      itemsLocker.lockInterruptibly();
      items.clear();
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      itemsLocker.unlock();
    }
  }

  @Override
  public boolean drawOverlay(final Drawer canvas, final long left, final long top)
  {
    final Lock itemsLocker = itemsLock.readLock();
    try
    {
      // Lock
      itemsLocker.lockInterruptibly();

      // Initialisations
      final boolean zoomChanged = controller.getZoom() != lastZoom;
      lastZoom = controller.getZoom();

      for (final OverlayItem<Drawer> item : items)
      {
        // Coordonnees OK ?
        if (item.getPoint() == null)
        {
          continue;
        }

        // Conversion en pixels
        if (zoomChanged || (item.x < 0) || (item.y < 0))
        {
          item.x = Math.round(MercatorProjection.longitudeToPixelX(item.getPoint().getLongitude(), lastZoom));
          item.y = Math.round(MercatorProjection.latitudeToPixelY(item.getPoint().getLatitude(), lastZoom));
        }
        drawPoint.set((int)(item.x - left), (int)(item.y - top));

        // Recuperation de coins de l'item
        final Rect bounds = item.getDrawable().getDisplayBounds();
        if ((drawPoint.x >= bounds.left) && (drawPoint.y >= bounds.top) && (drawPoint.x <= mapDisplayer.getPixelWidth() + bounds.right) && (drawPoint.y <= mapDisplayer.getPixelHeight() + bounds.bottom))
        {
          item.getDrawable().draw(canvas, drawPoint);
        }
      }

      return false;
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      itemsLocker.unlock();
    }

    return false;
  }

  /**
   * 
   * @param point
   * @param nbMax
   * @return
   */
  protected List<Item> findItemsUnder(final Point point, final int nbMax)
  {
    final Lock itemsLocker = itemsLock.readLock();
    try
    {
      itemsLocker.lockInterruptibly();
      return findItemsUnder(items, point, nbMax);
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      itemsLocker.unlock();
    }

    return null;
  }

  /**
   * 
   * @param candidates
   * @param point
   * @param nbMax
   * @return
   */
  protected final List<Item> findItemsUnder(final List<Item> candidates, final Point point, final int nbMax)
  {
    // Initialisations
    final List<Item> retour = new ArrayList<Item>();
    final Point itemPoint = new Point();

    // Inspection des items
    synchronized (candidates)
    {
      Item item;
      int nbFound = 0;
      for (int i = candidates.size() - 1; i >= 0; i--)
      {
        item = candidates.get(i);
        if (item.getPoint() != null)
        {
          mapDisplayer.getProjection().toPixels(item.getPoint(), itemPoint);
          final Rect bounds = item.getDrawable().getInteractiveBounds();
          if ((point.x >= itemPoint.x + bounds.left) && (point.x <= itemPoint.x + bounds.right) && (point.y >= itemPoint.y + bounds.top) && (point.y <= itemPoint.y + bounds.bottom))
          {
            if (isItemClickable(item))
            {
              retour.add(item);
              nbFound++;
              if ((nbMax >= 0) && (nbFound >= nbMax))
              {
                break;
              }
            }
          }
        }
      }
    }

    return retour;
  }

  /**
   * 
   * @param item
   * @return
   */
  @SuppressWarnings("unused")
  protected boolean isItemClickable(final Item item)
  {
    return true;
  }

  @Override
  public boolean onTap(final GeoPoint geoPoint, final Point point)
  {
    // Initialisations
    boolean retour = false;

    // Recherche des items
    final List<Item> tapItems = findItemsUnder(point, getTapItemsMaxCount());
    if ((tapItems != null) && !tapItems.isEmpty())
    {
      retour = onTap(tapItems);
    }

    // Repaint
    requestRedraw();

    return retour;
  }

  @Override
  public final boolean onLongTap(final GeoPoint geoPoint, final Point point)
  {
    // Initialisations
    boolean retour = false;

    // Recherche des items
    final List<Item> tapItems = findItemsUnder(point, getTapItemsMaxCount());
    if ((tapItems != null) && !tapItems.isEmpty())
    {
      retour = onLongTap(tapItems);
    }

    // Repaint
    requestRedraw();

    return retour;
  }

  @Override
  public final boolean onDoubleTap(final GeoPoint geoPoint, final Point point)
  {
    // Initialisations
    boolean retour = false;

    // Recherche des items
    final List<Item> tapItems = findItemsUnder(point, getTapItemsMaxCount());
    if ((tapItems != null) && !tapItems.isEmpty())
    {
      retour = onDoubleTap(tapItems);
    }

    // Repaint
    requestRedraw();

    return retour;
  }

  /**
   * 
   * @return
   */
  @SuppressWarnings("static-method")
  public int getTapItemsMaxCount()
  {
    return -1;
  }

  /**
   * 
   * @param tapedItems
   * @return
   */
  public abstract boolean onTap(final List<Item> tapedItems);

  /**
   * 
   * @param tapedItems
   * @return
   */
  public abstract boolean onLongTap(final List<Item> tapedItems);

  /**
   * 
   * @param tapedItems
   * @return
   */
  public abstract boolean onDoubleTap(final List<Item> tapedItems);
}
