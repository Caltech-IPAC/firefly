package edu.caltech.ipac.firefly.server.packagedata;

import edu.caltech.ipac.firefly.data.HasAccessInfos;

import java.io.File;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;

/**
 * User: roby
 * Date: Sep 26, 2008
 * Time: 12:51:41 PM
 * $Id: FileGroup.java,v 1.8 2011/03/17 23:57:33 tatianag Exp $
 */


/**
 * @author Trey Roby
 */
public class FileGroup implements Iterable<FileInfo>, HasAccessInfos {

    private final Collection<FileInfo> _fileList;
    private final File _baseDir;
    private long _sizeInBytes;
    private final String _desc;
    private boolean _packageTogether = false;
    private transient ArrayList<FileInfo> orderedFiles = null;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================


    public FileGroup(Collection<FileInfo> fileList,
                     File baseDir,
                     long sizeInBytes,
                     String desc) {
        _fileList= fileList;
        _baseDir= baseDir;
        _sizeInBytes= sizeInBytes;
        _desc = desc;
    }


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public Iterator<FileInfo> iterator() { return _fileList.iterator(); }

    public void setPackageTogether(boolean packageTogether) {
        _packageTogether = packageTogether;
    }

    public File getBaseDir() { return _baseDir; }
    public long getSizeInBytes() { return _sizeInBytes; }
    public void setSizeInBytes(long sizeInBytes) { _sizeInBytes = sizeInBytes; }    
    public String getDesc() { return _desc; }
    public boolean isPackageTogether() {return _packageTogether; }


    public FileInfo getFileInfo(int idx) {
        if (_fileList != null && orderedFiles == null) {
            orderedFiles = new ArrayList<FileInfo>(_fileList.size());
            orderedFiles.addAll(_fileList);
        }
        if (orderedFiles == null || idx < 0 || idx >= orderedFiles.size()) {
            return null;
        }
        return orderedFiles.get(idx);
    }

    public int getSize() {
        return _fileList == null ? 0 : _fileList.size();
    }

    public boolean hasAccess(int index) {
        FileInfo fi = getFileInfo(index);
        if (fi == null) {
            throw new IndexOutOfBoundsException("Group size:" + getSize() + "  requested index:" + index);
        } else {
            return fi.hasAccess();
        }
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

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
