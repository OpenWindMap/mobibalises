package org.pedro.android.map;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.pedro.map.BuiltInTileProvider;
import org.pedro.map.GraphicsHelper;
import org.pedro.map.MapController;
import org.pedro.map.MapDisplayer;
import org.pedro.map.Overlay;
import org.pedro.map.Point;
import org.pedro.map.Projection;
import org.pedro.map.Tile;
import org.pedro.map.TileCache;
import org.pedro.map.TileFileCacheLight;
import org.pedro.map.TileManager;
import org.pedro.map.TileMemoryCache;
import org.pedro.map.TileProvider;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ZoomControls;

/**
 * 
 * @author pedro.m
 */
public final class MapView extends ViewGroup implements MapDisplayer<Bitmap, Canvas, Integer>
{
  private static final String                           METHOD_GET_POINTER_COUNT       = "getPointerCount";
  private static final int                              BUILD_VERSION_CODES_ECLAIR     = 5;

  private static boolean                                MULTITOUCH                     = true;
  private static final int                              BUILD_VERSION_CODES_FROYO      = 8;

  private static final String                           MULTI_TOUCH_HANDLER_CLASS_NAME = MapView.class.getPackage().getName() + ".MultiTouchHandler";

  private static final int                              HANDLER_ZOOM_CONTROLS_HIDER    = 0;
  public static final long                              ZOOM_CONTROLS_HIDE_TIMEOUT     = ViewConfiguration.getZoomControlsTimeout();

  //TODO 201609 private static final File                             CACHE_PATH                     = new File(Environment.getExternalStorageDirectory(), ".pedromap_cache");
  private File                                          cachePath;

  private IMapActivity                                  mapActivity;

  //TODO 201609 private TileCache                                     tileCache                      = new TileFileCacheLight(CACHE_PATH.getPath());
  private TileCache                                     tileCache;

  private TileManager<Bitmap, Canvas, Integer>          tileManager;
  private final GraphicsHelper<Bitmap, Canvas, Integer> graphicsHelper;
  private final TileMemoryCache<Bitmap>                 tileMemoryCache;

  private int                                           width;
  private int                                           height;

  private final SingleTouchHandler                      singleTouchHandler             = new SingleTouchHandler();
  private TouchHandler                                  multiTouchHandler;
  private boolean                                       multitouch;

  ZoomControls                                          zoomControls;
  ZoomControlsHideHandler                               zoomControlsHideHandler;
  private ZoomClickListener                             zoomInClickListener;
  private ZoomClickListener                             zoomOutClickListener;

  // Introspection
  private Method                                        getPointerCountMethod;

  private final boolean                                 lightMode;
  private boolean                                       closing                        = false;

  /**
   * 
   * @author pedro.m
   */
  private static class ZoomControlsHideHandler extends Handler
  {
    private MapView mapView;

    /**
     * 
     * @param mapView
     */
    ZoomControlsHideHandler(final MapView mapView)
    {
      super(Looper.getMainLooper());
      this.mapView = mapView;
    }

    @Override
    public void handleMessage(final Message msg)
    {
      mapView.hideZoomControls();
    }

    /**
     * 
     */
    void shutdown()
    {
      mapView = null;
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ZoomClickListener implements OnClickListener
  {
    private MapView       mapView;
    private final boolean zoomIn;

    /**
     * 
     * @param mapView
     * @param zoomIn
     */
    ZoomClickListener(final MapView mapView, final boolean zoomIn)
    {
      this.mapView = mapView;
      this.zoomIn = zoomIn;
    }

    @Override
    public void onClick(final View view)
    {
      if (zoomIn)
      {
        zoomIn();
      }
      else
      {
        zoomOut();
      }
    }

    /**
     * 
     */
    private void zoomIn()
    {
      // RAZ tempo de disparition des boutons
      mapView.hideZoomControlsDelayed();

      // ZoomIn
      mapView.getController().zoomIn();

      // Activation/desactivation des boutons si necessaire
      mapView.zoomControls.setIsZoomInEnabled(mapView.getController().getZoom() < mapView.getController().getMaxZoom());
      mapView.zoomControls.setIsZoomOutEnabled(mapView.getController().getZoom() > mapView.getController().getMinZoom());
    }

    /**
     * 
     */
    private void zoomOut()
    {
      // RAZ tempo de disparition des boutons
      mapView.hideZoomControlsDelayed();

      // Zoom Out
      mapView.getController().zoomOut();

      // Activation/desactivation des boutons si necessaire
      mapView.zoomControls.setIsZoomInEnabled(mapView.getController().getZoom() < mapView.getController().getMaxZoom());
      mapView.zoomControls.setIsZoomOutEnabled(mapView.getController().getZoom() > mapView.getController().getMinZoom());
    }

    /**
     * 
     */
    void shutdown()
    {
      mapView = null;
    }
  }

  /**
   * 
   * @param context
   */
  public MapView(final Context context)
  {
    this(context, null, 0);
  }

  /**
   * 
   * @param context
   * @param attributeSet
   */
  public MapView(final Context context, final AttributeSet attributeSet)
  {
    this(context, attributeSet, 0);
  }

  /**
   * 
   * @param inContext
   * @param attributeSet
   * @param defStyle
   */
  public MapView(final Context inContext, final AttributeSet attributeSet, final int defStyle)
  {
    // Initialisations
    super(inContext, attributeSet, defStyle);
    Log.d(getClass().getSimpleName(), ">>> MapView : " + this);

    /* TODO ? Desactivation acceleration hardware par parametre
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
    {
      setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    } */

    // Cache
    final File cachePath = new File(inContext.getExternalFilesDir(null), "pedromap_cache");
    tileCache = new TileFileCacheLight(cachePath.getPath());

    // Proprietes
    final IMapActivity innerMapActivity = (IMapActivity)inContext;
    mapActivity = innerMapActivity;

    // Mode light graphique ?
    lightMode = innerMapActivity.isGraphicsLightMode();
    graphicsHelper = new AndroidGraphicsHelper(inContext.getApplicationContext(), lightMode);
    tileMemoryCache = new TileMemoryCache<Bitmap>(graphicsHelper, 0);

    // Initialisation
    init();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< MapView : " + this);
  }

  @Override
  public void setBingApiKey(final String bingApiKey)
  {
    tileManager.setBingApiKey(bingApiKey);
  }

  @Override
  public void setCloudMadeKeys(final String apiKey, final String token)
  {
    tileManager.setCloudMadeKeys(apiKey, token);
  }

  @Override
  public void setGoogleApiKey(final String googleApiKey)
  {
    tileManager.setGoogleApiKey(googleApiKey);
  }

  @Override
  public void setIgnApiKey(final String ignApiKey)
  {
    tileManager.setIgnApiKey(ignApiKey);
  }

  @Override
  public void setIsApiKeyDebug(final boolean isApiKeyDebug)
  {
    tileManager.setIsApiKeyDebug(isApiKeyDebug);
  }

  @Override
  public Object[] getTileProvidersParams()
  {
    return tileManager.getTileProvidersParams();
  }

  /**
   * 
   */
  private void init()
  {
    try
    {
      // Introspection
      initIntrospection();

      // Divers
      setWillNotDraw(false);

      // Providers
      initTileManager();

      // Zooms
      initZoomControls();

      // Actions utilisateur
      initTouchHandlers();
    }
    catch (final IOException ioe)
    {
      Log.e(getClass().getSimpleName(), "Error", ioe);
      throw new RuntimeException(ioe.getMessage());
    }
  }

  /**
   * 
   */
  private void initIntrospection()
  {
    try
    {
      getPointerCountMethod = MotionEvent.class.getDeclaredMethod(METHOD_GET_POINTER_COUNT);
    }
    catch (final SecurityException se)
    {
      //Nothing
    }
    catch (final NoSuchMethodException nsme)
    {
      //Nothing
    }
  }

  /**
   * 
   */
  private void initTouchHandlers()
  {
    // Initialisation du gestionnaire "simple"
    singleTouchHandler.initHandler(getContext(), this);

    // Il faut au moins Froyo (2.2, level 8)
    if (!MULTITOUCH || (Build.VERSION.SDK_INT < BUILD_VERSION_CODES_FROYO))
    {
      return;
    }

    // Instanciation et initialisation
    try
    {
      final Class<?> multiTouchHandlerClass = Class.forName(MULTI_TOUCH_HANDLER_CLASS_NAME);
      multiTouchHandler = (TouchHandler)multiTouchHandlerClass.newInstance();
      multitouch = multiTouchHandler.initHandler(getContext(), this);
      if (!multitouch)
      {
        multiTouchHandler.shutdownHandler();
        multiTouchHandler = null;
      }
    }
    catch (final ClassNotFoundException cnfe)
    {
      throw new RuntimeException(cnfe);
    }
    catch (final IllegalAccessException iae)
    {
      throw new RuntimeException(iae);
    }
    catch (final InstantiationException ie)
    {
      throw new RuntimeException(ie);
    }
  }

  /**
   * 
   * @throws IOException
   */
  private void initTileManager() throws IOException
  {
    // Taille de l'ecran
    final WindowManager windowManager = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
    final Display display = windowManager.getDefaultDisplay();
    final int screenWidth = display.getWidth();
    final int screenHeight = display.getHeight();
    final int nbTilesMax = (int)Math.ceil(Math.ceil(1 + (float)screenWidth / TileProvider.TILE_SIZE) * Math.ceil(1 + (float)screenHeight / TileProvider.TILE_SIZE));
    final int nbThreads = Math.max(2, nbTilesMax / 9);

    // Demarrage
    tileCache.start();
    tileManager = new TileManager<Bitmap, Canvas, Integer>(this, graphicsHelper, tileMemoryCache, tileCache, nbThreads);
    tileManager.start();
  }

  /**
   * 
   */
  private void shutdownTouchHandlers()
  {
    // SingleTouch
    singleTouchHandler.shutdownHandler();

    // Multitouch
    if (multitouch && (multiTouchHandler != null))
    {
      multiTouchHandler.shutdownHandler();
    }
  }

  /**
   * 
   */
  private void initZoomControls()
  {
    // Initialisations
    zoomInClickListener = new ZoomClickListener(this, true);
    zoomOutClickListener = new ZoomClickListener(this, false);
    zoomControls = new ZoomControls(((Activity)mapActivity).getApplicationContext());
    zoomControls.setVisibility(View.GONE);

    // Listener sur les boutons ZoomIn et ZoomOut
    zoomControls.setOnZoomInClickListener(zoomInClickListener);
    zoomControls.setOnZoomOutClickListener(zoomOutClickListener);

    // Controle de la visibilite
    zoomControlsHideHandler = new ZoomControlsHideHandler(this);

    // Ajout de la vue
    addView(zoomControls, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
  }

  /**
   * 
   */
  protected void showZoomControls()
  {
    zoomControlsHideHandler.removeMessages(HANDLER_ZOOM_CONTROLS_HIDER);
    if (zoomControls.getVisibility() != View.VISIBLE)
    {
      zoomControls.show();
    }
  }

  /**
   * 
   */
  protected void hideZoomControlsDelayed()
  {
    zoomControlsHideHandler.removeMessages(HANDLER_ZOOM_CONTROLS_HIDER);
    zoomControlsHideHandler.sendEmptyMessageDelayed(HANDLER_ZOOM_CONTROLS_HIDER, ZOOM_CONTROLS_HIDE_TIMEOUT);
  }

  /**
   * 
   */
  void hideZoomControls()
  {
    zoomControls.hide();
  }

  @Override
  protected void onLayout(final boolean changed, final int left, final int top, final int right, final int bottom)
  {
    if (!changed)
    {
      return;
    }

    // Calcul de la largeur et de la hauteur disponibles
    width = right - left;
    height = bottom - top;

    // Positionnement des boutons de zoom en bas au milieu
    zoomControls.layout((width - zoomControls.getMeasuredWidth()) / 2, height - zoomControls.getMeasuredHeight(), (width - zoomControls.getMeasuredWidth()) / 2 + zoomControls.getMeasuredWidth(), height);
  }

  @Override
  protected final void onMeasure(final int widthMeasure, final int heightMeasure)
  {
    // Mesure
    zoomControls.measure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasure), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasure), MeasureSpec.AT_MOST));

    // make sure that MapView is big enough to display the ZoomControls
    setMeasuredDimension(Math.max(MeasureSpec.getSize(widthMeasure), zoomControls.getMeasuredWidth()), Math.max(MeasureSpec.getSize(heightMeasure), zoomControls.getMeasuredHeight()));
  }

  @Override
  protected synchronized void onSizeChanged(final int newWidth, final int newHeight, final int oldWidth, final int oldHeight)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onSizeChanged : " + newWidth + "/" + newHeight + " (" + oldWidth + "/" + oldHeight + ")");

    // Sauvegarde taille
    this.width = newWidth;
    this.height = newHeight;

    // Gestion
    tileManager.onMapDisplayerSizeChanged();

    // Initialisations
    Log.d(getClass().getSimpleName(), "<<< onSizeChanged");
  }

  @Override
  protected void onDraw(final Canvas canvas)
  {
    // Fond de carte
    tileManager.draw(canvas);
  }

  @Override
  public void redraw()
  {
    tileManager.redraw();
  }

  @Override
  public void invalidateDisplayer()
  {
    postInvalidate();
  }

  @Override
  public void onTileDataNotAvailable(final Tile tile, final Point point, final IOException ioe)
  {
    if (ioe == null)
    {
      Log.e(getClass().getSimpleName(), "Tile " + tile + " not available !");
    }
    else
    {
      Log.e(getClass().getSimpleName(), "Tile " + tile + " not available !", ioe);
    }
  }

  @Override
  public boolean onTouchEvent(final MotionEvent event)
  {
    // Initialisations
    boolean handled = false;

    // Multitouch
    if (multitouch && (multiTouchHandler != null) && !closing)
    {
      handled = multiTouchHandler.handleTouchEvent(event);
      singleTouchHandler.lock(handled);
    }

    // Singletouch
    if (!handled && (getPointerCount(event) == 1) && !closing)
    {
      handled = singleTouchHandler.handleTouchEvent(event);
    }

    // Fin de deplacement
    if (!handled && (MotionEvent.ACTION_UP == event.getAction()))
    {
      getController().onMoveInputFinished();
    }

    return handled;
  }

  /**
   * 
   * @param event
   * @return
   */
  private int getPointerCount(final MotionEvent event)
  {
    if ((Build.VERSION.SDK_INT >= BUILD_VERSION_CODES_ECLAIR) && (getPointerCountMethod != null))
    {
      try
      {
        final Integer pointerCountInteger = (Integer)getPointerCountMethod.invoke(event);
        return (pointerCountInteger == null ? 1 : pointerCountInteger.intValue());
      }
      catch (final IllegalArgumentException iae)
      {
        //Nothing
      }
      catch (final IllegalAccessException iae)
      {
        //Nothing
      }
      catch (final InvocationTargetException ite)
      {
        //Nothing
      }
    }

    return 1;
  }

  @Override
  public int getPixelWidth()
  {
    return width;
  }

  @Override
  public int getPixelHeight()
  {
    return height;
  }

  @Override
  public MapController getController()
  {
    return tileManager.getController();
  }

  @Override
  public Projection getProjection()
  {
    return tileManager.getProjection();
  }

  @Override
  public void setTileProvider(final TileProvider tileProvider)
  {
    tileManager.setTileProvider(tileProvider);
    manageZoomControlsChange();
  }

  @Override
  public void setTileProvider(final BuiltInTileProvider tileProvider) throws IOException
  {
    tileManager.setTileProvider(tileProvider);
    manageZoomControlsChange();
  }

  /**
   * 
   */
  protected void manageZoomControlsChange()
  {
    // Initialisations
    final MapController controller = getController();

    // Zoom maxi
    if (controller.getZoom() > controller.getMaxZoom())
    {
      controller.setZoom(controller.getMaxZoom());
    }

    // Zoom mini
    if (controller.getZoom() < controller.getMinZoom())
    {
      controller.setZoom(controller.getMinZoom());
    }

    // Activation/desactivation des boutons si necessaire
    zoomControls.setIsZoomInEnabled(controller.getZoom() < controller.getMaxZoom());
    zoomControls.setIsZoomOutEnabled(controller.getZoom() > controller.getMinZoom());
  }

  @Override
  public GraphicsHelper<Bitmap, Canvas, Integer> getGraphicsHelper()
  {
    return graphicsHelper;
  }

  @Override
  public List<Overlay<Bitmap, Canvas>> getOverlays(final boolean writeLock) throws InterruptedException
  {
    return tileManager.getOverlays(writeLock);
  }

  @Override
  public void unlockReadOverlays()
  {
    tileManager.unlockReadOverlays();
  }

  @Override
  public void unlockWriteOverlays()
  {
    tileManager.unlockWriteOverlays();
  }

  /**
   * 
   */
  protected void manageCacheAvailabilityMessage()
  {
    if (!tileCache.isAvailable())
    {
      //TODO 201609 ((TileCache.CacheNotAvailableListener)mapActivity).onCacheNotAvailable(CACHE_PATH.getPath());
      ((TileCache.CacheNotAvailableListener)mapActivity).onCacheNotAvailable(cachePath.getPath());
    }
  }

  /**
   * 
   */
  protected void shutdown()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> shutdown : " + this);

    // Flag
    closing = true;

    // Gestion des boutons de zoom
    zoomControlsHideHandler.removeMessages(HANDLER_ZOOM_CONTROLS_HIDER);

    // Les gestionnaires de mouvement
    shutdownTouchHandlers();

    // Le gestionnaire de tuiles
    tileManager.shutdown();
    tileManager = null;

    // Le cache
    tileCache.shutdown();
    tileCache = null;

    // Gestion du zoom
    zoomControlsHideHandler.shutdown();
    zoomControlsHideHandler = null;
    zoomInClickListener.shutdown();
    zoomInClickListener = null;
    zoomOutClickListener.shutdown();
    zoomOutClickListener = null;

    // Retrait des vues
    zoomControls.setOnZoomInClickListener(null);
    zoomControls.setOnZoomOutClickListener(null);
    removeAllViews();
    zoomControls = null;

    // Divers
    graphicsHelper.onShutdown();
    mapActivity = null;

    // Fin
    Log.d(getClass().getSimpleName(), "<<< shutdown : " + this);
  }

  @Override
  public TileCache getTileCache()
  {
    return tileCache;
  }

  @Override
  public void onMissingTileException(final Tile tile, final Point point, final IOException ioe, final Canvas canvas, final Integer missingTileColor)
  {
    Log.w(getClass().getSimpleName(), ioe);
    graphicsHelper.drawText(canvas, (ioe.getMessage() == null ? ioe.getClass().getSimpleName() : ioe.getMessage()), point.x + 5, point.y + 15, missingTileColor);
  }

  @Override
  public void onAttachedToWindow()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onAttachedToWindow : " + this);

    // Flag
    closing = false;

    // Parent
    super.onAttachedToWindow();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onAttachedToWindow : " + this);
  }

  @Override
  public void onDetachedFromWindow()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onDetachedFromWindow : " + this);

    // Flag
    closing = true;

    // Parent
    super.onDetachedFromWindow();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDetachedFromWindow : " + this);
  }

  @Override
  public boolean isLightMode()
  {
    return lightMode;
  }

  /**
   * 
   * @param consumedByOverlays
   */
  protected void onSingleTap(final boolean consumedByOverlays)
  {
    // Zoom
    showZoomControls();
    hideZoomControlsDelayed();

    // Activity
    mapActivity.onSingleTap(consumedByOverlays);
  }
}
