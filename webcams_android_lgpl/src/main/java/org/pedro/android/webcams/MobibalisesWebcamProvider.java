package org.pedro.android.webcams;

import java.io.IOException;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;
import org.pedro.webcams.AbstractMobibalisesWebcamProvider;
import org.pedro.webcams.Webcam;

/**
 * 
 * @author pedro.m
 */
public class MobibalisesWebcamProvider extends AbstractMobibalisesWebcamProvider<JSONAbleWebcam>
{
  /**
   * 
   * @param apiKey
   */
  public MobibalisesWebcamProvider(final String apiKey)
  {
    super(apiKey);
  }

  @Override
  public Webcam newWebcam()
  {
    return new JSONAbleWebcam();
  }

  @Override
  public Collection<JSONAbleWebcam> parseMobibalisesResponse(final StringBuilder buffer) throws IOException
  {
    try
    {
      final JSONObject json = new JSONObject(buffer.toString());
      final MobibalisesWebcamGetResponse response = new MobibalisesWebcamGetResponse();
      response.fromJSON(json);

      if (response.code == 0)
      {
        return response.data;
      }

      throw new IOException("Error code " + response.code + " : " + response.message);
    }
    catch (final JSONException jse)
    {
      final IOException ioe = new IOException(jse.getMessage());
      ioe.setStackTrace(jse.getStackTrace());
      throw ioe;
    }
  }
}
