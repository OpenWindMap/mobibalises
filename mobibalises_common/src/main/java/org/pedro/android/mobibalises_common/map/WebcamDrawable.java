package org.pedro.android.mobibalises_common.map;

import org.pedro.android.map.MapView;
import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;
import org.pedro.android.mobibalises_common.webcam.WebcamDatabaseHelper.WebcamRow;
import org.pedro.balises.Utils;
import org.pedro.map.MapDrawable;
import org.pedro.map.Point;
import org.pedro.webcams.Webcam;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.util.FloatMath;

/**
 * 
 * @author pedro.m
 */
public final class WebcamDrawable extends InfosDrawable implements MapDrawable<Canvas>, InvalidableDrawable
{
  private static final Object             graphicsInitializedLock = new Object();
  private static boolean                  graphicsInitialized     = false;
  private static float                    scalingFactor;
  protected static int                    displayerWidth;
  public static float                     rayonWebcam;
  private static float                    rayonChamp;
  private static final org.pedro.map.Rect displayBounds           = new org.pedro.map.Rect();
  private static final org.pedro.map.Rect interactiveBounds       = new org.pedro.map.Rect();

  private static Bitmap                   webcamIconOk;
  private static Bitmap                   webcamIconKo;
  private static Bitmap                   webcamPanoIconOk;
  private static Bitmap                   webcamPanoIconKo;
  private static final Paint              iconPaint               = new Paint(Paint.FILTER_BITMAP_FLAG);

  private static int                      masquePratiques;
  private static boolean                  statutEnligne;
  private static boolean                  statutVariable;

  private boolean                         graphicsValidated       = false;
  private final Object                    graphicsValidationLock  = new Object();

  private final WebcamRow                 row;
  private Bitmap                          icon;
  private final Matrix                    iconMatrix              = new Matrix();
  private final Path                      champPath               = new Path();
  private final Matrix                    champMatrix             = new Matrix();
  private final Paint                     champPaint              = new Paint(Paint.ANTI_ALIAS_FLAG);

  private boolean                         displayable;

  /**
   * 
   * @param context
   */
  protected static void initGraphics(final Context context)
  {
    // Initilisation une seule fois !
    synchronized (graphicsInitializedLock)
    {
      if (graphicsInitialized)
      {
        return;
      }

      // ScalingFactor
      final DisplayMetrics metrics = ActivityCommons.getDisplayMetrics(context);
      scalingFactor = metrics.density;

      // Rayons
      rayonWebcam = 20 * scalingFactor;
      rayonChamp = 160 * scalingFactor;

      // Bounds
      displayBounds.right = (int)Math.ceil(rayonWebcam);
      displayBounds.left = -displayBounds.right;
      displayBounds.bottom = displayBounds.right;
      displayBounds.top = -displayBounds.bottom;
      interactiveBounds.right = (int)Math.ceil(rayonWebcam * 1.5f);
      interactiveBounds.left = -interactiveBounds.right;
      interactiveBounds.bottom = interactiveBounds.right;
      interactiveBounds.top = -interactiveBounds.bottom;

      // Icones
      final BitmapDrawable bmdOk = (BitmapDrawable)context.getResources().getDrawable(R.drawable.webcam_ok);
      webcamIconOk = Bitmap.createScaledBitmap(bmdOk.getBitmap(), (int)(2 * rayonWebcam), (int)(2 * rayonWebcam), true);
      final BitmapDrawable bmdKo = (BitmapDrawable)context.getResources().getDrawable(R.drawable.webcam_ko);
      webcamIconKo = Bitmap.createScaledBitmap(bmdKo.getBitmap(), (int)(2 * rayonWebcam), (int)(2 * rayonWebcam), false);
      final BitmapDrawable bmdPanoOk = (BitmapDrawable)context.getResources().getDrawable(R.drawable.webcam_pano_ok);
      webcamPanoIconOk = Bitmap.createScaledBitmap(bmdPanoOk.getBitmap(), (int)(2 * rayonWebcam), (int)(2 * rayonWebcam), true);
      final BitmapDrawable bmdPanoKo = (BitmapDrawable)context.getResources().getDrawable(R.drawable.webcam_pano_ko);
      webcamPanoIconKo = Bitmap.createScaledBitmap(bmdPanoKo.getBitmap(), (int)(2 * rayonWebcam), (int)(2 * rayonWebcam), false);

      // Fin
      graphicsInitialized = true;
    }
  }

  /**
   * 
   * @param row
   * @param mapView
   * @param touchableTitle
   */
  public WebcamDrawable(final WebcamRow row, final MapView mapView, final String touchableTitle)
  {
    super(mapView, touchableTitle);
    this.row = row;
  }

  /**
   * 
   * @return
   */
  private Bitmap getWebcamBitmap()
  {
    final boolean ok = !(Webcam.STATUT_ENLIGNE_HORSLIGNE.equals(row.statutEnLigne) || Webcam.STATUT_VARIABLE_FIGEE.equals(row.statutVariable));
    final boolean pano = (row.champ >= 360);
    return (pano ? (ok ? webcamPanoIconOk : webcamPanoIconKo) : (ok ? webcamIconOk : webcamIconKo));
  }

  @Override
  public void draw(final Canvas canvas, final Point point)
  {
    // Validation
    validateDrawable();

    // Champ de vision si cliquée
    if (drawDetails)
    {
      champMatrix.reset();
      champMatrix.postTranslate(point.x, point.y);
      champPaint.getShader().setLocalMatrix(champMatrix);
      if (row.champ >= 360)
      {
        canvas.drawCircle(point.x, point.y, rayonChamp, champPaint);
      }
      else
      {
        champPath.offset(point.x, point.y);
        canvas.drawPath(champPath, champPaint);
        champPath.offset(-point.x, -point.y);
      }
    }

    // Icone
    iconMatrix.reset();
    iconMatrix.postRotate(row.direction, rayonWebcam, rayonWebcam);
    iconMatrix.postTranslate(point.x - rayonWebcam, point.y - rayonWebcam);
    canvas.drawBitmap(icon, iconMatrix, iconPaint);

    // Infobulle si cliquée
    if (drawDetails)
    {
      drawDetails(canvas, point);
    }
  }

  @Override
  public org.pedro.map.Rect getDisplayBounds()
  {
    return displayBounds;
  }

  @Override
  public org.pedro.map.Rect getInteractiveBounds()
  {
    return interactiveBounds;
  }

  @Override
  public void invalidateDrawable()
  {
    synchronized (graphicsValidationLock)
    {
      graphicsValidated = false;
    }
  }

  @Override
  public void validateDrawable()
  {
    synchronized (graphicsValidationLock)
    {
      if (!graphicsValidated)
      {
        validateGraphics();
        graphicsValidated = true;
      }
    }
  }

  /**
   * 
   */
  private void validateGraphics()
  {
    // Icone
    icon = getWebcamBitmap();

    // Champ
    champPath.reset();
    final float firstX = -rayonWebcam * (float)Math.sin((float)Math.toRadians(row.champ / 2));
    final float firstY = -rayonWebcam * (float)Math.cos((float)Math.toRadians(row.champ / 2));
    champPath.moveTo(firstX, firstY);
    champPath.lineTo(-rayonChamp * (float)Math.sin((float)Math.toRadians(row.champ / 2)), -rayonChamp * (float)Math.cos((float)Math.toRadians(row.champ / 2)));
    champPath.arcTo(new RectF(-rayonChamp, -rayonChamp, rayonChamp, rayonChamp), -row.champ / 2 - 90, row.champ);
    champPath.lineTo(rayonWebcam * (float)Math.sin((float)Math.toRadians(row.champ / 2)), -rayonWebcam * (float)Math.cos((float)Math.toRadians(row.champ / 2)));
    champPath.lineTo(firstX, firstY);
    champPath.setLastPoint(firstX, firstY);
    final Matrix champLocalMatrix = new Matrix();
    champLocalMatrix.postRotate(row.direction);
    champPath.transform(champLocalMatrix);

    // Paint champ
    champPaint.setShader(new RadialGradient(0, 0, rayonChamp, Color.argb(192, 255, 0, 0), Color.argb(16, 255, 0, 0), Shader.TileMode.MIRROR));
    champPaint.setStyle(Paint.Style.FILL);

    // Infos
    validateInfosGraphics();

    // Affichage
    validateDisplayability();
  }

  /**
   * 
   */
  private void validateInfosGraphics()
  {
    // Elaboration du texte
    infos.clear();

    // Nom
    infos.add(new DrawableInfo(row.nom, null));
    PAINT_INFOS_TEXTE_TITRE_LEFT.getTextBounds(row.nom, 0, row.nom.length(), titleBounds);

    // Image
    if (!Utils.isStringVide(row.urlImage))
    {
      final int largeurImage = Math.max(200, Math.min(600, Math.round(displayerWidth / 1.5f / 25) * 25));
      final int hauteur = row.hauteur * largeurImage / row.largeur;
      infos.add(new DrawableInfo(Webcam.getUrlImage(row.urlImage, row.periodicite, row.decalagePeriodicite, row.decalageHorloge, row.fuseauHoraire, row.codeLocale), largeurImage, hauteur));
    }

    // Bounds du texte
    getBoxBounds(boxBounds, infos, true);
  }

  @Override
  protected void computeDetailsPosition(final Point point)
  {
    // Initialisations
    final boolean isCenter = (row.direction > 337.5) || (row.direction <= 22.5) || ((row.direction > 157.5) && (row.direction <= 202.5));
    final boolean isLeft = (row.direction > 22.5) && (row.direction <= 157.5);
    final boolean isRight = !isCenter && !isLeft;
    final boolean isMiddle = ((row.direction > 67.5) && (row.direction <= 112.5)) || ((row.direction > 247.5) && (row.direction <= 292.5));
    final boolean isTop = (row.direction > 112.5) && (row.direction <= 247.5);
    final boolean isBottom = !isMiddle && !isTop;

    // Horizontal
    if (isLeft)
    {
      // Texte a gauche de la balise
      if (isMiddle)
      {
        boxRelativePoint.setX(-boxBounds.right - (int)DELTA_X);
      }
      else
      {
        boxRelativePoint.setX(-boxBounds.right);
      }
    }
    else if (isRight)
    {
      // Texte a droite de la balise
      if (isMiddle)
      {
        boxRelativePoint.setX((int)DELTA_X);
      }
      else
      {
        boxRelativePoint.setX(0);
      }
    }
    else if (isCenter)
    {
      // Texte au niveau de la balise
      boxRelativePoint.setX(-boxBounds.right / 2);
    }

    // Vertical
    if (isTop)
    {
      // Texte au dessus
      boxRelativePoint.setY(-boxBounds.bottom - (int)DELTA_Y);
    }
    else if (isBottom)
    {
      // Texte en dessous
      boxRelativePoint.setY((int)DELTA_Y);
    }
    else if (isMiddle)
    {
      // Texte au niveau de la balise
      boxRelativePoint.setY(-boxBounds.bottom / 2);
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
   */
  private void validateDisplayability()
  {
    // Selon preferences et pratiques et statuts de la webcam
    final boolean pratiqueDisplayed = ((masquePratiques & row.pratiques) > 0);
    final boolean enligneDisplayed = statutEnligne || Webcam.STATUT_ENLIGNE_ENLIGNE.equals(row.statutEnLigne);
    final boolean variableDisplayed = statutVariable || Webcam.STATUT_VARIABLE_VARIABLE.equals(row.statutVariable);

    // MAJ des flags
    displayable = pratiqueDisplayed && enligneDisplayed && variableDisplayed;
  }

  @Override
  public boolean isDrawable()
  {
    // Validation si besoin
    validateDrawable();

    return displayable;
  }

  /**
   * 
   * @param sharedPreferences
   * @param context
   * @return
   */
  public static boolean updatePreferences(final SharedPreferences sharedPreferences, final Context context)
  {
    // Initialisations
    final Resources resources = context.getResources();

    // Pratiques
    final String[] practicesDefaults = resources.getStringArray(R.array.webcam_pratiques_defaults);
    boolean practicesChanged = false;
    final int nbPractices = practicesDefaults.length;
    int newMasque = 0;
    for (int i = 0; i < nbPractices; i++)
    {
      final String key = AbstractBalisesPreferencesActivity.formatWebcamPratiquePreferenceKey(i);
      final boolean value = sharedPreferences.getBoolean(key, Boolean.parseBoolean(practicesDefaults[i]));
      if (value)
      {
        newMasque += Math.pow(2, i);
      }
    }
    practicesChanged = (newMasque != masquePratiques);
    masquePratiques = newMasque;

    // Statut en ligne
    final boolean newStatutEnligne = sharedPreferences.getBoolean(resources.getString(R.string.webcam_statut_enligne_key), Boolean.parseBoolean(resources.getString(R.string.webcam_statut_enligne_default)));
    final boolean enligneChanged = (newStatutEnligne != statutEnligne);
    statutEnligne = newStatutEnligne;

    // Statut variable
    final boolean newStatutVariable = sharedPreferences.getBoolean(resources.getString(R.string.webcam_statut_variable_key), Boolean.parseBoolean(resources.getString(R.string.webcam_statut_variable_default)));
    final boolean variableChanged = (newStatutVariable != statutVariable);
    statutVariable = newStatutVariable;

    return practicesChanged || enligneChanged || variableChanged;
  }
}
