package org.pedro.android.mobibalises_common.map;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.map.AbstractSimpleOverlay;
import org.pedro.map.GeoPoint;
import org.pedro.map.Point;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;

/**
 * 
 * @author pedro.m
 *
 */
public final class ProviderMessageOverlay extends AbstractSimpleOverlay<Bitmap, Canvas>
{
  private static final int                PADDING_X       = 3;
  private static final int                PADDING_Y       = 2;

  private static float                    TEXT_SIZE;
  private static int                      WARNING_SIZE;
  private static int                      WARNING_TOUCH_SIZE;

  private AbstractBalisesMapActivity      mapActivity;

  private final Paint                     textPaint;
  private final Rect                      bounds          = new Rect();
  private int                             deltaX;
  private int                             deltaY;

  private final Paint                     okTextPaint;
  private final Paint                     warningPaint;
  private final Paint                     warningTextPaint;
  private final Paint                     errorPaint;
  private final Paint                     errorTextPaint;
  private final Path                      warningPath;

  private ActivityCommons.ProvidersStatus providersStatus = ActivityCommons.ProvidersStatus.OK;

  /**
   * 
   * @param mapActivity
   */
  public ProviderMessageOverlay(final AbstractBalisesMapActivity mapActivity)
  {
    // Initialisations
    super(mapActivity.getMapView());
    this.mapActivity = mapActivity;
    final Resources resources = mapActivity.getResources();

    // Initialisations communes
    DrawingCommons.initialize(mapActivity.getApplicationContext());

    // Tailles
    TEXT_SIZE = DrawingCommons.TEXT_SIZE;
    WARNING_SIZE = Math.round(TEXT_SIZE * 1.5f);
    WARNING_TOUCH_SIZE = Math.round(TEXT_SIZE * 3f);

    // Paint du texte
    textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    textPaint.setTextSize(TEXT_SIZE);
    textPaint.setColor(Color.WHITE);

    // Paint du fond de texte OK
    okTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    okTextPaint.setColor(resources.getColor(R.color.map_status_ok_text_background));
    okTextPaint.setStyle(Style.FILL);

    // Paint du texte de warning
    warningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    warningTextPaint.setColor(resources.getColor(R.color.map_status_warning_text_background));
    warningTextPaint.setStyle(Style.FILL);

    // Paint de l'icone de warning
    warningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    warningPaint.setColor(resources.getColor(R.color.map_status_warning_background));
    warningPaint.setStyle(Style.FILL);

    // Paint du texte d'erreur
    errorTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    errorTextPaint.setColor(resources.getColor(R.color.map_status_error_text_background));
    errorTextPaint.setStyle(Style.FILL);

    // Paint de l'icone d'erreur
    errorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    errorPaint.setColor(resources.getColor(R.color.map_status_error_background));
    errorPaint.setStyle(Style.FILL);

    // Icone d'erreur / warning
    warningPath = new Path();
    warningPath.moveTo(0, 0);
    warningPath.lineTo(WARNING_SIZE, 0);
    warningPath.lineTo(0, WARNING_SIZE);
    warningPath.lineTo(0, 0);
    warningPath.setLastPoint(0, 0);

    // Premier statut
    providersStatus = ActivityCommons.getProvidersStatus(mapActivity, null);
  }

  /**
   * 
   * @param providerName
   */
  @SuppressWarnings("unused")
  public void addProviderName(final String providerName)
  {
    synchronized (ActivityCommons.statusMessageLock)
    {
      // Calculs graphiques
      calculateBounds();

      // Erreur ?
      providersStatus = ActivityCommons.getProvidersStatus(mapActivity, null);
    }
  }

  /**
   * 
   * @param providerKey
   * @param providerName
   * @param balisesException
   * @param relevesException
   * @param paused
   */
  @SuppressWarnings("unused")
  public void removeProviderName(final String providerKey, final String providerName, final Throwable balisesException, final Throwable relevesException, final boolean paused)
  {
    synchronized (ActivityCommons.statusMessageLock)
    {
      // Calculs graphiques
      calculateBounds();

      // Erreur ?
      providersStatus = ActivityCommons.getProvidersStatus(mapActivity, null);
    }
  }

  /**
   * 
   */
  private void calculateBounds()
  {
    final String message = ActivityCommons.getStatusMessage();
    if (message != null)
    {
      textPaint.getTextBounds(message, 0, message.length(), bounds);
      deltaX = -bounds.left;
      deltaY = -bounds.top;
      bounds.offset(deltaX, deltaY);
      bounds.right += 2 * PADDING_X;
      bounds.bottom += 2 * PADDING_Y;
    }
  }

  @Override
  public void draw(final Canvas canvas)
  {
    // Selon le statut
    final Paint finalTextPaint;
    switch (providersStatus)
    {
      case ERROR:
        finalTextPaint = errorTextPaint;
        canvas.drawPath(warningPath, errorPaint);
        break;
      case WARNING:
        finalTextPaint = warningTextPaint;
        canvas.drawPath(warningPath, warningPaint);
        break;
      case OK:
      default:
        finalTextPaint = okTextPaint;
        break;
    }

    // Message
    final String statusMessage = ActivityCommons.getStatusMessage();
    if (statusMessage != null)
    {
      // Dessin
      canvas.drawRect(bounds, finalTextPaint);
      canvas.drawText(statusMessage, deltaX + PADDING_X, deltaY + PADDING_Y, textPaint);
    }
  }

  @Override
  public boolean needsLateDraw()
  {
    return true;
  }

  @Override
  public boolean onDoubleTap(final GeoPoint geoPoint, final Point point)
  {
    return false;
  }

  @Override
  public boolean onLongTap(final GeoPoint geoPoint, final Point point)
  {
    return false;
  }

  @Override
  public boolean onTap(final GeoPoint geoPoint, final Point point)
  {
    // Warning
    if (((providersStatus == ActivityCommons.ProvidersStatus.ERROR) || (providersStatus == ActivityCommons.ProvidersStatus.WARNING)) && (point.x <= WARNING_TOUCH_SIZE) && (point.y <= WARNING_TOUCH_SIZE))
    {
      mapActivity.dataInfos();
      return true;
    }

    return false;
  }

  @Override
  public void onShutdown()
  {
    // Super
    super.onShutdown();

    // Divers
    mapActivity = null;
  }

  @Override
  public void onMoveInputFinished(final GeoPoint inCenter, final GeoPoint topLeft, final GeoPoint bottomRight, final double pixelLatAngle, final double pixelLngAngle)
  {
    // Nothing
  }
}
