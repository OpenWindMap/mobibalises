package org.pedro.spots;

/**
 * 
 * @author pedro.m
 */
public enum Orientation
{
  N("n", 1), NNE("nne", 3), NE("ne", 2), ENE("ene", 3), E("e", 1), ESE("ese", 3), SE("se", 2), SSE("sse", 3), S("s", 1), SSO("sso", 3), SO("so", 2), OSO("oso", 3), O("o", 1), ONO("ono", 3), NO("no", 2), NNO("nno", 3);

  private final String key;
  private final int    level;

  /**
   * 
   * @param key
   * @param level
   */
  private Orientation(final String key, final int level)
  {
    this.key = key;
    this.level = level;
  }

  /**
   * 
   * @return
   */
  public String getKey()
  {
    return key;
  }

  public int getLevel()
  {
    return level;
  }

  /**
   * 
   * @param other
   * @return
   */
  public boolean isAlmostTheSame(final Orientation other)
  {
    switch (this)
    {
      case E:
        return (E.equals(other) || ENE.equals(other) || ESE.equals(other));
      case ENE:
        return (ENE.equals(other) || NE.equals(other) || E.equals(other));
      case ESE:
        return (ESE.equals(other) || SE.equals(other) || E.equals(other));
      case N:
        return (N.equals(other) || NNO.equals(other) || NNE.equals(other));
      case NE:
        return (NE.equals(other) || ENE.equals(other) || NNE.equals(other));
      case NNE:
        return (NNE.equals(other) || N.equals(other) || NE.equals(other));
      case NNO:
        return (NNO.equals(other) || NO.equals(other) || N.equals(other));
      case NO:
        return (NO.equals(other) || ONO.equals(other) || NNO.equals(other));
      case O:
        return (O.equals(other) || ONO.equals(other) || OSO.equals(other));
      case ONO:
        return (ONO.equals(other) || O.equals(other) || NO.equals(other));
      case OSO:
        return (OSO.equals(other) || O.equals(other) || SO.equals(other));
      case S:
        return (S.equals(other) || SSO.equals(other) || SSE.equals(other));
      case SE:
        return (SE.equals(other) || SSE.equals(other) || ESE.equals(other));
      case SO:
        return (SO.equals(other) || SSO.equals(other) || OSO.equals(other));
      case SSE:
        return (SSE.equals(other) || S.equals(other) || SE.equals(other));
      case SSO:
        return (SSO.equals(other) || S.equals(other) || SO.equals(other));
    }

    return false;
  }

  /**
   * 
   * @param otherKey
   * @return
   */
  public boolean isAlmostTheSame(final String otherKey)
  {
    final Orientation other = getOrientation(otherKey);

    return isAlmostTheSame(other);
  }

  /**
   * 
   * @param otherKey
   * @return
   */
  public static Orientation getOrientation(final String otherKey)
  {
    if (N.getKey().equals(otherKey))
    {
      return N;
    }
    else if (NNE.getKey().equals(otherKey))
    {
      return NNE;
    }
    else if (NE.getKey().equals(otherKey))
    {
      return NE;
    }
    else if (ENE.getKey().equals(otherKey))
    {
      return ENE;
    }
    else if (E.getKey().equals(otherKey))
    {
      return E;
    }
    else if (ESE.getKey().equals(otherKey))
    {
      return ESE;
    }
    else if (SE.getKey().equals(otherKey))
    {
      return SE;
    }
    else if (SSE.getKey().equals(otherKey))
    {
      return SSE;
    }
    else if (S.getKey().equals(otherKey))
    {
      return S;
    }
    else if (SSO.getKey().equals(otherKey))
    {
      return SSO;
    }
    else if (SO.getKey().equals(otherKey))
    {
      return SO;
    }
    else if (OSO.getKey().equals(otherKey))
    {
      return OSO;
    }
    else if (O.getKey().equals(otherKey))
    {
      return O;
    }
    else if (ONO.getKey().equals(otherKey))
    {
      return ONO;
    }
    else if (NO.getKey().equals(otherKey))
    {
      return NO;
    }
    else if (NNO.getKey().equals(otherKey))
    {
      return NNO;
    }

    return null;
  }

  /**
   * 
   * @return
   */
  public Orientation getOpposee()
  {
    switch (this)
    {
      case N:
        return S;
      case NNE:
        return SSO;
      case NE:
        return SO;
      case ENE:
        return OSO;
      case E:
        return O;
      case ESE:
        return ONO;
      case SE:
        return NO;
      case SSE:
        return NNO;
      case S:
        return N;
      case SSO:
        return NNE;
      case SO:
        return NE;
      case OSO:
        return ENE;
      case O:
        return E;
      case ONO:
        return ESE;
      case NO:
        return SE;
      case NNO:
        return SSE;
    }

    return null;
  }
}
