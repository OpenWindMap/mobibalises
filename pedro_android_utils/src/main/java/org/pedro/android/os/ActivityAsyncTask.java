package org.pedro.android.os;

import android.app.Activity;
import android.os.AsyncTask;

/**
 * 
 * @author pedro.m
 *
 * @param <Params>
 * @param <Progress>
 * @param <Result>
 */
public abstract class ActivityAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result>
{
  protected Activity activity;

  /**
   * 
   * @param activity
   */
  public ActivityAsyncTask(final Activity activity)
  {
    super();
    this.activity = activity;
  }

  @Override
  public void onCancelled()
  {
    // Super
    super.onCancelled();

    // Liberation
    activity = null;
  }

  @Override
  public void onPostExecute(final Result result)
  {
    // Super
    super.onPostExecute(result);

    // Liberation
    activity = null;
  }
}
