/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;


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
public record Downloader(DataInputStream in, long contentLength,
                         DownloadListener downloadListener, long maxDownloadSize) {

    private static final int BUFFER_SIZE = FileUtil.BUFFER_SIZE;

    public static void download(DataInputStream in, OutputStream out) throws IOException, FailedRequestException {
        download(in, out, 0, 0, null);
    }

    public static void download(DataInputStream in,
                                OutputStream out,
                                long contentLength,
                                long maxSize,
                                DownloadListener listener) throws IOException, FailedRequestException {
        try (out) {
            var downloader = new Downloader(in, contentLength, listener, maxSize);
            downloader.processDownload((buff, bytesRead) -> out.write(buff, 0, bytesRead));
        }
    }


    public static void download(DataInputStream in,
                                File outfile,
                                long contentLength,
                                long maxSize,
                                DownloadListener listener) throws IOException, FailedRequestException {
        try (var fc = FileChannel.open(outfile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            var downloader = new Downloader(in, contentLength, listener, maxSize);
            downloader.processDownload(
                    (buff, bytesRead) -> {
                var ignore= fc.write(ByteBuffer.wrap(buff, 0, bytesRead));
            });
        }
    }

    public static void download(DataInputStream in,
                                ByteBuffer outByteBuffer,
                                long contentLength,
                                long maxSize,
                                DownloadListener listener) throws IOException, FailedRequestException {
        var downloader = new Downloader(in, contentLength, listener, maxSize);
        downloader.processDownload((buff, bytesRead) -> outByteBuffer.put(ByteBuffer.wrap(buff, 0, bytesRead)));
    }


    private void processDownload(Writer writer) throws IOException, FailedRequestException {

        String outStr;
        Date startDate = null;
        TimeStats timeStats;
        long totalRead = 0;
        var messStr = (contentLength > 0) ? " out of " + FileUtil.getSizeAsString(contentLength) : "";
        try (in) {
            int byteRead;
            byte[] buffer = new byte[BUFFER_SIZE];
            long sTime = System.currentTimeMillis();
            while ((byteRead = in.read(buffer)) != -1) {
                totalRead += byteRead;
                if (System.currentTimeMillis() - sTime > 750) {
                    sTime = System.currentTimeMillis();
                    if (startDate == null) startDate = new Date();
                    if (contentLength > 0) {
                        timeStats = computeTimeStats(startDate, totalRead, contentLength);
                        outStr = FileUtil.getSizeAsString(totalRead) +
                                messStr + "  -  " +
                                timeStats.remainingStr;
                    } else {
                        outStr = (totalRead / 1024) + messStr;
                        timeStats = null;
                    }
                    checkSize(totalRead);
                    fireDownloadListeners(totalRead, contentLength, timeStats, outStr);
                }
                writer.write(buffer, byteRead);
            }
        } catch (EOFException e) {
            if (totalRead == 0) {
                throw new IOException("No data was downloaded", e);
            }
        }
    }

    private void checkSize(long totalRead) throws FailedRequestException {
        if (maxDownloadSize > 0 && totalRead > maxDownloadSize) {
            throw new FailedRequestException(
                    "File too big to download, Exceeds maximum size of: " + FileUtil.getSizeAsString(maxDownloadSize),
                    "URL does not have a content length header but the " +
                            "downloaded data exceeded the max size of " + maxDownloadSize);
        }
    }

    private void fireDownloadListeners(long current, long max, TimeStats timeStats, String mess) {
        if (downloadListener == null) return;
        DownloadEvent ev = timeStats == null
                ? new DownloadEvent(this, current, max, 0, 0, "", "", mess)
                : new DownloadEvent(this, current, max, timeStats.elapseSec, timeStats.remainSec,
                timeStats.elapseStr, timeStats.remainingStr, mess);
        downloadListener.dataDownloading(ev);
    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static TimeStats computeTimeStats(Date startDate, long cnt, long totalSize) {
        Date now = new Date();
        long elapseTime = now.getTime() - startDate.getTime();
        long projectedTime = (elapseTime * totalSize) / cnt;
        double percentLeft = 1.0F - ((double) cnt / (double) totalSize);
        long remainingTime = (long) (projectedTime * percentLeft + 1000L);

        return new TimeStats(timeeFormatedStr(remainingTime, true), millToSecStr(elapseTime),
                remainingTime / 1000, elapseTime / 1000);
    }

    public static String timeeFormatedStr(long milliSec, boolean userFriendly) {
        String retval;
        if (userFriendly) {
            long sec = milliSec / 1000;

            if (sec < 3300) {
                if (sec <= 5) retval = "Less than 5 sec";
                else if (sec <= 30) retval = "Less than 30 sec";
                else if (sec <= 45) retval = "Less than a minute";
                else if (sec < 75) retval = "About a minute";
                else retval = "About " + sec / 60 + " minutes";
            } else {
                float hour = sec / 3600F;
                if (hour < 1.2F && hour > .8F) {
                    retval = "About an hour";
                } else {
                    retval = millToSecStr(milliSec);
                }
            }
        } else {
            retval = millToSecStr(milliSec);
        }
        return retval;
    }

    public static String millToSecStr(long milliSec) {
        String minStr, secStr;
        long inSec = milliSec / 1000;
        long hours = inSec / 3600;
        long mins = (inSec - (hours * 3600)) / 60;
        minStr = (mins < 10) ? "0" + mins : mins + "";
        long secs = inSec - ((hours * 3600) + (mins * 60));
        secStr = (secs < 10) ? "0" + secs : secs + "";
        return hours + ":" + minStr + ":" + secStr;
    }



//======================================================================
//------------------ Private Inners classes ----------------------------
//======================================================================

    private record TimeStats(String remainingStr, String elapseStr, long remainSec, long elapseSec) { }
    private interface Writer { void write(byte[] buffer, int bytesRead) throws IOException; }

}

