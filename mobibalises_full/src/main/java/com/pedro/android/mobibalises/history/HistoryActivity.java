package com.pedro.android.mobibalises.history;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.Strings;
import org.pedro.android.mobibalises_common.analytics.AnalyticsService;
import org.pedro.android.mobibalises_common.provider.BaliseProviderUtils.TendanceVent;
import org.pedro.android.mobibalises_common.service.ProvidersServiceBinder;
import org.pedro.android.mobibalises_common.view.BaliseDrawable;
import org.pedro.android.mobibalises_common.view.DrawingCommons;
import org.pedro.android.mobibalises_common.view.WindIconInfos;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;
import org.pedro.map.Point;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pedro.android.mobibalises.R;
import com.pedro.android.mobibalises.service.IFullProvidersService;
import com.pedro.android.mobibalises.service.ProvidersService;
import com.pedro.android.mobibalises.view.FullDrawingCommons;

/**
 * 
 * @author pedro.m
 */
public class HistoryActivity extends Activity
{
  // Constantes publiques
  public static final String         INTENT_EXTRA_PROVIDER_KEY           = "providerKey";
  public static final String         INTENT_EXTRA_BALISE_ID              = "baliseId";
  public static final String         INTENT_EXTRA_BALISE_NAME            = "baliseName";
  public static final String         INTENT_EXTRA_PROVIDER_DELTA_RELEVES = "deltaReleves";
  public static final String         PREFERENCES_ACTIVE_INDEX            = "historyActivity.active";

  // Constantes privees
  private static final int           COULEUR_AXES_RADAR                  = Color.WHITE;
  private static final int           COULEUR_DIRECTIONS_RADAR            = Color.WHITE;
  private static final int           COULEUR_VALEURS_RADAR               = Color.LTGRAY;
  static final int                   COULEUR_GRAPH_DEBUT_RADAR           = Color.rgb(0, 26, 80);
  static final int                   COULEUR_GRAPH_FIN_RADAR             = Color.rgb(60, 120, 220);
  private static final int           COULEUR_LIMITE_MOY_RADAR            = Color.rgb(20, 80, 150);
  private static final int           COULEUR_POINT_RADAR                 = Color.YELLOW;
  private static final int           COULEUR_AXES_GRAPH                  = Color.WHITE;
  private static final int           COULEUR_GRADUATIONS_GRAPH           = Color.LTGRAY;
  private static final int           COULEUR_VENT_MINI_GRAPH             = Color.rgb(20, 150, 80);
  private static final int           COULEUR_VENT_MOYEN_GRAPH            = Color.rgb(20, 80, 150);
  private static final int           COULEUR_VENT_MAXI_GRAPH             = Color.rgb(150, 50, 50);
  private static final int           COULEUR_TEMPERATURE_GRAPH           = Color.rgb(150, 150, 150);
  private static final int           COULEUR_FOND_TEMPERATURE_GRAPH      = Color.rgb(60, 60, 60);
  private static final int           COULEUR_VALEURS_GRAPH               = Color.LTGRAY;
  private static final int           COULEUR_LIGNE_SELECTION_GRAPH       = Color.YELLOW;
  private static final int           COULEUR_POINT_SELECTION_GRAPH       = Color.YELLOW;

  // Dimensions
  static float                       marge;
  static float                       margeFois2;
  static float                       margeSur2;
  static float                       margeSur3;
  static int                         iconViewSize;
  static int                         diversPadding;
  static int                         tendancesPadding;
  static int                         largeurValeurVent;
  static int                         largeurTendanceVent;
  static float                       margeBoxLegende;
  static float                       margeLegendeLegende;
  static float                       decalTendanceHausse;
  static float                       decalTendanceBaisse;

  // Parametres
  Balise                             balise;
  String                             baliseName;
  String                             providerKey;
  String                             baliseId;
  int                                deltaReleves;

  // Service
  IFullProvidersService              providersService;
  private ProvidersServiceConnection providersServiceConnection;

  // Vue
  final HistoryAdapter               adapter                             = new HistoryAdapter(this);

  // Donnees
  final List<Date>                   dates                               = new ArrayList<Date>();
  final Map<Date, List<Releve>>      releves                             = new HashMap<Date, List<Releve>>();
  long                               deltaPeremption;

  // Radar
  static final Paint                 paintAxesRadar                      = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintAxeExterieurRadar              = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintDirectionsRadar                = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintValeursRadar                   = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintGraphRadar                     = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintLimiteMoyRadar                 = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintPointGraphRadar                = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintPointRadar                     = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final NumberFormat          formatValeursRadar                  = new DecimalFormat("#");

  // Graph
  static final Paint                 paintAxesGraph                      = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintGraduationsGraph               = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintLimiteMoyGraph                 = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintLimiteMaxGraph                 = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintVentMiniGraph                  = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintVentMoyenGraph                 = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintVentMaxiGraph                  = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintTemperatureGraph               = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintFondTemperatureGraph           = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintPointVentMiniGraph             = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintPointVentMoyenGraph            = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintPointVentMaxiGraph             = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintPointTemperatureGraph          = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintValeursGraph                   = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintTexteLegendeGraph              = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintVentMiniLegendeGraph           = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintVentMoyenLegendeGraph          = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintVentMaxiLegendeGraph           = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintTemperatureLegendeGraph        = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintLigneSelectionGraph            = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final Paint                 paintPointSelectionGraph            = new Paint(Paint.ANTI_ALIAS_FLAG);
  static final NumberFormat          formatValeursGraph                  = new DecimalFormat("#");
  static final NumberFormat          formatValeursAbscisseGraph          = new DecimalFormat("##h");

  // Divers
  NumberFormat                       decimalFormatEntier;
  Integer                            moyWindLimit;
  Integer                            maxWindLimit;
  int                                underLimitColor;
  int                                overLimitColor;
  float                              scalingFactor;
  long                               deltaPeremptionMs;

  // Formats date/heure
  static DateFormat                  HEURE_FORMAT;
  static DateFormat                  DATE_FORMAT_TITLE;

  /**
   * 
   * @author pedro.m
   */
  private static class RadarView extends View
  {
    private static final int       NB_DIVISIONS_ANGLE           = 8;
    private static final int       NB_DIVISIONS                 = 4;
    private static final double    DELTA_DIVISIONS_ANGLE        = Math.PI * 2 / NB_DIVISIONS_ANGLE;
    private static final double    DELTA_DEGRES_DIVISIONS_ANGLE = DELTA_DIVISIONS_ANGLE * 180 / Math.PI;

    private final Object           drawLock                     = new Object();

    private double                 maxi;
    private final List<RadarPoint> points                       = new ArrayList<RadarPoint>();
    private RadarPoint             selectedPoint                = null;

    private long                   debut;
    private long                   fin;

    private final Integer          moyWindLimit;
    private float                  relativeRayonLimiteMoy;

    /**
     * 
     * @author pedro.m
     */
    private static class RadarPoint
    {
      final double valeur;
      final float  angle;
      float        relativeX;
      float        relativeY;
      final Date   date;
      boolean      liaisonPrev;
      boolean      liaisonNext;
      int          couleur;

      /**
       * 
       * @param releve
       */
      RadarPoint(final Releve releve)
      {
        this.valeur = releve.ventMoyen;
        this.angle = (float)(releve.directionMoyenne * Math.PI / 180);
        this.date = releve.date;
      }
    }

    /**
     * 
     * @param context
     * @param moyWindLimit
     */
    RadarView(final Context context, final Integer moyWindLimit)
    {
      super(context);
      this.moyWindLimit = moyWindLimit;

      // Desactivation acceleration hardware (pour les pointilles et les ROUND_CAP)
      if (ActivityCommons.isHoneyComb())
      {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
      }
    }

    /**
     * 
     * @param releves
     * @param deltaPeremption
     */
    void setReleves(final List<Releve> releves, final long deltaPeremption)
    {
      synchronized (drawLock)
      {
        maxi = 0;
        points.clear();
        long lastTime = 0;
        debut = Long.MAX_VALUE;
        fin = Long.MIN_VALUE;
        RadarPoint lastPoint = null;
        for (final Releve releve : releves)
        {
          // Valeurs OK ?
          if ((releve.date != null) && !Double.isNaN(releve.ventMoyen) && (releve.directionMoyenne != Integer.MIN_VALUE))
          {
            // Point
            final RadarPoint radarPoint = new RadarPoint(releve);
            final long releveTime = releve.date.getTime();
            radarPoint.liaisonPrev = ((releveTime - lastTime) <= deltaPeremption);
            if (lastPoint != null)
            {
              lastPoint.liaisonNext = radarPoint.liaisonPrev;
            }
            lastTime = releveTime;

            // Valeur maxi
            if (releve.ventMoyen > maxi)
            {
              maxi = releve.ventMoyen;
            }

            // Debut et fin
            if (releveTime < debut)
            {
              debut = releveTime;
            }
            if (releveTime > fin)
            {
              fin = releveTime;
            }

            // Point
            points.add(radarPoint);

            // Next
            lastPoint = radarPoint;
          }
        }

        // Arrondi du maxi a la valeur superieure divisible par 4 (dans l'unite d'affichage)
        final double unitMaxi = Math.max(NB_DIVISIONS, NB_DIVISIONS * Math.ceil(ActivityCommons.getFinalSpeed(maxi) / NB_DIVISIONS));
        maxi = ActivityCommons.getInitialSpeed(unitMaxi);

        // Precalcul des points
        for (final RadarPoint point : points)
        {
          // Position relative
          point.relativeX = (float)(Math.sin(point.angle) * point.valeur / maxi);
          point.relativeY = (float)(Math.cos(point.angle) * point.valeur / maxi);

          // Couleur
          final long releveTime = point.date.getTime();
          final float pourcent = (float)(releveTime - debut) / (float)(fin - debut);
          point.couleur = moyenneCouleur(COULEUR_GRAPH_DEBUT_RADAR, COULEUR_GRAPH_FIN_RADAR, pourcent);

          // Point selectionne : le dernier
          selectedPoint = point;
        }
        if (moyWindLimit != null)
        {
          relativeRayonLimiteMoy = (float)(moyWindLimit.intValue() / unitMaxi);
        }
      }
    }

    /**
     * 
     * @param couleurDebut
     * @param couleurFin
     * @param pourcent
     * @return
     */
    private static int moyenneCouleur(final int couleurDebut, final int couleurFin, final float pourcent)
    {
      final int redDebut = Color.red(couleurDebut);
      final int redFin = Color.red(couleurFin);
      final int red = (int)(redDebut + pourcent * (redFin - redDebut));
      final int greenDebut = Color.green(couleurDebut);
      final int greenFin = Color.green(couleurFin);
      final int green = (int)(greenDebut + pourcent * (greenFin - greenDebut));
      final int blueDebut = Color.blue(couleurDebut);
      final int blueFin = Color.blue(couleurFin);
      final int blue = (int)(blueDebut + pourcent * (blueFin - blueDebut));

      return Color.rgb(red, green, blue);
    }

    /**
     * 
     * @param releve
     */
    void setSelectedReleve(final Releve releve)
    {
      synchronized (drawLock)
      {
        // Recherche du point selectionne
        for (final RadarPoint point : points)
        {
          if (point.date == releve.date)
          {
            selectedPoint = point;
            break;
          }
        }
      }

      // Repaint
      invalidate();
    }

    @Override
    public void draw(final Canvas canvas)
    {
      // Parent
      super.draw(canvas);

      synchronized (drawLock)
      {
        // Initialisations
        final float xm = getWidth() / 2;
        final float ym = getHeight() / 2;
        final float rayon = Math.min(xm, ym) - marge;
        final float rayonSur2 = rayon / 2;
        final float rayonPlusMargeSur2 = rayon + margeSur2;
        final float rayonMoinsMargeSur3 = rayon - margeSur3;
        final float rayonSur2MoinsMargeSur3 = rayonSur2 - margeSur3;
        final String textMaxi = formatValeursRadar.format(ActivityCommons.getFinalSpeed(maxi));
        final String textMaxiSur2 = formatValeursRadar.format(ActivityCommons.getFinalSpeed(maxi / 2));
        final float decalTextDirection = paintDirectionsRadar.getTextSize() / 3;
        final float decalTextValeur = paintValeursRadar.getTextSize() / 3;

        // Axes
        double angle = 0;
        int angleDegres = 0;
        for (int i = 0; i < NB_DIVISIONS_ANGLE; i++)
        {
          // Initialisations
          final float sinAngle = (float)Math.sin(angle);
          final float cosAngle = (float)Math.cos(angle);

          // Axe
          final float x = xm + rayon * sinAngle;
          final float y = ym - rayon * cosAngle;
          canvas.drawLine(xm, ym, x, y, paintAxesRadar);

          // Direction
          final float xt = xm + rayonPlusMargeSur2 * sinAngle;
          final float yt = ym - rayonPlusMargeSur2 * cosAngle;
          canvas.drawText(DrawingCommons.getLabelDirectionVent(angleDegres), xt, yt + decalTextDirection, paintDirectionsRadar);

          // Valeur du max et moitie
          if (i % 2 == 0)
          {
            final float xmax = xm + rayonMoinsMargeSur3 * sinAngle;
            final float ymax = ym - rayonMoinsMargeSur3 * cosAngle;
            canvas.drawText(textMaxi, xmax, ymax + decalTextValeur, paintValeursRadar);
            final float xmiddle = xm + rayonSur2MoinsMargeSur3 * sinAngle;
            final float ymiddle = ym - rayonSur2MoinsMargeSur3 * cosAngle;
            canvas.drawText(textMaxiSur2, xmiddle, ymiddle + decalTextValeur, paintValeursRadar);
          }

          // Next
          angle += DELTA_DIVISIONS_ANGLE;
          angleDegres += DELTA_DEGRES_DIVISIONS_ANGLE;
        }

        // Cercles
        canvas.drawCircle(xm, ym, rayon, paintAxeExterieurRadar);
        canvas.drawCircle(xm, ym, rayonSur2, paintAxesRadar);

        // Limite vent moyen
        if ((moyWindLimit != null) && (maxi >= moyWindLimit.intValue()))
        {
          canvas.drawCircle(xm, ym, rayon * relativeRayonLimiteMoy, paintLimiteMoyRadar);
        }

        // Points
        float xprec = 0;
        float yprec = 0;
        float xselected = 0;
        float yselected = 0;
        for (final RadarPoint point : points)
        {
          // Coordonnees
          final float x = xm + rayon * point.relativeX;
          final float y = ym - rayon * point.relativeY;
          if (point == selectedPoint)
          {
            xselected = x;
            yselected = y;
          }

          // Trace
          if (point.liaisonPrev)
          {
            paintGraphRadar.setColor(point.couleur);
            canvas.drawLine(xprec, yprec, x, y, paintGraphRadar);
          }
          else if (!point.liaisonNext)
          {
            paintPointGraphRadar.setColor(point.couleur);
            canvas.drawPoint(x, y, paintPointGraphRadar);
          }

          // Next
          xprec = x;
          yprec = y;
        }

        // Point de selection
        if (selectedPoint != null)
        {
          canvas.drawPoint(xselected, yselected, paintPointRadar);
        }
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ChartView extends View
  {
    private static final int         NB_DIVISIONS_ORD = 4;
    private static final int         NB_DIVISIONS_ABS = 6;

    private final Object             drawLock         = new Object();

    private double                   maxiWind;
    private boolean                  hasMinWindValue;
    private boolean                  hasMoyWindValue;
    private boolean                  hasMaxWindValue;
    private double                   miniTemp;
    private double                   maxiTemp;
    private boolean                  hasTempValue;
    private final List<ChartPoint>   points           = new ArrayList<ChartPoint>();

    private final ChartTouchListener touchListener;
    private float                    abscisseMini;
    private float                    abscisseMaxi;
    private ChartPoint               selectedPoint;

    private final Integer            moyWindLimit;
    private final Integer            maxWindLimit;
    private float                    relativeYMoyWindLimit;
    private float                    relativeYMaxWindLimit;

    private final String             minLegendText;
    private final String             moyLegendText;
    private final String             maxLegendText;
    private final String             tempLegendText;

    final List<ChartLegend>          legends          = new ArrayList<ChartLegend>();

    /**
     * 
     * @author pedro.m
     */
    private interface ChartTouchListener
    {
      /**
       * 
       * @param abscisse
       */
      public void onChartTouchEvent(final long abscisse);
    }

    /**
     * 
     * @author pedro.m
     */
    private static class ChartPoint
    {
      final long   originalAbscisse;
      final float  abscisse;

      final double valeurMini;
      final double valeurMoyen;
      final double valeurMaxi;
      final double valeurTemp;

      float        relativeYMini;
      float        relativeYMoyen;
      float        relativeYMaxi;
      float        relativeYTemp;

      boolean      liaisonPrevMini;
      boolean      liaisonPrevMoyen;
      boolean      liaisonPrevMaxi;
      boolean      liaisonPrevTemp;
      boolean      liaisonNextMini;
      boolean      liaisonNextMoyen;
      boolean      liaisonNextMaxi;
      boolean      liaisonNextTemp;

      /**
       * 
       * @param releve
       * @param dateJour
       */
      ChartPoint(final Releve releve, final Date dateJour)
      {
        this.valeurMini = releve.ventMini;
        this.valeurMoyen = releve.ventMoyen;
        this.valeurMaxi = releve.ventMaxi;
        this.valeurTemp = releve.temperature;
        this.originalAbscisse = releve.date.getTime();
        this.abscisse = ((float)(releve.date.getTime() - dateJour.getTime())) / 86400000;
      }

      @Override
      public String toString()
      {
        return abscisse + " : " + valeurMini + "/" + valeurMoyen + "/" + valeurMaxi + "/" + valeurTemp;
      }
    }

    /**
     * 
     * @author pedro.m
     */
    private static class ChartLegend
    {
      final String text;
      final int    textWidth;
      final Paint  textPaint;
      final float  boxWidth;
      final Paint  boxPaint;

      float        xBox1, xBox2, yBox1, yBox2;
      float        xText, yText;

      /**
       * 
       * @param text
       * @param textPaint
       * @param boxPaint
       */
      ChartLegend(final String text, final Paint textPaint, final Paint boxPaint)
      {
        this.text = text;
        this.textPaint = textPaint;
        this.boxPaint = boxPaint;

        // Calculs texte
        final Rect textBounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        textWidth = textBounds.right - textBounds.left;

        // Calculs box
        boxWidth = textPaint.getTextSize();
      }

      /**
       * 
       * @param canvas
       * @param x
       * @param y
       */
      void draw(final Canvas canvas, final float x, final float y)
      {
        canvas.drawRect(x + xBox1, y + yBox1, x + xBox2, y + yBox2, boxPaint);
        canvas.drawText(text, x + xText, y + yText, textPaint);
      }
    }

    /**
     * 
     * @param context
     * @param touchListener
     * @param moyWindLimit
     * @param maxWindLimit
     */
    ChartView(final Context context, final ChartTouchListener touchListener, final Integer moyWindLimit, final Integer maxWindLimit)
    {
      super(context);
      this.touchListener = touchListener;
      this.moyWindLimit = moyWindLimit;
      this.maxWindLimit = maxWindLimit;

      // Resources
      minLegendText = context.getResources().getString(R.string.label_history_legende_vent_mini);
      moyLegendText = context.getResources().getString(R.string.label_history_legende_vent_moyen);
      maxLegendText = context.getResources().getString(R.string.label_history_legende_vent_maxi);
      tempLegendText = context.getResources().getString(R.string.label_history_legende_temperature);

      // Desactivation acceleration hardware (pour les pointilles et les ROUND_CAP)
      if (ActivityCommons.isHoneyComb())
      {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
      }
    }

    /**
     * 
     * @param dateJour
     * @param releves
     * @param deltaPeremption
     */
    void setReleves(final Date dateJour, final List<Releve> releves, final long deltaPeremption)
    {
      synchronized (drawLock)
      {
        // Initialisations
        maxiWind = 0;
        hasMinWindValue = false;
        hasMoyWindValue = false;
        hasMaxWindValue = false;
        maxiTemp = Double.MIN_VALUE;
        miniTemp = Double.MAX_VALUE;
        hasTempValue = false;
        points.clear();
        long lastMiniTime = 0;
        long lastMoyenTime = 0;
        long lastMaxiTime = 0;
        long lastTempTime = 0;
        abscisseMini = Float.MAX_VALUE;
        abscisseMaxi = Float.MIN_VALUE;

        // Pour chaque releve
        ChartPoint lastPoint = null;
        for (final Releve releve : releves)
        {
          // Valeurs OK ?
          if ((releve.date != null) && (!Double.isNaN(releve.ventMini) || !Double.isNaN(releve.ventMoyen) || !Double.isNaN(releve.ventMaxi) || !Double.isNaN(releve.temperature)))
          {
            // Initialisations
            final ChartPoint chartPoint = new ChartPoint(releve, dateJour);
            final long releveTime = releve.date.getTime();

            // Abscisse mini et maxi
            if (chartPoint.abscisse < abscisseMini)
            {
              abscisseMini = chartPoint.abscisse;
            }
            if (chartPoint.abscisse > abscisseMaxi)
            {
              abscisseMaxi = chartPoint.abscisse;
            }

            // Vent mini
            if (!Double.isNaN(releve.ventMini))
            {
              // Flag
              hasMinWindValue = true;

              // Peremption
              chartPoint.liaisonPrevMini = ((releveTime - lastMiniTime) <= deltaPeremption);
              if (lastPoint != null)
              {
                lastPoint.liaisonNextMini = chartPoint.liaisonPrevMini;
              }
              lastMiniTime = releveTime;

              // Valeur maxi
              if (releve.ventMini > maxiWind)
              {
                maxiWind = releve.ventMini;
              }
            }

            // Vent moyen
            if (!Double.isNaN(releve.ventMoyen))
            {
              // Flag
              hasMoyWindValue = true;

              // Peremption
              chartPoint.liaisonPrevMoyen = ((releveTime - lastMoyenTime) <= deltaPeremption);
              if (lastPoint != null)
              {
                lastPoint.liaisonNextMoyen = chartPoint.liaisonPrevMoyen;
              }
              lastMoyenTime = releveTime;

              // Valeur maxi
              if (releve.ventMoyen > maxiWind)
              {
                maxiWind = releve.ventMoyen;
              }
            }

            // Vent maxi
            if (!Double.isNaN(releve.ventMaxi))
            {
              // Flag
              hasMaxWindValue = true;

              // Peremption
              chartPoint.liaisonPrevMaxi = ((releveTime - lastMaxiTime) <= deltaPeremption);
              if (lastPoint != null)
              {
                lastPoint.liaisonNextMaxi = chartPoint.liaisonPrevMaxi;
              }
              lastMaxiTime = releveTime;

              // Valeur maxi
              if (releve.ventMaxi > maxiWind)
              {
                maxiWind = releve.ventMaxi;
              }
            }

            // Temperature
            if (!Double.isNaN(releve.temperature))
            {
              // Flag
              hasTempValue = true;

              // Peremption
              chartPoint.liaisonPrevTemp = ((releveTime - lastTempTime) <= deltaPeremption);
              if (lastPoint != null)
              {
                lastPoint.liaisonNextTemp = chartPoint.liaisonPrevTemp;
              }
              lastTempTime = releveTime;

              // Valeur maxi
              if (releve.temperature < miniTemp)
              {
                miniTemp = releve.temperature;
              }
              if (releve.temperature > maxiTemp)
              {
                maxiTemp = releve.temperature;
              }
            }

            // Point
            points.add(chartPoint);

            // Next
            lastPoint = chartPoint;
          }
        }

        // Arrondi du vent maxi a la valeur superieure divisible par le nombre de divisions (dans l'unite d'affichage)
        final double unitMaxiWind = Math.max(NB_DIVISIONS_ORD, NB_DIVISIONS_ORD * Math.ceil(ActivityCommons.getFinalSpeed(maxiWind) / NB_DIVISIONS_ORD));
        maxiWind = ActivityCommons.getInitialSpeed(unitMaxiWind);

        // Arrondi de la temperature maxi a la valeur superieure divisible par le nombre de divisions (dans l'unite d'affichage)
        miniTemp = Math.floor(miniTemp);
        final double tempStep = Math.max(1, Math.ceil((maxiTemp - miniTemp) / NB_DIVISIONS_ORD));
        maxiTemp = miniTemp + NB_DIVISIONS_ORD * tempStep;

        // Precalcul des points
        for (final ChartPoint point : points)
        {
          point.relativeYMini = (float)(point.valeurMini / maxiWind);
          point.relativeYMoyen = (float)(point.valeurMoyen / maxiWind);
          point.relativeYMaxi = (float)(point.valeurMaxi / maxiWind);
          point.relativeYTemp = (float)((point.valeurTemp - miniTemp) / (maxiTemp - miniTemp));

          // Point selectionne : le dernier
          selectedPoint = point;
        }
        if (moyWindLimit != null)
        {
          relativeYMoyWindLimit = (float)(moyWindLimit.intValue() / unitMaxiWind);
        }
        if (maxWindLimit != null)
        {
          relativeYMaxWindLimit = (float)(maxWindLimit.intValue() / unitMaxiWind);
        }

        // Precalcul legendes
        legends.clear();
        float largeur = 0;
        int nbLegends = 0;
        if (hasMinWindValue)
        {
          final ChartLegend legend = new ChartLegend(minLegendText, paintTexteLegendeGraph, paintVentMiniLegendeGraph);
          legends.add(legend);
          largeur += legend.textWidth + legend.boxWidth;
          nbLegends++;
        }
        if (hasMoyWindValue)
        {
          final ChartLegend legend = new ChartLegend(moyLegendText, paintTexteLegendeGraph, paintVentMoyenLegendeGraph);
          legends.add(legend);
          largeur += legend.textWidth + legend.boxWidth;
          nbLegends++;
        }
        if (hasMaxWindValue)
        {
          final ChartLegend legend = new ChartLegend(maxLegendText, paintTexteLegendeGraph, paintVentMaxiLegendeGraph);
          legends.add(legend);
          largeur += legend.textWidth + legend.boxWidth;
          nbLegends++;
        }
        if (hasTempValue)
        {
          final ChartLegend legend = new ChartLegend(tempLegendText, paintTexteLegendeGraph, paintTemperatureLegendeGraph);
          legends.add(legend);
          largeur += legend.textWidth + legend.boxWidth;
          nbLegends++;
        }
        // Largeur des marges
        largeur += (nbLegends * margeBoxLegende) + (nbLegends - 1) * margeLegendeLegende;
        // Position des marges
        float x = -(largeur / 2);
        for (final ChartLegend legend : legends)
        {
          legend.xBox1 = x;
          legend.xBox2 = x + legend.boxWidth;
          legend.yBox1 = -legend.boxWidth / 2;
          legend.yBox2 = -legend.yBox1;
          legend.xText = legend.xBox2 + margeBoxLegende;
          legend.yText = legend.textPaint.getTextSize() / 2;
          x = legend.xText + legend.textWidth + margeLegendeLegende;
        }
      }
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event)
    {
      // Gestion des touchs ?
      if (touchListener == null)
      {
        return super.onTouchEvent(event);
      }

      // Initialisations
      final float largeur = getWidth();
      final float hauteur = getHeight();

      // Dans le graph ?
      final float abscisse = (event.getX() - marge) / (largeur - margeFois2);
      // Ligne ci dessous : pour que le graphe serve a la navigation dans les jours d'historique, quand on clique
      // suffisament loin d'un point.
      //final boolean captured = (abscisse >= abscisseMini) && (abscisse <= abscisseMaxi) && (event.getY() >= marge) && (event.getY() <= hauteur - marge);
      // Ligne ci dessous : toute la zone du graphe sert pour le detail
      final boolean captured = (event.getX() >= marge) && (event.getX() <= largeur - marge) && (event.getY() >= marge) && (event.getY() <= hauteur - marge);

      // En dehors du graph, on remonte au parent
      if (!captured)
      {
        return super.onTouchEvent(event);
      }

      // Dans le graph, recherche du releve le plus proche
      float minDistance = Float.MAX_VALUE;
      for (final ChartPoint point : points)
      {
        final float distance = Math.abs(abscisse - point.abscisse);
        if (distance < minDistance)
        {
          selectedPoint = point;
          minDistance = distance;
        }
      }

      // Redraw
      invalidate();

      // Notification
      touchListener.onChartTouchEvent(selectedPoint.originalAbscisse);

      return true;
    }

    @Override
    public void draw(final Canvas canvas)
    {
      // Parent
      super.draw(canvas);

      synchronized (drawLock)
      {
        // Initialisations
        final float largeur = getWidth() - margeFois2;
        final float hauteur = getHeight() - margeFois2;
        final float decalTextValeur = paintValeursGraph.getTextSize() / 2;
        final float widthMoinsMarge = getWidth() - marge;
        final float heightMoinsMarge = getHeight() - marge;

        // Fond temperature
        final Path tempPath = new Path();
        float xprec = 0;
        float yprectemp = 0;
        for (final ChartPoint point : points)
        {
          final float x = marge + point.abscisse * largeur;

          // Fond temperature
          if (!Double.isNaN(point.valeurTemp))
          {
            final float ytemp = heightMoinsMarge - point.relativeYTemp * hauteur;

            if (point.liaisonPrevTemp)
            {
              tempPath.reset();
              tempPath.moveTo(xprec, heightMoinsMarge);
              tempPath.lineTo(xprec, yprectemp);
              tempPath.lineTo(x, ytemp);
              tempPath.lineTo(x, heightMoinsMarge);
              tempPath.close();
              canvas.drawPath(tempPath, paintFondTemperatureGraph);
            }
            else if (!point.liaisonNextTemp)
            {
              canvas.drawPoint(x, ytemp, paintPointTemperatureGraph);
            }

            // Next
            yprectemp = ytemp;
          }

          // Next
          xprec = x;
        }

        // Graduations ordonnees
        {
          // Initialisations
          final float deltaY = hauteur / NB_DIVISIONS_ORD;
          float y = getHeight() - marge - deltaY;
          final float deltaValeurVent = (float)(maxiWind / NB_DIVISIONS_ORD);
          float valeurVent = deltaValeurVent;
          final float deltaValeurTemp = (float)((maxiTemp - miniTemp) / NB_DIVISIONS_ORD);
          float valeurTemp = deltaValeurTemp + (float)miniTemp;
          final float widthMoinsMargeSur2 = getWidth() - margeSur2;

          // Graduations
          for (int i = 1; i <= NB_DIVISIONS_ORD; i++)
          {
            // Position Y texte
            float yText = y + decalTextValeur;
            if (i == NB_DIVISIONS_ORD)
            {
              // Derniere graduation (en haut sur le graph) decalee un peu vers le bas, pour laisser la place a l'unite de vitesse
              yText += decalTextValeur;
            }

            // Ligne
            canvas.drawLine(marge, y, widthMoinsMarge, y, paintGraduationsGraph);

            // Texte vent
            final String textValeurVent = formatValeursGraph.format(ActivityCommons.getFinalSpeed(valeurVent));
            canvas.drawText(textValeurVent, margeSur2, yText, paintValeursGraph);

            // Texte temperature
            if (hasTempValue)
            {
              final String textValeurTemp = formatValeursGraph.format(ActivityCommons.getFinalTemperature(valeurTemp));
              canvas.drawText(textValeurTemp, widthMoinsMargeSur2, yText, paintValeursGraph);
            }

            // Next
            y -= deltaY;
            valeurVent += deltaValeurVent;
            valeurTemp += deltaValeurTemp;
          }
        }

        // Graduations abscisses
        {
          // Initialisations
          final float deltaX = largeur / NB_DIVISIONS_ABS;
          float x = marge + deltaX;
          final int deltaValeur = 24 / NB_DIVISIONS_ABS;
          int valeur = deltaValeur;
          final float yText = getHeight() - margeSur2 + decalTextValeur;

          // Graduations
          for (int i = 1; i < NB_DIVISIONS_ABS; i++)
          {
            // Ligne
            canvas.drawLine(x, marge, x, heightMoinsMarge, paintGraduationsGraph);

            // Texte
            canvas.drawText(formatValeursAbscisseGraph.format(valeur), x, yText, paintValeursGraph);

            // Next
            x += deltaX;
            valeur += deltaValeur;
          }
        }

        // Unites
        {
          final float yText = margeSur2 + decalTextValeur;
          final String uniteVent = ActivityCommons.getSpeedUnit();
          canvas.drawText(uniteVent, marge, yText, paintValeursGraph);
          if (hasTempValue)
          {
            final String uniteTemp = ActivityCommons.getTemperatureUnit();
            canvas.drawText(uniteTemp, widthMoinsMarge, yText, paintValeursGraph);
          }
        }

        // Legendes
        final float xLegend = getWidth() / 2;
        final float yLegend = margeSur2;
        for (final ChartLegend legend : legends)
        {
          legend.draw(canvas, xLegend, yLegend);
        }

        // Limites
        {
          // Limite vent moyen
          if ((moyWindLimit != null) && (maxiWind >= moyWindLimit.intValue()))
          {
            final float ymoy = heightMoinsMarge - relativeYMoyWindLimit * hauteur;
            canvas.drawLine(marge, ymoy, widthMoinsMarge, ymoy, paintLimiteMoyGraph);
          }

          // Limite vent maxi
          if (hasMaxWindValue && (maxWindLimit != null) && (maxiWind >= maxWindLimit.intValue()))
          {
            final float ymax = heightMoinsMarge - relativeYMaxWindLimit * hauteur;
            canvas.drawLine(marge, ymax, widthMoinsMarge, ymax, paintLimiteMaxGraph);
          }
        }

        // Axes
        canvas.drawLine(marge, heightMoinsMarge, widthMoinsMarge, heightMoinsMarge, paintAxesGraph);
        canvas.drawLine(marge, marge, marge, heightMoinsMarge, paintAxesGraph);
        canvas.drawLine(widthMoinsMarge, marge, widthMoinsMarge, heightMoinsMarge, paintAxesGraph);

        // Points
        xprec = 0;
        float yprecmin = 0;
        float yprecmoy = 0;
        float yprecmax = 0;
        yprectemp = 0;
        float xselected = 0;
        float yminselected = 0;
        float ymoyselected = 0;
        float ymaxselected = 0;
        for (final ChartPoint point : points)
        {
          final float x = marge + point.abscisse * largeur;
          final boolean selected = (selectedPoint == point);
          if (selected)
          {
            xselected = x;
          }

          // Temperature
          if (!Double.isNaN(point.valeurTemp))
          {
            final float ytemp = heightMoinsMarge - point.relativeYTemp * hauteur;

            if (point.liaisonPrevTemp)
            {
              canvas.drawLine(xprec, yprectemp, x, ytemp, paintTemperatureGraph);
            }
            else if (!point.liaisonNextTemp)
            {
              canvas.drawPoint(x, ytemp, paintPointTemperatureGraph);
            }

            // Next
            yprectemp = ytemp;
          }

          // Vent mini
          if (!Double.isNaN(point.valeurMini))
          {
            final float ymin = heightMoinsMarge - point.relativeYMini * hauteur;
            if (selected)
            {
              yminselected = ymin;
            }

            if (point.liaisonPrevMini)
            {
              canvas.drawLine(xprec, yprecmin, x, ymin, paintVentMiniGraph);
            }
            else if (!point.liaisonNextMini)
            {
              canvas.drawPoint(x, ymin, paintPointVentMiniGraph);
            }

            // Next
            yprecmin = ymin;
          }

          // Vent moyen
          if (!Double.isNaN(point.valeurMoyen))
          {
            final float ymoy = heightMoinsMarge - point.relativeYMoyen * hauteur;
            if (selected)
            {
              ymoyselected = ymoy;
            }

            if (point.liaisonPrevMoyen)
            {
              canvas.drawLine(xprec, yprecmoy, x, ymoy, paintVentMoyenGraph);
            }
            else if (!point.liaisonNextMoyen)
            {
              canvas.drawPoint(x, ymoy, paintPointVentMoyenGraph);
            }

            // Next
            yprecmoy = ymoy;
          }

          // Vent maxi
          if (!Double.isNaN(point.valeurMaxi))
          {
            final float ymax = heightMoinsMarge - point.relativeYMaxi * hauteur;
            if (selected)
            {
              ymaxselected = ymax;
            }

            if (point.liaisonPrevMaxi)
            {
              canvas.drawLine(xprec, yprecmax, x, ymax, paintVentMaxiGraph);
            }
            else if (!point.liaisonNextMaxi)
            {
              canvas.drawPoint(x, ymax, paintPointVentMaxiGraph);
            }

            // Next
            yprecmax = ymax;
          }

          // Next
          xprec = x;
        }

        // Selection
        if ((selectedPoint != null) && (touchListener != null))
        {
          // Ligne selection
          canvas.drawLine(xselected, marge, xselected, heightMoinsMarge, paintLigneSelectionGraph);

          // Point mini
          if (!Double.isNaN(selectedPoint.valeurMini))
          {
            canvas.drawPoint(xselected, yminselected, paintPointSelectionGraph);
          }

          // Point moyen
          if (!Double.isNaN(selectedPoint.valeurMoyen))
          {
            canvas.drawPoint(xselected, ymoyselected, paintPointSelectionGraph);
          }

          // Point maxi
          if (!Double.isNaN(selectedPoint.valeurMaxi))
          {
            canvas.drawPoint(xselected, ymaxselected, paintPointSelectionGraph);
          }
        }
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class TendancieView extends TextView
  {
    private static final String STRING_EGAL = "=";

    private final WindIconInfos infos       = new WindIconInfos();
    private final Point         point       = new Point(0, 0);
    private float               decalY;

    /**
     * 
     * @param context
     */
    TendancieView(final Context context)
    {
      super(context);
    }

    @Override
    public void draw(final Canvas canvas)
    {
      if (Utils.isStringVide(getText().toString()))
      {
        point.x = getWidth() / 2;
        point.y = (int)(decalY + getHeight() / 2);
        DrawingCommons.drawTendanceVentIfNeeded(canvas, point, infos);
      }
      else
      {
        super.draw(canvas);
      }
    }

    /**
     * 
     * @param releve
     * @param value
     * @param valueTendance
     */
    void validate(final Releve releve, final double value, final double valueTendance)
    {
      final TendanceVent tendance = DrawingCommons.validateTendance(infos, releve, value, valueTendance);
      switch (tendance)
      {
        case FAIBLE_BAISSE:
        case FORTE_BAISSE:
          decalY = decalTendanceBaisse;
          setText(Strings.VIDE);
          break;
        case FAIBLE_HAUSSE:
        case FORTE_HAUSSE:
          decalY = decalTendanceHausse;
          setText(Strings.VIDE);
          break;
        case STABLE:
          decalY = 0;
          setText(STRING_EGAL);
          break;
        case INCONNUE:
        default:
          decalY = 0;
          setText(Strings.VIDE);
          break;
      }
    }
  }

  /**
   * 
   * @author pedro.m
   */
  private static class HistoryAdapter extends BaseAdapter
  {
    private static final int    ID_DIVERS              = 1;
    private static final int    ID_BALISE              = 2;
    private static final int    ID_HEURE               = 3;
    private static final int    ID_LABEL_VENT_MINI     = 10;
    private static final int    ID_VENT_MINI           = 11;
    private static final int    ID_TEND_MINI           = 12;
    private static final int    ID_UNIT_VENT_MINI      = 13;
    private static final int    ID_LABEL_VENT_MOYEN    = 20;
    private static final int    ID_VENT_MOYEN          = 21;
    private static final int    ID_TEND_MOYEN          = 22;
    private static final int    ID_UNIT_VENT_MOYEN     = 23;
    private static final int    ID_LABEL_VENT_MAXI     = 30;
    private static final int    ID_VENT_MAXI           = 31;
    private static final int    ID_TEND_MAXI           = 32;
    private static final int    ID_UNIT_VENT_MAXI      = 33;
    private static final int    ID_LABEL_TEMPERATURE   = 34;
    private static final int    ID_TEMPERATURE         = 35;
    private static final int    ID_TEND_TEMPERATURE    = 36;
    private static final int    ID_UNIT_TEMPERATURE    = 37;
    private static final int    ID_RADAR               = 100;
    private static final int    ID_GRAPH               = 200;

    private static final int    PADDING                = 1;

    private static final String STRING_TITLE_SEPARATOR = " - ";
    private static final String STRING_DATE_PRECEDENTE = "< ";
    private static final String STRING_DATE_SUIVANTE   = " >";

    HistoryActivity             activity;

    private View                currentView;
    private int                 currentViewIndex       = -1;

    /**
     * 
     * @param activity
     */
    HistoryAdapter(final HistoryActivity activity)
    {
      this.activity = activity;
    }

    @Override
    public int getCount()
    {
      return activity.dates.size();
    }

    @Override
    public Object getItem(final int index)
    {
      return Integer.valueOf(index);
    }

    @Override
    public long getItemId(final int index)
    {
      return index;
    }

    /**
     * 
     */
    void shutdown()
    {
      activity = null;
      currentView = null;
    }

    @Override
    public View getView(final int index, final View view, final ViewGroup viewGroup)
    {
      // Initialisations
      Log.d(HistoryActivity.class.getSimpleName(), ">>> getView(" + index + ", " + view + ", ...)");
      final int orientation = activity.getResources().getConfiguration().orientation;

      // Recyclage interne ?
      if ((index == currentViewIndex) && (currentView != null))
      {
        Log.d(HistoryActivity.class.getSimpleName(), "<<< getView() with internal cache : " + currentView);
        return currentView;
      }

      // Vue recyclee ou creation nouvelle ?
      final View finalView;
      if (view != null)
      {
        finalView = view;
      }
      else
      {
        switch (orientation)
        {
          case Configuration.ORIENTATION_PORTRAIT:
            finalView = getNewPortraitView(index, viewGroup);
            break;
          default:
            finalView = getNewLandscapeView(index, viewGroup);
            break;
        }
      }

      // Synchro
      switch (orientation)
      {
        case Configuration.ORIENTATION_PORTRAIT:
          synchronizePortraitView(index, finalView);
          break;
        default:
          synchronizeLandscapeView(index, finalView);
          break;
      }

      // Fin
      currentViewIndex = index;
      currentView = finalView;
      Log.d(HistoryActivity.class.getSimpleName(), "<<< getView() : " + finalView);
      return finalView;
    }

    /**
     * 
     * @return
     */
    private View getNewDiversView()
    {
      // Initialisations
      final Resources resources = activity.getResources();

      // Vue generale
      final RelativeLayout diversView = new RelativeLayout(activity);
      diversView.setId(ID_DIVERS);
      diversView.setBackgroundDrawable(resources.getDrawable(R.drawable.history_item_background));
      if (ActivityCommons.isHoneyComb())
      {
        diversView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
      }

      // Balise
      {
        final HistoryIconView baliseView = new HistoryIconView(activity);
        baliseView.setBalise(activity.balise);
        final RelativeLayout.LayoutParams baliseLayoutParams = new RelativeLayout.LayoutParams(iconViewSize, iconViewSize);
        baliseLayoutParams.setMargins(0, 0, 0, 0);
        baliseLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        baliseLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        baliseView.setId(ID_BALISE);
        baliseView.setLayoutParams(baliseLayoutParams);
        diversView.addView(baliseView);
      }

      // Heure
      final TextView heureView = new TextView(activity);
      heureView.setId(ID_HEURE);
      final RelativeLayout.LayoutParams heureLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
      heureLayoutParams.addRule(RelativeLayout.BELOW, ID_BALISE);
      heureView.setLayoutParams(heureLayoutParams);
      heureView.setPadding(0, 0, 0, 0);
      heureView.setTextAppearance(activity, android.R.style.TextAppearance_Medium);
      heureView.setGravity(Gravity.CENTER);
      heureView.setTextColor(Color.WHITE);
      heureView.setBackgroundColor(Color.TRANSPARENT);
      heureView.setTypeface(heureView.getTypeface(), Typeface.BOLD);
      diversView.addView(heureView);

      // Layout textes
      final LinearLayout textsLayout = new LinearLayout(activity);
      textsLayout.setOrientation(LinearLayout.HORIZONTAL);
      textsLayout.setGravity(Gravity.CENTER);
      textsLayout.setPadding(diversPadding, 0, diversPadding, 0);
      final RelativeLayout.LayoutParams textsLayoutLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
      textsLayoutLayoutParams.addRule(RelativeLayout.BELOW, ID_HEURE);
      textsLayout.setLayoutParams(textsLayoutLayoutParams);
      diversView.addView(textsLayout);

      // Layout labels
      final LinearLayout labelsLayout = new LinearLayout(activity);
      labelsLayout.setOrientation(LinearLayout.VERTICAL);
      labelsLayout.setGravity(Gravity.RIGHT);
      final LinearLayout.LayoutParams labelsLayoutLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
      labelsLayout.setLayoutParams(labelsLayoutLayoutParams);
      textsLayout.addView(labelsLayout);

      // Layout valeurs
      final LinearLayout valuesLayout = new LinearLayout(activity);
      valuesLayout.setOrientation(LinearLayout.VERTICAL);
      valuesLayout.setGravity(Gravity.RIGHT);
      final LinearLayout.LayoutParams valuesLayoutLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
      valuesLayout.setLayoutParams(valuesLayoutLayoutParams);
      textsLayout.addView(valuesLayout);

      // Layout tendances
      final LinearLayout tendancesLayout = new LinearLayout(activity);
      tendancesLayout.setOrientation(LinearLayout.VERTICAL);
      tendancesLayout.setGravity(Gravity.LEFT);
      tendancesLayout.setPadding(tendancesPadding, 0, 0, 0);
      final LinearLayout.LayoutParams tendancesLayoutLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
      tendancesLayout.setLayoutParams(tendancesLayoutLayoutParams);
      textsLayout.addView(tendancesLayout);

      // Layout unites
      final LinearLayout unitsLayout = new LinearLayout(activity);
      unitsLayout.setOrientation(LinearLayout.VERTICAL);
      unitsLayout.setGravity(Gravity.LEFT);
      final LinearLayout.LayoutParams unitsLayoutLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
      unitsLayout.setLayoutParams(unitsLayoutLayoutParams);
      textsLayout.addView(unitsLayout);

      // Label vent mini
      {
        final TextView ventMiniLabelView = new TextView(activity);
        ventMiniLabelView.setId(ID_LABEL_VENT_MINI);
        ventMiniLabelView.setText(resources.getString(R.string.label_history_vent_mini) + Strings.SPACE);
        ventMiniLabelView.setSingleLine(true);
        ventMiniLabelView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ventMiniLabelView.setTextAppearance(activity, android.R.style.TextAppearance_Small);
        ventMiniLabelView.setTextColor(Color.WHITE);
        ventMiniLabelView.setBackgroundColor(Color.TRANSPARENT);
        labelsLayout.addView(ventMiniLabelView);
      }

      // Valeur vent mini
      {
        final TextView ventMiniView = new TextView(activity);
        ventMiniView.setId(ID_VENT_MINI);
        ventMiniView.setSingleLine(true);
        ventMiniView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ventMiniView.setMinimumWidth(largeurValeurVent);
        ventMiniView.setGravity(Gravity.RIGHT);
        ventMiniView.setTextColor(Color.WHITE);
        ventMiniView.setBackgroundColor(Color.TRANSPARENT);
        valuesLayout.addView(ventMiniView);
      }

      // Tendance vent mini
      {
        final TendancieView tendMiniView = new TendancieView(activity);
        tendMiniView.setId(ID_TEND_MINI);
        tendMiniView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        tendMiniView.setMinimumWidth(largeurTendanceVent);
        tendMiniView.setTextColor(Color.WHITE);
        tendMiniView.setBackgroundColor(Color.TRANSPARENT);
        tendancesLayout.addView(tendMiniView);
      }

      // Unite vent mini
      {
        final TextView ventMiniUnitView = new TextView(activity);
        ventMiniUnitView.setId(ID_UNIT_VENT_MINI);
        ventMiniUnitView.setSingleLine(true);
        ventMiniUnitView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ventMiniUnitView.setTextColor(Color.WHITE);
        ventMiniUnitView.setBackgroundColor(Color.TRANSPARENT);
        ventMiniUnitView.setText(Strings.SPACE + ActivityCommons.getSpeedUnit());
        unitsLayout.addView(ventMiniUnitView);
      }

      // Label vent moyen
      {
        final TextView ventMoyenLabelView = new TextView(activity);
        ventMoyenLabelView.setId(ID_LABEL_VENT_MOYEN);
        ventMoyenLabelView.setText(resources.getString(R.string.label_history_vent_moyen) + Strings.SPACE);
        ventMoyenLabelView.setSingleLine(true);
        ventMoyenLabelView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ventMoyenLabelView.setTextColor(Color.WHITE);
        ventMoyenLabelView.setBackgroundColor(Color.TRANSPARENT);
        labelsLayout.addView(ventMoyenLabelView);
      }

      // Valeur vent moyen
      {
        final TextView ventMoyenView = new TextView(activity);
        ventMoyenView.setId(ID_VENT_MOYEN);
        ventMoyenView.setSingleLine(true);
        ventMoyenView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ventMoyenView.setMinimumWidth(largeurValeurVent);
        ventMoyenView.setGravity(Gravity.RIGHT);
        ventMoyenView.setTextColor(Color.WHITE);
        ventMoyenView.setBackgroundColor(Color.TRANSPARENT);
        valuesLayout.addView(ventMoyenView);
      }

      // Tendance vent moyen
      {
        final TendancieView tendMoyenView = new TendancieView(activity);
        tendMoyenView.setId(ID_TEND_MOYEN);
        tendMoyenView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        tendMoyenView.setMinimumWidth(largeurTendanceVent);
        tendMoyenView.setTextColor(Color.WHITE);
        tendMoyenView.setBackgroundColor(Color.TRANSPARENT);
        tendancesLayout.addView(tendMoyenView);
      }

      // Unite vent moyen
      {
        final TextView ventMoyenUnitView = new TextView(activity);
        ventMoyenUnitView.setId(ID_UNIT_VENT_MOYEN);
        ventMoyenUnitView.setSingleLine(true);
        ventMoyenUnitView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ventMoyenUnitView.setTextColor(Color.WHITE);
        ventMoyenUnitView.setBackgroundColor(Color.TRANSPARENT);
        ventMoyenUnitView.setText(Strings.SPACE + ActivityCommons.getSpeedUnit());
        unitsLayout.addView(ventMoyenUnitView);
      }

      // Label vent maxi
      {
        final TextView ventMaxiLabelView = new TextView(activity);
        ventMaxiLabelView.setId(ID_LABEL_VENT_MAXI);
        ventMaxiLabelView.setText(resources.getString(R.string.label_history_vent_maxi) + Strings.SPACE);
        ventMaxiLabelView.setSingleLine(true);
        ventMaxiLabelView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ventMaxiLabelView.setTextColor(Color.WHITE);
        ventMaxiLabelView.setBackgroundColor(Color.TRANSPARENT);
        labelsLayout.addView(ventMaxiLabelView);
      }

      // Valeur vent maxi
      {
        final TextView ventMaxiView = new TextView(activity);
        ventMaxiView.setId(ID_VENT_MAXI);
        ventMaxiView.setSingleLine(true);
        ventMaxiView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ventMaxiView.setMinimumWidth(largeurValeurVent);
        ventMaxiView.setGravity(Gravity.RIGHT);
        ventMaxiView.setTextColor(Color.WHITE);
        ventMaxiView.setBackgroundColor(Color.TRANSPARENT);
        valuesLayout.addView(ventMaxiView);
      }

      // Tendance vent maxi
      {
        final TendancieView tendMaxiView = new TendancieView(activity);
        tendMaxiView.setId(ID_TEND_MAXI);
        tendMaxiView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        tendMaxiView.setMinimumWidth(largeurTendanceVent);
        tendMaxiView.setTextColor(Color.WHITE);
        tendMaxiView.setBackgroundColor(Color.TRANSPARENT);
        tendancesLayout.addView(tendMaxiView);
      }

      // Unite vent maxi
      {
        final TextView ventMaxiUnitView = new TextView(activity);
        ventMaxiUnitView.setId(ID_UNIT_VENT_MAXI);
        ventMaxiUnitView.setSingleLine(true);
        ventMaxiUnitView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        ventMaxiUnitView.setTextColor(Color.WHITE);
        ventMaxiUnitView.setBackgroundColor(Color.TRANSPARENT);
        ventMaxiUnitView.setText(Strings.SPACE + ActivityCommons.getSpeedUnit());
        unitsLayout.addView(ventMaxiUnitView);
      }

      // Label temperature
      {
        final TextView temperatureLabelView = new TextView(activity);
        temperatureLabelView.setId(ID_LABEL_TEMPERATURE);
        temperatureLabelView.setText(resources.getString(R.string.label_history_temperature) + Strings.SPACE);
        temperatureLabelView.setSingleLine(true);
        temperatureLabelView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        temperatureLabelView.setTextColor(Color.WHITE);
        temperatureLabelView.setBackgroundColor(Color.TRANSPARENT);
        labelsLayout.addView(temperatureLabelView);
      }

      // Valeur temperature
      {
        final TextView temperatureView = new TextView(activity);
        temperatureView.setId(ID_TEMPERATURE);
        temperatureView.setSingleLine(true);
        temperatureView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        temperatureView.setMinimumWidth(largeurValeurVent);
        temperatureView.setGravity(Gravity.RIGHT);
        temperatureView.setTextColor(Color.WHITE);
        temperatureView.setBackgroundColor(Color.TRANSPARENT);
        valuesLayout.addView(temperatureView);
      }

      // Tendance temperature
      {
        final TendancieView tendTemperatureView = new TendancieView(activity);
        tendTemperatureView.setId(ID_TEND_TEMPERATURE);
        tendTemperatureView.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        tendTemperatureView.setMinimumWidth(largeurTendanceVent);
        tendTemperatureView.setTextColor(Color.WHITE);
        tendTemperatureView.setBackgroundColor(Color.TRANSPARENT);
        tendancesLayout.addView(tendTemperatureView);
      }

      // Unite temperature
      {
        final TextView temperatureUnitView = new TextView(activity);
        temperatureUnitView.setId(ID_UNIT_TEMPERATURE);
        temperatureUnitView.setSingleLine(true);
        temperatureUnitView.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        temperatureUnitView.setTextColor(Color.WHITE);
        temperatureUnitView.setBackgroundColor(Color.TRANSPARENT);
        temperatureUnitView.setText(Strings.SPACE + ActivityCommons.getTemperatureUnit());
        unitsLayout.addView(temperatureUnitView);
      }

      return diversView;
    }

    /**
     * 
     * @return
     */
    private RadarView getNewRadarView()
    {
      final RadarView radarView = new RadarView(activity, activity.moyWindLimit);
      radarView.setId(ID_RADAR);
      radarView.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.history_item_background));

      return radarView;
    }

    /**
     * 
     * @param touchListener
     * @return
     */
    private ChartView getNewGraphView(final ChartView.ChartTouchListener touchListener)
    {
      final ChartView graphView = new ChartView(activity, touchListener, activity.moyWindLimit, activity.maxWindLimit);
      graphView.setId(ID_GRAPH);
      graphView.setBackgroundDrawable(activity.getResources().getDrawable(R.drawable.history_item_background));

      return graphView;
    }

    /**
     * 
     * @param index
     * @param viewGroup
     * @return
     */
    @SuppressWarnings("unused")
    private View getNewPortraitView(final int index, final ViewGroup viewGroup)
    {
      // Initialisations
      final float scalingFactor = activity.scalingFactor;

      // Layout
      final RelativeLayout layout = new RelativeLayout(activity);
      layout.setLayoutParams(new Gallery.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

      // Divers
      final View diversView = getNewDiversView();
      {
        final WindowManager windowManager = (WindowManager)activity.getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        final int screenHeight = display.getHeight();
        final int height = Math.max(175, (int)(display.getHeight() * 0.4 / scalingFactor));
        final RelativeLayout.LayoutParams diversViewLayoutParams = new RelativeLayout.LayoutParams((int)(140 * scalingFactor), (int)(height * scalingFactor));
        diversView.setLayoutParams(diversViewLayoutParams);
        layout.addView(diversView);
      }

      // Radar
      final RadarView radarView = getNewRadarView();
      {
        final RelativeLayout.LayoutParams radarViewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, 0);
        radarViewLayoutParams.addRule(RelativeLayout.RIGHT_OF, diversView.getId());
        radarViewLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, diversView.getId());
        radarViewLayoutParams.addRule(RelativeLayout.ALIGN_TOP, diversView.getId());
        radarViewLayoutParams.leftMargin = PADDING;
        radarView.setLayoutParams(radarViewLayoutParams);
        layout.addView(radarView);
      }

      // Graphe
      final View graphView = getNewGraphView(new ChartView.ChartTouchListener()
      {
        @Override
        public void onChartTouchEvent(final long abscisse)
        {
          // Recuperation du releve
          final Date date = activity.dates.get(index);
          final List<Releve> releves = activity.releves.get(date);
          final Releve releve = findReleve(releves, abscisse);

          // Details
          synchronizeDetails(releve, layout);

          // Radar
          radarView.setSelectedReleve(releve);
        }
      });

      {
        final RelativeLayout.LayoutParams graphViewLayoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        graphViewLayoutParams.topMargin = PADDING;
        graphViewLayoutParams.addRule(RelativeLayout.BELOW, diversView.getId());
        graphView.setLayoutParams(graphViewLayoutParams);
        layout.addView(graphView);
      }

      return layout;
    }

    /**
     * 
     * @param releves
     * @param abscisse
     * @return
     */
    protected static Releve findReleve(final List<Releve> releves, final long abscisse)
    {
      for (final Releve releve : releves)
      {
        if ((releve.date != null) && (releve.date.getTime() == abscisse))
        {
          return releve;
        }
      }

      return null;
    }

    /**
     * 
     * @param index
     * @param viewGroup
     * @return
     */
    @SuppressWarnings("unused")
    private View getNewLandscapeView(final int index, final ViewGroup viewGroup)
    {
      // Layout
      final LinearLayout layout = new LinearLayout(activity);
      layout.setOrientation(LinearLayout.VERTICAL);
      layout.setLayoutParams(new Gallery.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

      // Graphe
      final View graphView = getNewGraphView(null);
      final LinearLayout.LayoutParams graphLayoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
      graphLayoutParams.topMargin = PADDING;
      graphView.setLayoutParams(graphLayoutParams);
      layout.addView(graphView);

      return layout;
    }

    /**
     * 
     * @param index
     */
    void synchronizeTitleView(final int index)
    {
      // Precedent
      final TextView prevView = (TextView)activity.findViewById(R.id.history_previous);
      prevView.setText(index > 0 ? STRING_DATE_PRECEDENTE : "");

      // Suivant
      final TextView nextView = (TextView)activity.findViewById(R.id.history_next);
      nextView.setText(index < activity.dates.size() - 1 ? STRING_DATE_SUIVANTE : "");

      // Nom balise et date
      final TextView titleView = (TextView)activity.findViewById(R.id.history_title);
      final StringBuilder builder = new StringBuilder();
      builder.append(activity.baliseName);
      builder.append(STRING_TITLE_SEPARATOR);
      builder.append(DATE_FORMAT_TITLE.format(activity.dates.get(index)));
      titleView.setText(builder.toString());
    }

    /**
     * 
     * @param textView
     * @param value
     * @param limite
     */
    private void synchronizeVitesse(final TextView textView, final double value, final Integer limite)
    {
      textView.setText(Html.fromHtml(ActivityCommons.formatDouble(ActivityCommons.getFinalSpeed(value), limite, activity.decimalFormatEntier, null, activity.underLimitColor, activity.overLimitColor)));
    }

    /**
     * 
     * @param textView
     * @param value
     */
    private void synchronizeTemperature(final TextView textView, final double value)
    {
      textView.setText(Html.fromHtml(ActivityCommons.formatDouble(ActivityCommons.getFinalTemperature(value), null, activity.decimalFormatEntier, null, -1, -1)));
    }

    /**
     * 
     * @param tendView
     * @param releve
     * @param value
     * @param tendValue
     */
    private static void synchronizeTendance(final TendancieView tendView, final Releve releve, final double value, final double tendValue)
    {
      tendView.validate(releve, value, tendValue);
      tendView.invalidate();
    }

    /**
     * 
     * @param releve
     * @param view
     */
    void synchronizeDetails(final Releve releve, final View view)
    {
      // Icone balise
      final HistoryIconView iconView = (HistoryIconView)view.findViewById(ID_BALISE);
      iconView.setBaliseDatas(releve);
      iconView.invalidate();

      // Heure
      final TextView heureView = (TextView)view.findViewById(ID_HEURE);
      final Date date = new Date(releve.date.getTime());
      heureView.setText(HEURE_FORMAT.format(date));

      // Vent mini
      final TextView miniView = (TextView)view.findViewById(ID_VENT_MINI);
      synchronizeVitesse(miniView, releve.ventMini, null);

      // Tendance mini
      final TendancieView tendMiniView = (TendancieView)view.findViewById(ID_TEND_MINI);
      synchronizeTendance(tendMiniView, releve, releve.ventMini, releve.ventMiniTendance);

      // Vent moyen
      final TextView moyenView = (TextView)view.findViewById(ID_VENT_MOYEN);
      synchronizeVitesse(moyenView, releve.ventMoyen, activity.moyWindLimit);

      // Tendance moyen
      final TendancieView tendMoyenView = (TendancieView)view.findViewById(ID_TEND_MOYEN);
      synchronizeTendance(tendMoyenView, releve, releve.ventMoyen, releve.ventMoyenTendance);

      // Vent maxi
      final TextView maxiView = (TextView)view.findViewById(ID_VENT_MAXI);
      synchronizeVitesse(maxiView, releve.ventMaxi, activity.maxWindLimit);

      // Tendance maxi
      final TendancieView tendMaxiView = (TendancieView)view.findViewById(ID_TEND_MAXI);
      synchronizeTendance(tendMaxiView, releve, releve.ventMaxi, releve.ventMaxiTendance);

      // Temperature
      final TextView tempView = (TextView)view.findViewById(ID_TEMPERATURE);
      synchronizeTemperature(tempView, releve.temperature);

      // Repaint
      view.invalidate();
    }

    /**
     * 
     * @param index
     * @param view
     */
    private void synchronizeDiversView(final int index, final View view)
    {
      // Initialisations
      final Date date = activity.dates.get(index);
      final List<Releve> releves = activity.releves.get(date);
      final Releve releve = releves.get(releves.size() - 1);

      // Synchro
      synchronizeDetails(releve, view);
    }

    /**
     * 
     * @param index
     * @param view
     */
    private void synchronizeRadarView(final int index, final View view)
    {
      final Date date = activity.dates.get(index);
      final List<Releve> releves = activity.releves.get(date);

      final RadarView radarView = (RadarView)view.findViewById(ID_RADAR);
      radarView.setReleves(releves, activity.deltaPeremption);
    }

    /**
     * 
     * @param index
     * @param view
     */
    private void synchronizeGraphView(final int index, final View view)
    {
      final Date date = activity.dates.get(index);
      final List<Releve> releves = activity.releves.get(date);

      final ChartView chartView = (ChartView)view.findViewById(ID_GRAPH);
      chartView.setReleves(date, releves, activity.deltaPeremption);
    }

    /**
     * 
     * @param index
     * @param view
     */
    private void synchronizePortraitView(final int index, final View view)
    {
      // Details
      synchronizeDiversView(index, view);

      // Radar
      synchronizeRadarView(index, view);

      // Graph
      synchronizeGraphView(index, view);
    }

    /**
     * 
     * @param index
     * @param view
     */
    private void synchronizeLandscapeView(final int index, final View view)
    {
      // Graph
      synchronizeGraphView(index, view);
    }
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onCreate()");
    super.onCreate(savedInstanceState);
    final DisplayMetrics metrics = ActivityCommons.getDisplayMetrics(getApplicationContext());
    scalingFactor = metrics.density;

    // Formats date/heure
    DATE_FORMAT_TITLE = android.text.format.DateFormat.getDateFormat(getApplicationContext());
    HEURE_FORMAT = android.text.format.DateFormat.getTimeFormat(getApplicationContext());

    // Parametres
    baliseName = getIntent().getStringExtra(INTENT_EXTRA_BALISE_NAME);
    providerKey = getIntent().getStringExtra(INTENT_EXTRA_PROVIDER_KEY);
    baliseId = getIntent().getStringExtra(INTENT_EXTRA_BALISE_ID);
    deltaReleves = getIntent().getIntExtra(INTENT_EXTRA_PROVIDER_DELTA_RELEVES, -1);

    // Delta peremption
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(getApplicationContext());
    final int peremptionValue = sharedPreferences.getInt(getResources().getString(R.string.config_map_outofdate_key), Integer.parseInt(getResources().getString(R.string.config_map_outofdate_default), 10));
    deltaPeremption = Math.max(peremptionValue, deltaReleves) * 60000; // de minutes en millisecondes

    // Initialisation
    init(getApplicationContext());

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreate()");
  }

  @Override
  protected void onDestroy()
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onDestroy()");
    super.onDestroy();

    // Notification du service
    providersServiceConnection.privateOnServiceDisconnected();

    // Adapter
    adapter.shutdown();

    // Deconnexion du service
    unbindService(providersServiceConnection);

    // Nettoyage
    cleanUp();

    // Liberation vues
    ActivityCommons.unbindDrawables(findViewById(android.R.id.content));

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onDestroy()");
  }

  /**
   * 
   */
  private void cleanUp()
  {
    dates.clear();
    releves.clear();
  }

  /**
   * 
   */
  void initView()
  {
    setContentView(R.layout.history_layout);
    final Gallery gallery = (Gallery)findViewById(R.id.history_gallery);
    gallery.setAdapter(adapter);
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(getApplicationContext());
    int selection = sharedPreferences.getInt(PREFERENCES_ACTIVE_INDEX, adapter.getCount() - 1);
    if ((selection < 0) || (selection >= adapter.getCount()))
    {
      selection = adapter.getCount() - 1;
    }
    gallery.setSelection(selection);
    gallery.setOnItemSelectedListener(new OnItemSelectedListener()
    {
      @Override
      public void onItemSelected(final AdapterView<?> inAdapter, final View view, final int position, final long id)
      {
        // Synchro du titre
        adapter.synchronizeTitleView(position);

        // Sauvegarde de la position dans les preferences
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PREFERENCES_ACTIVE_INDEX, position);
        ActivityCommons.commitPreferences(editor);
      }

      @Override
      public void onNothingSelected(final AdapterView<?> inAdapter)
      {
        // Nothing
      }
    });
  }

  /**
   * 
   * @param context
   */
  private void init(final Context context)
  {
    // Resources
    initResources(context);
    initGraphics();
    initPaints();
    initSizes();

    // Service
    initProvidersService();
  }

  /**
   * 
   */
  private void initResources(final Context context)
  {
    // Initialisations
    final Resources resources = context.getResources();
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(getApplicationContext());

    // Divers
    decimalFormatEntier = new DecimalFormat(resources.getString(R.string.number_format_history_entier));

    // Couleurs
    overLimitColor = resources.getColor(R.color.map_balise_texte_ko);
    underLimitColor = resources.getColor(R.color.map_balise_texte_ok);

    // Limite Vent Moyen
    final boolean moyChecked = sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_moy_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_moy_check_default)));
    if (moyChecked)
    {
      moyWindLimit = Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_map_limit_moy_edit_key), Integer.parseInt(resources.getString(R.string.config_map_limit_moy_edit_default), 10)));
    }
    else
    {
      moyWindLimit = null;
    }

    // Limite Vent Maxi
    final boolean maxChecked = sharedPreferences.getBoolean(resources.getString(R.string.config_map_limit_max_check_key), Boolean.parseBoolean(resources.getString(R.string.config_map_limit_max_check_default)));
    if (maxChecked)
    {
      maxWindLimit = Integer.valueOf(sharedPreferences.getInt(resources.getString(R.string.config_map_limit_max_edit_key), Integer.parseInt(resources.getString(R.string.config_map_limit_max_edit_default), 10)));
    }
    else
    {
      maxWindLimit = null;
    }

    // Limite de peremption releve
    try
    {
      deltaPeremptionMs = 60000L * sharedPreferences.getInt(resources.getString(R.string.config_map_outofdate_key), Integer.parseInt(resources.getString(R.string.config_map_outofdate_default), 10));
    }
    catch (final NumberFormatException nfe)
    {
      Log.e(BaliseDrawable.class.getSimpleName(), nfe.getMessage(), nfe);
    }
  }

  /**
   * 
   */
  private void initGraphics()
  {
    // Gestion de la densite de pixels de l'ecran
    DrawingCommons.initialize(getApplicationContext());
    FullDrawingCommons.initialize(getApplicationContext());
  }

  /**
   * 
   */
  private void initPaints()
  {
    final DashPathEffect limitsEffect = new DashPathEffect(new float[] { 4 * scalingFactor, 6 * scalingFactor }, 0);

    paintAxesRadar.setStyle(Paint.Style.STROKE);
    paintAxesRadar.setStrokeWidth(0.6f * scalingFactor);
    paintAxesRadar.setColor(COULEUR_AXES_RADAR);

    paintAxeExterieurRadar.set(paintAxesRadar);
    paintAxeExterieurRadar.setStrokeWidth(1.5f * scalingFactor);

    paintDirectionsRadar.setColor(COULEUR_DIRECTIONS_RADAR);
    paintDirectionsRadar.setTextAlign(Align.CENTER);
    paintDirectionsRadar.setTextSize(DrawingCommons.TEXT_SIZE * 0.85f);

    paintValeursRadar.setColor(COULEUR_VALEURS_RADAR);
    paintValeursRadar.setTextSize(paintDirectionsRadar.getTextSize() * 0.75f);
    paintValeursRadar.setTextAlign(Align.CENTER);

    paintGraphRadar.setStyle(Paint.Style.STROKE);
    paintGraphRadar.setStrokeWidth(8f * scalingFactor);
    paintGraphRadar.setStrokeCap(Cap.ROUND);
    paintPointGraphRadar.set(paintGraphRadar);
    paintPointGraphRadar.setStrokeCap(Cap.ROUND);
    paintPointGraphRadar.setStrokeWidth(10f * scalingFactor);
    paintLimiteMoyRadar.set(paintGraphRadar);
    paintLimiteMoyRadar.setPathEffect(limitsEffect);
    paintLimiteMoyRadar.setStrokeWidth(3f * scalingFactor);
    paintLimiteMoyRadar.setColor(COULEUR_LIMITE_MOY_RADAR);

    paintPointRadar.setStrokeWidth(14f * scalingFactor);
    paintPointRadar.setStrokeCap(Cap.ROUND);
    paintPointRadar.setStyle(Paint.Style.FILL_AND_STROKE);
    paintPointRadar.setColor(COULEUR_POINT_RADAR);

    paintAxesGraph.set(paintAxesRadar);
    paintAxesGraph.setStrokeWidth(1.5f * scalingFactor);
    paintAxesGraph.setColor(COULEUR_AXES_GRAPH);

    paintGraduationsGraph.set(paintAxesGraph);
    paintGraduationsGraph.setStrokeWidth(0.6f * scalingFactor);
    paintGraduationsGraph.setColor(COULEUR_GRADUATIONS_GRAPH);

    paintVentMiniGraph.setStyle(Paint.Style.STROKE);
    paintVentMiniGraph.setStrokeWidth(4f * scalingFactor);
    paintVentMiniGraph.setStrokeCap(Cap.ROUND);
    paintVentMiniGraph.setColor(COULEUR_VENT_MINI_GRAPH);

    paintVentMoyenGraph.set(paintVentMiniGraph);
    paintVentMoyenGraph.setStrokeCap(Cap.ROUND);
    paintVentMoyenGraph.setColor(COULEUR_VENT_MOYEN_GRAPH);
    paintLimiteMoyGraph.set(paintVentMoyenGraph);
    paintLimiteMoyGraph.setStrokeWidth(3f * scalingFactor);
    paintLimiteMoyGraph.setPathEffect(limitsEffect);

    paintVentMaxiGraph.set(paintVentMiniGraph);
    paintVentMaxiGraph.setStrokeCap(Cap.ROUND);
    paintVentMaxiGraph.setColor(COULEUR_VENT_MAXI_GRAPH);
    paintLimiteMaxGraph.set(paintVentMaxiGraph);
    paintLimiteMaxGraph.setStrokeWidth(3f * scalingFactor);
    paintLimiteMaxGraph.setPathEffect(limitsEffect);

    paintTemperatureGraph.set(paintVentMiniGraph);
    paintTemperatureGraph.setStrokeWidth(1f * scalingFactor);
    paintTemperatureGraph.setColor(COULEUR_TEMPERATURE_GRAPH);
    paintFondTemperatureGraph.set(paintTemperatureGraph);
    paintFondTemperatureGraph.setStrokeWidth(1f * scalingFactor);
    paintFondTemperatureGraph.setStyle(Paint.Style.FILL_AND_STROKE);
    paintFondTemperatureGraph.setColor(COULEUR_FOND_TEMPERATURE_GRAPH);

    paintVentMiniLegendeGraph.setColor(COULEUR_VENT_MINI_GRAPH);
    paintVentMiniLegendeGraph.setStyle(Paint.Style.FILL_AND_STROKE);
    paintVentMoyenLegendeGraph.set(paintVentMiniLegendeGraph);
    paintVentMoyenLegendeGraph.setColor(COULEUR_VENT_MOYEN_GRAPH);
    paintVentMaxiLegendeGraph.set(paintVentMiniLegendeGraph);
    paintVentMaxiLegendeGraph.setColor(COULEUR_VENT_MAXI_GRAPH);
    paintTemperatureLegendeGraph.set(paintVentMiniLegendeGraph);
    paintTemperatureLegendeGraph.setColor(COULEUR_TEMPERATURE_GRAPH);

    final float pointStrokeWidthGraph = 8f * scalingFactor;
    paintPointVentMiniGraph.set(paintVentMiniGraph);
    paintPointVentMiniGraph.setStrokeCap(Cap.ROUND);
    paintPointVentMiniGraph.setStrokeWidth(pointStrokeWidthGraph);
    paintPointVentMoyenGraph.set(paintVentMoyenGraph);
    paintPointVentMoyenGraph.setStrokeCap(Cap.ROUND);
    paintPointVentMoyenGraph.setStrokeWidth(pointStrokeWidthGraph);
    paintPointVentMaxiGraph.set(paintVentMaxiGraph);
    paintPointVentMaxiGraph.setStrokeCap(Cap.ROUND);
    paintPointVentMaxiGraph.setStrokeWidth(pointStrokeWidthGraph);
    paintPointTemperatureGraph.set(paintTemperatureGraph);
    paintPointTemperatureGraph.setStrokeWidth(pointStrokeWidthGraph);

    paintValeursGraph.setColor(COULEUR_VALEURS_GRAPH);
    paintValeursGraph.setTextSize(paintDirectionsRadar.getTextSize() * 0.75f);
    paintValeursGraph.setTextAlign(Align.CENTER);

    paintTexteLegendeGraph.set(paintValeursGraph);
    paintTexteLegendeGraph.setTextAlign(Align.LEFT);

    paintLigneSelectionGraph.setColor(COULEUR_LIGNE_SELECTION_GRAPH);
    paintLigneSelectionGraph.setStrokeWidth(1f * scalingFactor);

    paintPointSelectionGraph.setColor(COULEUR_POINT_SELECTION_GRAPH);
    paintPointSelectionGraph.setStrokeWidth(9f * scalingFactor);
    paintPointSelectionGraph.setStrokeCap(Cap.ROUND);
  }

  /**
   * 
   */
  private void initSizes()
  {
    marge = 27 * scalingFactor;
    margeFois2 = marge * 2;
    margeSur2 = marge / 2;
    margeSur3 = marge / 3;
    iconViewSize = (int)Math.ceil(66 * scalingFactor);
    diversPadding = Math.round(7 * scalingFactor);
    tendancesPadding = Math.round(4 * scalingFactor);
    largeurValeurVent = (int)(25 * scalingFactor);
    largeurTendanceVent = (int)(12 * scalingFactor);
    margeBoxLegende = 5f * scalingFactor;
    margeLegendeLegende = 10f * scalingFactor;
    decalTendanceHausse = 8f * scalingFactor;
    decalTendanceBaisse = -6f * scalingFactor;
  }

  /**
   * 
   * @author pedro.m
   */
  private static class ProvidersServiceConnection implements ServiceConnection
  {
    private HistoryActivity historyActivity;

    /**
     * 
     * @param historyActivity
     */
    ProvidersServiceConnection(final HistoryActivity historyActivity)
    {
      this.historyActivity = historyActivity;
    }

    @Override
    public void onServiceConnected(final ComponentName name, final IBinder inBinder)
    {
      // Initialisations
      final Resources resources = historyActivity.getResources();

      // Recuperation du service
      historyActivity.providersService = (IFullProvidersService)((ProvidersServiceBinder)inBinder).getService();

      // Deja fini ?
      if (historyActivity.isFinishing())
      {
        return;
      }

      // Recuperation du provider
      final BaliseProvider provider = historyActivity.providersService.getBaliseProvider(historyActivity.providerKey);
      if (provider == null)
      {
        return;
      }

      // Recuperation de la balise
      historyActivity.balise = provider.getBaliseById(historyActivity.baliseId);
      if (historyActivity.balise == null)
      {
        return;
      }
      final String baliseName = historyActivity.balise.nom;

      // Recuperation des donnees
      try
      {
        final Collection<Releve> history = historyActivity.providersService.getHistory(historyActivity.providerKey, historyActivity.baliseId);
        historyActivity.analyseHistorique(history);
      }
      catch (final IOException ioe)
      {
        Log.e(HistoryActivity.class.getSimpleName(), "Erreur donnees historique", ioe);
        ActivityCommons.alertDialog(historyActivity, ActivityCommons.ALERT_DIALOG_HISTORY_READ_ERROR, R.drawable.icon, resources.getString(R.string.message_error_title),
            resources.getString(R.string.message_history_read_error, baliseName, provider.filterExceptionMessage(ioe.getMessage())), null, true, null, 0);
      }

      // Initialisation vues
      historyActivity.initView();

      // Analytics
      ActivityCommons.trackEvent(historyActivity.providersService, AnalyticsService.CAT_HISTORY_MODE, AnalyticsService.ACT_HISTORY, historyActivity.providerKey + "." + historyActivity.baliseId);
    }

    @Override
    public void onServiceDisconnected(final ComponentName name)
    {
      privateOnServiceDisconnected();
    }

    /**
     * 
     */
    void privateOnServiceDisconnected()
    {
      // Fin
      historyActivity = null;
    }
  }

  /**
   * 
   */
  private void initProvidersService()
  {
    // Initialisation service
    providersServiceConnection = new ProvidersServiceConnection(this);

    // Connexion au service
    final Intent providersServiceIntent = new Intent(getApplicationContext(), ProvidersService.class);
    bindService(providersServiceIntent, providersServiceConnection, Context.BIND_AUTO_CREATE);
  }

  /**
   * 
   * @param releves
   */
  void analyseHistorique(final Collection<Releve> history)
  {
    // Initialisations
    dates.clear();
    releves.clear();

    // Pour chaque releve
    final Calendar calendar = Calendar.getInstance();
    for (final Releve releve : history)
    {
      // Date du jour
      calendar.setTimeInMillis(releve.date.getTime());
      calendar.set(Calendar.HOUR, 0);
      calendar.set(Calendar.HOUR_OF_DAY, 0);
      calendar.set(Calendar.MINUTE, 0);
      calendar.set(Calendar.SECOND, 0);
      calendar.set(Calendar.MILLISECOND, 0);
      final Date date = new Date(calendar.getTime().getTime());

      // Repartition par date
      List<Releve> relevesDate = releves.get(date);
      if (relevesDate == null)
      {
        relevesDate = new ArrayList<Releve>();
        releves.put(date, relevesDate);
        dates.add(date);
      }
      relevesDate.add(releve);
    }

    // Fin
    Log.d(getClass().getSimpleName(), "dates : " + dates);
  }
}
