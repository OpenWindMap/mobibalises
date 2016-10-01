/*******************************************************************************
 * BalisesLib is Copyright 2012 by Pedro M.
 * 
 * This file is part of BalisesLib.
 *
 * BalisesLib is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * BalisesLib is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * Commercial Distribution License
 * If you would like to distribute BalisesLib (or portions thereof) under a
 * license other than the "GNU Lesser General Public License, version 3", please
 * contact Pedro M (pedro.pub@free.fr).
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with BalisesLib. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.pedro.balises.ffvl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.pedro.balises.Balise;
import org.pedro.saveable.SaveableUtils;

/**
 * 
 * @author pedro.m
 */
public final class BaliseFfvl extends Balise
{
  private static final long serialVersionUID = 7465967331951477510L;

  public String             departement;
  public Boolean            kite;
  public String             urlDetail;
  public String             urlHistorique;

  @Override
  public long getSerialUID()
  {
    return serialVersionUID;
  }

  @Override
  public void loadSaveable(final DataInputStream in) throws IOException
  {
    super.loadSaveable(in);

    departement = SaveableUtils.readString(in);
    kite = SaveableUtils.readBoolean(in);
    urlDetail = SaveableUtils.readString(in);
    urlHistorique = SaveableUtils.readString(in);
  }

  @Override
  public void saveSaveable(final DataOutputStream out) throws IOException
  {
    super.saveSaveable(out);

    SaveableUtils.writeString(out, departement);
    SaveableUtils.writeBoolean(out, kite);
    SaveableUtils.writeString(out, urlDetail);
    SaveableUtils.writeString(out, urlHistorique);
  }

  /**
   * 
   * @param oos
   * @throws IOException
   */
  private void writeObject(final ObjectOutputStream oos) throws IOException
  {
    writeBalise(oos);
  }

  @Override
  protected void writeBalise(final ObjectOutputStream oos) throws IOException
  {
    super.writeBalise(oos);
    oos.writeObject(departement);
    oos.writeObject(kite);
    oos.writeObject(urlDetail);
    oos.writeObject(urlHistorique);
  }

  /**
   * 
   * @param ois
   * @throws ClassNotFoundException
   * @throws IOException
   */
  private void readObject(final ObjectInputStream ois) throws ClassNotFoundException, IOException
  {
    readBalise(ois);
  }

  @Override
  protected void readBalise(final ObjectInputStream ois) throws IOException, ClassNotFoundException
  {
    super.readBalise(ois);
    departement = (String)ois.readObject();
    kite = (Boolean)ois.readObject();
    urlDetail = (String)ois.readObject();
    urlHistorique = (String)ois.readObject();
  }
}
