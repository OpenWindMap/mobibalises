package org.pedro.spots.pge;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public final class PgeAmeriqueSudSpotProvider extends PgeSpotProvider
{
  private final List<String> countries = new ArrayList<String>();

  /**
   * 
   */
  public PgeAmeriqueSudSpotProvider()
  {
    super();

    countries.add("br"); // Bresil
    countries.add("ar"); // Argentine
    countries.add("bb");
    countries.add("bo");
    countries.add("cl");
    countries.add("co");
    countries.add("cr");
    countries.add("cu");
    countries.add("do");
    countries.add("ec");
    countries.add("gf");
    countries.add("gt");
    countries.add("hn");
    countries.add("jm");
    countries.add("ni");
    countries.add("pe");
    countries.add("pr");
    countries.add("uy");
    countries.add("ve");
  }

  @Override
  public List<String> getAvailableCountries()
  {
    return countries;
  }
}
