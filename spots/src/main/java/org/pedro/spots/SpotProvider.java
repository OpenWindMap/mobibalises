package org.pedro.spots;

import java.io.IOException;
import java.util.List;

/**
 * 
 * @author pedro.m
 */
public interface SpotProvider
{
  /**
   * 
   * @return
   */
  public Spot newSpot();

  /**
   * 
   * @return
   * @throws IOException
   */
  public List<String> getAvailableCountries() throws IOException;

  /**
   * 
   * @param countryCode
   * @return
   * @throws IOException
   */
  public List<Spot> getSpots(String countryCode) throws IOException;

  /**
   * 
   * @param id
   * @param otherInfos
   * @return
   */
  public String getLienDetail(String id, Object[] otherInfos);

  /**
   * 
   * @return
   */
  public String[] getInfoKeys();
}
