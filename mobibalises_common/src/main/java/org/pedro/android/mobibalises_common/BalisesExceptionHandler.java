package org.pedro.android.mobibalises_common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;

/**
 * 
 * @author pedro.m
 */
public final class BalisesExceptionHandler implements UncaughtExceptionHandler
{
  public static final String             MOBIBALISES_LOG   = "mobibalises.log";

  private static final String            PARAM_FORCE_CLOSE = "forceClose";
  private static final String            PARAM_VERSION     = "version";
  private static final String            PARAM_PACKAGE     = "package";
  private static final String            PARAM_OS          = "os";
  private static final String            PARAM_STACKTRACE  = "stacktrace";
  private static final String            PARAM_THREAD      = "thread";

  private static final String            LOG_FORCE_CLOSE   = "Force close : ";
  private static final String            LOG_THREAD        = "Thread : ";
  private static final String            LOG_VERSION       = "Version : ";
  private static final String            LOG_ANDROID       = "Android : ";

  private static final String            DEBUG_VERSION     = " (debug)";
  private static final String            UNKNOWN_VERSION   = "unknown";

  private static final String            URL               = "http://data.mobibalises.net/error.php";

  private final UncaughtExceptionHandler defaultHandler;
  private boolean                        sendReport        = true;
  private final String                   version;
  private final String                   packageName;

  /**
   * 
   * @author pedro.m
   */
  private static class SendReportAsyncTask extends AsyncTask<Void, Void, Void>
  {
    final Thread    thread;
    final Throwable th;
    final boolean   forceClose;
    final String    inVersion;
    final String    inPackageName;

    /**
     * 
     * @param thread
     * @param th
     * @param forceClose
     * @param inVersion
     * @param inPackageName
     */
    SendReportAsyncTask(final Thread thread, final Throwable th, final boolean forceClose, final String inVersion, final String inPackageName)
    {
      this.thread = thread;
      this.th = th;
      this.forceClose = forceClose;
      this.inVersion = inVersion;
      this.inPackageName = inPackageName;
    }

    @Override
    protected Void doInBackground(final Void... args)
    {
      sendToServer(thread, th, forceClose, inVersion, inPackageName);
      return null;
    }
  }

  /**
   * 
   * @param context
   * @param defaultHandler
   */
  public BalisesExceptionHandler(final Context context, final UncaughtExceptionHandler defaultHandler)
  {
    this.defaultHandler = defaultHandler;
    this.version = getVersion(context);
    this.packageName = context.getPackageName();
    try
    {
      ActivityCommons.MOBIBALISES_EXTERNAL_STORAGE_PATH.mkdirs();
    }
    catch (final RuntimeException re)
    {
      re.printStackTrace(System.err);
    }
  }

  @Override
  public void uncaughtException(final Thread thread, final Throwable th)
  {
    // Trace
    traceException(thread, th, true);

    // Appel du gestionnaire par defaut
    if (defaultHandler != null)
    {
      defaultHandler.uncaughtException(thread, th);
    }
  }

  /**
   * 
   * @param thread
   * @param th
   * @param forceClose
   */
  public void traceException(final Thread thread, final Throwable th, final boolean forceClose)
  {
    // Trace
    log(thread, th, forceClose);
    if (sendReport)
    {
      final SendReportAsyncTask task = new SendReportAsyncTask(thread, th, forceClose, version, packageName);
      task.execute((Void)null);
    }
  }

  /**
   * 
   * @return
   */
  private static String getVersion(final Context context)
  {
    final Resources resources = (context == null ? null : context.getResources());
    String version = (resources == null ? UNKNOWN_VERSION : resources.getString(R.string.app_version));
    final boolean debugMode = (context == null ? false : (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0);
    if (debugMode)
    {
      version += DEBUG_VERSION;
    }

    return version;
  }

  /**
   * 
   * @param thread
   * @param th
   * @param forceClose
   */
  private synchronized void log(final Thread thread, final Throwable th, final boolean forceClose)
  {
    FileWriter fw = null;

    try
    {
      // Initialisations
      fw = new FileWriter(new File(ActivityCommons.MOBIBALISES_EXTERNAL_STORAGE_PATH, MOBIBALISES_LOG), false);

      // Date heure
      System.err.println(new Date());
      fw.write(new Date() + Strings.NEWLINE);

      // Android
      System.err.println(LOG_ANDROID + Build.VERSION.RELEASE);
      fw.write(LOG_ANDROID + Build.VERSION.RELEASE + Strings.NEWLINE);

      // Version
      System.err.println(LOG_VERSION + version);
      fw.write(LOG_VERSION + version + Strings.NEWLINE);

      // Thread
      System.err.println(LOG_THREAD + thread.toString());
      fw.write(LOG_THREAD + thread.toString() + Strings.NEWLINE);

      // Force Close
      System.err.println(LOG_FORCE_CLOSE + forceClose);
      fw.write(LOG_FORCE_CLOSE + forceClose + Strings.NEWLINE);

      // Stack
      if (th != null)
      {
        th.printStackTrace(System.err);
        th.printStackTrace(new PrintWriter(fw));
      }
    }
    catch (final IOException ioe)
    {
      ioe.printStackTrace(System.err);
    }
    finally
    {
      try
      {
        if (fw != null)
        {
          fw.close();
        }
      }
      catch (final IOException ioe)
      {
        //Nothing
      }
    }
  }

  /**
   * 
   * @param th
   * @return
   */
  private static String getStackTrace(final Throwable th)
  {
    final Writer result = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(result);
    th.printStackTrace(printWriter);

    return result.toString();
  }

  /**
   * 
   * @param thread
   * @param th
   * @param forceClose
   * @param inVersion
   * @param inPackageName
   */
  static void sendToServer(final Thread thread, final Throwable th, final boolean forceClose, final String inVersion, final String inPackageName)
  {
    final DefaultHttpClient httpClient = new DefaultHttpClient();
    final HttpPost httpPost = new HttpPost(URL);
    final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
    nvps.add(new BasicNameValuePair(PARAM_THREAD, thread.toString()));
    nvps.add(new BasicNameValuePair(PARAM_STACKTRACE, getStackTrace(th)));
    nvps.add(new BasicNameValuePair(PARAM_OS, Build.VERSION.RELEASE));
    nvps.add(new BasicNameValuePair(PARAM_VERSION, inVersion));
    nvps.add(new BasicNameValuePair(PARAM_PACKAGE, inPackageName));
    nvps.add(new BasicNameValuePair(PARAM_FORCE_CLOSE, Strings.VIDE + forceClose));
    try
    {
      httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
      httpClient.execute(httpPost);
    }
    catch (final Throwable innerTh)
    {
      innerTh.printStackTrace(System.err);
    }
  }

  /**
   * 
   * @param sendReport
   */
  public void setSendReport(final boolean sendReport)
  {
    this.sendReport = sendReport;
  }
}
