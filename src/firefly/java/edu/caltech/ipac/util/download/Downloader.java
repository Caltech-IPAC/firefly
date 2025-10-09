/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;
/**
 * User: roby
 * Date: 1/8/14
 * Time: 11:32 AM
 */


import edu.caltech.ipac.util.FileUtil;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Date;

/**
 * @author Trey Roby
 */
public class Downloader {

    private DataInputStream _in;
    private OutputStream _out;
    private ByteBuffer mappedOutBuf;
    private final File outfile;
    private final long _downloadSize;
    private long _maxDownloadSize= 0L;
    private DownloadListener downloadListener= null;
    private static final int BUFFER_SIZE = FileUtil.BUFFER_SIZE;

    public Downloader(DataInputStream in, OutputStream out, long contentLength) {
        _in = in;
        _out = out;
        _downloadSize = contentLength;
        outfile= null;
    }

    public Downloader(DataInputStream in, File outfile, long contentLength) {
        _in = in;
        _out = null;
        this.outfile = outfile;
        _downloadSize = contentLength;
    }

    public static void doDownload(DataInputStream in,
                                  File outfile,
                                  long contentLength,
                                  long maxSize,
                                  DownloadListener listener) throws IOException, FailedRequestException {
        var downloader = new Downloader(in, outfile, contentLength);
        downloader.setDownloadListener(listener);
        downloader.setMaxDownloadSize(maxSize);
        downloader.download();
    }

    public static void doDownload(DataInputStream in,
                                  ByteBuffer mappedOutBuf,
                                  long contentLength,
                                  long maxSize,
                                  DownloadListener listener) throws IOException, FailedRequestException {
        var downloader = new Downloader(in, (OutputStream) null, contentLength);
        downloader.setDownloadListener(listener);
        downloader.setMaxDownloadSize(maxSize);
        downloader.mappedOutBuf= mappedOutBuf;
        downloader.download();
    }

    public void setMaxDownloadSize(long maxDownloadSize) { _maxDownloadSize= maxDownloadSize; }

    public void download() throws IOException, FailedRequestException {

        FileChannel fc = outfile!= null
                ? FileChannel.open(outfile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                : null;


        long total = _downloadSize;
        String messStr;
        String outStr;
        Date startDate = null;
        TimeStats timeStats = null;
        long totalRead = 0;
        if (total > 0) {
            messStr = " out of " + FileUtil.getSizeAsString(total);
        } else {
            messStr = "";
        }
        try {
            int read;
            byte[] buffer = new byte[BUFFER_SIZE];
            long sTime= System.currentTimeMillis();
            while ((read = _in.read(buffer)) != -1) {
                totalRead += read;
                if (System.currentTimeMillis() - sTime > 750) {
                    sTime = System.currentTimeMillis();
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
                    fireDownloadListeners(totalRead, total, timeStats, outStr);
                }
                if (_out!=null) _out.write(buffer, 0, read);
                if (fc!=null) {
                    ByteBuffer fcBuff = ByteBuffer.wrap(buffer);
                    while (fcBuff.hasRemaining()) {
                        int ignore= fc.write(fcBuff);
                    }
                }
                if (mappedOutBuf!=null) {
                    ByteBuffer b = ByteBuffer.wrap(buffer,0,read);
                    while (b.hasRemaining()) {
                        mappedOutBuf.put(b);
                    }
                }
            }

        } catch (EOFException e) {
            if (totalRead == 0) {
                throw new IOException("No data was downloaded",e);
            }
        } finally {
            FileUtil.silentClose(_out);
            if (fc!=null) fc.close();
        }
        _out = null;
        _in = null;
    }

//=====================================================================
//----------- add / remove listener methods -----------
//=====================================================================

    public void setDownloadListener(DownloadListener l) {
        this.downloadListener= l;
    }

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
                if (sec <=5)       retval= "Less than 5 sec";
                else if (sec <=30) retval= "Less than 30 sec";
                else if (sec <=45) retval= "Less than a minute";
                else if (sec < 75) retval= "About a minute";
                else               retval= "About " + sec/60 + " minutes";
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


    private void fireDownloadListeners(long current, long max, TimeStats timeStats, String mess) {
        if (downloadListener==null) return;
        DownloadEvent ev;
        if (timeStats != null) {
            ev = new DownloadEvent(this, current, max,
                                   timeStats.elapseSec,
                                   timeStats.remainSec,
                                   timeStats.elapseStr,
                                   timeStats.remainingStr,
                                   mess);
        } else {
            ev = new DownloadEvent(this, current, max, 0, 0, "", "", mess);
        }
        downloadListener.dataDownloading(ev);
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

