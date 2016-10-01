package org.pedro.map;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.pedro.utils.ReadManyWriteSingleLock;
import org.pedro.utils.ThreadUtils;

/**
 * 
 * @author pedro.m
 *
 * @param <ImageData>
 * @param <Drawer>
 * @param <ColorData>
 */
public final class TileManager<ImageData, Drawer, ColorData>
{
  private static final String                           FILE_PROTOCOL                   = "file:";

  private static final String                           BACKGROUND_CHECKERED_LIGHT_GRAY = "org/pedro/map/tileprovider/CheckeredLightGray.png";
  private static final String                           BACKGROUND_CHECKERED_BLACK      = "org/pedro/map/tileprovider/CheckeredBlack.png";

  public static final double                            DEFAULT_CENTER_LATITUDE         = 45.307316;
  public static final double                            DEFAULT_CENTER_LONGITUDE        = 5.887016;
  public static final int                               DEFAULT_ZOOM                    = 8;

  public static final int                               DEFAULT_MAX_ZOOM                = 17;
  public static final int                               DEFAULT_MIN_ZOOM                = 0;

  MapDisplayer<ImageData, Drawer, ColorData>            mapDisplayer;
  private ManagerMapController                          controller;
  final ReadWriteLock                                   controllerLock                  = new ReadManyWriteSingleLock();
  MercatorProjection                                    projection;

  final TileCache                                       tileCache;
  private final TileMemoryCache<ImageData>              tileMemoryCache;
  TileProvider                                          tileProvider;
  final ReadWriteLock                                   tileProviderLock                = new ReadManyWriteSingleLock();

  final ExecutorService                                 executor;
  final int                                             threadPoolSize;

  PriorityQueue<DownloadTask<ImageData>>                taskQueue                       = new PriorityQueue<DownloadTask<ImageData>>();
  private PriorityQueue<DownloadTask<ImageData>>        tempTaskQueue                   = new PriorityQueue<DownloadTask<ImageData>>();
  final Object                                          taskQueueLock                   = new Object();
  final Collection<DownloadTask<ImageData>>             runningTasks                    = new HashSet<DownloadTask<ImageData>>();
  final ReadWriteLock                                   runningTasksLock                = new ReadManyWriteSingleLock();
  private final LinkedList<DownloadTask<ImageData>>     tasksPool                       = new LinkedList<DownloadTask<ImageData>>();
  private int                                           tasksPoolMaxSize;
  boolean                                               recalculateTaskPriorities       = false;

  int                                                   currentDisplayerWidth;
  int                                                   currentDisplayerHeight;

  final GeoPoint                                        center                          = new GeoPoint(DEFAULT_CENTER_LATITUDE, DEFAULT_CENTER_LONGITUDE);
  int                                                   zoom                            = DEFAULT_ZOOM;
  private final Map<Tile, DrawPoint>                    tilePlacements                  = new HashMap<Tile, DrawPoint>();
  private final Map<Tile, DrawPoint>                    tileReplacements                = new HashMap<Tile, DrawPoint>();
  boolean                                               forceAllPlacements              = true;

  private final LinkedList<Tile>                        tilesPool                       = new LinkedList<Tile>();
  private final LinkedList<DrawPoint>                   pointsPool                      = new LinkedList<DrawPoint>();

  final ImageDataTransformation<ImageData, Drawer>      transformation                  = new ImageDataTransformation<ImageData, Drawer>();
  final ReadWriteLock                                   transformationLock              = new ReadManyWriteSingleLock();

  final GraphicsHelper<ImageData, Drawer, ColorData>    graphicsHelper;

  // Buffers tuiles
  private ImageData                                     tileBuffer;
  private Drawer                                        tileDrawer;
  private ImageData                                     tempTileBuffer;
  private Drawer                                        tempTileDrawer;

  // Buffers principaux
  private final Object                                  mainBufferLock                  = new Object();
  private ImageData                                     backgroundTileBuffer;
  private Drawer                                        backgroundTileDrawer;
  private ImageData                                     mainBuffer;
  private Drawer                                        mainDrawer;
  private ImageData                                     tempMainBuffer;
  private Drawer                                        tempMainDrawer;

  private ColorData                                     backgroundColor;
  private final ColorData                               black;
  private final ColorData                               lightGray;
  protected final ColorData                             missingTileColor;
  private ImageData                                     backgroundTileImage;
  private int                                           backgroundTileImagePatternSize;
  private int                                           backgroundTileImageLeftOffset;
  private int                                           backgroundTileImageTopOffset;

  OverlaysList<ImageData, Drawer, ColorData>            overlays;
  final ReadWriteLock                                   overlaysLock                    = new ReadManyWriteSingleLock();
  private final List<Overlay<ImageData, Drawer>>        earlyOverlays                   = new ArrayList<Overlay<ImageData, Drawer>>();
  private final List<Overlay<ImageData, Drawer>>        lateOverlays                    = new ArrayList<Overlay<ImageData, Drawer>>();

  private int                                           tilesNeededX;
  private int                                           tilesNeededY;

  PointToPointMover                                     ptpMover;

  private String                                        bingApiKey;

  private String                                        cloudMadeApiKey;
  private String                                        cloudMadeToken;

  private String                                        googleApiKey;

  private String                                        ignApiKey;

  private boolean                                       isApiKeyDebug;

  private Object[]                                      tileProvidersParams;

  private final TileManagerThread<ImageData>            tileManagerThread;
  private final TileManagerMemoryCacheThread<ImageData> tileManagerMemoryCacheThread;

  private final RedrawThread<ImageData>                 redrawThread;

  /**
   * 
   * @param mapDisplayer
   * @param graphicsHelper
   * @param tileMemoryCache
   * @param tileCache
   * @param threadPoolSize
   * @throws IOException
   */
  public TileManager(final MapDisplayer<ImageData, Drawer, ColorData> mapDisplayer, final GraphicsHelper<ImageData, Drawer, ColorData> graphicsHelper, final TileMemoryCache<ImageData> tileMemoryCache, final TileCache tileCache,
      final int threadPoolSize) throws IOException
  {
    // Initialisations
    this.tileManagerThread = new TileManagerThread<ImageData>(this);
    this.tileManagerMemoryCacheThread = new TileManagerMemoryCacheThread<ImageData>(this);
    this.redrawThread = new RedrawThread<ImageData>(this);
    this.mapDisplayer = mapDisplayer;
    this.graphicsHelper = graphicsHelper;
    this.tileMemoryCache = tileMemoryCache;
    this.tileCache = tileCache;
    this.threadPoolSize = threadPoolSize;

    // Initialisation de l'afficheur
    initMapDisplayer();

    // Initialisation du pool de threads
    executor = Executors.newFixedThreadPool(threadPoolSize);

    // Initialisation des couleurs et du TileProvider
    black = graphicsHelper.newColor(0, 0, 0);
    lightGray = graphicsHelper.newColor(230, 230, 230);
    missingTileColor = graphicsHelper.newColor(200, 0, 0);
    setTileProvider(BuiltInTileProvider.BLANK_BLACK);

    // Initialisation de la projection
    initProjection();

    // Initialisation du controler
    initController();

    // Initialisation des couches
    initOverlays();
  }

  /**
   * 
   * @param bingApiKey
   */
  public void setBingApiKey(final String bingApiKey)
  {
    this.bingApiKey = bingApiKey;
    constructTileProvidersParams();
  }

  /**
   * 
   * @param apiKey
   * @param token
   */
  public void setCloudMadeKeys(final String apiKey, final String token)
  {
    this.cloudMadeApiKey = apiKey;
    this.cloudMadeToken = token;
    constructTileProvidersParams();
  }

  /**
   * 
   * @param googleApiKey
   */
  public void setGoogleApiKey(final String googleApiKey)
  {
    this.googleApiKey = googleApiKey;
    constructTileProvidersParams();
  }

  /**
   * 
   * @param ignApiKey
   */
  public void setIgnApiKey(final String ignApiKey)
  {
    this.ignApiKey = ignApiKey;
    constructTileProvidersParams();
  }

  /**
   * 
   * @param isApiKeyDebug
   */
  public void setIsApiKeyDebug(final boolean isApiKeyDebug)
  {
    this.isApiKeyDebug = isApiKeyDebug;
  }

  /**
   * 
   */
  private void constructTileProvidersParams()
  {
    tileProvidersParams = new Object[] { cloudMadeApiKey, cloudMadeToken, bingApiKey, googleApiKey, ignApiKey, Boolean.valueOf(isApiKeyDebug) };
  }

  /**
   * 
   * @return
   */
  public Object[] getTileProvidersParams()
  {
    return tileProvidersParams;
  }

  /**
   * 
   */
  private void initMapDisplayer()
  {
    currentDisplayerWidth = mapDisplayer.getPixelWidth();
    currentDisplayerHeight = mapDisplayer.getPixelHeight();
  }

  /**
   * 
   */
  private void initProjection()
  {
    projection = new MercatorProjection(mapDisplayer);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ManagerMapController extends AbstractMapController
  {
    private TileManager<?, ?, ?>    tileManager;
    private MapMover                currentMover = null;
    private final GeoPoint          oldCenter    = new GeoPoint();
    private final Object            moverLock    = new Object();
    private final PointToPointMover ptpMover;
    private final GeoPoint          topLeft      = new GeoPoint();
    private final GeoPoint          bottomRight  = new GeoPoint();
    private double                  pixelLatAngle;
    private double                  pixelLngAngle;

    /**
     * 
     * @param tileManager
     */
    ManagerMapController(final TileManager<?, ?, ?> tileManager)
    {
      this.tileManager = tileManager;

      // Point to Point Mover
      ptpMover = new PointToPointMover(this, tileManager.projection);
      ptpMover.start();
    }

    @Override
    public GeoPoint getCenter()
    {
      final Lock controllerLocker = tileManager.controllerLock.readLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        return tileManager.center;
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        controllerLocker.unlock();
      }

      return new GeoPoint(DEFAULT_CENTER_LATITUDE, DEFAULT_CENTER_LONGITUDE);
    }

    @Override
    public void setCenter(final GeoPoint newCenter)
    {
      final Lock controllerLocker = tileManager.controllerLock.writeLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        // Evenement
        fireCenterChanged(tileManager.center, newCenter);

        // Calcul de la translation
        final double oldX = MercatorProjection.longitudeToPixelX(tileManager.center.getLongitude(), tileManager.zoom);
        final double oldY = MercatorProjection.latitudeToPixelY(tileManager.center.getLatitude(), tileManager.zoom);
        final double newX = MercatorProjection.longitudeToPixelX(newCenter.getLongitude(), tileManager.zoom);
        final double newY = MercatorProjection.latitudeToPixelY(newCenter.getLatitude(), tileManager.zoom);
        final int deltaX = (int)Math.round(oldX - newX);
        final int deltaY = (int)Math.round(oldY - newY);

        // Sauvegarde du nouveau centre de la carte
        tileManager.center.copy(newCenter);

        // Gestion
        tileManager.manage(deltaX, deltaY, 0, true);
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        controllerLocker.unlock();
      }
    }

    @Override
    public void animateTo(final GeoPoint toCenter)
    {
      final Lock controllerLocker = tileManager.controllerLock.readLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        stopMove();
        ptpMover.initialize(tileManager.center, toCenter);
        setMover(ptpMover);
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        controllerLocker.unlock();
      }
    }

    @Override
    public void scrollBy(final int deltaX, final int deltaY)
    {
      final Lock controllerLocker = tileManager.controllerLock.writeLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        // Sauvegarde
        oldCenter.copy(tileManager.center);

        // Calcul du nouveau centre de la carte
        tileManager.projection.fromPixels((tileManager.currentDisplayerWidth / 2) - deltaX, (tileManager.currentDisplayerHeight / 2) - deltaY, tileManager.center);

        // Evenement
        fireCenterChanged(oldCenter, tileManager.center);

        // Gestion des tuiles
        tileManager.manage(deltaX, deltaY, 0, false);
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        controllerLocker.unlock();
      }
    }

    @Override
    public int getZoom()
    {
      final Lock controllerLocker = tileManager.controllerLock.readLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        return tileManager.zoom;
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        controllerLocker.unlock();
      }

      return DEFAULT_ZOOM;
    }

    @Override
    public int getMaxZoom()
    {
      final Lock tileProviderLocker = tileManager.tileProviderLock.readLock();
      try
      {
        // Lock
        tileProviderLocker.lockInterruptibly();

        if (tileManager.tileProvider != null)
        {
          return tileManager.tileProvider.getMaxZoomLevel();
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        tileProviderLocker.unlock();
      }

      return TileManager.DEFAULT_MAX_ZOOM;
    }

    @Override
    public int getMinZoom()
    {
      final Lock tileProviderLocker = tileManager.tileProviderLock.readLock();
      try
      {
        // Lock
        tileProviderLocker.lockInterruptibly();

        if (tileManager.tileProvider != null)
        {
          return tileManager.tileProvider.getMinZoomLevel();
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        tileProviderLocker.unlock();
      }

      return TileManager.DEFAULT_MIN_ZOOM;
    }

    @Override
    public void setZoom(final int zoom)
    {
      // Verification de la validite du zoom
      if ((zoom > getMaxZoom()) || (zoom < getMinZoom()))
      {
        return;
      }

      final Lock controllerLocker = tileManager.controllerLock.writeLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        // Evenement
        fireZoomChanged(tileManager.zoom, zoom);

        // Sauvegarde du nouveau zoom de la carte
        tileManager.zoom = zoom;

        // Gestion
        tileManager.forceAllPlacements = true;
        tileManager.manage(0, 0, 0, true);
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        controllerLocker.unlock();
      }
    }

    @Override
    public void zoomIn()
    {
      final Lock controllerLocker = tileManager.controllerLock.writeLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        final Lock tileProviderLocker = tileManager.tileProviderLock.readLock();
        try
        {
          // Lock
          tileProviderLocker.lockInterruptibly();

          if (tileManager.zoom < getMaxZoom())
          {
            // Evenement
            fireZoomChanged(tileManager.zoom, tileManager.zoom + 1);

            // Sauvegarde du nouveau zoom de la carte
            tileManager.zoom += 1;

            // Gestion des tuiles
            tileManager.manage(0, 0, 1, false);

            // Fin du déplacement
            onMoveInputFinished();
          }
        }
        finally
        {
          tileProviderLocker.unlock();
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
    }

    @Override
    public void zoomIn(final GeoPoint newCenter)
    {
      final Lock controllerLocker = tileManager.controllerLock.writeLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        final Lock tileProviderLocker = tileManager.tileProviderLock.readLock();
        try
        {
          // Lock
          tileProviderLocker.lockInterruptibly();

          // Calcul de la translation
          final double oldX = MercatorProjection.longitudeToPixelX(tileManager.center.getLongitude(), tileManager.zoom);
          final double oldY = MercatorProjection.latitudeToPixelY(tileManager.center.getLatitude(), tileManager.zoom);
          final double newX = MercatorProjection.longitudeToPixelX(newCenter.getLongitude(), tileManager.zoom);
          final double newY = MercatorProjection.latitudeToPixelY(newCenter.getLatitude(), tileManager.zoom);
          final int deltaX = (int)Math.round(oldX - newX);
          final int deltaY = (int)Math.round(oldY - newY);

          // Gestion du zoom
          final int deltaZoom = (tileManager.zoom < getMaxZoom() ? 1 : 0);
          if (deltaZoom > 0)
          {
            // Evenements
            fireZoomChanged(tileManager.zoom, tileManager.zoom + deltaZoom);

            // Sauvegarde du nouveau zoom de la carte
            tileManager.zoom += deltaZoom;
          }

          // Evenement
          fireCenterChanged(tileManager.center, newCenter);

          // Sauvegarde du nouveau centre de la carte
          tileManager.center.copy(newCenter);

          // Gestion des tuiles
          tileManager.manage(deltaX, deltaY, deltaZoom, false);

          // Fin du déplacement
          onMoveInputFinished();
        }
        finally
        {
          tileProviderLocker.unlock();
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
    }

    @Override
    public void zoomOut()
    {
      final Lock controllerLocker = tileManager.controllerLock.writeLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        final Lock tileProviderLocker = tileManager.tileProviderLock.readLock();
        try
        {
          tileProviderLocker.lockInterruptibly();
          if (tileManager.zoom > getMinZoom())
          {
            // Evenement
            fireZoomChanged(tileManager.zoom, tileManager.zoom - 1);

            // Sauvegarde du nouveau zoom de la carte
            tileManager.zoom -= 1;

            // Gestion des tuiles
            tileManager.manage(0, 0, -1, false);

            // Fin du déplacement
            onMoveInputFinished();
          }
        }
        finally
        {
          tileProviderLocker.unlock();
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
    }

    @Override
    public void postIntermediateZoom(final boolean doZoom, final float inZoom, final int focusX, final int focusY)
    {
      final Lock transformationLocker = tileManager.transformationLock.writeLock();
      try
      {
        // Lock
        transformationLocker.lockInterruptibly();

        if (doZoom)
        {
          tileManager.transformation.postIntermediateZoom(inZoom, tileManager.mapDisplayer.getPixelWidth() / 2, tileManager.mapDisplayer.getPixelHeight() / 2);
        }
        else
        {
          tileManager.transformation.resetIntermediateZoom();
        }
        final Lock overlaysLocker = tileManager.overlaysLock.readLock();
        try
        {
          // Lock
          overlaysLocker.lockInterruptibly();

          for (final Overlay<?, ?> overlay : tileManager.overlays)
          {
            if (doZoom)
            {
              overlay.transformationPostIntermediateZoom(inZoom, tileManager.mapDisplayer.getPixelWidth() / 2, tileManager.mapDisplayer.getPixelHeight() / 2, tileManager.center, tileManager.zoom);
            }
            else
            {
              overlay.transformationResetIntermediateZoom(tileManager.center, tileManager.zoom);
            }
          }
        }
        finally
        {
          overlaysLocker.unlock();
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        transformationLocker.unlock();
      }

      tileManager.mapDisplayer.redraw();
    }

    @Override
    public void postZoom(final int deltaZoom, final int centerX, final int centerY)
    {
      // Bornes du zoom
      int finalZoom = getZoom() + deltaZoom;
      if (finalZoom > getMaxZoom())
      {
        finalZoom = getMaxZoom();
      }
      if (finalZoom < getMinZoom())
      {
        finalZoom = getMinZoom();
      }
      final int finalDeltaZoom = finalZoom - tileManager.zoom;

      // Gestion
      final Lock controllerLocker = tileManager.controllerLock.writeLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        // Evenement
        fireZoomChanged(tileManager.zoom, finalZoom);

        // Sauvegarde du nouveau zoom de la carte
        tileManager.zoom = finalZoom;

        // Gestion des tuiles
        tileManager.manage(0, 0, finalDeltaZoom, false);
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        controllerLocker.unlock();
      }
    }

    @Override
    public void setMover(final MapMover mover)
    {
      synchronized (moverLock)
      {
        currentMover = mover;
        currentMover.startMove();
      }
    }

    @Override
    public void stopMove()
    {
      synchronized (moverLock)
      {
        if (currentMover != null)
        {
          currentMover.stopMove();
          currentMover = null;
        }
      }
    }

    /**
     * 
     */
    public void shutdown()
    {
      final Lock controllerLocker = tileManager.controllerLock.writeLock();
      try
      {
        // Lock
        controllerLocker.lockInterruptibly();

        // Fin du mover
        ptpMover.shutdown();
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        controllerLocker.unlock();
      }

      // Divers
      tileManager = null;
    }

    @Override
    public Lock getReadLock()
    {
      return tileManager.controllerLock.readLock();
    }

    /**
     * 
     */
    private void updatePixelAngles()
    {
      // Calcul du delta angulaire d'1 pixel (approximation sur le centre de la carte)
      final double pixXCentre = MercatorProjection.longitudeToPixelX(tileManager.center.getLongitude(), tileManager.zoom);
      final double xUn = MercatorProjection.pixelXToLongitude(pixXCentre + 1, tileManager.zoom);
      final double pixYCentre = MercatorProjection.latitudeToPixelY(tileManager.center.getLatitude(), tileManager.zoom);
      final double yUn = MercatorProjection.pixelYToLatitude(pixYCentre + 1, tileManager.zoom);
      pixelLatAngle = Math.abs(tileManager.center.getLatitude() - yUn);
      pixelLngAngle = Math.abs(xUn - tileManager.center.getLongitude());
    }

    @Override
    public void onMoveInputFinished()
    {
      try
      {
        // Calcul des coordonnees des coins
        tileManager.projection.fromPixels(0, 0, topLeft);
        tileManager.projection.fromPixels(tileManager.mapDisplayer.getPixelWidth(), tileManager.mapDisplayer.getPixelHeight(), bottomRight);

        // Calcul des delta angle pixel
        updatePixelAngles();

        // Notification aux overlays
        for (final Overlay<?, ?> overlay : tileManager.getOverlays(false))
        {
          overlay.onMoveInputFinished(tileManager.center, topLeft, bottomRight, pixelLatAngle, pixelLngAngle);
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        tileManager.unlockReadOverlays();
      }
    }
  }

  /**
   * 
   */
  private void initController()
  {
    controller = new ManagerMapController(this);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class OverlaysList<ImageData, Drawer, ColorData> extends ArrayList<Overlay<ImageData, Drawer>>
  {
    private static final long                          serialVersionUID = 1L;

    private MapDisplayer<ImageData, Drawer, ColorData> mapDisplayer;

    /**
     * 
     * @param mapDisplayer
     */
    OverlaysList(final MapDisplayer<ImageData, Drawer, ColorData> mapDisplayer)
    {
      this.mapDisplayer = mapDisplayer;
    }

    @Override
    public void clear()
    {
      for (int i = size() - 1; i >= 0; --i)
      {
        get(i).onShutdown();
      }
      super.clear();
      mapDisplayer.redraw();
    }

    @Override
    public Overlay<ImageData, Drawer> remove(final int index)
    {
      final Overlay<ImageData, Drawer> overlay = super.remove(index);
      overlay.onShutdown();
      mapDisplayer.redraw();

      return overlay;
    }

    @Override
    public boolean remove(final Object object)
    {
      ((Overlay<?, ?>)object).onShutdown();
      mapDisplayer.redraw();

      return super.remove(object);
    }

    @Override
    public boolean removeAll(final Collection<?> collection)
    {
      for (final Object object : collection)
      {
        ((Overlay<?, ?>)object).onShutdown();
      }
      mapDisplayer.redraw();

      return super.removeAll(collection);
    }

    @Override
    public Overlay<ImageData, Drawer> set(final int index, final Overlay<ImageData, Drawer> overlay)
    {
      get(index).onShutdown();
      final Overlay<ImageData, Drawer> retour = super.set(index, overlay);
      mapDisplayer.redraw();

      return retour;
    }

    /**
     * 
     */
    protected void onShutdown()
    {
      // Divers
      mapDisplayer = null;
    }
  }

  /**
   * 
   */
  private void initOverlays()
  {
    final Lock overlaysLocker = overlaysLock.writeLock();
    try
    {
      overlaysLocker.lockInterruptibly();
      overlays = new OverlaysList<ImageData, Drawer, ColorData>(mapDisplayer);
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      overlaysLocker.unlock();
    }
  }

  /**
   * 
   */
  private void calculateNumberOfTilesNeeded()
  {
    tilesNeededX = getNumberOfTilesNecessary(currentDisplayerWidth);
    tilesNeededY = getNumberOfTilesNecessary(currentDisplayerHeight);
  }

  /**
   * 
   * @param pixelSize
   * @return
   */
  private static int getNumberOfTilesNecessary(final int pixelSize)
  {
    return (int)(Math.ceil((float)pixelSize / TileProvider.TILE_SIZE)) + 1;
  }

  /**
   * 
   */
  private void adjustBufferSizes()
  {
    // Liberation memoire
    freeWorkingImageData();

    // Buffers tuiles
    createTileBuffers(tileProvider == null);

    // Buffer tuile de fond
    onBackgroundTileImageChanged();

    // Buffers principaux
    createMainBuffers();
  }

  /**
   * 
   */
  private void createTileBuffers(final boolean asNull)
  {
    if (!asNull)
    {
      // Principal
      tileBuffer = graphicsHelper.newAlphaImageData(currentDisplayerWidth, currentDisplayerHeight);
      graphicsHelper.setTransparent(tileBuffer);
      tileDrawer = graphicsHelper.getDrawer(tileBuffer);

      // Secondaire
      tempTileBuffer = graphicsHelper.newAlphaImageData(currentDisplayerWidth, currentDisplayerHeight);
      graphicsHelper.setTransparent(tempTileBuffer);
      tempTileDrawer = graphicsHelper.getDrawer(tempTileBuffer);
    }
    else
    {
      // Principal
      tileBuffer = null;
      tileDrawer = null;

      // Secondaire
      tempTileBuffer = null;
      tempTileDrawer = null;
    }
  }

  /**
   * 
   */
  private void createMainBuffers()
  {
    // Principal
    mainBuffer = graphicsHelper.newAlphaImageData(currentDisplayerWidth, currentDisplayerHeight);
    mainDrawer = graphicsHelper.getDrawer(mainBuffer);

    // Secondaire
    tempMainBuffer = graphicsHelper.newAlphaImageData(currentDisplayerWidth, currentDisplayerHeight);
    tempMainDrawer = graphicsHelper.getDrawer(tempMainBuffer);
  }

  /**
   * 
   */
  private void adjustMemoryCache()
  {
    tileMemoryCache.adjust((tilesNeededX + 2) * (tilesNeededY + 2));
  }

  /**
   * 
   * @param resource
   * @param width
   * @param height
   * @return
   * @throws IOException
   */
  private ImageData getLocalImageData(final String resource, final int width, final int height) throws IOException
  {
    // URL dans le classpath ?
    URL url = getClass().getClassLoader().getResource(resource);

    // Non : fichier ?
    if (url == null)
    {
      url = new URL(FILE_PROTOCOL + resource);
    }

    return graphicsHelper.getTileImageData(url, width, height);
  }

  /**
   * 
   * @param builtInTileProvider
   * @return
   */
  public void setTileProvider(final BuiltInTileProvider builtInTileProvider) throws IOException
  {
    // Initialisations
    ColorData bgColor = null;
    ImageData bgTileImage = null;
    int bgTileImagePatternSize = TileProvider.TILE_SIZE;

    // Selon le cas
    switch (builtInTileProvider)
    {
      case OSM_MAPNIK:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case OSM_OCM:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      /*
      case OSM_TAH:
      bgColor = lightGray;
      bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
      bgTileImagePatternSize = 32;
      break;
      */
      case IGN_SATELLITE:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case IGN_MAP:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case GOOGLE_HYBRID:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case GOOGLE_ROADMAP:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case GOOGLE_SATELLITE:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case GOOGLE_TERRAIN:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case HIKING_EUROPE:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case MRI_RELIEF:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case HIKE_BIKE_EUROPE:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      /*
      case CLOUDMADE_ORIGINAL:
      bgColor = lightGray;
      bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
      bgTileImagePatternSize = 32;
      break;
      case CLOUDMADE_FINE_LINE:
      bgColor = lightGray;
      bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
      bgTileImagePatternSize = 32;
      break;
      case CLOUDMADE_FRESH:
      bgColor = lightGray;
      bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
      bgTileImagePatternSize = 32;
      break;
      */
      case MAPQUEST_OSM:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case MAPQUEST_OPEN_AERIAL:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case MAPBOX:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case BING_ROAD:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case BING_AERIAL:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case BING_AERIAL_LABELS:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case BLANK_BLACK:
        bgColor = black;
        break;
      case BLANK_LIGHT_GRAY:
        bgColor = lightGray;
        break;
      case CHECKERED_BLACK:
        bgColor = black;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_BLACK, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
      case CHECKERED_LIGHT_GRAY:
        bgColor = lightGray;
        bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
        bgTileImagePatternSize = 32;
        break;
    }

    // Provider
    final TileProvider provider = builtInTileProvider.getNewTileProvider(cloudMadeApiKey, cloudMadeToken, bingApiKey, googleApiKey, ignApiKey, Boolean.valueOf(isApiKeyDebug));

    // Reglage
    setTileProvider(provider, bgColor, bgTileImage, bgTileImagePatternSize);
  }

  /**
   * 
   * @param writeLock
   * @return
   * @throws InterruptedException
   */
  public List<Overlay<ImageData, Drawer>> getOverlays(final boolean writeLock) throws InterruptedException
  {
    final Lock overlaysLocker = (writeLock ? overlaysLock.writeLock() : overlaysLock.readLock());
    overlaysLocker.lockInterruptibly();
    return overlays;
  }

  /**
   * 
   */
  public void unlockReadOverlays()
  {
    overlaysLock.readLock().unlock();
  }

  /**
   * 
   */
  public void unlockWriteOverlays()
  {
    overlaysLock.writeLock().unlock();
  }

  /**
   * 
   */
  public void onMapDisplayerSizeChanged()
  {
    // Sauvegarde de la taille courante
    currentDisplayerWidth = mapDisplayer.getPixelWidth();
    currentDisplayerHeight = mapDisplayer.getPixelHeight();

    // Gestion des tuiles
    calculateNumberOfTilesNeeded();

    // Gestion des buffers
    adjustBufferSizes();

    // Gestion du cache memoire
    adjustMemoryCache();

    // Gestion du cache des DownloadTasks
    adjustTasksPool();

    // Ajustement des couches
    final Lock overlaysLocker = overlaysLock.readLock();
    try
    {
      // Lock
      overlaysLocker.lockInterruptibly();

      // Ajustement des overlays
      for (final Overlay<?, ?> overlay : overlays)
      {
        overlay.onMapDisplayerSizeChanged();
      }

      // Fin du "mouvement"
      controller.onMoveInputFinished();
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      overlaysLocker.unlock();
    }

    // Gestion
    resetManage();
    forceAllPlacements = true;

    // Calculs
    final Lock controllerLocker = controllerLock.writeLock();
    try
    {
      // Lock
      controllerLocker.lockInterruptibly();

      // Calculs
      manage(0, 0, 0, false);
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      controllerLocker.unlock();
    }
  }

  /**
   * 
   */
  private void freeAllImageData()
  {
    // Les buffers de travail
    freeWorkingImageData();

    // Le buffer de fond
    graphicsHelper.freeImageData(backgroundTileImage);
    backgroundTileImage = null;
  }

  /**
   * 
   */
  private void freeWorkingImageData()
  {
    // Tuiles
    freeTileProviderImageData();

    // Fond des tuiles
    graphicsHelper.freeImageData(backgroundTileBuffer);
    backgroundTileBuffer = null;

    // Buffers principaux
    graphicsHelper.freeImageData(mainBuffer);
    mainBuffer = null;
    graphicsHelper.freeImageData(tempMainBuffer);
    tempMainBuffer = null;
  }

  /**
   * 
   */
  private void freeTileProviderImageData()
  {
    // Liberations
    graphicsHelper.freeImageData(tileBuffer);
    tileBuffer = null;
    graphicsHelper.freeImageData(tempTileBuffer);
    tempTileBuffer = null;
  }

  /**
   * 
   */
  private void shutdownExecutor()
  {
    executor.shutdownNow();
    try
    {
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * 
   */
  public void shutdown()
  {
    // Le thread de dessin
    redrawThread.interrupt();
    ThreadUtils.join(redrawThread);

    // Arret des telechargements
    stopAllDownloadTasks(true);

    // Le controller
    controller.shutdown();

    // Le gestionnaire de threads de telechargement
    shutdownExecutor();

    // Le Thread principal du gestionnaire
    tileManagerThread.interrupt();
    ThreadUtils.join(tileManagerThread);

    // Le Thread de cahce memoire du gestionnaire
    tileManagerMemoryCacheThread.interrupt();
    ThreadUtils.join(tileManagerMemoryCacheThread);

    // Les couches
    final Lock overlaysLocker = overlaysLock.writeLock();
    try
    {
      overlaysLocker.lockInterruptibly();
      overlays.clear();
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      overlaysLocker.unlock();
    }

    // Liberation du cache
    tileMemoryCache.shutdown();

    // Liberation des resources graphiques
    freeAllImageData();

    // Divers
    projection.onShutdown();
    overlays.onShutdown();
    mapDisplayer = null;
  }

  /**
   * 
   */
  private void onBackgroundTileImageChanged()
  {
    // Liberation memoire
    graphicsHelper.freeImageData(backgroundTileBuffer);
    backgroundTileBuffer = null;

    // Buffer tuile de fond
    if (backgroundTileImage != null)
    {
      backgroundTileBuffer = graphicsHelper.newAlphaImageData(currentDisplayerWidth + backgroundTileImagePatternSize, currentDisplayerHeight + backgroundTileImagePatternSize);
      backgroundTileDrawer = graphicsHelper.getDrawer(backgroundTileBuffer);

      for (int top = 0; top < currentDisplayerHeight + backgroundTileImagePatternSize; top += TileProvider.TILE_SIZE)
      {
        for (int left = 0; left < currentDisplayerWidth + backgroundTileImagePatternSize; left += TileProvider.TILE_SIZE)
        {
          graphicsHelper.drawImageData(backgroundTileDrawer, backgroundTileImage, left, top);
        }
      }
    }
    else
    {
      backgroundTileBuffer = null;
      backgroundTileDrawer = null;
    }
  }

  /**
   * 
   * @param tileProvider
   */
  public void setTileProvider(final TileProvider tileProvider)
  {
    ImageData bgTileImage = null;
    try
    {
      bgTileImage = getLocalImageData(BACKGROUND_CHECKERED_LIGHT_GRAY, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
    }
    catch (final IOException ioe)
    {
      ioe.printStackTrace(System.err);
    }
    setTileProvider(tileProvider, lightGray, bgTileImage, 32);
  }

  /**
   * 
   * @param tileProvider
   * @param bgColor
   * @param bgTileImage
   * @param bgTileImagePatternSize
   */
  private void setTileProvider(final TileProvider tileProvider, final ColorData bgColor, final ImageData bgTileImage, final int bgTileImagePatternSize)
  {
    final Lock controllerLocker = controllerLock.writeLock();
    try
    {
      // Lock
      controllerLocker.lockInterruptibly();

      final Lock tileProviderLocker = tileProviderLock.writeLock();
      try
      {
        // Lock
        tileProviderLocker.lockInterruptibly();

        // Liberation memoire
        graphicsHelper.freeImageData(backgroundTileImage);
        backgroundTileImage = null;

        // Fournisseur de tuiles et autres infos
        final TileProvider oldTileProvider = this.tileProvider;
        this.tileProvider = tileProvider;
        this.backgroundColor = bgColor;
        this.backgroundTileImage = bgTileImage;
        this.backgroundTileImagePatternSize = bgTileImagePatternSize;

        if ((currentDisplayerWidth > 0) && (currentDisplayerHeight > 0))
        {
          // Creation buffers si necessaire
          if ((oldTileProvider == null) && (tileProvider != null))
          {
            createTileBuffers(false);
          }
          // Liberation buffers si possible
          else if (tileProvider == null)
          {
            freeTileProviderImageData();
          }
          // Effacement sinon
          else
          {
            // Dessin
            graphicsHelper.setTransparent(tileBuffer);
          }

          // Gestion du l'image de fond
          onBackgroundTileImageChanged();

          // RAZ
          resetManage();

          // RAZ du cache memoire
          tileMemoryCache.clear();

          // Gestion
          forceAllPlacements = true;
          manage(0, 0, 0, false);
        }
      }
      finally
      {
        tileProviderLocker.unlock();
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
  }

  /**
   * 
   * @author pedro.m
   */
  private static class RedrawThread<DMImageData> extends Thread
  {
    private TileManager<DMImageData, ?, ?> tileManager;
    private int                            nbDemandes = 0;

    /**
     * 
     * @param tileManager
     */
    RedrawThread(final TileManager<DMImageData, ?, ?> tileManager)
    {
      super(RedrawThread.class.getName());
      this.tileManager = tileManager;
    }

    @Override
    public void run()
    {
      // Tant que le thread n'est pas interrompu
      while (!isInterrupted())
      {
        // Tant que la Queue est vide
        synchronized (this)
        {
          while (!isInterrupted() && (nbDemandes == 0))
          {
            try
            {
              wait();
            }
            catch (final InterruptedException ie)
            {
              Thread.currentThread().interrupt();
              return;
            }
          }
        }

        // Si non interrompu
        if (!isInterrupted())
        {
          tileManager.doDraw();
          nbDemandes = 0;
        }

        if (!isInterrupted())
        {
          tileManager.mapDisplayer.invalidateDisplayer();
        }
      }

      // Divers
      tileManager = null;
    }

    /**
     * 
     */
    void post()
    {
      synchronized (this)
      {
        nbDemandes++;
        notify();
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class DrawPoint extends Point
  {
    boolean drawn;

    /**
     * 
     * @param x
     * @param y
     */
    DrawPoint(final int x, final int y)
    {
      super(x, y);
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class TileManagerMemoryCacheThread<DMImageData> extends Thread
  {
    private TileManager<DMImageData, ?, ?>              tileManager;
    private final Collection<DownloadTask<DMImageData>> memoryTasks     = new HashSet<DownloadTask<DMImageData>>();
    private final Collection<DownloadTask<DMImageData>> memoryTasksCopy = new HashSet<DownloadTask<DMImageData>>();

    /**
     * 
     * @param tileManager
     */
    TileManagerMemoryCacheThread(final TileManager<DMImageData, ?, ?> tileManager)
    {
      super(TileManagerMemoryCacheThread.class.getName());
      this.tileManager = tileManager;
    }

    /**
     * 
     * @param task
     */
    void postMemoryTask(final DownloadTask<DMImageData> task)
    {
      synchronized (memoryTasks)
      {
        if (memoryTasks.contains(task))
        {
          // Tache deja en memoire => recyclage
          tileManager.recycleDownloadTask(task);
        }
        else
        {
          // Ajout
          memoryTasks.add(task);
        }
      }
    }

    @Override
    public void run()
    {
      // Tant que le thread n'est pas interrompu
      while (!isInterrupted())
      {
        // Tant que la Queue est vide
        synchronized (this)
        {
          while (!isInterrupted() && (tileManager != null) && memoryTasks.isEmpty())
          {
            try
            {
              wait();
            }
            catch (final InterruptedException ie)
            {
              Thread.currentThread().interrupt();
              return;
            }
          }
        }

        // Si non interrompu
        if (!isInterrupted())
        {
          synchronized (memoryTasks)
          {
            memoryTasksCopy.addAll(memoryTasks);
            memoryTasks.clear();
          }
          for (final DownloadTask<DMImageData> task : memoryTasksCopy)
          {
            tileManager.onTileDataAvailable(task.tile, task.memoryData, false, false);
            task.memoryData = null;
            tileManager.recycleDownloadTask(task);
          }
          tileManager.redraw();
          memoryTasksCopy.clear();
        }
      }

      // Divers
      tileManager = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class TileManagerThread<DMImageData> extends Thread
  {
    private TileManager<DMImageData, ?, ?> tileManager;

    /**
     * 
     * @param tileManager
     */
    TileManagerThread(final TileManager<DMImageData, ?, ?> tileManager)
    {
      super(TileManagerThread.class.getName());
      this.tileManager = tileManager;
    }

    @Override
    public void run()
    {
      // Tant que le thread n'est pas interrompu
      while (!isInterrupted())
      {
        // Tant que la Queue est vide
        synchronized (this)
        {
          while (!isInterrupted() && (tileManager != null) && (tileManager.taskQueue.isEmpty() || (tileManager.runningTasks.size() >= tileManager.threadPoolSize)))
          {
            try
            {
              wait();
            }
            catch (final InterruptedException ie)
            {
              Thread.currentThread().interrupt();
              return;
            }
          }
        }

        // Si non interrompu
        if (!isInterrupted())
        {
          final Lock controllerLocker = tileManager.controllerLock.readLock();
          try
          {
            // Lock
            controllerLocker.lockInterruptibly();

            synchronized (tileManager.taskQueueLock)
            {
              // Recalcul des priorites
              if (tileManager.recalculateTaskPriorities)
              {
                tileManager.calculateTaskPriorities();
                tileManager.recalculateTaskPriorities = false;
              }

              // Recuperation de la tache prioritaire
              final DownloadTask<DMImageData> task = tileManager.taskQueue.poll();
              if (task != null)
              {
                final Lock runningTasksLocker = tileManager.runningTasksLock.writeLock();
                try
                {
                  // Lock
                  runningTasksLocker.lockInterruptibly();

                  // Tourne-t-elle deja ?
                  if (!tileManager.runningTasks.contains(task))
                  {
                    // La tache ne tourne pas
                    // Est-ce que le pool est sature ?
                    if (tileManager.runningTasks.size() < tileManager.threadPoolSize)
                    {
                      // Pool non sature => on lance la tache
                      tileManager.executor.execute(task);
                      tileManager.runningTasks.add(task);
                    }
                    else
                    {
                      // Le pool est sature, on ne lance rien, et on remet la tache dans la Queue
                      tileManager.taskQueue.offer(task);
                    }
                  }
                }
                finally
                {
                  runningTasksLocker.unlock();
                }
              }
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
        }
      }

      // Divers
      tileManager = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class DownloadTask<DTImageData> implements Runnable, Comparable<DownloadTask<DTImageData>>
  {
    private static final int               IMAGE_FILE_BUFFER_MAX_SIZE = 80 * 1024;

    private TileManager<DTImageData, ?, ?> tileManager;
    final Tile                             tile                       = new Tile();
    DTImageData                            memoryData                 = null;
    TileProvider                           taskTileProvider;
    String                                 taskTileProviderKey;
    int                                    priority;

    boolean                                stopped                    = false;
    final Object                           stoppedLock                = new Object();

    private final byte[]                   buffer                     = new byte[IMAGE_FILE_BUFFER_MAX_SIZE];

    /**
     * 
     * @param tileManager
     */
    DownloadTask(final TileManager<DTImageData, ?, ?> tileManager)
    {
      super();
      this.tileManager = tileManager;
    }

    /**
     * 
     * @param inTile
     * @param inTileProvider
     */
    void start(final Tile inTile, final TileProvider inTileProvider)
    {
      synchronized (stoppedLock)
      {
        this.tile.copy(inTile);
        this.taskTileProvider = inTileProvider;
        this.taskTileProviderKey = inTileProvider.getKey();
        this.stopped = false;
      }
    }

    /**
     * 
     */
    void stop()
    {
      synchronized (stoppedLock)
      {
        stopped = true;
      }
    }

    @Override
    public void run()
    {
      // Initialisations
      DTImageData data = null;
      IOException exception = null;

      try
      {
        synchronized (stoppedLock)
        {
          if (!stopped)
          {
            if (taskTileProvider.hasTile(tile))
            {
              // D'abord dans le cache disque
              if (taskTileProvider.needsCache(tile) && (tileManager.tileCache != null) && tileManager.tileCache.isAvailable())
              {
                // Recuperation depuis le cache disque
                final int length = tileManager.tileCache.retrieveTileFromCache(taskTileProvider.getCacheKey(tile), tile, buffer);
                if (length > 0)
                {
                  data = tileManager.graphicsHelper.getTileImageData(buffer, length);
                }
              }

              // Sinon depuis la source
              if (data == null)
              {
                // Lecture depuis la source
                final int length = taskTileProvider.readData(tile, buffer);

                if (length > 0)
                {
                  // Conversion en image
                  data = tileManager.graphicsHelper.getTileImageData(buffer, length);

                  // Sauvegarde dans le cache
                  if (taskTileProvider.needsCache(tile) && (tileManager.tileCache != null) && (tileManager.tileCache.isAvailable()))
                  {
                    try
                    {
                      tileManager.tileCache.storeToCache(taskTileProvider.getCacheKey(tile), tile, buffer, length);
                    }
                    catch (final IOException ioe)
                    {
                      // Pas de plantage (la tuile est dispo, c'est juste qu'on ne peut pas la sauver dans le cache !)
                      ioe.printStackTrace(System.err);
                    }
                  }
                }
              }
            }
          }
        }
      }
      catch (final IOException ioe)
      {
        exception = ioe;
      }
      finally
      {
        // Utilisation
        tileManager.downloadTaskEnd(this, data, exception);
      }
    }

    /**
     * 
     */
    protected void onShutdown()
    {
      // Divers
      tileManager = null;
    }

    @Override
    public String toString()
    {
      return taskTileProviderKey + '.' + tile.toString();
    }

    @Override
    public boolean equals(final Object object)
    {
      if (this == object)
      {
        return true;
      }

      if (object == null)
      {
        return false;
      }

      if (!DownloadTask.class.isAssignableFrom(object.getClass()))
      {
        return false;
      }

      final DownloadTask<?> other = (DownloadTask<?>)object;

      return taskTileProviderKey.equals(other.taskTileProviderKey) && tile.equals(other.tile);
    }

    @Override
    public int hashCode()
    {
      return tile.hashCode();
    }

    @Override
    public int compareTo(final DownloadTask<DTImageData> downloadTask)
    {
      return priority - downloadTask.priority;
    }
  }

  /**
   * 
   * @param tile
   * @param data
   * @param ioe
   */
  void downloadTaskEnd(final DownloadTask<ImageData> task, final ImageData data, final IOException ioe)
  {
    // Verification que le provider n'a pas change entretemps
    final Lock tileProviderLocker = tileProviderLock.readLock();
    try
    {
      // Lock
      tileProviderLocker.lockInterruptibly();

      synchronized (task.stoppedLock)
      {
        if (!task.stopped)
        {
          // TileProvider inchange ?
          if ((tileProvider != null) && tileProvider.getKey().equals(task.taskTileProviderKey))
          {
            // Notification de fin de tache
            if ((data != null) && (ioe == null))
            {
              // Tuile disponible
              onTileDataAvailable(task.tile, data, true, true);
            }
            else if (ioe == null)
            {
              // Tuile non disponible
              onTileDataNotAvailable(task.tile);
            }
            else
            {
              // Erreur pour la tuile
              onTileDataError(task.tile, ioe);
            }
          }
        }
      }
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      tileProviderLocker.unlock();
    }

    // Retrait de la liste
    final Lock runningTasksLocker = runningTasksLock.writeLock();
    try
    {
      // Lock
      runningTasksLocker.lockInterruptibly();

      runningTasks.remove(task);
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      runningTasksLocker.unlock();
    }

    // Notification pour eventuellement lancer une autre tache
    synchronized (tileManagerThread)
    {
      tileManagerThread.notify();
    }

    // Remise dans le pool
    recycleDownloadTask(task);
  }

  /**
   * 
   * @param task
   * @param centerTileX
   * @param centerTileY
   * @return
   */
  private DownloadTask<ImageData> calculateTaskPriority(final DownloadTask<ImageData> task, final long centerTileX, final long centerTileY)
  {
    if (!task.taskTileProviderKey.equals(tileProvider.getKey()))
    {
      //task.priority = 100000 * Math.abs(task.tile.getZoom() - mapDisplayer.getZoom()); // Piorite "infinie"
      return null; // On annule le job
    }

    if (task.tile.zoom != zoom)
    {
      //task.priority = 1000 * Math.abs(task.tile.getZoom() - mapDisplayer.getZoom()); // Piorite "infinie"
      return null; // On annule le job
    }

    // Tuile dans le cache disque => tache prioritaire
    if ((tileCache != null) && tileCache.isTileAvailable(task.taskTileProvider.getCacheKey(task.tile), task.tile))
    {
      task.priority = 0;
      return task;
    }

    // Sinon calcul selon la distance
    final long deltaX = centerTileX - task.tile.x;
    final long deltaY = centerTileY - task.tile.y;
    task.priority = (int)Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    return task;
  }

  /**
   * 
   */
  void calculateTaskPriorities()
  {
    // Tuile du centre
    final long centerTileX = MercatorProjection.longitudeToTileX(center.getLongitude(), zoom, TileProvider.TILE_SIZE);
    final long centerTileY = MercatorProjection.latitudeToTileY(center.getLatitude(), zoom, TileProvider.TILE_SIZE);

    // Pour chaque job : recalcul de la priorite
    while (!taskQueue.isEmpty())
    {
      final DownloadTask<ImageData> task = calculateTaskPriority(taskQueue.poll(), centerTileX, centerTileY);
      if (task != null)
      {
        tempTaskQueue.offer(task);
      }
    }

    // Swap
    final PriorityQueue<DownloadTask<ImageData>> swapTaskQueue = taskQueue; // Pour se resservir de la Queue (vide maintenant) au prochain coup
    taskQueue = tempTaskQueue;
    tempTaskQueue = swapTaskQueue;
  }

  /**
   * 
   * @param deltaX
   * @param deltaY
   * @param deltaZoom
   */
  private void calculateTilePlacements(final int deltaX, final int deltaY, final int deltaZoom)
  {
    // Tuile du centre
    final double centerLongitude = center.getLongitude();
    final double centerLatitude = center.getLatitude();
    final long displayerPixelX = Math.round(MercatorProjection.longitudeToPixelX(centerLongitude, zoom) - (currentDisplayerWidth / 2));
    final long displayerPixelY = Math.round(MercatorProjection.latitudeToPixelY(centerLatitude, zoom) - (currentDisplayerHeight / 2));

    // Decalage du fond
    if (displayerPixelX >= 0)
    {
      backgroundTileImageLeftOffset = -(int)(displayerPixelX % backgroundTileImagePatternSize);
    }
    else
    {
      backgroundTileImageLeftOffset = -backgroundTileImagePatternSize - (int)(displayerPixelX % -backgroundTileImagePatternSize);
    }
    if (displayerPixelY >= 0)
    {
      backgroundTileImageTopOffset = -(int)(displayerPixelY % backgroundTileImagePatternSize);
    }
    else
    {
      backgroundTileImageTopOffset = -backgroundTileImagePatternSize - (int)(displayerPixelY % -backgroundTileImagePatternSize);
    }

    // Les tuiles
    if (tileProvider != null)
    {
      // Tuile de depart
      final long firstTileX = MercatorProjection.pixelXToTileX(displayerPixelX, zoom, TileProvider.TILE_SIZE);
      final long firstTileY = MercatorProjection.pixelYToTileY(displayerPixelY, zoom, TileProvider.TILE_SIZE);

      // Tuile de fin
      final long lastTileX = MercatorProjection.pixelXToTileX(displayerPixelX + currentDisplayerWidth - 1, zoom, TileProvider.TILE_SIZE);
      final long lastTileY = MercatorProjection.pixelYToTileY(displayerPixelY + currentDisplayerHeight - 1, zoom, TileProvider.TILE_SIZE);

      // Remise des re-points dans le pool et vidage des re-placements
      for (final Map.Entry<Tile, DrawPoint> entry : tileReplacements.entrySet())
      {
        final Tile tile = entry.getKey();
        final DrawPoint point = entry.getValue();
        recycleTile(tile);
        recyclePoint(point);
      }
      tileReplacements.clear();

      // Remise des points dans leur pool et vidage des placements
      for (final Map.Entry<Tile, DrawPoint> entry : tilePlacements.entrySet())
      {
        final Tile tile = entry.getKey();
        final DrawPoint point = entry.getValue();
        if (point.drawn)
        {
          recycleTile(tile);
          recyclePoint(point);
        }
        else
        {
          tileReplacements.put(tile, point);
        }
      }
      tilePlacements.clear();

      // Toutes les tuiles
      final boolean doAll = forceAllPlacements || (deltaZoom != 0);
      for (long y = firstTileY; y <= lastTileY; y++)
      {
        final int top = (int)(y * TileProvider.TILE_SIZE - displayerPixelY);
        for (long x = firstTileX; x <= lastTileX; x++)
        {
          final int left = (int)(x * TileProvider.TILE_SIZE - displayerPixelX);
          final Tile tile = getTile(x, y, zoom);
          final DrawPoint tilePoint = getPoint(left, top);
          if (doAll || tileReplacements.containsKey(tile) || needsTileRedraw(tilePoint, deltaX, deltaY))
          {
            tilePoint.drawn = false;
            tilePlacements.put(tile, tilePoint);
          }
          else
          {
            recycleTile(tile);
            recyclePoint(tilePoint);
          }
        }
      }

      // Fin
      forceAllPlacements = false;
    }
  }

  /**
   * 
   * @param tilePoint
   * @param deltaX
   * @param deltaY
   * @return
   */
  private boolean needsTileRedraw(final Point tilePoint, final int deltaX, final int deltaY)
  {
    // Coins avant translation
    final int left = tilePoint.x - deltaX;
    final int right = left + TileProvider.TILE_SIZE;
    final int top = tilePoint.y - deltaY;
    final int bottom = top + TileProvider.TILE_SIZE;

    // Sur l'axe X
    final boolean xNeeds;
    if (deltaX > 0)
    {
      xNeeds = (left < 0);
    }
    else if (deltaX < 0)
    {
      xNeeds = (right > currentDisplayerWidth);
    }
    else
    {
      xNeeds = false;
    }

    // Sur l'axe Y
    final boolean yNeeds;
    if (deltaY > 0)
    {
      yNeeds = (top < 0);
    }
    else if (deltaY < 0)
    {
      yNeeds = (bottom > currentDisplayerHeight);
    }
    else
    {
      yNeeds = false;
    }

    return (xNeeds || yNeeds);
  }

  /**
   * 
   * @param deltaX
   * @param deltaY
   * @param deltaZoom
   * @param redrawOverlays
   */
  void manage(final int deltaX, final int deltaY, final int deltaZoom, final boolean redrawOverlays)
  {
    // Size ok ?
    if ((currentDisplayerWidth == 0) || (currentDisplayerHeight == 0))
    {
      return;
    }

    // Les transformations
    if (deltaZoom != 0)
    {
      final Lock transformationLocker = transformationLock.writeLock();
      try
      {
        // Lock
        transformationLocker.lockInterruptibly();

        transformation.postZoom(deltaZoom);
        final Lock overlaysLocker = overlaysLock.readLock();
        try
        {
          // Lock
          overlaysLocker.lockInterruptibly();

          for (final Overlay<?, ?> overlay : overlays)
          {
            overlay.transformationPostZoom(deltaZoom, center, zoom);
          }
        }
        finally
        {
          overlaysLocker.unlock();
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        transformationLocker.unlock();
      }
    }

    if ((deltaX != 0) || (deltaY != 0))
    {
      // Gestion de la translation avec zoom
      int finalDeltaX = deltaX;
      int finalDeltaY = deltaY;
      if (deltaZoom > 0)
      {
        finalDeltaX *= Math.pow(2, deltaZoom);
        finalDeltaY *= Math.pow(2, deltaZoom);
      }
      else if (deltaZoom < 0)
      {
        finalDeltaX /= Math.pow(2, -deltaZoom);
        finalDeltaY /= Math.pow(2, -deltaZoom);
      }

      final Lock transformationLocker = transformationLock.writeLock();
      try
      {
        // Lock
        transformationLocker.lockInterruptibly();

        transformation.postTranslate(finalDeltaX, finalDeltaY);
        final Lock overlaysLocker = overlaysLock.readLock();
        try
        {
          // Lock
          overlaysLocker.lockInterruptibly();

          for (final Overlay<?, ?> overlay : overlays)
          {
            overlay.transformationPostTranslate(finalDeltaX, finalDeltaY, center, zoom);
          }
        }
        finally
        {
          overlaysLocker.unlock();
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        transformationLocker.unlock();
      }
    }

    // Calcul des placements
    calculateTilePlacements(deltaX, deltaY, deltaZoom);

    // Gestion des placements
    if (tileProvider != null)
    {
      for (final Tile tile : tilePlacements.keySet())
      {
        // Recuperation d'une tache de telechargement dans le pool
        final DownloadTask<ImageData> downloadTask = getDownloadTask(tile);

        // Si present dans le cache
        downloadTask.memoryData = tileMemoryCache.get(tile);
        if (downloadTask.memoryData != null)
        {
          tileManagerMemoryCacheThread.postMemoryTask(downloadTask);
          continue;
        }

        // Si la tache tourne deja
        final Lock runningTasksLocker = runningTasksLock.readLock();
        try
        {
          // Lock
          runningTasksLocker.lockInterruptibly();

          if (runningTasks.contains(downloadTask))
          {
            recycleDownloadTask(downloadTask);
            continue;
          }
        }
        catch (final InterruptedException ie)
        {
          Thread.currentThread().interrupt();
        }
        finally
        {
          runningTasksLocker.unlock();
        }

        // Ajout dans la Queue
        synchronized (taskQueueLock)
        {
          if (!taskQueue.contains(downloadTask))
          {
            taskQueue.offer(downloadTask);
          }
        }
      }
    }

    // Lancement des Threads de recuperation memoire
    if (tileProvider != null)
    {
      synchronized (tileManagerMemoryCacheThread)
      {
        tileManagerMemoryCacheThread.notify();
      }
    }

    // Rafraichissement des couches
    if (redrawOverlays)
    {
      final Lock overlaysLocker = overlaysLock.readLock();
      try
      {
        // Lock
        overlaysLocker.lockInterruptibly();

        for (final Overlay<ImageData, Drawer> overlay : overlays)
        {
          overlay.requestRedraw(center, zoom);
        }
      }
      catch (final InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
      finally
      {
        overlaysLocker.unlock();
      }
    }

    // Lancement des Threads de recuperation
    if (tileProvider != null)
    {
      synchronized (tileManagerThread)
      {
        recalculateTaskPriorities = true;
        tileManagerThread.notify();
      }
    }

    // Rafraichissement de l'affichage dans tous les cas
    redraw();
  }

  /**
   * 
   */
  private void resetManage()
  {
    // Arret des taches
    stopAllDownloadTasks(false);
  }

  /**
   * 
   */
  private void stopAllDownloadTasks(final boolean shutdown)
  {
    // Les taches en attente
    synchronized (taskQueueLock)
    {
      if (shutdown)
      {
        for (final DownloadTask<ImageData> task : taskQueue)
        {
          task.stop();
          task.onShutdown();
        }
      }
      taskQueue.clear();
    }

    // Les taches en cours
    final Lock runningTasksLocker = runningTasksLock.readLock();
    try
    {
      // Lock
      runningTasksLocker.lockInterruptibly();

      for (final DownloadTask<ImageData> task : runningTasks)
      {
        task.stop();
        if (shutdown)
        {
          task.onShutdown();
        }
      }
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      runningTasksLocker.unlock();
    }
  }

  /**
   * 
   */
  private void adjustTasksPool()
  {
    tasksPoolMaxSize = 2 * tilesNeededX * tilesNeededY;
  }

  /**
   * 
   * @param tile
   * @return
   */
  private DownloadTask<ImageData> getDownloadTask(final Tile tile)
  {
    // Initialisations
    DownloadTask<ImageData> task = null;

    // Recherche dans le pool
    synchronized (tasksPool)
    {
      if (tasksPool.size() > 0)
      {
        task = tasksPool.removeFirst();
      }
    }

    // Sinon creation
    if (task == null)
    {
      task = new DownloadTask<ImageData>(this);
    }

    // Marquage comme demarrable
    task.start(tile, tileProvider);

    return task;
  }

  /**
   * 
   * @param task
   */
  void recycleDownloadTask(final DownloadTask<ImageData> task)
  {
    synchronized (tasksPool)
    {
      if (tasksPool.size() < tasksPoolMaxSize)
      {
        tasksPool.addFirst(task);
      }
    }
  }

  /**
   * 
   * @param x
   * @param y
   * @param tileZoom
   * @return
   */
  private Tile getTile(final long x, final long y, final int tileZoom)
  {
    if (tilesPool.size() > 0)
    {
      final Tile tile = tilesPool.removeFirst();
      tile.set(x, y, tileZoom);
      return tile;
    }

    return new Tile(x, y, tileZoom);
  }

  /**
   * 
   * @param tile
   */
  private void recycleTile(final Tile tile)
  {
    tilesPool.addFirst(tile);
  }

  /**
   * 
   * @param x
   * @param y
   * @return
   */
  private DrawPoint getPoint(final int x, final int y)
  {
    if (pointsPool.size() > 0)
    {
      final DrawPoint point = pointsPool.removeFirst();
      point.set(x, y);
      return point;
    }

    return new DrawPoint(x, y);
  }

  /**
   * 
   * @param point
   */
  private void recyclePoint(final DrawPoint point)
  {
    pointsPool.addFirst(point);
  }

  /**
   * 
   */
  public void start()
  {
    // Divers
    forceAllPlacements = true;

    // Threads
    tileManagerThread.start();
    tileManagerMemoryCacheThread.start();
    redrawThread.start();
  }

  /**
   * 
   * @param drawer
   */
  public void draw(final Drawer drawer)
  {
    synchronized (mainBufferLock)
    {
      graphicsHelper.drawImageData(drawer, mainBuffer, 0, 0);
    }
  }

  /**
   * 
   */
  public void redraw()
  {
    redrawThread.post();
  }

  /**
   * 
   */
  void doDraw()
  {
    // Dessin
    if (backgroundTileBuffer != null)
    {
      graphicsHelper.drawImageData(tempMainDrawer, backgroundTileBuffer, backgroundTileImageLeftOffset, backgroundTileImageTopOffset);
    }
    else
    {
      graphicsHelper.fillImageData(tempMainDrawer, backgroundColor, currentDisplayerWidth, currentDisplayerHeight);
    }

    final Lock transformationLocker = transformationLock.readLock();
    try
    {
      if (tileBuffer != null)
      {
        // Lock
        transformationLocker.lockInterruptibly();

        // La carte
        transformation.drawTransformation(graphicsHelper, tempMainDrawer, tileBuffer, 0, 0);
      }

      // Les couches
      final Lock overlaysLocker = overlaysLock.readLock();
      try
      {
        // Lock
        overlaysLocker.lockInterruptibly();

        // Plusieurs couches => gestion des priorites
        final int overlaysCount = overlays.size();
        if (overlaysCount > 1)
        {
          // Initialisations
          Overlay<ImageData, Drawer> overlay;
          earlyOverlays.clear();
          lateOverlays.clear();

          // Parcours de couches, pour determiner l'ordre de dessin (tot ou tard)
          for (int i = overlays.size() - 1; i >= 0; i--)
          {
            overlay = overlays.get(i);
            if (overlay.needsLateDraw())
            {
              lateOverlays.add(overlay);
            }
            else
            {
              earlyOverlays.add(overlay);
            }
          }

          // Dessin des couches "tot"
          for (final Overlay<ImageData, Drawer> early : earlyOverlays)
          {
            early.draw(tempMainDrawer);
          }

          // Dessin des couches "tard"
          for (final Overlay<ImageData, Drawer> late : lateOverlays)
          {
            late.draw(tempMainDrawer);
          }
        }
        // Une seule couche => dessin
        else if (overlaysCount == 1)
        {
          overlays.get(0).draw(tempMainDrawer);
        }
        // Sinon pas de dessin !
      }
      finally
      {
        overlaysLocker.unlock();
      }
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      if (tileBuffer != null)
      {
        transformationLocker.unlock();
      }
    }

    // Swap !
    synchronized (mainBufferLock)
    {
      swapMainBuffers();
    }
  }

  /**
   * 
   */
  private void drawTransformation()
  {
    final Lock transformationLocker = transformationLock.writeLock();
    try
    {
      // Lock
      transformationLocker.lockInterruptibly();

      if (!transformation.isIdentity())
      {
        // Dessin de l'ancien contenu transforme
        graphicsHelper.setTransparent(tempTileBuffer);

        // Dessin
        transformation.drawTransformation(graphicsHelper, tempTileDrawer, tileBuffer, 0, 0);

        // Reset de la transformation
        transformation.reset();

        // Swap
        swapTileBuffers();
      }
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      transformationLocker.unlock();
    }
  }

  /**
   * 
   * @param tile
   * @param point
   * @param image
   * @param doRedraw
   */
  @SuppressWarnings("unused")
  private void drawTile(final Tile tile, final Point point, final ImageData image, final boolean doRedraw)
  {
    // Dessin de l'ancien buffer
    drawTransformation();

    // Dessin de la tuile
    graphicsHelper.drawImageData(tileDrawer, image, point.x, point.y);

    /* DEBUG
    graphicsHelper.drawText(tileDrawer, tile.toString(), point.x, point.y, 5, 15);
    graphicsHelper.drawText(tileDrawer, tile.toString(), point.x, point.y + TileProvider.TILE_SIZE, 5, -5);
    graphicsHelper.drawText(tileDrawer, tile.toString(), point.x + TileProvider.TILE_SIZE, point.y, -80, 15);
    graphicsHelper.drawText(tileDrawer, tile.toString(), point.x + TileProvider.TILE_SIZE, point.y + TileProvider.TILE_SIZE, -80, -5);
    final ColorData red = graphicsHelper.newColor(255, 0, 0);
    graphicsHelper.drawLine(tileDrawer, point.x, point.y, point.x + TileProvider.TILE_SIZE, point.y, red);
    graphicsHelper.drawLine(tileDrawer, point.x + TileProvider.TILE_SIZE, point.y, point.x + TileProvider.TILE_SIZE, point.y + TileProvider.TILE_SIZE, red);
    graphicsHelper.drawLine(tileDrawer, point.x, point.y + TileProvider.TILE_SIZE, point.x + TileProvider.TILE_SIZE, point.y + TileProvider.TILE_SIZE, red);
    graphicsHelper.drawLine(tileDrawer, point.x, point.y, point.x, point.y + TileProvider.TILE_SIZE, red);
    */

    // Dessin de la carte si besoin
    if (doRedraw)
    {
      mapDisplayer.redraw();
    }
  }

  /**
   * 
   * @param tile
   * @param point
   */
  @SuppressWarnings("unused")
  private void drawNotAvailableTile(final Tile tile, final Point point)
  {
    // Dessin de l'ancien buffer
    drawTransformation();

    // Fond
    if (backgroundTileImage == null)
    {
      graphicsHelper.fillImageData(tileDrawer, backgroundColor, point.x, point.y, TileProvider.TILE_SIZE, TileProvider.TILE_SIZE);
    }
    else
    {
      graphicsHelper.drawImageData(tileDrawer, backgroundTileImage, point.x, point.y);
    }

    // Dessin de la carte
    mapDisplayer.redraw();
  }

  /**
   * 
   * @param tile
   * @param point
   * @param ioe
   */
  private void drawErrorTile(final Tile tile, final Point point, final IOException ioe)
  {
    // Dessin de l'ancien buffer
    drawTransformation();

    // Croix rouge
    graphicsHelper.drawLine(tileDrawer, point.x, point.y, point.x + TileProvider.TILE_SIZE, point.y + TileProvider.TILE_SIZE, missingTileColor);
    graphicsHelper.drawLine(tileDrawer, point.x, point.y + TileProvider.TILE_SIZE, point.x + TileProvider.TILE_SIZE, point.y, missingTileColor);

    // Erreur
    if (ioe != null)
    {
      mapDisplayer.onMissingTileException(tile, point, ioe, tileDrawer, missingTileColor);
    }

    // Dessin de la carte
    mapDisplayer.redraw();
  }

  /**
   * 
   */
  private void swapTileBuffers()
  {
    // Swap buffers
    final ImageData swapBuffer = tileBuffer;
    tileBuffer = tempTileBuffer;
    tempTileBuffer = swapBuffer;

    // Swap drawers
    final Drawer swapDrawer = tileDrawer;
    tileDrawer = tempTileDrawer;
    tempTileDrawer = swapDrawer;
  }

  /**
   * 
   */
  private void swapMainBuffers()
  {
    // Swap
    final ImageData swapBuffer = mainBuffer;
    mainBuffer = tempMainBuffer;
    tempMainBuffer = swapBuffer;

    final Drawer swapDrawer = mainDrawer;
    mainDrawer = tempMainDrawer;
    tempMainDrawer = swapDrawer;
  }

  /**
   * 
   * @param tile
   * @param image
   * @param putOnMemoryCache
   * @param doRedraw
   */
  void onTileDataAvailable(final Tile tile, final ImageData image, final boolean putOnMemoryCache, final boolean doRedraw)
  {
    final Lock controllerLocker = controllerLock.readLock();
    try
    {
      // Lock
      controllerLocker.lockInterruptibly();

      // Zoom change ?
      if (zoom != tile.zoom)
      {
        return;
      }

      // Recuperation du point de placement
      final DrawPoint point = tilePlacements.get(tile);
      if (point == null)
      {
        return;
      }

      // Marquage comme dessine
      point.drawn = true;

      // Sauvegarde dans le cache memoire
      if (putOnMemoryCache)
      {
        tileMemoryCache.put(tile, image);
      }

      // Dessin
      drawTile(tile, point, image, doRedraw);
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      controllerLocker.unlock();
    }
  }

  /**
   * 
   * @param tile
   */
  private void onTileDataNotAvailable(final Tile tile)
  {
    final Lock controllerLocker = controllerLock.readLock();
    try
    {
      // Lock
      controllerLocker.lockInterruptibly();

      // Zoom change ?
      if (zoom != tile.zoom)
      {
        return;
      }

      // Recuperation du point de placement
      final DrawPoint point = tilePlacements.get(tile);
      if (point == null)
      {
        return;
      }

      // Marquage comme dessine
      point.drawn = true;

      // Dessin
      drawNotAvailableTile(tile, point);
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      controllerLocker.unlock();
    }
  }

  /**
   * 
   * @param tile
   * @param ioe
   */
  private void onTileDataError(final Tile tile, final IOException ioe)
  {
    final Lock controllerLocker = controllerLock.readLock();
    try
    {
      // Lock
      controllerLocker.lockInterruptibly();

      // Zoom change ?
      if (zoom != tile.zoom)
      {
        return;
      }

      // Recuperation du point de placement
      final DrawPoint point = tilePlacements.get(tile);
      if (point == null)
      {
        return;
      }

      // Marquage comme dessine
      point.drawn = true;

      // Dessin
      drawErrorTile(tile, point, ioe);

      // Information au client
      mapDisplayer.onTileDataNotAvailable(tile, point, ioe);
    }
    catch (final InterruptedException ie)
    {
      Thread.currentThread().interrupt();
    }
    finally
    {
      controllerLocker.unlock();
    }
  }

  /**
   * 
   * @return
   */
  public MapController getController()
  {
    return controller;
  }

  /**
   * 
   * @return
   */
  public Projection getProjection()
  {
    return projection;
  }

  /**
   * 
   * @return
   */
  public TileProvider getTileProvider()
  {
    return tileProvider;
  }
}
