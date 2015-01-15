/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;
/**
 * User: roby
 * Date: 1/8/14
 * Time: 11:32 AM
 */


import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Trey Roby
 */
public class Downloader {

    private enum ListenerCall {
        INCREMENT, START, DONE, ABORTED
    }

    private DataInputStream _in;
    private OutputStream _out;
    private Object _downloadObj;
    private long _downloadSize;
    private long _maxDownloadSize= 0L;
    private List<DownloadListener> _listenerList = new ArrayList<DownloadListener>(2);
    private static final int BUFFER_SIZE = (int) (16 * FileUtil.K);




    public Downloader(DataInputStream in,
                      OutputStream out,
                      URLConnection conn,
                      Object downloadObj) {
        Assert.argTst(out != null && in != null && conn != null,
                      "in, out, and conn cannot be null");
        _in = in;
        _out = out;
        _downloadObj = downloadObj;
        _downloadSize = conn!=null? conn.getContentLength() : 0;
    }

    public Downloader(DataInputStream in, OutputStream out, URLConnection conn) {
        this(in, out, conn, null);
    }

    public void setMaxDownloadSize(long maxDownloadSize) { _maxDownloadSize= maxDownloadSize; }

    public void download() throws IOException, VetoDownloadException, FailedRequestException {

        Assert.tst((_out != null && _in != null),
                   "Attempting to call URLDownload twice, an instance is " +
                           "only good for one call");

        int cnt = 0;
        long total = _downloadSize;
        String messStr;
        String outStr;
        Date startDate = null;
        TimeStats timeStats = null;
        int informInc = (int) (2* FileUtil.MEG / BUFFER_SIZE);
        long totalRead = 0;
        boolean elapseIncreased = false;
        long lastElapse = 0;
        boolean aborted = false;
        if (total > 0) {
            messStr = " out of " + FileUtil.getSizeAsString(total);
        } else {
            messStr = "";
        }
        try {
            if (total > 1024) {
                outStr = "Starting download of " +
                        FileUtil.getSizeAsString(total);
            } else {
                outStr = "Starting download";
            }
            fireDownloadListeners(0, total, null, outStr,
                                  _downloadObj, ListenerCall.START);

            int read;
            byte[] buffer = new byte[BUFFER_SIZE];
            while ((read = _in.read(buffer)) != -1) {
                totalRead += read;
                if ((++cnt % informInc) == 0) {
                    if (startDate == null) startDate = new Date();
                    if (total > 0) {
                        timeStats = computeTimeStats(startDate, totalRead, total);
                        outStr = FileUtil.getSizeAsString(totalRead) +
                                messStr + "  -  " +
                                timeStats.remainingStr;
                    } else {
                        outStr = (totalRead / 1024) + messStr;
                        timeStats = new TimeStats();
                    }
                    if (_maxDownloadSize>0 && totalRead>_maxDownloadSize) {
                        throw new FailedRequestException(
                                "File too big to download, Exceeds maximum size of: "+ FileUtil.getSizeAsString(_maxDownloadSize),
                                "URL does not have a content length header but the " +
                                        "downloaded data exceeded the max size of " +_maxDownloadSize);
                    }
                    fireVetoDownloadListeners(totalRead, total, timeStats, outStr,
                            _downloadObj);
                    fireDownloadListeners(totalRead, total, timeStats,
                            outStr, _downloadObj,
                            ListenerCall.INCREMENT);
                    if (!elapseIncreased) {
                        if (lastElapse == timeStats.elapseSec) {
                            elapseIncreased = true;
                            informInc *= 10;
                        }
                    }
                    lastElapse = timeStats.elapseSec;
                }
                _out.write(buffer, 0, read);
            }

        } catch (EOFException e) {
            if (totalRead == 0) {
                IOException ioe = new IOException("No data was downloaded");
                ioe.initCause(e);
                throw ioe;
            }
        } catch (VetoDownloadException e) {
            aborted = true;
            throw e;
        } finally {
            FileUtil.silentClose(_out);
            if (totalRead > 0) {
                outStr = "Download Completed.";
                ListenerCall stat = aborted ?
                                    ListenerCall.ABORTED : ListenerCall.DONE;
                fireDownloadListeners(total, total, timeStats, outStr,
                                      _downloadObj, stat);
            }
        }
        _listenerList = null;
        _out = null;
        _in = null;
        //_conn            = null;
    }

//=====================================================================
//----------- add / remove listener methods -----------
//=====================================================================

    public void addDownloadListener(DownloadListener l) {
        if (l!=null) _listenerList.add(l);
    }

    public void removeDownloadListener(DownloadListener l) {
        if (_listenerList != null) _listenerList.remove(l);
    }

    public void fireVetoDownloadListeners(long current,
                                          long max,
                                          TimeStats timeStats,
                                          String mess,
                                          Object downloadObj) throws VetoDownloadException {
        List<DownloadListener> newlist;
        DownloadEvent ev = new DownloadEvent(this, current, max,
                                             timeStats.elapseSec,
                                             timeStats.remainSec,
                                             timeStats.elapseStr,
                                             timeStats.remainingStr,
                                             downloadObj, mess);
        synchronized (this) {
            newlist = new ArrayList<DownloadListener>( _listenerList);
        }

        for (DownloadListener listener : newlist) {
            listener.checkDataDownloading(ev);
        }
    }


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private TimeStats computeTimeStats(Date startDate, long cnt, long totalSize) {
        TimeStats timeStats = new TimeStats();
        Date now = new Date();
        long elapseTime = now.getTime() - startDate.getTime();
        long projectedTime = (elapseTime * totalSize) / cnt;
        double percentLeft = 1.0F - ((double) cnt / (double) totalSize);
        long remainingTime = (long) (projectedTime * percentLeft + 1000L);

        timeStats.elapseSec = elapseTime / 1000;
        timeStats.remainSec = remainingTime / 1000;
        timeStats.remainingStr = millsecToFormatStr(remainingTime, true);
        timeStats.elapseStr = millsecToFormatStr(elapseTime);

        return timeStats;
    }

    public static String millsecToFormatStr(long milliSec,
                                            boolean userFriendly) {
        String retval;
        if (userFriendly) {
            long sec= milliSec / 1000;

            if (sec < 3300) {
                if (sec <=5)                     retval= "Less than 5 sec";
                else if (sec <=30)               retval= "Less than 30 sec";
                else if (sec <=45)               retval= "Less than a minute";
                else if (sec < 75 && sec > 45)   retval= "About a minute";
                else                     retval= "About " + sec/60 + " minutes";
            }
            else {
                float hour= sec / 3600F;
                if (hour < 1.2F && hour > .8F) {
                    retval= "About an hour";
                }
                else {
                    retval= millsecToFormatStr(milliSec);
                }
            }
        }
        else {
            retval= millsecToFormatStr(milliSec);
        }
        return retval;
    }

    public static String millsecToFormatStr(long milliSec) {
        String minStr, secStr;
        long inSec= milliSec / 1000;
        long hours= inSec/3600;
        long mins= (inSec - (hours*3600)) / 60;
        minStr=  (mins < 10) ? "0" + mins : mins + "";
        long secs= inSec - ((hours*3600) + (mins*60));
        secStr=  (secs < 10) ? "0" + secs : secs + "";
        return hours + ":" + minStr + ":" + secStr;
    }


    protected void fireDownloadListeners(long current,
                                         long max,
                                         TimeStats timeStats,
                                         String mess,
                                         Object downloadObj,
                                         ListenerCall type) {
        List<DownloadListener> newlist;
        DownloadEvent ev;
        if (timeStats != null) {
            ev = new DownloadEvent(this, current, max,
                                   timeStats.elapseSec,
                                   timeStats.remainSec,
                                   timeStats.elapseStr,
                                   timeStats.remainingStr,
                                   downloadObj, mess);
        } else {
            ev = new DownloadEvent(this, current, max, downloadObj, mess);
        }
        synchronized (this) {
            newlist = new ArrayList<DownloadListener>(_listenerList);
        }

        for (DownloadListener listener : newlist) {
            switch (type) {
                case INCREMENT:
                    listener.dataDownloading(ev);
                    break;
                case START:
                    listener.beginDownload(ev);
                    break;
                case DONE:
                    listener.downloadCompleted(ev);
                    break;
                case ABORTED:
                    listener.downloadAborted(ev);
                    break;
            }
        }
    }

//======================================================================
//------------------ Private Inners classes ----------------------------
//======================================================================

    private static class TimeStats {
        String remainingStr = "";
        String elapseStr = "";
        long remainSec = 0;
        long elapseSec = 0;
    }


}

