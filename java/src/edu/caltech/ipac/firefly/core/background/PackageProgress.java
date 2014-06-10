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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
