/*******************************************************************************
 * MobiBalisesLib is Copyright 2014 by Pedro M.
 * 
 * This file is part of MobiBalisesLib.
 *
 * MobiBalisesLib is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * MobiBalisesLib is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * Commercial Distribution License
 * If you would like to distribute MobiBalisesLib (or portions thereof) under a
 * license other than the "GNU Lesser General Public License, version 3", please
 * contact Pedro M (contact@mobibalises.net).
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with MobiBalisesLib. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.pedro.android.mobibalises_lgpl.alarm;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.pedro.android.json.JSONAble;
import org.pedro.android.json.JSONUtils;
import org.pedro.misc.Sector;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public class BaliseAlarm implements JSONAble, Parcelable
{
  // Balises JSON
  private static final String JSON_ACTIVE                                        = "active";
  private static final String JSON_CHECK_SECTEURS                                = "checkSecteurs";
  private static final String JSON_SECTEURS                                      = "secteurs";
  private static final String JSON_ACTIVATION                                    = "activation";
  private static final String JSON_CHECK_PLAGES_HORAIRES                         = "checkPlagesHoraire";
  private static final String JSON_PLAGES_HORAIRES                               = "plagesHoraires";
  private static final String JSON_ID_BALISE                                     = "idBalise";
  private static final String JSON_NOM_BALISE                                    = "nomBalise";
  private static final String JSON_PROVIDER                                      = "provider";
  private static final String JSON_NOM_PROVIDER                                  = "nomProvider";
  private static final String JSON_ID                                            = "id";
  private static final String JSON_NOTIFICATIONS                                 = "notifications";
  private static final String JSON_CHECK_NOTIFICATION_ANDROID_PERSO              = "checkNotificationAndroidPerso";
  private static final String JSON_TEXTE_VERIFIEE_NOTIFICATION_ANDROID_PERSO     = "texteVerifieeNotificationAndroidPerso";
  private static final String JSON_TEXTE_NON_VERIFIEE_NOTIFICATION_ANDROID_PERSO = "texteNonVerifieeNotificationAndroidPerso";
  private static final String JSON_CHECK_NOTIFICATION_ANDROID_AUDIO              = "checkNotificationAndroidAudio";
  private static final String JSON_URI_NOTIFICATION_ANDROID_AUDIO                = "uriNotificationAndroidAudio";
  private static final String JSON_CHECK_NOTIFICATION_ANDROID_VIBRATION          = "checkNotificationAndroidVibration";
  private static final String JSON_CHECK_NOTIFICATION_VOIX_PERSO                 = "checkNotificationVoixPerso";
  private static final String JSON_TEXTE_VERIFIEE_NOTIFICATION_VOIX_PERSO        = "texteVerifieeNotificationVoixPerso";
  private static final String JSON_TEXTE_NON_VERIFIEE_NOTIFICATION_VOIX_PERSO    = "texteNonVerifieeNotificationVoixPerso";
  private static final String JSON_CHECK_NOTIFICATION_BROADCAST_PERSO            = "checkNotificationBroadcastPerso";
  private static final String JSON_CHECK_VITESSE_MINI                            = "checkVitesseMini";
  private static final String JSON_PLAGES_VITESSE_MINI                           = "plagesVitesseMini";
  private static final String JSON_CHECK_VITESSE_MOY                             = "checkVitesseMoy";
  private static final String JSON_PLAGES_VITESSE_MOY                            = "plagesVitesseMoy";
  private static final String JSON_CHECK_VITESSE_MAXI                            = "checkVitesseMaxi";
  private static final String JSON_PLAGES_VITESSE_MAXI                           = "plagesVitesseMaxi";

  // ID de l'alarme
  private String              id;
  private int                 hashCode;

  // Balise concernee
  public String               provider;
  public String               nomProvider;
  public String               idBalise;
  public String               nomBalise;

  // Etat
  public boolean              active;

  /**
   * 
   * @author pedro.m
   */
  public static class PlageHoraire implements JSONAble, Parcelable
  {
    private static final String JSON_PLAGE_HORAIRE_HEURES_DEBUT  = "heuresDebut";
    private static final String JSON_PLAGE_HORAIRE_MINUTES_DEBUT = "minutesDebut";
    private static final String JSON_PLAGE_HORAIRE_HEURES_FIN    = "heuresFin";
    private static final String JSON_PLAGE_HORAIRE_MINUTES_FIN   = "minutesFin";

    public int                  heuresDebut;
    public int                  minutesDebut;
    public int                  heuresFin;
    public int                  minutesFin;

    /**
     * 
     */
    public PlageHoraire()
    {
      // Nothing
    }

    /**
     * 
     * @param heuresDebut
     * @param minutesDebut
     * @param heuresFin
     * @param minutesFin
     */
    public PlageHoraire(final int heuresDebut, final int minutesDebut, final int heuresFin, final int minutesFin)
    {
      this.heuresDebut = heuresDebut;
      this.minutesDebut = minutesDebut;
      this.heuresFin = heuresFin;
      this.minutesFin = minutesFin;
    }

    @Override
    public JSONObject toJSON() throws JSONException
    {
      // Initialisations
      final JSONObject json = new JSONObject();

      json.put(JSON_PLAGE_HORAIRE_HEURES_DEBUT, heuresDebut);
      json.put(JSON_PLAGE_HORAIRE_MINUTES_DEBUT, minutesDebut);
      json.put(JSON_PLAGE_HORAIRE_HEURES_FIN, heuresFin);
      json.put(JSON_PLAGE_HORAIRE_MINUTES_FIN, minutesFin);

      // Fin
      return json;
    }

    @Override
    public void fromJSON(final JSONObject json) throws JSONException
    {
      heuresDebut = json.getInt(JSON_PLAGE_HORAIRE_HEURES_DEBUT);
      minutesDebut = json.getInt(JSON_PLAGE_HORAIRE_MINUTES_DEBUT);
      heuresFin = json.getInt(JSON_PLAGE_HORAIRE_HEURES_FIN);
      minutesFin = json.getInt(JSON_PLAGE_HORAIRE_MINUTES_FIN);
    }

    @SuppressWarnings("hiding")
    public static final Parcelable.Creator<PlageHoraire> CREATOR = new PlageHoraireCreator();

    /**
     * 
     * @author pedro.m
     */
    private static class PlageHoraireCreator implements Parcelable.Creator<PlageHoraire>
    {
      /**
       * 
       */
      PlageHoraireCreator()
      {
        super();
      }

      @Override
      public PlageHoraire createFromParcel(final Parcel in)
      {
        // Initialisations
        final PlageHoraire plage = new PlageHoraire();

        plage.heuresDebut = in.readInt();
        plage.minutesDebut = in.readInt();
        plage.heuresFin = in.readInt();
        plage.minutesFin = in.readInt();

        return plage;
      }

      @Override
      public PlageHoraire[] newArray(int size)
      {
        return new PlageHoraire[size];
      }
    }

    @Override
    public int describeContents()
    {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags)
    {
      out.writeInt(heuresDebut);
      out.writeInt(minutesDebut);
      out.writeInt(heuresFin);
      out.writeInt(minutesFin);
    }
  }

  public boolean                  checkPlagesHoraires;
  public final List<PlageHoraire> plagesHoraires = new ArrayList<PlageHoraire>();

  // Type activation
  public enum Activation
  {
    DEVIENT_VALIDE, DEVIENT_INVALIDE, CHANGE_ETAT, VALIDE, INVALIDE;
  }

  public Activation activation = Activation.DEVIENT_VALIDE;

  public boolean            checkSecteurs = false;
  public final List<Sector> secteurs      = new ArrayList<Sector>();

  /**
   * 
   * @author pedro.m
   */
  public static class PlageVitesse implements JSONAble, Parcelable
  {
    private static final String JSON_PLAGE_VITESSE_MINI = "vitesseMini";
    private static final String JSON_PLAGE_VITESSE_MAXI = "vitesseMaxi";

    public double               vitesseMini             = 0;
    public double               vitesseMaxi             = 300;

    /**
     * 
     */
    public PlageVitesse()
    {
      // Nothing
    }

    /**
     * 
     * @param vitesseMini
     * @param vitesseMaxi
     */
    public PlageVitesse(final double vitesseMini, final double vitesseMaxi)
    {
      this.vitesseMini = vitesseMini;
      this.vitesseMaxi = vitesseMaxi;
    }

    @Override
    public JSONObject toJSON() throws JSONException
    {
      // Initialisations
      final JSONObject json = new JSONObject();

      json.put(JSON_PLAGE_VITESSE_MINI, vitesseMini);
      json.put(JSON_PLAGE_VITESSE_MAXI, vitesseMaxi);

      // Fin
      return json;
    }

    @Override
    public void fromJSON(final JSONObject json) throws JSONException
    {
      vitesseMini = json.getInt(JSON_PLAGE_VITESSE_MINI);
      vitesseMaxi = json.getInt(JSON_PLAGE_VITESSE_MAXI);
    }

    @SuppressWarnings("hiding")
    public static final Parcelable.Creator<PlageVitesse> CREATOR = new PlageVitesseCreator();

    /**
     * 
     * @author pedro.m
     */
    private static class PlageVitesseCreator implements Parcelable.Creator<PlageVitesse>
    {
      /**
       * 
       */
      PlageVitesseCreator()
      {
        super();
      }

      @Override
      public PlageVitesse createFromParcel(final Parcel in)
      {
        // Initialisations
        final PlageVitesse plage = new PlageVitesse();

        plage.vitesseMini = in.readDouble();
        plage.vitesseMaxi = in.readDouble();

        return plage;
      }

      @Override
      public PlageVitesse[] newArray(int size)
      {
        return new PlageVitesse[size];
      }
    }

    @Override
    public int describeContents()
    {
      return 0;
    }

    @Override
    public void writeToParcel(final Parcel out, final int flags)
    {
      out.writeDouble(vitesseMini);
      out.writeDouble(vitesseMaxi);
    }
  }

  public boolean                  checkVitesseMini;
  public final List<PlageVitesse> plagesVitesseMini = new ArrayList<PlageVitesse>();
  public boolean                  checkVitesseMoy;
  public final List<PlageVitesse> plagesVitesseMoy  = new ArrayList<PlageVitesse>();
  public boolean                  checkVitesseMaxi;
  public final List<PlageVitesse> plagesVitesseMaxi = new ArrayList<PlageVitesse>();

  // Notifications
  public enum Notification
  {
    ANDROID, VOIX, BROADCAST;
  }

  public List<Notification> notifications = new ArrayList<Notification>();

  // Personnalisation des notifications
  public boolean            checkNotificationAndroidPerso;
  public String             texteVerifieeNotificationAndroidPerso;
  public String             texteNonVerifieeNotificationAndroidPerso;
  public boolean            checkNotificationAndroidAudio;
  public Uri                uriNotificationAndroidAudio;
  public boolean            checkNotificationAndroidVibration;
  public boolean            checkNotificationVoixPerso;
  public String             texteVerifieeNotificationVoixPerso;
  public String             texteNonVerifieeNotificationVoixPerso;
  public boolean            checkNotificationBroadcastPerso;

  /**
   * 
   */
  public BaliseAlarm()
  {
    // Nothing
  }

  /**
   * 
   * @param id
   */
  public BaliseAlarm(final String id)
  {
    setId(id);
  }

  /**
   * 
   * @return
   */
  public String getId()
  {
    return id;
  }

  /**
   * 
   * @param inId
   */
  public void setId(final String inId)
  {
    this.id = inId;
    this.hashCode = id.hashCode();
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

    if (!BaliseAlarm.class.isAssignableFrom(object.getClass()))
    {
      return false;
    }

    final BaliseAlarm alarm = (BaliseAlarm)object;

    return id.equals(alarm.id);
  }

  @Override
  public int hashCode()
  {
    return hashCode;
  }

  @Override
  public String toString()
  {
    return id + "/" + provider + "/" + idBalise;
  }

  @Override
  public JSONObject toJSON() throws JSONException
  {
    // Initialisations
    final JSONObject json = new JSONObject();

    // Identification
    json.put(JSON_ID, id);
    json.put(JSON_PROVIDER, provider);
    json.put(JSON_NOM_PROVIDER, nomProvider);
    json.put(JSON_ID_BALISE, idBalise);
    json.put(JSON_NOM_BALISE, nomBalise);

    // Active
    json.put(JSON_ACTIVE, active);

    // Heures d'activite
    json.put(JSON_CHECK_PLAGES_HORAIRES, checkPlagesHoraires);
    JSONUtils.putArray(json, JSON_PLAGES_HORAIRES, plagesHoraires);

    // Activation
    json.put(JSON_ACTIVATION, activation);

    // Secteurs
    json.put(JSON_CHECK_SECTEURS, checkSecteurs);
    JSONUtils.putEnumArray(json, JSON_SECTEURS, secteurs);

    // Notifications
    JSONUtils.putEnumArray(json, JSON_NOTIFICATIONS, notifications);
    json.put(JSON_CHECK_NOTIFICATION_ANDROID_PERSO, checkNotificationAndroidPerso);
    json.put(JSON_TEXTE_VERIFIEE_NOTIFICATION_ANDROID_PERSO, texteVerifieeNotificationAndroidPerso);
    json.put(JSON_TEXTE_NON_VERIFIEE_NOTIFICATION_ANDROID_PERSO, texteNonVerifieeNotificationAndroidPerso);
    json.put(JSON_CHECK_NOTIFICATION_ANDROID_AUDIO, checkNotificationAndroidAudio);
    json.put(JSON_URI_NOTIFICATION_ANDROID_AUDIO, (uriNotificationAndroidAudio == null ? null : uriNotificationAndroidAudio.toString()));
    json.put(JSON_CHECK_NOTIFICATION_ANDROID_VIBRATION, checkNotificationAndroidVibration);
    json.put(JSON_CHECK_NOTIFICATION_VOIX_PERSO, checkNotificationVoixPerso);
    json.put(JSON_TEXTE_VERIFIEE_NOTIFICATION_VOIX_PERSO, texteVerifieeNotificationVoixPerso);
    json.put(JSON_TEXTE_NON_VERIFIEE_NOTIFICATION_VOIX_PERSO, texteNonVerifieeNotificationVoixPerso);
    json.put(JSON_CHECK_NOTIFICATION_BROADCAST_PERSO, checkNotificationBroadcastPerso);

    // Vitesse mini
    json.put(JSON_CHECK_VITESSE_MINI, checkVitesseMini);
    JSONUtils.putArray(json, JSON_PLAGES_VITESSE_MINI, plagesVitesseMini);

    // Vitesse moy
    json.put(JSON_CHECK_VITESSE_MOY, checkVitesseMoy);
    JSONUtils.putArray(json, JSON_PLAGES_VITESSE_MOY, plagesVitesseMoy);

    // Vitesse maxi
    json.put(JSON_CHECK_VITESSE_MAXI, checkVitesseMaxi);
    JSONUtils.putArray(json, JSON_PLAGES_VITESSE_MAXI, plagesVitesseMaxi);

    return json;
  }

  @Override
  public void fromJSON(final JSONObject json) throws JSONException
  {
    try
    {
      // Identification
      setId(json.getString(JSON_ID));
      provider = json.optString(JSON_PROVIDER, null);
      nomProvider = json.optString(JSON_NOM_PROVIDER, null);
      idBalise = json.optString(JSON_ID_BALISE, null);
      nomBalise = json.optString(JSON_NOM_BALISE, null);

      // Active
      active = json.getBoolean(JSON_ACTIVE);

      // Heures d'activite
      checkPlagesHoraires = json.getBoolean(JSON_CHECK_PLAGES_HORAIRES);
      JSONUtils.getArray(json, JSON_PLAGES_HORAIRES, plagesHoraires, PlageHoraire.class);

      // Activation
      activation = Activation.valueOf(json.getString(JSON_ACTIVATION));

      // Secteurs
      checkSecteurs = json.getBoolean(JSON_CHECK_SECTEURS);
      JSONUtils.getEnumArray(json, JSON_SECTEURS, secteurs, Sector.class, Sector.N_NNE);

      // Notifications
      JSONUtils.getEnumArray(json, JSON_NOTIFICATIONS, notifications, Notification.class, Notification.ANDROID);
      checkNotificationAndroidPerso = json.getBoolean(JSON_CHECK_NOTIFICATION_ANDROID_PERSO);
      texteVerifieeNotificationAndroidPerso = json.optString(JSON_TEXTE_VERIFIEE_NOTIFICATION_ANDROID_PERSO, null);
      texteNonVerifieeNotificationAndroidPerso = json.optString(JSON_TEXTE_NON_VERIFIEE_NOTIFICATION_ANDROID_PERSO, null);
      checkNotificationAndroidAudio = json.optBoolean(JSON_CHECK_NOTIFICATION_ANDROID_AUDIO, false);
      final String stringUriNotificationAndroidAudio = json.optString(JSON_URI_NOTIFICATION_ANDROID_AUDIO, null);
      uriNotificationAndroidAudio = (stringUriNotificationAndroidAudio == null ? null : Uri.parse(stringUriNotificationAndroidAudio));
      checkNotificationAndroidVibration = json.optBoolean(JSON_CHECK_NOTIFICATION_ANDROID_VIBRATION, false);
      checkNotificationVoixPerso = json.getBoolean(JSON_CHECK_NOTIFICATION_VOIX_PERSO);
      texteVerifieeNotificationVoixPerso = json.optString(JSON_TEXTE_VERIFIEE_NOTIFICATION_VOIX_PERSO, null);
      texteNonVerifieeNotificationVoixPerso = json.optString(JSON_TEXTE_NON_VERIFIEE_NOTIFICATION_VOIX_PERSO, null);
      checkNotificationBroadcastPerso = json.getBoolean(JSON_CHECK_NOTIFICATION_BROADCAST_PERSO);

      // Vitesse Mini
      checkVitesseMini = json.getBoolean(JSON_CHECK_VITESSE_MINI);
      JSONUtils.getArray(json, JSON_PLAGES_VITESSE_MINI, plagesVitesseMini, PlageVitesse.class);

      // Vitesse Moy
      checkVitesseMoy = json.getBoolean(JSON_CHECK_VITESSE_MOY);
      JSONUtils.getArray(json, JSON_PLAGES_VITESSE_MOY, plagesVitesseMoy, PlageVitesse.class);

      // Vitesse Maxi
      checkVitesseMaxi = json.getBoolean(JSON_CHECK_VITESSE_MAXI);
      JSONUtils.getArray(json, JSON_PLAGES_VITESSE_MAXI, plagesVitesseMaxi, PlageVitesse.class);
    }
    catch (final InstantiationException ie)
    {
      Log.e(getClass().getSimpleName(), ie.getMessage(), ie);
      throw new JSONException(ie.getMessage());
    }
    catch (final IllegalAccessException iae)
    {
      Log.e(getClass().getSimpleName(), iae.getMessage(), iae);
      throw new JSONException(iae.getMessage());
    }
  }

  /**
   * 
   * @param alarm
   * @param copyActive
   */
  public void copyFrom(final BaliseAlarm alarm, final boolean copyActive)
  {
    this.activation = alarm.activation;
    if (copyActive)
    {
      this.active = alarm.active;
    }
    this.checkNotificationAndroidPerso = alarm.checkNotificationAndroidPerso;
    this.checkNotificationAndroidAudio = alarm.checkNotificationAndroidAudio;
    this.uriNotificationAndroidAudio = alarm.uriNotificationAndroidAudio;
    this.checkNotificationAndroidVibration = alarm.checkNotificationAndroidVibration;
    this.checkNotificationBroadcastPerso = alarm.checkNotificationBroadcastPerso;
    this.checkNotificationVoixPerso = alarm.checkNotificationVoixPerso;
    this.checkPlagesHoraires = alarm.checkPlagesHoraires;
    this.checkSecteurs = alarm.checkSecteurs;
    this.checkVitesseMaxi = alarm.checkVitesseMaxi;
    this.checkVitesseMini = alarm.checkVitesseMini;
    this.checkVitesseMoy = alarm.checkVitesseMoy;
    this.hashCode = alarm.hashCode;
    this.id = alarm.id;
    this.idBalise = alarm.idBalise;
    this.nomBalise = alarm.nomBalise;
    this.nomProvider = alarm.nomProvider;
    this.notifications = alarm.notifications;
    this.plagesHoraires.clear();
    this.plagesHoraires.addAll(alarm.plagesHoraires);
    this.plagesVitesseMaxi.clear();
    this.plagesVitesseMaxi.addAll(alarm.plagesVitesseMaxi);
    this.plagesVitesseMini.clear();
    this.plagesVitesseMini.addAll(alarm.plagesVitesseMini);
    this.plagesVitesseMoy.clear();
    this.plagesVitesseMoy.addAll(alarm.plagesVitesseMoy);
    this.provider = alarm.provider;
    this.secteurs.clear();
    this.secteurs.addAll(alarm.secteurs);
    this.texteVerifieeNotificationAndroidPerso = alarm.texteVerifieeNotificationAndroidPerso;
    this.texteNonVerifieeNotificationAndroidPerso = alarm.texteNonVerifieeNotificationAndroidPerso;
    this.texteVerifieeNotificationVoixPerso = alarm.texteVerifieeNotificationVoixPerso;
    this.texteNonVerifieeNotificationVoixPerso = alarm.texteNonVerifieeNotificationVoixPerso;
  }

  /**
   * 
   * @author pedro.m
   */
  private static class BaliseAlarmCreator implements Parcelable.Creator<BaliseAlarm>
  {
    /**
     * 
     */
    BaliseAlarmCreator()
    {
      super();
    }

    @Override
    public BaliseAlarm createFromParcel(final Parcel in)
    {
      // Initialisations
      final BaliseAlarm alarm = new BaliseAlarm();
      final ClassLoader classLoader = getClass().getClassLoader();

      // Identification
      alarm.setId(in.readString());
      alarm.provider = in.readString();
      alarm.nomProvider = in.readString();
      alarm.idBalise = in.readString();
      alarm.nomBalise = in.readString();

      // Active
      alarm.active = (in.readByte() != 0);

      // Heures d'activite
      alarm.checkPlagesHoraires = (in.readByte() != 0);
      in.readList(alarm.plagesHoraires, classLoader);

      // Activation
      alarm.activation = Activation.valueOf(in.readString());

      // Secteurs
      alarm.checkSecteurs = (in.readByte() != 0);
      in.readList(alarm.secteurs, classLoader);

      // Notifications
      in.readList(alarm.notifications, classLoader);
      alarm.checkNotificationAndroidPerso = (in.readByte() != 0);
      alarm.texteVerifieeNotificationAndroidPerso = in.readString();
      alarm.texteNonVerifieeNotificationAndroidPerso = in.readString();
      alarm.checkNotificationAndroidAudio = (in.readByte() != 0);
      final String stringUriNotificationAndroidAudio = in.readString();
      alarm.uriNotificationAndroidAudio = (stringUriNotificationAndroidAudio == null ? null : Uri.parse(stringUriNotificationAndroidAudio));
      alarm.checkNotificationAndroidVibration = (in.readByte() != 0);
      alarm.checkNotificationVoixPerso = (in.readByte() != 0);
      alarm.texteVerifieeNotificationVoixPerso = in.readString();
      alarm.texteNonVerifieeNotificationVoixPerso = in.readString();
      alarm.checkNotificationBroadcastPerso = (in.readByte() != 0);

      // Vitesse Mini
      alarm.checkVitesseMini = (in.readByte() != 0);
      in.readList(alarm.plagesVitesseMini, classLoader);

      // Vitesse Moy
      alarm.checkVitesseMoy = (in.readByte() != 0);
      in.readList(alarm.plagesVitesseMoy, classLoader);

      // Vitesse Maxi
      alarm.checkVitesseMaxi = (in.readByte() != 0);
      in.readList(alarm.plagesVitesseMaxi, classLoader);

      return alarm;
    }

    @Override
    public BaliseAlarm[] newArray(int size)
    {
      return new BaliseAlarm[size];
    }
  }

  public static final Parcelable.Creator<BaliseAlarm> CREATOR = new BaliseAlarmCreator();

  @Override
  public int describeContents()
  {
    return 0;
  }

  @Override
  public void writeToParcel(final Parcel out, final int flags)
  {
    // Identification
    out.writeString(getId());
    out.writeString(provider);
    out.writeString(nomProvider);
    out.writeString(idBalise);
    out.writeString(nomBalise);

    // Active
    out.writeByte((byte)(active ? 1 : 0));

    // Heures d'activite
    out.writeByte((byte)(checkPlagesHoraires ? 1 : 0));
    out.writeList(plagesHoraires);

    // Activation
    out.writeString(activation.toString());

    // Secteurs
    out.writeByte((byte)(checkSecteurs ? 1 : 0));
    out.writeList(secteurs);

    // Notifications
    out.writeList(notifications);
    out.writeByte((byte)(checkNotificationAndroidPerso ? 1 : 0));
    out.writeString(texteVerifieeNotificationAndroidPerso);
    out.writeString(texteNonVerifieeNotificationAndroidPerso);
    out.writeByte((byte)(checkNotificationAndroidAudio ? 1 : 0));
    out.writeString(uriNotificationAndroidAudio == null ? null : uriNotificationAndroidAudio.toString());
    out.writeByte((byte)(checkNotificationAndroidVibration ? 1 : 0));
    out.writeByte((byte)(checkNotificationVoixPerso ? 1 : 0));
    out.writeString(texteVerifieeNotificationVoixPerso);
    out.writeString(texteNonVerifieeNotificationVoixPerso);
    out.writeByte((byte)(checkNotificationBroadcastPerso ? 1 : 0));

    // Vitesse Mini
    out.writeByte((byte)(checkVitesseMini ? 1 : 0));
    out.writeList(plagesVitesseMini);

    // Vitesse Moy
    out.writeByte((byte)(checkVitesseMoy ? 1 : 0));
    out.writeList(plagesVitesseMoy);

    // Vitesse Maxi
    out.writeByte((byte)(checkVitesseMaxi ? 1 : 0));
    out.writeList(plagesVitesseMaxi);
  }
}
