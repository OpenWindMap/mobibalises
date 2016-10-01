package org.pedro.spots;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pedro.saveable.Saveable;
import org.pedro.saveable.SaveableUtils;

/**
 * 
 * @author pedro.m
 */
public class Spot implements Serializable, Saveable
{
  private static final long  serialVersionUID = -3212356554267533754L;

  public String              id;
  public String              nom;
  public Double              latitude;
  public Double              longitude;
  public Integer             altitude;
  public Map<String, String> infos            = new HashMap<String, String>();
  public List<Pratique>      pratiques;
  public List<Orientation>   orientations     = new ArrayList<Orientation>();
  public TypeSpot            type;
  public Date                updateDate;

  private int                hashCode;

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
    latitude = SaveableUtils.readDouble(in);
    longitude = SaveableUtils.readDouble(in);
    altitude = SaveableUtils.readInteger(in);
    infos = loadInfos(in);
    pratiques = loadPratiques(in);
    orientations = loadOrientations(in);
    type = TypeSpot.valueOf(SaveableUtils.readString(in));
    updateDate = SaveableUtils.readDate(in);
  }

  /**
   * 
   * @param in
   * @return
   * @throws IOException
   */
  private static Map<String, String> loadInfos(final DataInputStream in) throws IOException
  {
    // Si pas d'infos
    final int size = in.readInt();
    if (size == Integer.MIN_VALUE)
    {
      return null;
    }

    // Initialisations
    final Map<String, String> infos = new HashMap<String, String>();

    // Infos
    for (int i = 0; i < size; i++)
    {
      infos.put(SaveableUtils.readString(in), SaveableUtils.readString(in));
    }

    return infos;
  }

  /**
   * 
   * @param in
   * @return
   * @throws IOException
   */
  private static List<Pratique> loadPratiques(final DataInputStream in) throws IOException
  {
    // Si pas de pratique
    final int size = in.readInt();
    if (size == Integer.MIN_VALUE)
    {
      return null;
    }

    // Initialisations
    final List<Pratique> pratiques = new ArrayList<Pratique>();

    // Pratiques
    for (int i = 0; i < size; i++)
    {
      pratiques.add(Pratique.valueOf(SaveableUtils.readString(in)));
    }

    return pratiques;
  }

  /**
   * 
   * @param in
   * @return
   * @throws IOException
   */
  private static List<Orientation> loadOrientations(final DataInputStream in) throws IOException
  {
    // Si pas de pratique
    final int size = in.readInt();
    if (size == Integer.MIN_VALUE)
    {
      return null;
    }

    // Initialisations
    final List<Orientation> orientations = new ArrayList<Orientation>();

    // Orientations
    for (int i = 0; i < size; i++)
    {
      orientations.add(Orientation.valueOf(SaveableUtils.readString(in)));
    }

    return orientations;
  }

  @Override
  public void saveSaveable(final DataOutputStream out) throws IOException
  {
    SaveableUtils.saveSerialUID(out, this);

    SaveableUtils.writeString(out, id);
    SaveableUtils.writeString(out, nom);
    SaveableUtils.writeDouble(out, latitude);
    SaveableUtils.writeDouble(out, longitude);
    SaveableUtils.writeInteger(out, altitude);
    saveInfos(out, infos);
    savePratiques(out, pratiques);
    saveOrientations(out, orientations);
    SaveableUtils.writeString(out, type.name());
    SaveableUtils.writeDate(out, updateDate);
  }

  /**
   * 
   * @param out
   * @param inInfos
   * @throws IOException
   */
  private static void saveInfos(final DataOutputStream out, final Map<String, String> inInfos) throws IOException
  {
    // Si pas d'infos
    if (inInfos == null)
    {
      out.writeInt(Integer.MIN_VALUE);
      return;
    }

    out.writeInt(inInfos.size());
    for (final Map.Entry<String, String> entry : inInfos.entrySet())
    {
      SaveableUtils.writeString(out, entry.getKey());
      SaveableUtils.writeString(out, entry.getValue());
    }
  }

  /**
   * 
   * @param out
   * @param pratiques
   * @throws IOException
   */
  private static void savePratiques(final DataOutputStream out, final List<Pratique> inPratiques) throws IOException
  {
    // Si pas de pratique
    if (inPratiques == null)
    {
      out.writeInt(Integer.MIN_VALUE);
      return;
    }

    out.writeInt(inPratiques.size());
    for (final Pratique pratique : inPratiques)
    {
      SaveableUtils.writeString(out, pratique.name());
    }
  }

  /**
   * 
   * @param out
   * @param inOrientations
   * @throws IOException
   */
  private static void saveOrientations(final DataOutputStream out, final List<Orientation> inOrientations) throws IOException
  {
    // Si pas d'orientations
    if (inOrientations == null)
    {
      out.writeInt(Integer.MIN_VALUE);
      return;
    }

    out.writeInt(inOrientations.size());
    for (final Orientation orientation : inOrientations)
    {
      SaveableUtils.writeString(out, orientation.name());
    }
  }

  /**
   * 
   * @param pratique
   */
  public final void addPratique(final Pratique pratique)
  {
    if (pratiques == null)
    {
      pratiques = new ArrayList<Pratique>();
    }
    pratiques.add(pratique);
  }

  @Override
  public String toString()
  {
    return "id=" + id + ", nom=" + nom + ", type=" + type + ", latitude=" + latitude + ", longitude=" + longitude + ", altitude=" + altitude + ", pratiques=" + pratiques + ", orientations=" + orientations;
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

    if (!Spot.class.isAssignableFrom(object.getClass()))
    {
      return false;
    }

    final Spot spot = (Spot)object;

    return id.equals(spot.id);
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
