package org.pedro.spots.dhv;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pedro.spots.Orientation;
import org.pedro.spots.Pratique;
import org.pedro.spots.Spot;
import org.pedro.spots.TypeSpot;
import org.pedro.spots.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * 
 * @author pedro.m
 */
public final class DhvContentHandler implements ContentHandler
{
  private static final String       STRING_VIDE                    = "";

  private static final String       SITE_TAG                       = "FlyingSite";
  private static final String       SITE_ID_TAG                    = "SiteID";
  private static final String       LOCATION_TAG                   = "Location";
  private static final String       LOCATION_ID_TAG                = "LocationID";
  private static final String       LOCATION_NAME_TAG              = "LocationName";
  private static final String       COORDINATES_TAG                = "Coordinates";
  private static final Pattern      COORDINATES_PATTERN            = Pattern.compile("(\\d+\\.\\d+),(\\d+\\.\\d+)");
  private static final String       LOCATION_TYPE_TAG              = "LocationType";
  private static final String       LOCATION_TYPE_DECO             = "1";
  private static final String       LOCATION_TYPE_ATTERRO          = "2";
  private static final String       LOCATION_TYPE_TREUIL           = "3";
  private static final String       ALTITUDE_TAG                   = "Altitude";
  private static final String       DIRECTIONS_TEXT_TAG            = "DirectionsText";
  private static final String       HANGGLIDING_TAG                = "Hanggliding";
  private static final String       PARAGLIDING_TAG                = "Paragliding";

  // Infos site
  private static final String       SITE_NAME_TAG                  = "SiteName";
  private static final String       SITE_TYPE_TAG                  = "SiteType";
  private static final String       SITE_TYPE_EN_TAG               = "SiteType_en";
  private static final String       DENIVELE_MAX_TAG               = "HeightDifferenceMax";
  private static final String       DE_CERTIFIED_TAG               = "DECertified";
  private static final String       DE_CERTIFICATION_HOLDER_TAG    = "DECertificationHolder";
  private static final String       SITE_INFORMATION_TAG           = "SiteInformation";
  private static final String       SUITABILITY_HG_TAG             = "SuitabilityHG";
  private static final String       SUITABILITY_HG_EN_TAG          = "SuitabilityHG_en";
  private static final String       SUITABILITY_PG_TAG             = "SuitabilityPG";
  private static final String       SUITABILITY_PG_EN_TAG          = "SuitabilityPG_en";
  private String                    nom;
  private String                    desc;
  private String                    enDesc;
  private Integer                   maxDeniv;
  private Boolean                   cert;
  private String                    certHolder;
  private String                    siteInformations;
  private String                    suitHg;
  private String                    suitHgEn;
  private String                    suitPg;
  private String                    suitPgEn;

  // Infos acces
  private static final String       CABLE_CAR_TAG                  = "CableCar";
  private static final String       POST_CODE_TAG                  = "PostCode";
  private static final String       REGION_TAG                     = "Region";
  private static final String       MUNICIPALITY_TAG               = "Municipality";
  private static final String       ACCESS_BY_CAR_TAG              = "AccessByCar";
  private static final String       ACCESS_BY_PUBLIC_TRANSPORT_TAG = "AccessByPublicTransport";
  private static final String       ACCESS_BY_FOOT_TAG             = "AccessByFoot";
  private static final String       ACCESS_REMARKS_TAG             = "AccessRemarks";
  private String                    remontee;
  private String                    codePostal;
  private String                    region;
  private String                    municipalite;
  private Boolean                   accVoiture;
  private Boolean                   accTC;
  private Boolean                   accMP;
  private String                    accRem;

  // Infos contact
  private static final String       SITE_CONTACT_TAG               = "SiteContact";
  private static final String       FURTHER_CONTACT_TAG            = "FurtherContact";
  private String                    contact;
  private String                    contact2;

  // Infos reglementation
  private static final String       REQUIREMENTS_TAG               = "Requirements";
  private static final String       GUEST_RULES_APPLY_TAG          = "GuestRulesApply";
  private static final String       GUEST_RULES_TAG                = "GuestRules";
  private String                    exigences;
  private Boolean                   reglesInvitesActives;
  private String                    reglesInvites;

  // Infos meteo
  private static final String       WEATHER_INFO_TAG               = "WeatherInfo";
  private static final String       WEATHER_PHONE_TAG              = "WeatherPhone";
  private String                    infosMeteo;
  private String                    telMeteo;

  // Infos remorquage
  private static final String       TOWING_LENGTH_TAG              = "TowingLength";
  private static final String       TOWING_HEIGHT1_TAG             = "TowingHeight1";
  private static final String       TOWING_HEIGHT2_TAG             = "TowingHeight2";
  private Integer                   lgTreuil;
  private Integer                   htTreuil1;
  private Integer                   htTreuil2;

  // Infos remarques
  private static final String       SITE_REMARKS_TAG               = "SiteRemarks";
  private static final String       LOCATION_REMARKS_TAG           = "LocationRemarks";
  private String                    siteComments;
  private String                    locComments;

  // Infos hebergement
  private static final String       ACCOMODATION_TAG               = "Accomodation";
  private String                    heberg;

  // Infos webcam
  private static final String       WEBCAM1_TAG                    = "WebCam1";
  private static final String       WEBCAM2_TAG                    = "WebCam2";
  private static final String       WEBCAM3_TAG                    = "WebCam3";
  private String                    webcam1;
  private String                    webcam2;
  private String                    webcam3;

  private static final String       ORIENTATION_N                  = "N";
  private static final String       ORIENTATION_NE                 = "NO";
  private static final String       ORIENTATION_E                  = "O";
  private static final String       ORIENTATION_SE                 = "SO";
  private static final String       ORIENTATION_S                  = "S";
  private static final String       ORIENTATION_SO                 = "SW";
  private static final String       ORIENTATION_O                  = "W";
  private static final String       ORIENTATION_NO                 = "NW";
  private static final List<String> ORIENTATIONS                   = new ArrayList<String>();

  private String                    currentString                  = STRING_VIDE;

  // Membres
  private final List<Spot>          spots                          = new ArrayList<Spot>();
  private DhvSpot                   spot;
  private String                    siteId;

  // Infos

  /**
   * 
   */
  static
  {
    ORIENTATIONS.add(ORIENTATION_N);
    ORIENTATIONS.add(ORIENTATION_NE);
    ORIENTATIONS.add(ORIENTATION_E);
    ORIENTATIONS.add(ORIENTATION_SE);
    ORIENTATIONS.add(ORIENTATION_S);
    ORIENTATIONS.add(ORIENTATION_SO);
    ORIENTATIONS.add(ORIENTATION_O);
    ORIENTATIONS.add(ORIENTATION_NO);
  }

  /**
   * Recuperation de la liste des spots
   * 
   * @return
   */
  public List<Spot> getSpots()
  {
    return spots;
  }

  @Override
  public void endElement(final String uri, final String localName, final String qName) throws SAXException
  {
    String finalName = qName;
    if ((finalName == null) || (finalName.length() == 0))
    {
      finalName = localName;
    }

    if (LOCATION_TAG.equals(finalName))
    {
      computeInfos();
      spot = null;
    }
    else if (SITE_TAG.equals(finalName))
    {
      resetInfos();
    }
    else if (SITE_ID_TAG.equals(finalName))
    {
      siteId = currentString;
    }
    else if (LOCATION_ID_TAG.equals(finalName))
    {
      spot.id = currentString;
    }
    else if (LOCATION_NAME_TAG.equals(finalName))
    {
      spot.nom = currentString;
    }
    else if (COORDINATES_TAG.equals(finalName))
    {
      final Matcher matcher = COORDINATES_PATTERN.matcher(currentString);
      if (matcher.matches())
      {
        try
        {
          spot.longitude = Utils.parseDouble(matcher.group(1));
          spot.latitude = Utils.parseDouble(matcher.group(2));
        }
        catch (final NumberFormatException nfe)
        {
          //Nothing
        }
      }
    }
    else if (LOCATION_TYPE_TAG.equals(finalName))
    {
      if (LOCATION_TYPE_DECO.equals(currentString))
      {
        spot.type = TypeSpot.DECOLLAGE;
      }
      else if (LOCATION_TYPE_ATTERRO.equals(currentString))
      {
        spot.type = TypeSpot.ATTERRISSAGE;
      }
      else if (LOCATION_TYPE_TREUIL.equals(currentString))
      {
        spot.type = TypeSpot.DECOLLAGE;
      }
    }
    else if (ALTITUDE_TAG.equals(finalName))
    {
      try
      {
        spot.altitude = Utils.parseInteger(currentString);
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (DIRECTIONS_TEXT_TAG.equals(finalName))
    {
      if (!Utils.isStringVide(currentString))
      {
        final String[] directions = currentString.split(",");
        for (final String direction : directions)
        {
          analysePlageDirection(direction.trim());
        }
      }
    }
    else if (PARAGLIDING_TAG.equals(finalName))
    {
      if (Boolean.parseBoolean(currentString))
      {
        addPratique(Pratique.PARAPENTE);
      }
    }
    else if (HANGGLIDING_TAG.equals(finalName))
    {
      if (Boolean.parseBoolean(currentString))
      {
        addPratique(Pratique.DELTA);
      }
    }

    // *********************
    // INFOS SITE
    // *********************
    else if (SITE_NAME_TAG.equals(finalName))
    {
      nom = currentString;
    }
    else if (SITE_TYPE_TAG.equals(finalName))
    {
      desc = currentString;
    }
    else if (SITE_TYPE_EN_TAG.equals(finalName))
    {
      enDesc = currentString;
    }
    else if (DENIVELE_MAX_TAG.equals(finalName))
    {
      try
      {
        maxDeniv = Utils.parseInteger(currentString);
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (DE_CERTIFIED_TAG.equals(finalName))
    {
      cert = Boolean.valueOf(currentString);
    }
    else if (DE_CERTIFICATION_HOLDER_TAG.equals(finalName))
    {
      certHolder = currentString;
    }
    else if (SITE_INFORMATION_TAG.equals(finalName))
    {
      siteInformations = currentString;
    }
    else if (SUITABILITY_HG_TAG.equals(finalName))
    {
      suitHg = currentString;
    }
    else if (SUITABILITY_HG_EN_TAG.equals(finalName))
    {
      suitHgEn = currentString;
    }
    else if (SUITABILITY_PG_TAG.equals(finalName))
    {
      suitPg = currentString;
    }
    else if (SUITABILITY_PG_EN_TAG.equals(finalName))
    {
      suitPgEn = currentString;
    }

    // *********************
    // INFOS ACCES
    // *********************
    else if (CABLE_CAR_TAG.equals(finalName))
    {
      remontee = currentString;
    }
    else if (POST_CODE_TAG.equals(finalName))
    {
      codePostal = currentString;
    }
    else if (REGION_TAG.equals(finalName))
    {
      region = currentString;
    }
    else if (MUNICIPALITY_TAG.equals(finalName))
    {
      municipalite = currentString;
    }
    else if (ACCESS_BY_CAR_TAG.equals(finalName))
    {
      accVoiture = Boolean.valueOf(currentString);
    }
    else if (ACCESS_BY_PUBLIC_TRANSPORT_TAG.equals(finalName))
    {
      accTC = Boolean.valueOf(currentString);
    }
    else if (ACCESS_BY_FOOT_TAG.equals(finalName))
    {
      accMP = Boolean.valueOf(currentString);
    }
    else if (ACCESS_REMARKS_TAG.equals(finalName))
    {
      accRem = currentString;
    }

    // *********************
    // INFOS CONTACT
    // *********************
    else if (SITE_CONTACT_TAG.equals(finalName))
    {
      contact = currentString;
    }
    else if (FURTHER_CONTACT_TAG.equals(finalName))
    {
      contact2 = currentString;
    }

    // *********************
    // INFOS REGLEMENTATION
    // *********************
    else if (REQUIREMENTS_TAG.equals(finalName))
    {
      exigences = currentString;
    }
    else if (GUEST_RULES_APPLY_TAG.equals(finalName))
    {
      reglesInvitesActives = Boolean.valueOf(currentString);
    }
    else if (GUEST_RULES_TAG.equals(finalName))
    {
      reglesInvites = currentString;
    }

    // *********************
    // INFOS METEO
    // *********************
    else if (WEATHER_INFO_TAG.equals(finalName))
    {
      infosMeteo = currentString;
    }
    else if (WEATHER_PHONE_TAG.equals(finalName))
    {
      telMeteo = currentString;
    }

    // *********************
    // INFOS TREUILLAGE
    // *********************
    else if (TOWING_LENGTH_TAG.equals(finalName))
    {
      try
      {
        lgTreuil = Utils.parseInteger(currentString);
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (TOWING_HEIGHT1_TAG.equals(finalName))
    {
      try
      {
        htTreuil1 = Utils.parseInteger(currentString);
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }
    else if (TOWING_HEIGHT2_TAG.equals(finalName))
    {
      try
      {
        htTreuil2 = Utils.parseInteger(currentString);
      }
      catch (final NumberFormatException nfe)
      {
        //Nothing
      }
    }

    // *********************
    // INFOS COMMENTAIRES
    // *********************
    else if (SITE_REMARKS_TAG.equals(finalName))
    {
      siteComments = currentString;
    }
    else if (LOCATION_REMARKS_TAG.equals(finalName))
    {
      locComments = currentString;
    }

    // *********************
    // INFOS HEBERGEMENTS
    // *********************
    else if (ACCOMODATION_TAG.equals(finalName))
    {
      heberg = currentString;
    }

    // *********************
    // INFOS WEBCAMS
    // *********************
    else if (WEBCAM1_TAG.equals(finalName))
    {
      webcam1 = currentString;
    }
    else if (WEBCAM2_TAG.equals(finalName))
    {
      webcam2 = currentString;
    }
    else if (WEBCAM3_TAG.equals(finalName))
    {
      webcam3 = currentString;
    }

    // RAZ
    currentString = STRING_VIDE;
  }

  /**
   * 
   */
  private void computeInfos()
  {
    computeInfosSite();
    computeInfosAcces();
    computeInfosContact();
    computeInfosReglementation();
    computeInfosMeteo();
    computeInfosTreuillage();
    computeInfosCommentaire();
    computeInfosHebergement();
    computeInfosWebcam();
  }

  /**
   * 
   */
  private void resetInfos()
  {
    resetInfosSite();
    resetInfosAcces();
    resetInfosContact();
    resetInfosReglementation();
    resetInfosMeteo();
    resetInfosTreuillage();
    resetInfosCommentaire();
    resetInfosHebergement();
    resetInfosWebcam();
  }

  /**
   * 
   */
  private void computeInfosSite()
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    String separator = "";

    // Nom
    if (!Utils.isStringVide(nom))
    {
      buffer.append(separator);
      buffer.append("{nom} : ");
      buffer.append(nom);
      separator = "\n";
    }

    // Desc
    if (!Utils.isStringVide(desc))
    {
      buffer.append(separator);
      buffer.append("{desc} : ");
      buffer.append(desc);
      separator = "\n";
    }

    // Desc EN
    if (!Utils.isStringVide(enDesc))
    {
      buffer.append(separator);
      buffer.append("{enDesc} : ");
      buffer.append(enDesc);
      separator = "\n";
    }

    // Denivele max
    if ((maxDeniv != null) && (maxDeniv.intValue() > 0))
    {
      buffer.append(separator);
      buffer.append("{maxDeniv} : ");
      buffer.append(maxDeniv.intValue());
      buffer.append("m");
      separator = "\n";
    }

    // Cert
    if (cert != null)
    {
      buffer.append(separator);
      buffer.append("{cert} : {");
      buffer.append(cert);
      buffer.append("}");
      separator = "\n";
    }

    // Cert Holder
    if (!Utils.isStringVide(certHolder))
    {
      buffer.append(separator);
      buffer.append("{certHolder} : ");
      buffer.append(certHolder);
      separator = "\n";
    }

    // Site information
    if (!Utils.isStringVide(siteInformations))
    {
      buffer.append(separator);
      buffer.append("{siteInformations} : ");
      buffer.append(siteInformations);
      separator = "\n";
    }

    // Info Delta
    if (!Utils.isStringVide(suitHg))
    {
      buffer.append(separator);
      buffer.append("{suitHg} : ");
      buffer.append(suitHg);
      separator = "\n";
    }

    // Info Delta EN
    if (!Utils.isStringVide(suitHgEn))
    {
      buffer.append(separator);
      buffer.append("{suitHgEn} : ");
      buffer.append(suitHgEn);
      separator = "\n";
    }

    // Info Parapente
    if (!Utils.isStringVide(suitPg))
    {
      buffer.append(separator);
      buffer.append("{suitPg} : ");
      buffer.append(suitPg);
      separator = "\n";
    }

    // Info Parapente EN
    if (!Utils.isStringVide(suitPgEn))
    {
      buffer.append(separator);
      buffer.append("{suitPgEn} : ");
      buffer.append(suitPgEn);
      separator = "\n";
    }

    // Fin
    final String infos = buffer.toString().trim();
    if (!Utils.isStringVide(infos))
    {
      spot.infos.put(DhvSpotProvider.Info.SITE.getKey(), infos);
    }
  }

  /**
   * 
   */
  private void computeInfosAcces()
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    String separator = "";

    // Code postal
    if (!Utils.isStringVide(codePostal))
    {
      buffer.append(separator);
      buffer.append("{codePostal} : ");
      buffer.append(codePostal);
      separator = "\n";
    }

    // Municipalite
    if (!Utils.isStringVide(municipalite))
    {
      buffer.append(separator);
      buffer.append("{municipalite} : ");
      buffer.append(municipalite);
      separator = "\n";
    }

    // Region
    if (!Utils.isStringVide(region))
    {
      buffer.append(separator);
      buffer.append("{region} : ");
      buffer.append(region);
      separator = "\n";
    }

    // Remontee
    if (!Utils.isStringVide(remontee))
    {
      buffer.append(separator);
      buffer.append("{remontee} : ");
      buffer.append(remontee);
      separator = "\n";
    }

    // Acces voiture
    if (accVoiture != null)
    {
      buffer.append(separator);
      buffer.append("{accVoiture} : {");
      buffer.append(accVoiture);
      buffer.append("}");
      separator = "\n";
    }

    // Acces TC
    if (accTC != null)
    {
      buffer.append(separator);
      buffer.append("{accTC} : {");
      buffer.append(accTC);
      buffer.append("}");
      separator = "\n";
    }

    // Acces MP
    if (accMP != null)
    {
      buffer.append(separator);
      buffer.append("{accMP} : {");
      buffer.append(accMP);
      buffer.append("}");
      separator = "\n";
    }

    // Remarques acces
    if (!Utils.isStringVide(accRem))
    {
      buffer.append(separator);
      buffer.append("{accRem} : ");
      buffer.append(accRem);
      separator = "\n";
    }

    // Fin
    final String infos = buffer.toString().trim();
    if (!Utils.isStringVide(infos))
    {
      spot.infos.put(DhvSpotProvider.Info.ACCES.getKey(), infos);
    }
  }

  /**
   * 
   */
  private void computeInfosContact()
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    String separator = "";

    // Contact
    if (!Utils.isStringVide(contact))
    {
      buffer.append(separator);
      buffer.append(contact);
      separator = "\n\n";
    }

    // Contact 2
    if (!Utils.isStringVide(contact2))
    {
      buffer.append(separator);
      buffer.append(contact2);
    }

    // Fin
    final String infos = buffer.toString().trim();
    if (!Utils.isStringVide(infos))
    {
      spot.infos.put(DhvSpotProvider.Info.CONTACT.getKey(), infos);
    }
  }

  /**
   * 
   */
  private void computeInfosReglementation()
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    String separator = "";

    // Exigences
    if (!Utils.isStringVide(exigences))
    {
      buffer.append(separator);
      buffer.append(exigences);
      separator = "\n";
    }

    // Regles invites actives
    if (reglesInvitesActives != null)
    {
      buffer.append(separator);
      buffer.append("{reglesInvitesActives} : {");
      buffer.append(reglesInvitesActives);
      buffer.append("}");
      separator = "\n";
    }

    // Regles invites
    if (!Utils.isStringVide(reglesInvites))
    {
      buffer.append(separator);
      buffer.append("{reglesInvites} : ");
      buffer.append(reglesInvites);
      separator = "\n";
    }

    // Fin
    final String infos = buffer.toString().trim();
    if (!Utils.isStringVide(infos))
    {
      spot.infos.put(DhvSpotProvider.Info.REGLEMENTATION.getKey(), infos);
    }
  }

  /**
   * 
   */
  private void computeInfosMeteo()
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    String separator = "";

    // Infos Meteo
    if (!Utils.isStringVide(infosMeteo))
    {
      buffer.append(separator);
      buffer.append(infosMeteo);
      separator = "\n";
    }

    // Tel Meteo
    if (!Utils.isStringVide(telMeteo))
    {
      buffer.append(separator);
      buffer.append(telMeteo);
      separator = "\n";
    }

    // Fin
    final String infos = buffer.toString().trim();
    if (!Utils.isStringVide(infos))
    {
      spot.infos.put(DhvSpotProvider.Info.METEO.getKey(), infos);
    }
  }

  /**
   * 
   */
  private void computeInfosTreuillage()
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    String separator = "";

    // Longueur treuil
    if ((lgTreuil != null) && (lgTreuil.intValue() > 0))
    {
      buffer.append(separator);
      buffer.append("{lgTreuil} : ");
      buffer.append(lgTreuil.intValue());
      buffer.append("m");
      separator = "\n";
    }

    // Hauteur 1 treuil
    if ((htTreuil1 != null) && (htTreuil1.intValue() > 0))
    {
      buffer.append(separator);
      buffer.append("{htTreuil1} : ");
      buffer.append(htTreuil1.intValue());
      buffer.append("m");
      separator = "\n";
    }

    // Hauteur 2 treuil
    if ((htTreuil2 != null) && (htTreuil2.intValue() > 0))
    {
      buffer.append(separator);
      buffer.append("{htTreuil2} : ");
      buffer.append(htTreuil2.intValue());
      buffer.append("m");
      separator = "\n";
    }

    // Fin
    final String infos = buffer.toString().trim();
    if (!Utils.isStringVide(infos))
    {
      spot.infos.put(DhvSpotProvider.Info.TREUILLAGE.getKey(), infos);
    }
  }

  /**
   * 
   */
  private void computeInfosCommentaire()
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    String separator = "";

    // Site
    if (!Utils.isStringVide(siteComments))
    {
      buffer.append(separator);
      buffer.append(siteComments);
      separator = "\n\n";
    }

    // Location
    if (!Utils.isStringVide(locComments))
    {
      buffer.append(separator);
      buffer.append(locComments);
      separator = "\n";
    }

    // Fin
    final String infos = buffer.toString().trim();
    if (!Utils.isStringVide(infos))
    {
      spot.infos.put(DhvSpotProvider.Info.REMARQUE.getKey(), infos);
    }
  }

  /**
   * 
   */
  private void computeInfosHebergement()
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    String separator = "";

    // Code postal
    if (!Utils.isStringVide(heberg))
    {
      buffer.append(separator);
      buffer.append(heberg);
      separator = "\n";
    }

    // Fin
    final String infos = buffer.toString().trim();
    if (!Utils.isStringVide(infos))
    {
      spot.infos.put(DhvSpotProvider.Info.HEBERGEMENT.getKey(), infos);
    }
  }

  /**
   * 
   */
  private void computeInfosWebcam()
  {
    // Initialisations
    final StringBuilder buffer = new StringBuilder();
    String separator = "";

    // Webcam1
    if (!Utils.isStringVide(webcam1))
    {
      buffer.append(separator);
      buffer.append(webcam1);
      separator = "\n";
    }

    // Webcam2
    if (!Utils.isStringVide(webcam2))
    {
      buffer.append(separator);
      buffer.append(webcam2);
      separator = "\n";
    }

    // Webcam3
    if (!Utils.isStringVide(webcam3))
    {
      buffer.append(separator);
      buffer.append(webcam3);
      separator = "\n";
    }

    // Fin
    final String infos = buffer.toString().trim();
    if (!Utils.isStringVide(infos))
    {
      spot.infos.put(DhvSpotProvider.Info.WEBCAM.getKey(), infos);
    }
  }

  /**
   * 
   */
  private void resetInfosSite()
  {
    nom = null;
    desc = null;
    enDesc = null;
    maxDeniv = null;
    cert = null;
    certHolder = null;
    siteInformations = null;
  }

  /**
   * 
   */
  private void resetInfosAcces()
  {
    remontee = null;
    codePostal = null;
    region = null;
    municipalite = null;
    accVoiture = null;
    accTC = null;
    accMP = null;
    accRem = null;
  }

  /**
   * 
   */
  private void resetInfosContact()
  {
    contact = null;
    contact2 = null;
  }

  /**
   * 
   */
  private void resetInfosReglementation()
  {
    exigences = null;
    reglesInvitesActives = null;
    reglesInvites = null;
  }

  /**
   * 
   */
  private void resetInfosMeteo()
  {
    infosMeteo = null;
    telMeteo = null;
  }

  /**
   * 
   */
  private void resetInfosTreuillage()
  {
    lgTreuil = null;
    htTreuil1 = null;
    htTreuil2 = null;
  }

  /**
   * 
   */
  private void resetInfosCommentaire()
  {
    siteComments = null;
    locComments = null;
  }

  /**
   * 
   */
  private void resetInfosHebergement()
  {
    heberg = null;
  }

  /**
   * 
   */
  private void resetInfosWebcam()
  {
    webcam1 = null;
    webcam2 = null;
    webcam3 = null;
  }

  /**
   * 
   * @param pratique
   */
  private void addPratique(final Pratique pratique)
  {
    if (spot.pratiques == null)
    {
      spot.pratiques = new ArrayList<Pratique>();
    }

    spot.pratiques.add(pratique);
  }

  /**
   * 
   * @param plageDirection
   */
  private void analysePlageDirection(final String plageDirection)
  {
    final String[] directions = plageDirection.split("-");
    if (directions.length == 1)
    {
      analyseDirection(directions[0]);
    }
    else if (directions.length == 2)
    {
      final int debut = ORIENTATIONS.indexOf(directions[0]);
      final int fin = ORIENTATIONS.indexOf(directions[1]);
      if ((debut >= 0) && (fin >= 0))
      {
        if (debut != fin)
        {
          final int modulo = ORIENTATIONS.size();
          final int finalFin = (debut < fin ? fin : fin + modulo);
          for (int i = debut; i <= finalFin; i++)
          {
            analyseDirection(ORIENTATIONS.get(i % modulo));
          }
        }
        else
        {
          analyseDirection(ORIENTATIONS.get(debut));
        }
      }
    }
  }

  /**
   * 
   * @param direction
   */
  private void analyseDirection(final String direction)
  {
    if (ORIENTATION_N.equalsIgnoreCase(direction))
    {
      spot.orientations.add(Orientation.N);
    }
    else if (ORIENTATION_NE.equalsIgnoreCase(direction))
    {
      spot.orientations.add(Orientation.NE);
    }
    else if (ORIENTATION_E.equalsIgnoreCase(direction))
    {
      spot.orientations.add(Orientation.E);
    }
    else if (ORIENTATION_SE.equalsIgnoreCase(direction))
    {
      spot.orientations.add(Orientation.SE);
    }
    else if (ORIENTATION_S.equalsIgnoreCase(direction))
    {
      spot.orientations.add(Orientation.S);
    }
    else if (ORIENTATION_SO.equalsIgnoreCase(direction))
    {
      spot.orientations.add(Orientation.SO);
    }
    else if (ORIENTATION_O.equalsIgnoreCase(direction))
    {
      spot.orientations.add(Orientation.O);
    }
    else if (ORIENTATION_NO.equalsIgnoreCase(direction))
    {
      spot.orientations.add(Orientation.NO);
    }
  }

  @Override
  public void startDocument() throws SAXException
  {
    spots.clear();
    currentString = STRING_VIDE;
  }

  @Override
  public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException
  {
    String finalName = qName;
    if ((finalName == null) || (finalName.length() == 0))
    {
      finalName = localName;
    }

    if (LOCATION_TAG.equals(finalName))
    {
      spot = new DhvSpot();
      spot.idSite = siteId;
      spots.add(spot);
    }

    // RAZ
    currentString = STRING_VIDE;
  }

  @Override
  public void characters(final char[] ch, final int start, final int length) throws SAXException
  {
    currentString += new String(ch, start, length);
    currentString = currentString.replace("\\'", "'");
  }

  @Override
  public void startPrefixMapping(final String arg0, final String arg1) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public void endDocument() throws SAXException
  {
    // Nothing to do
  }

  @Override
  public void endPrefixMapping(final String arg0) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public void ignorableWhitespace(final char[] ch, final int start, final int length) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public void processingInstruction(final String arg0, final String arg1) throws SAXException
  {
    // Nothing to do
  }

  @Override
  public void setDocumentLocator(final Locator arg0)
  {
    // Nothing to do
  }

  @Override
  public void skippedEntity(final String arg0) throws SAXException
  {
    // Nothing to do
  }
}
