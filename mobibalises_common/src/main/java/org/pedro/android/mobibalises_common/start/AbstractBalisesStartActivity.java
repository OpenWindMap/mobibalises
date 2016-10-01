package org.pedro.android.mobibalises_common.start;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.R;
import org.pedro.android.mobibalises_common.map.AbstractBalisesMapActivity;
import org.pedro.android.mobibalises_common.preferences.AbstractBalisesPreferencesActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

/**
 * 
 * @author pedro.m
 */
public abstract class AbstractBalisesStartActivity extends Activity
{
  public static final String  EXTRA_SPLASH_SCREEN      = "EXTRA_SPLASH_SCREEN";

  private static final String FFVL_PROVIDER_KEY        = "ffvl_FR";
  private static final String FFVL_URL_BALISE_PATTERN  = "^.*/balise.php\\?idBalise=(\\d+)";
  private static final String FFVL_URL_GENERAL         = "http://www.balisemeteo.com";

  private static final String ROMMA_PROVIDER_KEY       = "romma_FR";
  private static final String ROMMA_URL_BALISE_PATTERN = "^.*/station_24.php\\?id=(\\d+)";
  private static final String ROMMA_URL_GENERAL        = "http://www.romma.fr";

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    // Initialisations
    Log.d(getClass().getSimpleName(), ">>> onCreate(...)");
    super.onCreate(savedInstanceState);
    final Resources resources = getResources();

    // Intent
    final SharedPreferences sharedPreferences = ActivityCommons.getSharedPreferences(getApplicationContext());
    final String intentDefaultAction = sharedPreferences.getString(AbstractBalisesPreferencesActivity.CONFIG_START_ACTIVITY, resources.getString(R.string.intent_map_action));
    final Intent intent = new Intent(intentDefaultAction);
    intent.putExtra(EXTRA_SPLASH_SCREEN, true);

    // Analyse de l'intent de demarrage
    final Intent startIntent = getIntent();
    Log.d(getClass().getSimpleName(), "startIntent : " + startIntent);
    if ((startIntent != null) && Intent.ACTION_VIEW.equals(startIntent.getAction()))
    {
      // Capture d'une url FFVL ou ROMMA, on force le demarrage du mode carte
      intent.setAction(resources.getString(R.string.intent_map_action));

      // Initialisations
      final String dataString = startIntent.getDataString();

      // FFVL
      if (dataString.toLowerCase().startsWith(FFVL_URL_GENERAL))
      {
        Log.d(getClass().getSimpleName(), "FFVL");

        // Detail balise
        final Pattern balisePattern = Pattern.compile(FFVL_URL_BALISE_PATTERN);
        final Matcher baliseMatcher = balisePattern.matcher(dataString);
        if (baliseMatcher.matches())
        {
          Log.d(getClass().getSimpleName(), "Balise FFVL : " + baliseMatcher.group(1));
          intent.putExtra(AbstractBalisesMapActivity.EXTRA_BALISE_PROVIDER_ID, FFVL_PROVIDER_KEY);
          intent.putExtra(AbstractBalisesMapActivity.EXTRA_BALISE_ID, baliseMatcher.group(1));
        }
      }
      // ROMMA
      else if (dataString.toLowerCase().startsWith(ROMMA_URL_GENERAL))
      {
        Log.d(getClass().getSimpleName(), "ROMMA");

        // Detail balise
        final Pattern balisePattern = Pattern.compile(ROMMA_URL_BALISE_PATTERN);
        final Matcher baliseMatcher = balisePattern.matcher(dataString);
        if (baliseMatcher.matches())
        {
          Log.d(getClass().getSimpleName(), "Balise ROMMA : " + baliseMatcher.group(1));
          intent.putExtra(AbstractBalisesMapActivity.EXTRA_BALISE_PROVIDER_ID, ROMMA_PROVIDER_KEY);
          intent.putExtra(AbstractBalisesMapActivity.EXTRA_BALISE_ID, baliseMatcher.group(1));
        }
      }
    }

    // Lancement de l'activite qui va bien
    startActivity(intent);

    // Fin de l'activite de demarrage
    finish();

    // Fin
    Log.d(getClass().getSimpleName(), "<<< onCreate(...)");
  }

  @Override
  protected void onDestroy()
  {
    Log.d(getClass().getSimpleName(), ">>> onDestroy(...)");
    super.onDestroy();

    // Liberation vues
    ActivityCommons.unbindDrawables(findViewById(android.R.id.content));

    Log.d(getClass().getSimpleName(), "<<< onDestroy(...)");
  }
}
