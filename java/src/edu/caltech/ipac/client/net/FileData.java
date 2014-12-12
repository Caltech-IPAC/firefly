package edu.caltech.ipac.client.net;

import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.cache.FileHolder;

import java.io.File;
import java.io.Serializable;

/**
 * Date: Aug 31, 2005
 *
 * @author Trey Roby
 * @version $id:$
 */
public class FileData implements FileHolder, Serializable, Cloneable {

    public enum FileType {
        FITS("Fits file type"),
        TABLE("Table file type"),
        TEXT("Text file type"),
        UNKNOWN("Unknown file type");
        private String _desc;
        FileType(String desc) { _desc= desc; }
        public String toString() { return _desc; }
    }

    private final File _file;
    private final String _suggestedExternalName;
    private final FileType _fileType;
    private final boolean _fileDownloaded;

    public FileData(File file, String suggestedExternalName) {
        this(file,suggestedExternalName, null);
    }
    public FileData(File file, String suggestedExternalName, FileType fType) {
        this(file,suggestedExternalName,fType,true);
    }

    public FileData(File file,
                    String suggestedExternalName,
                    FileType fType,
                    boolean fileDownloaded) {
        _file= file;
        _suggestedExternalName = suggestedExternalName;
        _fileDownloaded= fileDownloaded;
        if (fType != null) {
            _fileType= fType;
        } else if (_suggestedExternalName == null) {
            _fileType= FileType.FITS;
        } else {
            String ext = FileUtil.getExtension(_suggestedExternalName);
            if (ext.equalsIgnoreCase(FileUtil.TBL)) {
                _fileType= FileType.TABLE;
            } else if (ext.equalsIgnoreCase(FileUtil.TXT)){
                _fileType= FileType.TEXT;
            } else {
                _fileType= FileType.FITS;
            }
        }
    }

//============================================================================
//---------------------------- Public Methods --------------------------------
//============================================================================

    public File getFile() {  return _file; }
    public FileType getFileType() {  return _fileType; }
    public String getSugestedExternalName()  { return _suggestedExternalName;}
    public boolean isDownloaded() { return _fileDownloaded; }

    public Object clone() {
        return new FileData(_file, _suggestedExternalName,_fileType);
    }

}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313) 
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