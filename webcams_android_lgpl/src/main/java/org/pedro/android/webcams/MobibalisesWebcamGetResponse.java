package org.pedro.android.webcams;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.pedro.android.json.JSONAble;
import org.pedro.android.json.JSONUtils;

import android.util.Log;

/**
 * 
 * @author pedro.m
 *
 */
public class MobibalisesWebcamGetResponse implements JSONAble
{
  // Balises JSON
  private static final String JSON_CODE    = "code";
  private static final String JSON_MESSAGE = "msg";
  private static final String JSON_DATA    = "data";

  public int                  code;
  public String               message;
  public List<JSONAbleWebcam> data         = new ArrayList<JSONAbleWebcam>();
  public String               csvWebcams;

  @Override
  public void fromJSON(final JSONObject json) throws JSONException
  {
    try
    {
      code = json.getInt(JSON_CODE);
      message = json.getString(JSON_MESSAGE);
      if (code == 0)
      {
        JSONUtils.getArray(json, JSON_DATA, data, JSONAbleWebcam.class);
      }
      else
      {
        throw new JSONException("Error code " + code + " : " + message);
      }
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

  @Override
  public Object toJSON() throws JSONException
  {
    // Initialisations
    final JSONObject json = new JSONObject();

    json.put(JSON_CODE, code);
    json.put(JSON_MESSAGE, message);
    JSONUtils.putArray(json, JSON_DATA, data);

    return json;
  }
}
