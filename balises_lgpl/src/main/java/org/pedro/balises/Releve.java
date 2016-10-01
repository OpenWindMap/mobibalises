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
import java.io.Serializable;
import java.util.Date;

import org.pedro.saveable.Saveable;
import org.pedro.saveable.SaveableUtils;

/**
 * 
 * @author pedro.m
 */
public class Releve implements Serializable, Saveable
{
  private static final long serialVersionUID        = 179670899197547407L;

  public static final int   PLUIE_AUCUNE            = 0;
  public static final int   PLUIE_FAIBLE            = 1;                  // hydro < 2 mm/h
  public static final int   PLUIE_MOYENNE           = 2;                  // 2 <= hydro < 7.6 mm/h
  public static final int   PLUIE_FORTE             = 3;                  // 7.6 <= hydro < 10 mm/h
  public static final int   PLUIE_TRES_FORTE        = 4;                  // hydro >= 10 mm/h

  public String             id;
  public Date               date;
  public Date               dateRelevePrecedent;
  public double             ventMoyen               = Double.NaN;
  public double             ventMoyenTendance       = Double.NaN;
  public double             ventMini                = Double.NaN;
  public double             ventMiniTendance        = Double.NaN;
  public double             ventMaxi                = Double.NaN;
  public double             ventMaxiTendance        = Double.NaN;
  public Date               dateHeureVentMaxi;
  public int                directionMoyenne        = Integer.MIN_VALUE;
  public int                directionInstantanee    = Integer.MIN_VALUE;
  public int                directionVentVariation1 = Integer.MIN_VALUE;
  public int                directionVentVariation2 = Integer.MIN_VALUE;
  public double             temperature             = Double.NaN;
  public double             pointRosee              = Double.NaN;
  public int                pluie                   = Integer.MIN_VALUE;  // 0:aucune, 1:pluie faible, 2:pluie, 3:pluie forte, 4:deluge/orage
  public double             hydrometrie             = Double.NaN;
  public int                nuages                  = Integer.MIN_VALUE;  // En 8emes
  public int                plafondNuages           = Integer.MIN_VALUE;
  public int                nuagesBourgeonnants     = Utils.BOOLEAN_NULL;
  public double             pression                = Double.NaN;
  public String             luminosite;
  public int                humidite                = Integer.MIN_VALUE;

  private int               hashCode;

  /**
   * 
   */
  public void clear()
  {
    id = null;
    date = null;
    dateRelevePrecedent = null;
    ventMoyen = Double.NaN;
    ventMoyenTendance = Double.NaN;
    ventMini = Double.NaN;
    ventMiniTendance = Double.NaN;
    ventMaxi = Double.NaN;
    ventMaxiTendance = Double.NaN;
    dateHeureVentMaxi = null;
    directionMoyenne = Integer.MIN_VALUE;
    directionInstantanee = Integer.MIN_VALUE;
    directionVentVariation1 = Integer.MIN_VALUE;
    directionVentVariation2 = Integer.MIN_VALUE;
    temperature = Double.NaN;
    pointRosee = Double.NaN;
    pluie = Integer.MIN_VALUE;
    hydrometrie = Double.NaN;
    nuages = Integer.MIN_VALUE;
    plafondNuages = Integer.MIN_VALUE;
    nuagesBourgeonnants = Utils.BOOLEAN_NULL;
    pression = Double.NaN;
    luminosite = null;
    humidite = Integer.MIN_VALUE;

    hashCode = 0;
  }

  /**
   * 
   */
  public void calculatePluieFromHydrometrie()
  {
    // Pas d'info
    if (Double.isNaN(hydrometrie))
    {
      pluie = Integer.MIN_VALUE;
      return;
    }

    // Pas de pluie
    if (hydrometrie == 0)
    {
      pluie = PLUIE_AUCUNE;
      return;
    }

    // Faible
    if (hydrometrie < 2)
    {
      pluie = PLUIE_FAIBLE;
      return;
    }

    // Moyenne
    if (hydrometrie < 7.6)
    {
      pluie = PLUIE_MOYENNE;
      return;
    }

    // Forte
    if (hydrometrie < 10)
    {
      pluie = PLUIE_FORTE;
      return;
    }

    // Tres forte
    pluie = PLUIE_TRES_FORTE;
  }

  /**
   * 
   * @param releve
   */
  public void copyFrom(final Releve releve)
  {
    id = releve.id;
    date = releve.date;
    dateRelevePrecedent = releve.dateRelevePrecedent;
    ventMoyen = releve.ventMoyen;
    ventMoyenTendance = releve.ventMoyenTendance;
    ventMini = releve.ventMini;
    ventMiniTendance = releve.ventMiniTendance;
    ventMaxi = releve.ventMaxi;
    ventMaxiTendance = releve.ventMaxiTendance;
    dateHeureVentMaxi = releve.dateHeureVentMaxi;
    directionMoyenne = releve.directionMoyenne;
    directionInstantanee = releve.directionInstantanee;
    directionVentVariation1 = releve.directionVentVariation1;
    directionVentVariation2 = releve.directionVentVariation2;
    temperature = releve.temperature;
    pointRosee = releve.pointRosee;
    pluie = releve.pluie;
    hydrometrie = releve.hydrometrie;
    nuages = releve.nuages;
    plafondNuages = releve.plafondNuages;
    nuagesBourgeonnants = releve.nuagesBourgeonnants;
    pression = releve.pression;
    luminosite = releve.luminosite;
    humidite = releve.humidite;

    hashCode = releve.hashCode;
  }

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

    date = SaveableUtils.readDate(in);
    dateRelevePrecedent = SaveableUtils.readDate(in);
    ventMoyen = in.readDouble();
    ventMoyenTendance = in.readDouble();
    ventMini = in.readDouble();
    ventMiniTendance = in.readDouble();
    ventMaxi = in.readDouble();
    ventMaxiTendance = in.readDouble();
    dateHeureVentMaxi = SaveableUtils.readDate(in);
    directionMoyenne = in.readInt();
    directionInstantanee = in.readInt();
    directionVentVariation1 = in.readInt();
    directionVentVariation2 = in.readInt();
    temperature = in.readDouble();
    pointRosee = in.readDouble();
    pluie = in.readInt();
    nuages = in.readInt();
    plafondNuages = in.readInt();
    nuagesBourgeonnants = in.readInt();
    hydrometrie = in.readDouble();
    pression = in.readDouble();
    luminosite = SaveableUtils.readString(in);
    humidite = in.readInt();
  }

  @Override
  public void saveSaveable(final DataOutputStream out) throws IOException
  {
    SaveableUtils.saveSerialUID(out, this);
    SaveableUtils.writeString(out, id);
    SaveableUtils.writeDate(out, date);
    SaveableUtils.writeDate(out, dateRelevePrecedent);
    out.writeDouble(ventMoyen);
    out.writeDouble(ventMoyenTendance);
    out.writeDouble(ventMini);
    out.writeDouble(ventMiniTendance);
    out.writeDouble(ventMaxi);
    out.writeDouble(ventMaxiTendance);
    SaveableUtils.writeDate(out, dateHeureVentMaxi);
    out.writeInt(directionMoyenne);
    out.writeInt(directionInstantanee);
    out.writeInt(directionVentVariation1);
    out.writeInt(directionVentVariation2);
    out.writeDouble(temperature);
    out.writeDouble(pointRosee);
    out.writeInt(pluie);
    out.writeInt(nuages);
    out.writeInt(plafondNuages);
    out.writeInt(nuagesBourgeonnants);
    out.writeDouble(hydrometrie);
    out.writeDouble(pression);
    SaveableUtils.writeString(out, luminosite);
    out.writeInt(humidite);
  }

  @Override
  public String toString()
  {
    return "id=" + id + ", date=" + date + ", dateRelPrec=" + dateRelevePrecedent + ", moy=" + ventMoyen + ", moyTend=" + ventMoyenTendance + ", maxi=" + ventMaxi + ", maxiTend=" + ventMaxiTendance + ", dateHeureVentMaxi="
        + dateHeureVentMaxi + ", mini=" + ventMini + ", miniTend=" + ventMiniTendance + ", dirMoy=" + directionMoyenne + ", dirInst=" + directionInstantanee + ", dirVar1=" + directionVentVariation1 + ", dirVar2=" + directionVentVariation2
        + ", temp=" + temperature + ", rosee=" + pointRosee + ", pluie=" + pluie + ", nuages=" + nuages + ", plafondNuages=" + plafondNuages + ", cum/cb=" + nuagesBourgeonnants + ", hydro=" + hydrometrie + ", pression=" + pression;
  }

  /**
   * 
   * @param b
   * @return
   */
  @Override
  public boolean equals(final Object object)
  {
    if (object == null)
    {
      return false;
    }

    if (!Releve.class.isAssignableFrom(object.getClass()))
    {
      return false;
    }

    final Releve releve = (Releve)object;

    return id.equals(releve.id) && ((date == null) || date.equals(releve.date));
  }

  /**
   * 
   */
  private void calculateHashCode()
  {
    hashCode = (id == null ? 0 : id.hashCode());
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
