package org.pedro.android.widget;

import java.util.ArrayList;
import java.util.List;

import org.pedro.misc.Sector;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * 
 * @author pedro.m
 */
public class SectorSelectorView extends View implements OnTouchListener, OnDoubleTapListener, OnGestureListener
{
  private final GestureDetector   detector;

  private float                   scalingFactor;

  private int                     width;
  private int                     height;

  private float                   deport;

  private float                   left;
  private float                   top;
  private float                   right;
  private float                   bottom;

  private float                   leftAxes;
  private float                   topAxes;
  private float                   rightAxes;
  private float                   bottomAxes;

  private float                   xCenter;
  private float                   yCenter;
  private float                   radius;

  private float                   xNE;
  private float                   yNE;
  private float                   xSW;
  private float                   ySW;

  private float                   xNNE, yNNE;
  private float                   xESE, yESE;
  private float                   xWNW, yWNW;
  private float                   xSSW, ySSW;

  private String                  northText;
  private float                   northTop;
  private String                  eastText;
  private float                   eastLeft;
  private float                   eastTop;
  private String                  westText;
  private float                   westLeft;
  private float                   westTop;
  private String                  southText;
  private float                   southTop;

  private final RectF             ovalBounds     = new RectF();

  private Paint                   paintAxes;
  private Paint                   paintSubAxes;
  private Paint                   paintSubSubAxes;
  private Paint                   paintCircle;
  private Paint                   paintSector;
  private Paint                   paintBackgroundSector;
  private Paint                   paintNSTexts;
  private Paint                   paintWText;
  private Paint                   paintEText;

  private List<Sector>            checkedSectors = new ArrayList<Sector>();
  private List<DrawSector>        drawSectors    = new ArrayList<DrawSector>();

  private OnSectorCheckedListener listener;

  /**
   * 
   * @author pedro.m
   */
  public interface OnSectorCheckedListener
  {
    /**
     * 
     * @param sector
     * @param checked
     */
    public void onSectorChecked(final Sector sector, final boolean checked);
  }

  /**
   * 
   * @author pedro.m
   */
  private static class DrawSector
  {
    float start;
    float angle;

    /**
     * 
     */
    DrawSector()
    {
      // Nothing
    }

    @Override
    public String toString()
    {
      return start + "/" + angle;
    }
  }

  /**
   * 
   * @param context
   */
  public SectorSelectorView(final Context context)
  {
    this(context, null, -1);
  }

  /**
   * 
   * @param context
   * @param attrs
   */
  public SectorSelectorView(final Context context, final AttributeSet attrs)
  {
    this(context, attrs, -1);
  }

  /**
   * 
   * @param context
   * @param attrs
   * @param defStyleAttr
   */
  public SectorSelectorView(final Context context, final AttributeSet attrs, final int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    detector = new GestureDetector(context, this);
    init();
  }

  /**
   * 
   */
  private void init()
  {
    // Desactivation de l'acceleration materielle
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
    {
      setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    // Initialisations
    final DisplayMetrics metrics = new DisplayMetrics();
    ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(metrics);
    scalingFactor = metrics.density;

    // Listener
    setOnTouchListener(this);

    // Axes
    paintAxes = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintAxes.setColor(Color.argb(200, 200, 200, 200));
    paintAxes.setStyle(Paint.Style.STROKE);
    paintAxes.setStrokeWidth(2 * scalingFactor);
    paintSubAxes = new Paint(paintAxes);
    paintSubAxes.setColor(Color.argb(100, 200, 200, 200));
    paintSubSubAxes = new Paint(paintSubAxes);
    paintSubSubAxes.setColor(Color.argb(50, 200, 200, 200));
    paintSubSubAxes.setStrokeWidth(1 * scalingFactor);
    paintSubSubAxes.setPathEffect(new DashPathEffect(new float[] { 5 * scalingFactor, 5 * scalingFactor }, 0));

    // Cercle
    paintCircle = new Paint(paintAxes);

    // Secteur
    paintBackgroundSector = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintBackgroundSector.setColor(Color.argb(128, 50, 200, 50));
    paintBackgroundSector.setStyle(Paint.Style.FILL);
    paintSector = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintSector.setStyle(Paint.Style.STROKE);
    paintSector.setColor(Color.argb(255, 150, 255, 150));
    paintSector.setStrokeWidth(3 * scalingFactor);

    // Textes
    paintNSTexts = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintNSTexts.setColor(getContext().getResources().getColor(android.R.color.primary_text_dark));
    paintNSTexts.setTextAlign(Paint.Align.CENTER);
    paintNSTexts.setTypeface(Typeface.DEFAULT_BOLD);
    paintNSTexts.setTextSize(paintNSTexts.getTextSize() * 1.5f);
    paintWText = new Paint(paintNSTexts);
    paintWText.setTextAlign(Paint.Align.RIGHT);
    paintEText = new Paint(paintNSTexts);
    paintEText.setTextAlign(Paint.Align.LEFT);
    final Resources resources = getContext().getResources();
    northText = resources.getString(R.string.north);
    eastText = resources.getString(R.string.east);
    southText = resources.getString(R.string.south);
    westText = resources.getString(R.string.west);
  }

  @Override
  protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
  {
    // Super
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    // Calcul marges
    final float margin = 30 * scalingFactor;
    deport = 10 * scalingFactor;
    final float textMargin = 2 * scalingFactor;

    // Recuperation largeur/hauteur
    width = getMeasuredWidth();
    height = getMeasuredHeight();

    // Centre
    xCenter = width / 2;
    yCenter = height / 2;

    // Bords
    if (width > height)
    {
      top = margin;
      bottom = height - margin;
      left = (width - height) / 2 + margin;
      right = left + height - 2 * margin;
      radius = height / 2 - margin;
    }
    else
    {
      left = margin;
      right = width - margin;
      top = (height - width) / 2 + margin;
      bottom = top + width - 2 * margin;
      radius = width / 2 - margin;
    }

    // Nord-Est et Sud-Ouest
    final float cos45 = (float)Math.cos((float)Math.PI / 4) * radius;
    xNE = xCenter + cos45;
    yNE = yCenter - cos45;
    xSW = xCenter - cos45;
    ySW = yCenter + cos45;

    // NNE/SSW et ESE/WNW
    final float cos22_5 = (float)Math.cos((float)Math.PI / 8) * radius;
    final float sin22_5 = (float)Math.sin((float)Math.PI / 8) * radius;
    xNNE = xCenter + sin22_5;
    yNNE = yCenter - cos22_5;
    xESE = xCenter + cos22_5;
    yESE = yCenter + sin22_5;
    xSSW = xCenter - sin22_5;
    ySSW = yCenter + cos22_5;
    xWNW = xCenter - cos22_5;
    yWNW = yCenter - sin22_5;

    // Axes
    leftAxes = left - deport;
    rightAxes = right + deport;
    topAxes = top - deport;
    bottomAxes = bottom + deport;

    // Oval pour les secteurs
    ovalBounds.top = top;
    ovalBounds.bottom = bottom;
    ovalBounds.left = left;
    ovalBounds.right = right;

    // Calcul des positions des textes
    final Rect bounds = new Rect();

    // Nord et Sud
    northTop = top - deport - textMargin;
    paintNSTexts.getTextBounds(southText, 0, southText.length(), bounds);
    southTop = bottom + deport + textMargin + bounds.height();

    // Est
    paintEText.getTextBounds(eastText, 0, eastText.length(), bounds);
    eastLeft = right + deport + textMargin;
    eastTop = yCenter + (float)bounds.height() / 2;

    // Ouest
    paintWText.getTextBounds(westText, 0, westText.length(), bounds);
    westLeft = left - deport - textMargin;
    westTop = yCenter + (float)bounds.height() / 2;
  }

  @Override
  public void onDraw(final Canvas canvas)
  {
    // Super
    super.onDraw(canvas);

    // Axes tres secondaires
    canvas.drawLine(xSSW, ySSW, xNNE, yNNE, paintSubSubAxes);
    canvas.drawLine(xWNW, yWNW, xESE, yESE, paintSubSubAxes);
    canvas.drawLine(xSSW, yNNE, xNNE, ySSW, paintSubSubAxes);
    canvas.drawLine(xWNW, yESE, xESE, yWNW, paintSubSubAxes);

    // Axes secondaires
    canvas.drawLine(xSW, ySW, xNE, yNE, paintSubAxes);
    canvas.drawLine(xSW, yNE, xNE, ySW, paintSubAxes);

    // Axes principaux
    canvas.drawLine(leftAxes, yCenter, rightAxes, yCenter, paintAxes);
    canvas.drawLine(xCenter, topAxes, xCenter, bottomAxes, paintAxes);

    // Cercle exterieur
    canvas.drawCircle(xCenter, yCenter, radius, paintCircle);

    // Textes
    canvas.drawText(northText, xCenter, northTop, paintNSTexts);
    canvas.drawText(eastText, eastLeft, eastTop, paintEText);
    canvas.drawText(southText, xCenter, southTop, paintNSTexts);
    canvas.drawText(westText, westLeft, westTop, paintWText);

    // Secteurs
    for (final DrawSector sector : drawSectors)
    {
      canvas.drawArc(ovalBounds, sector.start, sector.angle, true, paintBackgroundSector);
      canvas.drawArc(ovalBounds, sector.start, sector.angle, true, paintSector);
    }
  }

  @Override
  public boolean onTouch(final View view, final MotionEvent event)
  {
    return detector.onTouchEvent(event);
  }

  @Override
  public boolean onDown(final MotionEvent event)
  {
    return true;
  }

  @Override
  public boolean onFling(final MotionEvent event, MotionEvent arg1, float arg2, float arg3)
  {
    return false;
  }

  @Override
  public void onLongPress(final MotionEvent event)
  {
    // Nothing
  }

  @Override
  public boolean onScroll(final MotionEvent event, MotionEvent arg1, float arg2, float arg3)
  {
    return false;
  }

  @Override
  public void onShowPress(final MotionEvent event)
  {
    // Nothing
  }

  @Override
  public boolean onSingleTapUp(final MotionEvent event)
  {
    return false;
  }

  @Override
  public boolean onDoubleTap(final MotionEvent event)
  {
    return false;
  }

  @Override
  public boolean onDoubleTapEvent(final MotionEvent event)
  {
    return false;
  }

  @Override
  public boolean onSingleTapConfirmed(final MotionEvent event)
  {
    // Rayon
    final float dx = event.getX() - xCenter;
    final float dy = event.getY() - yCenter;
    final float r = (float)Math.sqrt(dx * dx + dy * dy);
    if ((r > radius) || (r == 0))
    {
      return false;
    }

    // Angle
    final float angle;
    if (dx != 0)
    {
      final float a = (float)(Math.atan(dy / dx) * 180 / Math.PI);
      if (dx >= 0)
      {
        angle = 90 + a;
      }
      else
      {
        angle = 270 + a;
      }
    }
    else if (dy > 0)
    {
      angle = 180;
    }
    else
    {
      angle = 0;
    }

    // Recuperation du secteur
    final Sector sector = Sector.getFromAngle(angle);
    final boolean checked = toggleSectorChecked(sector);
    if (listener != null)
    {
      listener.onSectorChecked(sector, checked);
    }

    return true;
  }

  /**
   * 
   * @return
   */
  public List<Sector> getCheckedSectors()
  {
    return checkedSectors;
  }

  /**
   * 
   * @param sectors
   */
  public void setCheckedSectors(final List<Sector> sectors)
  {
    checkedSectors.clear();
    checkedSectors.addAll(sectors);
    invalidate();
  }

  /**
   * 
   * @param sector
   * @return
   */
  public boolean isSectorChecked(final Sector sector)
  {
    return checkedSectors.contains(sector);
  }

  /**
   * 
   * @param sector
   * @param checked
   */
  public void setSectorChecked(final Sector sector, final boolean checked)
  {
    if (!checked && checkedSectors.contains(sector))
    {
      checkedSectors.remove(sector);
    }
    else if (checked && !checkedSectors.contains(sector))
    {
      checkedSectors.add(sector);
    }

    invalidate();
  }

  /**
   * 
   * @param sector
   * @return
   */
  private boolean toggleSectorChecked(final Sector sector)
  {
    final boolean checked = checkedSectors.contains(sector);

    if (checked)
    {
      checkedSectors.remove(sector);
    }
    else
    {
      checkedSectors.add(sector);
    }

    invalidate();
    return !checked;
  }

  /**
   * 
   * @param listener
   */
  public void setOnCheckedSectorListener(final OnSectorCheckedListener listener)
  {
    this.listener = listener;
  }

  @Override
  public void invalidate()
  {
    super.invalidate();
    computeDrawSectors();
  }

  /**
   * 
   */
  private void computeDrawSectors()
  {
    drawSectors.clear();
    boolean previousChecked = false;
    float startAngle = 0;
    Sector lastSector = null;
    boolean checked = false;
    for (final Sector sector : Sector.values())
    {
      lastSector = sector;
      checked = checkedSectors.contains(sector);
      if (checked && !previousChecked)
      {
        startAngle = sector.getStartAngle();
      }
      else if (!checked && previousChecked)
      {
        final DrawSector drawSector = new DrawSector();
        drawSector.start = startAngle - 90;
        drawSector.angle = sector.getStartAngle() - startAngle;
        drawSectors.add(drawSector);
      }

      // Next
      previousChecked = checked;
    }

    // Dernier
    if (checked && (lastSector != null))
    {
      final DrawSector drawSector = new DrawSector();
      drawSector.start = startAngle - 90;
      drawSector.angle = lastSector.getStartAngle() - startAngle + Sector.amplitude;
      drawSectors.add(drawSector);
    }

    // Fusion 1er et dernier ?
    final int nbSectors = drawSectors.size();
    if (nbSectors >= 2)
    {
      final DrawSector first = drawSectors.get(0);
      final DrawSector last = drawSectors.get(nbSectors - 1);

      // Secteurs joints ?
      final boolean joint = (last.start + last.angle) >= ((first.start + 360) % 360);
      if (joint)
      {
        first.start -= last.angle;
        first.angle += last.angle;
        drawSectors.remove(last);
      }
    }
  }
}
