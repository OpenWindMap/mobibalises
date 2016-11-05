package org.pedro.android.mobibalises_common.map;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.analytics.AnalyticsService;
import org.pedro.android.mobibalises_common.map.InfosDrawable.DrawableInfo;
import org.pedro.android.mobibalises_common.map.ItemDatabaseHelper.ItemRow;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.view.BaliseDrawable;
import org.pedro.android.mobibalises_common.view.BaliseDrawableHelper;
import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.android.mobibalises_common.webcam.WebcamDatabaseHelper;
import org.pedro.android.mobibalises_common.webcam.WebcamDatabaseHelper.WebcamRow;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.map.GeoPoint;
import org.pedro.map.ItemizedOverlay;
import org.pedro.map.MapDisplayer;
import org.pedro.map.MercatorProjection;
import org.pedro.map.OverlayItem;
import org.pedro.map.Point;
import org.pedro.map.Rect;
import org.pedro.spots.Spot;
import org.pedro.spots.Utils;
import org.pedro.utils.ThreadUtils;
import org.pedro.webcams.Webcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.AsyncTask;
import android.util.FloatMath;
import android.util.Log;
import android.view.KeyEvent;

/**
 * 
 * @author pedro.m
 */
public final class ItemsOverlay extends ItemizedOverlay<Bitmap, Canvas, OverlayItem<Canvas>>
{
  // Constantes
  private static final int                     ZOOM_LIMITE_COLLISIONS = 9;

  private boolean                              displayInactive;
  private boolean                              centering;
  private String                               metarSynopCollisions;

  private AnalyticsService                     analyticsService;

  private final List<ItemsTapListener>         itemsTapListeners      = new ArrayList<ItemsTapListener>();

  private final List<BaliseTapListener>        baliseTapListeners     = new ArrayList<BaliseTapListener>();
  private final Map<String, BaliseProvider>    baliseProviders        = new HashMap<String, BaliseProvider>();
  final Map<String, BaliseItem>                baliseItems            = new HashMap<String, BaliseItem>();

  private final List<SpotTapListener>          spotTapListeners       = new ArrayList<SpotTapListener>();
  private final Map<String, Map<String, Spot>> providerSpotMap        = new HashMap<String, Map<String, Spot>>();
  final Map<String, OverlayItem<Canvas>>       spotItems              = new HashMap<String, OverlayItem<Canvas>>();

  private final List<WebcamTapListener>        webcamTapListeners     = new ArrayList<WebcamTapListener>();
  final Map<String, OverlayItem<Canvas>>       webcamItems            = new HashMap<String, OverlayItem<Canvas>>();

  private InfosDrawable                        lastInfoDrawableTaped  = null;
  private OverlayItem<Canvas>                  lastItemTaped          = null;

  private final Point                          infosPoint             = new Point();
  private OverlayItem<Canvas>                  infosItem              = null;

  // Collisions
  private final List<List<BaliseItem>>         balisesProches         = new ArrayList<List<BaliseItem>>();
  private final Path                           pathWindCollisions;
  private final Path                           pathWeatherCollisions;
  private final Paint                          collisionFillPaint;
  private final Paint                          collisionStrokePaint;

  private final float                          rayonCollisions;

  // Thread de selection des items
  private final ItemDatabaseHelper             itemHelper;
  SQLiteDatabase                               itemDatabase;
  final WebcamDatabaseHelper                   webcamHelper;
  final ItemsSelectorThread                    selectorThread         = new ItemsSelectorThread(this);
  GeoPoint                                     lastCenter;
  GeoPoint                                     lastTopLeft;
  GeoPoint                                     lastBottomRight;
  double                                       lastPixelLatAngle;
  double                                       lastPixelLngAngle;
  boolean                                      lastBalisesLayer;
  boolean                                      lastSpotsLayer;
  boolean                                      lastWebcamsLayer;

  // Thread de téléchargement des images des webcams
  final WebcamDownloadThread                   webcamDownloadThread;

  /**
   * 
   * @author pedro.m
   */
  protected static class WebcamDownloadThread extends Thread
  {
    /**
     * 
     * @author pedro.m
     */
    private static class WebcamDownloadStep
    {
      final WebcamRow row;
      final String    url;
      boolean         interrupted;

      /**
       * 
       * @param row
       */
      WebcamDownloadStep(final WebcamRow row)
      {
        this.row = row;
        this.url = Webcam.getUrlImage(row.urlImage, row.periodicite, row.decalagePeriodicite, row.decalageHorloge, row.fuseauHoraire, row.codeLocale);
        this.interrupted = false;
      }

      @Override
      public String toString()
      {
        return url;
      }

      @Override
      public boolean equals(final Object obj)
      {
        if (obj == null)
        {
          return false;
        }

        final WebcamDownloadStep other = (WebcamDownloadStep)obj;
        if (url == null)
        {
          return (other.url == null);
        }

        return url.equals(other.url);
      }

      @Override
      public int hashCode()
      {
        return (url == null ? 0 : url.hashCode());
      }
    }

    private static File              WEBCAM_DIR;
    private static final long        CACHE_DELTA         = 24 * 3600 * 1000;                                                      // 24h
    private static int               READ_SIZE           = 10240;
    private static final int         DEFAULT_BUFFER_SIZE = 512 * 1024;
    private byte[]                   imageBuffer;
    private ItemsOverlay             overlay;
    private List<WebcamDownloadStep> steps               = new ArrayList<WebcamDownloadStep>();
    private WebcamDownloadStep       currentStep         = null;

    /**
     * 
     * @param overlay
     */
    WebcamDownloadThread(final ItemsOverlay overlay, final Context context)
    {
      super(WebcamDownloadThread.class.getName());
      ActivityCommons.init(context);
      WEBCAM_DIR = new File(ActivityCommons.MOBIBALISES_EXTERNAL_STORAGE_PATH, "webcams");
      this.overlay = overlay;
    }

    /**
     * 
     * @param step
     */
    void postStep(final WebcamDownloadStep step)
    {
      // Déjà en cours
      if (currentStep != null)
      {
        if (currentStep.equals(step))
        {
          // Le meme, on ne fait rien de plus
          return;
        }

        // Un step différent. On l'interrompt.
        currentStep.interrupted = true;
      }

      // Ajout du step à la liste
      boolean added = false;
      synchronized (steps)
      {
        if (!steps.contains(step))
        {
          added = steps.add(step);
        }
      }

      // Notification
      if (added)
      {
        synchronized (this)
        {
          notify();
        }
      }
    }

    @Override
    public void run()
    {
      // Creation du repertoire des webcams si besoin et nettoyage
      WEBCAM_DIR.mkdirs();
      cleanCache();

      // Boucle principale
      while (!isInterrupted())
      {
        // Besoin du provider ?
        while (!isInterrupted() && canWait())
        {
          try
          {
            // Attente
            synchronized (this)
            {
              Log.d(getClass().getSimpleName(), ">>> wait");
              wait();
              Log.d(getClass().getSimpleName(), "<<< wait");
            }
          }
          catch (final InterruptedException ie)
          {
            Log.d(getClass().getSimpleName(), ">>> interrupt");
            Thread.currentThread().interrupt();
          }
        }

        if (!isInterrupted())
        {
          // Récupération image
          doIt();
        }

        // Nettoyage cache
        cleanCache();
      }

      // Fin
      overlay = null;
      imageBuffer = null;
    }

    /**
     * 
     */
    private void cleanCache()
    {
      Log.d(getClass().getSimpleName(), "cleaning cache");
      for (final File file : WEBCAM_DIR.listFiles())
      {
        final long limit = System.currentTimeMillis() - CACHE_DELTA;
        if (file.getName().startsWith("webcam_") && file.getName().endsWith(".jpg") && (file.lastModified() < limit))
        {
          Log.d(getClass().getSimpleName(), "... deleting " + file.getName());
          file.delete();
        }
      }
    }

    /**
     * 
     * @param url
     * @return
     */
    private static int getImageSize(final URL url)
    {
      // Initialisations
      HttpURLConnection conn = null;

      try
      {
        // Ouverture
        conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("HEAD");
        conn.getInputStream();

        // Lecture
        return conn.getContentLength();
      }
      catch (final IOException ioe)
      {
        return -1;
      }
      finally
      {
        if (conn != null)
        {
          conn.disconnect();
        }
      }
    }

    /**
     * 
     * @param url
     * @param bufferSize
     * @throws IOException
     */
    private int readImage(final URL url, final int bufferSize, final boolean progressive) throws IOException
    {
      // Initialisations
      HttpURLConnection conn = null;
      InputStream is = null;
      BufferedInputStream bis = null;
      int finalBufferSize = (bufferSize <= 0 ? DEFAULT_BUFFER_SIZE : bufferSize);

      try
      {
        // (Re)allocation du buffer
        Log.d(getClass().getSimpleName(), "buffer allocated to " + (imageBuffer == null ? "null" : "" + imageBuffer.length) + " asking for " + finalBufferSize);
        if ((imageBuffer == null) || (imageBuffer.length < finalBufferSize) || ((imageBuffer.length == DEFAULT_BUFFER_SIZE) && (finalBufferSize < DEFAULT_BUFFER_SIZE)))
        {
          Log.d(getClass().getSimpleName(), "allocating " + finalBufferSize + " bytes for image buffer");
          imageBuffer = new byte[finalBufferSize];
        }

        // Initialisations
        Bitmap image = null;
        int offset = 0;

        // Ouvertures
        conn = (HttpURLConnection)url.openConnection();
        is = conn.getInputStream();
        bis = new BufferedInputStream(is);

        // Première lecture
        int read = bis.read(imageBuffer, 0, READ_SIZE);

        // Boucle de lecture
        while ((read > 0) && !currentStep.interrupted)
        {
          if (progressive)
          {
            // Recyclage de l'image
            if (image != null)
            {
              image.recycle();
            }

            // Decodage de l'image
            try
            {
              image = BitmapFactory.decodeByteArray(imageBuffer, 0, offset + read);
              if (image != null)
              {
                overlay.onWebcamImageDownloaded(image);
              }
            }
            catch (final Throwable th)
            {
              // Image non lisible : rien à faire (mode progressif, on espère qu'elle le sera à la fin)
            }
          }

          // Lecture suivante
          offset += read;
          read = bis.read(imageBuffer, offset, Math.min(READ_SIZE, finalBufferSize - offset));
        }

        // Decodage final si non progressif
        if (!progressive && !currentStep.interrupted)
        {
          // Decodage de l'image
          try
          {
            if (offset > 0)
            {
              image = BitmapFactory.decodeByteArray(imageBuffer, 0, offset);
              if (image != null)
              {
                overlay.onWebcamImageDownloaded(image);
              }
              else
              {
                overlay.onWebcamImageDownloadError(url.toString(), "???"); //TODO : resources ?
              }
            }
            else
            {
              overlay.onWebcamImageDownloadError(url.toString(), "[Pas de réponse]"); //TODO : resources ?
            }
          }
          catch (final Throwable th)
          {
            // Image non lisible
            overlay.onWebcamImageDownloadError(url.toString(), th.getMessage());
          }
        }

        // Fin
        return offset;
      }
      finally
      {
        try
        {
          if (is != null)
          {
            is.close();
          }
          if (bis != null)
          {
            bis.close();
          }
          if (conn != null)
          {
            conn.disconnect();
          }
        }
        catch (final IOException ioe)
        {
          Log.w(getClass().getSimpleName(), ioe);
        }
      }
    }

    /**
     * 
     * @param url
     * @throws IOException
     */
    private void readImage(final URL url) throws IOException
    {
      final Bitmap image = BitmapFactory.decodeStream(url.openStream());
      if (image == null)
      {
        throw new IOException("Error reading image from " + url);
      }
      overlay.onWebcamImageDownloaded(image);
    }

    /**
     * 
     * @param provider
     * @param id
     * @return
     */
    protected static File getWebcamCacheFile(final String provider, final String id)
    {
      return new File(WEBCAM_DIR, "webcam_" + provider + "_" + id + ".jpg");
    }

    /**
     * 
     */
    private void doIt()
    {
      // Etape a traiter ?
      synchronized (steps)
      {
        // Etape
        Log.d(getClass().getSimpleName(), "steps : " + steps);
        if (steps.size() > 0)
        {
          currentStep = steps.remove(0);
        }
        else
        {
          return;
        }
      }

      try
      {
        // Traitement etape
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        try
        {
          // Recherche dans le cache
          final File cacheFile = getWebcamCacheFile(currentStep.row.provider, currentStep.row.id);
          final long cacheFileSize;
          final long cacheFileTs;
          if (cacheFile.exists() && cacheFile.canRead() && cacheFile.isFile())
          {
            cacheFileSize = cacheFile.length();
            cacheFileTs = cacheFile.lastModified();
            Log.d(getClass().getSimpleName(), "cache file exists : " + cacheFile + " (" + cacheFileSize + " bytes)");
          }
          else
          {
            cacheFileSize = 0;
            cacheFileTs = -1;
            Log.d(getClass().getSimpleName(), "cache file does not exist");
          }

          // Interruption ?
          if (currentStep.interrupted)
          {
            return;
          }

          // Récupération de la taille de l'image distante
          Log.d(getClass().getSimpleName(), "getting distant image size...");
          final URL url = new URL(currentStep.url);
          final int bufferSize = getImageSize(url);

          // Interruption ?
          if (currentStep.interrupted)
          {
            return;
          }

          // Dans le cache ou distant ?
          final long cacheLimit = System.currentTimeMillis() - CACHE_DELTA;
          final boolean fromCache = ((cacheFileSize > 0) && (cacheFileTs > cacheLimit) && (bufferSize > 0) && (bufferSize == cacheFileSize));
          Log.d(getClass().getSimpleName(), "cache/distant, sizes: " + cacheFileSize + "/" + bufferSize + ", ts: " + cacheFileTs + "/" + cacheLimit + " => fromCache=" + fromCache);

          // Récupération de l'image
          if (fromCache)
          {
            // Depuis le cache
            Log.d(getClass().getSimpleName(), "reading image from cache");
            readImage(cacheFile.toURI().toURL());
          }
          else
          {
            // Image distante
            Log.d(getClass().getSimpleName(), "reading distant image");
            final int imageSize = readImage(url, bufferSize, bufferSize > 0);

            // Sauvegarde dans le cache
            if ((bufferSize > 0) && (bufferSize != DEFAULT_BUFFER_SIZE) && (bufferSize == imageSize))
            {
              Log.d(getClass().getSimpleName(), "saving distant image (" + imageSize + " bytes)");
              fos = new FileOutputStream(cacheFile, false);
              bos = new BufferedOutputStream(fos);
              fos.write(imageBuffer, 0, imageSize);
            }
          }
        }
        catch (final IOException ioe)
        {
          Log.w(getClass().getSimpleName(), ioe);
          overlay.onWebcamImageDownloadError(currentStep.url, ioe.getMessage());
        }
        finally
        {
          try
          {
            if (bos != null)
            {
              bos.close();
            }
            if (fos != null)
            {
              fos.close();
            }
          }
          catch (final IOException ioe)
          {
            Log.w(getClass().getSimpleName(), ioe);
          }
        }
      }
      finally
      {
        // Fin
        currentStep = null;
      }
    }

    /**
     * 
     * @return
     */
    private boolean canWait()
    {
      // Demandes d'enregistrement
      synchronized (steps)
      {
        return (steps.size() == 0);
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ItemsSelectorThread extends Thread
  {
    /**
     * 
     * @author pedro.m
     */
    private static class ItemsSelectorStep
    {
      // Type
      public enum StepType
      {
        BALISES, SPOTS, WEBCAMS, TOUT;
      }

      final StepType stepType;
      final GeoPoint center;
      final GeoPoint topLeft;
      final GeoPoint bottomRight;
      final double   pixelLatAngle;
      final double   pixelLngAngle;
      final boolean  balisesLayer;
      final boolean  spotsLayer;
      final boolean  webcamsLayer;
      boolean        interrupted;

      /**
       * 
       * @param stepType
       * @param center
       * @param topLeft
       * @param bottomRight
       * @param pixelLatAngle
       * @param pixelLngAngle
       * @param balisesLayer
       * @param spotsLayer
       * @param webcamsLayer
       */
      ItemsSelectorStep(final StepType stepType, final GeoPoint center, final GeoPoint topLeft, final GeoPoint bottomRight, final double pixelLatAngle, final double pixelLngAngle, final boolean balisesLayer, final boolean spotsLayer,
          final boolean webcamsLayer)
      {
        this.stepType = stepType;
        this.center = center;
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
        this.pixelLatAngle = pixelLatAngle;
        this.pixelLngAngle = pixelLngAngle;
        this.balisesLayer = balisesLayer;
        this.spotsLayer = spotsLayer;
        this.webcamsLayer = webcamsLayer;
        this.interrupted = false;
      }

      @Override
      public String toString()
      {
        return stepType.toString();
      }
    }

    private ItemsOverlay            overlay;
    private List<ItemsSelectorStep> steps       = new ArrayList<ItemsSelectorStep>();
    private ItemsSelectorStep       currentStep = null;

    /**
     * 
     * @param overlay
     */
    ItemsSelectorThread(final ItemsOverlay overlay)
    {
      super(ItemsSelectorThread.class.getName());
      this.overlay = overlay;
    }

    /**
     * 
     * @param step
     * @param redraw
     * @param forceRedraw
     * @return
     */
    private boolean doBalisesStep(final ItemsSelectorStep step, final boolean redraw, final boolean forceRedraw)
    {
      try
      {
        // Log
        Log.d(getClass().getSimpleName(), ">>> doBalisesStep()");

        // Pas d'affichage des webcams !
        if (!step.balisesLayer)
        {
          Log.d(getClass().getSimpleName(), "balises non affichées");
          return overlay.updateBaliseItems(null, redraw, forceRedraw);
        }

        // Calculs des coordonnées
        final float rayon = BaliseDrawable.DISPLAY_BOUNDS.right;
        final float minLat = (float)(step.bottomRight.getLatitude() - (rayon * step.pixelLatAngle));
        final float maxLat = (float)(step.topLeft.getLatitude() + (rayon * step.pixelLatAngle));
        final float minLng = (float)(step.topLeft.getLongitude() - (rayon * step.pixelLngAngle));
        final float maxLng = (float)(step.bottomRight.getLongitude() + (rayon * step.pixelLngAngle));

        // Sélection des webcams
        if (step.interrupted)
        {
          return false;
        }
        final Map<String, ItemRow> rows = ItemDatabaseHelper.selectItems(overlay.itemDatabase, ItemDatabaseHelper.TYPE_BALISE, minLat, maxLat, minLng, maxLng);
        Log.d(getClass().getSimpleName(), "balises rows : " + rows.size());

        // MAJ des items
        if (step.interrupted)
        {
          return false;
        }

        return overlay.updateBaliseItems(rows, redraw, forceRedraw);
      }
      finally
      {
        // Fin
        Log.d(getClass().getSimpleName(), "<<< doBalisesStep(), interupted=" + step.interrupted);
      }
    }

    /**
     * 
     * @param step
     * @param redraw
     * @param forceRedraw
     * @return
     */
    private boolean doSpotsStep(final ItemsSelectorStep step, final boolean redraw, final boolean forceRedraw)
    {
      try
      {
        // Log
        Log.d(getClass().getSimpleName(), ">>> doSpotsStep()");

        // Pas d'affichage des spots !
        if (!step.spotsLayer)
        {
          Log.d(getClass().getSimpleName(), "spots non affichés");
          return overlay.updateSpotItems(null, redraw, forceRedraw);
        }

        // Calculs des coordonnées
        final float minLat = (float)(step.bottomRight.getLatitude() - (SpotDrawable.RAYON_SPOT * step.pixelLatAngle));
        final float maxLat = (float)(step.topLeft.getLatitude() + (SpotDrawable.RAYON_SPOT * step.pixelLatAngle));
        final float minLng = (float)(step.topLeft.getLongitude() - (SpotDrawable.RAYON_SPOT * step.pixelLngAngle));
        final float maxLng = (float)(step.bottomRight.getLongitude() + (SpotDrawable.RAYON_SPOT * step.pixelLngAngle));

        // Sélection des spots
        if (step.interrupted)
        {
          return false;
        }
        final Map<String, ItemRow> rows = ItemDatabaseHelper.selectItems(overlay.itemDatabase, ItemDatabaseHelper.TYPE_SPOT, minLat, maxLat, minLng, maxLng);
        Log.d(getClass().getSimpleName(), "spot rows : " + rows.size());

        // MAJ des items
        if (step.interrupted)
        {
          return false;
        }

        return overlay.updateSpotItems(rows, redraw, forceRedraw);
      }
      finally
      {
        // Fin
        Log.d(getClass().getSimpleName(), "<<< doSpotsStep(), interupted=" + step.interrupted);
      }
    }

    /**
     * 
     * @param step
     * @param redraw
     * @param forceRedraw
     * @return
     */
    private boolean doWebcamsStep(final ItemsSelectorStep step, final boolean redraw, final boolean forceRedraw)
    {
      try
      {
        // Log
        Log.d(getClass().getSimpleName(), ">>> doWebcamsStep()");

        // Pas d'affichage des webcams !
        if (!step.webcamsLayer)
        {
          Log.d(getClass().getSimpleName(), "webcams non affichées");
          return overlay.updateWebcamItems(null, redraw, forceRedraw);
        }

        // Calculs des coordonnées
        final float minLat = (float)(step.bottomRight.getLatitude() - (WebcamDrawable.rayonWebcam * step.pixelLatAngle));
        final float maxLat = (float)(step.topLeft.getLatitude() + (WebcamDrawable.rayonWebcam * step.pixelLatAngle));
        final float minLng = (float)(step.topLeft.getLongitude() - (WebcamDrawable.rayonWebcam * step.pixelLngAngle));
        final float maxLng = (float)(step.bottomRight.getLongitude() + (WebcamDrawable.rayonWebcam * step.pixelLngAngle));

        // Sélection des webcams
        if (step.interrupted)
        {
          return false;
        }
        final SQLiteDatabase database = overlay.webcamHelper.getReadableDatabase();
        final Map<String, WebcamRow> rows = WebcamDatabaseHelper.selectWebcams(database, null, minLat, maxLat, minLng, maxLng);
        database.close();
        Log.d(getClass().getSimpleName(), "webcams rows : " + rows.size());

        // MAJ des items
        if (step.interrupted)
        {
          return false;
        }

        return overlay.updateWebcamItems(rows, redraw, forceRedraw);
      }
      finally
      {
        // Fin
        Log.d(getClass().getSimpleName(), "<<< doWebcamsStep(), interupted=" + step.interrupted);
      }
    }

    /**
     * 
     * @param step
     */
    void postStep(final ItemsSelectorStep step)
    {
      // Interruption du step courant si nécessaire (de même type)
      if (currentStep != null && (currentStep.stepType == step.stepType))
      {
        currentStep.interrupted = true;
      }

      // Ajout du step à la liste
      synchronized (steps)
      {
        steps.add(step);
      }

      // Notification
      synchronized (this)
      {
        notify();
      }
    }

    @Override
    public void run()
    {
      while (!isInterrupted())
      {
        // Besoin du provider ?
        while (!isInterrupted() && canWait())
        {
          try
          {
            // Attente
            synchronized (this)
            {
              Log.d(getClass().getSimpleName(), ">>> wait");
              wait();
              Log.d(getClass().getSimpleName(), "<<< wait");
            }
          }
          catch (final InterruptedException ie)
          {
            Log.d(getClass().getSimpleName(), ">>> interrupt");
            Thread.currentThread().interrupt();
          }
        }

        if (!isInterrupted())
        {
          doIt();
        }
      }

      // Fin
      overlay = null;
    }

    /**
     * 
     */
    private void doIt()
    {
      // Etape a traiter ?
      final ItemsSelectorStep step;
      synchronized (steps)
      {
        // Etape
        Log.d(getClass().getSimpleName(), "steps : " + steps);
        if (steps.size() > 0)
        {
          step = steps.remove(0);
        }
        else
        {
          step = null;
        }
      }

      // Traitement etape
      if ((step != null) && (step.center != null) && (step.topLeft != null) && (step.bottomRight != null))
      {
        switch (step.stepType)
        {
          case BALISES:
            doBalisesStep(step, true, true);
            break;
          case SPOTS:
            doSpotsStep(step, true, true);
            break;
          case WEBCAMS:
            doWebcamsStep(step, true, true);
            break;
          case TOUT:
            doBalisesStep(step, false, false);
            doSpotsStep(step, false, false);
            doWebcamsStep(step, true, true);
            break;
          default:
            break;
        }
      }
    }

    /**
     * 
     * @return
     */
    private boolean canWait()
    {
      // Demandes d'enregistrement
      synchronized (steps)
      {
        return (steps.size() == 0);
      }
    }
  }

  /**
   * 
   * @param context
   * @param sharedPreferences
   * @param mapView
   * @param scalingFactor
   */
  public ItemsOverlay(final Context context, final SharedPreferences sharedPreferences, final MapView mapView, final float scalingFactor)
  {
    // Super
    super(mapView, ItemsOverlay.class.getName());

    // Thread de téléchargement des webcams
    webcamDownloadThread   = new WebcamDownloadThread(this, context);

    // Preferences
    updatePreferences(context, sharedPreferences);

    // Dimensions
    rayonCollisions = 42 * scalingFactor;

    // Collisions : path vent
    final double betaWind = Math.asin(DrawingCommons.WIND_ICON_RAYON_CONTOUR / rayonCollisions);
    final float betaWindDegres = (float)Math.toDegrees(betaWind);
    final float dxWind = DrawingCommons.WIND_ICON_RAYON_CONTOUR * (float)Math.cos((float)betaWind);
    final float dyWind = DrawingCommons.WIND_ICON_RAYON_CONTOUR * (float)Math.sin((float)betaWind);
    pathWindCollisions = new Path();
    pathWindCollisions.lineTo(dxWind, -rayonCollisions + dyWind);
    pathWindCollisions.arcTo(new RectF(-DrawingCommons.WIND_ICON_RAYON_CONTOUR, -rayonCollisions - DrawingCommons.WIND_ICON_RAYON_CONTOUR, DrawingCommons.WIND_ICON_RAYON_CONTOUR, -rayonCollisions + DrawingCommons.WIND_ICON_RAYON_CONTOUR),
        betaWindDegres, 180 - 2 * betaWindDegres);
    pathWindCollisions.lineTo(0, 0);
    pathWindCollisions.setLastPoint(0, 0);

    // Collisions : path meteo
    final float rayonSpot = DrawingCommons.RAYON_SPOT_WEATHER * scalingFactor;
    final double betaWeather = Math.asin(rayonSpot / rayonCollisions);
    final float betaWeatherDegres = (float)Math.toDegrees(betaWeather);
    final float dxWeather = rayonSpot * (float)Math.cos((float)betaWeather);
    final float dyWeather = rayonSpot * (float)Math.sin((float)betaWeather);
    pathWeatherCollisions = new Path();
    pathWeatherCollisions.lineTo(dxWeather, -rayonCollisions + dyWeather);
    pathWeatherCollisions.arcTo(new RectF(-rayonSpot, -rayonCollisions - rayonSpot, rayonSpot, -rayonCollisions + rayonSpot), betaWeatherDegres, 180 - 2 * betaWeatherDegres);
    pathWeatherCollisions.lineTo(0, 0);
    pathWeatherCollisions.setLastPoint(0, 0);

    // Paint du cone de collision
    collisionFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    collisionFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    collisionFillPaint.setColor(mapView.getContext().getResources().getColor(R.color.map_balise_fill_collisions));
    collisionFillPaint.setShader(new RadialGradient(0, 0, rayonCollisions - DrawingCommons.WIND_ICON_RAYON_CONTOUR, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP));
    collisionStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    collisionStrokePaint.setStyle(Paint.Style.STROKE);
    collisionStrokePaint.setColor(mapView.getContext().getResources().getColor(R.color.map_balise_stroke_collisions));
    collisionStrokePaint.setShader(new RadialGradient(0, 0, rayonCollisions - DrawingCommons.WIND_ICON_RAYON_CONTOUR, Color.BLACK, Color.WHITE, Shader.TileMode.CLAMP));

    // Items database
    itemHelper = ItemDatabaseHelper.newInstance(context);
    itemDatabase = itemHelper.getWritableDatabase();

    // Webcam helper
    webcamHelper = WebcamDatabaseHelper.newInstance(context);

    // Thread selector
    selectorThread.start();

    // Thread webcam
    webcamDownloadThread.start();
  }

  /**
   * 
   * @param provider
   * @param providerKey
   */
  public void addBaliseProvider(final BaliseProvider provider, final String providerKey)
  {
    baliseProviders.put(providerKey, provider);
  }

  /**
   * 
   * @param centering
   */
  public void setCentering(final boolean centering)
  {
    this.centering = centering;
  }

  /**
   * 
   * @param baliseProviderKey
   * @param baliseId
   */
  protected void showTooltip(final String baliseProviderKey, final String baliseId)
  {
    // Recuperation de l'item
    try
    {
      final BaliseItem searchItem = new BaliseItem(baliseProviderKey, baliseId, null, null);
      final BaliseItem baliseItem = baliseItems.get(searchItem.getName());
      if (baliseItem != null)
      {
        onTap(baliseItem);
      }
    }
    catch (final ConcurrentModificationException cme)
    {
      Log.w(getClass().getSimpleName(), "!! CME dans showTooltip(" + baliseProviderKey + ", " + baliseId + ")");
    }
  }

  /**
   * 
   * @param item
   */
  private void setInfoDrawable(final OverlayItem<Canvas> item)
  {
    // Gestion de l'affichage de l'infobulle
    if ((lastInfoDrawableTaped != null) && (lastInfoDrawableTaped != item.getDrawable()))
    {
      lastInfoDrawableTaped.setDrawDetails(false);
    }
    lastInfoDrawableTaped = (InfosDrawable)item.getDrawable();
    lastInfoDrawableTaped.setDrawDetails(true);
  }

  @Override
  public boolean onTap(final List<OverlayItem<Canvas>> tapItems)
  {
    // Un seul item => pas de choix
    if (tapItems.size() == 1)
    {
      return onTap(tapItems.get(0));
    }

    // Plusieurs items => choix
    fireOnItemsTap(tapItems);
    return true;
  }

  /**
   * 
   * @param item
   * @return
   */
  protected boolean onTap(final OverlayItem<Canvas> item)
  {
    try
    {
      // Commun
      if (lastItemTaped != null)
      {
        lastItemTaped.recycle();
      }
      lastItemTaped = item;

      // Centrage
      if (centering)
      {
        mapDisplayer.getController().animateTo(item.getPoint());
      }

      // Balise
      if (BaliseItem.class.isAssignableFrom(item.getClass()))
      {
        // Initialisations
        final BaliseItem baliseItem = (BaliseItem)item;

        // Recherche dans les balises affichées
        final BaliseItem displayedBaliseItem = baliseItems.get(baliseItem.getName());
        if (displayedBaliseItem != null)
        {
          setTapedItem(displayedBaliseItem);
        }

        // Notification
        fireOnBaliseTap(baliseItem.providerKey, item.getName());

        // Google analytics
        if (analyticsService != null)
        {
          analyticsService.trackEvent(AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_BALISE_CLICKED, baliseItem.getName());
        }
      }
      // Spot
      else if (SpotItem.class.isAssignableFrom(item.getClass()))
      {
        // Initialisations
        final SpotItem spotItem = (SpotItem)item;

        // Recherche dans les spots affichés
        final SpotItem displayedSpotItem = (SpotItem)spotItems.get(spotItem.getName());
        if (displayedSpotItem != null)
        {
          setTapedItem(displayedSpotItem);
        }

        // Notification
        fireOnSpotTap(spotItem);

        // Google analytics
        if (analyticsService != null)
        {
          analyticsService.trackEvent(AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_SPOT_CLICKED, spotItem.getName());
        }
      }
      // Webcam
      else if (WebcamItem.class.isAssignableFrom(item.getClass()))
      {
        // Initialisations
        final WebcamItem webcamItem = (WebcamItem)item;

        // Recherche dans les spots affichés
        final WebcamItem displayedWebcamItem = (WebcamItem)webcamItems.get(webcamItem.getName());
        if (displayedWebcamItem != null)
        {
          // Infobulle
          setTapedItem(displayedWebcamItem);

          // Téléchargement image
          final WebcamDownloadThread.WebcamDownloadStep step = new WebcamDownloadThread.WebcamDownloadStep(displayedWebcamItem.row);
          webcamDownloadThread.postStep(step);

          // Notification
          fireOnWebcamTap(displayedWebcamItem);

          // Google analytics
          if (analyticsService != null)
          {
            analyticsService.trackEvent(AnalyticsService.CAT_MAP, AnalyticsService.ACT_MAP_WEBCAM_CLICKED, webcamItem.getName());
          }
        }
      }
    }
    catch (final ConcurrentModificationException cme)
    {
      Log.w(getClass().getSimpleName(), "!!! CME dans onTap");
    }

    return true;
  }

  /**
   * 
   * @param item
   */
  private void setTapedItem(final OverlayItem<Canvas> item)
  {
    // Balise
    if (BaliseItem.class.isAssignableFrom(item.getClass()))
    {
      // Gestion de l'affichage de l'infobulle
      if (BaliseDrawable.isTooltipDisplayedOnTap())
      {
        setInfoDrawable(item);
      }
    }
    // Spot
    else if (SpotItem.class.isAssignableFrom(item.getClass()))
    {
      // Gestion de l'affichage de l'infobulle
      setInfoDrawable(item);
    }
    // Webcam
    else if (WebcamItem.class.isAssignableFrom(item.getClass()))
    {
      // Gestion de l'affichage de l'infobulle
      setInfoDrawable(item);
    }
  }

  @Override
  public boolean onTap(final GeoPoint geoPoint, final Point point)
  {
    if (lastInfoDrawableTaped != null)
    {
      // Sur le lien d'infobulle ?
      if (!Utils.isStringVide(lastInfoDrawableTaped.touchableTitle))
      {
        boolean inside = point.x >= (lastInfoDrawableTaped.boxLeft + lastInfoDrawableTaped.touchableBox.left);
        inside = inside && (point.x <= (lastInfoDrawableTaped.boxLeft + lastInfoDrawableTaped.touchableBox.right));
        inside = inside && (point.y >= (lastInfoDrawableTaped.boxTop + lastInfoDrawableTaped.touchableBox.top));
        inside = inside && (point.y <= (lastInfoDrawableTaped.boxTop + lastInfoDrawableTaped.touchableBox.bottom));
        if (inside)
        {
          onTouchableInfoTap();
          return true;
        }
      }

      // Sur l'image d'infobulle ?
      if (lastInfoDrawableTaped.infos.size() >= 2)
      {
        final DrawableInfo imageInfo = lastInfoDrawableTaped.infos.get(1);
        if (!Utils.isStringVide(imageInfo.imageUrl))
        {
          boolean inside = point.x >= (lastInfoDrawableTaped.boxLeft + lastInfoDrawableTaped.imageBox.left);
          inside = inside && (point.x <= (lastInfoDrawableTaped.boxLeft + lastInfoDrawableTaped.imageBox.right));
          inside = inside && (point.y >= (lastInfoDrawableTaped.boxTop + lastInfoDrawableTaped.imageBox.top));
          inside = inside && (point.y <= (lastInfoDrawableTaped.boxTop + lastInfoDrawableTaped.imageBox.bottom));
          if (inside)
          {
            if ((imageInfo.image != null) && (imageInfo.imageError == null))
            {
              onImageInfoTap();
            }
            return true;
          }
        }
      }

      // Effacement de l'infobulle
      lastInfoDrawableTaped.setDrawDetails(false);
      lastInfoDrawableTaped = null;
    }

    if (lastItemTaped != null)
    {
      lastItemTaped.recycle();
      lastItemTaped = null;
    }

    return super.onTap(geoPoint, point);
  }

  /**
   * 
   */
  private void onTouchableInfoTap()
  {
    if (lastInfoDrawableTaped == null)
    {
      return;
    }

    if (BaliseDrawable.class.isAssignableFrom(lastInfoDrawableTaped.getClass()))
    {
      final BaliseItem baliseItem = (BaliseItem)infosItem;
      fireOnBaliseInfoLinkTap(baliseItem.providerKey, baliseItem.baliseId);
    }
    else if (SpotDrawable.class.isAssignableFrom(lastInfoDrawableTaped.getClass()))
    {
      fireOnSpotInfoLinkTap((SpotItem)infosItem);
    }
    else if (WebcamDrawable.class.isAssignableFrom(lastInfoDrawableTaped.getClass()))
    {
      fireOnWebcamInfoLinkTap((WebcamItem)infosItem);
    }
  }

  /**
   * 
   */
  private void onImageInfoTap()
  {
    if (lastInfoDrawableTaped == null)
    {
      return;
    }

    if (WebcamDrawable.class.isAssignableFrom(lastInfoDrawableTaped.getClass()))
    {
      fireOnWebcamImageTap((WebcamItem)infosItem);
    }
  }

  @Override
  public void onMapDisplayerSizeChanged()
  {
    super.onMapDisplayerSizeChanged();
    WebcamDrawable.displayerWidth = mapDisplayer.getPixelWidth();
  }

  @Override
  public boolean drawOverlay(final Canvas canvas, final long left, final long top)
  {
    // Initialisations
    infosItem = null;
    final boolean zoomChanged = controller.getZoom() != lastZoom;
    lastZoom = controller.getZoom();

    // Les webcams
    drawItems(canvas, webcamItems, left, top, zoomChanged);

    // Les spots
    drawItems(canvas, spotItems, left, top, zoomChanged);

    // Les balises
    drawBaliseItems(canvas, baliseItems, left, top, zoomChanged);

    // Le detail en dernier
    if (infosItem != null)
    {
      infosItem.getDrawable().draw(canvas, infosPoint);
    }

    return (infosItem != null);
  }

  /**
   * 
   * @param canvas
   * @param itemsToDraw
   * @param left
   * @param top
   * @param zoomChanged
   */
  private void drawItems(final Canvas canvas, final Map<String, OverlayItem<Canvas>> itemsToDraw, final long left, final long top, final boolean zoomChanged)
  {
    //Log.d(getClass().getSimpleName(), ">>> drawItems(..., [" + itemsToDraw.size() + "], " + left + ", " + top + ", " + zoomChanged + ")");
    try
    {
      for (final Map.Entry<String, OverlayItem<Canvas>> entry : itemsToDraw.entrySet())
      {
        final OverlayItem<Canvas> item = entry.getValue();
        // Conversion en pixels
        if (zoomChanged || (item.x < 0) || (item.y < 0))
        {
          item.x = Math.round(MercatorProjection.longitudeToPixelX(item.getPoint().getLongitude(), lastZoom));
          item.y = Math.round(MercatorProjection.latitudeToPixelY(item.getPoint().getLatitude(), lastZoom));
        }
        drawPoint.set((int)(item.x - left), (int)(item.y - top));

        // Si details => on l'affiche tout le temps (en dernier)
        if (((InfosDrawable)item.getDrawable()).isDrawDetails())
        {
          infosItem = item;
          infosPoint.set(drawPoint.x, drawPoint.y);
          item.visible = true;
        }
        else
        {
          // Recuperation de coins de l'item
          final Rect bounds = item.getDrawable().getDisplayBounds();
          if ((drawPoint.x >= bounds.left) && (drawPoint.y >= bounds.top) && (drawPoint.x <= mapDisplayer.getPixelWidth() + bounds.right) && (drawPoint.y <= mapDisplayer.getPixelHeight() + bounds.bottom))
          {
            if (isItemDisplayable(item))
            {
              item.getDrawable().draw(canvas, drawPoint);
              item.visible = true;
            }
          }
          else
          {
            item.visible = false;
          }
        }
      }
    }
    catch (final ConcurrentModificationException cme)
    {
      // Liste modifiee pendant le dessin : on ne fait rien, car si la liste est modifiee
      // c'est qu'un redessin va etre demande ensuite
    }
  }

  /**
   * 
   * @param canvas
   * @param itemsToDraw
   * @param left
   * @param top
   * @param zoomChanged
   */
  private void drawBaliseItems(final Canvas canvas, final Map<String, BaliseItem> itemsToDraw, final long left, final long top, final boolean zoomChanged)
  {
    Log.d(getClass().getSimpleName(), ">>> drawBaliseItems(..., [" + itemsToDraw.size() + "], " + left + ", " + top + ", " + zoomChanged + ")");
    try
    {
      // Dessin des rappels des balises "collisionnees"
      if ((BaliseDrawable.drawWindIcon || BaliseDrawable.drawWeatherIcon) && (lastZoom >= ZOOM_LIMITE_COLLISIONS))
      {
        int x;
        int y;
        for (final List<BaliseItem> groupeBalises : balisesProches)
        {
          for (final BaliseItem item : groupeBalises)
          {
            if (item.collide)
            {
              // Coordonnees pixels de la balise
              x = (int)(Math.round(MercatorProjection.longitudeToPixelX(item.getPoint().getLongitude(), lastZoom)) - left);
              y = (int)(Math.round(MercatorProjection.latitudeToPixelY(item.getPoint().getLatitude(), lastZoom)) - top);

              // Cone
              item.collisionMatrix.reset();
              item.collisionMatrix.postRotate(item.deltaAngle);
              item.collisionMatrix.postTranslate(x, y);
              if (BaliseDrawable.drawWindIcon)
              {
                // Cone large pour l'icone de vent
                pathWindCollisions.transform(item.collisionMatrix, item.collisionPath);
              }
              else
              {
                // Cone etroit pour le point de l'icone meteo
                pathWeatherCollisions.transform(item.collisionMatrix, item.collisionPath);
              }

              // Degrade
              collisionFillPaint.getShader().setLocalMatrix(item.collisionMatrix);
              collisionStrokePaint.getShader().setLocalMatrix(item.collisionMatrix);

              // Cone (remplissage et exterieur)
              canvas.drawPath(item.collisionPath, collisionFillPaint);
              canvas.drawPath(item.collisionPath, collisionStrokePaint);
            }
          }
        }
      }

      // Dessin des balises
      for (final Map.Entry<String, BaliseItem> entry : itemsToDraw.entrySet())
      {
        final BaliseItem baliseItem = entry.getValue();
        // Conversion en pixels
        if (zoomChanged || (baliseItem.x < 0) || (baliseItem.y < 0))
        {
          baliseItem.x = Math.round(MercatorProjection.longitudeToPixelX(baliseItem.getPoint().getLongitude(), lastZoom));
          baliseItem.y = Math.round(MercatorProjection.latitudeToPixelY(baliseItem.getPoint().getLatitude(), lastZoom));
        }
        final int deltaX = (baliseItem.collide && (lastZoom >= ZOOM_LIMITE_COLLISIONS) ? baliseItem.deltaX : 0);
        final int deltaY = (baliseItem.collide && (lastZoom >= ZOOM_LIMITE_COLLISIONS) ? baliseItem.deltaY : 0);
        drawPoint.set((int)(baliseItem.x - left + deltaX), (int)(baliseItem.y - top + deltaY));

        // Si details => on l'affiche tout le temps (en dernier)
        if (((InfosDrawable)baliseItem.getDrawable()).isDrawDetails())
        {
          infosItem = baliseItem;
          infosPoint.set(drawPoint.x, drawPoint.y);
          baliseItem.visible = true;
        }
        else
        {
          // Recuperation de coins de l'item
          final Rect bounds = baliseItem.getDrawable().getDisplayBounds();
          if ((drawPoint.x >= bounds.left) && (drawPoint.y >= bounds.top) && (drawPoint.x <= mapDisplayer.getPixelWidth() + bounds.right) && (drawPoint.y <= mapDisplayer.getPixelHeight() + bounds.bottom))
          {
            if (isItemDisplayable(baliseItem) && isItemCollisionDisplayable(baliseItem))
            {
              baliseItem.getDrawable().draw(canvas, drawPoint);
              baliseItem.visible = true;
            }
          }
          else
          {
            baliseItem.visible = false;
          }
        }
      }
    }
    catch (final ConcurrentModificationException cme)
    {
      // Liste modifiee pendant le dessin : on ne fait rien, car si la liste est modifiee
      // c'est qu'un redessin va etre demande ensuite
      Log.w(getClass().getSimpleName(), "!! CME dans drawBaliseItems(..., [" + itemsToDraw.size() + "], " + left + ", " + top + ", " + zoomChanged + ")");
    }
    finally
    {
      Log.d(getClass().getSimpleName(), "<<< drawBaliseItems()");
    }
  }

  /**
   * 
   * @param context
   * @param sharedPreferences
   */
  public void updatePreferences(final Context context, final SharedPreferences sharedPreferences)
  {
    // Initialisations
    final Resources resources = context.getResources();

    // Layers
    final boolean windLayer = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES, AbstractBalisesPreferencesActivity.CONFIG_DATA_WIND_BALISES_DEFAULT);
    final boolean weatherLayer = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WEATHER_BALISES, Boolean.parseBoolean(resources.getString(R.string.config_map_layers_weather_default)));
    lastBalisesLayer = windLayer || weatherLayer;
    lastSpotsLayer = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES, AbstractBalisesPreferencesActivity.CONFIG_DATA_SITES_DEFAULT);
    lastWebcamsLayer = sharedPreferences.getBoolean(AbstractBalisesPreferencesActivity.CONFIG_DATA_WEBCAMS, AbstractBalisesPreferencesActivity.CONFIG_DATA_WEBCAMS_DEFAULT);

    // MAJ des balises
    final boolean balisesUpdated = BaliseDrawable.updatePreferences(sharedPreferences, context);

    // MAJ des spots
    final boolean spotsUpdated = SpotDrawable.updatePreferences(sharedPreferences, context);

    // MAJ des webcams
    final boolean webcamsUpdated = WebcamDrawable.updatePreferences(sharedPreferences, context);

    // Invalidation selon les changements
    if (balisesUpdated)
    {
      invalidateBalisesDrawables(null);
    }
    if (spotsUpdated)
    {
      invalidateSpotsDrawables();
    }
    if (webcamsUpdated)
    {
      invalidateWebcamsDrawables();
    }

    // Redessin dans tous les cas
    requestRedraw();

    // Affichage ou non des balises inactives et gestion des collisions
    final boolean newDisplayInactive = !sharedPreferences.getBoolean(resources.getString(R.string.config_map_balise_hide_inactive_key), Boolean.parseBoolean(resources.getString(R.string.config_map_balise_hide_inactive_default)));
    final String newMetarSynopCollisions = sharedPreferences.getString(resources.getString(R.string.config_map_metar_synop_collision_key), resources.getString(R.string.config_map_metar_synop_collision_default));
    if ((newDisplayInactive != displayInactive) || !newMetarSynopCollisions.equals(metarSynopCollisions))
    {
      displayInactive = newDisplayInactive;
      metarSynopCollisions = newMetarSynopCollisions;
      manageBalisesProches(context, sharedPreferences);
    }

    // Centrage ou non sur click
    centering = sharedPreferences.getBoolean(resources.getString(R.string.config_map_centering_key), Boolean.parseBoolean(resources.getString(R.string.config_map_centering_default)));
  }

  /**
   * 
   * @param item
   * @return
   */
  private static boolean isItemDisplayable(final OverlayItem<Canvas> item)
  {
    if (!InvalidableDrawable.class.isAssignableFrom(item.getDrawable().getClass()))
    {
      return true;
    }

    return ((InvalidableDrawable)item.getDrawable()).isDrawable();
  }

  /**
   * 
   * @param item
   * @return
   */
  private static boolean isItemCollisionDisplayable(final OverlayItem<Canvas> item)
  {
    return !BaliseItem.class.isAssignableFrom(item.getClass()) || ((BaliseItem)item).collisionDisplayable;
  }

  @Override
  protected boolean isItemClickable(final OverlayItem<Canvas> item)
  {
    return isItemDisplayable(item) && isItemCollisionDisplayable(item);
  }

  /**
   * 
   * @param key
   */
  public void invalidateBalisesDrawables(final String key)
  {
    // Invalidation des drawables pour les balises
    try
    {
      for (final Map.Entry<String, BaliseItem> entry : baliseItems.entrySet())
      {
        final BaliseItem item = entry.getValue();
        if ((key == null) || key.equals(item.providerKey))
        {
          ((InvalidableDrawable)item.getDrawable()).invalidateDrawable();
        }
      }
    }
    catch (final ConcurrentModificationException cme)
    {
      Log.w(getClass().getSimpleName(), "!! CME dans invalidateBalisesDrawables(" + key + ")");
    }
  }

  /**
   * 
   */
  public void invalidateSpotsDrawables()
  {
    // Invalidation des drawables pour les spots
    try
    {
      for (final Map.Entry<String, OverlayItem<Canvas>> entry : spotItems.entrySet())
      {
        final OverlayItem<Canvas> item = entry.getValue();
        ((InvalidableDrawable)item.getDrawable()).invalidateDrawable();
      }
    }
    catch (final ConcurrentModificationException cme)
    {
      Log.w(getClass().getSimpleName(), "!! CME dans invalidateSpotsDrawables()");
    }
  }

  /**
   * 
   */
  public void invalidateWebcamsDrawables()
  {
    // Invalidation des drawables pour les webcams
    try
    {
      for (final Map.Entry<String, OverlayItem<Canvas>> entry : webcamItems.entrySet())
      {
        final OverlayItem<Canvas> item = entry.getValue();
        ((InvalidableDrawable)item.getDrawable()).invalidateDrawable();
      }
    }
    catch (final ConcurrentModificationException cme)
    {
      Log.w(getClass().getSimpleName(), "!! CME dans invalidateWebcamsDrawables()");
    }
  }

  /**
   * 
   * @param listener
   */
  public boolean addItemsTapListener(final ItemsTapListener listener)
  {
    if (!itemsTapListeners.contains(listener))
    {
      return itemsTapListeners.add(listener);
    }

    return false;
  }

  /**
   * 
   * @param listener
   * @return
   */
  public boolean removeItemsTapListener(final ItemsTapListener listener)
  {
    return itemsTapListeners.remove(listener);
  }

  /**
   * 
   * @param tapItems
   */
  private void fireOnItemsTap(final List<OverlayItem<Canvas>> tapItems)
  {
    for (final ItemsTapListener listener : itemsTapListeners)
    {
      listener.onItemsTap(tapItems);
    }
  }

  /**
   * 
   * @param tapItems
   */
  private void fireOnItemsLongTap(final List<OverlayItem<Canvas>> tapItems)
  {
    for (final ItemsTapListener listener : itemsTapListeners)
    {
      listener.onItemsLongTap(tapItems);
    }
  }

  /**
   * 
   * @param listener
   */
  public boolean addBaliseLongTapListener(final BaliseTapListener listener)
  {
    if (!baliseTapListeners.contains(listener))
    {
      return baliseTapListeners.add(listener);
    }

    return false;
  }

  /**
   * 
   * @param listener
   * @return
   */
  public boolean removeBaliseLongTapListener(final BaliseTapListener listener)
  {
    return baliseTapListeners.remove(listener);
  }

  /**
   * 
   * @param providerKey
   * @param idBalise
   */
  private void fireOnBaliseTap(final String providerKey, final String idBalise)
  {
    final BaliseProvider provider = baliseProviders.get(providerKey);
    for (final BaliseTapListener listener : baliseTapListeners)
    {
      listener.onBaliseTap(provider, providerKey, idBalise);
    }
  }

  /**
   * 
   * @param providerKey
   * @param idBalise
   */
  private void fireOnBaliseLongTap(final String providerKey, final String idBalise)
  {
    final BaliseProvider provider = baliseProviders.get(providerKey);
    for (final BaliseTapListener listener : baliseTapListeners)
    {
      listener.onBaliseLongTap(provider, providerKey, idBalise);
    }
  }

  /**
   * 
   * @param providerKey
   * @param idBalise
   */
  private void fireOnBaliseInfoLinkTap(final String providerKey, final String idBalise)
  {
    final BaliseProvider provider = baliseProviders.get(providerKey);
    for (final BaliseTapListener listener : baliseTapListeners)
    {
      listener.onBaliseInfoLinkTap(provider, providerKey, idBalise);
    }
  }

  @Override
  public boolean onDoubleTap(final List<OverlayItem<Canvas>> tapItems)
  {
    return true;
  }

  @Override
  public boolean onLongTap(final List<OverlayItem<Canvas>> tapItems)
  {
    // Un seul item => pas de choix
    if (tapItems.size() == 1)
    {
      return onLongTap(tapItems.get(0));
    }

    // Plusieurs items => choix
    fireOnItemsLongTap(tapItems);
    return true;
  }

  /**
   * 
   * @param item
   * @return
   */
  protected boolean onLongTap(final OverlayItem<Canvas> item)
  {
    // Balises
    if (BaliseItem.class.isAssignableFrom(item.getClass()))
    {
      final BaliseItem baliseItem = (BaliseItem)item;
      fireOnBaliseLongTap(baliseItem.providerKey, baliseItem.baliseId);
      return true;
    }
    // Spots
    else if (SpotItem.class.isAssignableFrom(item.getClass()))
    {
      final SpotItem spotItem = (SpotItem)item;
      fireOnSpotLongTap(spotItem);
      return true;
    }
    // Webcams
    else if (WebcamItem.class.isAssignableFrom(item.getClass()))
    {
      final WebcamItem webcamItem = (WebcamItem)item;
      fireOnWebcamLongTap(webcamItem);
      return true;
    }

    return false;
  }

  /**
   * 
   * @param providerKey
   * @param balises
   */
  public void addBalises(final String providerKey, final List<Balise> balises)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> addBalises(" + providerKey + ", [" + balises.size() + "])");

    // Tache de fond
    final AsyncTask<Object, Void, Void> task = new AsyncTask<Object, Void, Void>()
    {
      @Override
      protected Void doInBackground(final Object... args)
      {
        itemDatabase.beginTransaction();
        try
        {
          // Pour chaque item
          for (final Balise balise : balises)
          {
            ItemDatabaseHelper.replaceBalise(itemDatabase, providerKey, balise);
          }
          itemDatabase.setTransactionSuccessful();
        }
        finally
        {
          itemDatabase.endTransaction();
        }

        // MAJ des balises visibles
        selectorThread.postStep(new ItemsSelectorThread.ItemsSelectorStep(ItemsSelectorThread.ItemsSelectorStep.StepType.BALISES, lastCenter, lastTopLeft, lastBottomRight, lastPixelLatAngle, lastPixelLngAngle, lastBalisesLayer,
            lastSpotsLayer, lastWebcamsLayer));

        return null;
      }
    };
    task.execute();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< addBalises(" + providerKey + ", [" + balises.size() + "])");
  }

  /**
   * 
   * @param first
   * @param second
   * @return
   */
  private List<BaliseItem> findGroupeBalisesProches(final BaliseItem first, final BaliseItem second)
  {
    // Recherche si une des deux balises fait partie d'un groupe 
    for (final List<BaliseItem> groupeBalise : balisesProches)
    {
      if (groupeBalise.contains(first) || groupeBalise.contains(second))
      {
        // On ne cherche pas plus loin
        return groupeBalise;
      }
    }

    // Pas de groupe
    return null;
  }

  /**
   * 
   * @param first
   * @param second
   */
  private void declareBalisesProches(final BaliseItem first, final BaliseItem second)
  {
    // Recherche si une des deux balises appartient deja a un groupe de balises proches
    List<BaliseItem> groupeBalises = findGroupeBalisesProches(first, second);
    if (groupeBalises == null)
    {
      // Si non, creation du groupe
      groupeBalises = new ArrayList<BaliseItem>();
      balisesProches.add(groupeBalises);
    }

    // Ajout des balises au groupe
    if (!groupeBalises.contains(first))
    {
      groupeBalises.add(first);
    }
    if (!groupeBalises.contains(second))
    {
      groupeBalises.add(second);
    }
  }

  /**
   * 
   * @param context
   * @param sharedPreferences
   */
  protected void manageBalisesProches(final Context context, final SharedPreferences sharedPreferences)
  {
    final AsyncTask<Object, Void, Void> task = new AsyncTask<Object, Void, Void>()
    {
      @Override
      protected Void doInBackground(final Object... params)
      {
        privateManageBalisesProches(context, sharedPreferences, true);
        return null;
      }
    };
    task.execute();
  }

  /**
   * 
   * @param context
   * @param sharedPreferences
   * @param redraw
   */
  protected void privateManageBalisesProches(final Context context, final SharedPreferences sharedPreferences, final boolean redraw)
  {
    try
    {
      // Initialisations
      final Resources resources = context.getResources();
      final String[] keys = context.getResources().getStringArray(R.array.providers_keys);
      final String metarKey = keys[1];
      final String synopKey = keys[3];
      final String metarSynopCollision = sharedPreferences.getString(resources.getString(R.string.config_map_metar_synop_collision_key), resources.getString(R.string.config_map_metar_synop_collision_default));
      final String hideCollisionSynopsValue = resources.getString(R.string.config_map_metar_synop_collision_hide_synop);
      final String hideCollisionMetarsValue = resources.getString(R.string.config_map_metar_synop_collision_hide_metar);
      final boolean hideCollisionSynops = metarSynopCollision.equals(hideCollisionSynopsValue);
      final boolean hideCollisionMetars = metarSynopCollision.equals(hideCollisionMetarsValue);

      // RAZ
      balisesProches.clear();
      for (final Map.Entry<String, BaliseItem> entry : baliseItems.entrySet())
      {
        final BaliseItem baliseItem = entry.getValue();
        baliseItem.collide = false;
        baliseItem.collisionDisplayable = true;
      }

      // Inspection de la liste de balises
      final List<OverlayItem<Canvas>> baliseList = new ArrayList<OverlayItem<Canvas>>(baliseItems.values());
      final int fin = baliseItems.size() - 1;
      for (int i = 0; i < fin; i++)
      {
        // Initialisations
        final BaliseItem first = (BaliseItem)baliseList.get(i);

        // Deja marquee comme collision ou proche ?
        // => pas la peine de chercher plus loin
        if (first.collide || !first.collisionDisplayable)
        {
          continue;
        }

        // Recherche de voisines
        for (int j = i + 1; j <= fin; j++)
        {
          // Initialisations
          final BaliseItem second = (BaliseItem)baliseList.get(j);

          if (isItemDisplayable(first) && isItemDisplayable(second))
          {
            final double distance = MercatorProjection.calculateDistance(first.getPoint(), second.getPoint());
            final boolean firstIsMetar = first.providerKey.startsWith(metarKey);
            boolean metarAndSynop = firstIsMetar || second.providerKey.startsWith(metarKey);
            metarAndSynop = metarAndSynop && (first.providerKey.startsWith(synopKey) || second.providerKey.startsWith(synopKey));
            if (metarAndSynop && (distance < 4))
            {
              if (hideCollisionSynops || hideCollisionMetars)
              {
                // Option de cacher METAR ou SYNOP
                final BaliseItem synopItem = (firstIsMetar ? second : first);
                final BaliseItem metarItem = (firstIsMetar ? first : second);
                final BaliseItem itemToHide = (hideCollisionSynops ? synopItem : metarItem);
                itemToHide.collisionDisplayable = false;
              }
              else
              {
                // Ni METAR ni SYNOP cache, mais declares comme proches
                declareBalisesProches(first, second);
              }

              // Next
              continue;
            }

            // Collision entre 2 balises autres que 2 balises METAR/SYNOP
            if (distance < 0.1)
            {
              declareBalisesProches(first, second);
            }
          }
        }
      }

      // Gestion des placements
      for (final List<BaliseItem> groupeBalises : balisesProches)
      {
        // Tri du nord au sud
        final BaliseItem[] tableauBalises = groupeBalises.toArray(new BaliseItem[0]);
        Arrays.sort(tableauBalises, new Comparator<BaliseItem>()
        {
          @Override
          public int compare(final BaliseItem first, final BaliseItem second)
          {
            return second.getPoint().getLatitudeE6() - first.getPoint().getLatitudeE6();
          }
        });

        // Affectation d'un delta a chaque balise
        final int tailleGroupe = tableauBalises.length;
        final double deltaAngle = Math.PI * 2 / tailleGroupe;
        double angle = 0;
        for (final BaliseItem item : tableauBalises)
        {
          // Item
          item.deltaAngle = (float)(angle * 180 / Math.PI);
          item.deltaX = (int)Math.round(rayonCollisions * Math.sin(angle));
          item.deltaY = (int)Math.round(-rayonCollisions * Math.cos(angle));
          if (item.collisionPath == null)
          {
            item.collisionPath = new Path();
          }
          if (item.collisionMatrix == null)
          {
            item.collisionMatrix = new Matrix();
          }
          item.collide = true;

          // Drawable
          ((BaliseDrawable)item.getDrawable()).setCollide(true);
          ((InvalidableDrawable)item.getDrawable()).invalidateDrawable();

          // Next
          angle += deltaAngle;
        }
      }

      // Fin
      if (redraw)
      {
        requestRedraw();
      }
    }
    catch (final ConcurrentModificationException cme)
    {
      Log.w(getClass().getSimpleName(), "#### CME dans privateManageBalisesProches()");
    }
  }

  @Override
  @Deprecated
  public void clearItems()
  {
    super.clearItems();
  }

  @Override
  @Deprecated
  public void addItem(final OverlayItem<Canvas> item)
  {
    super.addItem(item);
  }

  /**
   * 
   * @param providerKey
   */
  public void clearBalises(final String providerKey)
  {
    // Tache de fond
    final AsyncTask<Object, Void, Void> task = new AsyncTask<Object, Void, Void>()
    {
      @Override
      protected Void doInBackground(final Object... args)
      {
        // Suppression en base
        itemDatabase.beginTransaction();
        ItemDatabaseHelper.deleteBalises(itemDatabase, providerKey);
        itemDatabase.setTransactionSuccessful();
        itemDatabase.endTransaction();

        // MAJ des balises visibles
        selectorThread.postStep(new ItemsSelectorThread.ItemsSelectorStep(ItemsSelectorThread.ItemsSelectorStep.StepType.BALISES, lastCenter, lastTopLeft, lastBottomRight, lastPixelLatAngle, lastPixelLngAngle, lastBalisesLayer,
            lastSpotsLayer, lastWebcamsLayer));

        return null;
      }
    };
    task.execute();
  }

  /**
   * 
   * @param providerKey
   */
  protected void removeBaliseProvider(final String providerKey)
  {
    Log.d(getClass().getSimpleName(), ">>> removeBaliseProvider(" + providerKey + ")");

    // Suppression des balises
    clearBalises(providerKey);

    // Retrait du provider
    baliseProviders.remove(providerKey);
  }

  /**
   * 
   */
  protected void removeBaliseProviders()
  {
    // Copie de la liste des clefs (sinon ConcurrentModificationException)
    final String[] providerKeys = baliseProviders.keySet().toArray(new String[0]);

    // Suppression de tous les providers
    for (final String providerKey : providerKeys)
    {
      removeBaliseProvider(providerKey);
    }
  }

  /**
   * 
   * @param providerKey
   */
  public void removeSpotProvider(final String providerKey)
  {
    // Suppression des balises
    clearSpots(providerKey);

    // Retrait du provider
    providerSpotMap.remove(providerKey);
  }

  /**
   * 
   * @param providerKey
   * @param spots
   */
  public void addSpots(final String providerKey, final List<Spot> spots)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> addSpots(" + providerKey + ", [" + spots.size() + "])");

    // Ajout dans la référence des spots
    final Map<String, Spot> spotMap = new HashMap<String, Spot>();
    for (final Spot spot : spots)
    {
      spotMap.put(spot.id, spot);
    }
    providerSpotMap.put(providerKey, spotMap);

    // Tache de fond
    final AsyncTask<Object, Void, Void> task = new AsyncTask<Object, Void, Void>()
    {
      @Override
      protected Void doInBackground(final Object... args)
      {
        itemDatabase.beginTransaction();
        try
        {
          // Pour chaque item
          for (final Spot spot : spots)
          {
            ItemDatabaseHelper.replaceSpot(itemDatabase, providerKey, spot);
          }
          itemDatabase.setTransactionSuccessful();
        }
        finally
        {
          itemDatabase.endTransaction();
        }

        // MAJ des balises visibles
        selectorThread.postStep(new ItemsSelectorThread.ItemsSelectorStep(ItemsSelectorThread.ItemsSelectorStep.StepType.SPOTS, lastCenter, lastTopLeft, lastBottomRight, lastPixelLatAngle, lastPixelLngAngle, lastBalisesLayer,
            lastSpotsLayer, lastWebcamsLayer));

        return null;
      }
    };
    task.execute();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< addSpots(" + providerKey + ", [" + spots.size() + "])");
  }

  /**
   * 
   * @param providerKey
   */
  public void clearSpots(final String providerKey)
  {
    // Tache de fond
    final AsyncTask<Object, Void, Void> task = new AsyncTask<Object, Void, Void>()
    {
      @Override
      protected Void doInBackground(final Object... args)
      {
        // Suppression en base
        itemDatabase.beginTransaction();
        ItemDatabaseHelper.deleteSpots(itemDatabase, providerKey);
        itemDatabase.setTransactionSuccessful();
        itemDatabase.endTransaction();

        // MAJ des balises visibles
        selectorThread.postStep(new ItemsSelectorThread.ItemsSelectorStep(ItemsSelectorThread.ItemsSelectorStep.StepType.SPOTS, lastCenter, lastTopLeft, lastBottomRight, lastPixelLatAngle, lastPixelLngAngle, lastBalisesLayer,
            lastSpotsLayer, lastWebcamsLayer));

        return null;
      }
    };
    task.execute();
  }

  /**
   * 
   * @param listener
   */
  public boolean addSpotLongTapListener(final SpotTapListener listener)
  {
    if (!spotTapListeners.contains(listener))
    {
      return spotTapListeners.add(listener);
    }

    return false;
  }

  /**
   * 
   * @param listener
   * @return
   */
  public boolean removeSpotLongTapListener(final SpotTapListener listener)
  {
    return spotTapListeners.remove(listener);
  }

  /**
   * 
   * @param spotItem
   */
  private void fireOnSpotTap(final SpotItem spotItem)
  {
    for (final SpotTapListener listener : spotTapListeners)
    {
      listener.onSpotTap(spotItem);
    }
  }

  /**
   * 
   * @param spotItem
   */
  private void fireOnSpotLongTap(final SpotItem spotItem)
  {
    for (final SpotTapListener listener : spotTapListeners)
    {
      listener.onSpotLongTap(spotItem);
    }
  }

  /**
   * 
   * @param spotItem
   */
  private void fireOnSpotInfoLinkTap(final SpotItem spotItem)
  {
    for (final SpotTapListener listener : spotTapListeners)
    {
      listener.onSpotInfoLinkTap(spotItem);
    }
  }

  /**
   * 
   * @param listener
   */
  public boolean addWebcamLongTapListener(final WebcamTapListener listener)
  {
    if (!webcamTapListeners.contains(listener))
    {
      return webcamTapListeners.add(listener);
    }

    return false;
  }

  /**
   * 
   * @param listener
   * @return
   */
  public boolean removeWebcamLongTapListener(final WebcamTapListener listener)
  {
    return webcamTapListeners.remove(listener);
  }

  /**
   * 
   * @param webcamItem
   */
  private void fireOnWebcamTap(final WebcamItem webcamItem)
  {
    for (final WebcamTapListener listener : webcamTapListeners)
    {
      listener.onWebcamTap(webcamItem);
    }
  }

  /**
   * 
   * @param webcamItem
   */
  private void fireOnWebcamLongTap(final WebcamItem webcamItem)
  {
    for (final WebcamTapListener listener : webcamTapListeners)
    {
      listener.onWebcamLongTap(webcamItem);
    }
  }

  /**
   * 
   * @param webcamItem
   */
  private void fireOnWebcamInfoLinkTap(final WebcamItem webcamItem)
  {
    for (final WebcamTapListener listener : webcamTapListeners)
    {
      listener.onWebcamInfoLinkTap(webcamItem);
    }
  }

  /**
   * 
   * @param webcamItem
   */
  private void fireOnWebcamImageTap(final WebcamItem webcamItem)
  {
    for (final WebcamTapListener listener : webcamTapListeners)
    {
      listener.onWebcamImageTap(webcamItem);
    }
  }

  @Override
  protected List<OverlayItem<Canvas>> findItemsUnder(final Point point, final int nbMax)
  {
    final List<OverlayItem<Canvas>> foundItems = new ArrayList<OverlayItem<Canvas>>();

    // D'abord les balises
    try
    {
      findBaliseItemsUnder(baliseItems, point, nbMax, foundItems);
    }
    catch (final ConcurrentModificationException cme)
    {
      Log.w(getClass().getSimpleName(), "!! CME dans findBaliseItemUnder()");
    }

    // Puis les spots
    try
    {
      findOverlayItemsUnder(spotItems, point, nbMax, foundItems);
    }
    catch (final ConcurrentModificationException cme)
    {
      Log.w(getClass().getSimpleName(), "!! CME dans findOverlayItemUnder() pour les spots");
    }

    // Puis les webcams
    try
    {
      findOverlayItemsUnder(webcamItems, point, nbMax, foundItems);
    }
    catch (final ConcurrentModificationException cme)
    {
      Log.w(getClass().getSimpleName(), "!! CME dans findOverlayItemUnder() pour les webcams");
    }

    return foundItems;
  }

  /**
   * 
   * @param candidates
   * @param point
   * @param nbMax
   * @param foundItems
   * @throws ConcurrentModificationException
   */
  private final void findBaliseItemsUnder(final Map<String, BaliseItem> candidates, final Point point, final int nbMax, final List<OverlayItem<Canvas>> foundItems) throws ConcurrentModificationException
  {
    // Limite atteinte ?
    int nbFound = foundItems.size();
    if ((nbMax >= 0) && (nbFound >= nbMax))
    {
      return;
    }

    // Initialisations
    final Point itemPoint = new Point();

    // Inspection des items
    for (final Map.Entry<String, BaliseItem> entry : candidates.entrySet())
    {
      final BaliseItem baliseItem = entry.getValue();
      if (baliseItem.getPoint() != null)
      {
        // Coordonnees pixels
        mapDisplayer.getProjection().toPixels(baliseItem.getPoint(), itemPoint);

        // Gestion collision
        if (baliseItem.collide && (lastZoom >= ZOOM_LIMITE_COLLISIONS))
        {
          itemPoint.x += baliseItem.deltaX;
          itemPoint.y += baliseItem.deltaY;
        }

        // Zone toucher
        final Rect bounds = baliseItem.getDrawable().getInteractiveBounds();
        if ((point.x >= itemPoint.x + bounds.left) && (point.x <= itemPoint.x + bounds.right) && (point.y >= itemPoint.y + bounds.top) && (point.y <= itemPoint.y + bounds.bottom))
        {
          if (isItemClickable(baliseItem))
          {
            foundItems.add(baliseItem);
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

  /**
   * 
   * @param candidates
   * @param point
   * @param nbMax
   * @param foundItems
   * @throws ConcurrentModificationException
   */
  private final void findOverlayItemsUnder(final Map<String, ? extends OverlayItem<Canvas>> candidates, final Point point, final int nbMax, final List<OverlayItem<Canvas>> foundItems) throws ConcurrentModificationException
  {
    // Limite atteinte ?
    int nbFound = foundItems.size();
    if ((nbMax >= 0) && (nbFound >= nbMax))
    {
      return;
    }

    // Initialisations
    final Point itemPoint = new Point();

    // Inspection des items
    for (final Map.Entry<String, ? extends OverlayItem<Canvas>> entry : candidates.entrySet())
    {
      final OverlayItem<Canvas> item = entry.getValue();
      if (item.getPoint() != null)
      {
        // Coordonnees pixels
        mapDisplayer.getProjection().toPixels(item.getPoint(), itemPoint);

        // Zone toucher
        final Rect bounds = item.getDrawable().getInteractiveBounds();
        if ((point.x >= itemPoint.x + bounds.left) && (point.x <= itemPoint.x + bounds.right) && (point.y >= itemPoint.y + bounds.top) && (point.y <= itemPoint.y + bounds.bottom))
        {
          if (isItemClickable(item))
          {
            foundItems.add(item);
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

  @Override
  public boolean onKeyPressed(final int keyCode)
  {
    if ((keyCode == KeyEvent.KEYCODE_BACK) && (lastInfoDrawableTaped != null))
    {
      lastInfoDrawableTaped.setDrawDetails(false);
      lastInfoDrawableTaped = null;
      if (lastItemTaped != null)
      {
        lastItemTaped.recycle();
        lastItemTaped = null;
      }

      requestRedraw();

      return true;
    }

    return false;
  }

  /**
   * 
   * @return
   */
  public List<BaliseItem> getVisibleBalises()
  {
    // Initialisations
    final List<BaliseItem> retour = new ArrayList<BaliseItem>();

    synchronized (baliseItems)
    {
      // Pour chaque balise
      for (final Map.Entry<String, BaliseItem> entry : baliseItems.entrySet())
      {
        // Si la balise est visible
        final BaliseItem baliseItem = entry.getValue();
        if (baliseItem.visible)
        {
          retour.add(baliseItem);
        }
      }
    }

    return retour;
  }

  /**
   * 
   * @param analyticsService
   */
  public void setAnalyticsService(final AnalyticsService analyticsService)
  {
    this.analyticsService = analyticsService;
  }

  /**
   * 
   * @param rows
   * @param redraw
   * @param forceRedraw
   * @return
   */
  boolean updateBaliseItems(final Map<String, ItemRow> rows, final boolean redraw, final boolean forceRedraw)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> updateBaliseItems([" + (rows == null ? "null" : "" + rows.size()) + "], " + redraw + ", " + forceRedraw + ")");
    final MapView mapView = (MapView)mapDisplayer;
    final Context context = mapView.getContext();
    final BaliseItem tapedBalise = ((lastItemTaped != null) && BaliseItem.class.isAssignableFrom(lastItemTaped.getClass()) ? (BaliseItem)lastItemTaped : null);

    // Suppression des balises qui ne sont plus visibles
    final int before = baliseItems.size();
    final List<BaliseItem> toDel = new ArrayList<BaliseItem>();
    for (final Entry<String, BaliseItem> entry : baliseItems.entrySet())
    {
      final BaliseItem baliseItem = entry.getValue();
      if ((rows == null) || !rows.containsKey(entry.getKey()))
      {
        toDel.add(entry.getValue());
      }
      else if ((tapedBalise != null) && (tapedBalise.equals(baliseItem)))
      {
        setTapedItem(baliseItem);
      }
    }
    for (final BaliseItem itemToDel : toDel)
    {
      baliseItems.remove(itemToDel.getName());
    }

    // Ajout des balises qui sont devenues visibles
    int toAdd = 0;
    if (rows != null)
    {
      for (final Entry<String, ItemRow> entry : rows.entrySet())
      {
        if (!baliseItems.containsKey(entry.getKey()))
        {
          final ItemRow row = entry.getValue();
          final BaliseProvider provider = baliseProviders.get(row.provider);
          if (provider != null)
          {
            final BaliseDrawable baliseDrawable = BaliseDrawableHelper.newBaliseDrawable(context, row.id, baliseProviders.get(row.provider), mapView);
            baliseDrawable.validateDrawable();
            final BaliseItem baliseItem = new BaliseItem(row.provider, row.id, new GeoPoint(row.latitude, row.longitude), baliseDrawable);
            baliseItems.put(baliseItem.getName(), baliseItem);
            toAdd++;

            if ((tapedBalise != null) && (tapedBalise.equals(baliseItem)))
            {
              setTapedItem(baliseItem);
            }
          }
        }
      }
    }
    final boolean changed = ((toDel.size() + toAdd) > 0);
    Log.d(getClass().getSimpleName(), "balises before/removed/added/total from view : " + before + "/-" + toDel.size() + "/" + toAdd + "/" + baliseItems.size());

    // Gestion des balises proches, rafraichissement appelé dedans si besoin
    if (changed)
    {
      privateManageBalisesProches(context, ActivityCommons.getSharedPreferences(context), (redraw || forceRedraw));
    }
    // Sinon seulement rafraichissement si demandé
    else if (forceRedraw || redraw)
    {
      // Rafraichissement
      requestRedraw();
    }

    return changed;
  }

  /**
   * 
   * @param rows
   * @param redraw
   * @param forceRedraw
   * @return
   */
  boolean updateSpotItems(final Map<String, ItemRow> rows, final boolean redraw, final boolean forceRedraw)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> updateSpotItems([" + (rows == null ? "null" : "" + rows.size()) + "], " + redraw + ", " + forceRedraw + ")");
    final MapView mapView = (MapView)mapDisplayer;
    final Context context = mapView.getContext();
    final Resources resources = context.getResources();
    final SpotItem tapedSpot = ((lastItemTaped != null) && SpotItem.class.isAssignableFrom(lastItemTaped.getClass()) ? (SpotItem)lastItemTaped : null);

    // Suppression des spots qui ne sont plus visibles
    final int before = spotItems.size();
    final List<OverlayItem<Canvas>> toDel = new ArrayList<OverlayItem<Canvas>>();
    for (final Entry<String, OverlayItem<Canvas>> entry : spotItems.entrySet())
    {
      final SpotItem spotItem = (SpotItem)entry.getValue();
      if ((rows == null) || !rows.containsKey(entry.getKey()))
      {
        toDel.add(entry.getValue());
      }
      else if ((tapedSpot != null) && (tapedSpot.equals(spotItem)))
      {
        setTapedItem(spotItem);
      }
    }
    for (final OverlayItem<Canvas> itemToDel : toDel)
    {
      spotItems.remove(itemToDel.getName());
    }

    // Ajout des spots qui sont devenus visibles
    int toAdd = 0;
    if (rows != null)
    {
      for (final Entry<String, ItemRow> entry : rows.entrySet())
      {
        if (!spotItems.containsKey(entry.getKey()))
        {
          final ItemRow row = entry.getValue();
          final Spot spot = providerSpotMap.get(row.provider).get(row.id);
          final String informations = ((spot.infos == null) || (spot.infos.size() == 0) ? null : resources.getString(R.string.menu_context_map_infos_spot));
          final SpotDrawable spotDrawable = new SpotDrawable(spot, mapView, informations);
          spotDrawable.validateDrawable();
          final SpotItem spotItem = new SpotItem(row.provider, spot, new GeoPoint(row.latitude, row.longitude), spotDrawable);
          spotItems.put(spotItem.getName(), spotItem);
          toAdd++;

          if ((tapedSpot != null) && (tapedSpot.equals(spotItem)))
          {
            setTapedItem(spotItem);
          }
        }
      }
    }
    final boolean changed = ((toDel.size() + toAdd) > 0);
    Log.d(getClass().getSimpleName(), "spots before/removed/added/total from view : " + before + "/-" + toDel.size() + "/" + toAdd + "/" + spotItems.size());

    // Rafraichissement si besoin
    if (forceRedraw || (redraw && changed))
    {
      requestRedraw();
    }

    return changed;
  }

  /**
   * 
   * @param rows
   * @param redraw
   * @param forceRedraw
   * @return
   */
  boolean updateWebcamItems(final Map<String, WebcamRow> rows, final boolean redraw, final boolean forceRedraw)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> updateWebcamItems([" + (rows == null ? "null" : "" + rows.size()) + "], " + redraw + ", " + forceRedraw + ")");
    final MapView mapView = (MapView)mapDisplayer;
    final Context context = mapView.getContext();
    final Resources resources = context.getResources();
    final WebcamItem tapedWebcam = ((lastItemTaped != null) && WebcamItem.class.isAssignableFrom(lastItemTaped.getClass()) ? (WebcamItem)lastItemTaped : null);

    // Suppression des webcams qui ne sont plus visibles
    final int before = webcamItems.size();
    final List<OverlayItem<Canvas>> toDel = new ArrayList<OverlayItem<Canvas>>();
    for (final Entry<String, OverlayItem<Canvas>> entry : webcamItems.entrySet())
    {
      final WebcamItem webcamItem = (WebcamItem)entry.getValue();
      if ((rows == null) || !rows.containsKey(entry.getKey()))
      {
        toDel.add(entry.getValue());
      }
      else if ((tapedWebcam != null) && (tapedWebcam.equals(webcamItem)))
      {
        setTapedItem(webcamItem);
      }
    }
    for (final OverlayItem<Canvas> itemToDel : toDel)
    {
      webcamItems.remove(itemToDel.getName());
    }

    // Ajout des webcams qui sont devenues visibles
    int toAdd = 0;
    if (rows != null)
    {
      for (final Entry<String, WebcamRow> entry : rows.entrySet())
      {
        if (!webcamItems.containsKey(entry.getKey()))
        {
          final WebcamRow row = entry.getValue();
          final GeoPoint point = new GeoPoint(row.latitude, row.longitude);
          final WebcamDrawable webcamDrawable = new WebcamDrawable(row, (MapView)this.mapDisplayer, resources.getString(R.string.menu_context_map_webcam_webmap));
          webcamDrawable.validateDrawable();
          final WebcamItem webcamItem = new WebcamItem(row, point, webcamDrawable);
          webcamItems.put(webcamItem.getName(), webcamItem);
          toAdd++;

          if ((tapedWebcam != null) && (tapedWebcam.equals(webcamItem)))
          {
            setTapedItem(webcamItem);

            // Téléchargement image
            final WebcamDownloadThread.WebcamDownloadStep step = new WebcamDownloadThread.WebcamDownloadStep(webcamItem.row);
            webcamDownloadThread.postStep(step);
          }
        }
      }
    }
    final boolean changed = (toDel.size() + toAdd > 0);
    Log.d(getClass().getSimpleName(), "webcams before/removed/added/total from view : " + before + "/-" + toDel.size() + "/" + toAdd + "/" + webcamItems.size());

    // Rafraichissement
    if (forceRedraw || (redraw && changed))
    {
      requestRedraw();
    }

    return changed;
  }

  @Override
  public void onShutdown()
  {
    // Thread selector
    selectorThread.interrupt();
    ThreadUtils.join(selectorThread);

    // Thread webcam
    webcamDownloadThread.interrupt();
    ThreadUtils.join(webcamDownloadThread);

    // Fermeture base de donnée des items
    if (itemDatabase != null)
    {
      itemDatabase.close();
      itemDatabase = null;
    }

    // Super
    super.onShutdown();
  }

  @Override
  public void onMoveInputFinished(final GeoPoint center, final GeoPoint topLeft, final GeoPoint bottomRight, final double pixelLatAngle, final double pixelLngAngle)
  {
    Log.d(getClass().getSimpleName(), ">>> onMoveInputFinished(..., " + topLeft + ", " + bottomRight + ", " + pixelLatAngle + ", " + pixelLngAngle);
    lastCenter = center;
    lastTopLeft = topLeft;
    lastBottomRight = bottomRight;
    lastPixelLatAngle = pixelLatAngle;
    lastPixelLngAngle = pixelLngAngle;

    if (selectorThread.isAlive())
    {
      selectorThread.postStep(new ItemsSelectorThread.ItemsSelectorStep(ItemsSelectorThread.ItemsSelectorStep.StepType.TOUT, center, topLeft, bottomRight, pixelLatAngle, pixelLngAngle, lastBalisesLayer, lastSpotsLayer, lastWebcamsLayer));
    }
  }

  /**
   * 
   */
  public void onLayersChanged(final boolean balisesLayer, final boolean spotsLayer, final boolean webcamsLayer)
  {
    Log.d(getClass().getSimpleName(), ">>> onLayersChanged(" + balisesLayer + ", " + spotsLayer + ", " + webcamsLayer + ")");

    lastBalisesLayer = balisesLayer;
    lastSpotsLayer = spotsLayer;
    lastWebcamsLayer = webcamsLayer;
    selectorThread.postStep(new ItemsSelectorThread.ItemsSelectorStep(ItemsSelectorThread.ItemsSelectorStep.StepType.TOUT, lastCenter, lastTopLeft, lastBottomRight, lastPixelLatAngle, lastPixelLngAngle, balisesLayer, spotsLayer,
        webcamsLayer));

    Log.d(getClass().getSimpleName(), "<<< onLayersChanged()");
  }

  /**
   * 
   * @return
   */
  MapDisplayer<Bitmap, Canvas, ?> getMapDisplayer()
  {
    return mapDisplayer;
  }

  /**
   * 
   * @param image
   */
  void onWebcamImageDownloaded(final Bitmap image)
  {
    Log.d(getClass().getSimpleName(), "onWebcamImageDownloaded(" + image + "), lastItemTaped=" + lastItemTaped);
    if ((lastItemTaped != null) && (WebcamItem.class.isAssignableFrom(lastItemTaped.getClass())))
    {
      Log.d(getClass().getSimpleName(), "onWebcamImageDownloaded, updating image");
      // Retaillage à la bonne dimension
      final InfosDrawable infoDrawable = lastInfoDrawableTaped;
      final DrawableInfo imageInfo = infoDrawable.infos.get(1);
      imageInfo.image = Bitmap.createScaledBitmap(image, imageInfo.imageWidth, imageInfo.imageHeight, true);
      imageInfo.imageError = null;
      requestRedraw();
    }
  }

  /**
   * 
   * @param url
   * @param message
   */
  void onWebcamImageDownloadError(final String url, final String message)
  {
    Log.w(getClass().getSimpleName(), "Erreur de téléchargement de l'image " + url + " : " + message);
    if ((lastItemTaped != null) && (WebcamItem.class.isAssignableFrom(lastItemTaped.getClass())))
    {
      final WebcamItem webcamItem = (WebcamItem)lastItemTaped;
      final InfosDrawable infoDrawable = (InfosDrawable)webcamItem.getDrawable();
      final DrawableInfo imageInfo = infoDrawable.infos.get(1);
      imageInfo.image = null;
      imageInfo.imageError = message;
      requestRedraw();
    }
  }

  /**
   * 
   * @param key
   */
  @SuppressWarnings("unused")
  public void onWebcamsUpdate(final String key)
  {
    selectorThread.postStep(new ItemsSelectorThread.ItemsSelectorStep(ItemsSelectorThread.ItemsSelectorStep.StepType.WEBCAMS, lastCenter, lastTopLeft, lastBottomRight, lastPixelLatAngle, lastPixelLngAngle, lastBalisesLayer, lastSpotsLayer,
        lastWebcamsLayer));
  }
}
