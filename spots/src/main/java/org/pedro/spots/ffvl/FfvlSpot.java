package org.pedro.spots.ffvl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.pedro.saveable.SaveableUtils;
import org.pedro.spots.Spot;

/**
 * 
 * @author pedro.m
 */
public final class FfvlSpot extends Spot
{
  private static final long serialVersionUID = -6152676042843195974L;

  public String             idSite;
  public String             idStructure;

  @Override
  public long getSerialUID()
  {
    return serialVersionUID;
  }

  @Override
  public void loadSaveable(final DataInputStream in) throws IOException
  {
    super.loadSaveable(in);

    idSite = SaveableUtils.readString(in);
    idStructure = SaveableUtils.readString(in);
  }

  @Override
  public void saveSaveable(final DataOutputStream out) throws IOException
  {
    super.saveSaveable(out);

    SaveableUtils.writeString(out, idSite);
    SaveableUtils.writeString(out, idStructure);
  }
}
