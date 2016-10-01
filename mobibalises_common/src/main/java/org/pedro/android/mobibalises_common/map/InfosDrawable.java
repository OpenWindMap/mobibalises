package org.pedro.android.mobibalises_common.map;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.view.BaliseDrawable;
import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.map.Point;
import org.pedro.spots.Utils;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.FloatMath;

/**
 * 
 * @author pedro.m
 */
public abstract class InfosDrawable
{
  private static boolean                 INFOS_INITIALIZED          = false;
  private static final Object            INFOS_INITIALIZED_LOCK     = new Object();

  // =========================== Infos
  protected static Paint                 PAINT_INFOS_TEXTE_LEFT;
  protected static Paint                 PAINT_INFOS_TEXTE_RIGHT;
  protected static Paint                 PAINT_INFOS_TEXTE_CENTER;
  protected static Paint                 PAINT_INFOS_TEXTE_TITRE_LEFT;
  protected static Paint                 PAINT_INFOS_TEXTE_TITRE_CENTER;
  protected static Paint                 PAINT_INFOS_REMPLISSAGE;
  protected static Paint                 PAINT_INFOS_CONTOUR;
  protected static Paint                 PAINT_TOUCHABLE_TITLE;
  protected static Paint                 PAINT_TENDANCE_FAIBLE;
  protected static Paint                 PAINT_TENDANCE_FORTE;
  protected static Paint                 PAINT_IMAGE_ERROR;
  protected static float                 INTERLIGNE                 = 3;
  protected static float                 ESPACEMENT_INFO            = 20;
  protected static float                 ESPACEMENT_INFO_TENDANCE   = 5;
  protected static float                 LARGEUR_TENDANCE           = 15;
  protected static float                 MARGE_X                    = 10;
  protected static float                 MARGE_Y                    = 10;
  protected static float                 RAYON_INFOS                = 12;
  protected static float                 RAYON_FLECHE               = 7;
  protected static float                 DELTA_X;
  protected static float                 DELTA_Y;
  protected static float                 EPAISSEUR_FLECHE_BOX       = 20;
  protected static float                 EPAISSEUR_CONTOUR          = 5;
  protected static float                 LARGEUR_INFOBULLE_MINIMALE = 125;

  protected static boolean               MAP_CENTERING;
  private static float                   scalingFactor;

  // =========================== Afficheur
  protected final WeakReference<MapView> mapView;

  // =========================== Infos
  protected boolean                      drawDetails;
  protected boolean                      computeDetailsPosition;
  protected final List<DrawableInfo>     infos                      = new ArrayList<DrawableInfo>(16);
  protected float                        maxInfoTitleWidth;
  protected float                        maxInfoValueWidth;
  protected float                        maxInfoTendancieWidth;
  protected final Rect                   titleBounds                = new Rect();
  protected final Rect                   boxBounds                  = new Rect();
  protected final Point                  boxRelativePoint           = new Point();
  private final RectF                    boxRect                    = new RectF();
  protected final Path                   boxFlechePath              = new Path();
  protected int                          boxLeft;
  protected int                          boxTop;
  protected final String                 touchableTitle;
  protected final Rect                   touchableBox               = new Rect();
  protected final Rect                   imageBox                   = new Rect();

  /**
   * 
   * @author pedro.m
   */
  public static class DrawableInfo
  {
    String  title;
    String  value;
    String  tendancie;
    Paint   tendanciePaint;
    boolean span   = false;
    boolean center = false;
    String  imageUrl;
    Bitmap  image;
    String  imageError;
    int     imageWidth;
    int     imageHeight;

    /**
     * 
     * @param title
     * @param value
     */
    public DrawableInfo(final String title, final String value)
    {
      this(title, value, null, null, false, false, null, -1, -1);
    }

    /**
     * 
     * @param title
     * @param value
     * @param tendancie
     * @param tendanciePaint
     */
    public DrawableInfo(final String title, final String value, final String tendancie, final Paint tendanciePaint)
    {
      this(title, value, tendancie, tendanciePaint, false, false, null, -1, -1);
    }

    /**
     * 
     * @param title
     * @param span
     * @param center
     */
    public DrawableInfo(final String title, final boolean span, final boolean center)
    {
      this(title, null, null, null, span, center, null, -1, -1);
    }

    /**
     * 
     * @param imageUrl
     * @param imageWidth
     * @param imageHeight
     */
    public DrawableInfo(final String imageUrl, final int imageWidth, final int imageHeight)
    {
      this(null, null, null, null, false, false, imageUrl, imageWidth, imageHeight);
    }

    /**
     * 
     * @param title
     * @param value
     * @param tendancie
     * @param tendanciePaint
     * @param span
     * @param center
     * @param imageUrl
     * @param imageWidth
     * @param imageHeight
     */
    private DrawableInfo(final String title, final String value, final String tendancie, final Paint tendanciePaint, final boolean span, final boolean center, final String imageUrl, final int imageWidth, final int imageHeight)
    {
      this.title = title;
      this.value = value;
      this.tendancie = tendancie;
      this.tendanciePaint = tendanciePaint;
      this.span = span;
      this.center = center;
      this.imageUrl = imageUrl;
      this.image = null;
      this.imageError = null;
      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;
    }
  }

  /**
   * 
   * @param mapView
   * @param touchableTitle
   */
  protected InfosDrawable(final MapView mapView, final String touchableTitle)
  {
    this.mapView = new WeakReference<MapView>(mapView);
    this.touchableTitle = touchableTitle;
  }

  /**
   * 
   * @param resources
   * @param inScalingFactor
   */
  protected static void initInfosGraphics(final Resources resources, final float inScalingFactor)
  {
    // Initialisation une seule fois
    synchronized (INFOS_INITIALIZED_LOCK)
    {
      if (INFOS_INITIALIZED)
      {
        return;
      }

      // Initialisations
      scalingFactor = inScalingFactor;
      INTERLIGNE *= inScalingFactor;
      ESPACEMENT_INFO *= inScalingFactor;
      ESPACEMENT_INFO_TENDANCE *= inScalingFactor;
      LARGEUR_TENDANCE *= inScalingFactor;
      MARGE_X *= inScalingFactor;
      MARGE_Y *= inScalingFactor;
      RAYON_INFOS *= inScalingFactor;
      RAYON_FLECHE *= inScalingFactor;
      DELTA_X = Math.round(DrawingCommons.WIND_ICON_FLECHE_MAX * inScalingFactor * 0.7f);
      DELTA_Y = Math.round(DrawingCommons.WIND_ICON_FLECHE_MAX * inScalingFactor * 0.7f);
      EPAISSEUR_CONTOUR *= inScalingFactor;
      LARGEUR_INFOBULLE_MINIMALE *= inScalingFactor;
      final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setTextSize(DrawingCommons.TEXT_SIZE);

      // Remplissage
      paint.setColor(resources.getColor(R.color.map_info_background));
      paint.setStyle(Paint.Style.FILL);
      PAINT_INFOS_REMPLISSAGE = new Paint(paint);

      // Contour
      paint.setColor(resources.getColor(R.color.map_info_stroke));
      paint.setStrokeWidth(EPAISSEUR_CONTOUR);
      paint.setStyle(Paint.Style.STROKE);
      PAINT_INFOS_CONTOUR = new Paint(paint);

      // Texte
      paint.setTypeface(Typeface.create((Typeface)null, Typeface.NORMAL));
      paint.setStyle(Paint.Style.FILL);
      paint.setTextAlign(Paint.Align.LEFT);
      paint.setColor(resources.getColor(R.color.map_info_text));
      PAINT_INFOS_TEXTE_LEFT = new Paint(paint);
      paint.setTextAlign(Paint.Align.RIGHT);
      PAINT_INFOS_TEXTE_RIGHT = new Paint(paint);
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setTextSize(DrawingCommons.TEXT_SIZE * 0.8f);
      PAINT_INFOS_TEXTE_CENTER = new Paint(paint);

      paint.setTextSize(DrawingCommons.TEXT_SIZE);
      paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.BOLD));
      paint.setStrokeWidth(1);
      paint.setTextAlign(Paint.Align.LEFT);
      PAINT_INFOS_TEXTE_TITRE_LEFT = new Paint(paint);

      paint.setTextAlign(Paint.Align.CENTER);
      PAINT_INFOS_TEXTE_TITRE_CENTER = new Paint(paint);

      paint.setUnderlineText(true);
      paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.ITALIC));
      PAINT_TOUCHABLE_TITLE = new Paint(paint);

      PAINT_TENDANCE_FAIBLE = new Paint(PAINT_INFOS_TEXTE_LEFT);

      // Les tendances sur la boite d'info de la balise sont dessinees avec des caracteres
      // "speciaux" (BaliseDrawable.DELTA_FORTE_BAISSE par exemple).
      // La taille de ces caracteres n'est pas identique a celle des autres.
      // Au debut un coeff multiplicateur de 1.75 allait bien pour afficher tout a peu pres a la meme taille.
      // Mais depuis (il semblerait) Android 4.4.2, sur certains terminaux ce coefficient n'est pas
      // correct, rendant les fleches de tendance trop grandes.
      // => ce petit calcul ci dessous permet d'ajuster la TextSize du Paint des tendances en fonction
      // du terminal
      final Rect bounds = new Rect();
      PAINT_TENDANCE_FAIBLE.getTextBounds(BaliseDrawable.DELTA_FORTE_BAISSE, 0, 1, bounds);
      final float dy = Math.abs(bounds.top - bounds.bottom);
      final float constante = 14f / 17f; // =0.8235.... le rapport voulu entre taille effective du car. et la taille de la font
      final float tendanceCoeff = constante * PAINT_TENDANCE_FAIBLE.getTextSize() / dy;

      PAINT_TENDANCE_FAIBLE.setTextSize(PAINT_TENDANCE_FAIBLE.getTextSize() * tendanceCoeff);
      PAINT_TENDANCE_FAIBLE.setColor(resources.getColor(R.color.map_balise_infos_tendance_vent_faible));
      PAINT_TENDANCE_FORTE = new Paint(PAINT_TENDANCE_FAIBLE);
      PAINT_TENDANCE_FORTE.setColor(resources.getColor(R.color.map_balise_infos_tendance_vent_forte));

      PAINT_IMAGE_ERROR = new Paint(Paint.ANTI_ALIAS_FLAG);
      PAINT_IMAGE_ERROR.setStrokeWidth(2 * scalingFactor);
      PAINT_IMAGE_ERROR.setColor(Color.RED);
      PAINT_IMAGE_ERROR.setStyle(Paint.Style.STROKE);

      // Fin
      INFOS_INITIALIZED = true;
    }
  }

  /**
   * 
   * @param bounds
   * @param inInfos
   * @param includeTitleWidth
   */
  protected void getBoxBounds(final Rect bounds, final List<DrawableInfo> inInfos, final boolean includeTitleWidth)
  {
    float width = 0;
    float height = 0;
    maxInfoTitleWidth = 0;
    maxInfoValueWidth = 0;
    maxInfoTendancieWidth = 0;
    int infoImageWidth = 0;
    int infoImageHeight = 0;
    int maxInfoSpannedWidth = 0;
    final int size = inInfos.size();
    for (int i = 0; i < size; i++)
    {
      final DrawableInfo info = inInfos.get(i);
      if (!Utils.isStringVide(info.imageUrl))
      {
        // Image
        height += info.imageHeight;
        infoImageWidth = info.imageWidth;
        infoImageHeight = info.imageHeight;
      }
      else
      {
        // Texte !
        height += (info.span && info.center ? DrawingCommons.TEXT_SIZE * 0.8f : DrawingCommons.TEXT_SIZE);
        if (i == 0)
        {
          // Titre
          if (includeTitleWidth)
          {
            PAINT_INFOS_TEXTE_TITRE_LEFT.getTextBounds(info.title, 0, info.title.length(), bounds);
            final int w = bounds.right - bounds.left;
            width = Math.max(w, width);
          }
        }
        else if (info.span)
        {
          PAINT_INFOS_TEXTE_LEFT.getTextBounds(info.title, 0, info.title.length(), bounds);
          final int titleWidth = bounds.right - bounds.left;
          if (titleWidth > maxInfoSpannedWidth)
          {
            maxInfoSpannedWidth = titleWidth;
          }
        }
        else
        {
          // Title
          if (!Utils.isStringVide(info.title))
          {
            PAINT_INFOS_TEXTE_LEFT.getTextBounds(info.title, 0, info.title.length(), bounds);
            final int titleWidth = bounds.right - bounds.left;
            if (titleWidth > maxInfoTitleWidth)
            {
              maxInfoTitleWidth = titleWidth;
            }
          }
          // Valeur
          if (!Utils.isStringVide(info.value))
          {
            PAINT_INFOS_TEXTE_RIGHT.getTextBounds(info.value, 0, info.value.length(), bounds);
            final int valueWidth = bounds.right - bounds.left;
            if (valueWidth > maxInfoValueWidth)
            {
              maxInfoValueWidth = valueWidth;
            }
          }
          // Tendance
          if (!Utils.isStringVide(info.tendancie))
          {
            maxInfoTendancieWidth = LARGEUR_TENDANCE;
          }
        }
      }
    }

    // Calcul largeur
    final float espacement = (maxInfoTitleWidth > 0 && maxInfoValueWidth > 0 ? ESPACEMENT_INFO : 0) + (maxInfoTendancieWidth > 0 ? ESPACEMENT_INFO_TENDANCE : 0);
    float infosWidth = maxInfoTitleWidth + maxInfoValueWidth + maxInfoTendancieWidth + espacement;
    width = Math.max(width, LARGEUR_INFOBULLE_MINIMALE);
    if (infosWidth < width)
    {
      maxInfoTitleWidth += width - infosWidth;
    }
    if (infoImageWidth > 0)
    {
      width = infoImageWidth;
      height -= 2 * INTERLIGNE;
    }
    else
    {
      width = Math.max(maxInfoSpannedWidth, Math.max(infosWidth, width));
      width += 2 * MARGE_X;
    }

    // Calcul hauteur
    height += (INTERLIGNE * (size - 1)) + (2 * MARGE_Y);
    if (!Utils.isStringVide(touchableTitle))
    {
      height += INTERLIGNE + DrawingCommons.TEXT_SIZE;
    }

    // Box
    bounds.bottom = (int)Math.ceil(height);
    bounds.top = 0;
    bounds.left = 0;
    bounds.right = (int)Math.ceil(width);

    // Box pour le lien
    touchableBox.left = 0;
    touchableBox.top = bounds.bottom - (int)DrawingCommons.TEXT_SIZE - (int)MARGE_Y;
    touchableBox.right = bounds.right;
    touchableBox.bottom = bounds.bottom;

    // Box pour l'image
    imageBox.left = 0;
    imageBox.top = (int)DrawingCommons.TEXT_SIZE;
    imageBox.right = bounds.right;
    imageBox.bottom = imageBox.top + infoImageHeight;
  }

  /**
   * 
   * @param point
   */
  protected void computeDetailsPosition(final Point point)
  {
    // Initialisations
    final int widthLimit = mapView.get().getPixelWidth() / 3;
    final int heightLimit = mapView.get().getPixelHeight() / 3;
    final int halfHeight = mapView.get().getPixelHeight() / 2;
    boolean xCentered = false;
    boolean xRight = false;

    // Horizontal
    if (!MAP_CENTERING && (point.x < widthLimit))
    {
      // Texte a droite de la balise
      boxRelativePoint.setX((int)DELTA_X);
      xRight = true;
    }
    else if (MAP_CENTERING || (point.x < 2 * widthLimit))
    {
      // Texte au niveau de la balise
      boxRelativePoint.setX(-boxBounds.right / 2);
      xCentered = true;
    }
    else
    {
      // Texte a gauche de la balise
      boxRelativePoint.setX(-boxBounds.right - (int)DELTA_X);
    }

    // Vertical
    if (xCentered)
    {
      // Si centre horizontal => par rapport au milieu de l'ecran
      if (MAP_CENTERING || (point.y >= halfHeight))
      {
        // Texte au dessus
        boxRelativePoint.setY(-boxBounds.bottom - (int)DELTA_Y);
      }
      else
      {
        // Texte en dessous
        boxRelativePoint.setY((int)DELTA_Y);
      }
    }
    else
    {
      // Si non horizontale => par rapport au tiers de l'ecran
      if (point.y < heightLimit)
      {
        // Texte en dessous de la balise
        boxRelativePoint.setY((int)DELTA_Y);
        boxRelativePoint.setX(xRight ? 0 : -boxBounds.right);
      }
      else if (point.y < 2 * heightLimit)
      {
        // Texte au niveau de la balise
        boxRelativePoint.setY(-boxBounds.bottom / 2);
      }
      else
      {
        // Texte au dessus de la balise
        boxRelativePoint.setY(-boxBounds.bottom - (int)DELTA_Y);
        boxRelativePoint.setX(xRight ? 0 : -boxBounds.right);
      }
    }

    // Fleche vers le centre de la box
    final float centerX = boxRelativePoint.x + boxBounds.right / 2;
    final float centerY = boxRelativePoint.y + boxBounds.bottom / 2;
    final float longueur = (float)Math.sqrt(centerX * centerX + centerY * centerY);
    final float dx = centerX / longueur;
    final float dy = centerY / longueur;

    // La fleche
    final float epaisseur = Math.min(boxBounds.right / 2, Math.min(boxBounds.bottom / 2, EPAISSEUR_FLECHE_BOX));
    boxFlechePath.reset();
    boxFlechePath.moveTo(0, 0);
    boxFlechePath.lineTo(centerX - epaisseur * dy, centerY + epaisseur * dx);
    boxFlechePath.lineTo(centerX + epaisseur * dy, centerY - epaisseur * dx);
    boxFlechePath.lineTo(0, 0);
    boxFlechePath.setLastPoint(0, 0);

    // Fin
    computeDetailsPosition = false;
  }

  /**
   * 
   * @param canvas
   * @param point
   */
  protected final void drawDetails(final Canvas canvas, final Point point)
  {
    // Calcul de la position relative
    if (computeDetailsPosition)
    {
      computeDetailsPosition(point);
    }

    // Coin superieur gauche
    boxLeft = point.x + boxRelativePoint.x;
    boxTop = point.y + boxRelativePoint.y;

    // Calcul contour rectangle arrondi
    boxRect.set(boxLeft, boxTop, boxLeft + boxBounds.right, boxTop + boxBounds.bottom);

    // Dessin contour rectangle arrondi
    canvas.drawRoundRect(boxRect, RAYON_INFOS, RAYON_INFOS, PAINT_INFOS_CONTOUR);

    // Contour fleche
    boxFlechePath.offset(point.x, point.y);
    canvas.save(Canvas.CLIP_SAVE_FLAG);
    final float clipLeft;
    final float clipRight;
    if ((boxRect.top < point.y) && (boxRect.bottom > point.y))
    {
      clipLeft = (boxRect.right < point.x ? boxRect.right : point.x);
      clipRight = (boxRect.left > point.x ? boxRect.left : point.x);
    }
    else
    {
      clipLeft = Math.min(boxRect.left, point.x);
      clipRight = Math.max(boxRect.right, point.x);
    }
    final float clipTop = (boxRect.bottom < point.y ? boxRect.bottom : Math.min(boxRect.top, point.y));
    final float clipBottom = (boxRect.top > point.y ? boxRect.top : Math.max(boxRect.bottom, point.y));
    canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom);
    canvas.drawPath(boxFlechePath, PAINT_INFOS_CONTOUR);
    canvas.restore();

    // Remplissage rectangle arrondi
    canvas.drawRoundRect(boxRect, RAYON_INFOS, RAYON_INFOS, PAINT_INFOS_REMPLISSAGE);

    // Remplissage fleche
    canvas.save(Canvas.CLIP_SAVE_FLAG);
    canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom);
    canvas.drawPath(boxFlechePath, PAINT_INFOS_REMPLISSAGE);
    boxFlechePath.offset(-point.x, -point.y);
    canvas.restore();

    // Trait sous le titre
    final int textsSize = infos.size();
    final float firstY = boxTop + MARGE_Y + DrawingCommons.TEXT_SIZE;
    if (textsSize > 1)
    {
      canvas.drawLine(boxRect.left, firstY, boxRect.right, firstY, PAINT_INFOS_TEXTE_TITRE_LEFT);
    }

    // Textes
    canvas.save(Canvas.CLIP_SAVE_FLAG);
    canvas.clipRect(boxRect.left + MARGE_X, boxRect.top, boxRect.right - MARGE_X, boxRect.bottom);

    // Nom de la balise
    final float boxCenter = boxRect.left + (boxRect.right - boxRect.left) / 2;
    final float x = boxLeft + MARGE_X;
    final float diffLargeur = (boxRect.right - boxRect.left - 2 * MARGE_X) - (titleBounds.right - titleBounds.left);
    final String nom = infos.get(0).title;
    final float titleYOffset = 1.5f;
    if (diffLargeur > 0)
    {
      // Centree
      canvas.drawText(nom, boxCenter, firstY - MARGE_Y + titleYOffset, PAINT_INFOS_TEXTE_TITRE_CENTER);
    }
    else
    {
      // A gauche
      canvas.drawText(nom, x, firstY - MARGE_Y + titleYOffset, PAINT_INFOS_TEXTE_TITRE_LEFT);
    }

    // Donnees
    float y = firstY;
    int imageHeight = 0;
    for (int i = 1; i < textsSize; i++)
    {
      // Next
      final DrawableInfo info = infos.get(i);

      y += INTERLIGNE + (imageHeight > 0 ? imageHeight : (info.span && info.center ? DrawingCommons.TEXT_SIZE * 0.8f : DrawingCommons.TEXT_SIZE));
      if (Utils.isStringVide(info.imageUrl))
      {
        // Donnees
        if (!Utils.isStringVide(info.title))
        {
          if (info.span && info.center)
          {
            canvas.drawText(info.title, boxCenter, y, PAINT_INFOS_TEXTE_CENTER);
          }
          else
          {
            canvas.drawText(info.title, x, y, PAINT_INFOS_TEXTE_LEFT);
          }
        }
        if (!info.span)
        {
          if (!Utils.isStringVide(info.value))
          {
            canvas.drawText(info.value, x + maxInfoTitleWidth + maxInfoValueWidth + ESPACEMENT_INFO, y, PAINT_INFOS_TEXTE_RIGHT);
          }
          if (!Utils.isStringVide(info.tendancie))
          {
            final float deltaY = (info.tendanciePaint.getTextSize() - PAINT_INFOS_TEXTE_LEFT.getTextSize()) / 2;
            canvas.drawText(info.tendancie, x + maxInfoTitleWidth + maxInfoValueWidth + ESPACEMENT_INFO + ESPACEMENT_INFO_TENDANCE, y + deltaY, info.tendanciePaint);
          }
        }

        // Next
        imageHeight = 0;
      }
      else
      {
        final float startX = x - MARGE_X;
        final float startY = y - DrawingCommons.TEXT_SIZE - 2 * scalingFactor;
        // Une image
        if (info.image != null)
        {
          canvas.restore();
          canvas.drawBitmap(info.image, startX, startY, null);
          canvas.save(Canvas.CLIP_SAVE_FLAG);
          canvas.clipRect(boxRect.left + MARGE_X, boxRect.top, boxRect.right - MARGE_X, boxRect.bottom);
        }
        if (info.imageError != null)
        {
          canvas.restore();
          final float endX = startX + info.imageWidth;
          final float endY = startY + info.imageHeight;
          canvas.drawLine(startX, startY, endX, endY, PAINT_IMAGE_ERROR);
          canvas.drawLine(startX, endY, endX, startY, PAINT_IMAGE_ERROR);
          canvas.save(Canvas.CLIP_SAVE_FLAG);
          canvas.clipRect(boxRect.left + MARGE_X, boxRect.top, boxRect.right - MARGE_X, boxRect.bottom);
        }

        // Next
        imageHeight = info.imageHeight;
      }
    }

    // Lien
    if (!Utils.isStringVide(touchableTitle))
    {
      y += (imageHeight > 0 ? imageHeight : 2 * INTERLIGNE + DrawingCommons.TEXT_SIZE);
      canvas.drawText(touchableTitle, boxCenter, y, PAINT_TOUCHABLE_TITLE);
    }

    // Fin
    canvas.restore();

    // Trait sur le lien
    if (!Utils.isStringVide(touchableTitle))
    {
      y -= DrawingCommons.TEXT_SIZE + 2 * scalingFactor;
      canvas.drawLine(boxRect.left, y, boxRect.right, y, PAINT_TOUCHABLE_TITLE);
    }
  }

  /**
   * @param drawDetails the drawDetails to set
   */
  public final void setDrawDetails(final boolean drawDetails)
  {
    computeDetailsPosition = (drawDetails && !this.drawDetails);
    this.drawDetails = drawDetails;
  }

  /**
   * @return the drawDetails
   */
  public final boolean isDrawDetails()
  {
    return drawDetails;
  }

  /**
   * @return the mAP_CENTERING
   */
  public static boolean isMAP_CENTERING()
  {
    return MAP_CENTERING;
  }

  /**
   * @param mAPCENTERING the mAP_CENTERING to set
   */
  public static void setMAP_CENTERING(final boolean mAPCENTERING)
  {
    MAP_CENTERING = mAPCENTERING;
  }
}
