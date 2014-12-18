package edu.caltech.ipac.firefly.core.background;
/**
 * User: roby
 * Date: 6/6/14
 * Time: 11:22 AM
 */


import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;

import java.io.Serializable;

/**
* @author Trey Roby
*/
public class PackageProgress implements Serializable, HandSerialize {
    private final static String SPLIT_TOKEN= "--BGProgress--";
    private int _totalFiles= 0;
    private int _processedFiles= 0;
    private long _totalBytes= 0;
    private long _processedBytes= 0;
    private long _finalCompressedBytes= 0;
    private String _url= null;

    PackageProgress() {}


    public PackageProgress(int totalFiles,
                           int processedFiles,
                           long totalBytes,
                           long processedBytes,
                           long finalCompressedBytes,
                           String url) {
        _totalFiles = totalFiles;
        _processedFiles = processedFiles;
        _totalBytes = totalBytes;
        _processedBytes = processedBytes;
        _finalCompressedBytes = finalCompressedBytes;
        _url= url;
    }

    public int getTotalFiles() {
        return _totalFiles;
    }

    public int getProcessedFiles() {
        return _processedFiles;
    }

    public long getTotalByes() {
        return _totalBytes;
    }

    public long getProcessedBytes() {
        return _processedBytes;
    }

    public long getFinalCompressedBytes() {
        return _finalCompressedBytes;
    }

    public String getURL() { return _url; }

    public boolean isDone() { return _url!=null; }

    public String serialize() {
        return StringUtils.combine(SPLIT_TOKEN,
                                   _totalFiles + "",
                                   _processedFiles + "",
                                   _totalBytes + "",
                                   _processedBytes + "",
                                   _finalCompressedBytes + "",
                                   _url);
    }
    public static PackageProgress parse(String s) {
        if (s==null) return null;
        PackageProgress p= new PackageProgress();
        try {
            String sAry[]= StringUtils.parseHelper(s,6,SPLIT_TOKEN);
            int i= 0;
            p._totalFiles= StringUtils.getInt(sAry[i++],0);
            p._processedFiles= StringUtils.getInt(sAry[i++],0);
            p._totalBytes= StringUtils.getLong(sAry[i++],0);
            p._processedBytes= StringUtils.getLong(sAry[i++],0);
            p._finalCompressedBytes= StringUtils.getLong(sAry[i++],0);
            p._url= StringUtils.checkNull(sAry[i++]);
        } catch (IllegalArgumentException e) {
            p= null;
        }
        return p;
    }
}

