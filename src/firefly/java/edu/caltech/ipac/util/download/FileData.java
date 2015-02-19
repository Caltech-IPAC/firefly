/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.download;

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

