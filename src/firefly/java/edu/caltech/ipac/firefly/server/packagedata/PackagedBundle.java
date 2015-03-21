/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.core.background.BackgroundState;
import edu.caltech.ipac.firefly.core.background.DefaultBackgroundPart;
import edu.caltech.ipac.firefly.core.background.PackageProgress;
/**
 * User: roby
 * Date: Sep 24, 2008
 * Time: 12:27:11 PM
 * $Id: PackagedBundle.java,v 1.11 2012/09/18 22:26:16 roby Exp $
 */


/**
 * @author Trey Roby
 */
public class PackagedBundle extends DefaultBackgroundPart {

    private String _url;
    private String _desc;
    private int _packageIdx;
    private int _firstFileIdx; // starts from 0
    private int _numFiles;
    private int _processedFiles;
    private long _totalBytes;
    private long _processedBytes;
    private long _uncompressedBytes;
    private long _compressedBytes;

    // This variable should go away as we switch from static to dynamic bundles.
    // If the bundle is dynamic, we don't know in advance what number of files
    // it will contain. A follow up bundle, also dynamic, may be created for everything that
    // did not fit at the time finish was called.
    // If the bundle is not dynamic, it is assumed to be static, which means we should attempt
    // all files from _firstFileIdx to _firstFileIdx+_numFiles into it and no follow up bundle
    // will be created.
    //boolean _dynamicBundle = false;

    // follow up bundle if this bundle did not include all the files
    private PackagedBundle _followUpBundle = null;

    public PackagedBundle(int id, int firstFileIdx, int numFiles, long totalBytes) {
        super(BackgroundState.WAITING);
        _packageIdx = id;
        _firstFileIdx = firstFileIdx;
        _numFiles = numFiles;
        _desc = "Files "+(firstFileIdx+1)+"-"+(firstFileIdx+numFiles);
        _totalBytes = totalBytes;
        _processedBytes = 0;
        _uncompressedBytes = 0;
        _compressedBytes = 0;

    }

    private PackagedBundle() {}

    public void addProcessedBytes(long completedFiles, long completedBytes, long uncompressedBytes, long compressedBytes) {
        // ideally processed and uncompressed bytes should be equal
        // they are not equal if estimated size is not equal real size
        // or if some files that had to be packaged are missing
        _processedFiles += completedFiles;
        _processedBytes += completedBytes;
        _uncompressedBytes += uncompressedBytes;
        _compressedBytes += compressedBytes;
        if (completedBytes > 0)
            setState(BackgroundState.WORKING);
    }

    public void setProcessedFiles(int procFiles) { _processedFiles= procFiles; }

    public void finish(String url) {
        _url = url;
        setState(BackgroundState.SUCCESS);
        assert(_numFiles > _processedFiles);
        if (_numFiles > _processedFiles) {
            //create follow-up bundle for files that were not packaged
            int newPackageIdx = _packageIdx+1;
            int newStartIdx = _firstFileIdx+_processedFiles;
            int newNumFiles = _numFiles-_processedFiles;
            long newTotalBytes = (_totalBytes == 0) ? 0 : (_totalBytes - _processedBytes);
            assert (newTotalBytes >= 0);
            _numFiles = _processedFiles;
            _totalBytes = _processedBytes;
            _desc = "Files "+(_firstFileIdx+1)+"-"+(_firstFileIdx+_numFiles);
            _followUpBundle = new PackagedBundle(newPackageIdx, newStartIdx, newNumFiles, newTotalBytes);
        }
    }

    public void fail() {
        if (getState() != BackgroundState.SUCCESS) {
            setState(BackgroundState.FAIL);
        }
    }

    public void cancel() {
        if (getState() != BackgroundState.SUCCESS) {
            setState(BackgroundState.CANCELED);
        }
    }

    public int getPackageIdx() {return _packageIdx;}

    public int getFirstFileIdx() {return _firstFileIdx;}

    public int getNumFiles() {return _numFiles;}
    public void setNumFiles(int numFiles) {_numFiles= numFiles;}

    public String getDesc() {return _desc;}

    public String getUrl() {return _url;}
    public void setUrl(String url) {_url= url;}
    public void setDesc(String desc) {_desc= desc;}

    /**
     * @return number of processed files
     */
    public int getProcessedFiles() {return _processedFiles;}

    /**
     *  @return total estimated size of the files in the bundle
     */
    public long getTotalBytes() {return _totalBytes;}

    /**
     * @return estimated size of the processed files
     */
    public long getProcessedBytes() {return _processedBytes; }
    public void setProcessedBytes(long pb) {_processedBytes= pb; }

    /**
     * @return uncompressed size of files actually packaged
     */
    public long getUncompressedBytes() {return _uncompressedBytes; }
    public void setUncompressedBytes(long uB) {_uncompressedBytes= uB; }

    /**
     * @return compressed size of the package
     */
    public long getCompressedBytes() {return _compressedBytes; }
    public void setCompressedBytes(long cb) {_compressedBytes= cb; }

    /**
     * @return the next bundle for files that were not packaged
     */
    public PackagedBundle getFollowUpBundle() {return _followUpBundle; }


    @Override
    public String getFileKey() {
        String fileKey= null;
        String url= getUrl();
        String sAry[]= url.split("\\?");
        if (sAry.length==2)  {
            fileKey= sAry[1];
        }
        return fileKey;
    }


    @Override
    public boolean hasFileKey() {
        return (getState()==BackgroundState.SUCCESS && getFileKey()!=null);
    }


    public PackageProgress makePackageProgress() {
        return new PackageProgress(getNumFiles(),
                                   getProcessedFiles(),
                                   getTotalBytes(),
                                   getProcessedBytes(),
                                   getCompressedBytes(),
                                   getUrl());
    }

    //======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

}

