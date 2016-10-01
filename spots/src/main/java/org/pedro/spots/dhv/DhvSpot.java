package org.pedro.spots.dhv;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.pedro.saveable.SaveableUtils;
import org.pedro.spots.Spot;

/**
 * 
 * @author pedro.m
 */
public final class DhvSpot extends Spot
{
  private static final long serialVersionUID = 6653561663366238704L;

  public String             idSite;

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
  }

  @Override
  public void saveSaveable(final DataOutputStream out) throws IOException
  {
    super.saveSaveable(out);

    SaveableUtils.writeString(out, idSite);
  }
}
