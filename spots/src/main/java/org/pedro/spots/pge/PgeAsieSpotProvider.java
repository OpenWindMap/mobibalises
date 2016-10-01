package org.pedro.spots.pge;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public final class PgeAsieSpotProvider extends PgeSpotProvider
{
  private final List<String> countries = new ArrayList<String>();

  /**
   * 
   */
  public PgeAsieSpotProvider()
  {
    super();

    countries.add("kh");
    countries.add("cn");
    countries.add("hk");
    countries.add("in");
    countries.add("id");
    countries.add("jp");
    countries.add("kr");
    countries.add("kp");
    countries.add("my");
    countries.add("np");
    countries.add("pk");
    countries.add("ph");
    countries.add("tw");
    countries.add("th");
    countries.add("vn");
  }

  @Override
  public List<String> getAvailableCountries()
  {
    return countries;
  }
}
