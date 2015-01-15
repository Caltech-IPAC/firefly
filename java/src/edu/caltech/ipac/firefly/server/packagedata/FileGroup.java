/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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

