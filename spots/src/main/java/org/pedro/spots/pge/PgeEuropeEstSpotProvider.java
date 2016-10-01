package org.pedro.spots.pge;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public final class PgeEuropeEstSpotProvider extends PgeSpotProvider
{
  private final List<String> countries = new ArrayList<String>();

  /**
   * 
   */
  public PgeEuropeEstSpotProvider()
  {
    super();

    countries.add("al");
    countries.add("ba");
    countries.add("bg");
    countries.add("hr");
    countries.add("cz");
    countries.add("ee");
    countries.add("hu");
    countries.add("lv");
    countries.add("lt");
    countries.add("md");
    countries.add("me");
    countries.add("pl");
    countries.add("ro");
    countries.add("rs");
    countries.add("ru");
    countries.add("sk");
    countries.add("si");
    countries.add("ua");
  }

  @Override
  public List<String> getAvailableCountries()
  {
    return countries;
  }
}
