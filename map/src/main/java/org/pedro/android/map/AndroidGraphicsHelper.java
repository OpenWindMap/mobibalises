package org.pedro.android.map;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import org.pedro.map.GraphicsHelper;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
 * 
 * @author pedro.m
 */
public final class AndroidGraphicsHelper implements GraphicsHelper<Bitmap, Canvas, Integer>
{
  private static final String         STRING_NULL       = "null";
  private static final char           POINT             = '.';
  private static final char           SLASH             = '/';
  private static final String         RAW               = "raw";

  private final Paint                 backgroundPaint;
  private Context                     context;
  private final Resources             resources;

  private final LinkedList<Matrix>    matrixPool        = new LinkedList<Matrix>();

  private final Bitmap.Config         alphaBitmapConfig;
  private final BitmapFactory.Options tileBitmapOptions = new BitmapFactory.Options();

  /**
   * 
   * @param context
   * @param lightMode
   */
  public AndroidGraphicsHelper(final Context context, final boolean lightMode)
  {
    backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    backgroundPaint.setStyle(Style.FILL);
    this.context = context;
    this.resources = context.getResources();

    alphaBitmapConfig = lightMode ? Bitmap.Config.ARGB_4444 : Bitmap.Config.ARGB_8888;
    tileBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
  }

  @Override
  public void fillImageData(final Canvas canvas, final Integer color, final int width, final int height)
  {
    backgroundPaint.setColor(color.intValue());
    canvas.drawRect(0, 0, width, height, backgroundPaint);
  }

  @Override
  public void fillImageData(final Canvas canvas, final Integer color, final int left, final int top, final int width, final int height)
  {
    backgroundPaint.setColor(color.intValue());
    canvas.drawRect(left, top, width, height, backgroundPaint);
  }

  @Override
  public void setTransparent(final Bitmap image)
  {
    image.eraseColor(Color.TRANSPARENT);
  }

  @Override
  public void drawImageData(final Canvas canvas, final Bitmap image, final int x, final int y)
  {
    if (image == null)
    {
      return;
    }
    if (image.isRecycled())
    {
      return;
    }

    canvas.drawBitmap(image, x, y, null);
  }

  @Override
  public void drawText(final Canvas canvas, final String text, final int x, final int y, final Integer color)
  {
    final String finalText = (text == null ? STRING_NULL : text);
    final Paint paint = new Paint();
    paint.setColor(color.intValue());
    canvas.drawText(finalText, x, y, paint);
  }

  @Override
  public void drawLine(final Canvas canvas, final int left, final int top, final int right, final int bottom, final Integer color)
  {
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(color.intValue());
    canvas.drawLine(left, top, right, bottom, paint);
  }

  @Override
  public void intermediateZoom(final Canvas canvas, final Bitmap image, final float zoom, final int x, final int y)
  {
    final Matrix matrix = getMatrix();
    matrix.postScale(zoom, zoom, x, y);

    canvas.drawBitmap(image, matrix, null);

    recycleMatrix(matrix);
  }

  @Override
  public void zoomIn(final Canvas canvas, final Bitmap image, final int zoom, final int x, final int y)
  {
    final float factor = (float)Math.pow(2, zoom);
    final int centerX = image.getWidth() / 2;
    final int centerY = image.getHeight() / 2;

    final Matrix matrix = getMatrix();
    matrix.postScale(factor, factor, centerX, centerY);
    matrix.postTranslate(x, y);

    canvas.drawBitmap(image, matrix, null);

    recycleMatrix(matrix);
  }

  @Override
  public void zoomOut(final Canvas canvas, final Bitmap image, final int zoom, final int x, final int y)
  {
    final float powZoom = (float)Math.pow(2, zoom);
    final float factor = 1.0f / powZoom;
    final int newWidth = image.getWidth() / (int)powZoom;
    final int newHeight = image.getHeight() / (int)powZoom;
    final int centerX = image.getWidth() / 2;
    final int centerY = image.getHeight() / 2;

    if ((newWidth < 5) || (newHeight < 5))
    {
      // Trop petit (trop dezoome, ca sert a pas grand chose) !
      return;
    }

    final Matrix matrix = getMatrix();
    matrix.postScale(factor, factor, centerX, centerY);
    matrix.postTranslate(x, y);

    canvas.drawBitmap(image, matrix, null);

    recycleMatrix(matrix);
  }

  /**
   * 
   * @return
   */
  private Matrix getMatrix()
  {
    synchronized (matrixPool)
    {
      if (matrixPool.size() > 0)
      {
        final Matrix matrix = matrixPool.removeFirst();
        matrix.reset();
        return matrix;
      }
    }

    return new Matrix();
  }

  /**
   * 
   * @param matrix
   */
  private void recycleMatrix(final Matrix matrix)
  {
    synchronized (matrixPool)
    {
      matrixPool.addFirst(matrix);
    }
  }

  /**
   * 
   * @param width
   * @param height
   * @param config
   * @return
   */
  private static Bitmap newImageData(final int width, final int height, final Bitmap.Config config)
  {
    try
    {
      return Bitmap.createBitmap(width, height, config);
    }
    catch (final OutOfMemoryError oome)
    {
      final OutOfMemoryError newOne = new OutOfMemoryError(oome.getMessage() + " for " + width + "/" + height + " pixels");
      newOne.setStackTrace(oome.getStackTrace());
      throw newOne;
    }
  }

  @Override
  public Bitmap newAlphaImageData(final int width, final int height)
  {
    return newImageData(width, height, alphaBitmapConfig);
  }

  @Override
  public void freeImageData(final Bitmap imageData)
  {
    if (imageData != null)
    {
      imageData.recycle();
    }
  }

  @Override
  public Canvas getDrawer(final Bitmap image)
  {
    return new Canvas(image);
  }

  @Override
  public Integer newColor(final int red, final int green, final int blue)
  {
    return Integer.valueOf(Color.rgb(red, green, blue));
  }

  @Override
  public Integer newColor(final int red, final int green, final int blue, final int alpha)
  {
    return Integer.valueOf(Color.argb(alpha, red, green, blue));
  }

  @Override
  public Bitmap getTileImageData(final URL url, final int width, final int height) throws IOException
  {
    // Initialisations
    Bitmap retour = null;

    // Dans les resources android d'abord, on prend le nom seul de la resource
    final String urlString = url.toString();
    final int lastSlash = urlString.lastIndexOf(SLASH);
    if (lastSlash >= 0)
    {
      // Nom seul de la resource (ex : "http://www.net/MonImage.png" => "MonImage.png")
      String resourceName = urlString.substring(lastSlash + 1);

      // Sans l'extension (ex : "MonImage.png" => "MonImage")
      final int lastPoint = resourceName.indexOf(POINT);
      if (lastPoint > 0)
      {
        resourceName = resourceName.substring(0, lastPoint);
      }

      // En minuscules (ex : "MonImage" => "monimage"
      resourceName = resourceName.toLowerCase();

      // Recherche de l'ID
      final int id = resources.getIdentifier(resourceName, RAW, context.getPackageName());
      if (id > 0)
      {
        // ID trouve, on recupere le Drawable...
        final Drawable drawable = resources.getDrawable(id);
        if (drawable != null)
        {
          // Et le Bitmap
          retour = ((BitmapDrawable)drawable).getBitmap();
        }
      }
    }

    // Sinon a partir de l'URL
    if (retour == null)
    {
      retour = BitmapFactory.decodeStream(url.openStream(), null, tileBitmapOptions);
    }

    // Redimensionnement si besoin
    if ((retour != null) && (width > 0) && (height > 0))
    {
      retour = Bitmap.createScaledBitmap(retour, width, height, false);
    }

    return retour;
  }

  @Override
  public Bitmap getTileImageData(final byte[] buffer, final int length) throws IOException
  {
    return BitmapFactory.decodeByteArray(buffer, 0, length, tileBitmapOptions);
  }

  @Override
  public void onShutdown()
  {
    // Divers
    context = null;
  }
}
