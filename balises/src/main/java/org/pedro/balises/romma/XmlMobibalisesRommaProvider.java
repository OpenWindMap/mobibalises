package org.pedro.balises.romma;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.pedro.balises.HistoryBaliseProvider;
import org.pedro.balises.MobibalisesHistoryBaliseProviderHelper;
import org.pedro.balises.Releve;
import org.xml.sax.InputSource;

/**
 * 
 * @author pedro.m
 */
public final class XmlMobibalisesRommaProvider extends XmlRommaProvider implements HistoryBaliseProvider
{
  private static final String URL_RELEVES_MOBIBALISES  = "http://data.mobibalises.net/{" + URL_ROMMA_KEY + "}/releves_romma.xml{" + URL_GZIP_KEY + "}";

  private static final String URL_BALISE_ID_KEY        = "baliseId";
  private static final String URL_BALISE_ID_KEY_GROUP  = "\\{" + URL_BALISE_ID_KEY + "\\}";
  private static final String URL_DUREE_KEY            = "duree";
  private static final String URL_DUREE_KEY_GROUP      = "\\{" + URL_DUREE_KEY + "\\}";
  private static final String URL_PEREMPTION_KEY       = "peremption";
  private static final String URL_PEREMPTION_KEY_GROUP = "\\{" + URL_PEREMPTION_KEY + "\\}";

  //private static final String URL_HISTO                   = "file:D:/download/romma-histo-{" + URL_BALISE_ID_KEY + "}.gz";
  private static final String URL_HISTO                = "http://data.mobibalises.net/{" + URL_ROMMA_KEY + "}/romma-histo.php?baliseId={" + URL_BALISE_ID_KEY + "}&duree={" + URL_DUREE_KEY + "}&peremption={" + URL_PEREMPTION_KEY
                                                           + "}&temperature=1";

  /**
   * 
   * @param name
   * @param country
   * @param rommaKey
   * @param useZippedData
   */
  public XmlMobibalisesRommaProvider(final String name, final String country, final String rommaKey, final boolean useZippedData)
  {
    // Initialisation
    super(name, country, rommaKey, useZippedData, new ReleveRommaMobibalisesContentHandler());
  }

  @Override
  public boolean updateReleves() throws IOException
  {
    // Initialisations
    InputStream input = null;

    try
    {
      updatedReleves.clear();
      final String finalUrl = URL_RELEVES_MOBIBALISES.replaceAll(URL_ROMMA_KEY_GROUP, rommaKey);
      input = getUnzippedInputStream(finalUrl, true);
      parseReleves(new InputSource(input));
    }
    finally
    {
      if (input != null)
      {
        input.close();
      }
    }

    return !updatedReleves.isEmpty();
  }

  @Override
  public Collection<Releve> getHistoriqueBalise(final String baliseId, final int duree, final int peremption) throws IOException
  {
    // Elaboration URL
    final String finalUrl = URL_HISTO.replaceAll(URL_ROMMA_KEY_GROUP, rommaKey).replaceAll(URL_BALISE_ID_KEY_GROUP, baliseId).replaceAll(URL_DUREE_KEY_GROUP, "" + duree).replaceAll(URL_PEREMPTION_KEY_GROUP, "" + peremption);

    // Lecture donnees
    final List<Releve> releves = new ArrayList<Releve>();
    BufferedInputStream bis = null;
    GZIPInputStream gzis = null;
    InputStreamReader isr = null;
    BufferedReader br = null;
    try
    {
      // Initialisations
      bis = new BufferedInputStream(new URL(finalUrl).openStream());
      gzis = new GZIPInputStream(bis);
      isr = new InputStreamReader(gzis);
      br = new BufferedReader(isr);

      // Lecture
      String line = br.readLine();
      while ((line != null) && (line.trim().length() > 0))
      {
        // Analyse
        final Releve releve = MobibalisesHistoryBaliseProviderHelper.parseLigneReleve(line, baliseId);
        if (releve != null)
        {
          releves.add(releve);
        }

        // Next
        line = br.readLine();
      }
    }
    finally
    {
      if (br != null)
      {
        br.close();
      }
      if (isr != null)
      {
        isr.close();
      }
      if (gzis != null)
      {
        gzis.close();
      }
      if (bis != null)
      {
        bis.close();
      }
    }

    // Fin
    return releves;
  }
}
