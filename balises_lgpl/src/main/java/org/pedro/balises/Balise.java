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
package org.pedro.balises;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.pedro.saveable.Saveable;
import org.pedro.saveable.SaveableUtils;

/**
 * 
 * @author pedro.m
 */
public class Balise implements Serializable, Saveable
{
  private static final long serialVersionUID = -6293533947624575102L;

  public String             id;
  public String             nom;
  public double             latitude         = Double.NaN;
  public double             longitude        = Double.NaN;
  public int                altitude         = Integer.MIN_VALUE;
  public String             description;
  public String             commentaire;
  public int                active           = Utils.BOOLEAN_NULL;

  private int               hashCode;

  @Override
  public long getSerialUID()
  {
    return serialVersionUID;
  }

  @Override
  public void loadSaveable(final DataInputStream in) throws IOException
  {
    SaveableUtils.checkSerialUID(in, this);

    id = SaveableUtils.readString(in);
    calculateHashCode();

    nom = SaveableUtils.readString(in);
    latitude = in.readDouble();
    longitude = in.readDouble();
    altitude = in.readInt();
    description = SaveableUtils.readString(in);
    commentaire = SaveableUtils.readString(in);
    active = in.readInt();
  }

  @Override
  public void saveSaveable(final DataOutputStream out) throws IOException
  {
    SaveableUtils.saveSerialUID(out, this);
    SaveableUtils.writeString(out, id);
    SaveableUtils.writeString(out, nom);
    out.writeDouble(latitude);
    out.writeDouble(longitude);
    out.writeInt(altitude);
    SaveableUtils.writeString(out, description);
    SaveableUtils.writeString(out, commentaire);
    out.writeInt(active);
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

  /**
   * 
   * @param oos
   * @throws IOException
   */
  protected void writeBalise(final ObjectOutputStream oos) throws IOException
  {
    oos.writeObject(id);
    oos.writeObject(nom);
    oos.writeDouble(latitude);
    oos.writeDouble(longitude);
    oos.writeInt(altitude);
    oos.writeObject(description);
    oos.writeObject(commentaire);
    oos.writeInt(active);
    oos.writeInt(hashCode);
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

  /**
   * 
   * @param ois
   * @throws IOException
   * @throws ClassNotFoundException
   */
  protected void readBalise(final ObjectInputStream ois) throws IOException, ClassNotFoundException
  {
    id = (String)ois.readObject();
    nom = (String)ois.readObject();
    latitude = ois.readDouble();
    longitude = ois.readDouble();
    altitude = ois.readInt();
    description = (String)ois.readObject();
    commentaire = (String)ois.readObject();
    active = ois.readInt();
    hashCode = ois.readInt();
  }

  @Override
  public String toString()
  {
    return id + ", nom=" + nom + ", alt=" + altitude + ", active=" + active + ", lat=" + latitude + ", long=" + longitude + ", comment.=" + commentaire;
  }

  @Override
  public boolean equals(final Object object)
  {
    if (object == null)
    {
      return false;
    }

    if (!Balise.class.isAssignableFrom(object.getClass()))
    {
      return false;
    }

    final Balise balise = (Balise)object;

    return id.equals(balise.id);
  }

  /**
   * 
   */
  private void calculateHashCode()
  {
    hashCode = id.hashCode();
  }

  @Override
  public int hashCode()
  {
    return hashCode;
  }

  /**
   * @param id the id to set
   */
  public final void setId(final String id)
  {
    this.id = id;
    calculateHashCode();
  }
}
