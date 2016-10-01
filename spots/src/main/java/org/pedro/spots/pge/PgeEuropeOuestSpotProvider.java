package org.pedro.spots.pge;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public final class PgeEuropeOuestSpotProvider extends PgeSpotProvider
{
  private final List<String> countries = new ArrayList<String>();

  /**
   * 
   */
  public PgeEuropeOuestSpotProvider()
  {
    super();

    countries.add("fr");
    countries.add("de");
    countries.add("ch");
    countries.add("at");
    countries.add("es");
    countries.add("it");
    countries.add("be");
    countries.add("dk");
    countries.add("fi");
    countries.add("gr");
    countries.add("gl");
    countries.add("is");
    countries.add("ie");
    countries.add("lu");
    countries.add("mk");
    countries.add("mt");
    countries.add("no");
    countries.add("nl");
    countries.add("pt");
    countries.add("gb");
    countries.add("se");
  }

  @Override
  public List<String> getAvailableCountries()
  {
    return countries;
  }
}
