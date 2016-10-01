package org.pedro.spots;

/**
 * 
 * @author pedro.m
 */
public enum Pratique
{
  PARAPENTE("parapente"), DELTA("delta"), RIGIDE("rigide"), SPEED_RIDING("speed-riding"), KITE("kite"), CERF_VOLANT("cerf_volant");

  private String key;

  /**
   * 
   * @param key
   */
  private Pratique(final String key)
  {
    this.key = key;
  }

  /**
   * @return the key
   */
  public String getKey()
  {
    return key;
  }
}
