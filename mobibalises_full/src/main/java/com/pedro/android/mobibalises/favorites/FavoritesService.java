package com.pedro.android.mobibalises.favorites;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.service.IProvidersService;
import org.pedro.balises.Balise;
import org.pedro.balises.BaliseProvider;
import org.pedro.balises.Releve;
import org.pedro.balises.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public class FavoritesService
{
  private static final String                     PREF_LABELS_SIZE        = "prefs.labels.size";
  private static final String                     PREF_LABELS_ITEM        = "prefs.labels.item.{0}";

  private static final String                     PREF_LABEL_BALISES_SIZE = "prefs.balises.{0}.size";
  private static final String                     PREF_LABEL_BALISES_ITEM = "prefs.balises.{0}.item.{1}";

  private final Resources                         resources;
  private IProvidersService                       providersService;
  private final SharedPreferences                 sharedPreferences;

  private final List<String>                      labels                  = new ArrayList<String>();
  private final Map<String, List<BaliseFavorite>> favorites               = new HashMap<String, List<BaliseFavorite>>();

  private final List<LabelListener>               labelListeners          = new ArrayList<LabelListener>();

  /**
   * 
   * @author pedro.m
   */
  public interface LabelListener
  {
    /**
     * 
     * @param from
     * @param to
     */
    public void onLabelRenamed(final String from, final String to);

    /**
     * 
     * @param label
     */
    public void onLabelRemoved(final String label);
  }

  /**
   * 
   * @param inContext
   * @param inResources
   * @param inProvidersService
   */
  public FavoritesService(final Context inContext, final Resources inResources, final IProvidersService inProvidersService)
  {
    this.resources = inResources;
    this.providersService = inProvidersService;
    this.sharedPreferences = ActivityCommons.getSharedPreferences(inContext);

    // Chargement
    loadLabels();
    loadBalisesFavorites();
  }

  /**
   * 
   */
  private void loadLabels()
  {
    // Initialisations
    labels.clear();

    // Chargement
    final int nbLabels = sharedPreferences.getInt(PREF_LABELS_SIZE, 0);
    for (int i = 0; i < nbLabels; i++)
    {
      final String label = sharedPreferences.getString(MessageFormat.format(PREF_LABELS_ITEM, Integer.valueOf(i)), null);
      if (!Utils.isStringVide(label))
      {
        labels.add(label);
      }
    }

    // Verification si besoin du libelle par defaut
    ensureDefaultLabel();

    // Fin
    Log.d(getClass().getSimpleName(), "Labels loaded : " + labels);
  }

  /**
   * 
   */
  private void ensureDefaultLabel()
  {
    // Si aucun label, ajout du label par defaut
    if (labels.size() <= 0)
    {
      labels.add(resources.getString(R.string.label_default_label));
    }
  }

  /**
   * 
   */
  public void saveLabels()
  {
    // Initialisations
    final int nbExistingLabels = sharedPreferences.getInt(PREF_LABELS_SIZE, 0);
    final SharedPreferences.Editor editor = sharedPreferences.edit();

    // Verification si besoin du libelle par defaut
    ensureDefaultLabel();

    // Sauvegarde des labels existants
    editor.putInt(PREF_LABELS_SIZE, labels.size());
    for (int i = 0; i < labels.size(); i++)
    {
      editor.putString(MessageFormat.format(PREF_LABELS_ITEM, Integer.valueOf(i)), labels.get(i));
    }

    // Effacement des labels en trop
    for (int i = labels.size(); i < nbExistingLabels; i++)
    {
      editor.remove(MessageFormat.format(PREF_LABELS_ITEM, Integer.valueOf(i)));
    }

    // Fin
    ActivityCommons.commitPreferences(editor);
    Log.d(getClass().getSimpleName(), "Labels saved : " + labels);
  }

  /**
   * 
   * @param from
   * @param to
   */
  protected void renameLabel(final String from, final String to)
  {
    // Le label n'existe pas
    if (!labels.contains(from))
    {
      return;
    }

    // Renommage du label
    final int index = labels.indexOf(from);
    labels.set(index, to);

    // MAJ des favoris correspondants
    final List<BaliseFavorite> liste = favorites.remove(from);
    if (liste != null)
    {
      favorites.put(to, liste);
    }

    // Propagation evenement
    fireLabelRenamed(from, to);
  }

  /**
   * 
   * @param label
   */
  public void addLabel(final String label)
  {
    if (!labels.contains(label))
    {
      labels.add(label);
    }
  }

  /**
   * 
   * @param label
   */
  protected void removeLabel(final String label)
  {
    // Suppression
    if (!labels.remove(label))
    {
      return;
    }

    // Suppression des balises associees
    favorites.remove(label);

    // Verification si besoin du libelle par defaut
    ensureDefaultLabel();

    // Propagation evenement
    fireLabelRemoved(label);
  }

  /**
   * 
   */
  private void loadBalisesFavorites()
  {
    // Effacement
    favorites.clear();

    // Pour chaque label
    for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++)
    {
      // Nombre de favoris
      final Integer integerLabelIndex = Integer.valueOf(labelIndex);
      final int nbFavorites = sharedPreferences.getInt(MessageFormat.format(PREF_LABEL_BALISES_SIZE, integerLabelIndex), 0);

      // Lecture des favoris
      for (int favoriteIndex = 0; favoriteIndex < nbFavorites; favoriteIndex++)
      {
        // Chaine favori
        final Integer integerFavoriteIndex = Integer.valueOf(favoriteIndex);
        final String chaine = sharedPreferences.getString(MessageFormat.format(PREF_LABEL_BALISES_ITEM, integerLabelIndex, integerFavoriteIndex), null);

        // Analyse
        final BaliseFavorite favorite = BaliseFavorite.parseBalise(chaine, this);
        if (favorite != null)
        {
          addBaliseFavorite(favorite, labels.get(labelIndex));
        }
      }
    }

    // Fin
    Log.d(getClass().getSimpleName(), "Favorites loaded : " + favorites);
  }

  /**
   * 
   */
  public void saveBalisesFavorites()
  {
    // Debut de transaction
    final SharedPreferences.Editor editor = sharedPreferences.edit();

    // Pour chaque label
    for (int labelIndex = 0; labelIndex < labels.size(); labelIndex++)
    {
      final Integer integerLabelIndex = Integer.valueOf(labelIndex);
      final int nbExistingFavorites = sharedPreferences.getInt(MessageFormat.format(PREF_LABEL_BALISES_SIZE, integerLabelIndex), 0);

      // Les favoris du label
      final List<BaliseFavorite> liste = favorites.get(labels.get(labelIndex));
      if (liste != null)
      {
        // Index final (dans les preferences)
        int finalFavoriteIndex = 0;

        // Pour chaque favori
        for (int favoriteIndex = 0; favoriteIndex < liste.size(); favoriteIndex++)
        {
          final BaliseFavorite favorite = liste.get(favoriteIndex);
          if (favorite != null)
          {
            final Integer integerFavoriteIndex = Integer.valueOf(finalFavoriteIndex);
            editor.putString(MessageFormat.format(PREF_LABEL_BALISES_ITEM, integerLabelIndex, integerFavoriteIndex), favorite.toString());
            finalFavoriteIndex++;
          }
        }

        // Sauvegarde nombre de favoris
        editor.putInt(MessageFormat.format(PREF_LABEL_BALISES_SIZE, integerLabelIndex), finalFavoriteIndex);

        // Suppression des eventuels favoris precedents
        for (int favoriteIndex = finalFavoriteIndex; favoriteIndex < nbExistingFavorites; favoriteIndex++)
        {
          final Integer integerFavoriteIndex = Integer.valueOf(favoriteIndex);
          editor.remove(MessageFormat.format(PREF_LABEL_BALISES_ITEM, integerLabelIndex, integerFavoriteIndex));
        }
      }
      else
      {
        editor.putInt(MessageFormat.format(PREF_LABEL_BALISES_SIZE, integerLabelIndex), 0);
      }
    }

    // Suppression des eventuels favoris pour les labels precedents
    for (int labelIndex = labels.size();; labelIndex++)
    {
      final Integer integerLabelIndex = Integer.valueOf(labelIndex);

      // Y avait-il des favoris a cet index ?
      if (!sharedPreferences.contains(MessageFormat.format(PREF_LABEL_BALISES_SIZE, integerLabelIndex)))
      {
        // Non
        break;
      }

      // Oui => suppression
      final int nbExistingFavorites = sharedPreferences.getInt(MessageFormat.format(PREF_LABEL_BALISES_SIZE, integerLabelIndex), 0);

      // Suppression des eventuels favoris precedents
      for (int favoriteIndex = 0; favoriteIndex < nbExistingFavorites; favoriteIndex++)
      {
        final Integer integerFavoriteIndex = Integer.valueOf(favoriteIndex);
        editor.remove(MessageFormat.format(PREF_LABEL_BALISES_ITEM, integerLabelIndex, integerFavoriteIndex));
      }
    }

    // Fin
    ActivityCommons.commitPreferences(editor);
    Log.d(getClass().getSimpleName(), "BalisesFavorites saved : " + favorites);
  }

  /**
   * 
   * @param providerKey
   * @param baliseId
   * @return
   */
  public Balise getBalise(final String providerKey, final String baliseId)
  {
    if (providersService == null)
    {
      return null;
    }

    final BaliseProvider provider = providersService.getBaliseProvider(providerKey);

    return (provider == null ? null : provider.getBaliseById(baliseId));
  }

  /**
   * 
   * @param providerKey
   * @param baliseId
   * @return
   */
  public Releve getReleve(final String providerKey, final String baliseId)
  {
    if (providersService == null)
    {
      return null;
    }

    final BaliseProvider provider = providersService.getBaliseProvider(providerKey);

    return (provider == null ? null : provider.getReleveById(baliseId));
  }

  /**
   * 
   * @return
   */
  public List<String> getLabels()
  {
    return labels;
  }

  /**
   * 
   * @param providerId
   * @param baliseId
   * @return
   */
  public boolean isBaliseFavorite(final String providerId, final String baliseId)
  {
    // Initialisations
    final BaliseFavorite favorite = new BaliseFavorite(providerId, baliseId, this);

    // Pour chaque label
    for (final String label : labels)
    {
      final List<BaliseFavorite> liste = favorites.get(label);
      if ((liste != null) && liste.contains(favorite))
      {
        return true;
      }
    }

    return false;
  }

  /**
   * 
   * @param providerId
   * @param baliseId
   * @param label
   * @return
   */
  public boolean isBaliseFavoriteForLabel(final String providerId, final String baliseId, final String label)
  {
    // Initialisations
    final BaliseFavorite favorite = new BaliseFavorite(providerId, baliseId, this);

    // Pour chaque label
    final List<BaliseFavorite> liste = favorites.get(label);

    if ((liste != null) && liste.contains(favorite))
    {
      return true;
    }

    return false;
  }

  /**
   * 
   * @param providerId
   * @param baliseId
   * @return
   */
  public List<String> getBaliseFavoriteLabels(final String providerId, final String baliseId)
  {
    // Initialisations
    final BaliseFavorite favorite = new BaliseFavorite(providerId, baliseId, this);
    final List<String> favoriteLabels = new ArrayList<String>();

    // Pour chaque label
    for (final String label : labels)
    {
      final List<BaliseFavorite> liste = favorites.get(label);
      if ((liste != null) && liste.contains(favorite))
      {
        favoriteLabels.add(label);
      }
    }

    return favoriteLabels;
  }

  /**
   * 
   * @param providerId
   * @param baliseId
   * @param choosedLabels
   */
  public void addBaliseFavorite(final String providerId, final String baliseId, final List<String> choosedLabels)
  {
    // Initialisations
    final BaliseFavorite favorite = new BaliseFavorite(providerId, baliseId, this);

    // Ajout
    for (final String label : choosedLabels)
    {
      addBaliseFavorite(favorite, label);
    }
  }

  /**
   * 
   * @param favorite
   * @param label
   * @return
   */
  public boolean addBaliseFavorite(final BaliseFavorite favorite, final String label)
  {
    // Verification existence label
    if (!labels.contains(label))
    {
      return false;
    }

    // Recuperation de la liste pour le label
    List<BaliseFavorite> liste = favorites.get(label);

    // Creation si necessaire
    if (liste == null)
    {
      liste = new ArrayList<BaliseFavorite>();
      favorites.put(label, liste);
    }

    // Verification existence
    if (liste.contains(favorite))
    {
      return true;
    }

    // Ajout
    return liste.add(favorite);
  }

  /**
   * 
   * @param providerId
   * @param baliseId
   * @param choosedLabels
   */
  public void updateBaliseFavorite(final String providerId, final String baliseId, final List<String> choosedLabels)
  {
    // Initialisations
    final BaliseFavorite favorite = new BaliseFavorite(providerId, baliseId, this);

    // Pour chaque label
    for (final String label : labels)
    {
      final List<BaliseFavorite> liste = favorites.get(label);

      // Ajout si necessaire
      if ((choosedLabels != null) && choosedLabels.contains(label))
      {
        if ((liste == null) || !liste.contains(favorite))
        {
          addBaliseFavorite(favorite, label);
        }
      }
      // Retrait si necessaire
      else if ((liste != null) && liste.contains(favorite))
      {
        liste.remove(favorite);
      }
    }
  }

  /**
   * 
   * @param label
   * @return
   */
  public List<BaliseFavorite> getBalisesForLabel(final String label)
  {
    return favorites.get(label);
  }

  /**
   * 
   */
  public void shutdown()
  {
    // Nettoyage
    cleanUp();
  }

  /**
   * 
   */
  private void cleanUp()
  {
    labels.clear();
    favorites.clear();
    labelListeners.clear();
  }

  /**
   * 
   * @param label
   * @param from
   * @param to
   * @param usedProviders
   */
  public void moveBaliseFavorite(final String label, final int inFrom, final int inTo, final List<String> usedProviders)
  {
    // Initialisation
    final List<BaliseFavorite> liste = favorites.get(label);

    // Verification
    if ((liste == null) || (liste.size() == 0))
    {
      return;
    }

    // Calcul des positions finales (en tenant compte des balises dans la liste dont le provider est desactive (non utilise)
    // ou des balises non affichees car inactives (si option "cacher les balises inactives))
    final int from = calculPosition(liste, inFrom, usedProviders);
    final int to = calculPosition(liste, inTo, usedProviders);

    // Mouvement
    final BaliseFavorite favorite = liste.remove(from);
    if (to >= liste.size())
    {
      liste.add(favorite);
    }
    else
    {
      liste.add(to, favorite);
    }

    // Fin
    Log.d(getClass().getSimpleName(), "BaliseFavorite moved from " + from + " to " + to + " : " + liste);
  }

  /**
   * 
   * @param liste
   * @param count
   * @param usedProviders
   * @return
   */
  private int calculPosition(final List<BaliseFavorite> liste, final int count, final List<String> usedProviders)
  {
    // Initialisations
    int index = 0;
    int found = 0;
    final boolean displayInactive = !sharedPreferences.getBoolean(resources.getString(R.string.config_map_balise_hide_inactive_key), Boolean.parseBoolean(resources.getString(R.string.config_map_balise_hide_inactive_default)));

    // Pour chaque favori
    for (final BaliseFavorite favorite : liste)
    {
      if (usedProviders.contains(favorite.getProviderId()) && (displayInactive || favorite.isDrawable()))
      {
        found++;
      }

      if (found > count)
      {
        break;
      }

      index++;
    }

    return index;
  }

  /**
   * 
   * @param label
   * @param index
   */
  public void removeBaliseFavorite(final String label, final int index)
  {
    // Initialisation
    final List<BaliseFavorite> liste = favorites.get(label);

    // Verification
    if ((liste == null) || (liste.size() == 0))
    {
      return;
    }

    // Mouvement
    liste.remove(index);

    // Fin
    Log.d(getClass().getSimpleName(), "BaliseFavorite removed : " + index + " : " + liste);
  }

  /**
   * 
   * @param favorite
   * @param label
   */
  public void removeBaliseFavorite(final BaliseFavorite favorite, final String label)
  {
    // Initialisation
    final List<BaliseFavorite> liste = favorites.get(label);

    // Verification
    if ((liste == null) || (liste.size() == 0))
    {
      return;
    }

    // Mouvement
    liste.remove(favorite);

    // Fin
    Log.d(getClass().getSimpleName(), "BaliseFavorite removed : " + favorite + " : " + liste);
  }

  /**
   * 
   * @param listener
   * @return
   */
  public boolean addLabelListener(final LabelListener listener)
  {
    if (!labelListeners.contains(listener))
    {
      return labelListeners.add(listener);
    }

    return false;
  }

  /**
   * 
   * @param listener
   * @return
   */
  public boolean removeLabelListener(final LabelListener listener)
  {
    if (labelListeners.contains(listener))
    {
      return labelListeners.remove(listener);
    }

    return false;
  }

  /**
   * 
   * @param from
   * @param to
   */
  private void fireLabelRenamed(final String from, final String to)
  {
    for (final LabelListener listener : labelListeners)
    {
      listener.onLabelRenamed(from, to);
    }
  }

  /**
   * 
   * @param label
   */
  private void fireLabelRemoved(final String label)
  {
    for (final LabelListener listener : labelListeners)
    {
      listener.onLabelRemoved(label);
    }
  }
}
