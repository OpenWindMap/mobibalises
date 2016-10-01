package org.pedro.balises;

import java.util.Calendar;
import java.util.Date;

/**
 * 
 * @author pedro.m
 */
public class CalculSolaire
{
  private static final int    _3                         = 3;
  private static final double _1_5                       = 1.5;
  private static final int    _365                       = 365;
  private static final int    _2                         = 2;
  private static final int    _0                         = 0;
  private static final int    _4                         = 4;
  private static final int    _720                       = 720;
  private static final double _0_00148                   = 0.00148;
  private static final double _0_002697                  = 0.002697;
  private static final double _0_000907                  = 0.000907;
  private static final double _0_006758                  = 0.006758;
  private static final double _0_070257                  = 0.070257;
  private static final double _0_399912                  = 0.399912;
  private static final double _0_006918                  = 0.006918;
  private static final double _0_040849                  = 0.040849;
  private static final double _0_014615                  = 0.014615;
  private static final double _0_032077                  = 0.032077;
  private static final double _0_001868                  = 0.001868;
  private static final double _0_000075                  = 0.000075;
  private static final double _229_18                    = 229.18;
  private static final double _PI_SUR_180                = Math.PI / 180;
  private static final double _180_SUR_PI                = 180 / Math.PI;
  private static final double COS_90_833_FOIS_PI_SUR_180 = Math.cos(90.833 * _PI_SUR_180);

  private int                 dayOfYear;
  private double              fractionalYear;                                             // radians
  private double              eqtime;                                                     // minutes
  private double              decl;                                                       // radians
  private double              sunriseSunsetHourAngle;                                     // degres

  public Date                 heureLever;
  public Date                 heureCoucher;

  /**
   * 
   * @param date
   * @param latitude
   * @param longitude
   */
  public void calculLeverCoucher(final Date date, final double latitude, final double longitude)
  {
    // Jour de l'annee
    final Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

    // Fractional Year
    //final double hour = 0;
    //fractionalYear = 2 * Math.PI / 365 * (dayOfYear - 1 + (hour - 12) / 24);
    fractionalYear = _2 * Math.PI / _365 * (dayOfYear - _1_5);
    final double deuxFractionalYear = _2 * fractionalYear;
    final double troisFractionalYear = _3 * fractionalYear;

    // eqtime
    eqtime = _229_18 * (_0_000075 + _0_001868 * Math.cos(fractionalYear) - _0_032077 * Math.sin(fractionalYear) - _0_014615 * Math.cos(deuxFractionalYear) - _0_040849 * Math.sin(deuxFractionalYear));

    // decl
    decl = _0_006918 - _0_399912 * Math.cos(fractionalYear) + _0_070257 * Math.sin(fractionalYear) - _0_006758 * Math.cos(deuxFractionalYear) + _0_000907 * Math.sin(deuxFractionalYear) - _0_002697 * Math.cos(troisFractionalYear) + _0_00148
        * Math.sin(troisFractionalYear);

    // Sunrise sinset hour angle
    final double latitudeRadians = latitude * _PI_SUR_180;
    sunriseSunsetHourAngle = Math.acos(COS_90_833_FOIS_PI_SUR_180 / (Math.cos(latitudeRadians) * Math.cos(decl)) - Math.tan(latitudeRadians) * Math.tan(decl));
    final double sunriseSunsetHourAngleDegres = sunriseSunsetHourAngle * _180_SUR_PI;

    // Sunrise
    final double sunrise = _720 + _4 * (-longitude - sunriseSunsetHourAngleDegres) - eqtime;

    // Sunset
    final double sunset = _720 + _4 * (-longitude + sunriseSunsetHourAngleDegres) - eqtime;

    // Lever
    cal.set(Calendar.HOUR_OF_DAY, _0);
    cal.set(Calendar.MINUTE, _0);
    cal.set(Calendar.SECOND, _0);
    cal.add(Calendar.MINUTE, (int)Math.round(sunrise + 1440) % 1440);
    heureLever = new Date(cal.getTimeInMillis());
    //System.out.println("heureLever : " + heureLever);

    // Coucher
    cal.setTime(date);
    cal.set(Calendar.HOUR_OF_DAY, _0);
    cal.set(Calendar.MINUTE, _0);
    cal.set(Calendar.SECOND, _0);
    cal.add(Calendar.MINUTE, (int)Math.round(sunset + (sunrise < 0 ? 1440 : 0)));
    heureCoucher = new Date(cal.getTimeInMillis());
    //System.out.println("heureCoucher : " + heureCoucher);
  }

  /**
   * 
   * @param args
   */
  /*
  public static void main(final String[] args)
  {
    System.out.println(">>> main");

    // Initialisations
    final CalculSolaire calcul = new CalculSolaire();
    final DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    // Grenoble
    calcul.calculLeverCoucher(new Date(), 45.191716, 5.724707);
    System.out.println("----- Grenoble lever : " + df.format(calcul.heureLever));
    System.out.println("----- Grenoble coucher : " + df.format(calcul.heureCoucher));

    // Nassau
    calcul.calculLeverCoucher(new Date(), 25.067874, -77.345181);
    System.out.println("----- Nassau lever : " + df.format(calcul.heureLever));
    System.out.println("----- Nassau coucher : " + df.format(calcul.heureCoucher));

    System.out.println("<<< main");
  }
  */
}
