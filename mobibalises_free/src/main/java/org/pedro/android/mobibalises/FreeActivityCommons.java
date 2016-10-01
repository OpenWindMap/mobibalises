package org.pedro.android.mobibalises;

import org.pedro.android.mobibalises_common.ActivityCommons;
import org.pedro.android.mobibalises_common.Strings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * 
 * @author pedro.m
 */
public abstract class FreeActivityCommons
{
  // Initialisation
  private static final Object initLock    = new Object();
  private static boolean      initialized = false;

  private static Handler      fullVersionDialogHandler;

  /**
   * 
   * @param context
   */
  @SuppressWarnings("unused")
  public static void init(final Context context)
  {
    // Une seule fois
    synchronized (initLock)
    {
      if (initialized)
      {
        return;
      }

      // Handler dialogue version complete
      initFullVersionDialogHandler();

      // Fin
      initialized = true;
    }
  }

  /**
   * 
   */
  private static void initFullVersionDialogHandler()
  {
    fullVersionDialogHandler = new Handler(Looper.getMainLooper())
    {
      @Override
      public void handleMessage(final Message msg)
      {
        final Activity activity = (Activity)((Object[])msg.obj)[0];
        final Resources resources = activity.getResources();
        final String msgTitle = (String)((Object[])msg.obj)[1];
        final String msgMessage = (String)((Object[])msg.obj)[2];
        final Integer imageId = (Integer)((Object[])msg.obj)[3];
        final Integer soundId = (Integer)((Object[])msg.obj)[4];
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(msgTitle);
        builder.setIcon(R.drawable.icon);
        builder.setCancelable(true);
        final View view = LayoutInflater.from(activity.getApplicationContext()).inflate(R.layout.full_version_dialog, null);

        // TextView
        final TextView textView = (TextView)view.findViewById(R.id.full_version_dialog_text);
        textView.setAutoLinkMask(Linkify.ALL);
        textView.setText(Html.fromHtml(Strings.toHtmlString(msgMessage)));

        // ImageView
        final ImageView imageView = (ImageView)view.findViewById(R.id.full_version_dialog_image);
        if (imageId == null)
        {
          imageView.setVisibility(View.GONE);
        }
        else
        {
          imageView.setVisibility(View.VISIBLE);
          imageView.setImageResource(imageId.intValue());
        }

        // Vue
        builder.setView(view);

        // Echantillon sonore
        final MediaPlayer mediaPlayer;
        if ((soundId != null) && (soundId.intValue() >= 0))
        {
          // Config du media player
          mediaPlayer = MediaPlayer.create(activity.getApplicationContext(), soundId.intValue());

          // Bouton
          builder.setNeutralButton(resources.getString(R.string.full_version_sound_sample_button), new DialogInterface.OnClickListener()
          {
            @Override
            public void onClick(final DialogInterface dialog, final int which)
            {
              // Rien, sert juste a ne pas passer null (car dans ce cas ca ne fonctionne pas sur certaines versions (en 1.6 L8 par exemple))
            }
          });
        }
        else
        {
          mediaPlayer = null;
        }

        // Bouton Market
        builder.setPositiveButton(resources.getString(R.string.full_version_market_button), new DialogInterface.OnClickListener()
        {
          @Override
          public void onClick(final DialogInterface dialog, final int id)
          {
            // Fin de l'echantillon sonore
            try
            {
              if (mediaPlayer != null)
              {
                if (mediaPlayer.isPlaying())
                {
                  mediaPlayer.stop();
                }
                mediaPlayer.release();
              }
            }
            catch (final IllegalStateException ise)
            {
              // Nothing
            }

            // Play store
            ActivityCommons.goToMarket(activity);

            // Fermeture dialogue
            dialog.dismiss();
          }
        });

        // Annulation
        builder.setOnCancelListener(new OnCancelListener()
        {
          @Override
          public void onCancel(final DialogInterface dialog)
          {
            // Fin de l'echantillon sonore
            try
            {
              if (mediaPlayer != null)
              {
                if (mediaPlayer.isPlaying())
                {
                  mediaPlayer.stop();
                }
                mediaPlayer.release();
              }
            }
            catch (final IllegalStateException ise)
            {
              // Nothing
            }

            // Fermeture dialogue
            dialog.dismiss();
          }
        });

        // Creation du dialogue
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        ActivityCommons.registerAlertDialog(ActivityCommons.ALERT_DIALOG_FULL_VERSION_PROMOTE, alertDialog, null);

        // Bouton echantillon sonore
        if ((soundId != null) && (soundId.intValue() >= 0) && (mediaPlayer != null))
        {
          final Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
          neutralButton.setOnClickListener(new View.OnClickListener()
          {
            @Override
            public void onClick(final View inView)
            {
              mediaPlayer.start();
            }
          });
        }
      }
    };
  }

  /**
   * 
   * @param activity
   * @param messageId
   * @param imageId
   * @param soundId
   */
  public static void promoteFullVersion(final Activity activity, final int messageId, final int imageId, final int soundId)
  {
    // Initialisations
    final Resources resources = activity.getResources();

    // Boite de dialogue
    fullVersionDialog(activity, resources.getString(R.string.app_name), resources.getString(messageId), imageId, soundId);
  }

  /**
   * 
   * @param activity
   * @param title
   * @param message
   * @param imageId
   * @param soundId
   */
  private static void fullVersionDialog(final Activity activity, final String title, final String message, final int imageId, final int soundId)
  {
    final Message msg = new Message();
    msg.obj = new Object[] { activity, title, message, (imageId >= 0 ? Integer.valueOf(imageId) : null), (soundId >= 0 ? Integer.valueOf(soundId) : null) };
    fullVersionDialogHandler.sendMessage(msg);
  }
}
