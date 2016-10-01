package org.pedro.spots;

/**
 * 
 * @author pedro.m
 */
public abstract class Utils
{
  /**
   * 
   * @param text
   * @return
   */
  public static final Integer parseInteger(final String text)
  {
    if ((text == null) || (text.trim().length() == 0))
    {
      return null;
    }

    return Integer.valueOf(text, 10);
  }

  /**
   * 
   * @param text
   * @return
   */
  public static final Double parseDouble(final String text)
  {
    if ((text == null) || (text.trim().length() == 0))
    {
      return null;
    }

    return Double.valueOf(text);
  }

  /**
   * 
   * @param degres
   * @param minutes
   * @param secondes
   * @return
   */
  public static final double degresMinutesSecondesToFraction(final double degres, final double minutes, final double secondes)
  {
    return degres + (minutes / 60) + (secondes / 3600);
  }

  /**
   * 
   * @param degres
   * @param minutes
   * @param secondes
   * @return
   */
  public static final double degresMinutesToFraction(final double degres, final double minutes)
  {
    return degres + (minutes / 60);
  }

  /**
   * 
   * @param chaine
   * @return
   */
  public static final boolean isStringVide(final String chaine)
  {
    if (chaine == null)
    {
      return true;
    }

    if (chaine.length() == 0)
    {
      return true;
    }

    if (chaine.trim().length() == 0)
    {
      return true;
    }

    return false;
  }
}
