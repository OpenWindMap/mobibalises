/*******************************************************************************
 * BalisesLib is Copyright 2012 by Pedro M.
 * 
 * This file is part of BalisesLib.
 *
 * BalisesLib is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * BalisesLib is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * Commercial Distribution License
 * If you would like to distribute BalisesLib (or portions thereof) under a
 * license other than the "GNU Lesser General Public License, version 3", please
 * contact Pedro M (pedro.pub@free.fr).
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with BalisesLib. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.pedro.balises.ffvl;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pedro.balises.Utils;

/**
 * 
 * @author pedro.m
 */
public abstract class FfvlUtils
{
  private static final Pattern    GPS_PATTERN              = Pattern.compile("([\\d\\.\\,]+)\\D+([\\d\\.\\,]+)\\D+([\\d\\.\\,]+)\\D*([NnSs])\\D*([\\d\\.\\,]+)\\D+([\\d\\.\\,]+)\\D+([\\d\\.\\,]+)\\D*([OoWwEe])\\D*");
  private static final Pattern    GPS_DECIMAL_PATTERN      = Pattern.compile("([\\d\\.\\,]+)\\D*([NnSs])\\D*([\\d\\.\\,]+)\\D*([OoWwEe])\\D*");
  private static final Pattern    GPS_SEMI_DECIMAL_PATTERN = Pattern.compile("([NnSs])([\\d\\.\\,]+)\\D+([\\d\\.\\,]+)\\D+([OoWwEe])([\\d\\.\\,]+)\\D+([\\d\\.\\,]+)\\D*");
  private static final DateFormat DATE_FORMAT              = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final String     VIRGULE                  = ",";
  private static final String     POINT                    = ".";
  private static final String     NORD                     = "n";
  private static final String     EST                      = "e";

  /**
   * 
   * @param text
   * @return
   */
  private static String cleanDouble(final String text)
  {
    final String retour = text.replaceAll(VIRGULE, POINT);

    return retour;
  }

  /**
   * 
   * @param coords
   * @return
   */
  public static Double parseLatitude(final String coords)
  {
    // Initialisations
    Double retour = null;

    // Analyse
    final Matcher matcher = GPS_PATTERN.matcher(coords);
    if (matcher.matches())
    {
      final double degres = Double.parseDouble(cleanDouble(matcher.group(1)));
      final double minutes = Double.parseDouble(cleanDouble(matcher.group(2)));
      final double secondes = Double.parseDouble(cleanDouble(matcher.group(3)));
      final int signe = NORD.equalsIgnoreCase(matcher.group(4)) ? 1 : -1;

      retour = Double.valueOf(signe * Utils.degresMinutesSecondesToFraction(degres, minutes, secondes));
    }
    else
    {
      final Matcher decimalMatcher = GPS_DECIMAL_PATTERN.matcher(coords);
      if (decimalMatcher.matches())
      {
        final int signe = NORD.equalsIgnoreCase(decimalMatcher.group(2)) ? 1 : -1;
        retour = Double.valueOf(signe * Double.parseDouble(cleanDouble(decimalMatcher.group(1))));
      }
      else
      {
        final Matcher semiDecimalMatcher = GPS_SEMI_DECIMAL_PATTERN.matcher(coords);
        if (semiDecimalMatcher.matches())
        {
          final int signe = NORD.equalsIgnoreCase(semiDecimalMatcher.group(1)) ? 1 : -1;
          final double degres = Double.parseDouble(cleanDouble(semiDecimalMatcher.group(2)));
          final double minutes = Double.parseDouble(cleanDouble(semiDecimalMatcher.group(3)));

          retour = Double.valueOf(signe * Utils.degresMinutesToFraction(degres, minutes));
        }
      }
    }

    return retour;
  }

  /**
   * 
   * @param coords
   * @return
   */
  public static Double parseLongitude(final String coords)
  {
    // Initialisations
    Double retour = null;

    // Analyse
    final Matcher matcher = GPS_PATTERN.matcher(coords);
    if (matcher.matches())
    {
      final double degres = Double.parseDouble(cleanDouble(matcher.group(5)));
      final double minutes = Double.parseDouble(cleanDouble(matcher.group(6)));
      final double secondes = Double.parseDouble(cleanDouble(matcher.group(7)));
      final int signe = EST.equalsIgnoreCase(matcher.group(8)) ? 1 : -1;

      retour = Double.valueOf(signe * Utils.degresMinutesSecondesToFraction(degres, minutes, secondes));
    }
    else
    {
      final Matcher decimalMatcher = GPS_DECIMAL_PATTERN.matcher(coords);
      if (decimalMatcher.matches())
      {
        final int signe = EST.equalsIgnoreCase(decimalMatcher.group(4)) ? 1 : -1;
        retour = Double.valueOf(signe * Double.parseDouble(cleanDouble(decimalMatcher.group(3))));
      }
      else
      {
        final Matcher semiDecimalMatcher = GPS_SEMI_DECIMAL_PATTERN.matcher(coords);
        if (semiDecimalMatcher.matches())
        {
          final int signe = EST.equalsIgnoreCase(semiDecimalMatcher.group(4)) ? 1 : -1;
          final double degres = Double.parseDouble(cleanDouble(semiDecimalMatcher.group(5)));
          final double minutes = Double.parseDouble(cleanDouble(semiDecimalMatcher.group(6)));

          retour = Double.valueOf(signe * Utils.degresMinutesToFraction(degres, minutes));
        }
      }
    }

    return retour;
  }

  /**
   * 
   * @param text
   * @return
   * @throws ParseException
   */
  public static Date parseDate(final String text) throws ParseException
  {
    if (Utils.isStringVide(text))
    {
      return null;
    }

    return DATE_FORMAT.parse(text);
  }
}
