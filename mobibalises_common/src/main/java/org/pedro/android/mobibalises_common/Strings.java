package org.pedro.android.mobibalises_common;

import java.text.Normalizer;

import org.pedro.balises.Utils;

import android.os.Build;

/**
 * 
 * @author pedro.m
 */
public abstract class Strings
{
  public static final String   VIDE                       = "";
  public static final String   SPACE                      = " ";
  public static final String   ZERO                       = "0";
  public static final String   NEWLINE                    = "\n";
  public static final String   PIPE                       = "|";
  public static final String   TAB                        = "\t";
  public static final String   TROIS_POINTS_INTERROGATION = "???";
  public static final String   TIRET                      = "-";

  public static final char     CHAR_UNDERSCORE            = '_';
  public static final char     CHAR_POINT                 = '.';
  public static final char     CHAR_NEWLINE               = '\n';
  public static final char     CHAR_ASTERISQUE            = '*';
  public static final char     CHAR_SPACE                 = ' ';
  public static final char     CHAR_PARENTHESE_DEB        = '(';
  public static final char     CHAR_PARENTHESE_FIN        = ')';
  public static final char     CHAR_EGAL                  = '=';
  public static final char     CHAR_INFERIEUR             = '<';
  public static final char     CHAR_SUPERIEUR             = '>';
  public static final char     CHAR_SLASH                 = '/';
  public static final char     CHAR_PIPE                  = '|';
  public static final char     CHAR_AROBASE               = '@';
  public static final char     CHAR_DIESE                 = '#';
  public static final char     CHAR_POINT_INTERROGATION   = '?';
  public static final char     CHAR_MOINS                 = '-';
  public static final char     CHAR_VIRGULE               = ',';

  public static final String   REGEXP_PIPE                = "\\|";

  public static final String   RESOURCES_ARRAY            = "array";
  public static final String   RESOURCES_STRING           = "string";

  public static final String   ENCODING_UTF_8             = "UTF-8";
  public static final String   NULL                       = "null";

  public static final String   HTML_BR                    = "<br>";

  private static final String  HEXA_COLOR_FORMAT          = "%06X";

  // Mirror of the unicode table from 00c0 to 017f without diacritics.
  private static final char    CHAR_U017F                 = '\u017f';
  private static final char    CHAR_U00C0                 = '\u00c0';
  private static final String  TAB_SANS_ACCENTS           = "AAAAAAACEEEEIIII" + "DNOOOOO\u00d7\u00d8UUUUYI\u00df" + "aaaaaaaceeeeiiii" + "\u00f0nooooo\u00f7\u00f8uuuuy\u00fey" + "AaAaAaCcCcCcCcDd" + "DdEeEeEeEeEeGgGg" + "GgGgHhHhIiIiIiIi"
                                                              + "IiJjJjKkkLlLlLlL" + "lLlNnNnNnnNnOoOo" + "OoOoRrRrRrSsSsSs" + "SsTtTtTtUuUuUuUu" + "UuUuWwYyYZzZzZzF";

  // Version API GingerBread ?
  private static final boolean isGingerBread              = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD);

  /**
   * 
   * @param object
   * @return
   */
  public static final String toAdressString(final Object object)
  {
    if (object == null)
    {
      return NULL;
    }

    return object.getClass().getName() + CHAR_AROBASE + Integer.toHexString(object.hashCode());
  }

  /**
   * 
   * @param chaine
   * @return
   */
  public static final String toHtmlString(final String chaine)
  {
    // Vide ?
    if (Utils.isStringVide(chaine))
    {
      return chaine;
    }

    return chaine.replaceAll(NEWLINE, HTML_BR);
  }

  /**
   * 
   * @param color
   * @return
   */
  public static final String toHexColor(final int color)
  {
    final StringBuffer buffer = new StringBuffer();

    buffer.append(CHAR_DIESE);
    buffer.append(String.format(HEXA_COLOR_FORMAT, Integer.valueOf(color & 0xFFFFFF)));

    return buffer.toString();
  }

  /**
   * 
   * @param accentuee
   * @return
   */
  public static String removeAccents(final String accentuee)
  {
    // Methode pour SDK >= 9
    if (isGingerBread)
    {
      return Normalizer.normalize(accentuee, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "").replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    // Methode pour SDK < 9
    return removeDiacritic(accentuee);
  }

  /**
   * Returns string without diacritics - 7 bit approximation.
   *
   * @param source string to convert
   * @return corresponding string without diacritics
   */
  public static String removeDiacritic(final String source)
  {
    final char[] vysl = new char[source.length()];
    for (int i = 0; i < source.length(); i++)
    {
      char one = source.charAt(i);
      if ((one >= CHAR_U00C0) && (one <= CHAR_U017F))
      {
        one = TAB_SANS_ACCENTS.charAt(one - CHAR_U00C0);
      }
      vysl[i] = one;
    }

    return new String(vysl);
  }
}
