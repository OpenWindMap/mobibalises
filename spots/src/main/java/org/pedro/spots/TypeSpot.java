package org.pedro.spots;

/**
 * 
 * @author pedro.m
 */
public enum TypeSpot
{
  DECOLLAGE("deco"), ATTERRISSAGE("atterro"), SPOT("spot");

  private String key;

  /**
   * 
   * @param key;
   */
  private TypeSpot(final String key)
  {
    this.key = key;
  }

  /**
   * 
   * @return
   */
  public String getKey()
  {
    return key;
  }
}
